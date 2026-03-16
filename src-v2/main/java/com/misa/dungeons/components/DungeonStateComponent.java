package com.misa.dungeons.components;

import com.hytale.server.ecs.component.Component;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.type.ComponentType;
import com.hytale.server.ecs.Ref;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ECS component attached to player entities while they are inside a dungeon.
 * Tracks per-player dungeon state: which dungeon, current room, mob tracking, etc.
 * <p>
 * Removed from the player when they leave the dungeon or the session ends.
 */
public class DungeonStateComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, DungeonStateComponent> componentType;

    public static ComponentType<EntityStore, DungeonStateComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, DungeonStateComponent> type) {
        componentType = type;
    }

    // ---- fields ----

    /** Unique session ID for this dungeon run */
    private UUID sessionId;

    /** Config ID of the dungeon (e.g., "forest_crypt") */
    private String dungeonId;

    /** Current room index within the dungeon */
    private int currentRoom;

    /** Current wave index within the current room */
    private int currentWave;

    /** Number of mobs remaining in the current room (across all spawned waves) */
    private int mobsRemaining;

    /** Whether the current room has been cleared */
    private boolean roomCleared;

    /** Timestamp (System.currentTimeMillis) when the dungeon was entered */
    private long startTime;

    /** Elapsed time in the current room (seconds, accumulated by tick system) */
    private double roomElapsedTime;

    /** Total deaths this player has accrued in this session */
    private int deathCount;

    /** Whether this player is currently dead and awaiting respawn */
    private boolean dead;

    /** Tracked mob entity refs spawned for this session (shared via DungeonSession) */
    private Set<Ref> trackedMobs;

    public DungeonStateComponent() {
        this.sessionId = null;
        this.dungeonId = "";
        this.currentRoom = 0;
        this.currentWave = 0;
        this.mobsRemaining = 0;
        this.roomCleared = false;
        this.startTime = 0L;
        this.roomElapsedTime = 0.0;
        this.deathCount = 0;
        this.dead = false;
        this.trackedMobs = ConcurrentHashMap.newKeySet();
    }

    @Override
    public DungeonStateComponent clone() {
        DungeonStateComponent copy = new DungeonStateComponent();
        copy.sessionId = this.sessionId;
        copy.dungeonId = this.dungeonId;
        copy.currentRoom = this.currentRoom;
        copy.currentWave = this.currentWave;
        copy.mobsRemaining = this.mobsRemaining;
        copy.roomCleared = this.roomCleared;
        copy.startTime = this.startTime;
        copy.roomElapsedTime = this.roomElapsedTime;
        copy.deathCount = this.deathCount;
        copy.dead = this.dead;
        copy.trackedMobs = ConcurrentHashMap.newKeySet();
        copy.trackedMobs.addAll(this.trackedMobs);
        return copy;
    }

    // ---- getters / setters ----

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
    }

    public int getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(int currentRoom) {
        this.currentRoom = currentRoom;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public int getMobsRemaining() {
        return mobsRemaining;
    }

    public void setMobsRemaining(int mobsRemaining) {
        this.mobsRemaining = mobsRemaining;
    }

    public void decrementMobsRemaining() {
        if (this.mobsRemaining > 0) {
            this.mobsRemaining--;
        }
    }

    public boolean isRoomCleared() {
        return roomCleared;
    }

    public void setRoomCleared(boolean roomCleared) {
        this.roomCleared = roomCleared;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public double getRoomElapsedTime() {
        return roomElapsedTime;
    }

    public void setRoomElapsedTime(double roomElapsedTime) {
        this.roomElapsedTime = roomElapsedTime;
    }

    public void addRoomElapsedTime(double dt) {
        this.roomElapsedTime += dt;
    }

    public int getDeathCount() {
        return deathCount;
    }

    public void setDeathCount(int deathCount) {
        this.deathCount = deathCount;
    }

    public void incrementDeathCount() {
        this.deathCount++;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public Set<Ref> getTrackedMobs() {
        return trackedMobs;
    }

    public void setTrackedMobs(Set<Ref> trackedMobs) {
        this.trackedMobs = trackedMobs;
    }

    /**
     * Returns total dungeon elapsed time in seconds since startTime.
     */
    public double getTotalElapsedSeconds() {
        if (startTime <= 0L) {
            return 0.0;
        }
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
}
