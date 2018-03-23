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
import net.reallifegames.glm.sponge.RequestQueue;
import net.reallifegames.glm.sponge.WorldModuleSponge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Returns internal info about the plugin.
 *
 * @author Tyler Bucher
 */
public class DebugCommand extends CoreCommand {

    /**
     * Constructs a new {@link CoreCommand}.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    DebugCommand(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull final CommandSource src, @Nonnull final CommandContext args) {
        try (final Connection connection = pluginInstance.getDataSource().getConnection()) {
            // General information
            final Text.Builder builder = Text.builder().append(Text.of(TextColors.GOLD, "=============== GLM DEBUG INFO ===============")).append(Text.NEW_LINE)
                    .append(Text.of(TextColors.GREEN, "GLM Version: " + pluginInstance.getPluginContainer().getVersion())).append(Text.NEW_LINE)
                    .append(Text.of(TextColors.GOLD, TextStyles.UNDERLINE, "Available Worlds:")).append(Text.NEW_LINE);
            // Available worlds
            this.pluginInstance.getConfig().getWorldList().forEach(worldName->
                    Sponge.getServer().getWorld(worldName).ifPresent(world->
                            builder.append(Text.of(TextColors.GRAY, "    " + worldName + " : " + world.getUniqueId().toString())).append(Text.NEW_LINE)
                    )
            );
            // Cache information
            builder.append(Text.of(TextColors.GOLD, TextStyles.UNDERLINE, "Cache Information:")).append(Text.NEW_LINE)
                    .append(Text.of(TextColors.GREEN, "    Total Cache Size: " + WorldModuleSponge.getTotalCacheSize())).append(Text.NEW_LINE)
                    .append(Text.of(TextColors.GREEN, "    Total Sql Size: " + SqlModule.countTotalRows(connection))).append(Text.NEW_LINE)
                    .append(Text.of(TextColors.GREEN, "    Chunk queue Size: " + RequestQueue.getCurrentQueueSize())).append(Text.NEW_LINE)
                    .append(Text.of(TextColors.GREEN, "    World Cache:")).append(Text.NEW_LINE);
            // Per world cache information
            this.pluginInstance.getConfig().getWorldList().forEach(worldName->
                    Sponge.getServer().getWorld(worldName).ifPresent(world->
                            builder.append(Text.of(TextColors.GRAY, "        " + worldName + " : " +
                                    WorldModuleSponge.getCacheSize(world.getUniqueId().toString()))).append(Text.NEW_LINE)
                    )
            );
            builder.append(Text.of(TextColors.GOLD, "    Sql Cache:")).append(Text.NEW_LINE);
            // Per world sql information
            this.pluginInstance.getConfig().getWorldList().forEach(worldName->
                    Sponge.getServer().getWorld(worldName).ifPresent(world->{
                        try {
                            builder.append(Text.of(TextColors.GRAY, "        " + worldName + " : " +
                                    SqlModule.countRowsForWorld(connection, world.getUniqueId().toString())))
                                    .append(Text.NEW_LINE);
                        } catch (SQLException e) {
                            pluginInstance.getLogger().error("Sql error", e);
                        }
                    })
            );
            // Web socket server
            builder.append(Text.of(TextColors.GOLD, TextStyles.UNDERLINE, "WebSocket server:")).append(Text.NEW_LINE)
                    .append(Text.of(TextColors.GREEN, "    Current connections: " + pluginInstance.getBaseGlmServer().getCurrentConnections()))
                    .append(Text.NEW_LINE);
            src.sendMessage(builder.build());
            return CommandResult.success();
        } catch (SQLException e) {
            pluginInstance.getLogger().error("Sql error", e);
            src.sendMessage(Text.of(TextColors.RED, "Could not connect to database."));
            return CommandResult.empty();
        }
    }
}
