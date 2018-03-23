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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import net.reallifegames.glm.sponge.server.BaseGlmServer;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.WorldBorder;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Holds information about a chunk for lists or queues.
 *
 * @author Tyler Bucher
 */
class ChunkEntry {

    /**
     * The key or position of the chunk.
     */
    @Nonnull
    private final Vector3i key;

    /**
     * The {@link UUID} of the world in string form.
     */
    @Nonnull
    private final String worldId;

    /**
     * The list of {@link ChunkRunnable}s to run when this {@link ChunkEntry} is processed.
     */
    @Nonnull
    private final List<ChunkRunnable> callbackList;

    /**
     * The inet address of the client.
     */
    @Nonnull
    private final InetSocketAddress inetAddress;

    /**
     * Creates a new {@link ChunkEntry} with the given information.
     *
     * @param key      the key or position of the chunk.
     * @param worldId  the {@link UUID} of the world in string form.
     * @param callback the call back to create a list from for this {@link ChunkEntry}.
     */
    ChunkEntry(@Nonnull final Vector3i key, @Nonnull final String worldId, @Nonnull final ChunkRunnable callback,
               @Nonnull final InetSocketAddress inetAddress) {
        this.key = key;
        this.worldId = worldId;
        this.callbackList = new ArrayList<>(Collections.singletonList(callback));
        this.inetAddress = inetAddress;
    }

    /**
     * Creates a new {@link ChunkEntry} with the given information.
     *
     * @param key          the key or position of the chunk.
     * @param worldId      the {@link UUID} of the world in string form.
     * @param callbackList the list of callbacks to process.
     */
    ChunkEntry(@Nonnull final Vector3i key, @Nonnull final String worldId,
               @Nonnull final List<ChunkRunnable> callbackList, @Nonnull final InetSocketAddress inetAddress) {
        this.key = key;
        this.worldId = worldId;
        this.callbackList = callbackList;
        this.inetAddress = inetAddress;
    }

    /**
     * @return the position of the chunk.
     */
    @Nonnull
    public Vector3i getKey() {
        return key;
    }

    /**
     * @return the {@link UUID} of the world in string form.
     */
    @Nonnull
    public String getWorldId() {
        return worldId;
    }

    /**
     * @return the list of {@link ChunkRunnable}s to run when this {@link ChunkEntry} is processed.
     */
    @Nonnull
    public List<ChunkRunnable> getCallbackList() {
        return callbackList;
    }

    /**
     * @return the inet address of the client.
     */
    @Nonnull
    public InetSocketAddress getInetAddress() {
        return inetAddress;
    }
}

/**
 * Handles information about a {@link Chunk} and if it is already generated.
 *
 * @author Tyler Bucher
 */
class ChunkProcessEntry {

    /**
     * States if the chunk exists in file.
     */
    @Nonnull
    private final Boolean chunkExists;

    /**
     * The {@link Chunk} information in {@link ChunkEntry} form.
     */
    @Nonnull
    private final ChunkEntry chunkEntry;

    /**
     * Crates a new {@link ChunkProcessEntry} with the given information.
     *
     * @param chunkExists states if the chunk exists in file.
     * @param chunkEntry  the {@link Chunk} information in {@link ChunkEntry} form.
     */
    ChunkProcessEntry(@Nonnull final Boolean chunkExists, @Nonnull final ChunkEntry chunkEntry) {
        this.chunkExists = chunkExists;
        this.chunkEntry = chunkEntry;
    }

    /**
     * @return true if the chunk exists false otherwise.
     */
    @Nonnull
    public Boolean getChunkExists() {
        return chunkExists;
    }

    /**
     * @return the {@link Chunk} information in {@link ChunkEntry} form.
     */
    @Nonnull
    public ChunkEntry getChunkEntry() {
        return chunkEntry;
    }
}

/**
 * Handles requests from multiple clients via the {@link BaseGlmServer WebSocket server} with out duplicating requests for
 * chunk generation. It also helps to prevent the {@link GlMap} plugin from crashing the server due to too many chunk
 * generations during one tick.
 *
 * @author Tyler Bucher
 */
