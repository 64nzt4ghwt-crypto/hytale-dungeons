package com.howlstudio.dungeons.manager;

/**
 * Tracks the state of a room within a dungeon instance.
 * 
 * Lifecycle: LOCKED -> ACTIVE -> CLEARED
 * 
 * LOCKED  - Room is not yet reachable. Barriers/doors are closed.
 * ACTIVE  - Players have entered the room. Mobs spawn, doors lock behind them.
 * CLEARED - All mobs killed (or puzzle solved). Barriers removed, loot spawned.
 */
public enum RoomState {
    /** Room is locked and inaccessible */
    LOCKED,
    /** Room is active - mobs spawned, players fighting */
    ACTIVE,
    /** Room is cleared - all mobs dead, barriers removed */
    CLEARED
}
