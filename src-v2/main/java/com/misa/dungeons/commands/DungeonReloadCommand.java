package com.misa.dungeons.commands;

import com.hytale.server.command.Command;
import com.hytale.server.command.CommandContext;
import com.hytale.server.command.CommandResult;
import com.misa.dungeons.manager.DungeonRegistry;
import com.misa.dungeons.manager.SessionManager;

import java.util.logging.Logger;

/**
 * Command: /dungeon reload
 * <p>
 * Hot-reloads all dungeon configuration files from disk.
 * Active sessions are NOT affected — they continue with their original config.
 * New sessions will use the reloaded configs.
 */
public class DungeonReloadCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(DungeonReloadCommand.class.getName());

    private final SessionManager sessionManager;

    public DungeonReloadCommand(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() {
        return "dungeon reload";
    }

    @Override
    public String getDescription() {
        return "Reload dungeon configurations from disk. Admin only.";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        DungeonRegistry registry = sessionManager.getRegistry();
        int loaded = registry.reload();

        String msg = "Reloaded " + loaded + " dungeon config(s). "
                + sessionManager.getActiveSessionCount() + " active session(s) unaffected.";

        LOGGER.info("[Dungeons] " + msg);
        return CommandResult.success(msg);
    }
}
