package com.howlstudio.dungeons.commands.sub;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /dungeon join <player>
 * Joins another player's active dungeon instance.
 */
public class JoinSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public JoinSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        String targetName = args.trim();
        if (targetName.isEmpty()) {
            ctx.sendMessage(Msg.error("Missing player name"));
            ctx.sendMessage(Msg.hint("Usage: /dungeon join <player>"));
            return;
        }

        if (targetName.equalsIgnoreCase(playerRef.getUsername())) {
            ctx.sendMessage(Msg.error("You're already in your own party!"));
            return;
        }

        if (dungeonManager.getInstanceForPlayer(player.getUuid()) != null) {
            ctx.sendMessage(Msg.error("Leave your current dungeon first"));
            ctx.sendMessage(Msg.hint("Use /dungeon leave"));
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
