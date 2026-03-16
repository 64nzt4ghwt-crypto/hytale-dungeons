package com.misa.dungeons.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for a single dungeon room.
 * Rooms are progressed through sequentially.
 * <p>
 * Room types:
 *  - "combat"  : clear all mobs to proceed
 *  - "boss"    : boss encounter, single-wave or multi-wave
 *  - "timed"   : clear within timeLimitSeconds
 *  - "puzzle"  : placeholder for non-combat progression (future)
 */
public class RoomConfig {

    private int roomIndex;
    private String name;
    private String type;
    private List<WaveConfig> waves;
    private String lootTable;
    private int timeLimitSeconds;

    public RoomConfig() {
        this.waves = new ArrayList<>();
    }

    public RoomConfig(int roomIndex, String name, String type,
                      List<WaveConfig> waves, String lootTable, int timeLimitSeconds) {
        this.roomIndex = roomIndex;
        this.name = name;
        this.type = type;
        this.waves = waves != null ? waves : new ArrayList<>();
        this.lootTable = lootTable;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public int getRoomIndex() {
        return roomIndex;
    }

    public void setRoomIndex(int roomIndex) {
        this.roomIndex = roomIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<WaveConfig> getWaves() {
        return waves;
    }

    public void setWaves(List<WaveConfig> waves) {
        this.waves = waves;
    }

    public String getLootTable() {
        return lootTable;
    }

    public void setLootTable(String lootTable) {
        this.lootTable = lootTable;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(int timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public boolean isBoss() {
        return "boss".equalsIgnoreCase(type);
    }

    public boolean isTimed() {
        return "timed".equalsIgnoreCase(type) && timeLimitSeconds > 0;
    }

    /**
     * Returns total mobs across all waves in this room.
     */
    public int getTotalMobCount() {
        int total = 0;
        for (WaveConfig wave : waves) {
            total += wave.getTotalMobCount();
        }
        return total;
    }
}
