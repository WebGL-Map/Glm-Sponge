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
package net.reallifegames.glm.sponge.server;

import net.reallifegames.glm.server.GlmServer;
import net.reallifegames.glm.sponge.GlMap;
import org.java_websocket.WebSocket;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;

/**
 * The server to talk to the gl map clients.
 *
 * @author Tyler Bucher
 */
public class BaseGlmServer extends GlmServer {

    /**
     * The {@link GlMap} instance.
     */
    @Nonnull
    private final GlMap pluginInstance;

    /**
     * Creates a new Gl server.
     *
     * @param address        The address to bind to.
     * @param pluginInstance The plugin instance to get data from.
     */
    public BaseGlmServer(@Nonnull final InetSocketAddress address, @Nonnull final GlMap pluginInstance) {
        super(address, new CommandRegistrarWk(pluginInstance));
        this.pluginInstance = pluginInstance;
    }

    @Override
    public void onError(@Nonnull final WebSocket conn, @Nonnull final Exception ex) {
        pluginInstance.getLogger().error("WebSocket error", conn, ex);
    }

    @Override
    public void onStart() {
        pluginInstance.getLogger().info("Starting WebSocket server on " + this.getAddress().getHostString() + ":" + this.getAddress().getPort());
    }
}
