package com.howlstudio.dungeons.manager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.howlstudio.dungeons.components.DungeonData;
import com.howlstudio.dungeons.config.DungeonConfig;
import com.howlstudio.dungeons.config.DungeonTemplate;
import com.howlstudio.dungeons.config.DungeonWorldConfig;
import com.howlstudio.dungeons.config.SpawnPointConfig;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.vector.Vector3d;
import com.howlstudio.dungeons.ui.DungeonHud;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Singleton manager for all active dungeon instances.
 * 
 * Responsibilities:
 * - Track which players are in which dungeons
 * - Create and cleanup dungeon instances
 * - Load and cache dungeon templates from config
 * - Manage world-based dungeon registrations
 * - Handle player teleportation to dungeon worlds
 * - Coordinate between commands, systems, and events
 * 
 * Thread-safe: uses ConcurrentHashMap for all mutable state.
 */
public class DungeonManager {
    
    private static DungeonManager instance;
    
    // Active dungeon instances mapped by ID
    private final Map<UUID, DungeonInstance> activeInstances;
    
    // Quick lookup: player UUID -> current instance ID
    private final Map<UUID, UUID> playerToInstance;
    
    // Config systems
    private final DungeonConfig config;
    private final DungeonWorldConfig worldConfig;
    private File dataDirectory;
    
    // ECS component type for player data (set during plugin init)
    private ComponentType<EntityStore, DungeonData> dungeonDataType;
    
    // Mob spawner for despawning on cleanup
    private com.howlstudio.dungeons.spawning.MobSpawner mobSpawner;
    
