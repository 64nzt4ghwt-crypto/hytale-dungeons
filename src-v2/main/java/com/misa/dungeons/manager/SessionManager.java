package com.misa.dungeons.manager;

import com.hytale.server.ecs.Ref;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.store.Store;
import com.hytale.server.ecs.component.Player;
import com.hytale.server.ecs.component.TransformComponent;
import com.hytale.server.plugin.instances.InstancesPlugin;
import com.hytale.server.world.World;
import com.hytale.server.transform.Transform;
import com.misa.dungeons.components.DungeonStateComponent;
import com.misa.dungeons.config.DungeonConfig;
import com.misa.dungeons.config.RoomConfig;
import com.misa.dungeons.config.WaveConfig;

import org.joml.Vector3d;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of all active DungeonSessions.
 * Handles creating instances, adding/removing players, and cleanup.
 * <p>
 * Thread-safe: uses ConcurrentHashMaps and delegates world mutations to world.execute().
 */
public class SessionManager {

    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    /** Active sessions keyed by session ID */
    private final Map<UUID, DungeonSession> sessions = new ConcurrentHashMap<>();

    /** Lookup: player ref -> session ID for fast retrieval */
    private final Map<Ref, UUID> playerSessionMap = new ConcurrentHashMap<>();

    /** Lookup: world -> session ID for world-based lookups */
    private final Map<World, UUID> worldSessionMap = new ConcurrentHashMap<>();

    private final DungeonRegistry registry;

    public SessionManager(DungeonRegistry registry) {
        this.registry = registry;
    }

