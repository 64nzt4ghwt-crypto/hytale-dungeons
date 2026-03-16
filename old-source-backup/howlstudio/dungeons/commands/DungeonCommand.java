package com.howlstudio.dungeons.commands;

import com.howlstudio.dungeons.commands.sub.*;
import com.howlstudio.dungeons.manager.DungeonInstance;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.spawning.MobSpawner;
import com.howlstudio.dungeons.ui.DungeonListPage;
import com.howlstudio.dungeons.ui.DungeonStatusPage;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root command: /dungeon <subcommand> [args...]
 * 
 * Routes to subcommand handlers for all dungeon operations.
 * Uses ctx.getInputString() to parse the full raw input since
 * Hytale's command system doesn't have a GREEDY_STRING type.
 * 
 * We still declare a STRING arg for the subcommand name so the
 * framework validates that at least one argument is provided.
 */
public class DungeonCommand extends AbstractPlayerCommand {

    private final DungeonManager dungeonManager;
    private final RequiredArg<String> subcommandArg;

    // Subcommand handlers
    private final Map<String, SubCommand> subcommands;

    public DungeonCommand(DungeonManager manager, MobSpawner mobSpawner) {
        super("dungeon", "Dungeon system commands", false);
        this.dungeonManager = manager;
        this.subcommandArg = withRequiredArg("subcommand", "Subcommand name", ArgTypes.STRING);

        // Register all subcommands
        this.subcommands = new LinkedHashMap<>();
        subcommands.put("create", new CreateSubCommand(manager, mobSpawner));
        subcommands.put("list", new ListSubCommand(manager));
        subcommands.put("join", new JoinSubCommand(manager));
        subcommands.put("leave", new LeaveSubCommand(manager));
        subcommands.put("info", new InfoSubCommand(manager));
        subcommands.put("register", new RegisterSubCommand(manager));
        subcommands.put("unregister", new UnregisterSubCommand(manager));
        subcommands.put("mob", new MobSubCommand(manager));
        subcommands.put("loot", new LootSubCommand(manager));
        subcommands.put("reload", new ReloadSubCommand(manager));
        subcommands.put("forceend", new ForceEndSubCommand(manager));
        subcommands.put("setportal", new SetPortalSubCommand(manager));
        subcommands.put("browse", null);  // handled inline (opens UI)
        subcommands.put("status", null);  // handled inline (opens UI)
        subcommands.put("help", null);    // handled inline
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

        // Parse the raw input to get subcommand + args
        // Input format: "dungeon <sub> [args...]" or just the args portion
        String rawInput = ctx.getInputString();
        String argsString = extractArgsAfterCommand(rawInput);

        if (argsString.isEmpty()) {
            showHelp(ctx);
            return;
        }

        // Split into subcommand + remaining args
        String[] parts = argsString.split("\\s+", 2);
        String subName = parts[0].toLowerCase();
        String subArgs = parts.length > 1 ? parts[1] : "";

        if (subName.equals("help")) {
            showHelp(ctx);
            return;
        }
        
        // Handle UI subcommands that need Player context
        if (subName.equals("browse") || subName.equals("status")) {
            handleUISubcommand(subName, ctx, store, ref, playerRef, player, world);
            return;
        }

        SubCommand handler = subcommands.get(subName);
        if (handler == null) {
            ctx.sendMessage(Msg.error("Unknown subcommand: " + subName));
            ctx.sendMessage(Msg.hint("Use /dungeon help for available commands"));
            return;
        }

        handler.execute(ctx, store, ref, playerRef, player, world, subArgs);
    }

    /**
     * Extracts everything after the command name from the raw input.
     * Handles formats like "dungeon create foo" -> "create foo"
     * or "/dungeon create foo" -> "create foo"
     */
    private String extractArgsAfterCommand(String rawInput) {
        if (rawInput == null) return "";
        String trimmed = rawInput.trim();
        
        // Remove leading slash if present
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        
        // Remove the command name ("dungeon")
        if (trimmed.toLowerCase().startsWith("dungeon")) {
            trimmed = trimmed.substring("dungeon".length()).trim();
        }
        
        return trimmed;
    }

    @SuppressWarnings("deprecation")
    private void handleUISubcommand(String subName, CommandContext ctx,
                                     Store<EntityStore> store, Ref<EntityStore> ref,
                                     PlayerRef playerRef, Player player, World world) {
        try {
            if (subName.equals("browse")) {
                DungeonListPage page = new DungeonListPage(player, playerRef, world);
                player.getPageManager().openCustomPage(ref, store, page);
            } else if (subName.equals("status")) {
                DungeonInstance instance = dungeonManager.getInstanceForPlayer(player.getUuid());
                if (instance == null) {
                    ctx.sendMessage(Msg.error("You're not in a dungeon"));
                    return;
                }
                DungeonStatusPage page = new DungeonStatusPage(player, playerRef, instance.getInstanceId());
                player.getPageManager().openCustomPage(ref, store, page);
            }
        } catch (Exception e) {
            ctx.sendMessage(Msg.error("Failed to open UI: " + e.getMessage()));
        }
    }
    
    private void showHelp(CommandContext ctx) {
        ctx.sendMessage(Msg.header("Dungeon Commands"));
        ctx.sendMessage(Msg.text("", Msg.DARK));
        ctx.sendMessage(Msg.bold("  --- Player ---", Msg.GREEN));
        ctx.sendMessage(Msg.pair("  /dcreate <template>", " - Start a dungeon run", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dlist", " - List available dungeons", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dbrowse", " - Open dungeon browser UI", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /djoin <player>", " - Join a player's dungeon", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dleave", " - Leave your current dungeon", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dinfo", " - Show dungeon status (text)", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dstatus", " - Open dungeon status UI panel", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dungeon help", " - This help message", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.text("", Msg.DARK));
        ctx.sendMessage(Msg.bold("  --- Admin ---", Msg.AMBER));
        ctx.sendMessage(Msg.pair("  /dungeon register <world>", " - Register a world as a dungeon", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dungeon unregister <world>", " - Unregister a dungeon world", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dungeon mob add <dungeon> <entity> <pos>", " - Add mob spawn point", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dungeon loot add <dungeon> <room> <item> <chance>", " - Add loot to room", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dungeon setportal <dungeon>", " - Set portal at your location", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dungeon reload", " - Reload all configs", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.pair("  /dungeon forceend", " - Force end all dungeons", Msg.AQUA, Msg.GRAY));
        ctx.sendMessage(Msg.divider());
    }
}
