package com.howlstudio.dungeons.events;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

/**
 * Handles global events related to dungeon state.
 * Currently handles reconnection detection via PlayerReadyEvent.
 */
public class DungeonEventListener {
    
    private final DungeonManager dungeonManager;
    
    public DungeonEventListener(DungeonManager manager) {
        this.dungeonManager = manager;
    }
    
    /**
     * When a player reconnects, check if they were in a dungeon.
     * If the dungeon is still active, show a reconnect message.
     * If it ended, clean up their session data.
     */
    public void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        DungeonInstance instance = dungeonManager.getInstanceForPlayer(player.getUuid());
        if (instance == null) return;
        
        if (!instance.isActive()) {
            dungeonManager.leaveInstance(player);
            player.sendMessage(Msg.text("Your dungeon ended while you were offline", Msg.AMBER));
            return;
        }
        
        player.sendMessage(Msg.success("Reconnected to your dungeon!"));
        player.sendMessage(Msg.bullet("Dungeon", instance.getTemplate().getName() 
            + " -- Room " + (instance.getCurrentRoomIndex() + 1)));
        
        // Teleport them back to the dungeon world if applicable
        dungeonManager.teleportPlayerToDungeon(player, instance);
    }
}
