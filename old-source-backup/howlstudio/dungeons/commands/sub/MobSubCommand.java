package com.howlstudio.dungeons.commands.sub;

import com.howlstudio.dungeons.config.SpawnPointConfig;
import com.howlstudio.dungeons.manager.DungeonManager;
import com.howlstudio.dungeons.util.Msg;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * /dungeon mob add <dungeon> <entity> <position> [options]
 * 
 * Position can be:
 *   player       - uses command sender's current position
 *   coords x y z - specific world coordinates
 * 
 * Options (key-value pairs after position):
 *   amount <number>  - how many to spawn (default 1)
 *   radius <number>  - spread radius (default 0)
 *   wave <number>    - wave number (default 1)
 *   chance <0-100>   - spawn probability (default 100)
 *   room <name>      - which room (default "default")
 */
public class MobSubCommand implements SubCommand {

    private final DungeonManager dungeonManager;

    public MobSubCommand(DungeonManager manager) {
        this.dungeonManager = manager;
    }

    @Override
    public void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                        PlayerRef playerRef, Player player, World world, String args) {

        String[] parts = args.trim().split("\\s+");

        // Must start with "add"
        if (parts.length < 1 || !parts[0].equalsIgnoreCase("add")) {
            ctx.sendMessage(Msg.error("Usage: /dungeon mob add <dungeon> <entity> <position> [options]"));
            ctx.sendMessage(Msg.hint("Position: player | coords <x> <y> <z>"));
            ctx.sendMessage(Msg.hint("Options: amount <n> radius <n> wave <n> chance <n> room <name>"));
            return;
        }

        if (parts.length < 4) {
            ctx.sendMessage(Msg.error("Not enough arguments"));
            ctx.sendMessage(Msg.hint("Usage: /dungeon mob add <dungeon> <entity> <position> [options]"));
            return;
        }

        String dungeonName = parts[1];
        String entityType = parts[2];
        String posType = parts[3].toLowerCase();

        // Check dungeon exists (template or registered world)
        if (!dungeonManager.hasDungeon(dungeonName)) {
            ctx.sendMessage(Msg.error("Unknown dungeon: " + dungeonName));
            ctx.sendMessage(Msg.hint("Register a world first: /dungeon register <world>"));
            return;
        }

        // Parse position
        double x, y, z;
        int nextArgIndex;

        if (posType.equals("player")) {
            // Get position from TransformComponent
            TransformComponent transform = player.getTransformComponent();
            if (transform == null) {
                ctx.sendMessage(Msg.error("Could not get your position"));
                return;
            }
            Vector3d pos = transform.getPosition();
            if (pos == null) {
                ctx.sendMessage(Msg.error("Could not get your position"));
                return;
            }
            x = pos.x;
            y = pos.y;
            z = pos.z;
            nextArgIndex = 4;
        } else if (posType.equals("coords")) {
            if (parts.length < 7) {
                ctx.sendMessage(Msg.error("coords requires x y z values"));
                ctx.sendMessage(Msg.hint("Example: /dungeon mob add my_dungeon skeleton coords 10 64 -5"));
                return;
            }
            try {
                x = Double.parseDouble(parts[4]);
                y = Double.parseDouble(parts[5]);
                z = Double.parseDouble(parts[6]);
            } catch (NumberFormatException e) {
                ctx.sendMessage(Msg.error("Invalid coordinates. Expected numbers for x y z"));
                return;
            }
            nextArgIndex = 7;
        } else {
            ctx.sendMessage(Msg.error("Unknown position type: " + posType));
            ctx.sendMessage(Msg.hint("Use 'player' for your position or 'coords <x> <y> <z>'"));
            return;
        }

        // Parse optional parameters
        int amount = 1;
        double radius = 0;
        int wave = 1;
        int chance = 100;
        String roomName = "default";

        for (int i = nextArgIndex; i < parts.length - 1; i += 2) {
            String key = parts[i].toLowerCase();
            String val = parts[i + 1];

            try {
                switch (key) {
                    case "amount":
                        amount = Integer.parseInt(val);
                        if (amount < 1) amount = 1;
                        break;
                    case "radius":
                        radius = Double.parseDouble(val);
                        if (radius < 0) radius = 0;
                        break;
                    case "wave":
                        wave = Integer.parseInt(val);
                        if (wave < 1) wave = 1;
                        break;
                    case "chance":
                        chance = Integer.parseInt(val);
                        if (chance < 0) chance = 0;
                        if (chance > 100) chance = 100;
                        break;
                    case "room":
                        roomName = val;
                        break;
                    default:
                        ctx.sendMessage(Msg.warn("Unknown option: " + key + " (ignoring)"));
                        break;
                }
            } catch (NumberFormatException e) {
                ctx.sendMessage(Msg.warn("Invalid value for " + key + ": " + val + " (using default)"));
            }
        }

        // Build the spawn point config
        SpawnPointConfig spawnPoint = new SpawnPointConfig(entityType, x, y, z);
        spawnPoint.setAmount(amount);
        spawnPoint.setRadius(radius);
        spawnPoint.setWave(wave);
        spawnPoint.setChance(chance);

        // Save to the world config
        dungeonManager.getWorldConfig().addSpawnPoint(dungeonName, roomName, spawnPoint);

        // Confirm to player
        ctx.sendMessage(Msg.success("Added mob spawn point to " + dungeonName));
        ctx.sendMessage(Msg.bullet("Entity", entityType));
        ctx.sendMessage(Msg.bullet("Position", String.format("%.1f, %.1f, %.1f", x, y, z)));
        ctx.sendMessage(Msg.bullet("Room", roomName));
        if (amount > 1) {
            ctx.sendMessage(Msg.bullet("Amount", String.valueOf(amount)));
        }
        if (radius > 0) {
            ctx.sendMessage(Msg.bullet("Radius", String.valueOf(radius)));
        }
        if (wave > 1) {
            ctx.sendMessage(Msg.bullet("Wave", String.valueOf(wave)));
        }
        if (chance < 100) {
            ctx.sendMessage(Msg.bullet("Chance", chance + "%"));
        }
    }
}
