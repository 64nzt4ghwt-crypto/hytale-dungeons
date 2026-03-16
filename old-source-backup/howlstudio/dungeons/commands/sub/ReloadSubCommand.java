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
 * /dungeon reload
 * Reloads all dungeon configs (templates and world registrations) from disk.
 */
public class ReloadSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public ReloadSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        try {
            dungeonManager.reloadAll();
            int templates = dungeonManager.getAvailableTemplates().size();
            int worlds = dungeonManager.getWorldConfig().getRegisteredWorldNames().size();

            ctx.sendMessage(Msg.success("Configs reloaded"));
            ctx.sendMessage(Msg.bullet("Templates", String.valueOf(templates)));
            ctx.sendMessage(Msg.bullet("Registered Worlds", String.valueOf(worlds)));

            int active = dungeonManager.getActiveInstanceCount();
            if (active > 0) {
                ctx.sendMessage(Msg.warn(active + " active instances were not affected"));
            }
        } catch (Exception e) {
            ctx.sendMessage(Msg.error("Reload failed: " + e.getMessage()));
        }
    }
}
