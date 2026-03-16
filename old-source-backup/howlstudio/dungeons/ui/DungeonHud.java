package com.howlstudio.dungeons.ui;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.UUID;

/**
 * Persistent HUD overlay that shows dungeon status while inside a dungeon.
 * Displays: dungeon name, room progress, mob count, timer.
 * 
 * Unlike CustomUIPage (modal popup), this stays on screen during gameplay.
 * Updated periodically by the tick system via refresh().
 * 
 * Uses HudManager: player.getHudManager().setCustomHud(playerRef, hud)
 */
public class DungeonHud extends CustomUIHud {
    
    private final UUID playerUuid;
    private final UUID instanceId;
    private final DungeonManager dungeonManager;
    
    public DungeonHud(PlayerRef playerRef, UUID playerUuid, UUID instanceId) {
        super(playerRef);
        this.playerUuid = playerUuid;
        this.instanceId = instanceId;
        this.dungeonManager = DungeonManager.getInstance();
    }
    
    @Override
    protected void build(UICommandBuilder ui) {
        DungeonInstance inst = dungeonManager.getInstance(instanceId);
        if (inst == null) {
            ui.set("dungeonHud.visible", false);
            return;
        }
        
        ui.set("dungeonHud.visible", true);
        
        // Dungeon name
        ui.set("dungeonHud.name", 
            Message.raw(inst.getTemplate().getName()).color(Msg.GOLD).bold(true));
        
        // Room progress bar
        int currentRoom = inst.getCurrentRoomIndex() + 1;
        int totalRooms = inst.getTotalRooms();
        var room = inst.getCurrentRoom();
        String roomName = (room != null) ? room.getName() : "Room " + currentRoom;
        boolean isBoss = (room != null) && room.isBoss();
        
        ui.set("dungeonHud.room", 
            Message.raw(roomName + " (" + currentRoom + "/" + totalRooms + ")")
                .color(isBoss ? Msg.RED : Msg.AQUA));
        
        // Mob count
        int remaining = inst.getRemainingMobCount();
        int total = inst.getSpawnedMobCount();
        if (total > 0) {
            java.awt.Color mobColor = remaining == 0 ? Msg.GREEN : Msg.WHITE;
            ui.set("dungeonHud.mobs", 
                Message.raw("Mobs: " + remaining + "/" + total).color(mobColor));
        } else {
            ui.set("dungeonHud.mobs", 
                Message.raw("No mobs").color(Msg.GRAY));
        }
        
        // Status indicator
        if (inst.isRoomCleared()) {
            if (inst.isFinalRoom()) {
                ui.set("dungeonHud.status", 
                    Message.raw("COMPLETE!").color(Msg.GOLD).bold(true));
            } else {
                ui.set("dungeonHud.status", 
                    Message.raw("CLEARED").color(Msg.GREEN).bold(true));
            }
        } else if (isBoss) {
            ui.set("dungeonHud.status", 
                Message.raw("BOSS").color(Msg.RED).bold(true));
        } else {
            ui.set("dungeonHud.status", "");
        }
        
        // Timer
        long remaining_sec = inst.getRemainingSeconds();
        long mins = remaining_sec / 60;
        long secs = remaining_sec % 60;
        java.awt.Color timeColor = remaining_sec < 60 ? Msg.RED : 
                                   (remaining_sec < 180 ? Msg.AMBER : Msg.WHITE);
        ui.set("dungeonHud.timer", 
            Message.raw(String.format("%d:%02d", mins, secs)).color(timeColor));
        
        // Party size
        ui.set("dungeonHud.party", 
            Message.raw(inst.getPlayerCount() + " players").color(Msg.GRAY));
    }
    
    /**
     * Refreshes the HUD with current dungeon state.
     * Called by the tick system every few seconds.
     */
    public void refresh() {
        UICommandBuilder commands = new UICommandBuilder();
        build(commands);
        update(true, commands);
    }
    
    /**
     * Gets the instance ID this HUD is tracking.
     */
    public UUID getInstanceId() {
        return instanceId;
    }
}
