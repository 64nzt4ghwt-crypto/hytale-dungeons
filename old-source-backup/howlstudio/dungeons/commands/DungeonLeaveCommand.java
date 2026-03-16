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

public class DungeonLeaveCommand extends AbstractPlayerCommand {
    
    private final DungeonManager dungeonManager;
    
    public DungeonLeaveCommand(DungeonManager manager) {
        super("dleave", "Leave your current dungeon", false);
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
            return;
        }
        
        String dungeonName = instance.getTemplate().getName();
        int playerCountBefore = instance.getPlayerCount();
        
        boolean success = dungeonManager.leaveInstance(player);
        if (!success) {
            ctx.sendMessage(Msg.error("Failed to leave dungeon"));
            return;
        }
        
        ctx.sendMessage(Msg.success("Left dungeon: " + dungeonName));
        
        if (playerCountBefore <= 1) {
            ctx.sendMessage(Msg.text("  Instance closed", Msg.GRAY));
        } else {
            ctx.sendMessage(Msg.text("  " + (playerCountBefore - 1) + " player(s) remain", Msg.GRAY));
        }
    }
}
