package com.misa.dungeons.commands;

import com.hytale.server.command.Command;
import com.hytale.server.command.CommandContext;
import com.hytale.server.command.CommandResult;
import com.hytale.server.ecs.Ref;
import com.misa.dungeons.manager.DungeonSession;
import com.misa.dungeons.manager.SessionManager;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Command: /dungeon forceend [sessionId]
 * <p>
 * Admin command to forcefully end a dungeon session.
 * If no session ID is provided, ends the session of the executing player (if any).
 */
public class DungeonForceEndCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(DungeonForceEndCommand.class.getName());

    private final SessionManager sessionManager;

    public DungeonForceEndCommand(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() {
        return "dungeon forceend";
    }

    @Override
    public String getDescription() {
        return "Forcefully end a dungeon session. Admin only. Usage: /dungeon forceend [sessionId]";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        String[] args = context.getArgs();

        UUID targetSessionId = null;

        if (args.length >= 1) {
            // Parse session ID from argument
            try {
                targetSessionId = UUID.fromString(args[0]);
            } catch (IllegalArgumentException e) {
                return CommandResult.failure("Invalid session ID: " + args[0]);
            }
        } else {
            // Try to use the executing player's session
            Ref playerRef = context.getPlayerRef();
            if (playerRef != null) {
                DungeonSession session = sessionManager.getSessionForPlayer(playerRef);
                if (session != null) {
                    targetSessionId = session.getSessionId();
                }
            }
        }

        if (targetSessionId == null) {
            return CommandResult.failure(
                    "No session specified. Usage: /dungeon forceend [sessionId]");
        }

        DungeonSession session = sessionManager.getSession(targetSessionId);
        if (session == null) {
            return CommandResult.failure("Session not found: " + targetSessionId);
        }

        String dungeonName = session.getConfig().getDisplayName();
        int playerCount = session.getPlayerCount();

        sessionManager.endSession(targetSessionId);

        String msg = "Force-ended session for '" + dungeonName + "' with "
                + playerCount + " player(s).";
        LOGGER.info("[Dungeons] " + msg);
        return CommandResult.success(msg);
    }
}
