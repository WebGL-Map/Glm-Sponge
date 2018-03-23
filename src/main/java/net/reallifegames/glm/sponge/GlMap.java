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
package net.reallifegames.glm.sponge;

import net.reallifegames.glm.module.SqlModule;
import net.reallifegames.glm.module.SslModule;
import net.reallifegames.glm.server.GlmServer;
import net.reallifegames.glm.sponge.events.GlmRegisterCommand;
import net.reallifegames.glm.sponge.server.BaseGlmServer;
import net.reallifegames.glm.sponge.commands.CommandRegistrar;
import net.reallifegames.glm.sponge.eventlisteners.EventRegistrar;
import net.reallifegames.glm.sponge.server.command.GetChunksForPositions;
import net.reallifegames.glm.sponge.server.command.GetPlayers;
import net.reallifegames.glm.sponge.server.command.GetWorlds;
import net.reallifegames.glm.sponge.server.command.Init;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.slf4j.Logger;
import org.spongepowered.api.GameState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.sql.SqlService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main plugin class that sponge will load. You should only attempt to inject dependencies into this class.
 *
 * @author Tyler Bucher
 * @see <a href="https://docs.spongepowered.org/stable/en/plugin/injection.html#injection-examples">
 * Dependency Injection</a>
 */
@Plugin (id = "glm", name = "GLM", version = "1.0.0", description = "Minecraft WebGL Map.")
public final class GlMap {

    /**
     * Loads the config from the file. call {@link ConfigurationLoader#load()} to refresh the
     * current config.
     */
    @Inject
    @DefaultConfig (sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configLoader;

    /**
     * Provides the full config path (weather it be there or not).
     * Example: C:/Server/config/[plugin]/config.conf or C:/Server/config/config.conf
     */
    @Inject
    @DefaultConfig (sharedRoot = false)
    private Path defaultConfig;

    /**
     * Provides the full config dir path.
     * Example: C:/Server/config/[plugin]/ or C:/Server/config/
     */
    @Inject
    @ConfigDir (sharedRoot = false)
    private Path privateConfigDir;

    /**
     * A wrapper around a class marked with an Plugin annotation to retrieve information
     * from the annotation for easier use. Can be used to get assets or the plugin instance.
     */
    @Inject
    private PluginContainer pluginContainer;

    /**
     * Injected console logger for the plugin to use.
     */
    @Inject
    private Logger logger;

    /**
     * Config wrapper for the base sponge {@link CommentedConfigurationNode}.
     */
    @Nonnull
    private final ConfigurationFile config = ConfigurationFile.getInstance();

    /**
     * Type list for all blocks returned by the game.
     */
    private List<BlockState> stateList;

    /**
     * The server to respond to client requests with.
     */
    private GlmServer baseGlmServer;

    /**
     * The sql service for this plugin.
     */
    private SqlService sqlService;

    /**
     * The chunk load cause for this plugin.
     */
    private Cause chunkloadCause;

    /**
     * The {@link GamePreInitializationEvent} is triggered. During this state, the plugin gets ready for
     * initialization. Access to a default logger instance and access to information regarding
     * preferred configuration file locations is available.
     *
     * @param event represents {@link GameState#PRE_INITIALIZATION} event.
     */
    @Listener
    public void onGamePreInitialization(@Nonnull final GamePreInitializationEvent event) {
        // Initialize config
        config.initialize(this);
        // Load config values
        config.loadConfig();
        // Initialize sql
        SqlModule.init(config.getDatabaseTablePrefix());
    }

    /**
     * The {@link GameInitializationEvent} is triggered. During this state, the plugin should finish any work
     * needed in order to be functional. Global event handlers should get registered in this stage.
     *
     * @param event represents {@link GameState#INITIALIZATION} event.
     */
    @Listener
    public void onGameInitialization(@Nonnull final GameInitializationEvent event) {
        // If config is not loaded return so no NPE
        if (!config.isLoaded()) {
            return;
        }
        CommandRegistrar.register(this);
        EventRegistrar.register(this);
    }

    /**
     * The {@link GamePostInitializationEvent} is triggered. During this state, the plugin should finish any work
     * needed in order to be functional.
     *
     * @param event represents {@link GameState#POST_INITIALIZATION} event.
     */
    @Listener
    public void onPostGameInitialization(@Nonnull final GamePostInitializationEvent event) {
        chunkloadCause = Cause.builder().append(this.getPluginContainer()).build(EventContext.builder().build());
        // If config is not loaded return so no NPE
        if (!config.isLoaded()) {
            return;
        }
        // Create table if not present
        try (final Connection connection = getDataSource().getConnection()) {
            SqlModule.createTable(connection);
        } catch (SQLException e) {
            logger.error("Error creating table in sql server: ", e);
        }
        // Create and sort all of the block types
        stateList = new ArrayList<>(Sponge.getGame().getRegistry().getAllOf(BlockState.class));
        // Init chunk load queue
        RequestQueue.init(this);
        // Start the GL server
        baseGlmServer = new BaseGlmServer(new InetSocketAddress(config.getGlServerAddress(), config.getGlServerPort()), this);
        if (config.useSsl()) {
            try {
                baseGlmServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(SslModule.getSSLContextFromKeystore()));
            } catch (IllegalStateException e) {
                logger.warn("Unable to make the WebSocket server ssl secure", e);
            }
        }
        if (Sponge.getEventManager().post(new GlmRegisterCommand(Cause.of(EventContext.builder().add(
                EventContextKeys.PLUGIN, this.getPluginContainer()).build(), this), baseGlmServer))) {
            // Register available commands
            baseGlmServer.getRegistrar().registerCommand("init", new Init(this));
            baseGlmServer.getRegistrar().registerCommand("getWorlds", new GetWorlds(this));
            baseGlmServer.getRegistrar().registerCommand("getPlayers", new GetPlayers(this));
            baseGlmServer.getRegistrar().registerCommand("getChunksForPositions", new GetChunksForPositions(this));
        }
        baseGlmServer.start();
    }

