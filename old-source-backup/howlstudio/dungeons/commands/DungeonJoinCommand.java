package com.howlstudio.dungeons.commands;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DungeonJoinCommand extends AbstractPlayerCommand {
    
    private final DungeonManager dungeonManager;
    private final RequiredArg<String> targetArg;
    
    public DungeonJoinCommand(DungeonManager manager) {
        super("djoin", "Join another player's dungeon", false);
        this.dungeonManager = manager;
        this.targetArg = withRequiredArg("player", "Player to join", ArgTypes.STRING);
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
        
        String targetName = targetArg.get(ctx);
        if (targetName == null || targetName.isEmpty()) {
            ctx.sendMessage(Msg.hint("Usage: /djoin <player>"));
            return;
        }
        
        if (targetName.equalsIgnoreCase(playerRef.getUsername())) {
            ctx.sendMessage(Msg.error("You're already in your own party!"));
            return;
        }
        
        if (dungeonManager.getInstanceForPlayer(player.getUuid()) != null) {
            ctx.sendMessage(Msg.error("Leave your current dungeon first"));
            ctx.sendMessage(Msg.hint("Use /dleave"));
            return;
        }
        
        PlayerRef targetRef = Universe.get().getPlayerByUsername(targetName, NameMatching.EXACT_IGNORE_CASE);
        if (targetRef == null) {
            ctx.sendMessage(Msg.error("Player not found: " + targetName));
            return;
        }
        
        DungeonInstance targetInstance = dungeonManager.getInstanceForPlayer(targetRef.getUuid());
        if (targetInstance == null) {
            ctx.sendMessage(Msg.error(targetName + " is not in a dungeon"));
            return;
        }
        
        if (targetInstance.isFull()) {
            ctx.sendMessage(Msg.error("Dungeon is full (" + targetInstance.getPlayerCount() 
                + "/" + targetInstance.getTemplate().getMaxPlayers() + ")"));
            return;
        }
        
        if (!targetInstance.isActive()) {
            ctx.sendMessage(Msg.error("That dungeon has already ended"));
            return;
        }
        
        boolean success = dungeonManager.joinInstance(targetInstance.getInstanceId(), player);
        if (!success) {
            ctx.sendMessage(Msg.error("Failed to join dungeon"));
            return;
        }
        
        ctx.sendMessage(Msg.success("Joined " + targetName + "'s dungeon!"));
        ctx.sendMessage(Msg.bullet("Dungeon", targetInstance.getTemplate().getName()));
        ctx.sendMessage(Msg.bullet("Room", (targetInstance.getCurrentRoomIndex() + 1) + "/" + targetInstance.getTotalRooms()));
    }
}
