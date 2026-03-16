package com.howlstudio.dungeons.systems;

import java.util.Set;
import java.util.UUID;

import com.howlstudio.dungeons.components.DungeonData;
import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.portal.PortalManager;
import com.howlstudio.dungeons.spawning.MobSpawner;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Periodic tick system for dungeon state management.
 * 
 * Hooks into the damage event system (runs every frame on damage events)
 * and throttles to check every 5 seconds:
 * 
 * 1. Checks for expired dungeon instances (time limit)
 * 2. Checks if all mobs in current room are dead (room clear)
 * 3. Advances to next room or completes dungeon
 * 4. Spawns mobs for new rooms
 * 5. Notifies players of state changes
 */
public class DungeonTickSystem extends DamageEventSystem {
    
    private static final Query<EntityStore> QUERY = AllLegacyLivingEntityTypesQuery.INSTANCE;
    
    private final ComponentType<EntityStore, DungeonData> dungeonDataType;
    private DungeonManager dungeonManager;
    private MobSpawner mobSpawner;
    private PortalManager portalManager;
    private long lastCheckMillis = 0;
    private long lastPortalCheckMillis = 0;
    
    /** Portal checks every 2 seconds */
    private static final long PORTAL_CHECK_INTERVAL_MS = 2000;
    
    /** Check interval: 3 seconds for responsive room-clear detection */
    private static final long CHECK_INTERVAL_MS = 3000;
    
    public DungeonTickSystem(ComponentType<EntityStore, DungeonData> dungeonDataType) {
        this.dungeonDataType = dungeonDataType;
    }
    
    public void setDungeonManager(DungeonManager manager) {
        this.dungeonManager = manager;
    }
    
    public void setMobSpawner(MobSpawner spawner) {
        this.mobSpawner = spawner;
    }
    