public final class RequestQueue {

    /**
     * The queue to handle requests from clients, and process if the requested chunk exists in file.
     */
    @Nonnull
    private static final LinkedBlockingQueue<ChunkEntry> checkQueue = new LinkedBlockingQueue<>();

    /**
     * Internal queue which handles chunk loading and or generation for the {@link Scheduler Sponge scheduler}.
     */
    @Nonnull
    private static final LinkedBlockingQueue<ChunkProcessEntry> processQueue = new LinkedBlockingQueue<>();

    /**
     * Should the {@link RequestQueue} continue to take new client requests.
     */
    private static volatile boolean process = true;

    /**
     * The instance of the {@link GlMap} plugin.
     */
    private static GlMap pluginInstance;

    /**
     * The maximum amount of time allowed during a tick to process chunks.
     */
    private static long processTime;

    /**
     * Sponge generation task.
     */
    private static Task generationTask;

    /**
     * The thread which processes client requests and passes off the result to the {@link #processQueue}.
     */
    @Nonnull
    private static final Thread queueExecutor = new Thread(()->{
        // Continue if we should handle more client requests
        while (process) {
            try {
                // Block and wait for the next item.
                final ChunkEntry entry = checkQueue.take();
                // If the world exists pass off the information gathered from WorldStorage#doesChunkExist
                // and create new item for the processQueue
                Sponge.getServer().getWorld(UUID.fromString(entry.getWorldId())).ifPresent(world->
                        world.getWorldStorage().doesChunkExist(entry.getKey()).thenAccept(chunkStatus->
                                // Only allow generation if config says so
                                processQueue.offer(new ChunkProcessEntry(
                                        pluginInstance.getConfig().canGenerateWorld() ? chunkStatus : false,
                                        entry
                                ))
                        )
                );
            } catch (InterruptedException e) {
                // This error should almost always be thrown because of how we close the thread.
                final GameState state = Sponge.getGame().getState();
                // Only log if the game is not shutting down.
                if (state != GameState.GAME_STOPPING && state != GameState.GAME_STOPPED) {
                    pluginInstance.getLogger().warn("The queue executor thread was stopped unexpectedly. " +
                            "This could be a problem");
                }
            }
        }
    });

    /**
     * Initializes this class. Starts the {@link #queueExecutor} thread and sponge task for processing chunk generation.
     *
     * @param pluginInstance the instance of {@link GlMap}.
     */
    public static void init(@Nonnull final GlMap pluginInstance) {
        RequestQueue.pluginInstance = pluginInstance;
        // Return because we can not load chunks.
        if (!pluginInstance.getConfig().canLoadWorld()) {
            return;
        }
        // Start queueExecutor
        queueExecutor.setName("Gl Map client queue executor");
        queueExecutor.start();
        // Compute the maximum amount of time allowed during a tick to process chunks
        processTime = Math.round(Sponge.getScheduler().getPreferredTickInterval() * pluginInstance.getConfig().getTotalTickPercentage());
        // Create the sponge task
        generationTask = Task.builder().execute(()->{
            // Get task start time
            final long startTime = System.currentTimeMillis();
            // get first item but don't block for input
            ChunkProcessEntry entry = processQueue.poll();
            // If null don't process this task this time around
            if (entry != null) {
                Sponge.getCauseStackManager().pushCause(pluginInstance.getChunkLoadCause());
                do {
                    Optional<World> optionalWorld = Sponge.getServer().getWorld(UUID.fromString(entry.getChunkEntry().getWorldId()));
                    // Only load chunk if world is present
                    if (optionalWorld.isPresent()) {
                        final World world = optionalWorld.get();
                        // Create final var for lambda statement
                        final ChunkProcessEntry entry1 = entry;
                        // Load chunk asynchronously
                        if (pluginInstance.getConfig().shouldRespectWorldBorder()) {
                            final WorldBorder border = world.getWorldBorder();
                            if (!RequestQueue.containsPosition(entry.getChunkEntry().getKey(), border.getCenter(), border.getDiameter())) {
                                // Skip request because not in world border.
                                entry = processQueue.poll();
                                continue;
                            }
                        }
                        world.loadChunkAsync(entry.getChunkEntry().getKey(), !entry.getChunkExists()).thenAccept(chunkOptional->chunkOptional.ifPresent(chunk->{
                            entry1.getChunkEntry().getCallbackList().forEach(chunkRunnable->chunkRunnable.run(chunk));
                        }));
                    }
                    // Get next item
                    entry = processQueue.poll();
                    // Only continue if item is not null and we still have time during this tick to process more chunks
                } while (entry != null && (System.currentTimeMillis() - startTime) < processTime);
                Sponge.getCauseStackManager().popCause();
            }
        }).intervalTicks(pluginInstance.getConfig().getTickInterval()).name("GlM Chunk Generation Task").submit(pluginInstance);
    }

