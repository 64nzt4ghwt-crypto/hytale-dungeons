package com.howlstudio.dungeons.commands.sub;

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

/**
 * /dungeon loot add <dungeon> <room> <item> <chance>
 * 
 * Adds a loot entry to a dungeon room's loot table.
 * Chance is a percentage (0-100).
 * 
 * Example: /dungeon loot add ice_temple boss_room hytale:enchanted_blade 30
 */
public class LootSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public LootSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        String[] parts = args.trim().split("\\s+");

        if (parts.length < 1 || !parts[0].equalsIgnoreCase("add")) {
            ctx.sendMessage(Msg.error("Usage: /dungeon loot add <dungeon> <room> <item> <chance>"));
            ctx.sendMessage(Msg.hint("Chance is 0-100 (percentage)"));
            return;
        }

        if (parts.length < 5) {
            ctx.sendMessage(Msg.error("Not enough arguments"));
            ctx.sendMessage(Msg.hint("Usage: /dungeon loot add <dungeon> <room> <item> <chance>"));
            return;
        }

        String dungeonName = parts[1];
        String roomName = parts[2];
        String item = parts[3];
        String chanceStr = parts[4];

        // Validate dungeon exists
        if (!dungeonManager.getWorldConfig().isRegistered(dungeonName)) {
            ctx.sendMessage(Msg.error("Unknown dungeon world: " + dungeonName));
            ctx.sendMessage(Msg.hint("Register a world first: /dungeon register <world>"));
            return;
        }

        // Parse chance
        double chance;
        try {
            chance = Double.parseDouble(chanceStr);
            if (chance < 0 || chance > 100) {
                ctx.sendMessage(Msg.error("Chance must be between 0 and 100"));
                return;
            }
            // Convert percentage to 0-1 range for storage
            chance = chance / 100.0;
        } catch (NumberFormatException e) {
            ctx.sendMessage(Msg.error("Invalid chance value: " + chanceStr));
            return;
        }

        // Create and add the loot entry
        DungeonWorldConfig.LootEntryConfig lootEntry = new DungeonWorldConfig.LootEntryConfig(item, chance);

        dungeonManager.getWorldConfig().addLootEntry(dungeonName, roomName, lootEntry);

        ctx.sendMessage(Msg.success("Added loot entry to " + dungeonName + " / " + roomName));
        ctx.sendMessage(Msg.bullet("Item", item));
        ctx.sendMessage(Msg.bullet("Chance", chanceStr + "%"));
    }
}
