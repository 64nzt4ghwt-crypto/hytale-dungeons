package com.howlstudio.dungeons.manager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.howlstudio.dungeons.config.DungeonTemplate;

/**
 * Represents one active dungeon instance (a "run" in progress).
 * 
 * Each instance has:
 * - A unique session ID
 * - The template it's based on (or the registered world name)
 * - Current room index and progression state
 * - Per-room state tracking (LOCKED/ACTIVE/CLEARED)
 * - Spawned mobs being tracked
 * - Party members (player refs)
 * - Start time for timeout handling
 * - Optional linked world name for registered world dungeons
 * 
 * Multiple players can share one instance (party system).
 */
public class DungeonInstance {
    
    private final UUID instanceId;
    private final DungeonTemplate template;
    private final long startTimeMillis;
    
    // For world-based dungeons
    private String worldName;
    
    // Progression state
    private int currentRoomIndex;
    private int currentWave;  // current wave within current room (0-indexed)
    private int spawnedMobCount;
    private int remainingMobCount;
    private boolean roomCleared;
    private boolean completed;
    private boolean failed;
    
    // Timer warning tracking
    private boolean warned5min;
    private boolean warned1min;
    private boolean warned30sec;
    private boolean warned10sec;
    
    // Per-room state tracking (room index -> state)
    private final Map<Integer, RoomState> roomStates;
    
    // Party tracking
    private final Set<UUID> playerUuids;
    private final Set<UUID> spawnedMobUuids;
    
    // Loot generated for this instance (to prevent duping)
    private final Set<String> lootGranted;
    
    // Return positions: save where players were before entering dungeon
    // Format: playerUuid -> "worldName,x,y,z"
    private final Map<UUID, String> returnPositions;
    
    public DungeonInstance(UUID id, DungeonTemplate template) {
        this.instanceId = id;
        this.template = template;
        this.startTimeMillis = System.currentTimeMillis();
        this.currentRoomIndex = 0;
        this.currentWave = 0;
        this.spawnedMobCount = 0;
        this.remainingMobCount = 0;
        this.roomCleared = false;
        this.completed = false;
        this.failed = false;
        this.warned5min = false;
        this.warned1min = false;
        this.warned30sec = false;
        this.warned10sec = false;
        this.roomStates = new LinkedHashMap<>();
        this.playerUuids = ConcurrentHashMap.newKeySet();
        this.spawnedMobUuids = ConcurrentHashMap.newKeySet();
        this.lootGranted = ConcurrentHashMap.newKeySet();
        this.returnPositions = new ConcurrentHashMap<>();
        
        // Initialize room states: first room is ACTIVE, rest are LOCKED
        initializeRoomStates();
    }
    
    /**
     * Sets up initial room states based on template.
     */
    private void initializeRoomStates() {
        int roomCount = template.getRoomCount();
        for (int i = 0; i < roomCount; i++) {
            roomStates.put(i, i == 0 ? RoomState.ACTIVE : RoomState.LOCKED);
        }
    }
    
    // ---- Identity ----
    
    public UUID getInstanceId() {
        return instanceId;
    }
    
    public DungeonTemplate getTemplate() {
        return template;
    }
    
    public String getDungeonId() {
        return template.getId();
    }
    
    public long getStartTimeMillis() {
        return startTimeMillis;
    }
    
    // ---- World linkage ----
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public boolean hasLinkedWorld() {
        return worldName != null && !worldName.isEmpty();
    }
    
    // ---- Room state tracking ----
    
    /**
     * Gets the state of a specific room.
     */
    public RoomState getRoomState(int roomIndex) {
        return roomStates.getOrDefault(roomIndex, RoomState.LOCKED);
    }
    
    /**
     * Sets the state of a specific room.
     */
    public void setRoomState(int roomIndex, RoomState state) {
        roomStates.put(roomIndex, state);
    }
    
    /**
     * Activates a room (transitions from LOCKED to ACTIVE).
     * @return true if the room was successfully activated
     */
    public boolean activateRoom(int roomIndex) {
        RoomState current = getRoomState(roomIndex);
        if (current == RoomState.LOCKED) {
            roomStates.put(roomIndex, RoomState.ACTIVE);
            return true;
        }
        return false;
    }
    
