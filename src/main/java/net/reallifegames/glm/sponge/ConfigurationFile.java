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

import com.google.common.reflect.TypeToken;
import net.reallifegames.glm.api.GlmChunk;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.asset.Asset;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This is a wrapper for the base sponge {@link CommentedConfigurationNode}. We do this in-case
 * we change a node configuration in the config, so we do not need to change it in 10+ places.
 *
 * @author Tyler Bucher
 */
public class ConfigurationFile {

    /**
     * Holds the singleton instance for {@link ConfigurationFile}.
     */
    private static final class SingletonHolder {

        /**
         * The {@link ConfigurationFile} singleton instance.
         */
        @Nonnull
        private static final ConfigurationFile INSTANCE = new ConfigurationFile();
    }

    /**
     * The plugin instance.
     */
    private GlMap pluginInstance;

    /**
     * The actual configuration file given from sponge.
     */
    private CommentedConfigurationNode config;

    /**
     * States if the config was loaded.
     */
    private boolean isLoaded;

    /**
     * The name of the server to send to the web map.
     */
    private String serverName;

    /**
     * The list of server to network together.
     */
    private List<String> serverList;

    /**
     * The port to start the gl map server on.
     */
    private int port;

    /**
     * The address to start the gl map server on.
     */
    private String address;

    /**
     * True if the web socket server should use ssl.
     */
    private boolean useSsl;

    /**
     * The url of the map.
     */
    private String url;

    /**
     * The number of warns a client can receive before it is kicked off the server for not respecting the command interval.
     */
    private int maxWarns;

    /**
     * The amount of time in milliseconds which a {@link GlmChunk} can live in the cache.
     */
    private long chunkCacheLifetime;

    /**
     * The amount of time in between calls for player information.
     */
    private long playerRequestTime;

    /**
     * The amount of time in between calls for general commands like 'init'.
     */
    private long generalCommandInterval;

    /**
     * The percent of a tick to consume processing chunks. Values range from 0.1 to 1.0.
     */
    private float totalTickPercentage;

    /**
     * The amount of ticks to wait until the next iteration of chunk processing.
     */
    private int tickInterval;

    /**
     * The name of the default world for the map to load.
     */
    private String defaultWorld;

    /**
     * The list of available worlds to render.
     */
    private List<String> worldList;

    /**
     * True if this plugin is allowed to generate terrain false otherwise.
     */
    private boolean generateWorld;

    /**
     * True if this plugin is allowed to load terrain false otherwise.
     */
    private boolean loadWorld;

    /**
     * True if this plugin should respect the world border when generating terrain.
     */
    private boolean respectWorldBorder;

    /**
     * True if the size of the cache is Limited.
     */
    private boolean limitCache;

    /**
     * Maximum number of chunks (per world) allowed in the cache at one time.
     */
    private int maximumChunksInCache;

    /**
     * The jdbc url for chunk storage.
     */
    private String jdbcDatabaseUrl;

    /**
     * The prefix for database tables.
     */
    private String databaseTablePrefix; //todo get two new vars from config and use thoes for the new glm server command

    /**
     * Should the plugin only allow certified map uuid's to connect.
     */
    private boolean certifiedUuids;

    /**
     * A list of certified uuid's for client maps. There should be at least one value in this list.
     */
    private List<String> uuidList;

    /**
     * Creates a new {@link ConfigurationFile} object.
     */
    private ConfigurationFile() {
    }

    /**
     * @return the {@link ConfigurationFile} singleton instance.
     */
    @Nonnull
    public static ConfigurationFile getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initializes the Config object for the main plugin object. Note that you should only call this once per server lifecycle.
     *
     * @param pluginInstance the {@link GlMap} plugin instance.
     */
    public void initialize(@Nonnull final GlMap pluginInstance) {
        if (this.pluginInstance != null) {
            throw new IllegalStateException("ConfigurationFile attempted to be initialized twice");
        }
        this.pluginInstance = pluginInstance;
    }

    /**
     * Loads or reloads the CommentedConfigurationNode object.
     */
    public void loadConfig() {
        throwIfNotInitialized();
        try {
            if (Files.notExists(pluginInstance.getDefaultConfig())) {
                final Optional<Asset> optionalAsset = pluginInstance.getPluginContainer().getAsset("glm.conf");
                if (optionalAsset.isPresent()) {
                    optionalAsset.get().copyToFile(pluginInstance.getDefaultConfig(), true, true);
                }
            }
        } catch (IOException e) {
            pluginInstance.getLogger().warn("Unable to create default config file", e);
            isLoaded = false;
        }
        // Try to load the config
        try {
            config = pluginInstance.getConfigLoader().load();

            serverName = config.getNode("glm", "serverName").getString();
            try {
                serverList = config.getNode("glm", "serverList").getList(TypeToken.of(String.class));
            } catch (ObjectMappingException e) {
                pluginInstance.getLogger().error("Error parsing serverList config option", e);
                serverList = new ArrayList<>();
            }
            port = config.getNode("glm", "port").getInt();
            address = config.getNode("glm", "address").getString();
            useSsl = config.getNode("glm", "useSsl").getBoolean();
            url = config.getNode("glm", "url").getString();
            maxWarns = config.getNode("glm", "maxWarns").getInt();
            chunkCacheLifetime = config.getNode("glm", "chunkCacheLifetime").getLong();
            playerRequestTime = config.getNode("glm", "playerRequestTime").getLong();
            generalCommandInterval = config.getNode("glm", "generalCommandInterval").getLong();
            totalTickPercentage = config.getNode("glm", "totalTickPercentage").getFloat();
            tickInterval = config.getNode("glm", "tickInterval").getInt();
            defaultWorld = config.getNode("glm", "defaultWorld").getString();
            try {
                worldList = config.getNode("glm", "worldList").getList(TypeToken.of(String.class));
            } catch (ObjectMappingException e) {
                pluginInstance.getLogger().error("Error parsing worldList config option", e);
                worldList = new ArrayList<>();
            }
            generateWorld = config.getNode("glm", "generateWorld").getBoolean();
            loadWorld = config.getNode("glm", "loadWorld").getBoolean();
            respectWorldBorder = config.getNode("glm", "respectWorldBorder").getBoolean();
            limitCache = config.getNode("glm", "limitCache").getBoolean();
            maximumChunksInCache = config.getNode("glm", "maximumChunksInCache").getInt();
            jdbcDatabaseUrl = config.getNode("glm", "jdbcDatabaseUrl").getString();
            databaseTablePrefix = config.getNode("glm", "databaseTablePrefix").getString();
            certifiedUuids = config.getNode("glm", "certifiedUuids").getBoolean();
            try {
                uuidList = config.getNode("glm", "uuidList").getList(TypeToken.of(String.class));
            } catch (ObjectMappingException e) {
                pluginInstance.getLogger().error("Error parsing worldList config option", e);
                uuidList = new ArrayList<>();
            }
        } catch (IOException e) {
            pluginInstance.getLogger().warn("Unable to load config", e);
            isLoaded = false;
        }
        isLoaded = true;
    }