    /**
     * The {@link GameStoppingEvent} is triggered. During this state, the plugin should finish any work
     * needed in order to shutdown.
     *
     * @param event represents {@link GameState#GAME_STOPPING} event.
     */
    @Listener
    public void onGameStoppingEvent(@Nonnull final GameStoppingEvent event) {
        // If config is not loaded return so no NPE
        if (!config.isLoaded()) {
            return;
        }
        // Stop the server
        try {
            baseGlmServer.stop();
            baseGlmServer = null;
        } catch (IOException | InterruptedException e) {
            logger.error("Error stopping the gl web socket server: ", e);
        }
        // stop request queue
        RequestQueue.stop();
    }

    /**
     * @return the Injected {@link ConfigurationLoader} for this plugin.
     */
    @Nonnull
    public ConfigurationLoader<CommentedConfigurationNode> getConfigLoader() {
        return configLoader;
    }

    /**
     * @return the Injected configuration path for this plugin.
     */
    @Nonnull
    public Path getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * @return the Injected configuration directory for this plugin.
     */
    @Nonnull
    public Path getPrivateConfigDir() {
        return privateConfigDir;
    }

    /**
     * @return the Injected {@link PluginContainer} for this plugin.
     */
    @Nonnull
    public PluginContainer getPluginContainer() {
        return pluginContainer;
    }

    /**
     * @return the Injected {@link ConfigurationFile} for this plugin.
     */
    @Nonnull
    public ConfigurationFile getConfig() {
        return config;
    }

    /**
     * @return the Injected {@link Logger} for this plugin.
     */
    @Nonnull
    public Logger getLogger() {
        return logger;
    }

    /**
     * @return the list of states for the game.
     */
    @Nonnull
    public List<BlockState> getStateList() {
        return stateList;
    }

    /**
     * @return the database source for this plugin.
     *
     * @throws SQLException if a connection to the database could not be established.
     */
    @Nonnull
    public DataSource getDataSource() throws SQLException {
        if (sqlService == null) {
            Sponge.getServiceManager().provide(SqlService.class).ifPresent(sqlService1->sqlService = sqlService1);
        }
        return sqlService.getDataSource(this.config.getJdbcDatabaseUrl());
    }

    /**
     * @return the WebSocket server.
     */
    @Nonnull
    public GlmServer getBaseGlmServer() {
        return baseGlmServer;
    }

    /**
     * @return a cause for chunk loading which this plugin performs.
     */
    public Cause getChunkLoadCause() {
        return chunkloadCause;
    }
}
