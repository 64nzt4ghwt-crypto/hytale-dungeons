package com.howlstudio.dungeons.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.howlstudio.dungeons.DungeonManager;
import com.howlstudio.dungeons.model.*;
import java.util.*;

/** /dungeon [list | join <id> | leave | status] */
public class DungeonCommand extends AbstractPlayerCommand {
    private final DungeonManager manager;
    public DungeonCommand(DungeonManager manager) { super("dungeon", "Dungeon system. Usage: /dungeon [list|join <id>|leave|status]"); this.manager = manager; }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef playerRef, World world) {
        String input = ctx.getInputString().trim();
        String[] parts = input.split("\\s+", 3);
        String sub = parts.length > 1 ? parts[1].toLowerCase() : "list";
        UUID uuid = playerRef.getUuid();

        switch (sub) {
            case "list": {
                playerRef.sendMessage(Message.raw("§6§l[ Dungeons ]"));
                for (var def : manager.getDefs()) {
                    playerRef.sendMessage(Message.raw("  §e" + def.id + " §7— §f" + def.displayName + " §7(lvl " + def.minLevel + "+, max " + def.maxPlayers + "p)"));
                }
                playerRef.sendMessage(Message.raw("§7Use §e/dungeon join <id>§7 to create or join a run."));
                break;
            }
            case "join": {
                if (parts.length < 3) { playerRef.sendMessage(Message.raw("§c[Dungeons] Usage: /dungeon join <dungeon-id>")); return; }
                if (manager.getPlayerInstance(uuid).isPresent()) { playerRef.sendMessage(Message.raw("§c[Dungeons] You're already in a dungeon. /dungeon leave first.")); return; }
                DungeonDef def = manager.getDef(parts[2]);
                if (def == null) { playerRef.sendMessage(Message.raw("§c[Dungeons] Unknown dungeon §e" + parts[2] + "§c. Try §e/dungeon list§c.")); return; }
                // Find waiting instance or create new
                Optional<DungeonInstance> waiting = manager.getActiveInstances().stream()
                    .filter(i -> i.def.id.equals(def.id) && i.state == DungeonInstance.State.WAITING && !i.isFull())
                    .findFirst();
                DungeonInstance inst = waiting.orElseGet(() -> manager.createInstance(def));
                manager.joinInstance(uuid, inst.instanceId);
                playerRef.sendMessage(Message.raw("§a[Dungeons] §fJoined §e" + def.displayName + "§f (instance §7" + inst.instanceId + "§f, " + inst.playerUuids.size() + "/" + def.maxPlayers + " players)."));
                if (inst.playerUuids.size() >= 1) {
                    inst.start();
                    playerRef.sendMessage(Message.raw("§6[Dungeons] §fDungeon started! Good luck."));
                }
                break;
            }
            case "leave": {
                if (manager.getPlayerInstance(uuid).isEmpty()) { playerRef.sendMessage(Message.raw("§c[Dungeons] You're not in a dungeon.")); return; }
                manager.leaveInstance(uuid);
                playerRef.sendMessage(Message.raw("§e[Dungeons] §fYou left the dungeon."));
                break;
            }
            case "status": {
                Optional<DungeonInstance> inst = manager.getPlayerInstance(uuid);
                if (inst.isEmpty()) { playerRef.sendMessage(Message.raw("§e[Dungeons] §fYou're not in a dungeon.")); return; }
                DungeonInstance i = inst.get();
                long elapsed = i.startTime > 0 ? (System.currentTimeMillis() - i.startTime) / 1000 : 0;
                playerRef.sendMessage(Message.raw("§6[Dungeons] §f" + i.def.displayName + " | Room " + (i.currentRoom+1) + " | " + i.playerUuids.size() + " players | " + elapsed + "s elapsed"));
                break;
            }
            default:
                playerRef.sendMessage(Message.raw("§c[Dungeons] Usage: /dungeon [list|join <id>|leave|status]"));
        }
    }
}
