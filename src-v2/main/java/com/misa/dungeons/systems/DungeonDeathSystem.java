package com.misa.dungeons.systems;

import com.hytale.server.ecs.Ref;
import com.hytale.server.ecs.component.DeathComponent;
import com.hytale.server.ecs.component.Player;
import com.hytale.server.ecs.component.damage.Damage;
import com.hytale.server.ecs.query.Query;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.store.Store;
import com.hytale.server.ecs.system.CommandBuffer;
import com.hytale.server.ecs.system.death.DeathSystems;
import com.hytale.server.ecs.type.ComponentType;
import com.misa.dungeons.components.DungeonStateComponent;
import com.misa.dungeons.manager.DungeonSession;
import com.misa.dungeons.manager.SessionManager;

import java.util.logging.Logger;

/**
 * ECS system that reacts to player death events while in a dungeon.
 * Extends DeathSystems.OnDeathSystem to hook into the native death pipeline.
 * <p>
 * Handles:
 * <ul>
 *     <li>Tracking death count per player</li>
 *     <li>Marking player as dead (respawn pending)</li>
 *     <li>Party wipe detection (all players dead = session ends)</li>
 *     <li>Boss room death penalties</li>
 * </ul>
 */
public class DungeonDeathSystem extends DeathSystems.OnDeathSystem {

    private static final Logger LOGGER = Logger.getLogger(DungeonDeathSystem.class.getName());

    /** Maximum deaths allowed before forced session end (0 = unlimited) */
    private static final int MAX_DEATHS_BEFORE_WIPE = 0;

    /** Respawn delay in seconds */
    private static final double RESPAWN_DELAY_SECONDS = 5.0;

    private final ComponentType<EntityStore, DungeonStateComponent> dungeonStateType;
    private SessionManager sessionManager;

    public DungeonDeathSystem(ComponentType<EntityStore, DungeonStateComponent> dungeonStateType) {
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
        // Only process players who have a DungeonStateComponent (i.e., are in a dungeon)
        return Query.and(Player.getComponentType(), dungeonStateType);
    }

    @Override
    public void onComponentAdded(Ref ref, DeathComponent deathComponent,
                                  Store<EntityStore> store,
                                  CommandBuffer<EntityStore> commandBuffer) {
        if (sessionManager == null) return;

        DungeonStateComponent state = store.getComponent(ref, dungeonStateType);
        if (state == null || state.getSessionId() == null) return;

        DungeonSession session = sessionManager.getSession(state.getSessionId());
        if (session == null || !session.isActive()) return;

        // Record the death
        state.incrementDeathCount();
        state.setDead(true);

        // Extract death info
        Damage deathInfo = deathComponent.getDeathInfo();
        String causeInfo = deathInfo != null ? deathInfo.toString() : "unknown";

        Player player = store.getComponent(ref, Player.getComponentType());
        String playerName = player != null ? player.getName() : "unknown";

        LOGGER.info("[Dungeons] Player '" + playerName + "' died in dungeon '"
                + state.getDungeonId() + "' room " + state.getCurrentRoom()
                + " (deaths: " + state.getDeathCount() + ", cause: " + causeInfo + ")");

        // Check for max deaths threshold
        if (MAX_DEATHS_BEFORE_WIPE > 0 && state.getDeathCount() >= MAX_DEATHS_BEFORE_WIPE) {
            LOGGER.info("[Dungeons] Max deaths reached for player '" + playerName
                    + "', ending session");
            sessionManager.endSession(state.getSessionId());
            return;
        }

        // Check for party wipe (all players dead)
        if (isPartyWiped(session, store)) {
            LOGGER.info("[Dungeons] PARTY WIPE in session " + session.getSessionId());
            handlePartyWipe(session);
            return;
        }

        // Schedule respawn after delay
        // The respawn itself is handled by Hytale's native respawn system.
        // We just mark the player as dead so the tick system skips them
        // and reset the dead flag when DeathComponent is removed (respawn).
        LOGGER.info("[Dungeons] Player '" + playerName
                + "' awaiting respawn in " + RESPAWN_DELAY_SECONDS + "s");
    }

    /**
     * Checks if all players in the session are currently dead.
     *
     * @param session the dungeon session
     * @param store   the entity store
     * @return true if every player in the session has isDead() == true
     */
    private boolean isPartyWiped(DungeonSession session, Store<EntityStore> store) {
        for (Ref playerRef : session.getPlayers()) {
            DungeonStateComponent playerState = store.getComponent(playerRef, dungeonStateType);
            if (playerState != null && !playerState.isDead()) {
                return false; // At least one player is alive
            }
        }
        return true;
    }

    /**
     * Handles a full party wipe (all players dead simultaneously).
     * Ends the session and returns all players to the hub.
     *
     * @param session the wiped session
     */
    private void handlePartyWipe(DungeonSession session) {
        LOGGER.warning("[Dungeons] Party wipe! Ending session " + session.getSessionId()
                + " for dungeon " + session.getDungeonId());
        sessionManager.endSession(session.getSessionId());
    }
}
