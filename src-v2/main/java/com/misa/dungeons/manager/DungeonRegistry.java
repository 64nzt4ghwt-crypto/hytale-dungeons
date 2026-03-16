package com.misa.dungeons.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.misa.dungeons.config.DungeonConfig;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central registry of all dungeon configurations loaded from disk.
 * Thread-safe: configs are loaded once at startup and stored in a ConcurrentHashMap.
 */
public class DungeonRegistry {

    private static final Logger LOGGER = Logger.getLogger(DungeonRegistry.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, DungeonConfig> configs = new ConcurrentHashMap<>();
    private final Path configDirectory;

    public DungeonRegistry(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    /**
     * Loads all .json files from the config directory as DungeonConfig objects.
     * Validates each config and logs errors for invalid ones (skipping them).
     *
     * @return number of configs successfully loaded
     */
    public int loadAll() {
        configs.clear();
        int loaded = 0;

        if (!Files.isDirectory(configDirectory)) {
            LOGGER.warning("[Dungeons] Config directory does not exist: " + configDirectory);
            return 0;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(configDirectory, "*.json")) {
            for (Path file : stream) {
                try {
                    String json = Files.readString(file);
                    DungeonConfig config = GSON.fromJson(json, DungeonConfig.class);

                    if (config == null) {
                        LOGGER.warning("[Dungeons] Null config from file: " + file.getFileName());
                        continue;
                    }

                    String error = config.validate();
                    if (error != null) {
                        LOGGER.warning("[Dungeons] Invalid config " + file.getFileName() + ": " + error);
                        continue;
                    }

                    configs.put(config.getId(), config);
                    loaded++;
                    LOGGER.info("[Dungeons] Loaded dungeon: " + config.getId()
                            + " (" + config.getRoomCount() + " rooms)");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                            "[Dungeons] Failed to parse config: " + file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[Dungeons] Failed to read config directory", e);
        }

        LOGGER.info("[Dungeons] Loaded " + loaded + " dungeon config(s)");
        return loaded;
    }

    /**
     * Returns the config for the given dungeon ID, or null if not found.
     */
    public DungeonConfig getConfig(String dungeonId) {
        return configs.get(dungeonId);
    }

    /**
     * Returns all registered dungeon configs (unmodifiable).
     */
    public Collection<DungeonConfig> getAllConfigs() {
        return Collections.unmodifiableCollection(configs.values());
    }

    /**
     * Returns all registered dungeon IDs (unmodifiable).
     */
    public Collection<String> getAllIds() {
        return Collections.unmodifiableCollection(configs.keySet());
    }

    /**
     * Returns whether a dungeon with the given ID exists.
     */
    public boolean exists(String dungeonId) {
        return configs.containsKey(dungeonId);
    }

    /**
     * Hot-reloads all configs from disk.
     * @return number of configs loaded
     */
    public int reload() {
        LOGGER.info("[Dungeons] Reloading dungeon configs...");
        return loadAll();
    }

    public int size() {
        return configs.size();
    }
}
