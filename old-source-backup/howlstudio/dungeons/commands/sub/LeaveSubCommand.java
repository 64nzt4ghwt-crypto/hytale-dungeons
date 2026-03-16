package com.howlstudio.dungeons.commands.sub;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /dungeon leave
 * Leaves the player's current dungeon instance.
 */
public class LeaveSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public LeaveSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

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
