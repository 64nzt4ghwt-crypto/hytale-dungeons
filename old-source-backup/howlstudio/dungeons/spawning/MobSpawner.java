package com.howlstudio.dungeons.spawning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.howlstudio.dungeons.config.DungeonTemplate;
import com.howlstudio.dungeons.manager.DungeonInstance;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/**
 * Handles spawning, tracking, and despawning of dungeon mobs.
 *
 * Each dungeon instance gets its own list of tracked entity UUIDs.
 * When a room activates, this class reads the MobSpawn configs from
 * the room definition, creates NPCEntity instances with the
 * appropriate role names, and scatters them within each spawn
 * point's configured radius.
 *
 * Room-clear checks iterate the tracked UUIDs and query the world
 * to see if the entities still exist (dead entities are removed
 * from the world automatically by the engine).
 */
public class MobSpawner {

    /** Default spread radius when none is configured on the spawn point. */
    private static final double DEFAULT_SPREAD_RADIUS = 3.0;

    /**
     * Per-instance tracking: instance UUID -> list of spawned entity UUIDs.
     * ConcurrentHashMap for thread safety across ticks / async events.
     */
    private final Map<UUID, List<UUID>> instanceMobs = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ //
    //  Spawning
    // ------------------------------------------------------------------ //

    /**
     * Spawns all mobs for a given room in a dungeon instance.
     *
     * Reads every {@link DungeonTemplate.MobSpawn} from the room definition,
     * creates {@code count} NPCEntity instances for each entry, applies a
     * random positional spread within the configured radius, and tracks
     * every spawned entity UUID on both this class and the instance itself.
     *
     * @param world     the world to spawn entities in
     * @param instance  the active dungeon instance
     * @param roomIndex the room whose mobs should be spawned
     * @return the number of mobs spawned, or 0 if the room has no spawns
     */
    @SuppressWarnings("deprecation")
    public int spawnMobsForRoom(World world, DungeonInstance instance, int roomIndex) {
        DungeonTemplate.RoomDefinition room = instance.getTemplate().getRoom(roomIndex);
        if (room == null) {
            System.err.println("[MobSpawner] Room index " + roomIndex
                    + " out of range for dungeon " + instance.getDungeonId());
            return 0;
        }

        List<DungeonTemplate.MobSpawn> spawns = room.getMobSpawns();
        if (spawns == null || spawns.isEmpty()) {
            System.out.println("[MobSpawner] Room " + room.getName()
                    + " has no mob spawns configured");
            return 0;
        }

        // Ensure tracking list exists for this instance
        List<UUID> tracked = instanceMobs.computeIfAbsent(
                instance.getInstanceId(), k -> Collections.synchronizedList(new ArrayList<>()));

        int totalSpawned = 0;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (DungeonTemplate.MobSpawn spawn : spawns) {
            String roleName = spawn.getType();
            int count = spawn.getCount();

            if (roleName == null || roleName.isEmpty()) {
                System.err.println("[MobSpawner] Skipping spawn with null/empty type in room "
                        + room.getName());
                continue;
            }

            for (int i = 0; i < count; i++) {
                try {
                    NPCEntity npc = new NPCEntity(world);
                    npc.setRoleName(roleName);

                    // Compute spawn position with random spread
                    double baseX = 0, baseY = 64, baseZ = 0;
                    // If the spawn has position data embedded (via SpawnPointConfig
                    // converted to MobSpawn during template build), we fall back to
                    // a centered position.  In the future, MobSpawn can carry coords.
                    double radius = DEFAULT_SPREAD_RADIUS;

                    double offsetX = (rng.nextDouble() * 2.0 - 1.0) * radius;
                    double offsetZ = (rng.nextDouble() * 2.0 - 1.0) * radius;
                    double spawnX = baseX + offsetX;
                    double spawnZ = baseZ + offsetZ;

                    float yaw = rng.nextFloat() * 360.0f;

                    world.spawnEntity(npc,
                            new Vector3d(spawnX, baseY, spawnZ),
                            new Vector3f(0, yaw, 0));

                    UUID entityUuid = npc.getUuid();
                    tracked.add(entityUuid);
                    instance.trackMob(entityUuid);
                    instance.onMobSpawned();
                    totalSpawned++;

                } catch (Exception e) {
                    System.err.println("[MobSpawner] Failed to spawn " + roleName
                            + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[MobSpawner] Spawned " + totalSpawned + " mobs in room "
                + room.getName() + " for instance " + instance.getInstanceId());
        return totalSpawned;
    }

    /**
     * Spawns mobs for a room using explicit spawn-point positions from
     * {@link com.howlstudio.dungeons.config.SpawnPointConfig} entries.
     *
     * This variant is used when the dungeon was created from a registered
     * world that stores per-spawn-point coordinates and radii.
     *
     * @param world        the world to spawn in
     * @param instance     the active dungeon instance
     * @param roomIndex    the room index
     * @param spawnPoints  explicit spawn-point configs with x/y/z + radius
     * @return the number of mobs spawned
     */
    @SuppressWarnings("deprecation")
    public int spawnMobsAtPoints(World world, DungeonInstance instance, int roomIndex,
                                 List<com.howlstudio.dungeons.config.SpawnPointConfig> spawnPoints) {
        if (spawnPoints == null || spawnPoints.isEmpty()) {
            return spawnMobsForRoom(world, instance, roomIndex);
        }

        List<UUID> tracked = instanceMobs.computeIfAbsent(
                instance.getInstanceId(), k -> Collections.synchronizedList(new ArrayList<>()));

        int totalSpawned = 0;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (com.howlstudio.dungeons.config.SpawnPointConfig sp : spawnPoints) {
            String roleName = sp.getEntity();
            if (roleName == null || roleName.isEmpty()) continue;

            // Probability gate
            if (sp.getChance() < 100 && rng.nextInt(100) >= sp.getChance()) {
                continue;
            }

            int count = sp.getAmount();
            double radius = sp.getRadius() > 0 ? sp.getRadius() : DEFAULT_SPREAD_RADIUS;

            for (int i = 0; i < count; i++) {
                try {
                    NPCEntity npc = new NPCEntity(world);
                    npc.setRoleName(roleName);

                    double offsetX = (rng.nextDouble() * 2.0 - 1.0) * radius;
                    double offsetZ = (rng.nextDouble() * 2.0 - 1.0) * radius;

                    float yaw = rng.nextFloat() * 360.0f;

                    world.spawnEntity(npc,
                            new Vector3d(sp.getX() + offsetX, sp.getY(), sp.getZ() + offsetZ),
                            new Vector3f(0, yaw, 0));

                    UUID entityUuid = npc.getUuid();
                    tracked.add(entityUuid);
                    instance.trackMob(entityUuid);
                    instance.onMobSpawned();
                    totalSpawned++;

                } catch (Exception e) {
                    System.err.println("[MobSpawner] Failed to spawn " + roleName
                            + " at (" + sp.positionString() + "): " + e.getMessage());
                }
            }
        }

        System.out.println("[MobSpawner] Spawned " + totalSpawned
                + " mobs (explicit points) for instance " + instance.getInstanceId());
        return totalSpawned;
    }

    // ------------------------------------------------------------------ //
    //  Room-cleared check
    // ------------------------------------------------------------------ //

    /**
     * Checks whether all mobs for the current room of a dungeon instance are dead.
     *
     * An entity is considered dead when the world no longer contains it (the
     * engine removes dead entities). For every missing entity the instance's
     * remaining-mob count is decremented.
     *
     * If all tracked mobs are gone the instance is marked as room-cleared.
     *
     * @param world    the world the dungeon lives in
     * @param instance the active dungeon instance
     * @return {@code true} if the room is now cleared
     */
    @SuppressWarnings("deprecation")
    public boolean checkRoomCleared(World world, DungeonInstance instance) {
        List<UUID> tracked = instanceMobs.get(instance.getInstanceId());
        if (tracked == null || tracked.isEmpty()) {
            // No mobs were ever spawned - room is trivially cleared
            instance.setRoomCleared(true);
            return true;
        }

        // Iterate a snapshot so we can modify the list safely
        List<UUID> snapshot = new ArrayList<>(tracked);
        int removed = 0;

        for (UUID entityUuid : snapshot) {
            Entity entity = world.getEntity(entityUuid);
            if (entity == null) {
                // Entity no longer in world -> dead / despawned
                tracked.remove(entityUuid);
                instance.onMobKilled();
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("[MobSpawner] " + removed + " mob(s) confirmed dead in instance "
                    + instance.getInstanceId() + " (" + instance.getRemainingMobCount() + " remaining)");
        }

        boolean cleared = tracked.isEmpty() || instance.areAllMobsDead();
        if (cleared) {
            instance.setRoomCleared(true);
        }
        return cleared;
    }

    // ------------------------------------------------------------------ //
    //  Despawning / cleanup
    // ------------------------------------------------------------------ //

    /**
     * Forcefully removes all remaining mobs belonging to a dungeon instance.
     * Called when a dungeon ends (completed, failed, or force-stopped).
     *
     * @param world    the world the dungeon lives in
     * @param instance the dungeon instance to clean up
     * @return the number of entities that were despawned
     */
    @SuppressWarnings("deprecation")
    public int despawnAll(World world, DungeonInstance instance) {
        List<UUID> tracked = instanceMobs.remove(instance.getInstanceId());
        if (tracked == null || tracked.isEmpty()) {
            return 0;
        }

        int despawned = 0;
        for (UUID entityUuid : tracked) {
            try {
                Entity entity = world.getEntity(entityUuid);
                if (entity != null) {
                    // Try NPC-specific despawn first, then hard remove as fallback
                    if (entity instanceof com.hypixel.hytale.server.npc.entities.NPCEntity) {
                        ((com.hypixel.hytale.server.npc.entities.NPCEntity) entity).setToDespawn();
                    } else {
                        entity.remove();
                    }
                    despawned++;
                }
            } catch (Exception e) {
                System.err.println("[MobSpawner] Error despawning entity "
                        + entityUuid + ": " + e.getMessage());
            }
        }

        instance.clearSpawnedMobs();
        System.out.println("[MobSpawner] Despawned " + despawned
                + " remaining mobs for instance " + instance.getInstanceId());
        return despawned;
    }

    // ------------------------------------------------------------------ //
    //  Queries
    // ------------------------------------------------------------------ //

    /**
     * Gets the list of tracked entity UUIDs for an instance.
     *
     * @param instanceId the dungeon instance UUID
     * @return unmodifiable list of entity UUIDs, or empty list if none
     */
    public List<UUID> getTrackedMobs(UUID instanceId) {
        List<UUID> tracked = instanceMobs.get(instanceId);
        if (tracked == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(tracked));
    }

    /**
     * Checks if this spawner is tracking any mobs for the given instance.
     */
    public boolean hasTrackedMobs(UUID instanceId) {
        List<UUID> tracked = instanceMobs.get(instanceId);
        return tracked != null && !tracked.isEmpty();
    }

    /**
     * Removes all tracking data without despawning entities.
     * Used when ownership is transferred or during hot-reload.
     */
    public void clearTracking(UUID instanceId) {
        instanceMobs.remove(instanceId);
    }

    /**
     * Clears all tracking data for all instances.
     */
    public void clearAll() {
        instanceMobs.clear();
    }
}
