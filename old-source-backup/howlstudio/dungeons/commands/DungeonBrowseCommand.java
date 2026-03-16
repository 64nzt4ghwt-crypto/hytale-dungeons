package com.howlstudio.dungeons.commands;

import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.ui.DungeonListPage;
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
 * /dbrowse - Opens the dungeon browser UI panel.
 * Shows a clickable list of available dungeons that players can join.
 */
public class DungeonBrowseCommand extends AbstractPlayerCommand {
    
    private final DungeonManager dungeonManager;
    
    public DungeonBrowseCommand(DungeonManager manager) {
        super("dbrowse", "Open dungeon browser panel", false);
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
        
        try {
            DungeonListPage page = new DungeonListPage(player, playerRef, world);
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception e) {
            ctx.sendMessage(Msg.error("Failed to open dungeon browser: " + e.getMessage()));
            // Fallback: show text list
            ctx.sendMessage(Msg.hint("Use /dlist for text list instead"));
        }
    }
}