    /**
     * Marks a room as cleared (transitions from ACTIVE to CLEARED).
     * @return true if the room was successfully cleared
     */
    public boolean clearRoom(int roomIndex) {
        RoomState current = getRoomState(roomIndex);
        if (current == RoomState.ACTIVE) {
            roomStates.put(roomIndex, RoomState.CLEARED);
            return true;
        }
        return false;
    }
    
    /**
     * Gets all room states as an unmodifiable map.
     */
    public Map<Integer, RoomState> getAllRoomStates() {
        return Collections.unmodifiableMap(roomStates);
    }
    
    /**
     * Counts rooms in a given state.
     */
    public int countRoomsInState(RoomState state) {
        int count = 0;
        for (RoomState rs : roomStates.values()) {
            if (rs == state) count++;
        }
        return count;
    }
    
    // ---- Progression ----
    
    public int getCurrentRoomIndex() {
        return currentRoomIndex;
    }
    
    public void setCurrentRoomIndex(int index) {
        this.currentRoomIndex = index;
    }
    
    /**
     * Advances to the next room.
     * Marks current room as CLEARED and next room as ACTIVE.
     * @return true if advanced successfully
     */
    public boolean advanceRoom() {
        if (currentRoomIndex + 1 < template.getRoomCount()) {
            // Mark current room as cleared
            clearRoom(currentRoomIndex);
            
            // Move to next room
            currentRoomIndex++;
            currentWave = 0;
            roomCleared = false;
            spawnedMobCount = 0;
            remainingMobCount = 0;
            
            // Activate the new room
            activateRoom(currentRoomIndex);
            
            return true;
        }
        return false;
    }
    
    public boolean isFinalRoom() {
        return currentRoomIndex == template.getRoomCount() - 1;
    }
    
    public int getTotalRooms() {
        return template.getRoomCount();
    }
    
    public DungeonTemplate.RoomDefinition getCurrentRoom() {
        return template.getRoom(currentRoomIndex);
    }
    
    // ---- Wave management ----
    
    public int getCurrentWave() {
        return currentWave;
    }
    
    public void setCurrentWave(int wave) {
        this.currentWave = wave;
    }
    
