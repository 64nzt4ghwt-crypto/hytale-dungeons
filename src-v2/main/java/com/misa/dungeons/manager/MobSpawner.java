package com.misa.dungeons.manager;

import com.hytale.server.ecs.Ref;
import com.hytale.server.ecs.component.BoundingBox;
import com.hytale.server.ecs.component.ModelComponent;
import com.hytale.server.ecs.component.NetworkId;
import com.hytale.server.ecs.component.PersistentModel;
import com.hytale.server.ecs.component.TransformComponent;
import com.hytale.server.ecs.component.UUIDComponent;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.type.Holder;
import com.hytale.server.ecs.store.Store;
import com.hytale.server.model.Model;
import com.hytale.server.model.ModelAsset;
import com.hytale.server.world.World;
import com.hytale.server.ecs.AddReason;
import com.misa.dungeons.config.MobSpawnEntry;

import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class responsible for spawning mob entities into a dungeon instance world.
 * All entity creation runs inside world.execute() for thread safety.
 */
public class MobSpawner {

    private static final Logger LOGGER = Logger.getLogger(MobSpawner.class.getName());

    /**
     * Spawns mobs defined by a MobSpawnEntry into the given world at the specified center point.
     * Mobs are spread within spawnRadius around the center.
     * <p>
     * MUST be called from an appropriate context; internally delegates to world.execute().
     *
     * @param world       the instance world
     * @param entry       mob spawn configuration
     * @param center      center position for the spawn group
     * @param session     the active session (for mob tracking)
     * @return list of spawned entity Refs (populated after world.execute completes)
     */
    public static List<Ref> spawnMobs(World world, MobSpawnEntry entry, Vector3d center,
                                       DungeonSession session) {
        List<Ref> spawnedRefs = new ArrayList<>();

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(entry.getEntityType());
        if (modelAsset == null) {
            LOGGER.warning("[Dungeons] Unknown entity model: " + entry.getEntityType());
            return spawnedRefs;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();

        world.execute(() -> {
            for (int i = 0; i < entry.getCount(); i++) {
                try {
                    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

                    Model model = Model.createScaledModel(modelAsset, 1.0f);

                    // Calculate spawn position with radius spread
                    Vector3d spawnPos = calculateSpawnPosition(center, entry.getSpawnRadius());
                    Vector3f rotation = new Vector3f(
                            0.0f,
                            (float) (ThreadLocalRandom.current().nextDouble() * 360.0),
                            0.0f
                    );

                    // Required components for a visible, networked entity
                    holder.addComponent(
                            TransformComponent.getComponentType(),
                            new TransformComponent(spawnPos, rotation)
                    );
                    holder.addComponent(
                            PersistentModel.getComponentType(),
                            new PersistentModel(model.toReference())
                    );
                    holder.addComponent(
                            ModelComponent.getComponentType(),
                            new ModelComponent(model)
                    );
                    holder.addComponent(
                            BoundingBox.getComponentType(),
                            new BoundingBox(model.getBoundingBox())
                    );
                    holder.addComponent(
                            NetworkId.getComponentType(),
                            new NetworkId(store.getExternalData().takeNextNetworkId())
                    );

                    // Ensure UUID is generated
                    holder.ensureComponent(UUIDComponent.getComponentType());

                    // Add to world
                    Ref ref = store.addEntity(holder, AddReason.SPAWN);

                    if (ref != null) {
                        spawnedRefs.add(ref);
                        if (session != null) {
                            session.trackMob(ref);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING,
                            "[Dungeons] Failed to spawn " + entry.getEntityType(), e);
                }
            }
        });

        LOGGER.info("[Dungeons] Spawned " + entry.getCount() + "x " + entry.getEntityType()
                + " at " + formatVec(center));
        return spawnedRefs;
    }

    /**
     * Spawns all mobs from a list of entries at the given center point.
     *
     * @param world    the instance world
     * @param entries  list of mob spawn entries
     * @param center   center position for spawning
     * @param session  the active session
     * @return total number of mobs spawned
     */
    public static int spawnWave(World world, List<MobSpawnEntry> entries, Vector3d center,
                                DungeonSession session) {
        int total = 0;
        for (MobSpawnEntry entry : entries) {
            List<Ref> refs = spawnMobs(world, entry, center, session);
            total += refs.size();
        }
        return total;
    }

    /**
     * Removes all alive mobs tracked by a session from the world.
     * Used during cleanup or wipe scenarios.
     *
     * @param world   the instance world
     * @param session the session whose mobs to despawn
     */
    public static void despawnAllMobs(World world, DungeonSession session) {
        if (session.getAliveMobs().isEmpty()) {
            return;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();

        world.execute(() -> {
            for (Ref mobRef : session.getAliveMobs()) {
                try {
                    store.removeEntity(mobRef);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "[Dungeons] Could not remove mob ref: " + mobRef, e);
                }
            }
            session.getAliveMobs().clear();
        });

        LOGGER.info("[Dungeons] Despawned all mobs for session: " + session.getSessionId());
    }

    /**
     * Calculate a spawn position within a radius around a center point.
     * Uses uniform distribution in a circle on the XZ plane, keeping Y constant.
     */
    private static Vector3d calculateSpawnPosition(Vector3d center, double radius) {
        if (radius <= 0.0) {
            return new Vector3d(center);
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double dist = Math.sqrt(rng.nextDouble()) * radius; // sqrt for uniform distribution
        double offsetX = Math.cos(angle) * dist;
        double offsetZ = Math.sin(angle) * dist;

        return new Vector3d(
                center.x + offsetX,
                center.y,
                center.z + offsetZ
        );
    }

    private static String formatVec(Vector3d v) {
        return String.format("(%.1f, %.1f, %.1f)", v.x, v.y, v.z);
    }
}
