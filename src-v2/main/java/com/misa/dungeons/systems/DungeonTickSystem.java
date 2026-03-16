package com.misa.dungeons.systems;

import com.hytale.server.ecs.Ref;
import com.hytale.server.ecs.component.Player;
import com.hytale.server.ecs.query.Query;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.store.Store;
import com.hytale.server.ecs.system.EntityTickingSystem;
import com.hytale.server.ecs.system.CommandBuffer;
import com.hytale.server.ecs.system.SystemGroup;
import com.hytale.server.ecs.type.ComponentType;
import com.hytale.server.module.DamageModule;
import com.misa.dungeons.components.DungeonStateComponent;
import com.misa.dungeons.config.RoomConfig;
import com.misa.dungeons.config.WaveConfig;
import com.misa.dungeons.manager.DungeonSession;
import com.misa.dungeons.manager.SessionManager;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * ECS system that ticks every frame for each player who has a DungeonStateComponent.
 * Responsible for:
 * <ul>
 *     <li>Tracking room timers</li>
 *     <li>Detecting when all mobs in a wave/room are dead</li>
 *     <li>Triggering wave spawns after delays</li>
 *     <li>Advancing rooms when cleared</li>
 *     <li>Detecting dungeon completion</li>
 *     <li>Enforcing time limits</li>
 * </ul>
 * <p>
 * Placed AFTER the damage pipeline so death/health changes are already processed.
 */
