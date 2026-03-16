package com.howlstudio.dungeons.events;

import com.howlstudio.dungeons.config.DungeonWorldConfig;
import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;

/**
 * Handles player death while inside a dungeon.
 * 
 * When a player dies in a dungeon:
 * - If solo: dungeon fails (game over)
 * - If in party: player respawns at dungeon entrance after a delay
 * - The dungeon continues as long as at least one player is alive
 * 
 * Death count is tracked for scoring purposes.
 */
public class DungeonDeathHandler {
    
    private final DungeonManager dungeonManager;
    
    public DungeonDeathHandler(DungeonManager manager) {
        this.dungeonManager = manager;
    }
    
    /**
     * Called when a player dies.
     * Checks if they're in a dungeon and handles accordingly.
     * 
     * @param player the player who died
     * @return true if the death was handled (dungeon context), false otherwise
     */
    @SuppressWarnings("deprecation")
    public boolean handlePlayerDeath(Player player) {
        DungeonInstance instance = dungeonManager.getInstanceForPlayer(player.getUuid());
        if (instance == null) {
            return false; // Not in a dungeon, let normal death handle it
        }
        
        if (instance.getPlayerCount() <= 1) {
            // Solo player died — dungeon fails
            player.sendMessage(Msg.error("You died! Dungeon failed."));
            dungeonManager.failInstance(instance.getInstanceId(), "Player died (solo)");
            return true;
        }
        
        // Party dungeon — respawn at dungeon entrance
        player.sendMessage(Msg.text("You died! Respawning at dungeon entrance...", Msg.AMBER));
        
        // Notify other party members
        broadcastToOthers(instance, player, 
            Msg.text(player.getDisplayName() + " has fallen!", Msg.AMBER));
        
        // Respawn at dungeon entrance after a brief delay
        // The actual respawn position is the dungeon's spawn point
        try {
            String worldName = instance.getWorldName();
            if (worldName != null) {
                DungeonWorldConfig.WorldEntry worldEntry = 
                    dungeonManager.getWorldConfig().getWorld(worldName);
                
                if (worldEntry != null) {
                    var tc = player.getTransformComponent();
                    if (tc != null) {
                        tc.teleportPosition(new Vector3d(
                            worldEntry.getSpawnX(),
                            worldEntry.getSpawnY(),
                            worldEntry.getSpawnZ()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DungeonDeath] Failed to respawn player: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Sends a message to all players in an instance except one.
     */
    @SuppressWarnings("deprecation")
    private void broadcastToOthers(DungeonInstance inst, Player exclude, 
                                    com.hypixel.hytale.server.core.Message msg) {
        try {
            var world = exclude.getWorld();
            if (world == null) return;
            
            for (java.util.UUID playerUuid : inst.getPlayers()) {
                if (playerUuid.equals(exclude.getUuid())) continue;
                for (Player p : world.getPlayers()) {
                    if (p.getUuid().equals(playerUuid)) {
                        p.sendMessage(msg);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Player may have disconnected
        }
    }
}
