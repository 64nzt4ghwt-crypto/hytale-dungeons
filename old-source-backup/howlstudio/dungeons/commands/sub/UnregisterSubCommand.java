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
 * /dungeon unregister <world>
 * Unregisters a world from the dungeon system.
 * Does NOT delete the world - just removes dungeon config.
 */
public class UnregisterSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public UnregisterSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        String worldName = args.trim();
        if (worldName.isEmpty()) {
            ctx.sendMessage(Msg.error("Missing world name"));
            ctx.sendMessage(Msg.hint("Usage: /dungeon unregister <world>"));
            return;
        }

        if (!dungeonManager.getWorldConfig().isRegistered(worldName)) {
            ctx.sendMessage(Msg.error("World '" + worldName + "' is not registered as a dungeon"));
            return;
        }

        boolean removed = dungeonManager.getWorldConfig().unregisterWorld(worldName);
        if (!removed) {
            ctx.sendMessage(Msg.error("Failed to unregister world: " + worldName));
            return;
        }

        ctx.sendMessage(Msg.success("Unregistered dungeon world: " + worldName));
        ctx.sendMessage(Msg.text("  World files are not deleted", Msg.GRAY));
    }
}