    /**
     * Advances to the next wave in the current room.
     * @return true if there's another wave, false if all waves done
     */
    public boolean advanceWave() {
        var room = getCurrentRoom();
        if (room == null) return false;
        
        int totalWaves = room.getWaves();
        if (currentWave + 1 < totalWaves) {
            currentWave++;
            spawnedMobCount = 0;
            remainingMobCount = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Returns true if the current wave is the last wave of the room.
     */
    public boolean isLastWave() {
        var room = getCurrentRoom();
        if (room == null) return true;
        return currentWave >= room.getWaves() - 1;
    }
    
    // ---- Timer warnings ----
    
    public boolean shouldWarn5min() {
        if (warned5min) return false;
        long remaining = getRemainingSeconds();
        if (remaining <= 300 && remaining > 0) { warned5min = true; return true; }
        return false;
    }
    
    public boolean shouldWarn1min() {
        if (warned1min) return false;
        long remaining = getRemainingSeconds();
        if (remaining <= 60 && remaining > 0) { warned1min = true; return true; }
        return false;
    }
    
    public boolean shouldWarn30sec() {
        if (warned30sec) return false;
        long remaining = getRemainingSeconds();
        if (remaining <= 30 && remaining > 0) { warned30sec = true; return true; }
        return false;
    }
    
    public boolean shouldWarn10sec() {
        if (warned10sec) return false;
        long remaining = getRemainingSeconds();
        if (remaining <= 10 && remaining > 0) { warned10sec = true; return true; }
        return false;
    }
    
    // ---- Mob management ----
    
    public int getSpawnedMobCount() {
        return spawnedMobCount;
    }
    
    public int getRemainingMobCount() {
        return remainingMobCount;
    }
    
    public void setRemainingMobCount(int count) {
        this.remainingMobCount = count;
    }
    
    public void onMobSpawned() {
        spawnedMobCount++;
        remainingMobCount++;
    }
    
    public void onMobKilled() {
        remainingMobCount = Math.max(0, remainingMobCount - 1);
    }
    
    public boolean areAllMobsDead() {
        return remainingMobCount <= 0;
    }
    
    public void trackMob(UUID mobUuid) {
        spawnedMobUuids.add(mobUuid);
    }
    
    /**
     * Adds a spawned mob UUID to tracking and increments counters.
     * Convenience method combining trackMob + onMobSpawned.
     */
    public void addSpawnedMob(UUID mobUuid) {
        trackMob(mobUuid);
        onMobSpawned();
    }
    
    /**
     * Removes a mob UUID from tracking (confirmed dead / despawned).
     * Decrements the remaining count.
     * @return true if the mob was being tracked
     */
    public boolean removeMob(UUID mobUuid) {
        boolean removed = spawnedMobUuids.remove(mobUuid);
        if (removed) {
            onMobKilled();
        }
        return removed;
    }
    
    public Set<UUID> getSpawnedMobUuids() {
        return Collections.unmodifiableSet(spawnedMobUuids);
    }
    
    public void clearSpawnedMobs() {
        spawnedMobUuids.clear();
        spawnedMobCount = 0;
        remainingMobCount = 0;
    }
    
    // ---- Room state (legacy compat + current room shorthand) ----
    
    public boolean isRoomCleared() {
        return roomCleared;
    }
    
    public void setRoomCleared(boolean cleared) {
        this.roomCleared = cleared;
        if (cleared) {
            clearRoom(currentRoomIndex);
        }
    }
    
    // ---- Completion state ----
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public boolean isFailed() {
        return failed;
    }
    
    public void setFailed(boolean failed) {
        this.failed = failed;
    }
    
    public boolean isActive() {
        return !completed && !failed;
    }
    
    // ---- Party management ----
    
    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(playerUuids);
    }
    
    public void addPlayer(UUID playerUuid) {
        playerUuids.add(playerUuid);
    }
    
    public void removePlayer(UUID playerUuid) {
        playerUuids.remove(playerUuid);
    }
    
    public boolean hasPlayer(UUID playerUuid) {
        return playerUuids.contains(playerUuid);
    }
    
    public int getPlayerCount() {
        return playerUuids.size();
    }
    
    public boolean isEmpty() {
        return playerUuids.isEmpty();
    }
    
    public boolean isFull() {
        return playerUuids.size() >= template.getMaxPlayers();
    }
    
    public boolean canJoin() {
        return isActive() && !isFull();
    }
    
    // ---- Time handling ----
    
    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }
    
    public boolean isTimeExpired() {
        return getElapsedSeconds() >= template.getTimeLimitSeconds();
    }
    
    public long getRemainingSeconds() {
        long elapsed = getElapsedSeconds();
        return Math.max(0, template.getTimeLimitSeconds() - elapsed);
    }
    
    // ---- Loot tracking ----
    
    public boolean hasGrantedLoot(String lootId) {
        return lootGranted.contains(lootId);
    }
    
    public void markLootGranted(String lootId) {
        lootGranted.add(lootId);
    }
    
    // ---- Difficulty scaling ----
    
    public double getHealthMultiplier() {
        return template.getDifficulty().getHealthMultiplier(getPlayerCount());
    }
    
    public double getDamageMultiplier() {
        return template.getDifficulty().getDamageMultiplier(getPlayerCount());
    }
    
    // ---- Return position tracking ----
    
    /**
     * Saves where a player was before entering the dungeon,
     * so they can be teleported back when they leave.
     */
    public void saveReturnPosition(UUID playerUuid, String worldName, double x, double y, double z) {
        returnPositions.put(playerUuid, worldName + "," + x + "," + y + "," + z);
    }
    
    /**
     * Gets the saved return position for a player.
     * @return "worldName,x,y,z" or null if not saved
     */
    public String getReturnPosition(UUID playerUuid) {
        return returnPositions.get(playerUuid);
    }
    
    /**
     * Removes and returns the saved position (used when player leaves).
     */
    public String removeReturnPosition(UUID playerUuid) {
        return returnPositions.remove(playerUuid);
    }
    
    @Override
    public String toString() {
        return "DungeonInstance{id=" + instanceId 
            + ", dungeon=" + getDungeonId()
            + ", world=" + worldName
            + ", room=" + (currentRoomIndex + 1) + "/" + getTotalRooms()
            + ", players=" + getPlayerCount()
            + ", mobs=" + remainingMobCount + "/" + spawnedMobCount
            + ", active=" + isActive() + "}";
    }
}
