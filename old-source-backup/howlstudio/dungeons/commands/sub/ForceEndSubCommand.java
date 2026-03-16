package com.howlstudio.dungeons.commands.sub;

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
 * /dungeon forceend
 * Force-ends all active dungeon instances. Admin command.
 */
public class ForceEndSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public ForceEndSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        int active = dungeonManager.getActiveInstanceCount();
        if (active == 0) {
            ctx.sendMessage(Msg.text("No active dungeon instances", Msg.GRAY));
            return;
        }

        int ended = dungeonManager.forceEndAll();
        ctx.sendMessage(Msg.success("Force ended " + ended + " dungeon instance(s)"));
    }
}
