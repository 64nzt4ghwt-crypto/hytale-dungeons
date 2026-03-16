package com.howlstudio.dungeons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Manages world-based dungeon registrations.
 * 
 * Admins register existing worlds as dungeons via /dungeon register <world>.
 * Each registered world has:
 * - A spawn point where players teleport in
 * - A portal location (optional, for portal-based entry)
 * - Rooms with spawn points and loot tables
 * 
 * Data is persisted to dungeons.json in the plugin data directory.
 */
public class DungeonWorldConfig {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /** Represents one registered dungeon world */
    public static class WorldEntry {
        private String worldName;
        private String displayName;
        private double spawnX;
        private double spawnY;
        private double spawnZ;
        private String portalWorld;
        private double portalX;
        private double portalY;
        private double portalZ;
        private int maxPlayers;
        private int timeLimitSeconds;
        private Map<String, RoomEntry> rooms;

        public WorldEntry() {
            this.maxPlayers = 4;
            this.timeLimitSeconds = 600;
            this.rooms = new LinkedHashMap<>();
        }

        public String getWorldName() { return worldName; }
        public void setWorldName(String worldName) { this.worldName = worldName; }

        public String getDisplayName() {
            return displayName != null ? displayName : worldName;
        }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public double getSpawnX() { return spawnX; }
        public void setSpawnX(double x) { this.spawnX = x; }

        public double getSpawnY() { return spawnY; }
        public void setSpawnY(double y) { this.spawnY = y; }

        public double getSpawnZ() { return spawnZ; }
        public void setSpawnZ(double z) { this.spawnZ = z; }

        public String getPortalWorld() { return portalWorld; }
        public void setPortalWorld(String w) { this.portalWorld = w; }

        public double getPortalX() { return portalX; }
        public void setPortalX(double x) { this.portalX = x; }

        public double getPortalY() { return portalY; }
        public void setPortalY(double y) { this.portalY = y; }

        public double getPortalZ() { return portalZ; }
        public void setPortalZ(double z) { this.portalZ = z; }

        public int getMaxPlayers() { return maxPlayers; }
        public void setMaxPlayers(int max) { this.maxPlayers = Math.max(1, max); }

        public int getTimeLimitSeconds() { return timeLimitSeconds; }
        public void setTimeLimitSeconds(int seconds) { this.timeLimitSeconds = seconds; }

        public Map<String, RoomEntry> getRooms() { return rooms; }
        public void setRooms(Map<String, RoomEntry> rooms) {
            this.rooms = rooms != null ? rooms : new LinkedHashMap<>();
        }

        public boolean hasPortal() {
            return portalWorld != null && !portalWorld.isEmpty();
        }
    }

    /** A room within a registered dungeon world */
    public static class RoomEntry {
        private String name;
        private boolean isBoss;
        private List<SpawnPointConfig> spawnPoints;
        private List<LootEntryConfig> lootTable;

        public RoomEntry() {
            this.spawnPoints = new ArrayList<>();
            this.lootTable = new ArrayList<>();
        }

        public String getName() { return name != null ? name : "Unnamed Room"; }
        public void setName(String name) { this.name = name; }

        public boolean isBoss() { return isBoss; }
        public void setBoss(boolean boss) { this.isBoss = boss; }

        public List<SpawnPointConfig> getSpawnPoints() { return spawnPoints; }
        public void setSpawnPoints(List<SpawnPointConfig> sp) {
            this.spawnPoints = sp != null ? sp : new ArrayList<>();
        }

        public List<LootEntryConfig> getLootTable() { return lootTable; }
        public void setLootTable(List<LootEntryConfig> loot) {
            this.lootTable = loot != null ? loot : new ArrayList<>();
        }
    }

    /** Loot entry in a room's loot table */
    public static class LootEntryConfig {
        private String item;
        private int minCount;
        private int maxCount;
        private double chance;

        public LootEntryConfig() {
            this.minCount = 1;
            this.maxCount = 1;
            this.chance = 1.0;
        }

        public LootEntryConfig(String item, double chance) {
            this();
            this.item = item;
            this.chance = chance;
        }

        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }

        public int getMinCount() { return minCount; }
        public void setMinCount(int c) { this.minCount = Math.max(0, c); }

        public int getMaxCount() { return maxCount; }
        public void setMaxCount(int c) { this.maxCount = Math.max(0, c); }

