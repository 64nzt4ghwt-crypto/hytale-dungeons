package com.misa.dungeons.manager;

import com.hytale.server.world.World;
import com.hytale.server.ecs.Ref;
import com.misa.dungeons.config.DungeonConfig;
import com.misa.dungeons.config.RoomConfig;
import com.misa.dungeons.config.WaveConfig;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a live dungeon instance session.
 * Tracks the instance world, participating players, spawned mobs,
 * current room/wave state, and timing.
 * <p>
 * One DungeonSession exists per active instance. Multiple players share
 * the same session when running a dungeon together (party).
 */
public class DungeonSession {

    /** Unique session ID */
    private final UUID sessionId;

    /** The dungeon config this session is based on */
    private final DungeonConfig config;

    /** The Hytale World object for this instance */
    private volatile World instanceWorld;

    /** Player refs currently in this session */
    private final Set<Ref> players;

    /** All mob refs spawned and still alive in this session */
    private final Set<Ref> aliveMobs;

    /** Current room index */
    private volatile int currentRoom;

    /** Current wave index within the current room */
    private volatile int currentWave;

    /** Whether all waves for the current room have been spawned */
    private volatile boolean allWavesSpawned;

    /** Time (seconds) elapsed since the current wave was spawned */
    private volatile double waveTimer;

    /** Time (seconds) elapsed in the current room */
    private volatile double roomTimer;

    /** Timestamp when the session started */
    private final long startTimeMillis;

    /** Whether this session is completed (all rooms cleared) */
    private volatile boolean completed;

    /** Whether this session is being torn down */
    private volatile boolean ending;

    public DungeonSession(UUID sessionId, DungeonConfig config) {
        this.sessionId = sessionId;
        this.config = config;
        this.players = ConcurrentHashMap.newKeySet();
        this.aliveMobs = ConcurrentHashMap.newKeySet();
        this.currentRoom = 0;
        this.currentWave = 0;
        this.allWavesSpawned = false;
        this.waveTimer = 0.0;
        this.roomTimer = 0.0;
        this.startTimeMillis = System.currentTimeMillis();
        this.completed = false;
        this.ending = false;
    }

    // ---- identity ----

    public UUID getSessionId() {
        return sessionId;
    }

    public DungeonConfig getConfig() {
        return config;
    }

    public String getDungeonId() {
        return config.getId();
    }

    // ---- world ----

    public World getInstanceWorld() {
        return instanceWorld;
    }

    public void setInstanceWorld(World instanceWorld) {
        this.instanceWorld = instanceWorld;
    }

    // ---- players ----

    public Set<Ref> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public void addPlayer(Ref playerRef) {
        players.add(playerRef);
    }

    public void removePlayer(Ref playerRef) {
        players.remove(playerRef);
    }

    public boolean hasPlayer(Ref playerRef) {
        return players.contains(playerRef);
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public int getPlayerCount() {
        return players.size();
    }

    // ---- mobs ----

    public Set<Ref> getAliveMobs() {
        return aliveMobs;
    }

    public void trackMob(Ref mobRef) {
        aliveMobs.add(mobRef);
    }

    public void removeMob(Ref mobRef) {
        aliveMobs.remove(mobRef);
    }

    public int getAliveMobCount() {
        return aliveMobs.size();
    }

    public boolean allMobsDead() {
        return aliveMobs.isEmpty();
    }

    // ---- room / wave progression ----

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

    public boolean isAllWavesSpawned() {
        return allWavesSpawned;
    }

    public void setAllWavesSpawned(boolean allWavesSpawned) {
        this.allWavesSpawned = allWavesSpawned;
    }

    /**
     * Returns the current room config, or null if index is out of range.
     */
    public RoomConfig getCurrentRoomConfig() {
        return config.getRoom(currentRoom);
    }

    /**
     * Returns the current wave config, or null if index is out of range.
     */
    public WaveConfig getCurrentWaveConfig() {
        RoomConfig room = getCurrentRoomConfig();
        if (room == null) return null;
        if (currentWave < 0 || currentWave >= room.getWaves().size()) return null;
        return room.getWaves().get(currentWave);
    }

    /**
     * Returns true if there are more waves after the current one in this room.
     */
    public boolean hasNextWave() {
        RoomConfig room = getCurrentRoomConfig();
        if (room == null) return false;
        return currentWave + 1 < room.getWaves().size();
    }

    /**
     * Returns true if there are more rooms after the current one.
     */
    public boolean hasNextRoom() {
        return currentRoom + 1 < config.getRoomCount();
    }

    /**
     * Advances to the next wave within the current room.
     * @return true if advanced, false if no more waves
     */
    public boolean advanceWave() {
        if (hasNextWave()) {
            currentWave++;
            waveTimer = 0.0;
            return true;
        }
        allWavesSpawned = true;
        return false;
    }

    /**
     * Advances to the next room, resetting wave/mob tracking.
     * @return true if advanced, false if no more rooms (dungeon complete)
     */
    public boolean advanceRoom() {
        if (hasNextRoom()) {
            currentRoom++;
            currentWave = 0;
            allWavesSpawned = false;
            waveTimer = 0.0;
            roomTimer = 0.0;
            aliveMobs.clear();
            return true;
        }
        return false;
    }

    // ---- timers ----

    public double getWaveTimer() {
        return waveTimer;
    }

    public void setWaveTimer(double waveTimer) {
        this.waveTimer = waveTimer;
    }

    public void addWaveTimer(double dt) {
        this.waveTimer += dt;
    }

    public double getRoomTimer() {
        return roomTimer;
    }

    public void setRoomTimer(double roomTimer) {
        this.roomTimer = roomTimer;
    }

    public void addRoomTimer(double dt) {
        this.roomTimer += dt;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public double getTotalElapsedSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000.0;
    }

    /**
     * Returns true if the dungeon-global time limit has been exceeded.
     */
    public boolean isTimeLimitExceeded() {
        int limit = config.getTimeLimit();
        if (limit <= 0) return false;
        return getTotalElapsedSeconds() >= limit;
    }

    /**
     * Returns true if the current room's time limit has been exceeded.
     */
    public boolean isRoomTimeLimitExceeded() {
        RoomConfig room = getCurrentRoomConfig();
        if (room == null || !room.isTimed()) return false;
        return roomTimer >= room.getTimeLimitSeconds();
    }

    // ---- lifecycle ----

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isEnding() {
        return ending;
    }

    public void setEnding(boolean ending) {
        this.ending = ending;
    }

    /**
     * Returns true if this session should still be actively running.
     */
    public boolean isActive() {
        return !completed && !ending;
    }

    @Override
    public String toString() {
        return "DungeonSession{id=" + sessionId
                + ", dungeon=" + config.getId()
                + ", room=" + currentRoom + "/" + config.getRoomCount()
                + ", wave=" + currentWave
                + ", players=" + players.size()
                + ", mobs=" + aliveMobs.size()
                + ", active=" + isActive() + "}";
    }
}
