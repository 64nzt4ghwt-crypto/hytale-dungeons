package com.howlstudio.dungeons.commands;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.ui.DungeonStatusPage;
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

/**
 * /dstatus - Opens the dungeon status UI panel.
 * Shows live dungeon progress: room, mobs, timer, party info.
 */
public class DungeonStatusCommand extends AbstractPlayerCommand {
    
    private final DungeonManager dungeonManager;
    
    public DungeonStatusCommand(DungeonManager manager) {
        super("dstatus", "Open dungeon status panel", false);
        this.dungeonManager = manager;
    }
    
    @Override
    @SuppressWarnings("deprecation")
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
            ctx.sendMessage(Msg.hint("Use /dcreate <template> or /dbrowse"));
            return;
        }
        
        try {
            DungeonStatusPage page = new DungeonStatusPage(player, playerRef, instance.getInstanceId());
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception e) {
            ctx.sendMessage(Msg.error("Failed to open status panel: " + e.getMessage()));
            ctx.sendMessage(Msg.hint("Use /dinfo for text status instead"));
        }
    }
}
