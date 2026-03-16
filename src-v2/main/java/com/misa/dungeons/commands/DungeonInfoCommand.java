package com.misa.dungeons.commands;

import com.hytale.server.command.Command;
import com.hytale.server.command.CommandContext;
import com.hytale.server.command.CommandResult;
import com.hytale.server.ecs.Ref;
import com.misa.dungeons.config.DungeonConfig;
import com.misa.dungeons.config.RoomConfig;
import com.misa.dungeons.manager.DungeonSession;
import com.misa.dungeons.manager.SessionManager;

/**
 * Command: /dungeon info
 * <p>
 * Displays detailed information about the player's current dungeon session:
 * dungeon name, current room, mobs remaining, elapsed time, death count, etc.
 */
public class DungeonInfoCommand implements Command {

    private final SessionManager sessionManager;

    public DungeonInfoCommand(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public String getName() {
        return "dungeon info";
    }

    @Override
    public String getDescription() {
        return "Show info about your current dungeon session.";
    }

    @Override
    public CommandResult execute(CommandContext context) {
        Ref playerRef = context.getPlayerRef();
        if (playerRef == null) {
            return CommandResult.failure("This command can only be run by a player.");
        }

        DungeonSession session = sessionManager.getSessionForPlayer(playerRef);
        if (session == null) {
            return CommandResult.failure("You are not currently in a dungeon.");
        }

        DungeonConfig config = session.getConfig();
        RoomConfig currentRoom = session.getCurrentRoomConfig();

        StringBuilder sb = new StringBuilder();
        sb.append("=== Dungeon: ").append(config.getDisplayName()).append(" ===\n");
        sb.append("Difficulty: ").append(config.getDifficulty()).append("\n");
        sb.append("Room: ").append(session.getCurrentRoom() + 1)
          .append("/").append(config.getRoomCount());

        if (currentRoom != null) {
            sb.append(" - ").append(currentRoom.getName());
            if (currentRoom.isBoss()) {
                sb.append(" [BOSS]");
            }
        }
        sb.append("\n");

        sb.append("Wave: ").append(session.getCurrentWave() + 1);
        if (currentRoom != null) {
            sb.append("/").append(currentRoom.getWaves().size());
        }
        sb.append("\n");

        sb.append("Mobs remaining: ").append(session.getAliveMobCount()).append("\n");
        sb.append("Players: ").append(session.getPlayerCount())
          .append("/").append(config.getMaxPlayers()).append("\n");

        double elapsed = session.getTotalElapsedSeconds();
        int minutes = (int) (elapsed / 60);
        int seconds = (int) (elapsed % 60);
        sb.append("Time: ").append(String.format("%d:%02d", minutes, seconds));
        if (config.getTimeLimit() > 0) {
            int limitMin = config.getTimeLimit() / 60;
            int limitSec = config.getTimeLimit() % 60;
            sb.append(" / ").append(String.format("%d:%02d", limitMin, limitSec));
        }
        sb.append("\n");

        if (currentRoom != null && currentRoom.isTimed()) {
            double roomElapsed = session.getRoomTimer();
            int rMin = (int) (roomElapsed / 60);
            int rSec = (int) (roomElapsed % 60);
            int rlMin = currentRoom.getTimeLimitSeconds() / 60;
            int rlSec = currentRoom.getTimeLimitSeconds() % 60;
            sb.append("Room timer: ").append(String.format("%d:%02d / %d:%02d", rMin, rSec, rlMin, rlSec))
              .append("\n");
        }

        sb.append("Session: ").append(session.getSessionId().toString().substring(0, 8)).append("...");

        return CommandResult.success(sb.toString());
    }
}