    public void setPortalManager(PortalManager portalManager) {
        this.portalManager = portalManager;
    }
    
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }
    
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer, Damage damage) {
        if (dungeonManager == null) return;
        
        // Throttle checks
        long now = System.currentTimeMillis();
        if (now - lastCheckMillis < CHECK_INTERVAL_MS) return;
        lastCheckMillis = now;
        
        // Process all active instances
        for (DungeonInstance inst : dungeonManager.getAllInstances()) {
            if (!inst.isActive()) continue;
            
            // 1. Timer warnings
            checkTimerWarnings(inst);
            
            // 2. Check time expiry
            if (inst.isTimeExpired()) {
                handleTimeExpired(inst);
                continue;
            }
            
            // 3. Check mob deaths + wave/room clear
            if (mobSpawner != null && !inst.isRoomCleared()) {
                checkMobDeaths(inst);
            }
            
            // 4. Handle room cleared -> advance or complete
            if (inst.isRoomCleared()) {
                handleRoomCleared(inst);
            }
            
            // 5. Refresh HUD overlays for all players in this instance
            dungeonManager.refreshHuds(inst);
        }
        
        // 6. Portal proximity checks (separate interval)
        if (portalManager != null && now - lastPortalCheckMillis >= PORTAL_CHECK_INTERVAL_MS) {
            lastPortalCheckMillis = now;
            checkPortalProximity();
        }
    }
    
    /**
     * Checks all online players for proximity to dungeon portals.
     */
    @SuppressWarnings("deprecation")
    private void checkPortalProximity() {
        try {
            Universe universe = Universe.get();
            // Check players in all loaded worlds
            for (String worldName : dungeonManager.getWorldConfig().getPortalWorlds()) {
                World world = universe.getWorld(worldName);
                if (world == null) continue;
                
                for (Player p : world.getPlayers()) {
                    try {
                        var tc = p.getTransformComponent();
                        if (tc != null) {
                            com.hypixel.hytale.math.vector.Vector3d pos = tc.getPosition();
                            portalManager.checkPlayerNearPortal(p, pos, worldName);
                        }
                    } catch (Exception e) {
                        // Player may have disconnected mid-check
                    }
                }
            }
        } catch (Exception e) {
            // Universe may not be fully loaded
        }
    }
    
    /**
     * Checks and sends timer warnings at key thresholds.
     */
    private void checkTimerWarnings(DungeonInstance inst) {
        if (inst.shouldWarn5min()) {
            broadcastToInstance(inst, Msg.text("5 minutes remaining!", Msg.AMBER));
        }
        if (inst.shouldWarn1min()) {
            broadcastToInstance(inst, Msg.bold("1 MINUTE remaining!", Msg.RED));
        }
        if (inst.shouldWarn30sec()) {
            broadcastToInstance(inst, Msg.bold("30 seconds!", Msg.RED));
        }
        if (inst.shouldWarn10sec()) {
            broadcastToInstance(inst, Msg.bold("10 SECONDS!", Msg.RED));
        }
    }
    
    /**
     * Handles a dungeon that ran out of time.
     */
    private void handleTimeExpired(DungeonInstance inst) {
        // Notify all players
        broadcastToInstance(inst, Msg.error("TIME'S UP! Dungeon failed."));
        broadcastToInstance(inst, Msg.text("Better luck next time!", Msg.GRAY));
        
        // Despawn remaining mobs
        if (mobSpawner != null) {
            World world = getInstanceWorld(inst);
            if (world != null) {
                mobSpawner.despawnAll(world, inst);
            }
        }
        
        // Teleport all players back and remove HUDs
        teleportAllPlayersBack(inst);
        
        dungeonManager.failInstance(inst.getInstanceId(), "Time limit exceeded");
    }
    
    /**
     * Checks if mobs in the current wave/room are dead.
     * Handles wave advancement before marking room as cleared.
     */
    private void checkMobDeaths(DungeonInstance inst) {
        World world = getInstanceWorld(inst);
        if (world == null) return;
        
        boolean waveCleared = mobSpawner.checkRoomCleared(world, inst);
        
        if (waveCleared) {
            var room = inst.getCurrentRoom();
            
            // Check if there are more waves in this room
            if (!inst.isLastWave()) {
                // Advance to next wave
                int nextWave = inst.getCurrentWave() + 1;
                inst.advanceWave();
                
                broadcastToInstance(inst, Msg.bold("Wave " + (nextWave + 1) + "!", Msg.AMBER));
                
                // Spawn mobs for next wave
                if (room != null) {
                    var waveSpawns = room.getMobSpawnsForWave(nextWave);
                    if (waveSpawns != null && !waveSpawns.isEmpty()) {
                        // Use the wave-specific spawns via MobSpawner
                        int spawned = mobSpawner.spawnMobsForRoom(world, inst, inst.getCurrentRoomIndex());
                        if (spawned > 0) {
                            broadcastToInstance(inst, Msg.text(spawned + " enemies appeared!", Msg.AMBER));
                        }
                    }
                }
                return;  // Don't mark room as cleared yet
            }
            
            // All waves done — room is cleared
            inst.setRoomCleared(true);
            
            String roomName = (room != null) ? room.getName() : "Room " + (inst.getCurrentRoomIndex() + 1);
            
            if (room != null && room.isBoss()) {
                broadcastToInstance(inst, Msg.success("** BOSS DEFEATED! **"));
            }
            broadcastToInstance(inst, Msg.success(roomName + " cleared!"));
            
            // Grant loot for this room
            grantRoomLoot(inst);
        }
    }
    
    /**
     * Handles logic after a room is cleared:
     * - If final room: complete the dungeon
     * - Otherwise: advance to next room and spawn mobs
     */
    private void handleRoomCleared(DungeonInstance inst) {
        if (inst.isFinalRoom()) {
            // Dungeon complete!
            handleDungeonComplete(inst);
        } else {
            // Advance to next room
            advanceToNextRoom(inst);
        }
    }
    
    /**
     * Completes a dungeon successfully.
     */
    private void handleDungeonComplete(DungeonInstance inst) {
        long elapsed = inst.getElapsedSeconds();
        long mins = elapsed / 60;
        long secs = elapsed % 60;
        
        broadcastToInstance(inst, Msg.header("DUNGEON COMPLETE"));
        broadcastToInstance(inst, Msg.success("Congratulations!"));
        broadcastToInstance(inst, Msg.bullet("Dungeon", inst.getTemplate().getName()));
        broadcastToInstance(inst, Msg.bullet("Clear Time", mins + "m " + secs + "s"));
        broadcastToInstance(inst, Msg.bullet("Rooms Cleared", String.valueOf(inst.getTotalRooms())));
        
        // Grant loot for final room
        grantRoomLoot(inst);
        
        broadcastToInstance(inst, Msg.divider());
        broadcastToInstance(inst, Msg.hint("Teleporting back in 5 seconds..."));
        
        // Despawn any remaining mobs
        if (mobSpawner != null) {
            World world = getInstanceWorld(inst);
            if (world != null) {
                mobSpawner.despawnAll(world, inst);
            }
        }
        
        // Teleport all players back and remove HUDs
        teleportAllPlayersBack(inst);
        
        dungeonManager.completeInstance(inst.getInstanceId());
    }
    
    /**
     * Advances to the next room: resets state, spawns new mobs, notifies players.
     */
    private void advanceToNextRoom(DungeonInstance inst) {
        int prevRoom = inst.getCurrentRoomIndex();
        
        if (!inst.advanceRoom()) {
            System.err.println("[DungeonTick] Failed to advance room for " + inst.getInstanceId());
            return;
        }
        
        int newRoomIndex = inst.getCurrentRoomIndex();
        var newRoom = inst.getCurrentRoom();
        String roomName = (newRoom != null) ? newRoom.getName() : "Room " + (newRoomIndex + 1);
        boolean isBoss = (newRoom != null) && newRoom.isBoss();
        
        // Notify players
        broadcastToInstance(inst, Msg.header("Room " + (newRoomIndex + 1) + "/" + inst.getTotalRooms()));
        broadcastToInstance(inst, Msg.bold("Entering: " + roomName, Msg.AQUA));
        
        if (isBoss) {
            broadcastToInstance(inst, Msg.bold("[BOSS ROOM]", Msg.RED));
        }
        
        // Spawn mobs for new room
        if (mobSpawner != null) {
            World world = getInstanceWorld(inst);
            if (world != null) {
                int spawned = mobSpawner.spawnMobsForRoom(world, inst, newRoomIndex);
                if (spawned > 0) {
                    broadcastToInstance(inst, Msg.text("Enemies appeared! (" + spawned + " mobs)", Msg.AMBER));
                } else {
                    // No mobs in this room — auto-clear
                    inst.setRoomCleared(true);
                    broadcastToInstance(inst, Msg.text("Room is empty — proceeding...", Msg.GRAY));
                }
            }
        }
    }
    
    /**
     * Grants loot from the current room to all players in the dungeon.
     * Uses the instance loot tracking to prevent double grants.
     */
    @SuppressWarnings("deprecation")
    private void grantRoomLoot(DungeonInstance inst) {
        var room = inst.getCurrentRoom();
        if (room == null) return;
        
        // Deduplicate: track which rooms have granted loot
        String lootKey = "room_" + inst.getCurrentRoomIndex();
        if (inst.hasGrantedLoot(lootKey)) return;
        inst.markLootGranted(lootKey);
        
        var lootEntries = room.getLoot();
        if (lootEntries == null || lootEntries.isEmpty()) return;
        
        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
        StringBuilder lootSummary = new StringBuilder();
        
        for (var loot : lootEntries) {
            if (rng.nextDouble() > loot.getChance()) continue;
            
            int count = rng.nextInt(loot.getMinCount(), loot.getMaxCount() + 1);
            if (count <= 0) continue;
            
            // TODO: Actually give items via inventory API when available
            // For now, announce the loot
            lootSummary.append("  ").append(loot.getItem()).append(" x").append(count).append("\n");
        }
        
        if (lootSummary.length() > 0) {
            broadcastToInstance(inst, Msg.bold("Loot Dropped:", Msg.GOLD));
            for (String line : lootSummary.toString().split("\n")) {
                if (!line.trim().isEmpty()) {
                    broadcastToInstance(inst, Msg.text(line, Msg.AMBER));
                }
            }
        }
    }
    
    /**
     * Teleports all players in a dungeon instance back to their saved positions.
     * Also removes HUD overlays.
     */
    @SuppressWarnings("deprecation")
    private void teleportAllPlayersBack(DungeonInstance inst) {
        World world = getInstanceWorld(inst);
        if (world == null) return;
        
        for (UUID playerUuid : inst.getPlayers()) {
            try {
                for (Player p : world.getPlayers()) {
                    if (p.getUuid().equals(playerUuid)) {
                        dungeonManager.teleportPlayerBack(p, inst);
                        dungeonManager.removeHud(p);
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("[DungeonTick] Failed to teleport player back: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets the world for a dungeon instance.
     */
    private World getInstanceWorld(DungeonInstance inst) {
        if (!inst.hasLinkedWorld()) return null;
        
        try {
            Universe universe = Universe.get();
            return universe.getWorld(inst.getWorldName());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Sends a message to all players in a dungeon instance.
     */
    @SuppressWarnings("deprecation")
    private void broadcastToInstance(DungeonInstance inst, Message msg) {
        World world = getInstanceWorld(inst);
        if (world == null) return;
        
        for (UUID playerUuid : inst.getPlayers()) {
            try {
                for (Player p : world.getPlayers()) {
                    if (p.getUuid().equals(playerUuid)) {
                        p.sendMessage(msg);
                        break;
                    }
                }
            } catch (Exception e) {
                // Player may have disconnected
            }
        }
    }
}
