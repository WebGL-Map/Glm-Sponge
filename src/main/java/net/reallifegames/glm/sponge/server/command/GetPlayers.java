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
import net.reallifegames.glm.sponge.GlMap;
import net.reallifegames.glm.sponge.server.GlmServerCommand;
import org.java_websocket.WebSocket;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Gets a list of players for a world.
 *
 * @author Tyler Bucher
 */
public final class GetPlayers extends GlmServerCommand {

    /**
     * Creates a new Glm server command.
     *
     * @param pluginInstance the plugin instance to get data from.
     */
    public GetPlayers(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public void handle(@Nonnull final WebSocket connection, @Nonnull final JsonNode commandNode) {
        // Make sure world is is present
        if (commandNode.get("worldId") == null) {
            connection.send("{\"error\": \"Incomplete request\"}");
            return;
        }
        // Get world if present
        final Optional<World> optionalWorld = Sponge.getGame().getServer().getWorld(UUID.fromString(commandNode.get("worldId").asText()));
        if (optionalWorld.isPresent()) {
            // Get world
            final World world = optionalWorld.get();
            // Get players in world
            final Collection<Player> players = world.getPlayers();
            try {
                StringWriter stringWriter = new StringWriter();
                JsonGenerator jsonGenerator = new JsonFactory().createGenerator(stringWriter);
                // Start json object
                jsonGenerator.writeStartObject();
                // echo command back
                jsonGenerator.writeStringField("cmd", "getPlayers");
                // Start data block
                jsonGenerator.writeObjectFieldStart("data");
                // echo world id
                jsonGenerator.writeStringField("worldId", world.getUniqueId().toString());
                // start player array
                jsonGenerator.writeArrayFieldStart("players");
                // echo players
                for (Player player : players) {
                    if (!player.hasPermission("glm.map.hide")) {
                        // start general player object
                        jsonGenerator.writeStartObject();
                        jsonGenerator.writeStringField("name", player.getName());
                        jsonGenerator.writeStringField("id", player.getUniqueId().toString());
                        jsonGenerator.writeObjectFieldStart("position");
                        jsonGenerator.writeNumberField("x", player.getLocation().getX());
                        jsonGenerator.writeNumberField("y", player.getLocation().getY());
                        jsonGenerator.writeNumberField("z", player.getLocation().getZ());
                        // close position object
                        jsonGenerator.writeEndObject();
                        // close general object
                        jsonGenerator.writeEndObject();
                    }
                }
                // close player array
                jsonGenerator.writeEndArray();
                // close data object
                jsonGenerator.writeEndObject();
                // Close json object
                jsonGenerator.writeEndObject();
                // Flush data and send to client
                jsonGenerator.flush();
                connection.send(stringWriter.toString());
            } catch (IOException e1) {
                pluginInstance.getLogger().error("Json error", e1);
                connection.send("{\"error\": \"Internal error\"}");
            }
        } else {
            connection.send("{\"error\": \"Unknown world\"}");
        }
    }

    @Override
    public long getInterval() {
        return pluginInstance.getConfig().getPlayerRequestTime();
    }
}
