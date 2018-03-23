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
package net.reallifegames.glm.sponge.eventlisteners;

import net.reallifegames.glm.sponge.GlMap;
import net.reallifegames.glm.sponge.WorldModuleSponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.plugin.PluginContainer;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Updates the cache when a chunk is loaded.
 *
 * @author Tyler Bucher
 */
public class ChunkLoadListener extends CoreListener {

    /**
     * Constructs a new {@link CoreListener}.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    ChunkLoadListener(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Listener
    public void onLoad(@Nonnull final LoadChunkEvent event) {
        final Optional<PluginContainer> optionalPluginContainer = event.getCause().first(PluginContainer.class);
        if (optionalPluginContainer.isPresent()) {
            if (!optionalPluginContainer.get().getId().equals(pluginInstance.getPluginContainer().getId())) {
                WorldModuleSponge.updateCache(event.getTargetChunk().getWorld(), event.getTargetChunk(), pluginInstance, true);
            }
        } else {
            WorldModuleSponge.updateCache(event.getTargetChunk().getWorld(), event.getTargetChunk(), pluginInstance, true);
        }
    }
}
