package com.howlstudio.dungeons.commands;

import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.spawning.MobSpawner;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DungeonCreateCommand extends AbstractPlayerCommand {
    
    private final DungeonManager dungeonManager;
    private final MobSpawner mobSpawner;
    private final RequiredArg<String> templateArg;
    
    public DungeonCreateCommand(DungeonManager manager, MobSpawner mobSpawner) {
        super("dcreate", "Create a new dungeon instance", false);
        this.dungeonManager = manager;
        this.mobSpawner = mobSpawner;
        this.templateArg = withRequiredArg("template", "Dungeon template ID", ArgTypes.STRING);
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
        
        String templateId = templateArg.get(ctx);
        if (templateId == null || templateId.isEmpty()) {
            ctx.sendMessage(Msg.error("Missing template name"));
            ctx.sendMessage(Msg.hint("Usage: /dcreate <template>"));
            return;
        }
        
        if (!dungeonManager.hasTemplate(templateId)) {
            ctx.sendMessage(Msg.error("Unknown template: " + templateId));
            ctx.sendMessage(Msg.hint("Use /dlist to see available dungeons"));
            return;
        }
        
        if (dungeonManager.getInstanceForPlayer(player.getUuid()) != null) {
            ctx.sendMessage(Msg.error("You're already in a dungeon!"));
            ctx.sendMessage(Msg.hint("Use /dleave to exit first"));
            return;
        }
        
        DungeonInstance instance = dungeonManager.createInstance(templateId, player);
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
        ctx.sendMessage(Msg.hint("Invite others: /djoin " + playerRef.getUsername()));
    }
}