    /**
     * Creates a new dungeon session, spawns the instance, and teleports the player.
     *
     * @param dungeonId    the dungeon config ID
     * @param playerRef    the initiating player's entity ref
     * @param store        the player's component accessor
     * @param currentWorld the world the player is currently in
     * @return CompletableFuture that completes with the session, or exceptionally on failure
     */
    public CompletableFuture<DungeonSession> createSession(String dungeonId, Ref playerRef,
                                                            Store<EntityStore> store,
                                                            World currentWorld) {
        DungeonConfig config = registry.getConfig(dungeonId);
        if (config == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unknown dungeon: " + dungeonId));
        }

        // Check if player is already in a session
        if (playerSessionMap.containsKey(playerRef)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Player is already in a dungeon session"));
        }

        UUID sessionId = UUID.randomUUID();
        DungeonSession session = new DungeonSession(sessionId, config);

        // Build return point from player's current transform
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Transform returnPoint = new Transform(
                transform != null ? transform.getPosition() : new Vector3d(0, 64, 0)
        );

        // Spawn the instance
        CompletableFuture<World> worldFuture = InstancesPlugin.get()
                .spawnInstance(config.getInstanceTemplate(), currentWorld, returnPoint);

        // Teleport player to loading screen while instance loads
        InstancesPlugin.teleportPlayerToLoadingInstance(playerRef, store, worldFuture, null);

        return worldFuture.thenApply(instanceWorld -> {
            session.setInstanceWorld(instanceWorld);
            session.addPlayer(playerRef);

            // Register session in all lookups
            sessions.put(sessionId, session);
            playerSessionMap.put(playerRef, sessionId);
            worldSessionMap.put(instanceWorld, sessionId);

            // Attach DungeonStateComponent to the player entity
            attachDungeonState(playerRef, store, session);

            // Spawn first room's first wave
            spawnCurrentWave(session);

            LOGGER.info("[Dungeons] Created session " + sessionId + " for dungeon "
                    + dungeonId + " (player joined)");
            return session;
        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "[Dungeons] Failed to create session for " + dungeonId, ex);
            return null;
        });
    }

    /**
     * Adds an additional player to an existing session (party join).
     *
     * @param sessionId the session to join
     * @param playerRef the player ref
     * @param store     the player's component accessor
     * @return true if joined successfully
     */
    public boolean joinSession(UUID sessionId, Ref playerRef, Store<EntityStore> store) {
        DungeonSession session = sessions.get(sessionId);
        if (session == null || !session.isActive()) {
            return false;
        }

        if (session.getPlayerCount() >= session.getConfig().getMaxPlayers()) {
            return false;
        }

        if (playerSessionMap.containsKey(playerRef)) {
            return false;
        }

        World instanceWorld = session.getInstanceWorld();
        if (instanceWorld == null) {
            return false;
        }

        // Teleport player to the instance
        InstancesPlugin.teleportPlayerToInstance(playerRef, store, instanceWorld, null);

        session.addPlayer(playerRef);
        playerSessionMap.put(playerRef, sessionId);

        // Attach dungeon state
        attachDungeonState(playerRef, store, session);

        LOGGER.info("[Dungeons] Player joined session " + sessionId);
        return true;
    }

    /**
     * Removes a player from their current session.
     * If the session becomes empty, triggers cleanup.
     *
     * @param playerRef the player ref
     * @param store     the player's component accessor
     */
    public void removePlayer(Ref playerRef, Store<EntityStore> store) {
        UUID sessionId = playerSessionMap.remove(playerRef);
        if (sessionId == null) return;

        DungeonSession session = sessions.get(sessionId);
        if (session == null) return;

        session.removePlayer(playerRef);

        // Remove the DungeonStateComponent
        detachDungeonState(playerRef, store);

        // Exit the instance (teleports back to return point)
        InstancesPlugin.exitInstance(playerRef, store);

        LOGGER.info("[Dungeons] Player removed from session " + sessionId);

        // If session is now empty, clean up
        if (session.isEmpty()) {
            endSession(sessionId);
        }
    }

    /**
     * Ends a session: despawns mobs, removes instance, cleans up tracking.
     *
     * @param sessionId the session to end
     */
    public void endSession(UUID sessionId) {
        DungeonSession session = sessions.get(sessionId);
        if (session == null) return;

        if (session.isEnding()) return; // prevent double-cleanup
        session.setEnding(true);

        LOGGER.info("[Dungeons] Ending session " + sessionId + " (dungeon: "
                + session.getDungeonId() + ")");

        World instanceWorld = session.getInstanceWorld();

        // Despawn remaining mobs
        if (instanceWorld != null) {
            MobSpawner.despawnAllMobs(instanceWorld, session);
        }

        // Remove all remaining players from the session
        Store<EntityStore> store = instanceWorld != null
                ? instanceWorld.getEntityStore().getStore() : null;

        for (Ref playerRef : session.getPlayers()) {
            playerSessionMap.remove(playerRef);
            if (store != null) {
                detachDungeonState(playerRef, store);
                InstancesPlugin.exitInstance(playerRef, store);
            }
        }

        // Remove instance world
        if (instanceWorld != null) {
            worldSessionMap.remove(instanceWorld);
            InstancesPlugin.safeRemoveInstance(instanceWorld);
        }

        sessions.remove(sessionId);

        LOGGER.info("[Dungeons] Session " + sessionId + " fully cleaned up");
    }

    /**
     * Spawns the current wave of mobs for a session based on its room/wave state.
     *
     * @param session the session
     */
    public void spawnCurrentWave(DungeonSession session) {
        WaveConfig wave = session.getCurrentWaveConfig();
        if (wave == null) return;

        World world = session.getInstanceWorld();
        if (world == null) return;

        // Use the center of the instance world as spawn anchor (0, 64, 0)
        // In production, this would come from room-specific spawn points in the prefab
        Vector3d spawnCenter = new Vector3d(0.0, 64.0, 0.0);

        int spawned = MobSpawner.spawnWave(world, wave.getMobs(), spawnCenter, session);

        LOGGER.info("[Dungeons] Session " + session.getSessionId()
                + " room " + session.getCurrentRoom()
                + " wave " + session.getCurrentWave()
                + " spawned " + spawned + " mobs");
    }

    /**
     * Attaches a DungeonStateComponent to a player entity.
     */
    private void attachDungeonState(Ref playerRef, Store<EntityStore> store,
                                     DungeonSession session) {
        DungeonStateComponent state = new DungeonStateComponent();
        state.setSessionId(session.getSessionId());
        state.setDungeonId(session.getDungeonId());
        state.setCurrentRoom(session.getCurrentRoom());
        state.setCurrentWave(session.getCurrentWave());
        state.setStartTime(session.getStartTimeMillis());
        state.setTrackedMobs(session.getAliveMobs());

        RoomConfig room = session.getCurrentRoomConfig();
        if (room != null) {
            WaveConfig wave = session.getCurrentWaveConfig();
            state.setMobsRemaining(wave != null ? wave.getTotalMobCount() : room.getTotalMobCount());
        }

        store.setComponent(playerRef, DungeonStateComponent.getComponentType(), state);
    }

    /**
     * Removes the DungeonStateComponent from a player entity.
     */
    private void detachDungeonState(Ref playerRef, Store<EntityStore> store) {
        store.removeComponent(playerRef, DungeonStateComponent.getComponentType());
    }

    // ---- queries ----

    /**
     * Returns the session a player is currently in, or null.
     */
    public DungeonSession getSessionForPlayer(Ref playerRef) {
        UUID sessionId = playerSessionMap.get(playerRef);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    /**
     * Returns the session running in a given world, or null.
     */
    public DungeonSession getSessionForWorld(World world) {
        UUID sessionId = worldSessionMap.get(world);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    /**
     * Returns the session by ID, or null.
     */
    public DungeonSession getSession(UUID sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Returns all active sessions (unmodifiable).
     */
    public Collection<DungeonSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * Returns the number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Checks if a player is currently in any dungeon session.
     */
    public boolean isPlayerInDungeon(Ref playerRef) {
        return playerSessionMap.containsKey(playerRef);
    }

    /**
     * Returns the DungeonRegistry used by this manager.
     */
    public DungeonRegistry getRegistry() {
        return registry;
    }
}
