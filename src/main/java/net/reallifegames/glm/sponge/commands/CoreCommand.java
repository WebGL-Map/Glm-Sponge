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
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

import javax.annotation.Nonnull;

/**
 * An abstract class for all commands to extend.
 *
 * @author Tyler Bucher
 */
abstract class CoreCommand implements CommandExecutor {

    /**
     * The {@link GlMap} instance.
     */
    @Nonnull
    protected final GlMap pluginInstance;

    /**
     * Constructs a new {@link CoreCommand}.
     *
     * @param pluginInstance the {@link GlMap} instance.
     */
    CoreCommand(@Nonnull final GlMap pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    /**
     * Fetch an argument from the provided {@link CommandContext} and cast it to the provided {@link Class}<{@link T}>.
     *
     * @param args  the arguments that should contain the argument to cast.
     * @param key   they key to use to extract an argument from the {@link CommandContext}.
     * @param clazz the class to cast the key to.
     * @param <T>   the type of the return value as specified by the {@code clazz} parameter.
     * @return the value extracted from the {@link CommandContext} cast to the requested type.
     *
     * @throws CommandException if no arguments with the given {@code key} are present in the {@link CommandContext}.
     */
    @Nonnull
    <T> T castArgument(@Nonnull final CommandContext args, @Nonnull final String key, @Nonnull final Class<T> clazz) throws CommandException {
        return args.getOne(Text.of(key))
                .map(clazz::cast)
                .orElseThrow(()->new CommandException(Text.of("'" + key + "' did not match any provided arguments")));
    }
}
