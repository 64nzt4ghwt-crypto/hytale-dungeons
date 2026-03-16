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
 * /dungeon register <world>
 * Registers an existing world as a dungeon.
 * The player's current position is saved as the spawn point.
 */
public class RegisterSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public RegisterSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        String worldName = args.trim();
        if (worldName.isEmpty()) {
            ctx.sendMessage(Msg.error("Missing world name"));
            ctx.sendMessage(Msg.hint("Usage: /dungeon register <world>"));
            return;
        }

        if (dungeonManager.getWorldConfig().isRegistered(worldName)) {
            ctx.sendMessage(Msg.error("World '" + worldName + "' is already registered as a dungeon"));
            return;
        }

        boolean registered = dungeonManager.getWorldConfig().registerWorld(worldName);
        if (!registered) {
            ctx.sendMessage(Msg.error("Failed to register world: " + worldName));
            return;
        }

        // Save the player's current position as the default spawn point
        com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc = player.getTransformComponent();
        com.hypixel.hytale.math.vector.Vector3d pos = (tc != null) ? tc.getPosition() : null;
        if (pos != null) {
            var entry = dungeonManager.getWorldConfig().getWorld(worldName);
            if (entry != null) {
                entry.setSpawnX(pos.x);
                entry.setSpawnY(pos.y);
                entry.setSpawnZ(pos.z);
                dungeonManager.getWorldConfig().save();
            }
        }

        ctx.sendMessage(Msg.success("Registered world '" + worldName + "' as a dungeon"));
        if (pos != null) {
            ctx.sendMessage(Msg.bullet("Spawn Point", String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)));
        }
        ctx.sendMessage(Msg.hint("Add rooms: /dungeon mob add " + worldName + " <entity> player"));
        ctx.sendMessage(Msg.hint("Set portal: /dungeon setportal " + worldName));
    }
}
