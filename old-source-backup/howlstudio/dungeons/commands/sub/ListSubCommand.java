package com.howlstudio.dungeons.commands.sub;

import com.howlstudio.dungeons.config.DungeonTemplate;
import com.howlstudio.dungeons.config.DungeonWorldConfig;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Collection;

/**
 * /dungeon list
 * Lists all available dungeon templates and registered worlds.
 */
public class ListSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public ListSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        Collection<String> templateIds = dungeonManager.getAvailableTemplates();
        Collection<String> worldNames = dungeonManager.getWorldConfig().getRegisteredWorldNames();

        if (templateIds.isEmpty() && worldNames.isEmpty()) {
            ctx.sendMessage(Msg.error("No dungeons configured"));
            ctx.sendMessage(Msg.hint("Add JSON files to Server/DungeonConfigs/ or use /dungeon register <world>"));
            return;
        }

        ctx.sendMessage(Msg.header("Available Dungeons"));
        ctx.sendMessage(Msg.text("", Msg.DARK));

        // List templates
        if (!templateIds.isEmpty()) {
            ctx.sendMessage(Msg.bold("  Templates:", Msg.AMBER));
            for (String id : templateIds) {
                DungeonTemplate template = dungeonManager.getTemplate(id);
                if (template == null) continue;

                String name = template.getName();
                int rooms = template.getRoomCount();
                String players = template.getMinPlayers() + "-" + template.getMaxPlayers();
                int timeMin = template.getTimeLimitSeconds() / 60;

                ctx.sendMessage(Msg.pair("    " + id + " ", "-- " + name, Msg.AMBER, Msg.WHITE));
                ctx.sendMessage(Msg.text("      " + rooms + " rooms, " + players + " players, " + timeMin + " min", Msg.GRAY));
            }
        }

        // List registered worlds
        if (!worldNames.isEmpty()) {
            ctx.sendMessage(Msg.text("", Msg.DARK));
            ctx.sendMessage(Msg.bold("  Registered Worlds:", Msg.GREEN));
            for (String wn : worldNames) {
                DungeonWorldConfig.WorldEntry entry = dungeonManager.getWorldConfig().getWorld(wn);
                if (entry == null) continue;

                String displayName = entry.getDisplayName();
                int roomCount = entry.getRooms().size();
                String portal = entry.hasPortal() ? "yes" : "no";

                ctx.sendMessage(Msg.pair("    " + wn + " ", "-- " + displayName, Msg.GREEN, Msg.WHITE));
                ctx.sendMessage(Msg.text("      " + roomCount + " rooms, portal: " + portal, Msg.GRAY));
            }
        }

        ctx.sendMessage(Msg.text("", Msg.DARK));
        ctx.sendMessage(Msg.divider());

        int active = dungeonManager.getActiveInstanceCount();
        if (active > 0) {
            ctx.sendMessage(Msg.text("  Active instances: " + active, Msg.GRAY));
        }

        ctx.sendMessage(Msg.hint("Create a dungeon: /dungeon create <template>"));
    }
}
