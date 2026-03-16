package com.misa.dungeons.commands;

import com.hytale.server.command.Command;
import com.hytale.server.command.CommandContext;
import com.hytale.server.command.CommandResult;
import com.hytale.server.command.argument.StringArgument;
import com.hytale.server.ecs.Ref;
import com.hytale.server.ecs.component.Player;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.store.Store;
import com.hytale.server.world.World;
import com.misa.dungeons.config.DungeonConfig;
import com.misa.dungeons.manager.DungeonSession;
import com.misa.dungeons.manager.SessionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command: /dungeon enter <dungeonId>
 * <p>
 * Creates a new dungeon instance and teleports the executing player into it.
 * If the player is already in a dungeon, they must leave first.
 */
public class DungeonEnterCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(DungeonEnterCommand.class.getName());

    private final SessionManager sessionManager;

    public DungeonEnterCommand(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() {
        return "dungeon enter";
    }

    @Override
    public String getDescription() {
        return "Enter a dungeon instance. Usage: /dungeon enter <dungeonId>";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Ref playerRef = context.getPlayerRef();
        if (playerRef == null) {
            return CommandResult.failure("This command can only be run by a player.");
        }

        String[] args = context.getArgs();
        if (args.length < 1) {
            return CommandResult.failure("Usage: /dungeon enter <dungeonId>");
        }

        String dungeonId = args[0].toLowerCase();

        // Validate dungeon exists
        DungeonConfig config = sessionManager.getRegistry().getConfig(dungeonId);
        if (config == null) {
            return CommandResult.failure("Unknown dungeon: '" + dungeonId
                    + "'. Use /dungeon list to see available dungeons.");
        }

        // Check if player is already in a dungeon
        if (sessionManager.isPlayerInDungeon(playerRef)) {
            return CommandResult.failure("You are already in a dungeon. Use /dungeon leave first.");
        }

        Store<EntityStore> store = context.getWorld().getEntityStore().getStore();
        World currentWorld = context.getWorld();

        // Create session asynchronously
        sessionManager.createSession(dungeonId, playerRef, store, currentWorld)
                .thenAccept(session -> {
                    if (session != null) {
                        LOGGER.info("[Dungeons] Player entered dungeon '"
                                + config.getDisplayName() + "'");
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "[Dungeons] Failed to create dungeon session", ex);
                    return null;
                });

        return CommandResult.success("Entering dungeon: " + config.getDisplayName() + "...");
    }
}
