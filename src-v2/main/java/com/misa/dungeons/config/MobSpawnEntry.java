package com.misa.dungeons.config;

/**
 * Describes a single mob type to spawn within a wave.
 * Deserialized from dungeon config JSON.
 */
public class MobSpawnEntry {

    private String entityType;
    private int count;
    private double spawnRadius;

    public MobSpawnEntry() {
    }

    public MobSpawnEntry(String entityType, int count, double spawnRadius) {
        this.entityType = entityType;
        this.count = count;
        this.spawnRadius = spawnRadius;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getSpawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(double spawnRadius) {
        this.spawnRadius = spawnRadius;
    }

    @Override
    public String toString() {
        return entityType + " x" + count + " (r=" + spawnRadius + ")";
    }
}