    private DungeonManager() {
        this.activeInstances = new ConcurrentHashMap<>();
        this.playerToInstance = new ConcurrentHashMap<>();
        this.config = new DungeonConfig();
        this.worldConfig = new DungeonWorldConfig();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized DungeonManager getInstance() {
        if (instance == null) {
            instance = new DungeonManager();
        }
        return instance;
    }
    
    /**
     * Sets the data directory for config loading.
     * Called during plugin startup.
     */
    public void setDataDirectory(File dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.config.loadFromDirectory(dataDirectory);
        this.worldConfig.load(dataDirectory);
    }
    
    /**
     * Sets the component type for player dungeon data.
     * Called by DungeonPlugin during initialization.
     */
    public void setDungeonDataType(ComponentType<EntityStore, DungeonData> type) {
        this.dungeonDataType = type;
    }
    
    /**
     * Sets the mob spawner for cleanup during instance end.
     */
    public void setMobSpawner(com.howlstudio.dungeons.spawning.MobSpawner spawner) {
        this.mobSpawner = spawner;
    }
    
    /**
     * Gets the mob spawner.
     */
    public com.howlstudio.dungeons.spawning.MobSpawner getMobSpawner() {
        return mobSpawner;
    }
    
    /**
     * Gets the world config manager.
     */
    public DungeonWorldConfig getWorldConfig() {
        return worldConfig;
    }
    
    // ---- Instance lifecycle ----
    
    /**
     * Creates a new dungeon instance for a player.
     * If the template corresponds to a registered world, links it and teleports players.
     * @param templateId the dungeon template to use
     * @param leader the player creating the dungeon
     * @return the new instance, or null if template not found
     */
    @SuppressWarnings("deprecation")
    public DungeonInstance createInstance(String templateId, Player leader) {
        DungeonTemplate template = config.getTemplate(templateId);
        if (template == null) {
            System.err.println("[DungeonManager] Unknown template: " + templateId);
            return null;
        }
        
        // Check if player is already in a dungeon
        if (getInstanceForPlayer(leader.getUuid()) != null) {
            System.err.println("[DungeonManager] Player already in a dungeon");
            return null;
        }
        
        UUID instanceId = UUID.randomUUID();
        DungeonInstance inst = new DungeonInstance(instanceId, template);
        inst.addPlayer(leader.getUuid());
        
        // Check if this template has a registered world
        if (worldConfig.isRegistered(templateId)) {
            inst.setWorldName(templateId);
        }
        
        activeInstances.put(instanceId, inst);
        playerToInstance.put(leader.getUuid(), instanceId);
        
        // Update player's dungeon data
        updatePlayerDungeonData(leader, inst);
        
        // Teleport leader to dungeon world if linked
        teleportPlayerToDungeon(leader, inst);
        
        // Setup persistent HUD overlay
        setupHud(leader, inst);
        
        System.out.println("[DungeonManager] Created instance " + instanceId 
            + " for dungeon " + templateId);
        
        return inst;
    }
    
    /**
     * Creates a dungeon instance from a registered world (not a template).
     * @param worldName the registered world name
     * @param leader the player creating the dungeon
     * @return the new instance, or null on failure
     */
    @SuppressWarnings("deprecation")
    public DungeonInstance createWorldInstance(String worldName, Player leader) {
        DungeonWorldConfig.WorldEntry worldEntry = worldConfig.getWorld(worldName);
        if (worldEntry == null) {
            System.err.println("[DungeonManager] World not registered: " + worldName);
            return null;
        }
        
        if (getInstanceForPlayer(leader.getUuid()) != null) {
            System.err.println("[DungeonManager] Player already in a dungeon");
            return null;
        }
        
        // Build a lightweight template from the world config
        DungeonTemplate template = buildTemplateFromWorld(worldEntry);
        
        UUID instanceId = UUID.randomUUID();
        DungeonInstance inst = new DungeonInstance(instanceId, template);
        inst.addPlayer(leader.getUuid());
        inst.setWorldName(worldName);
        
        activeInstances.put(instanceId, inst);
        playerToInstance.put(leader.getUuid(), instanceId);
        
        updatePlayerDungeonData(leader, inst);
        teleportPlayerToDungeon(leader, inst);
        setupHud(leader, inst);
        
        System.out.println("[DungeonManager] Created world instance " + instanceId 
            + " for world " + worldName);
        
        return inst;
    }
    
    /**
     * Builds a DungeonTemplate from a world registration entry.
     */
    private DungeonTemplate buildTemplateFromWorld(DungeonWorldConfig.WorldEntry worldEntry) {
        DungeonTemplate template = new DungeonTemplate();
        template.setId(worldEntry.getWorldName());
        template.setName(worldEntry.getDisplayName());
        template.setDescription("World dungeon: " + worldEntry.getWorldName());
        template.setMaxPlayers(worldEntry.getMaxPlayers());
        template.setTimeLimitSeconds(worldEntry.getTimeLimitSeconds());
        
        // Convert room entries to room definitions
        java.util.List<DungeonTemplate.RoomDefinition> rooms = new java.util.ArrayList<>();
        if (worldEntry.getRooms().isEmpty()) {
            // Create a default room if none configured
            DungeonTemplate.RoomDefinition defaultRoom = new DungeonTemplate.RoomDefinition();
            defaultRoom.setName("Main");
            rooms.add(defaultRoom);
        } else {
            for (Map.Entry<String, DungeonWorldConfig.RoomEntry> entry : worldEntry.getRooms().entrySet()) {
                DungeonWorldConfig.RoomEntry re = entry.getValue();
                DungeonTemplate.RoomDefinition rd = new DungeonTemplate.RoomDefinition();
                rd.setName(re.getName());
                rd.setBoss(re.isBoss());
                
                // Convert spawn points to mob spawns
                java.util.List<DungeonTemplate.MobSpawn> mobSpawns = new java.util.ArrayList<>();
                for (SpawnPointConfig sp : re.getSpawnPoints()) {
                    DungeonTemplate.MobSpawn ms = new DungeonTemplate.MobSpawn();
                    ms.setType(sp.getEntity());
                    ms.setCount(sp.getAmount());
                    mobSpawns.add(ms);
                }
                rd.setMobSpawns(mobSpawns);
                
                // Convert loot entries
                java.util.List<DungeonTemplate.LootEntry> lootEntries = new java.util.ArrayList<>();
                for (DungeonWorldConfig.LootEntryConfig le : re.getLootTable()) {
                    DungeonTemplate.LootEntry loot = new DungeonTemplate.LootEntry();
                    loot.setItem(le.getItem());
                    loot.setMinCount(le.getMinCount());
                    loot.setMaxCount(le.getMaxCount());
                    loot.setChance(le.getChance());
                    lootEntries.add(loot);
                }
                rd.setLoot(lootEntries);
                
                rooms.add(rd);
            }
        }
        template.setRooms(rooms);
        
        return template;
    }
    
    /**
     * Teleports a player to the dungeon world spawn point.
     * Saves their current position for return teleport on leave/complete.
     * Handles both same-world position changes and cross-world teleportation.
     */
    @SuppressWarnings("deprecation")
    public void teleportPlayerToDungeon(Player player, DungeonInstance inst) {
        if (!inst.hasLinkedWorld()) return;
        
        // Save return position before teleporting
        try {
            World currentWorld = player.getWorld();
            if (currentWorld != null) {
                var tc = player.getTransformComponent();
                if (tc != null) {
                    Vector3d pos = tc.getPosition();
                    inst.saveReturnPosition(player.getUuid(), 
                        currentWorld.getName(), pos.x, pos.y, pos.z);
                }
            }
        } catch (Exception e) {
            System.err.println("[DungeonManager] Failed to save return position: " + e.getMessage());
        }
        
        String worldName = inst.getWorldName();
        DungeonWorldConfig.WorldEntry worldEntry = worldConfig.getWorld(worldName);
        
        // Determine spawn coordinates
        double spawnX = 0, spawnY = 64, spawnZ = 0;
        if (worldEntry != null) {
            spawnX = worldEntry.getSpawnX();
            spawnY = worldEntry.getSpawnY();
            spawnZ = worldEntry.getSpawnZ();
        }
        
        final double fx = spawnX, fy = spawnY, fz = spawnZ;
        
        try {
            Universe universe = Universe.get();
            World targetWorld = universe.getWorld(worldName);
            
            if (targetWorld != null) {
                // World already loaded - use moveTo for same-server teleport
                doTeleport(player, targetWorld, fx, fy, fz);
                player.sendMessage(Msg.success("Teleported to dungeon: " + inst.getTemplate().getName()));
            } else {
                // Load the world asynchronously, then teleport
                player.sendMessage(Msg.text("Loading dungeon world...", Msg.GRAY));
                universe.loadWorld(worldName).thenAccept(loadedWorld -> {
                    if (loadedWorld != null) {
                        doTeleport(player, loadedWorld, fx, fy, fz);
                        player.sendMessage(Msg.success("Teleported to dungeon: " + inst.getTemplate().getName()));
                    } else {
                        player.sendMessage(Msg.error("Failed to load dungeon world: " + worldName));
                    }
                }).exceptionally(ex -> {
                    player.sendMessage(Msg.error("Error loading world: " + ex.getMessage()));
                    System.err.println("[DungeonManager] World load failed: " + ex.getMessage());
                    return null;
                });
            }
        } catch (Exception e) {
            player.sendMessage(Msg.error("Teleport failed: " + e.getMessage()));
            System.err.println("[DungeonManager] Teleport error: " + e.getMessage());
        }
    }
    
    /**
     * Performs the actual teleport using TransformComponent.
     * For cross-world teleports, uses PlayerRef.updatePosition().
     * For same-world teleports, uses Entity.moveTo() or TransformComponent.teleportPosition().
     */
    @SuppressWarnings("deprecation")
    private void doTeleport(Player player, World targetWorld, double x, double y, double z) {
        try {
            World currentWorld = player.getWorld();
            
            if (currentWorld != null && currentWorld.getName().equals(targetWorld.getName())) {
                // Same world: use TransformComponent to teleport position
                var transform = player.getTransformComponent();
                if (transform != null) {
                    transform.teleportPosition(new Vector3d(x, y, z));
                }
            } else {
                // Cross-world: use PlayerRef.updatePosition
                PlayerRef pRef = player.getPlayerRef();
                if (pRef != null) {
                    com.hypixel.hytale.math.vector.Transform t = 
                        new com.hypixel.hytale.math.vector.Transform(x, y, z);
                    pRef.updatePosition(targetWorld, t, null);
                }
            }
        } catch (Exception e) {
            System.err.println("[DungeonManager] doTeleport error: " + e.getMessage());
        }
    }
    
    /**
     * Teleports a player back to their saved position (before dungeon entry).
     * Called when leaving/completing a dungeon.
     */
    @SuppressWarnings("deprecation")
    public void teleportPlayerBack(Player player, DungeonInstance inst) {
        String returnPos = inst.removeReturnPosition(player.getUuid());
        if (returnPos == null) return;
        
        try {
            String[] parts = returnPos.split(",");
            if (parts.length < 4) return;
            
            String worldName = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            
            Universe universe = Universe.get();
            World targetWorld = universe.getWorld(worldName);
            
            if (targetWorld != null) {
                doTeleport(player, targetWorld, x, y, z);
                player.sendMessage(Msg.text("Returned to " + worldName, Msg.GRAY));
            } else {
                // World not loaded, try loading it
                universe.loadWorld(worldName).thenAccept(loaded -> {
                    if (loaded != null) {
                        doTeleport(player, loaded, x, y, z);
                        player.sendMessage(Msg.text("Returned to " + worldName, Msg.GRAY));
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[DungeonManager] Return teleport failed: " + e.getMessage());
        }
    }
    
    /**
     * Allows a player to join an existing instance.
     * @param instanceId the instance to join
     * @param player the player joining
     * @return true if joined successfully
     */
    @SuppressWarnings("deprecation")
    public boolean joinInstance(UUID instanceId, Player player) {
        DungeonInstance inst = activeInstances.get(instanceId);
        if (inst == null) {
            return false;
        }
        
        if (!inst.canJoin()) {
            return false;
        }
        
        // Remove from old instance if any
        leaveCurrentInstance(player);
        
        inst.addPlayer(player.getUuid());
        playerToInstance.put(player.getUuid(), instanceId);
        
        updatePlayerDungeonData(player, inst);
        
        // Teleport joining player to dungeon
        teleportPlayerToDungeon(player, inst);
        
        // Setup HUD for the joining player
        setupHud(player, inst);
        
        System.out.println("[DungeonManager] Player " + player.getUuid() 
            + " joined instance " + instanceId);
        return true;
    }
    
    /**
     * Makes a player join another player's dungeon.
     */
    public boolean joinPlayerDungeon(Player joiner, UUID targetPlayerUuid) {
        DungeonInstance targetInstance = getInstanceForPlayer(targetPlayerUuid);
        if (targetInstance == null) {
            return false;
        }
        return joinInstance(targetInstance.getInstanceId(), joiner);
    }
    
    /**
     * Removes a player from their current dungeon instance.
     * Cleans up empty instances.
     */
    public boolean leaveInstance(Player player) {
        return leaveCurrentInstance(player);
    }
    
    @SuppressWarnings("deprecation")
    private boolean leaveCurrentInstance(Player player) {
        UUID playerUuid = player.getUuid();
        UUID instanceId = playerToInstance.remove(playerUuid);
        
        if (instanceId == null) {
            return false;
        }
        
        DungeonInstance inst = activeInstances.get(instanceId);
        if (inst != null) {
            // Teleport player back to their original position
            teleportPlayerBack(player, inst);
            
            // Remove HUD
            removeHud(player);
            
            inst.removePlayer(playerUuid);
            
            // Clean up empty instances
            if (inst.isEmpty()) {
                cleanupInstance(instanceId);
            }
        }
        
        // Clear player's dungeon data
        clearPlayerDungeonData(player);
        
        System.out.println("[DungeonManager] Player " + playerUuid + " left instance " + instanceId);
        return true;
    }
    
    /**
     * Sets up the dungeon HUD overlay for a player.
     * Shows persistent dungeon status on screen during gameplay.
     */
    @SuppressWarnings("deprecation")
    public void setupHud(Player player, DungeonInstance inst) {
        try {
            PlayerRef pRef = player.getPlayerRef();
            if (pRef != null) {
                DungeonHud hud = new DungeonHud(pRef, player.getUuid(), inst.getInstanceId());
                player.getHudManager().setCustomHud(pRef, hud);
                hud.show();
            }
        } catch (Exception e) {
            System.err.println("[DungeonManager] Failed to setup HUD: " + e.getMessage());
        }
    }
    
    /**
     * Refreshes the dungeon HUD for all players in an instance.
     */
    @SuppressWarnings("deprecation")
    public void refreshHuds(DungeonInstance inst) {
        World world = getInstanceWorld(inst);
        if (world == null) return;
        
        for (UUID playerUuid : inst.getPlayers()) {
            try {
                for (Player p : world.getPlayers()) {
                    if (p.getUuid().equals(playerUuid)) {
                        var hud = p.getHudManager().getCustomHud();
                        if (hud instanceof DungeonHud) {
                            ((DungeonHud) hud).refresh();
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                // Player may have disconnected
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
     * Removes the dungeon HUD from a player.
     */
    @SuppressWarnings("deprecation")
    public void removeHud(Player player) {
        try {
            PlayerRef pRef = player.getPlayerRef();
            if (pRef != null) {
                player.getHudManager().resetHud(pRef);
            }
        } catch (Exception e) {
            // HUD may not have been set
        }
    }
    
    /**
     * Completes a dungeon instance successfully.
     * Updates player stats and cleans up.
     */
    public void completeInstance(UUID instanceId) {
        DungeonInstance inst = activeInstances.get(instanceId);
        if (inst == null || !inst.isActive()) {
            return;
        }
        
        inst.setCompleted(true);
        
        long clearTime = System.currentTimeMillis() - inst.getStartTimeMillis();
        
        // Update stats for all players
        for (UUID playerUuid : inst.getPlayers()) {
            dungeonDataOp(playerUuid, data -> {
                data.incrementDungeonsCompleted();
                data.updateBestTime(clearTime);
                data.clearSession();
            });
        }
        
        System.out.println("[DungeonManager] Instance " + instanceId + " completed in " + clearTime + "ms");
        
        // Schedule cleanup
        cleanupInstance(instanceId);
    }
    
    /**
     * Marks a dungeon as failed (time expired, etc).
     * Despawns all mobs and cleans up.
     */
    public void failInstance(UUID instanceId, String reason) {
        DungeonInstance inst = activeInstances.get(instanceId);
        if (inst == null) {
            return;
        }
        
        inst.setFailed(true);
        System.out.println("[DungeonManager] Instance " + instanceId + " failed: " + reason);
        
        // Despawn remaining mobs
        if (mobSpawner != null && inst.hasLinkedWorld()) {
            World world = getInstanceWorld(inst);
            if (world != null) {
                mobSpawner.despawnAll(world, inst);
            }
        }
        
        // Clear session data for players
        for (UUID playerUuid : inst.getPlayers()) {
            dungeonDataOp(playerUuid, DungeonData::clearSession);
        }
        
        cleanupInstance(instanceId);
    }
    
    /**
     * Force-ends all active dungeon instances.
     * @return the number of instances ended
     */
    public int forceEndAll() {
        int count = activeInstances.size();
        for (UUID instanceId : new java.util.ArrayList<>(activeInstances.keySet())) {
            failInstance(instanceId, "Force ended by admin");
        }
        return count;
    }
    
    /**
     * Cleans up an instance and removes all players.
     */
    public void cleanupInstance(UUID instanceId) {
        DungeonInstance inst = activeInstances.remove(instanceId);
        if (inst == null) {
            return;
        }
        
        // Remove player mappings
        for (UUID playerUuid : inst.getPlayers()) {
            playerToInstance.remove(playerUuid);
        }
        
        System.out.println("[DungeonManager] Cleaned up instance " + instanceId);
    }
    
    // ---- Player data management ----
    
    @SuppressWarnings("deprecation")
    private void updatePlayerDungeonData(Player player, DungeonInstance inst) {
        if (dungeonDataType == null) return;
        
        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) return;
        
        Holder holder = playerRef.getHolder();
        if (holder == null) return;
        
        DungeonData data = (DungeonData) holder.ensureAndGetComponent(dungeonDataType);
        data.setCurrentDungeonId(inst.getDungeonId());
        data.setCurrentRoomIndex(inst.getCurrentRoomIndex());
        data.setSessionStartTime(inst.getStartTimeMillis());
        holder.putComponent(dungeonDataType, (DungeonData) data.clone());
    }
    
    @SuppressWarnings("deprecation")
    private void clearPlayerDungeonData(Player player) {
        if (dungeonDataType == null) return;
        
        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) return;
        
        Holder holder = playerRef.getHolder();
        if (holder == null) return;
        
        DungeonData data = (DungeonData) holder.ensureAndGetComponent(dungeonDataType);
        data.clearSession();
        holder.putComponent(dungeonDataType, (DungeonData) data.clone());
    }
    
    /**
     * Applies a function to a player's DungeonData if they have the component.
     */
    private void dungeonDataOp(UUID playerUuid, java.util.function.Consumer<DungeonData> action) {
        // This is a simplified version - in production you'd need PlayerRef lookup
        // For now, this documents the pattern
    }
    
    // ---- Lookups ----
    
    @SuppressWarnings("deprecation")
    public DungeonInstance getInstance(UUID instanceId) {
        return activeInstances.get(instanceId);
    }
    
    public DungeonInstance getInstanceForPlayer(UUID playerUuid) {
        UUID instanceId = playerToInstance.get(playerUuid);
        if (instanceId == null) {
            return null;
        }
        return activeInstances.get(instanceId);
    }
    
    public UUID getInstanceIdForPlayer(UUID playerUuid) {
        return playerToInstance.get(playerUuid);
    }
    
    public Collection<DungeonInstance> getAllInstances() {
        return Collections.unmodifiableCollection(activeInstances.values());
    }
    
    public int getActiveInstanceCount() {
        return activeInstances.size();
    }
    
    // ---- Template access ----
    
    public DungeonTemplate getTemplate(String id) {
        return config.getTemplate(id);
    }
    
    public boolean hasTemplate(String id) {
        return config.hasTemplate(id);
    }
    
    public Collection<String> getAvailableTemplates() {
        return config.getTemplateIds();
    }
    
    /**
     * Checks if a dungeon exists either as a template or registered world.
     */
    public boolean hasDungeon(String id) {
        return config.hasTemplate(id) || worldConfig.isRegistered(id);
    }
    
    /**
     * Reloads all configs (templates + world registrations).
     */
    public void reloadAll() {
        config.reload();
        worldConfig.reload();
    }
    
    public void reloadTemplates() {
        config.reload();
    }
    
    // ---- Maintenance ----
    
    /**
     * Checks for expired instances and cleans them up.
     * Called periodically by the tick system.
     */
    public void checkExpiredInstances() {
        for (DungeonInstance inst : activeInstances.values()) {
            if (!inst.isActive()) {
                continue;
            }
            
            if (inst.isTimeExpired()) {
                failInstance(inst.getInstanceId(), "Time limit exceeded");
            }
        }
    }
}
