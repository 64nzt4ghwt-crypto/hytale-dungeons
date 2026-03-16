package com.misa.dungeons.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes one wave of mob spawns within a room.
 * Waves trigger sequentially: wave 0 spawns on room entry,
 * subsequent waves spawn after their spawnDelay (in seconds) once the prior wave starts.
 */
public class WaveConfig {

    private int waveIndex;
    private double spawnDelay;
    private List<MobSpawnEntry> mobs;

    public WaveConfig() {
        this.mobs = new ArrayList<>();
    }

    public WaveConfig(int waveIndex, double spawnDelay, List<MobSpawnEntry> mobs) {
        this.waveIndex = waveIndex;
        this.spawnDelay = spawnDelay;
        this.mobs = mobs != null ? mobs : new ArrayList<>();
    }

    public int getWaveIndex() {
        return waveIndex;
    }

    public void setWaveIndex(int waveIndex) {
        this.waveIndex = waveIndex;
    }

    public double getSpawnDelay() {
        return spawnDelay;
    }

    public void setSpawnDelay(double spawnDelay) {
        this.spawnDelay = spawnDelay;
    }

    public List<MobSpawnEntry> getMobs() {
        return mobs;
    }

    public void setMobs(List<MobSpawnEntry> mobs) {
        this.mobs = mobs;
    }

    /**
     * Returns the total mob count across all entries in this wave.
     */
    public int getTotalMobCount() {
        int total = 0;
        for (MobSpawnEntry entry : mobs) {
            total += entry.getCount();
        }
        return total;
    }
}
