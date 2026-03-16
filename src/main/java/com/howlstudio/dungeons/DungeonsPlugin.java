package com.howlstudio.dungeons;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.howlstudio.dungeons.commands.DungeonCommand;

/**
 * Dungeons — Instanced dungeon system for Hytale servers.
 *
 * Features:
 *   - 4 built-in dungeons (Cave of Spiders, Ruined Keep, Shadow Vault, Void Citadel)
 *   - Party-based instanced runs (1-5 players)
 *   - Join queue system — multiple groups per dungeon type
 *   - Room-based progression tracking
 *   - Reward pool per dungeon
 *
 * Commands:
 *   /dungeon list              — list available dungeons
 *   /dungeon join <id>         — join or create a run
 *   /dungeon leave             — exit current run
 *   /dungeon status            — show current run status
 */
public final class DungeonsPlugin extends JavaPlugin {
    private DungeonManager manager;

    public DungeonsPlugin(JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        System.out.println("[Dungeons] Loading Dungeons v1.0.0...");
        manager = new DungeonManager();
        CommandManager.get().register(new DungeonCommand(manager));
        System.out.println("[Dungeons] Loaded. " + manager.getDefs().size() + " dungeons available.");
    }

    @Override
    protected void shutdown() {
        System.out.println("[Dungeons] Stopped. " + manager.getActiveInstances().size() + " instances cleared.");
    }
}
