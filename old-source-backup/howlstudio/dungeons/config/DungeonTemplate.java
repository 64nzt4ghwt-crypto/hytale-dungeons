package com.howlstudio.dungeons.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a dungeon template loaded from JSON.
 * Defines the structure: rooms, mob spawns, loot tables, time limits.
 * 
 * This is the blueprint - actual instances are created from these templates.
 * 
 * Example JSON structure:
 * {
 *   "id": "skeleton_crypt",
 *   "name": "Skeleton Crypt",
 *   "rooms": [...],
 *   "difficulty": {...}
 * }
 */
public class DungeonTemplate {
    
    private String id;
    private String name;
    private String description;
    private int minPlayers;
    private int maxPlayers;
    private int timeLimitSeconds;
    private List<RoomDefinition> rooms;
    private DifficultyScaling difficulty;
    
    public DungeonTemplate() {
        this.rooms = new ArrayList<>();
        this.minPlayers = 1;
        this.maxPlayers = 4;
        this.timeLimitSeconds = 600;
        this.difficulty = new DifficultyScaling();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name != null ? name : id;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description != null ? description : "A mysterious dungeon";
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public int getMinPlayers() {
        return minPlayers;
    }
    
    public void setMinPlayers(int minPlayers) {
        this.minPlayers = Math.max(1, minPlayers);
    }
    
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(1, maxPlayers);
    }
    
    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
    
    public void setTimeLimitSeconds(int seconds) {
        this.timeLimitSeconds = seconds;
    }
    
    public List<RoomDefinition> getRooms() {
        return rooms;
    }
    
    public void setRooms(List<RoomDefinition> rooms) {
        this.rooms = rooms != null ? rooms : new ArrayList<>();
    }
    
    public int getRoomCount() {
        return rooms.size();
    }
    
    public RoomDefinition getRoom(int index) {
        if (index < 0 || index >= rooms.size()) {
            return null;
        }
        return rooms.get(index);
    }
    
    public DifficultyScaling getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(DifficultyScaling difficulty) {
        this.difficulty = difficulty != null ? difficulty : new DifficultyScaling();
    }
    
