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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import javax.annotation.Nonnull;

/**
 * Register commands for the plugin.
 *
 * @author Tyler Bucher
 */
public final class CommandRegistrar {

    /**
     * Register commands for the {@link GlMap} plugin.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    public static void register(@Nonnull final GlMap pluginInstance) {
        // Url command
        final CommandSpec urlCommand = CommandSpec.builder()
                .description(Text.of("Gets the url of the GLM map"))
                .permission("glm.commands.url")
                .executor(new UrlCommand(pluginInstance))
                .build();
        // Purge command
        final CommandSpec createCommand = CommandSpec.builder()
                .arguments(GenericArguments.world(Text.of("world")),
                        GenericArguments.integer(Text.of("x1")),
                        GenericArguments.integer(Text.of("z1")),
                        GenericArguments.integer(Text.of("x2")),
                        GenericArguments.integer(Text.of("z2")),
                        GenericArguments.bool(Text.of("force")))
                .description(Text.of("Creates chunks and attempts to add them to the cache. Please note that " +
                        "this command could crash the server if you attempt to create too many chunks."))
                .permission("glm.commands.create")
                .executor(new CreateCommand(pluginInstance))
                .build();
        // Purge command
        final CommandSpec purgeCommand = CommandSpec.builder()
                .arguments(GenericArguments.world(Text.of("world")),
                        GenericArguments.integer(Text.of("x1")),
                        GenericArguments.integer(Text.of("z1")),
                        GenericArguments.integer(Text.of("x2")),
                        GenericArguments.integer(Text.of("z2")))
                .description(Text.of("Purges chunks from the cache"))
                .permission("glm.commands.purge")
                .executor(new PurgeCommand(pluginInstance))
                .build();
        // Toggle command
        final CommandSpec toggleCommand = CommandSpec.builder()
                .description(Text.of("Toggles a players visibility on the web map"))
                .permission("glm.commands.toggle")
                .executor(new ToggleCommand(pluginInstance))
                .build();
        // Debug command
        final CommandSpec debugCommand = CommandSpec.builder()
                .description(Text.of("Gets debug information for the GLM plugin"))
                .permission("glm.commands.debug")
                .executor(new DebugCommand(pluginInstance))
                .build();
        // Version command
        final CommandSpec versionCommand = CommandSpec.builder()
                .description(Text.of("Get the current version of the GLM plugin"))
                .permission("glm.commands.version")
                .executor(new VersionCommand(pluginInstance))
                .build();
        // Create base command
        final CommandSpec baseCommand = CommandSpec.builder()
                .child(urlCommand, "url")
                .child(createCommand, "create")
                .child(purgeCommand, "purge")
                .child(toggleCommand, "toggle")
                .child(debugCommand, "debug")
                .child(versionCommand, "version")
                .build();
        // Register commands
        final CommandManager commandManager = Sponge.getCommandManager();
        commandManager.register(pluginInstance, baseCommand, "glm");
    }
}
