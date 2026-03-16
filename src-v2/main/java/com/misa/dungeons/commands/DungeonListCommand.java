package com.misa.dungeons.commands;

import com.hytale.server.command.Command;
import com.hytale.server.command.CommandContext;
import com.hytale.server.command.CommandResult;
import com.misa.dungeons.config.DungeonConfig;
import com.misa.dungeons.manager.DungeonRegistry;
import com.misa.dungeons.manager.SessionManager;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Command: /dungeon list
 * <p>
 * Lists all registered dungeons with their display name, difficulty,
 * room count, and player limits.
 */
public class DungeonListCommand implements Command {

    private static final Logger LOGGER = Logger.getLogger(DungeonListCommand.class.getName());

    private final SessionManager sessionManager;

    public DungeonListCommand(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() {
        return "dungeon list";
    }

    @Override
    public String getDescription() {
        return "List all available dungeons.";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        DungeonRegistry registry = sessionManager.getRegistry();
        Collection<DungeonConfig> configs = registry.getAllConfigs();

        if (configs.isEmpty()) {
            return CommandResult.success("No dungeons are currently registered.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Available Dungeons (").append(configs.size()).append(") ===\n");

        for (DungeonConfig config : configs) {
            sb.append(" - ").append(config.getId())
              .append(": ").append(config.getDisplayName())
              .append(" [").append(config.getDifficulty()).append("]")
              .append(" (").append(config.getRoomCount()).append(" rooms")
              .append(", ").append(config.getMinPlayers()).append("-")
              .append(config.getMaxPlayers()).append(" players")
              .append(", ").append(config.getTimeLimit()).append("s limit")
              .append(")\n");
        }

        sb.append("Use /dungeon enter <id> to start a dungeon.");
        return CommandResult.success(sb.toString());
    }
}