        public double getChance() { return chance; }
        public void setChance(double c) { this.chance = Math.max(0.0, Math.min(1.0, c)); }
    }

    // --- Config manager state ---

    private final Map<String, WorldEntry> registeredWorlds;
    private File configFile;

    public DungeonWorldConfig() {
        this.registeredWorlds = new LinkedHashMap<>();
    }

    /**
     * Loads the config from disk.
     */
    public void load(File dataDirectory) {
        this.configFile = new File(dataDirectory, "dungeons.json");

        if (!configFile.exists()) {
            System.out.println("[Dungeons] No dungeons.json found, starting fresh");
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Type mapType = new TypeToken<Map<String, WorldEntry>>() {}.getType();
            Map<String, WorldEntry> loaded = GSON.fromJson(reader, mapType);
            if (loaded != null) {
                registeredWorlds.clear();
                registeredWorlds.putAll(loaded);
            }
            System.out.println("[Dungeons] Loaded " + registeredWorlds.size() + " registered dungeon worlds");
        } catch (IOException e) {
            System.err.println("[Dungeons] Failed to load dungeons.json: " + e.getMessage());
        }
    }

    /**
     * Saves the config to disk.
     */
    public void save() {
        if (configFile == null) return;

        configFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(registeredWorlds, writer);
        } catch (IOException e) {
            System.err.println("[Dungeons] Failed to save dungeons.json: " + e.getMessage());
        }
    }

    /**
     * Registers a world as a dungeon.
     * @return true if newly registered, false if already existed
     */
    public boolean registerWorld(String worldName) {
        if (registeredWorlds.containsKey(worldName)) {
            return false;
        }

        WorldEntry entry = new WorldEntry();
        entry.setWorldName(worldName);
        entry.setDisplayName(worldName);
        // Default spawn at 0,64,0
        entry.setSpawnX(0);
        entry.setSpawnY(64);
        entry.setSpawnZ(0);

        registeredWorlds.put(worldName, entry);
        save();
        return true;
    }

    /**
     * Unregisters a world as a dungeon.
     * @return true if was registered, false if not found
     */
    public boolean unregisterWorld(String worldName) {
        if (registeredWorlds.remove(worldName) != null) {
            save();
            return true;
        }
        return false;
    }

    /**
     * Gets a registered world entry.
     */
    public WorldEntry getWorld(String worldName) {
        return registeredWorlds.get(worldName);
    }

    /**
     * Checks if a world is registered as a dungeon.
     */
    public boolean isRegistered(String worldName) {
        return registeredWorlds.containsKey(worldName);
    }

    /**
     * Gets all registered world names.
     */
    public Collection<String> getRegisteredWorldNames() {
        return Collections.unmodifiableSet(registeredWorlds.keySet());
    }

    /**
     * Gets all registered world entries.
     */
    public Map<String, WorldEntry> getAllWorlds() {
        return Collections.unmodifiableMap(registeredWorlds);
    }

    /**
     * Adds a spawn point to a dungeon room.
     * Creates the room if it doesn't exist.
     */
    public void addSpawnPoint(String worldName, String roomName, SpawnPointConfig spawnPoint) {
        WorldEntry world = registeredWorlds.get(worldName);
        if (world == null) return;

        RoomEntry room = world.getRooms().computeIfAbsent(roomName, k -> {
            RoomEntry r = new RoomEntry();
            r.setName(k);
            return r;
        });

        room.getSpawnPoints().add(spawnPoint);
        save();
    }

    /**
     * Adds a loot entry to a dungeon room.
     * Creates the room if it doesn't exist.
     */
    public void addLootEntry(String dungeonName, String roomName, LootEntryConfig lootEntry) {
        // Try registered worlds first
        WorldEntry world = registeredWorlds.get(dungeonName);
        if (world == null) return;

        RoomEntry room = world.getRooms().computeIfAbsent(roomName, k -> {
            RoomEntry r = new RoomEntry();
            r.setName(k);
            return r;
        });

        room.getLootTable().add(lootEntry);
        save();
    }

    /**
     * Sets the portal location for a dungeon.
     */
    public void setPortal(String worldName, String portalWorld, double x, double y, double z) {
        WorldEntry entry = registeredWorlds.get(worldName);
        if (entry == null) return;

        entry.setPortalWorld(portalWorld);
        entry.setPortalX(x);
        entry.setPortalY(y);
        entry.setPortalZ(z);
        save();
    }

    /**
     * Gets all unique portal worlds (worlds that contain dungeon portals).
     * Used by the tick system to know which worlds to scan for player proximity.
     */
    public Collection<String> getPortalWorlds() {
        java.util.Set<String> portalWorlds = new java.util.HashSet<>();
        for (WorldEntry entry : registeredWorlds.values()) {
            if (entry.hasPortal() && entry.getPortalWorld() != null) {
                portalWorlds.add(entry.getPortalWorld());
            }
        }
        return portalWorlds;
    }
    
    /**
     * Reload config from disk.
     */
    public void reload() {
        if (configFile != null) {
            registeredWorlds.clear();
            load(configFile.getParentFile());
        }
    }
}
