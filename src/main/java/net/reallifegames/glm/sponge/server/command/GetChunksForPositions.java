/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Tyler Bucher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.reallifegames.glm.sponge.server.command;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.flowpowered.math.vector.Vector3i;
import net.reallifegames.glm.api.GlmChunk;
import net.reallifegames.glm.module.SqlModule;
import net.reallifegames.glm.sponge.GlMap;
import net.reallifegames.glm.sponge.RequestQueue;
import net.reallifegames.glm.sponge.WorldModuleSponge;
import net.reallifegames.glm.sponge.server.GlmServerCommand;
import org.java_websocket.WebSocket;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Returns chunk data for the map.
 *
 * @author Tyler Bucher
 */
public final class GetChunksForPositions extends GlmServerCommand {

    /**
     * Creates a new Glm server command.
     *
     * @param pluginInstance the plugin instance to get data from.
     */
    public GetChunksForPositions(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public void handle(@Nonnull final WebSocket connection, @Nonnull final JsonNode commandNode) {
        // Make sure world is is present
        if (commandNode.get("worldId") == null) {
            connection.send("{\"error\": \"Incomplete request\"}");
            return;
        }
        // Make sure world is is present
        if (commandNode.get("dataType") == null) {
            connection.send("{\"error\": \"Incomplete request\"}");
            return;
        }
        // Make sure world is is present
        if (commandNode.get("data") == null) {
            connection.send("{\"error\": \"Incomplete request\"}");
            return;
        }
        // Only continue if world is present
        final Optional<World> worldOptional = Sponge.getServer().getWorld(UUID.fromString(commandNode.get("worldId").asText()));
        if (worldOptional.isPresent()) {
            final World world = worldOptional.get();
            final String worldId = world.getUniqueId().toString();
            if (commandNode.get("data").isArray()) {
                // Loop through data
                Map<Vector3i, GlmChunk> glChunkMap = new HashMap<>();
                List<Integer> sqlPositions = new ArrayList<>();
                for (JsonNode node : commandNode.get("data")) {
                    // load chunks based off of position keys.
                    final Vector3i chunkLocation = commandNode.get("dataType").asText().equals("chunkPosition") ?
                            new Vector3i(node.get("x").asInt(), node.get("y").asInt(), node.get("z").asInt()) :
                            world.getLocation(node.get("x").asDouble(), node.get("y").asDouble(), node.get("z").asDouble()).getChunkPosition();
                    // Check respect for world border
                    if (pluginInstance.getConfig().shouldRespectWorldBorder()) {
                        final Optional<WorldBorder> optionalWorldBorder = pluginInstance.getWorldBorderMap()
                                .getOrDefault(world.getUniqueId(), Optional.empty());
                        if (optionalWorldBorder.isPresent()) {
                            final WorldBorder border = optionalWorldBorder.get();
                            final Vector3i chunkLocationWorldPos = new Vector3i(chunkLocation.getX() << 4,
                                    chunkLocation.getY() << 4, chunkLocation.getZ() << 4);
                            if (!RequestQueue.containsPosition(chunkLocationWorldPos, border.getCenter(), border.getDiameter() + 2.0d)) {
                                continue;
                            }
                        }
                    }
                    final boolean isCachePresent = WorldModuleSponge.chunkInCache(worldId,
                            chunkLocation.getX(),
                            chunkLocation.getY(),
                            chunkLocation.getZ()
                    );
                    // Get chunk
                    if (isCachePresent) {
                        final Optional<Chunk> optionalChunk = world.getChunk(chunkLocation);
                        // If chunk is loaded try and update if needed
                        if (optionalChunk.isPresent()) {
                            final GlmChunk cachedChunk = WorldModuleSponge.getGlChunk(world, optionalChunk.get(), pluginInstance);
                            glChunkMap.put(chunkLocation, cachedChunk);
                        } else {
                            // Get cached chunk if chunk is not loaded
                            glChunkMap.put(chunkLocation, WorldModuleSponge.getCacheChunk(
                                    worldId,
                                    chunkLocation.getX(),
                                    chunkLocation.getY(),
                                    chunkLocation.getZ()
                            ));
                        }
                    } else {
                        try (Connection databaseConnection = pluginInstance.getDataSource().getConnection()) {
                            // Check if present in sql server
                            if (SqlModule.rowExists(databaseConnection, worldId, chunkLocation.getX(), chunkLocation.getZ())) {
                                // Check if cache is limited
                                if (pluginInstance.getConfig().isCacheLimited()) {
                                    // Check if there is room in the cache
                                    if (WorldModuleSponge.isRoomInCache(worldId, pluginInstance.getConfig().getMaximumChunksInCache())) {
                                        buildSql(databaseConnection, worldId, chunkLocation, glChunkMap);
                                    } else {
                                        sqlPositions.add(chunkLocation.getX());
                                        sqlPositions.add(chunkLocation.getZ());
                                    }
                                } else {
                                    buildSql(databaseConnection, worldId, chunkLocation, glChunkMap);
                                }
                            } else {
                                RequestQueue.createOrAddToQueue(
                                        chunkLocation,
                                        commandNode.get("worldId").asText(),
                                        connection.getRemoteSocketAddress(),
                                        chunk->GetChunksForPositions.response(pluginInstance, connection, world, chunk)
                                );
                            }
                        } catch (SQLException e) {
                            pluginInstance.getLogger().error("Error getting sql database: ", e);
                        }
                    }
                }
                // Send info back to the clients if chunks are cached
                if (!glChunkMap.isEmpty() || !sqlPositions.isEmpty()) {
                    try {
                        StringWriter stringWriter = new StringWriter();
                        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(stringWriter);
                        // Start json object
                        jsonGenerator.writeStartObject();
                        // echo command back
                        jsonGenerator.writeStringField("cmd", "getChunkForPosition");
                        // Start data block
                        jsonGenerator.writeObjectFieldStart("data");
                        // echo world id
                        jsonGenerator.writeStringField("worldId", world.getUniqueId().toString());
                        // start chunk array
                        jsonGenerator.writeArrayFieldStart("chunks");
                        // Loop for cached chunks
                        for (Map.Entry<Vector3i, GlmChunk> kvp : glChunkMap.entrySet()) {
                            jsonGenerator.writeStartObject();
                            jsonGenerator.writeObjectFieldStart("position");
                            jsonGenerator.writeNumberField("x", kvp.getKey().getX());
                            jsonGenerator.writeNumberField("y", kvp.getKey().getY());
                            jsonGenerator.writeNumberField("z", kvp.getKey().getZ());
                            // close spawn point object
                            jsonGenerator.writeEndObject();
                            jsonGenerator.writeStringField("chunkData", kvp.getValue().getChunkData());
                            jsonGenerator.writeStringField("chunkHeightData", kvp.getValue().getChunkHeightData());
                            jsonGenerator.writeNumberField("generationTime", kvp.getValue().getChunkGenerationTime());
                            // close chunk
                            jsonGenerator.writeEndObject();
                        }
                        // Loop for sql chunks
                        if (sqlPositions.size() > 0) {
                            try (Connection databaseConnection = pluginInstance.getDataSource().getConnection()) {
                                // Build and cache sql chunk
                                PreparedStatement preparedStatement = databaseConnection.prepareStatement(SqlModule.getNewGetChunks(sqlPositions));
                                // Set parameters
                                preparedStatement.setString(1, worldId);
                                // Execute query
                                ResultSet results = preparedStatement.executeQuery();
                                while (results.next()) {
                                    jsonGenerator.writeStartObject();
                                    jsonGenerator.writeObjectFieldStart("position");
                                    jsonGenerator.writeNumberField("x", results.getInt("X"));
                                    jsonGenerator.writeNumberField("y", 0);
                                    jsonGenerator.writeNumberField("z", results.getInt("Z"));
                                    // close spawn point object
                                    jsonGenerator.writeEndObject();
                                    jsonGenerator.writeStringField("chunkData", results.getString("chunk_data"));
                                    jsonGenerator.writeStringField("chunkHeightData", results.getString("height_data"));
                                    jsonGenerator.writeNumberField("generationTime", results.getLong("generation_time"));
                                    // close chunk
                                    jsonGenerator.writeEndObject();
                                }
                            } catch (SQLException e) {
                                pluginInstance.getLogger().error("Error getting sql database: ", e);
                            }
                        }
                        // close chunk array
                        jsonGenerator.writeEndArray();
                        // close data object
                        jsonGenerator.writeEndObject();
                        // Close json object
                        jsonGenerator.writeEndObject();
                        // Flush data and send to client
                        jsonGenerator.flush();
                        if (connection.isOpen()) {
                            connection.send(stringWriter.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            connection.send("{\"error\": \"Invalid World\"}");
        }
    }

    @Override
    public long getInterval() {
        // Normally one would think to use the getGlChunkCacheLifetime function from the config. However because a
        // client can request multiple different chunks back to back this function would end up punishing them for doing
        // so. Instead we will return 0 so the client can get as many chunks as they want in any given time period.
        return 0;
    }

    /**
     * Attempts to build a {@link GlmChunk} from sql data.
     *
     * @param connection    the sql connection.
     * @param worldId       the id of the world.
     * @param chunkLocation the location of the chunk.
     * @param glChunkMap    the cache map to send back to the user.
     * @throws SQLException if a database access error occurs or this method is called on a closed connection.
     */
    private static void buildSql(@Nonnull final Connection connection, @Nonnull final String worldId,
                                 @Nonnull final Vector3i chunkLocation,
                                 @Nonnull final Map<Vector3i, GlmChunk> glChunkMap) throws SQLException {
        // Build and cache sql chunk
        PreparedStatement preparedStatement = connection.prepareStatement(SqlModule.getGetChunkSqlString());
        // Set parameters
        preparedStatement.setString(1, worldId);
        preparedStatement.setInt(2, chunkLocation.getX());
        preparedStatement.setInt(3, chunkLocation.getZ());
        // Execute query
        ResultSet results = preparedStatement.executeQuery();
        if (results.next()) {
            final GlmChunk cachedChunk = WorldModuleSponge.buildFromParametersUnsafe(
                    worldId,
                    chunkLocation.getX(),
                    chunkLocation.getZ(),
                    results.getLong("generation_time"),
                    results.getString("chunk_data"),
                    results.getString("height_data")
            );
            glChunkMap.put(chunkLocation, cachedChunk);
        }
    }

    /**
     * Static response for lambda and asynchronous calls.
     *
     * @param pluginInstance the {@link GlMap} instance.
     * @param connection     the {@link WebSocket} connection.
     * @param world          the world of the chunk.
     * @param chunk          the {@link Chunk}to get a {@link GlmChunk} for.
     */
    private static void response(@Nonnull final GlMap pluginInstance, @Nonnull final WebSocket connection,
                                 @Nonnull final World world, @Nonnull final Chunk chunk) {
        if (connection.isOpen()) {
            try {
                StringWriter stringWriter = new StringWriter();
                JsonGenerator jsonGenerator = new JsonFactory().createGenerator(stringWriter);
                // Start json object
                jsonGenerator.writeStartObject();
                // echo command back
                jsonGenerator.writeStringField("cmd", "getChunkForPosition");
                // Start data block
                jsonGenerator.writeObjectFieldStart("data");
                // echo world id
                jsonGenerator.writeStringField("worldId", world.getUniqueId().toString());
                // start chunk array
                jsonGenerator.writeArrayFieldStart("chunks");
                jsonGenerator.writeStartObject();
                GlmChunk newGlChunk = WorldModuleSponge.getGlChunk(world, chunk, pluginInstance);
                jsonGenerator.writeObjectFieldStart("position");
                jsonGenerator.writeNumberField("x", chunk.getPosition().getX());
                jsonGenerator.writeNumberField("y", chunk.getPosition().getY());
                jsonGenerator.writeNumberField("z", chunk.getPosition().getZ());
                // close spawn point object
                jsonGenerator.writeEndObject();
                jsonGenerator.writeStringField("chunkData", newGlChunk.getChunkData());
                jsonGenerator.writeStringField("chunkHeightData", newGlChunk.getChunkHeightData());
                jsonGenerator.writeNumberField("generationTime", newGlChunk.getChunkGenerationTime());
                // close chunk
                jsonGenerator.writeEndObject();
                // close chunk object
                jsonGenerator.writeEndArray();
                // close data object
                jsonGenerator.writeEndObject();
                // Close json object
                jsonGenerator.writeEndObject();
                // Flush data and send to client
                jsonGenerator.flush();
                if (connection.isOpen()) {
                    connection.send(stringWriter.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
