package com.misa.dungeons.systems;

import com.hytale.server.ecs.Ref;
import com.hytale.server.ecs.component.DeathComponent;
import com.hytale.server.ecs.component.Player;
import com.hytale.server.ecs.query.Query;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.store.Store;
import com.hytale.server.ecs.system.CommandBuffer;
import com.hytale.server.ecs.system.RefChangeSystem;
import com.hytale.server.ecs.type.ComponentType;
import com.misa.dungeons.components.DungeonStateComponent;
import com.misa.dungeons.manager.DungeonSession;
import com.misa.dungeons.manager.SessionManager;

import java.util.logging.Logger;

/**
 * ECS system that detects when a player respawns (DeathComponent removed)
 * while in a dungeon, and resets their dead state.
 * <p>
 * Uses RefChangeSystem to react to component removal events on the DeathComponent.
 */
public class DungeonRespawnSystem extends RefChangeSystem<EntityStore, DeathComponent> {

    private static final Logger LOGGER = Logger.getLogger(DungeonRespawnSystem.class.getName());

    private final ComponentType<EntityStore, DungeonStateComponent> dungeonStateType;
    private SessionManager sessionManager;

    public DungeonRespawnSystem(ComponentType<EntityStore, DungeonStateComponent> dungeonStateType) {
        super(DeathComponent.getComponentType());
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
        return Query.and(Player.getComponentType(), dungeonStateType);
    }

    @Override
    public void onComponentRemoved(Ref ref, DeathComponent deathComponent,
                                    Store<EntityStore> store,
                                    CommandBuffer<EntityStore> commandBuffer) {
        if (sessionManager == null) return;

        DungeonStateComponent state = store.getComponent(ref, dungeonStateType);
        if (state == null) return;

        // Player has respawned — clear the dead flag
        state.setDead(false);

        DungeonSession session = sessionManager.getSession(state.getSessionId());
        if (session != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            String playerName = player != null ? player.getName() : "unknown";
            LOGGER.info("[Dungeons] Player '" + playerName + "' respawned in session "
                    + session.getSessionId());
        }
    }

    @Override
    public void onComponentAdded(Ref ref, DeathComponent component,
                                  Store<EntityStore> store,
                                  CommandBuffer<EntityStore> commandBuffer) {
        // Handled by DungeonDeathSystem
    }

    @Override
    public void onComponentSet(Ref ref, DeathComponent oldComponent, DeathComponent newComponent,
                                Store<EntityStore> store,
                                CommandBuffer<EntityStore> commandBuffer) {
        // No action needed
    }
}
