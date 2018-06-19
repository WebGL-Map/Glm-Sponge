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
package net.reallifegames.glm.sponge.server.command;

import com.fasterxml.jackson.databind.JsonNode;
import net.reallifegames.glm.sponge.GlMap;
import net.reallifegames.glm.sponge.server.GlmServerCommand;
import org.java_websocket.WebSocket;

import javax.annotation.Nonnull;

/**
 * Sets the current clients uuid.
 *
 * @author Tyler Bucher
 */
public class SetClientUuid extends GlmServerCommand {

    /**
     * Static message for when a client gets kicked.
     */
    @Nonnull
    private static final String KICK_MESSAGE = "{\"cmd\": \"kick\", \"data\": {\"message\": \"Invalid map id specified.\"}}";

    /**
     * Creates a new Glm server command.
     *
     * @param pluginInstance the plugin instance to get data from.
     */
    public SetClientUuid(@Nonnull final GlMap pluginInstance) {
        super(pluginInstance);
    }

    @Override
    public void handle(@Nonnull final WebSocket connection, @Nonnull final JsonNode commandNode) {
        // Make sure world is is present
        final JsonNode dataNode = commandNode.get("data");
        if (dataNode == null) {
            connection.send("{\"error\": \"Incomplete request\"}");
            return;
        }
        // Make sure world is is present
        final JsonNode uuidNode = commandNode.get("data").get("uuid");
        if (uuidNode == null) {
            connection.send("{\"error\": \"Incomplete request\"}");
            return;
        }
        if (uuidNode.asText().length() != 36) {
            connection.send("{\"error\": \"Incomplete request\"}");
            return;
        }
        if (pluginInstance.getConfig().isCertifiedUuids()) {
            if (!pluginInstance.getConfig().getUuidList().contains(uuidNode.asText())) {
                connection.close(1008, SetClientUuid.KICK_MESSAGE);
                return;
            }
        }
        pluginInstance.getClientMapIdMap().put(connection.getRemoteSocketAddress().getHostString(), uuidNode.asText());
    }

    @Override
    public long getInterval() {
        return 0;
    }
}
