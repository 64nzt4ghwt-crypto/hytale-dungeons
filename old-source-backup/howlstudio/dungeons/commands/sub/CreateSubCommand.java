package com.howlstudio.dungeons.commands.sub;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.spawning.MobSpawner;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /dungeon create <template>
 * Creates a new dungeon instance from a template or registered world.
 */
public class CreateSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;
    private final MobSpawner mobSpawner;

    public CreateSubCommand(DungeonManager manager, MobSpawner mobSpawner) {
        this.dungeonManager = manager;
        this.mobSpawner = mobSpawner;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        String templateId = args.trim();
        if (templateId.isEmpty()) {
            ctx.sendMessage(Msg.error("Missing template name"));
            ctx.sendMessage(Msg.hint("Usage: /dungeon create <template>"));
            return;
        }

        if (dungeonManager.getInstanceForPlayer(player.getUuid()) != null) {
            ctx.sendMessage(Msg.error("You're already in a dungeon!"));
            ctx.sendMessage(Msg.hint("Use /dungeon leave to exit first"));
            return;
        }

        // Try registered world first, then template
        DungeonInstance instance;
        if (dungeonManager.getWorldConfig().isRegistered(templateId)) {
            instance = dungeonManager.createWorldInstance(templateId, player);
        } else if (dungeonManager.hasTemplate(templateId)) {
            instance = dungeonManager.createInstance(templateId, player);
        } else {
            ctx.sendMessage(Msg.error("Unknown dungeon: " + templateId));
            ctx.sendMessage(Msg.hint("Use /dungeon list to see available dungeons"));
            return;
        }

        if (instance == null) {
            ctx.sendMessage(Msg.error("Failed to create dungeon"));
            return;
        }

        // Spawn mobs for the first room
        int mobsSpawned = mobSpawner.spawnMobsForRoom(world, instance, instance.getCurrentRoomIndex());
        
        ctx.sendMessage(Msg.header("Dungeon Created"));
        ctx.sendMessage(Msg.bullet("Dungeon", instance.getTemplate().getName()));
        ctx.sendMessage(Msg.bullet("Rooms", String.valueOf(instance.getTotalRooms())));
        ctx.sendMessage(Msg.bullet("Time Limit", (instance.getTemplate().getTimeLimitSeconds() / 60) + " min"));
        ctx.sendMessage(Msg.bullet("Instance", instance.getInstanceId().toString().substring(0, 8)));
        if (mobsSpawned > 0) {
            ctx.sendMessage(Msg.bullet("Mobs Spawned", String.valueOf(mobsSpawned)));
        }
        ctx.sendMessage(Msg.divider());
        ctx.sendMessage(Msg.success("Party leader: " + playerRef.getUsername()));
        ctx.sendMessage(Msg.hint("Invite others: /dungeon join " + playerRef.getUsername()));
    }
}
