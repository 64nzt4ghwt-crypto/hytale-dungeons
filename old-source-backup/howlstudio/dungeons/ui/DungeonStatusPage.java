package com.howlstudio.dungeons.ui;

import java.util.UUID;

import com.howlstudio.dungeons.config.DungeonTemplate;
import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Live dungeon status HUD showing room progress, mob count, timer, party.
 */
public class DungeonStatusPage extends CustomUIPage {

    private final Player player;
    private final UUID instanceId;
    private final DungeonManager dungeonManager;

    public DungeonStatusPage(Player player, PlayerRef playerRef, UUID instanceId) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.player = player;
        this.instanceId = instanceId;
        this.dungeonManager = DungeonManager.getInstance();
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        DungeonInstance inst = dungeonManager.getInstance(instanceId);
        if (inst == null) {
            ui.set("title", Message.raw("Dungeon Ended").color(Msg.RED));
            return;
        }

        DungeonTemplate template = inst.getTemplate();

        // Title
        ui.set("title", Message.raw(template.getName()).color(Msg.GOLD).bold(true));

        // Room progress
        int currentRoom = inst.getCurrentRoomIndex() + 1;
        int totalRooms = inst.getTotalRooms();
        var room = inst.getCurrentRoom();
        String roomName = (room != null) ? room.getName() : "Unknown";
        ui.set("roomProgress", Message.raw("Room " + currentRoom + "/" + totalRooms + " - " + roomName).color(Msg.AQUA));

        if (room != null && room.isBoss()) {
            ui.set("bossIndicator", Message.raw("[BOSS ROOM]").color(Msg.RED).bold(true));
        } else {
            ui.set("bossIndicator", "");
        }

        // Mob count
        int remaining = inst.getRemainingMobCount();
        int total = inst.getSpawnedMobCount();
        if (total > 0) {
            java.awt.Color mobColor = remaining == 0 ? Msg.GREEN : Msg.AMBER;
            ui.set("mobCount", Message.raw("Mobs: " + remaining + "/" + total).color(mobColor));
        } else {
            ui.set("mobCount", Message.raw("No mobs spawned").color(Msg.GRAY));
        }

        if (inst.isRoomCleared()) {
            if (inst.isFinalRoom()) {
                ui.set("statusMessage", Message.raw("** DUNGEON COMPLETE **").color(Msg.GOLD).bold(true));
            } else {
                ui.set("statusMessage", Message.raw("Room cleared!").color(Msg.GREEN).bold(true));
            }
        } else {
            ui.set("statusMessage", "");
        }

        // Timer
        long elapsed = inst.getElapsedSeconds();
        long remainingSec = inst.getRemainingSeconds();
        String timeStr = formatTime(elapsed) + " / " + formatTime(inst.getTemplate().getTimeLimitSeconds());
        java.awt.Color timeColor = remainingSec < 60 ? Msg.RED : (remainingSec < 180 ? Msg.AMBER : Msg.WHITE);
        ui.set("timer", Message.raw(timeStr).color(timeColor));

        // Party
        ui.set("partyCount", Message.raw("Party: " + inst.getPlayerCount() + " players").color(Msg.GRAY));

        // Completion state
        if (inst.isCompleted()) {
            ui.set("completionState", Message.raw("Status: Completed").color(Msg.GREEN).bold(true));
        } else if (inst.isFailed()) {
            ui.set("completionState", Message.raw("Status: Failed").color(Msg.RED).bold(true));
        } else {
            ui.set("completionState", "");
        }

        // Leave button
        events.addEventBinding(CustomUIEventBindingType.Activating, "leave_button");
    }

    /**
     * Triggers a UI refresh with current dungeon state.
     */
    public void refresh() {
        rebuild();
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, String data) {
        if ("leave_button".equals(data)) {
            dungeonManager.leaveInstance(player);
            player.sendMessage(Msg.success("Left the dungeon"));
            close();
        }
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        // Player dismissed the panel — that's fine, dungeon continues
    }

    private String formatTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}
