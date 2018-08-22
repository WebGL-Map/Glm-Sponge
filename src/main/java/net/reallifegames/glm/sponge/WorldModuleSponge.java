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
package net.reallifegames.glm.sponge;

import com.flowpowered.math.vector.Vector3i;
import net.reallifegames.glm.GlmUtil;
import net.reallifegames.glm.GzipGlmChunk;
import net.reallifegames.glm.api.GlmChunk;
import net.reallifegames.glm.module.SqlModule;
import net.reallifegames.glm.module.WorldModule;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Helps with getting info from server and transforming it into data to be sent to the client.
 *
 * @author Tyler Bucher
 */
public final class WorldModuleSponge extends WorldModule {

    /**
     * Safely updates the cache and sql server.
     *
     * @param world          the world which contains the {@link Chunk}.
     * @param chunk          the {@link Chunk} to convert.
     * @param pluginInstance the plugin instance.
     * @param checked        should the function check the chunk cache time.
     */
    public static void updateCache(@Nonnull final World world, @Nonnull final Chunk chunk, @Nonnull final GlMap pluginInstance,
                                   boolean checked) {
        // Get world and chunk ids
        final String worldId = world.getUniqueId().toString();
        final Vector3i position = chunk.getPosition();
        final String chunkId = getChunkCacheId(position);
        // Add world if absent
        cache.computeIfAbsent(worldId, k->new HashMap<>());
        GlmChunk glChunk = cache.get(worldId).get(chunkId);
        // Check if chunk is in cache
        if (checked && glChunk != null) {
            // If current chunk time is less than expire time return it.
            if ((System.currentTimeMillis() - glChunk.getChunkGenerationTime()) < pluginInstance.getConfig().getGlChunkCacheLifetime()) {
                return;
            }
        }
        // Generate chunk
        glChunk = createGlChunk(chunk);
        // Update sql server
        try (Connection connection = pluginInstance.getDataSource().getConnection()) {
            SqlModule.updateGlChunk(connection, worldId, position.getX(), position.getZ(), glChunk);
        } catch (SQLException e) {
            pluginInstance.getLogger().error("Error updating sql server chunk: ", e);
        }
        // add chunk to cache if possible
        if (pluginInstance.getConfig().isCacheLimited()) {
            // Check if there is room in the cache
            if (cache.get(worldId).size() < pluginInstance.getConfig().getMaximumChunksInCache()) {
                cache.get(worldId).put(chunkId, glChunk);
            }
        } else {
            cache.get(worldId).put(chunkId, glChunk);
        }
    }

    /**
     * Get the {@link GlmChunk} for the corresponding minecraft {@link Chunk}.
     *
     * @param world          the world which contains the {@link Chunk}.
     * @param chunk          the {@link Chunk} to convert.
     * @param pluginInstance the plugin instance.
     * @return the newly created {@link GlmChunk} or a cached chunk.
     */
    @Nonnull
    public static GlmChunk getGlChunk(@Nonnull final World world, @Nonnull final Chunk chunk, @Nonnull final GlMap pluginInstance) {
        // Get world and chunk ids
        final String worldId = world.getUniqueId().toString();
        final Vector3i position = chunk.getPosition();
        final String chunkId = getChunkCacheId(position);
        // Add world if absent
        cache.computeIfAbsent(worldId, k->new HashMap<>());
        GlmChunk glChunk = cache.get(worldId).get(chunkId);
        // Check if chunk is in cache
        if (glChunk != null) {
            // If current chunk time is less than expire time return it.
            if ((System.currentTimeMillis() - glChunk.getChunkGenerationTime()) < pluginInstance.getConfig().getGlChunkCacheLifetime()) {
                return glChunk;
            }
        }
        // Generate chunk
        glChunk = createGlChunk(chunk);
        // Update sql server
        try (Connection connection = pluginInstance.getDataSource().getConnection()) {
            SqlModule.updateGlChunk(connection, worldId, position.getX(), position.getZ(), glChunk);
        } catch (SQLException e) {
            pluginInstance.getLogger().error("Error updating sql server chunk: ", e);
        }
        // add chunk to cache if possible
        if (pluginInstance.getConfig().isCacheLimited()) {
            // Check if there is room in the cache
            if (cache.get(worldId).size() < pluginInstance.getConfig().getMaximumChunksInCache()) {
                cache.get(worldId).put(chunkId, glChunk);
            }
        } else {
            cache.get(worldId).put(chunkId, glChunk);
        }
        // return chunk
        return glChunk;
    }

    /**
     * Creates a {@link GlmChunk} from the given {@link Chunk}.
     *
     * @param chunk the {@link Chunk} to convert.
     * @return the newly created {@link GlmChunk}.
     */
    @Nonnull
    private static GlmChunk createGlChunk(@Nonnull final Chunk chunk) {
        // Create chunk data
        final StringBuilder chunkBlockTypeBuilder = new StringBuilder();
        final int[] chunkBlockHeight = new int[256];
        // Create height index position and chunk boundary positions
        Vector3i min = chunk.getBlockMin();
        Vector3i max = chunk.getBlockMax();
        // Initialize block type
        int chunkHeightIndex = 0;
        // Create chunk data
        for (int z = min.getZ(); z < max.getZ() + 1; z++) {
            for (int x = min.getX(); x < max.getX() + 1; x++) {
                // Get block type at height
                final int y = chunk.getHighestYAt(15 - (max.getX() - x), 15 - (max.getZ() - z));
                // Add to block height and data builder
                chunkBlockTypeBuilder.append(chunk.getBlock(x, y == 0 ? 0 : y - 1, z).toString()).append('|');
                chunkBlockHeight[chunkHeightIndex++] = y;
            }
        }
        // Remove trailing character
        chunkBlockTypeBuilder.deleteCharAt(chunkBlockTypeBuilder.length()-1);
        // Return new gl chunk
        return new GzipGlmChunk(
                System.currentTimeMillis(),
                GzipGlmChunk.compressString(chunkBlockTypeBuilder.toString()),
                GzipGlmChunk.compressHeightData(GlmUtil.intToByte(chunkBlockHeight))
        );
    }

    /**
     * Gets a chunk id from the supplied information.
     *
     * @param chunk the chunk to get an id for.
     * @return the id of the chunk.
     */
    @Nonnull
    public static String getChunkCacheId(@Nonnull final Chunk chunk) {
        return getChunkCacheId(chunk.getPosition());
    }

    /**
     * Gets a chunk id from the supplied information.
     *
     * @param position the position to get an id for.
     * @return the id of the chunk position.
     */
    @Nonnull
    public static String getChunkCacheId(@Nonnull final Vector3i position) {
        return getChunkCacheId(position.getX(), position.getY(), position.getZ());
    }
}