    /**
     * @throws IllegalStateException if the {@link GlMap} instance is not set.
     */
    private void throwIfNotInitialized() {
        if (pluginInstance == null) {
            throw new IllegalStateException("ConfigurationFile not initialized");
        }
    }

    /**
     * @return the internal sponge {@link CommentedConfigurationNode}.
     */
    @Nonnull
    public CommentedConfigurationNode getConfig() {
        return config;
    }

    /**
     * @return true if the config was loaded false otherwise.
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * @return the address to start the gl map server on.
     */
    @Nonnull
    public String getServerName() {
        return serverName;
    }

    /**
     * @return the list of available worlds to render.
     */
    @Nonnull
    public List<String> getServerList() {
        return serverList;
    }

    /**
     * @return the port to start the gl map server on.
     */
    public int getGlServerPort() {
        return port;
    }

    /**
     * @return the address to start the gl map server on.
     */
    @Nonnull
    public String getGlServerAddress() {
        return address;
    }

    /**
     * @return true if the web socket server should use ssl.
     */
    public boolean useSsl() {
        return useSsl;
    }

    /**
     * @return the url of the map.
     */
    @Nonnull
    public String getUrl() {
        return url;
    }

    /**
     * @return the number of warns a client can receive before it is kicked off the server for not respecting the
     * command interval.
     */
    public long getMaxWarns() {
        return chunkCacheLifetime;
    }

    /**
     * @return the amount of time in milliseconds which a {@link GlmChunk} can live in the cache.
     */
    public long getGlChunkCacheLifetime() {
        return chunkCacheLifetime;
    }

    /**
     * @return the amount of time in between calls for player information.
     */
    public long getPlayerRequestTime() {
        return playerRequestTime;
    }

    /**
     * @return the amount of time in between calls for general commands like 'init'.
     */
    public long getGeneralCommandInterval() {
        return generalCommandInterval;
    }

    /**
     * @return the percent of a tick to consume processing chunks. Values range from 0.1 to 1.0.
     */
    public float getTotalTickPercentage() {
        return totalTickPercentage;
    }

    /**
     * @return the amount of ticks to wait until the next iteration of chunk processing.
     */
    public int getTickInterval() {
        return tickInterval;
    }

    /**
     * @return the name of the default world for the map to load.
     */
    @Nonnull
    public String getDefaultWorldName() {
        return defaultWorld;
    }

    /**
     * @return the list of available worlds to render.
     */
    @Nonnull
    public List<String> getWorldList() {
        return worldList;
    }

    /**
     * @return true if this plugin is allowed to generate terrain false otherwise.
     */
    public boolean canGenerateWorld() {
        return generateWorld;
    }

    /**
     * @return true if this plugin is allowed to load terrain false otherwise.
     */
    public boolean canLoadWorld() {
        return loadWorld;
    }

    /**
     * @return true if this plugin should respect the world border when generating terrain.
     */
    public boolean shouldRespectWorldBorder() {
        return respectWorldBorder;
    }

    /**
     * @return true if the size of the cache is Limited.
     */
    public boolean isCacheLimited() {
        return limitCache;
    }

    /**
     * @return maximum number of chunks (per world) allowed in the cache at one time.
     */
    public int getMaximumChunksInCache() {
        return maximumChunksInCache;
    }

    /**
     * @return the jdbc url for chunk storage.
     */
    @Nonnull
    public String getJdbcDatabaseUrl() {
        return jdbcDatabaseUrl;
    }

    /**
     * @return the prefix for database tables.
     */
    @Nonnull
    public String getDatabaseTablePrefix() {
        return databaseTablePrefix;
    }

    /**
     * @return true if the plugin should only allow certified map uuid's to connect false otherwise.
     */
    @Nonnull
    public boolean isCertifiedUuids() {
        return certifiedUuids;
    }

    /**
     * @return A certified list of uuid's for client maps. There should be at least one value in this list.
     */
    @Nonnull
    public List<String> getUuidList() {
        return uuidList;
    }
}