public class DungeonTickSystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(DungeonTickSystem.class.getName());

    private final ComponentType<EntityStore, DungeonStateComponent> dungeonStateType;
    private SessionManager sessionManager;

    public DungeonTickSystem(ComponentType<EntityStore, DungeonStateComponent> dungeonStateType) {
        this.dungeonStateType = dungeonStateType;
    }

    /**
     * Must be called after plugin start to wire the session manager.
     */
    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(dungeonStateType, Player.getComponentType());
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        // Run after the damage pipeline has fully resolved
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    public void tick(Ref ref, Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                     float dt) {
        if (sessionManager == null) return;

        DungeonStateComponent state = store.getComponent(ref, dungeonStateType);
        if (state == null || state.getSessionId() == null) return;

        // Don't tick dead players
        if (state.isDead()) return;

        DungeonSession session = sessionManager.getSession(state.getSessionId());
        if (session == null || !session.isActive()) return;

        // Accumulate room timer
        state.addRoomElapsedTime(dt);
        session.addRoomTimer(dt);
        session.addWaveTimer(dt);

        // ---- Check dungeon-global time limit ----
        if (session.isTimeLimitExceeded()) {
            handleTimeLimitExceeded(session, ref, store);
            return;
        }

        // ---- Check room time limit (for timed rooms) ----
        RoomConfig roomConfig = session.getCurrentRoomConfig();
        if (roomConfig != null && roomConfig.isTimed() && session.isRoomTimeLimitExceeded()) {
            handleRoomTimeLimitExceeded(session, ref, store, roomConfig);
            return;
        }

        // ---- Prune dead mobs from the alive set ----
        pruneDeadMobs(session, store);

        // ---- Check if current wave's delay has elapsed for next wave spawn ----
        if (!session.isAllWavesSpawned() && session.hasNextWave()) {
            WaveConfig nextWave = getNextWaveConfig(session);
            if (nextWave != null && session.getWaveTimer() >= nextWave.getSpawnDelay()) {
                session.advanceWave();
                sessionManager.spawnCurrentWave(session);
                syncStateFromSession(state, session);
            }
        }

        // ---- Check if room is cleared (all waves spawned + all mobs dead) ----
        if (session.isAllWavesSpawned() && session.allMobsDead()) {
            if (!state.isRoomCleared()) {
                state.setRoomCleared(true);
                handleRoomCleared(session, ref, store, state, commandBuffer);
            }
        }

        // Sync mob count to component for HUD display
        state.setMobsRemaining(session.getAliveMobCount());
    }

    /**
     * Called when all mobs in a room are dead and all waves have been spawned.
     */
    private void handleRoomCleared(DungeonSession session, Ref playerRef,
                                    Store<EntityStore> store,
                                    DungeonStateComponent state,
                                    CommandBuffer<EntityStore> commandBuffer) {
        LOGGER.info("[Dungeons] Room " + session.getCurrentRoom() + " cleared in session "
                + session.getSessionId());

        if (session.hasNextRoom()) {
            // Advance to next room
            session.advanceRoom();
            syncStateFromSession(state, session);
            state.setRoomCleared(false);
            state.setRoomElapsedTime(0.0);

            // Spawn first wave of the new room
            sessionManager.spawnCurrentWave(session);

            LOGGER.info("[Dungeons] Advanced to room " + session.getCurrentRoom()
                    + " in session " + session.getSessionId());
        } else {
            // Dungeon complete!
            handleDungeonComplete(session, playerRef, store, state);
        }
    }

    /**
     * Called when all rooms have been cleared.
     */
    private void handleDungeonComplete(DungeonSession session, Ref playerRef,
                                        Store<EntityStore> store,
                                        DungeonStateComponent state) {
        session.setCompleted(true);

        double totalTime = session.getTotalElapsedSeconds();
        LOGGER.info("[Dungeons] Session " + session.getSessionId() + " COMPLETED! Dungeon: "
                + session.getDungeonId() + " Time: " + String.format("%.1f", totalTime) + "s"
                + " Deaths: " + state.getDeathCount());

        // Distribute loot for the final room
        RoomConfig finalRoom = session.getCurrentRoomConfig();
        if (finalRoom != null && finalRoom.getLootTable() != null) {
            distributeLoot(session, finalRoom.getLootTable());
        }

        // End the session (teleports players back, cleans up instance)
        sessionManager.endSession(session.getSessionId());
    }

    /**
     * Called when the dungeon-global time limit is exceeded.
     */
    private void handleTimeLimitExceeded(DungeonSession session, Ref playerRef,
                                          Store<EntityStore> store) {
        LOGGER.warning("[Dungeons] Time limit exceeded for session " + session.getSessionId());
        sessionManager.endSession(session.getSessionId());
    }

    /**
     * Called when a timed room's time limit is exceeded.
     */
    private void handleRoomTimeLimitExceeded(DungeonSession session, Ref playerRef,
                                              Store<EntityStore> store, RoomConfig roomConfig) {
        LOGGER.warning("[Dungeons] Room time limit exceeded for room " + session.getCurrentRoom()
                + " in session " + session.getSessionId());
        // Wipe the session on timed room failure
        sessionManager.endSession(session.getSessionId());
    }

    /**
     * Removes dead mob refs from the session's alive set.
     * Checks if the entity still exists and has no DeathComponent.
     */
    private void pruneDeadMobs(DungeonSession session, Store<EntityStore> store) {
        session.getAliveMobs().removeIf(mobRef -> {
            try {
                // If the entity no longer exists in the store, it's dead/removed
                return !store.hasEntity(mobRef);
            } catch (Exception e) {
                // Entity invalid, remove from tracking
                return true;
            }
        });
    }

    /**
     * Returns the next wave config (the one after current), or null.
     */
    private WaveConfig getNextWaveConfig(DungeonSession session) {
        RoomConfig room = session.getCurrentRoomConfig();
        if (room == null) return null;
        int nextIndex = session.getCurrentWave() + 1;
        if (nextIndex >= room.getWaves().size()) return null;
        return room.getWaves().get(nextIndex);
    }

    /**
     * Synchronizes the player's DungeonStateComponent with the shared session state.
     */
    private void syncStateFromSession(DungeonStateComponent state, DungeonSession session) {
        state.setCurrentRoom(session.getCurrentRoom());
        state.setCurrentWave(session.getCurrentWave());
        state.setMobsRemaining(session.getAliveMobCount());
    }

    /**
     * Distributes loot to all players in the session.
     * Placeholder: actual implementation would use Hytale's loot table system.
     */
    private void distributeLoot(DungeonSession session, String lootTableId) {
        LOGGER.info("[Dungeons] Distributing loot table '" + lootTableId
                + "' to " + session.getPlayerCount() + " players in session "
                + session.getSessionId());

        // Loot distribution would integrate with Hytale's inventory/item system.
        // Each player in session.getPlayers() would receive items from the loot table.
        // This depends on the server's LootTable API which handles rarity tiers and rolls.
        for (Ref playerRef : session.getPlayers()) {
            // LootTable.distribute(playerRef, lootTableId);
            LOGGER.fine("[Dungeons] Loot distributed to player ref: " + playerRef);
        }
    }
}
