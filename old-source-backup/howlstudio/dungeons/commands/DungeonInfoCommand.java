package com.howlstudio.dungeons.commands;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DungeonInfoCommand extends AbstractPlayerCommand {
    
    private final DungeonManager dungeonManager;
    
    public DungeonInfoCommand(DungeonManager manager) {
        super("dinfo", "Show your current dungeon status", false);
        this.dungeonManager = manager;
    }
    
    @Override
    protected void execute(
            @Nonnull CommandContext ctx,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Msg.error("Could not access player data"));
            return;
        }
        
        DungeonInstance instance = dungeonManager.getInstanceForPlayer(player.getUuid());
        if (instance == null) {
            ctx.sendMessage(Msg.error("You're not in a dungeon"));
            ctx.sendMessage(Msg.hint("Create one: /dcreate <template>"));
            return;
        }
        
        long elapsed = instance.getElapsedSeconds();
        long remaining = instance.getRemainingSeconds();
        String elapsedStr = formatTime(elapsed);
        String remainingStr = instance.isActive() ? formatTime(remaining) : "—";
        
        ctx.sendMessage(Msg.header("Dungeon Status"));
        ctx.sendMessage(Msg.bullet("Dungeon", instance.getTemplate().getName()));
        ctx.sendMessage(Msg.bullet("Description", instance.getTemplate().getDescription()));
        ctx.sendMessage(Msg.text("", Msg.DARK));
        
        ctx.sendMessage(Msg.bullet("Progress", "Room " + (instance.getCurrentRoomIndex() + 1) + " of " + instance.getTotalRooms()));
        
        var currentRoom = instance.getCurrentRoom();
        if (currentRoom != null) {
            ctx.sendMessage(Msg.bullet("Location", currentRoom.getName()));
            if (currentRoom.isBoss()) {
                ctx.sendMessage(Msg.bold("    [BOSS ROOM]", Msg.RED));
            }
        }
        
        int remainingMobs = instance.getRemainingMobCount();
        int totalMobs = instance.getSpawnedMobCount();
        if (totalMobs > 0) {
            ctx.sendMessage(Msg.bullet("Mobs", remainingMobs + " / " + totalMobs + " remaining"));
            if (instance.areAllMobsDead()) {
                ctx.sendMessage(Msg.success("Room cleared!"));
            }
        }
        
        if (instance.isRoomCleared()) {
            if (instance.isFinalRoom()) {
                ctx.sendMessage(Msg.bold("  ** DUNGEON COMPLETE **", Msg.GOLD));
            } else {
                ctx.sendMessage(Msg.success("Room cleared — ready to advance"));
            }
        }
        
        ctx.sendMessage(Msg.text("", Msg.DARK));
        ctx.sendMessage(Msg.bullet("Time", elapsedStr + " elapsed · " + remainingStr + " remaining"));
        ctx.sendMessage(Msg.bullet("Party", instance.getPlayerCount() + " players"));
        
        if (instance.isCompleted()) {
            ctx.sendMessage(Msg.bold("  Status: Completed", Msg.GREEN));
        } else if (instance.isFailed()) {
            ctx.sendMessage(Msg.bold("  Status: Failed", Msg.RED));
        }
        
        ctx.sendMessage(Msg.divider());
    }
    
    private String formatTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}
