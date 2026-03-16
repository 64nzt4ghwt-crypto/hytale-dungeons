package com.howlstudio.dungeons.config;

/**
 * A mob spawn point definition saved to JSON config.
 * 
 * Spawn points are stored per dungeon and per room.
 * When a room activates, all its spawn points fire.
 * 
 * Supports:
 * - Entity type (e.g., "hytale:skeleton")
 * - Position (x, y, z)
 * - Amount to spawn
 * - Spread radius around the point
 * - Wave number (for multi-wave spawns)
 * - Spawn chance (probability 0-100)
 */
public class SpawnPointConfig {

    private String entity;
    private double x;
    private double y;
    private double z;
    private int amount;
    private double radius;
    private int wave;
    private int chance;

    public SpawnPointConfig() {
        this.amount = 1;
        this.radius = 0;
        this.wave = 1;
        this.chance = 100;
    }

    public SpawnPointConfig(String entity, double x, double y, double z) {
        this();
        this.entity = entity;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ---- Getters & Setters ----

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = Math.max(0, radius);
    }

    public int getWave() {
        return wave;
    }

    public void setWave(int wave) {
        this.wave = Math.max(1, wave);
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = Math.max(0, Math.min(100, chance));
    }

    /**
     * Returns a formatted position string.
     */
    public String positionString() {
        return String.format("%.1f, %.1f, %.1f", x, y, z);
    }

    @Override
    public String toString() {
        return "SpawnPoint{entity=" + entity
            + ", pos=(" + positionString() + ")"
            + ", amount=" + amount
            + ", wave=" + wave
            + ", chance=" + chance + "%"
            + "}";
    }
}
