package com.howlstudio.dungeons.commands.sub;

import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /dungeon setportal <dungeon>
 * Sets the portal entry point for a dungeon at the player's current position/world.
 * Players standing at this location will be teleported into the dungeon.
 */
public class SetPortalSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public SetPortalSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        String dungeonName = args.trim();
        if (dungeonName.isEmpty()) {
            ctx.sendMessage(Msg.error("Missing dungeon name"));
            ctx.sendMessage(Msg.hint("Usage: /dungeon setportal <dungeon>"));
            return;
        }

        if (!dungeonManager.getWorldConfig().isRegistered(dungeonName)) {
            ctx.sendMessage(Msg.error("Unknown dungeon world: " + dungeonName));
            ctx.sendMessage(Msg.hint("Register a world first: /dungeon register <world>"));
            return;
        }

        TransformComponent transform = player.getTransformComponent();
        if (transform == null) {
            ctx.sendMessage(Msg.error("Could not get your position"));
            return;
        }

        Vector3d pos = transform.getPosition();
        if (pos == null) {
            ctx.sendMessage(Msg.error("Could not get your position"));
            return;
        }

        String portalWorldName = world.getName();

        dungeonManager.getWorldConfig().setPortal(dungeonName, portalWorldName, pos.x, pos.y, pos.z);

        ctx.sendMessage(Msg.success("Portal set for dungeon: " + dungeonName));
        ctx.sendMessage(Msg.bullet("World", portalWorldName));
        ctx.sendMessage(Msg.bullet("Position", String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z)));
        ctx.sendMessage(Msg.hint("Players near this spot can enter the dungeon"));
    }
}
