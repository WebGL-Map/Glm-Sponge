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
package net.reallifegames.glm.sponge.commands;

import net.reallifegames.glm.sponge.GlMap;
import net.reallifegames.glm.sponge.WorldModuleSponge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Attempts to load chunks and add them to the cache.
 *
 * @author Tyler Bucher
 */
public class CreateCommand extends CoreCommand {

    /**
     * Constructs a new {@link CoreCommand}.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    CreateCommand(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull final CommandSource src, @Nonnull final CommandContext args) throws CommandException {
        // Get world
        final WorldProperties worldProperties = castArgument(args, "world", WorldProperties.class);
        // Get positions from args
        final int x1 = castArgument(args, "x1", Integer.class);
        final int z1 = castArgument(args, "z1", Integer.class);
        final int x2 = castArgument(args, "x2", Integer.class);
        final int z2 = castArgument(args, "z2", Integer.class);
        // force world and chunks
        final boolean force = castArgument(args, "force", Boolean.class);
        // load chunks
        if (pluginInstance.getConfig().getWorldList().contains(worldProperties.getWorldName())) {
            // Create chunks and bypass all checks
            final Optional<World> optionalWorld = Sponge.getServer().getWorld(worldProperties.getUniqueId());
            if (optionalWorld.isPresent()) {
                final World world = optionalWorld.get();
                int total = 0;
                Sponge.getCauseStackManager().pushCause(pluginInstance.getChunkLoadCause());
                for (int i = x1; i < x2; i++) {
                    for (int j = z1; j < z2; j++) {
                        final Optional<Chunk> optionalChunk = world.loadChunk(i, 0, j, force);
                        if (optionalChunk.isPresent()) {
                            total++;
                            WorldModuleSponge.updateCache(world, optionalChunk.get(), pluginInstance, false);
                        }
                    }
                }
                Sponge.getCauseStackManager().popCause();
                src.sendMessage(Text.of(TextColors.GREEN, (force ? "Generated " : "Loaded ") + total + " chunks"));
                return CommandResult.builder().successCount(total).affectedBlocks(total * 256).build();
            } else {
                src.sendMessage(Text.of(TextColors.RED, "Invalid world: " + worldProperties.getWorldName()));
            }
        } else {
            src.sendMessage(Text.of(TextColors.RED, "World is not available to the plugin: " + worldProperties.getWorldName()));
        }
        return CommandResult.empty();
    }
}
