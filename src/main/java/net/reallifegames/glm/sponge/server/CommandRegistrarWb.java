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

import com.fasterxml.jackson.databind.JsonNode;
import net.reallifegames.glm.module.SqlModule;
import net.reallifegames.glm.server.CommandRegistrar;
import net.reallifegames.glm.sponge.GlMap;
import org.java_websocket.WebSocket;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Maintains control over all gl server commands.
 *
 * @author Tyler Bucher
 */
public class CommandRegistrarWb extends CommandRegistrar {

    /**
     * Static message for when a client gets kicked.
     */
    @Nonnull
    private static final String BAN_MESSAGE = "{\"cmd\": \"BAN\", \"data\": {\"message\": \"You called commands too fast and did not respect the command interval's\"}}";

    /**
     * The {@link GlMap} instance.
     */
    @Nonnull
    private final GlMap pluginInstance;

    /**
     * The map of commands to command handler objects.
     */
    @Nonnull
    private final ConcurrentMap<InetSocketAddress, Integer> warnMap;

    /**
     * Creates a new gl server command registrar.
     *
     * @param pluginInstance The plugin instance to get data from.
     */
    CommandRegistrarWb(@Nonnull final GlMap pluginInstance) {
        super();
        this.pluginInstance = pluginInstance;
        warnMap = new ConcurrentHashMap<>();
    }

    @Override
    protected void punishClient(@Nonnull final WebSocket connection, @Nonnull final String command, @Nonnull final JsonNode commandNode) {
        Integer strike = warnMap.getOrDefault(connection.getRemoteSocketAddress(), 0);
        warnMap.put(connection.getRemoteSocketAddress(), ++strike);
        if (strike > pluginInstance.getConfig().getMaxWarns()) {
            warnMap.put(connection.getRemoteSocketAddress(), 0);
            // https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent
            connection.close(1008, CommandRegistrarWb.BAN_MESSAGE);
            // Add ban to sql table
            try (Connection databaseConnection = pluginInstance.getDataSource().getConnection()) {
                final String hostNameString = connection.getRemoteSocketAddress().getHostString();
                String uuid = pluginInstance.getClientMapIdMap().get(hostNameString);
                if(uuid == null) {
                    uuid = pluginInstance.getConfig().getUuidList().get(0);
                }
                SqlModule.insertBan(databaseConnection, hostNameString, uuid);
            } catch (SQLException e) {
                pluginInstance.getLogger().error("Error getting sql database: ", e);
            }
        }
        connection.send("{\"cmd\": \"commandInterval\", \"data\": {\"command\": \"" + command + "\", \"interval\": \"" + this.commandMap.get(command).getInterval() + "\"}}");
    }
}