    /**
     * Stop all processes of this class related to chunk processing.
     */
    public static void stop() {
        // Return because we can not load chunks. And nothing was started in the first place.
        if (!pluginInstance.getConfig().canLoadWorld()) {
            return;
        }
        // Stop taking new requests form clients
        process = false;
        // Stop client processor thread
        queueExecutor.interrupt();
        // Cancel sponge task
        generationTask.cancel();
    }

    /**
     * Attempts to create a new requests for chunk generation or loading. If a request all ready exists, the callback
     * will be added to the list for processing for the requested chunk.
     *
     * @param chunkPosition the position of the chunk to load or generate.
     * @param worldId       the {@link UUID} of the world in string form.
     * @param inetAddress   the address to callback when done.
     * @param callback      the {@link ChunkRunnable} to call when the chunk is done loading or generating.
     * @return true if new entry was created or callback was added to the list, false otherwise.
     */
    public static boolean createOrAddToQueue(@Nonnull final Vector3i chunkPosition, @Nonnull final String worldId,
                                             @Nonnull final InetSocketAddress inetAddress, @Nonnull final ChunkRunnable callback) {
        // Return because we can not load chunks. And chances are the executor thread is not running.
        if (!pluginInstance.getConfig().canLoadWorld()) {
            return false;
        }
        ChunkEntry item = null;
        // Check to make sure we don't add the same client multiple times
        for (ChunkEntry entry : checkQueue) {
            // Check if queue has a entry for the given chunk position
            if (entry.getKey().equals(chunkPosition)) {
                if (entry.getInetAddress().equals(inetAddress)) {
                    return false;
                }
                item = entry;
                break;
            }
        }
        // Check to make sure we don't add the same client multiple times
        for (ChunkProcessEntry entry : processQueue) {
            if (entry.getChunkEntry().getKey().equals(chunkPosition) &&
                    entry.getChunkEntry().getInetAddress().equals(inetAddress)) {
                return false;
            }
        }
        // If an entry does not exists create it, otherwise add callback to list for entry
        if (item == null) {
            return checkQueue.offer(new ChunkEntry(chunkPosition, worldId, callback, inetAddress));
        } else {
            return item.getCallbackList().add(callback);
        }
    }

    /**
     * Checks if a position and a diameter contain a position.
     *
     * @param position the position to check.
     * @param center   the center of the radius.
     * @param diameter the diameter of the area.
     * @return true if a position and a diameter contain a position, false otherwise.
     */
    public static boolean containsPosition(@Nonnull final Vector3i position, @Nonnull final Vector3d center, double diameter) {
        double rad = diameter / 2.0d;
        return position.getX() >= center.getX() - rad && position.getX() <= center.getX() + rad &&
                position.getZ() >= center.getZ() - rad && position.getZ() <= center.getZ() + rad;
    }

    /**
     * @return the current size of the queue.
     */
    public static int getCurrentQueueSize() {
        return processQueue.size();
    }
}
