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

import net.reallifegames.glm.module.SqlModule;
import net.reallifegames.glm.sponge.GlMap;
import net.reallifegames.glm.sponge.WorldModuleSponge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Attempts to purge chunks from the cache.
 *
 * @author Tyler Bucher
 */
public class PurgeCommand extends CoreCommand {

    /**
     * Constructs a new {@link CoreCommand}.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    PurgeCommand(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull final CommandSource src, @Nonnull final CommandContext args) throws CommandException {
        try (final Connection connection = pluginInstance.getDataSource().getConnection()) {
            // Get world
            final WorldProperties worldProperties = castArgument(args, "world", WorldProperties.class);
            // Get positions from args
            final int x1 = castArgument(args, "x1", Integer.class);
            final int z1 = castArgument(args, "z1", Integer.class);
            final int x2 = castArgument(args, "x2", Integer.class);
            final int z2 = castArgument(args, "z2", Integer.class);
            // Purge chunks if world is valid
            if (pluginInstance.getConfig().getWorldList().contains(worldProperties.getWorldName())) {
                final Optional<World> optionalWorld = Sponge.getServer().getWorld(worldProperties.getUniqueId());
                if (optionalWorld.isPresent()) {
                    final World world = optionalWorld.get();
                    final String worldId = world.getUniqueId().toString();
                    // Remove chunks from cache
                    WorldModuleSponge.purgeCache(worldId, x1, z1, x2, z2);
                    // Purge from sql
                    SqlModule.removeChunks(connection, worldId, x1, z1, x2, z2);
                    src.sendMessage(Text.of(TextColors.GREEN, "Purged chunks from the cache"));
                    return CommandResult.success();
                } else {
                    src.sendMessage(Text.of(TextColors.RED, "Invalid world: " + worldProperties.getWorldName()));
                }
            } else {
                src.sendMessage(Text.of(TextColors.RED, "World is not available to the plugin: " + worldProperties.getWorldName()));
            }
            return CommandResult.empty();
        } catch (SQLException e) {
            pluginInstance.getLogger().error("Sql error", e);
            src.sendMessage(Text.of(TextColors.RED, "Could not connect to database."));
            return CommandResult.empty();
        }
    }
}
