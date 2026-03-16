package com.howlstudio.dungeons.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Persistent component storing a player's dungeon progression data.
 * Attached to player entities and automatically saved/loaded by Hytale's ECS.
 * 
 * Tracks:
 * - Lifetime stats (dungeons completed, total clears, best times)
 * - Current dungeon state (for reconnect support)
 * - Per-dungeon-type statistics
 * 
 * Similar to how Orbis stores race data, this stays with the player across sessions.
 */
public class DungeonData implements Component<EntityStore> {

    /**
     * Codec for serialization. Hytale uses this to persist across restarts.
     */
    public static final BuilderCodec<DungeonData> CODEC;

    private int dungeonsCompleted;
    private String currentDungeonId;
    private int currentRoomIndex;
    private long sessionStartTime;
    private int totalClears;
    private long bestClearTimeMillis;
    private boolean markedForCleanup;
    
    public DungeonData() {
        this.dungeonsCompleted = 0;
        this.currentDungeonId = null;
        this.currentRoomIndex = 0;
        this.sessionStartTime = 0;
        this.totalClears = 0;
        this.bestClearTimeMillis = Long.MAX_VALUE;
        this.markedForCleanup = false;
    }
    
    // ---- Lifetime stats ----
    
    public int getDungeonsCompleted() {
        return dungeonsCompleted;
    }
    
    public void setDungeonsCompleted(int count) {
        this.dungeonsCompleted = count;
    }
    
    public void incrementDungeonsCompleted() {
        this.dungeonsCompleted++;
        this.totalClears++;
    }
    
    public int getTotalClears() {
        return totalClears;
    }
    
    public void setTotalClears(int total) {
        this.totalClears = total;
    }
    
    public long getBestClearTimeMillis() {
        return bestClearTimeMillis == Long.MAX_VALUE ? 0 : bestClearTimeMillis;
    }
    
    public void setBestClearTimeMillis(long time) {
        this.bestClearTimeMillis = time;
    }
    
    /**
     * Updates best time if the new time is better.
     * @param clearTimeMillis time taken to clear the dungeon
     */
    public void updateBestTime(long clearTimeMillis) {
        if (clearTimeMillis < this.bestClearTimeMillis) {
            this.bestClearTimeMillis = clearTimeMillis;
        }
    }
    
    // ---- Current session tracking ----
    
    public String getCurrentDungeonId() {
        return currentDungeonId;
    }
    
    public void setCurrentDungeonId(String dungeonId) {
        this.currentDungeonId = dungeonId;
    }
    
    public boolean isInDungeon() {
        return currentDungeonId != null && !currentDungeonId.isEmpty();
    }
    
    public int getCurrentRoomIndex() {
        return currentRoomIndex;
    }
    
    public void setCurrentRoomIndex(int index) {
        this.currentRoomIndex = index;
    }
    
    public long getSessionStartTime() {
        return sessionStartTime;
    }
    
    public void setSessionStartTime(long time) {
        this.sessionStartTime = time;
    }
    
    /**
     * Clears current session data. Called when dungeon ends or player leaves.
     */
    public void clearSession() {
        this.currentDungeonId = null;
        this.currentRoomIndex = 0;
        this.sessionStartTime = 0;
        this.markedForCleanup = false;
    }
    
    public boolean isMarkedForCleanup() {
        return markedForCleanup;
    }
    
    public void setMarkedForCleanup(boolean marked) {
        this.markedForCleanup = marked;
    }
    
    /**
     * Calculates elapsed time for current session.
     * @return elapsed milliseconds, or 0 if not in session
     */
    public long getElapsedSessionTime() {
        if (sessionStartTime == 0) return 0;
        return System.currentTimeMillis() - sessionStartTime;
    }
    
    @Override
    public Component<EntityStore> clone() {
        DungeonData copy = new DungeonData();
        copy.dungeonsCompleted = this.dungeonsCompleted;
        copy.currentDungeonId = this.currentDungeonId;
        copy.currentRoomIndex = this.currentRoomIndex;
        copy.sessionStartTime = this.sessionStartTime;
        copy.totalClears = this.totalClears;
        copy.bestClearTimeMillis = this.bestClearTimeMillis;
        copy.markedForCleanup = this.markedForCleanup;
        return copy;
    }
    
    @Override
    public String toString() {
        return "DungeonData{completed=" + dungeonsCompleted 
            + ", clears=" + totalClears 
            + ", best=" + bestClearTimeMillis + "ms"
            + ", current=" + currentDungeonId 
            + ", room=" + currentRoomIndex + "}";
    }
    
    // Build the codec for Hytale's persistence system
    static {
        CODEC = BuilderCodec.builder(DungeonData.class, DungeonData::new)
            .append(new KeyedCodec<>("DungeonsCompleted", Codec.INTEGER),
                DungeonData::setDungeonsCompleted,
                DungeonData::getDungeonsCompleted).add()
            .append(new KeyedCodec<>("CurrentDungeonId", Codec.STRING),
                DungeonData::setCurrentDungeonId,
                DungeonData::getCurrentDungeonId).add()
            .append(new KeyedCodec<>("CurrentRoomIndex", Codec.INTEGER),
                DungeonData::setCurrentRoomIndex,
                DungeonData::getCurrentRoomIndex).add()
            .append(new KeyedCodec<>("SessionStartTime", Codec.LONG),
                DungeonData::setSessionStartTime,
                DungeonData::getSessionStartTime).add()
            .append(new KeyedCodec<>("TotalClears", Codec.INTEGER),
                DungeonData::setTotalClears,
                DungeonData::getTotalClears).add()
            .append(new KeyedCodec<>("BestClearTime", Codec.LONG),
                DungeonData::setBestClearTimeMillis,
                DungeonData::getBestClearTimeMillis).add()
            .build();
    }
}
