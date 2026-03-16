package com.howlstudio.dungeons.portal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.howlstudio.dungeons.config.DungeonWorldConfig;
import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;

/**
 * Manages dungeon portal locations and player proximity detection.
 * 
 * Portals are set via /dungeon setportal <dungeon> and stored in the 
 * world config. When a player gets within PORTAL_RADIUS blocks of a
 * portal location, they're prompted to enter the dungeon.
 * 
 * Cooldown prevents spam-activation (5 second cooldown per player).
 */
public class PortalManager {
    
    /** Distance in blocks to trigger portal activation */
    private static final double PORTAL_RADIUS = 3.0;
    
    /** Cooldown between portal activations (ms) */
    private static final long PORTAL_COOLDOWN_MS = 5000;
    
    /** Player UUID -> last portal activation timestamp */
    private final Map<UUID, Long> portalCooldowns = new ConcurrentHashMap<>();
    
    private final DungeonManager dungeonManager;
    
    public PortalManager(DungeonManager dungeonManager) {
        this.dungeonManager = dungeonManager;
    }
    
    /**
     * Checks if a player is near any dungeon portal.
     * Called periodically by the tick system.
     * 
     * @param player the player to check
     * @param playerPos the player's current position
     * @param worldName the world the player is in
     */
    @SuppressWarnings("deprecation")
    public void checkPlayerNearPortal(Player player, Vector3d playerPos, String worldName) {
        UUID playerUuid = player.getUuid();
        
        // Check cooldown
        Long lastActivation = portalCooldowns.get(playerUuid);
        if (lastActivation != null && System.currentTimeMillis() - lastActivation < PORTAL_COOLDOWN_MS) {
            return;
        }
        
        // Skip if player is already in a dungeon
        if (dungeonManager.getInstanceForPlayer(playerUuid) != null) {
            return;
        }
        
        // Check all registered dungeons for portal proximity
        DungeonWorldConfig worldConfig = dungeonManager.getWorldConfig();
        for (String dungeonName : worldConfig.getRegisteredWorldNames()) {
            DungeonWorldConfig.WorldEntry entry = worldConfig.getWorld(dungeonName);
            if (entry == null || !entry.hasPortal()) continue;
            
            // Check if portal is in this world
            if (!worldName.equals(entry.getPortalWorld())) continue;
            
            // Distance check
            double dx = playerPos.x - entry.getPortalX();
            double dy = playerPos.y - entry.getPortalY();
            double dz = playerPos.z - entry.getPortalZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            
            if (distSq <= PORTAL_RADIUS * PORTAL_RADIUS) {
                activatePortal(player, dungeonName, entry);
                portalCooldowns.put(playerUuid, System.currentTimeMillis());
                return;
            }
        }
    }
    
    /**
     * Activates a dungeon portal for a player.
     * Creates a new instance or joins an existing open one.
     */
    private void activatePortal(Player player, String dungeonName, DungeonWorldConfig.WorldEntry entry) {
        player.sendMessage(Msg.header("Dungeon Portal"));
        player.sendMessage(Msg.bullet("Dungeon", entry.getDisplayName()));
        
        // Check if there's already an instance the player can join
        for (DungeonInstance inst : dungeonManager.getAllInstances()) {
            if (inst.getDungeonId().equals(dungeonName) && inst.canJoin()) {
                // Join existing instance
                dungeonManager.joinInstance(inst.getInstanceId(), player);
                player.sendMessage(Msg.success("Joined existing dungeon run!"));
                return;
            }
        }
        
        // Create new instance
        DungeonInstance instance;
        if (dungeonManager.hasTemplate(dungeonName)) {
            instance = dungeonManager.createInstance(dungeonName, player);
        } else {
            instance = dungeonManager.createWorldInstance(dungeonName, player);
        }
        
        if (instance != null) {
            player.sendMessage(Msg.success("Entering " + entry.getDisplayName() + "..."));
            player.sendMessage(Msg.hint("Use /dleave to exit anytime"));
        } else {
            player.sendMessage(Msg.error("Failed to create dungeon instance"));
        }
    }
    
    /**
     * Clears cooldown for a player (e.g., on disconnect).
     */
    public void clearCooldown(UUID playerUuid) {
        portalCooldowns.remove(playerUuid);
    }
    
    /**
     * Clears all cooldowns.
     */
    public void clearAllCooldowns() {
        portalCooldowns.clear();
    }
}
