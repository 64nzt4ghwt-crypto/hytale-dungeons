package com.misa.dungeons.commands;

import com.hytale.server.command.Command;
import com.hytale.server.command.CommandContext;
import com.hytale.server.command.CommandResult;
import com.hytale.server.ecs.Ref;
import com.hytale.server.ecs.store.EntityStore;
import com.hytale.server.ecs.store.Store;
import com.misa.dungeons.manager.DungeonSession;
import com.misa.dungeons.manager.SessionManager;

import java.util.logging.Logger;

/**
 * Command: /dungeon leave
 * <p>
 * Removes the executing player from their current dungeon session.
 * Teleports them back to their return point (where they entered from).
 * If they are the last player, the session is cleaned up.
 */
public class DungeonLeaveCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(DungeonLeaveCommand.class.getName());

    private final SessionManager sessionManager;

    public DungeonLeaveCommand(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() {
        return "dungeon leave";
    }

    @Override
    public String getDescription() {
        return "Leave the current dungeon and return to the hub.";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Ref playerRef = context.getPlayerRef();
        if (playerRef == null) {
            return CommandResult.failure("This command can only be run by a player.");
        }

        if (!sessionManager.isPlayerInDungeon(playerRef)) {
            return CommandResult.failure("You are not currently in a dungeon.");
        }

        DungeonSession session = sessionManager.getSessionForPlayer(playerRef);
        String dungeonName = session != null ? session.getConfig().getDisplayName() : "unknown";

        Store<EntityStore> store = context.getWorld().getEntityStore().getStore();
        sessionManager.removePlayer(playerRef, store);

        LOGGER.info("[Dungeons] Player left dungeon '" + dungeonName + "'");
        return CommandResult.success("Left dungeon: " + dungeonName);
    }
}