    /**
     * Validates this template configuration.
     * @return null if valid, error message otherwise
     */
    public String validate() {
        if (id == null || id.isEmpty()) {
            return "Dungeon template missing 'id' field";
        }
        if (rooms.isEmpty()) {
            return "Dungeon " + id + " has no rooms defined";
        }
        for (int i = 0; i < rooms.size(); i++) {
            RoomDefinition room = rooms.get(i);
            if (room.getName() == null || room.getName().isEmpty()) {
                return "Room " + i + " in dungeon " + id + " missing name";
            }
        }
        if (minPlayers > maxPlayers) {
            return "minPlayers cannot exceed maxPlayers in dungeon " + id;
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "DungeonTemplate{id='" + id + "', name='" + name 
            + "', rooms=" + rooms.size() + ", players=" + minPlayers + "-" + maxPlayers + "}";
    }
    
    // ---- Nested classes for structure ----
    
    /**
     * Defines a room within a dungeon.
     */
    public static class RoomDefinition {
        private String name;
        private List<MobSpawn> mobSpawns;
        private List<LootEntry> loot;
        private boolean isBoss;
        private int waves;  // number of waves (0 or 1 = single wave)
        private List<List<MobSpawn>> waveSpawns;  // per-wave spawn lists (optional)
        
        public RoomDefinition() {
            this.mobSpawns = new ArrayList<>();
            this.loot = new ArrayList<>();
            this.isBoss = false;
            this.waves = 1;
        }
        
        public String getName() {
            return name != null ? name : "Unnamed Room";
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<MobSpawn> getMobSpawns() {
            return mobSpawns;
        }
        
        public void setMobSpawns(List<MobSpawn> spawns) {
            this.mobSpawns = spawns != null ? spawns : new ArrayList<>();
        }
        
        public List<LootEntry> getLoot() {
            return loot;
        }
        
        public void setLoot(List<LootEntry> loot) {
            this.loot = loot != null ? loot : new ArrayList<>();
        }
        
        public boolean isBoss() {
            return isBoss;
        }
        
        public void setBoss(boolean boss) {
            this.isBoss = boss;
        }
        
        public int getWaves() {
            return Math.max(1, waves);
        }
        
        public void setWaves(int waves) {
            this.waves = Math.max(1, waves);
        }
        
        public List<List<MobSpawn>> getWaveSpawns() {
            return waveSpawns;
        }
        
        public void setWaveSpawns(List<List<MobSpawn>> waveSpawns) {
            this.waveSpawns = waveSpawns;
        }
        
        /**
         * Returns true if this room has multiple waves.
         */
        public boolean hasMultipleWaves() {
            return waves > 1 || (waveSpawns != null && waveSpawns.size() > 1);
        }
        
        /**
         * Gets mob spawns for a specific wave.
         * If waveSpawns is configured, uses that; otherwise all spawns are wave 0.
         */
        public List<MobSpawn> getMobSpawnsForWave(int waveIndex) {
            if (waveSpawns != null && waveIndex < waveSpawns.size()) {
                return waveSpawns.get(waveIndex);
            }
            // Fallback: all mobs spawn in wave 0
            return (waveIndex == 0) ? mobSpawns : new ArrayList<>();
        }
        
        /**
         * Calculates total mobs that will spawn in this room.
         */
        public int getTotalMobCount() {
            int total = 0;
            for (MobSpawn spawn : mobSpawns) {
                total += spawn.getCount();
            }
            return total;
        }
    }
    
    /**
     * Mob spawn entry for a room.
     */
    public static class MobSpawn {
        private String type;
        private int count;
        
        public MobSpawn() {
            this.count = 1;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = Math.max(1, count);
        }
    }
    
    /**
     * Loot entry for a room completion reward.
     */
    public static class LootEntry {
        private String item;
        private int minCount;
        private int maxCount;
        private double chance;
        
        public LootEntry() {
            this.minCount = 1;
            this.maxCount = 1;
            this.chance = 1.0;
        }
        
        public String getItem() {
            return item;
        }
        
        public void setItem(String item) {
            this.item = item;
        }
        
        public int getMinCount() {
            return minCount;
        }
        
        public void setMinCount(int count) {
            this.minCount = Math.max(0, count);
        }
        
        public int getMaxCount() {
            return maxCount;
        }
        
        public void setMaxCount(int count) {
            this.maxCount = Math.max(0, count);
        }
        
        public double getChance() {
            return chance;
        }
        
        public void setChance(double chance) {
            this.chance = Math.max(0.0, Math.min(1.0, chance));
        }
    }
    
    /**
     * Difficulty scaling configuration.
     */
    public static class DifficultyScaling {
        private double baseHealth;
        private double baseDamage;
        private double perPlayerHealthScale;
        private double perPlayerDamageScale;
        
        public DifficultyScaling() {
            this.baseHealth = 1.0;
            this.baseDamage = 1.0;
            this.perPlayerHealthScale = 0.3;
            this.perPlayerDamageScale = 0.1;
        }
        
        public double getBaseHealth() {
            return baseHealth;
        }
        
        public void setBaseHealth(double base) {
            this.baseHealth = base;
        }
        
        public double getBaseDamage() {
            return baseDamage;
        }
        
        public void setBaseDamage(double base) {
            this.baseDamage = base;
        }
        
        public double getPerPlayerHealthScale() {
            return perPlayerHealthScale;
        }
        
        public void setPerPlayerHealthScale(double scale) {
            this.perPlayerHealthScale = scale;
        }
        
        public double getPerPlayerDamageScale() {
            return perPlayerDamageScale;
        }
        
        public void setPerPlayerDamageScale(double scale) {
            this.perPlayerDamageScale = scale;
        }
        
        /**
         * Calculates health multiplier based on player count.
         */
        public double getHealthMultiplier(int playerCount) {
            double scale = baseHealth + (perPlayerHealthScale * (playerCount - 1));
            return Math.max(0.1, scale);
        }
        
        /**
         * Calculates damage multiplier based on player count.
         */
        public double getDamageMultiplier(int playerCount) {
            double scale = baseDamage + (perPlayerDamageScale * (playerCount - 1));
            return Math.max(0.1, scale);
        }
    }
}
