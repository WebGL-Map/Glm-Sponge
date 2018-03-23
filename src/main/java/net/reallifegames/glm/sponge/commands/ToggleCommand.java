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
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nonnull;

/**
 * Toggles a players visibility on the web map.
 *
 * @author Tyler Bucher
 */
public class ToggleCommand extends CoreCommand {

    /**
     * Constructs a new {@link CoreCommand}.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    ToggleCommand(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Override
    @Nonnull
    public CommandResult execute(@Nonnull final CommandSource src, @Nonnull final CommandContext args) {
        // Make sure CommandSource is a user
        if (!(src instanceof Player)) {
            src.sendMessage(Text.of(TextColors.RED, "You need to be a player to use this command."));
            return CommandResult.empty();
        }
        // Get player
        final Player player = (Player) src;
        if (player.hasPermission("glm.map.hide")) {
            // Remove permission from player
            player.getSubjectData().setPermission(player.getActiveContexts(), "glm.map.hide", Tristate.UNDEFINED);
            src.sendMessage(Text.of(TextColors.GREEN, "You are now visible to the web map."));
        } else {
            // Add permission to player
            player.getSubjectData().setPermission(player.getActiveContexts(), "glm.map.hide", Tristate.TRUE);
            src.sendMessage(Text.of(TextColors.GREEN, "You are now hidden from the web map."));
        }
        return CommandResult.success();
    }
}
