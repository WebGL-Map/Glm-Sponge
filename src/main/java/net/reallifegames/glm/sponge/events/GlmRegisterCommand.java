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
package net.reallifegames.glm.sponge.events;

import net.reallifegames.glm.server.GlmServer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

import javax.annotation.Nonnull;

/**
 * Fires an event when the glm web socket server is registering its commands.
 *
 * @author Tyler Bucher
 */
public class GlmRegisterCommand extends AbstractEvent implements Cancellable {

    /**
     * States if this event is canceled.
     */
    private boolean isCancelled;

    /**
     * The cause which triggered this event.
     */
    @Nonnull
    private final Cause cause;

    /**
     * The Glm server to interact with.
     */
    @Nonnull
    private final GlmServer glmServer;

    /**
     * Creates a new GlmRegisterCommand event instance.
     *
     * @param cause     the cause which triggered this event.
     * @param glmServer the Glm server to interact with.
     */
    public GlmRegisterCommand(@Nonnull final Cause cause, @Nonnull final GlmServer glmServer) {
        this.isCancelled = false;
        this.cause = cause;
        this.glmServer = glmServer;
    }

    /**
     * @return the Glm server to interact with.
     */
    @Nonnull
    public GlmServer getGlmServer() {
        return glmServer;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override
    public Object getSource() {
        return this.cause.root();
    }

    @Override
    public EventContext getContext() {
        return this.cause.getContext();
    }
}
