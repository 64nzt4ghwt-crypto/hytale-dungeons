package com.howlstudio.dungeons.ui;

import java.util.Collection;

import com.howlstudio.dungeons.config.DungeonTemplate;
import com.howlstudio.dungeons.config.DungeonWorldConfig;
import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Custom UI popup listing available dungeons with clickable entries.
 * Open via: player.getPageManager().openCustomPage(ref, store, page)
 */
public class DungeonListPage extends CustomUIPage {

    private static final String ENTRY_PREFIX = "dungeon_entry_";

    private final Player player;
    private final World world;
    private final DungeonManager dungeonManager;

    public DungeonListPage(Player player, PlayerRef playerRef, World world) {
        super(playerRef, CustomPageLifetime.CanDismiss);
        this.player = player;
        this.world = world;
        this.dungeonManager = DungeonManager.getInstance();
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder ui, UIEventBuilder events, Store<EntityStore> store) {
        ui.set("title", Message.raw("=== Dungeon Browser ===").color(Msg.GOLD).bold(true));
        ui.clear("dungeonList");

        int index = 0;

        // Template-based dungeons
        Collection<String> templateIds = dungeonManager.getAvailableTemplates();
        for (String templateId : templateIds) {
            DungeonTemplate template = dungeonManager.getTemplate(templateId);
            if (template == null) continue;

            String elementId = ENTRY_PREFIX + index;

            ui.append("dungeonList");
            ui.set("dungeonList[" + index + "].name",
                    Message.raw(template.getName()).color(Msg.AQUA).bold(true));
            ui.set("dungeonList[" + index + "].description",
                    Message.raw(template.getDescription()).color(Msg.GRAY));
            ui.set("dungeonList[" + index + "].info",
                    Message.raw("Rooms: " + template.getRoomCount()
                            + " | Players: " + template.getMinPlayers()
                            + "-" + template.getMaxPlayers()
                            + " | Time: " + (template.getTimeLimitSeconds() / 60) + "m")
                            .color(Msg.DARK));
            ui.set("dungeonList[" + index + "].buttonLabel",
                    Message.raw("Enter").color(Msg.GREEN).bold(true));

            events.addEventBinding(CustomUIEventBindingType.Activating, elementId);
            index++;
        }

        // Registered-world dungeons
        DungeonWorldConfig worldConfig = dungeonManager.getWorldConfig();
        for (String worldName : worldConfig.getRegisteredWorldNames()) {
            if (dungeonManager.hasTemplate(worldName)) continue;

            DungeonWorldConfig.WorldEntry entry = worldConfig.getWorld(worldName);
            if (entry == null) continue;

            String elementId = ENTRY_PREFIX + index;
            ui.append("dungeonList");
            ui.set("dungeonList[" + index + "].name",
                    Message.raw(entry.getDisplayName()).color(Msg.AQUA).bold(true));
            ui.set("dungeonList[" + index + "].description",
                    Message.raw("World dungeon: " + worldName).color(Msg.GRAY));

            events.addEventBinding(CustomUIEventBindingType.Activating, elementId);
            index++;
        }

        if (index == 0) {
            ui.set("emptyMessage",
                    Message.raw("No dungeons available.").color(Msg.GRAY).italic(true));
        }

        events.addEventBinding(CustomUIEventBindingType.Activating, "close_button");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, String data) {
        if ("close_button".equals(data)) {
            close();
            return;
        }

        if (!data.startsWith(ENTRY_PREFIX)) return;

        int clickedIndex;
        try {
            clickedIndex = Integer.parseInt(data.substring(ENTRY_PREFIX.length()));
        } catch (NumberFormatException e) {
            return;
        }

        String templateId = resolveTemplateIdByIndex(clickedIndex);
        if (templateId == null) {
            player.sendMessage(Msg.error("Could not resolve dungeon selection"));
            return;
        }

        if (dungeonManager.getInstanceForPlayer(player.getUuid()) != null) {
            player.sendMessage(Msg.error("You're already in a dungeon!"));
            player.sendMessage(Msg.hint("Use /dleave to exit first"));
            close();
            return;
        }

        DungeonInstance instance;
        if (dungeonManager.getWorldConfig().isRegistered(templateId)) {
            instance = dungeonManager.createWorldInstance(templateId, player);
        } else if (dungeonManager.hasTemplate(templateId)) {
            instance = dungeonManager.createInstance(templateId, player);
        } else {
            player.sendMessage(Msg.error("Dungeon no longer available: " + templateId));
            close();
            return;
        }

        if (instance == null) {
            player.sendMessage(Msg.error("Failed to create dungeon instance"));
            close();
            return;
        }

        player.sendMessage(Msg.header("Dungeon Created"));
        player.sendMessage(Msg.bullet("Dungeon", instance.getTemplate().getName()));
        player.sendMessage(Msg.bullet("Rooms", String.valueOf(instance.getTotalRooms())));
        player.sendMessage(Msg.success("Entering dungeon..."));
        close();
    }

    private String resolveTemplateIdByIndex(int targetIndex) {
        int index = 0;
        for (String templateId : dungeonManager.getAvailableTemplates()) {
            if (dungeonManager.getTemplate(templateId) == null) continue;
            if (index == targetIndex) return templateId;
            index++;
        }
        DungeonWorldConfig worldConfig = dungeonManager.getWorldConfig();
        for (String worldName : worldConfig.getRegisteredWorldNames()) {
            if (dungeonManager.hasTemplate(worldName)) continue;
            if (worldConfig.getWorld(worldName) == null) continue;
            if (index == targetIndex) return worldName;
            index++;
        }
        return null;
    }
}
