# Hytale Dungeon Plugin — Complete Build Specification
## For Codex CLI: Build the ENTIRE mod as one drag-and-drop package

---

## OVERVIEW

Build a Hytale server-side dungeon plugin from scratch. The output must be a COMPLETE, self-contained Gradle project that compiles to a single JAR + config files. When built, the user copies one folder into their Hytale mods directory and it works.

**Output structure:**
```
plugin/
├── build.gradle
├── settings.gradle
├── gradle/wrapper/...
├── gradlew, gradlew.bat
├── libs/HytaleServer.jar          (already exists, DO NOT modify)
├── src/main/java/com/howlstudio/dungeons/
│   ├── DungeonPlugin.java         (main plugin class)
│   ├── commands/                   (all commands)
│   ├── config/                     (template/config loading)
│   ├── events/                     (event listeners)
│   ├── manager/                    (dungeon instances, rooms)
│   ├── spawning/                   (mob spawner)
│   ├── systems/                    (tick system)
│   ├── ui/                         (HUD + UI pages)
│   ├── loot/                       (item giving)
│   └── util/                       (Msg utility)
└── src/main/resources/
    ├── META-INF/MANIFEST.MF
    └── Server/DungeonConfigs/      (sample dungeon JSONs)
```

---

## CRITICAL CONSTRAINTS (READ FIRST)

### Hytale API Rules
1. **Use `com.hypixel.hytale.math.vector.Vector3d`** — NOT `org.joml.Vector3d`
2. **Use `com.hypixel.hytale.math.vector.Vector3f`** for rotations
3. **`ArgTypes.STRING`** — there is NO `GREEDY_STRING`
4. **Position access:** `player.getTransformComponent().getPosition()` — NOT `player.getPosition()`
5. **Hytale font is ASCII only** — NO Unicode symbols (▰, ★, ⚔ render as `???`). Use ASCII: `[`, `]`, `*`, `#`, `-`, `>`, `!`
6. **Message API:** `Message.raw("text").color(Color).bold(true)` — NOT Minecraft `§` color codes
7. **Java 21** — can use records, pattern matching, etc.
8. **Vector3d has public fields:** `pos.x`, `pos.y`, `pos.z` (not just getters)

### Plugin Structure
- Main class extends `JavaPlugin`
- Must have `META-INF/MANIFEST.MF` with `Plugin-Class: com.howlstudio.dungeons.DungeonPlugin`
- Commands registered via `getCommandRegistry().registerCommand()`
- Events registered via `getEventRegistry().register()` or `HytaleServer.get().getEventBus()`
- ECS systems via `getEntityStoreRegistry().registerSystem()`
- Plugin ID: `com.howlstudio:DungeonPlugin`

### Build System
- **Kotlin DSL** Gradle (`build.gradle.kts` and `settings.gradle.kts` already exist — DO NOT overwrite)
- `libs/HytaleServer.jar` as `compileOnly` dependency (already configured)
- `com.google.code.gson:gson:2.11.0` as `implementation` dependency (for JSON config parsing)
- Target: `plugin/build/libs/DungeonPlugin-0.1.0.jar`
- Java 21 source/target
- Add a `tasks.jar` block to include `Plugin-Class` manifest attribute:
  ```kotlin
  tasks.jar {
      manifest {
          attributes("Plugin-Class" to "com.howlstudio.dungeons.DungeonPlugin")
      }
  }
  ```

---

## FEATURE SPECIFICATION

### 1. Dungeon Templates (JSON Config)

Load dungeon definitions from `Server/DungeonConfigs/*.json` at plugin startup.

**JSON Schema:**
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "minPlayers": 1,
  "maxPlayers": 4,
  "timeLimitSeconds": 900,
  "rooms": [
    {
      "name": "string",
      "isBoss": false,
      "waves": 1,
      "waveSpawns": [
        [
          {"role": "Skeleton_Fighter", "count": 3},
          {"role": "Skeleton_Archer", "count": 2}
        ],
        [
          {"role": "Skeleton_Knight", "count": 2}
        ]
      ],
      "loot": [
        {"itemId": "iron_longsword", "minCount": 1, "maxCount": 1, "chance": 0.3}
      ],
      "spawnCenter": {"x": 0, "y": 64, "z": 0},
      "spawnRadius": 5.0
    }
  ]
}
```

**If `waveSpawns` is absent, fall back to a flat `mobSpawns` array (single wave).**

**Room progression:** Players must clear ALL waves of the current room before advancing. After the last room (boss), the dungeon is complete.

### 2. Dungeon Manager

Central class managing all active dungeon instances.

**DungeonInstance state:**
- `id` (UUID)
- `templateId` (which dungeon)
- `players` (Set<UUID>)
- `currentRoom` (int, 0-indexed)
- `currentWave` (int, 0-indexed)
- `startTimeMs` (System.currentTimeMillis)
- `state` enum: `WAITING`, `ACTIVE`, `COMPLETED`, `FAILED`
- `spawnedMobUuids` (Set<UUID> per room — tracked for room-clear detection)
- `returnPositions` (Map<UUID, ReturnInfo> — world name + position per player)
- `lootGranted` (Set<String> — dedup keys like "room_0", "room_1")
- `warned5min`, `warned1min`, `warned30sec`, `warned10sec` (boolean flags)

**Key operations:**
- `createInstance(templateId, creator)` → creates instance, saves return positions
- `joinInstance(instanceId, player)` → adds player, saves their return position
- `leaveInstance(player)` → removes player, teleports back
- `startInstance(instanceId)` → sets ACTIVE, spawns room 0 mobs
- `advanceRoom(instanceId)` → increments room, spawns next room's mobs
- `advanceWave(instanceId)` → increments wave, spawns next wave's mobs
- `completeInstance(instanceId)` → announces victory, grants final loot, teleports all back
- `failInstance(instanceId)` → announces failure, despawns mobs, teleports all back

### 3. Mob Spawning System

**Spawn pattern (from API research):**
```java
NPCEntity npc = new NPCEntity(world);
npc.setRoleName(roleName);  // e.g. "Skeleton_Fighter"
Vector3d spawnPos = new Vector3d(
    center.x + (random.nextDouble() * 2 - 1) * radius,
    center.y,
    center.z + (random.nextDouble() * 2 - 1) * radius
);
world.spawnEntity(npc, spawnPos, new Vector3f(0, 0, 0));
// Track UUID for room-clear detection
instance.getSpawnedMobUuids().add(npc.getUuid());
```

**Available NPC role names** (use these exact strings with `setRoleName()`):

**Tier 1 - Easy:**
- `Skeleton_Fighter` (36 HP), `Skeleton_Archer` (~36 HP), `Skeleton_Scout` (~29 HP)
- `Goblin_Scavenger`, `Goblin_Scrapper`, `Goblin_Thief`, `Goblin_Miner`
- `Scarak_Louse` (~21 HP), `Scarak_Seeker` (~61 HP)
- `Spider` (~49 HP)

**Tier 2 - Medium:**
- `Skeleton_Knight` (74 HP), `Skeleton_Soldier` (~49 HP), `Skeleton_Mage` (~36 HP)
- `Trork_Warrior` (61 HP), `Trork_Guard` (~81 HP), `Trork_Hunter` (~49 HP)
- `Goblin_Lobber`, `Goblin_Ogre` (~170 HP)
- `Scarak_Fighter` (~81 HP), `Scarak_Defender` (~103 HP)
- `Outlander_Peon` (~49 HP), `Outlander_Marauder` (~81 HP)

**Tier 3 - Hard:**
- `Skeleton_Ranger` (~49 HP), `Skeleton_Archmage` (~49 HP)
- `Trork_Chieftain` (~103 HP), `Trork_Shaman` (~36 HP)
- `Outlander_Berserker` (103 HP), `Outlander_Brute` (~126 HP), `Outlander_Stalker` (~74 HP)
- `Zombie_Aberrant` (~61 HP), `Zombie_Aberrant_Big` (~81 HP)

**Bosses:**
- `Shadow_Knight` (400 HP, 119 dmg)
- `Goblin_Duke` (226 HP, multi-phase)
- `Scarak_Broodmother` (~145 HP)
- `Emberwulf` (193 HP)
- `Dragon_Fire` (400 HP), `Dragon_Frost` (400 HP)

**Dungeon-specific variants (pre-tuned by Hytale devs):**
- `Dungeon_Scarak_Fighter` (81 HP), `Dungeon_Scarak_Defender` (103 HP)
- `Dungeon_Scarak_Seeker` (61 HP), `Dungeon_Scarak_Louse` (21 HP)
- `Dungeon_Scarak_Broodmother` (145 HP)

**Despawn pattern:**
```java
npc.setToDespawn();   // NPC despawn with animation
// fallback:
entity.remove();      // immediate removal
```

### 4. Room-Clear Detection via EntityRemoveEvent

**DO NOT use polling.** Use the global EventBus:

```java
// In plugin setup:
HytaleServer.get().getEventBus().registerGlobal(EntityRemoveEvent.class, event -> {
    Entity entity = event.getEntity();
    UUID uuid = entity.getUuid();
    // Check all active instances for this mob UUID
    for (DungeonInstance instance : manager.getActiveInstances()) {
        if (instance.getSpawnedMobUuids().remove(uuid)) {
            // Check if wave/room is cleared
            if (instance.getSpawnedMobUuids().isEmpty()) {
                onWaveOrRoomCleared(instance);
            }
            break;
        }
    }
});
```

**EntityRemoveEvent** is in package `com.hypixel.hytale.server.core.event.events.entity`.
It has `getEntity()` returning the removed Entity.

### 5. Tick System (Timer + Periodic Checks)

**No built-in scheduler.** Use `HytaleServer.SCHEDULED_EXECUTOR` + `world.execute()`:

```java
// In plugin onEnable():
ScheduledExecutorService scheduler = HytaleServer.SCHEDULED_EXECUTOR;
scheduler.scheduleAtFixedRate(() -> {
    for (DungeonInstance instance : manager.getActiveInstances()) {
        World world = instance.getWorld();
        if (world != null && world.isAlive()) {
            world.execute(() -> tickInstance(instance));
        }
    }
}, 3, 3, TimeUnit.SECONDS);  // Every 3 seconds
```

**Tick logic (5-step loop):**
1. **Timer warnings** — check elapsed time, send color-coded warnings at 5min/1min/30s/10s remaining
2. **Time expiry** — if time's up, `failInstance()`
3. **Room cleared check** — if `spawnedMobUuids` is empty AND room has more waves → `advanceWave()`, else if last wave → room is cleared
4. **Room cleared handling** — grant loot, announce, advance to next room or complete dungeon
5. **HUD refresh** — update all player HUDs

### 6. Loot System (Actually Give Items)

**Pattern to give items to players:**
```java
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;

// Create item
ItemStack item = new ItemStack(itemId, quantity);  // e.g. new ItemStack("iron_longsword", 1)

// Give to player
Inventory inv = player.getInventory();
inv.getCombinedHotbarFirst().addItemStack(item);
```

**Loot granting flow:**
- On room clear, iterate room's loot table
- For each loot entry, roll `Math.random() < chance`
- If success, create `ItemStack(itemId, randomCount(min, max))`
- Give to ALL players in the instance
- Announce: "[LOOT] PlayerName received ItemName x3!"
- Track in `lootGranted` set to prevent double-granting

### 7. Return Teleport

**Save position on join:**
```java
// Save return info
TransformComponent tc = player.getTransformComponent();
Vector3d pos = tc.getPosition();
String worldName = player.getWorld().getName();
instance.saveReturnPosition(player.getUuid(), worldName, pos);
```

**Restore on leave/complete/fail:**
```java
// Teleport back
ReturnInfo info = instance.getReturnPosition(player.getUuid());
World returnWorld = Universe.get().getWorld(info.worldName);
if (returnWorld != null) {
    player.loadIntoWorld(returnWorld);
    player.getTransformComponent().teleportPosition(
        new Vector3d(info.x, info.y, info.z)
    );
}
```

### 8. HUD System (Persistent Overlay)

**Use `CustomUIHud`:**
```java
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;

public class DungeonHud extends CustomUIHud {
    public DungeonHud(PlayerRef playerRef) {
        super(playerRef);
    }
    
    @Override
    protected void build(UICommandBuilder commands) {
        // Set HUD data fields
        commands.set("dungeon.name", dungeonName);
        commands.set("dungeon.room", roomName);
        commands.set("dungeon.mobsLeft", mobsRemaining);
        commands.set("dungeon.timer", formatTime(timeLeft));
        commands.set("dungeon.party", partySize);
    }
}

// Show HUD:
DungeonHud hud = new DungeonHud(player.getPlayerRef());
player.getHudManager().setCustomHud(player.getPlayerRef(), hud);
hud.show();

// Update HUD:
UICommandBuilder cmds = new UICommandBuilder();
cmds.set("dungeon.mobsLeft", newCount);
hud.update(true, cmds);

// Remove HUD:
player.getHudManager().resetHud(player.getPlayerRef());
```

### 9. UI Pages (Modal Popups)

**Dungeon Browser (list available dungeons):**
```java
public class DungeonListPage extends CustomUIPage {
    public DungeonListPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
    }
    
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, 
                      UIEventBuilder events, Store<EntityStore> store) {
        // List dungeons
        for (DungeonTemplate t : templates) {
            commands.append("dungeons.list", /* dungeon data */);
        }
        // Add click handler
        events.addEventBinding(CustomUIEventBindingType.Activating, "dungeons.list");
    }
}

// Open page:
Player player = ...;
PlayerRef ref = player.getPlayerRef();
player.getPageManager().openCustomPage(
    ref.getReference(),
    player.getWorld().getEntityStore(),
    new DungeonListPage(ref)
);
```

### 10. Commands

All commands extend `AbstractPlayerCommand`. Signature:
```java
public void execute(CommandContext ctx, Store<EntityStore> store, 
                    Ref<EntityStore> ref, PlayerRef playerRef, World world)
```

**Player commands (standalone):**
| Command | Description |
|---------|-------------|
| `/dcreate <templateId>` | Create a new dungeon instance |
| `/djoin <instanceId>` | Join an existing instance |
| `/dleave` | Leave current dungeon |
| `/dlist` | List active dungeons |
| `/dinfo [instanceId]` | Show dungeon info |
| `/dbrowse` | Open dungeon browser UI page |
| `/dstatus` | Open dungeon status UI page |

**Admin command (`/dungeon <sub>`):**
| Subcommand | Description |
|------------|-------------|
| `/dungeon reload` | Reload dungeon configs from disk |
| `/dungeon forceend [instanceId]` | Force-end a dungeon |
| `/dungeon mob <role>` | Spawn a test mob |
| `/dungeon loot <itemId> [count]` | Give test loot |
| `/dungeon list` | Admin list with details |
| `/dungeon help` | Show all commands |

**Registration pattern:**
```java
getCommandRegistry().registerCommand("dcreate", new DungeonCreateCommand(manager));
// For subcommands, parse args manually in execute()
```

### 11. Message Utility (Msg.java)

Centralized formatting with themed colors. **ASCII ONLY — no Unicode!**

```java
public final class Msg {
    // Theme colors
    public static final Color PRIMARY = new Color(255, 170, 200);    // Pink
    public static final Color SECONDARY = new Color(170, 200, 255);  // Blue
    public static final Color SUCCESS = new Color(100, 255, 100);    // Green
    public static final Color ERROR = new Color(255, 80, 80);        // Red
    public static final Color WARNING = new Color(255, 200, 50);     // Yellow
    public static final Color AMBER = new Color(255, 180, 50);       // Amber
    public static final Color INFO = new Color(200, 200, 200);       // Gray
    public static final Color GOLD = new Color(255, 215, 0);         // Gold
    public static final Color HIGHLIGHT = new Color(255, 255, 100);  // Bright yellow
    
    // ASCII symbols (NO UNICODE!)
    public static final String ICON_SWORD = "[!]";
    public static final String ICON_SHIELD = "[#]";
    public static final String ICON_SKULL = "[X]";
    public static final String ICON_STAR = "[*]";
    public static final String ICON_HEART = "<3";
    public static final String ICON_ARROW = ">>";
    public static final String ICON_CHECK = "[OK]";
    public static final String ICON_CROSS = "[NO]";
    public static final String ICON_WARN = "[!!]";
    public static final String ICON_LOOT = "[$]";
    public static final String ICON_BOSS = "[BOSS]";
    public static final String ICON_TIMER = "[T]";
    public static final String DIVIDER = "--------------------";
    
    public static Message title(String text) {
        return Message.raw(text).color(PRIMARY).bold(true);
    }
    
    public static Message success(String text) {
        return Message.raw(ICON_CHECK + " " + text).color(SUCCESS);
    }
    
    public static Message error(String text) {
        return Message.raw(ICON_CROSS + " " + text).color(ERROR);
    }
    
    public static Message warning(String text) {
        return Message.raw(ICON_WARN + " " + text).color(WARNING);
    }
    
    public static Message info(String text) {
        return Message.raw(ICON_ARROW + " " + text).color(INFO);
    }
    
    public static Message loot(String text) {
        return Message.raw(ICON_LOOT + " " + text).color(GOLD);
    }
    
    public static Message boss(String text) {
        return Message.raw(ICON_BOSS + " " + text).color(ERROR).bold(true);
    }
    
    public static Message timer(String text) {
        return Message.raw(ICON_TIMER + " " + text).color(AMBER);
    }
    
    // Send to all players in instance
    public static void broadcast(Collection<Player> players, Message msg) {
        for (Player p : players) {
            p.sendMessage(msg);
        }
    }
}
```

### 12. Timer Warnings

Send color-coded warnings at specific time thresholds:

| Time Remaining | Color | Bold | Message |
|----------------|-------|------|---------|
| 5 minutes | AMBER | no | "[T] 5 minutes remaining!" |
| 1 minute | RED | no | "[T] 1 minute remaining!" |
| 30 seconds | RED | yes | "[T] 30 SECONDS remaining!" |
| 10 seconds | RED | yes | "[T] 10 SECONDS! HURRY!" |

Track with boolean flags on the instance (`warned5min`, etc.) to avoid repeats.

### 13. Wave System

Rooms can have multiple waves. Wave flow:
1. Room starts → spawn wave 0 mobs
2. All wave 0 mobs die (detected via EntityRemoveEvent) → spawn wave 1 mobs
3. All wave N (last) mobs die → room is cleared
4. Grant loot, announce, advance to next room

If room has only 1 wave (or no `waveSpawns`), it's just: spawn mobs → all die → room cleared.

---

## SAMPLE DUNGEON CONFIGS

### goblin_den.json
```json
{
  "id": "goblin_den",
  "name": "Goblin Den",
  "description": "A festering cave network infested with goblins and their chieftain.",
  "minPlayers": 1,
  "maxPlayers": 4,
  "timeLimitSeconds": 900,
  "rooms": [
    {
      "name": "Cave Entrance",
      "isBoss": false,
      "waves": 1,
      "waveSpawns": [
        [{"role": "Goblin_Scrapper", "count": 2}, {"role": "Goblin_Thief", "count": 1}]
      ],
      "loot": [],
      "spawnCenter": {"x": 0, "y": 64, "z": 0},
      "spawnRadius": 5.0
    },
    {
      "name": "Storage Tunnels",
      "isBoss": false,
      "waves": 2,
      "waveSpawns": [
        [{"role": "Goblin_Scrapper", "count": 3}, {"role": "Goblin_Lobber", "count": 1}],
        [{"role": "Goblin_Miner", "count": 2}, {"role": "Goblin_Scavenger", "count": 2}]
      ],
      "loot": [
        {"itemId": "iron_longsword", "minCount": 1, "maxCount": 1, "chance": 0.3},
        {"itemId": "gold_coin", "minCount": 5, "maxCount": 15, "chance": 0.7}
      ],
      "spawnCenter": {"x": 0, "y": 64, "z": 30},
      "spawnRadius": 6.0
    },
    {
      "name": "Chieftains Throne",
      "isBoss": true,
      "waves": 2,
      "waveSpawns": [
        [{"role": "Goblin_Scrapper", "count": 3}, {"role": "Goblin_Lobber", "count": 2}],
        [{"role": "Goblin_Duke", "count": 1}, {"role": "Goblin_Ogre", "count": 1}]
      ],
      "loot": [
        {"itemId": "steel_battleaxe", "minCount": 1, "maxCount": 1, "chance": 0.25},
        {"itemId": "gold_coin", "minCount": 30, "maxCount": 60, "chance": 1.0}
      ],
      "spawnCenter": {"x": 0, "y": 64, "z": 60},
      "spawnRadius": 8.0
    }
  ]
}
```

### skeleton_crypt.json
```json
{
  "id": "skeleton_crypt",
  "name": "Skeleton Crypt",
  "description": "A dark crypt crawling with undead. The Shadow Knight guards the deepest chamber.",
  "minPlayers": 1,
  "maxPlayers": 4,
  "timeLimitSeconds": 720,
  "rooms": [
    {
      "name": "Entrance Hall",
      "isBoss": false,
      "waves": 1,
      "waveSpawns": [
        [{"role": "Skeleton_Fighter", "count": 3}, {"role": "Skeleton_Scout", "count": 1}]
      ],
      "loot": [],
      "spawnCenter": {"x": 0, "y": 64, "z": 0},
      "spawnRadius": 5.0
    },
    {
      "name": "Bone Gallery",
      "isBoss": false,
      "waves": 2,
      "waveSpawns": [
        [{"role": "Skeleton_Fighter", "count": 3}, {"role": "Skeleton_Archer", "count": 2}],
        [{"role": "Skeleton_Knight", "count": 1}, {"role": "Skeleton_Mage", "count": 1}]
      ],
      "loot": [
        {"itemId": "gold_coin", "minCount": 5, "maxCount": 15, "chance": 0.8},
        {"itemId": "lesser_healing_potion", "minCount": 1, "maxCount": 3, "chance": 0.5}
      ],
      "spawnCenter": {"x": 0, "y": 64, "z": 30},
      "spawnRadius": 6.0
    },
    {
      "name": "Crypt Lords Chamber",
      "isBoss": true,
      "waves": 2,
      "waveSpawns": [
        [{"role": "Skeleton_Soldier", "count": 3}, {"role": "Skeleton_Archmage", "count": 1}],
        [{"role": "Shadow_Knight", "count": 1}]
      ],
      "loot": [
        {"itemId": "enchanted_blade", "minCount": 1, "maxCount": 1, "chance": 0.3},
        {"itemId": "gold_coin", "minCount": 20, "maxCount": 50, "chance": 1.0}
      ],
      "spawnCenter": {"x": 0, "y": 64, "z": 60},
      "spawnRadius": 8.0
    }
  ]
}
```

### scarak_hive.json
```json
{
  "id": "scarak_hive",
  "name": "Scarak Hive",
  "description": "A chittering insect hive deep underground. The Broodmother awaits.",
  "minPlayers": 2,
  "maxPlayers": 4,
  "timeLimitSeconds": 720,
  "rooms": [
    {
      "name": "Hive Entrance",
      "isBoss": false,
      "waves": 1,
      "waveSpawns": [
        [{"role": "Dungeon_Scarak_Louse", "count": 5}, {"role": "Dungeon_Scarak_Seeker", "count": 2}]
      ],
      "loot": [],
      "spawnCenter": {"x": 0, "y": 64, "z": 0},
      "spawnRadius": 5.0
    },
    {
      "name": "Breeding Chambers",
      "isBoss": false,
      "waves": 2,
      "waveSpawns": [
        [{"role": "Dungeon_Scarak_Fighter", "count": 3}, {"role": "Dungeon_Scarak_Louse", "count": 4}],
        [{"role": "Dungeon_Scarak_Defender", "count": 2}, {"role": "Dungeon_Scarak_Fighter", "count": 2}]
      ],
      "loot": [
        {"itemId": "scarak_chitin", "minCount": 2, "maxCount": 5, "chance": 0.8},
        {"itemId": "gold_coin", "minCount": 10, "maxCount": 25, "chance": 0.6}
      ],
      "spawnCenter": {"x": 0, "y": 64, "z": 30},
      "spawnRadius": 7.0
    },
    {
      "name": "Broodmothers Lair",
      "isBoss": true,
      "waves": 2,
      "waveSpawns": [
        [{"role": "Dungeon_Scarak_Fighter", "count": 4}, {"role": "Dungeon_Scarak_Defender", "count": 2}],
        [{"role": "Dungeon_Scarak_Broodmother", "count": 1}, {"role": "Dungeon_Scarak_Louse", "count": 6}]
      ],
      "loot": [
        {"itemId": "broodmother_fang", "minCount": 1, "maxCount": 1, "chance": 0.4},
        {"itemId": "gold_coin", "minCount": 30, "maxCount": 50, "chance": 1.0}
      ],
      "spawnCenter": {"x": 0, "y": 64, "z": 60},
      "spawnRadius": 10.0
    }
  ]
}
```

---

## EXACT API SIGNATURES (from javap -p decompilation)

### World
```java
// com.hypixel.hytale.server.core.universe.world.World
public <T extends Entity> T spawnEntity(T entity, Vector3d position, Vector3f rotation);
public Entity getEntity(UUID uuid);
public List<Player> getPlayers();
public String getName();
public boolean isAlive();
public void execute(Runnable task);
public EventRegistry getEventRegistry();
public long getTick();
```

### Entity
```java
// com.hypixel.hytale.server.core.entity.Entity
public boolean remove();
public UUID getUuid();
public World getWorld();
public boolean wasRemoved();
public void loadIntoWorld(World world);
public TransformComponent getTransformComponent();
```

### NPCEntity
```java
// com.hypixel.hytale.server.npc.entities.NPCEntity
public NPCEntity(World world);
public void setRoleName(String roleName);
public String getRoleName();
public void setToDespawn();
public boolean isDespawning();
```

### Player
```java
// com.hypixel.hytale.server.core.entity.entities.Player
public PageManager getPageManager();
public HudManager getHudManager();
public PlayerRef getPlayerRef();
public void sendMessage(Message message);
public Inventory getInventory();  // inherited from LivingEntity
```

### TransformComponent
```java
// com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
public Vector3d getPosition();
public void teleportPosition(Vector3d position);
public Vector3f getRotation();
public void teleportRotation(Vector3f rotation);
```

### ItemStack
```java
// com.hypixel.hytale.server.core.inventory.ItemStack
public ItemStack(String itemId);
public ItemStack(String itemId, int quantity);
public String getItemId();
public int getQuantity();
public boolean isEmpty();
```

### Inventory
```java
// com.hypixel.hytale.server.core.inventory.Inventory
public ItemContainer getHotbar();
public ItemContainer getStorage();
public ItemContainer getArmor();
public CombinedItemContainer getCombinedHotbarFirst();
```

### ItemContainer
```java
// com.hypixel.hytale.server.core.inventory.ItemContainer
public ItemStackTransaction addItemStack(ItemStack stack);
```

### CustomUIHud
```java
// com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
public CustomUIHud(PlayerRef playerRef);
public void show();
public void update(boolean force, UICommandBuilder commands);
protected abstract void build(UICommandBuilder commands);
```

### HudManager
```java
// com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager
public void setCustomHud(PlayerRef ref, CustomUIHud hud);
public void resetHud(PlayerRef ref);
```

### CustomUIPage
```java
// com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
public CustomUIPage(PlayerRef playerRef, CustomPageLifetime lifetime);
public abstract void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store);
protected void rebuild();
protected void close();
```

### PageManager
```java
// com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager
public void openCustomPage(Ref<EntityStore> ref, Store<EntityStore> store, CustomUIPage page);
```

### UICommandBuilder
```java
// com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
public UICommandBuilder set(String selector, String value);
public UICommandBuilder set(String selector, boolean value);
public UICommandBuilder set(String selector, int value);
public UICommandBuilder set(String selector, Message value);
public UICommandBuilder append(String selector, String data);
public UICommandBuilder clear(String selector);
```

### UIEventBuilder
```java
// com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
public UIEventBuilder addEventBinding(CustomUIEventBindingType type, String selector);
```

### EventBus
```java
// com.hypixel.hytale.event.EventBus (via HytaleServer.get().getEventBus())
public <E extends IBaseEvent<Void>> EventRegistration<Void, E> register(Class<? super E>, Consumer<E>);
public <K, E extends IBaseEvent<K>> EventRegistration<K, E> registerGlobal(Class<? super E>, Consumer<E>);
```

### HytaleServer
```java
// com.hypixel.hytale.server.HytaleServer
public static HytaleServer get();
public EventBus getEventBus();
public static final ScheduledExecutorService SCHEDULED_EXECUTOR;
```

### Universe
```java
// com.hypixel.hytale.server.core.universe.Universe
public static Universe get();
public World getWorld(String name);
public CompletableFuture<World> loadWorld(String name);
```

### Key Enums
```java
// com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
CantClose, CanDismiss, CanDismissOrCloseThroughInteraction

// com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
Activating, RightClicking, Dismissing, ValueChanged, ... (24 values)
```

### Key Imports Cheat Sheet
```java
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemContainer;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.command.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.CommandContext;
import com.hypixel.hytale.server.core.command.args.ArgTypes;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.chat.Message;
import java.awt.Color;
```

---

## GAMEFLOW SUMMARY

```
Player runs /dcreate goblin_den
  → DungeonInstance created (state=ACTIVE)
  → Return position saved
  → HUD appears (dungeon name, room, mobs, timer)
  → Room 0 mobs spawn around spawnCenter

Mobs die (EntityRemoveEvent fires)
  → UUIDs removed from spawnedMobUuids
  → When empty: wave/room clear check
  → If more waves: spawn next wave
  → If last wave: ROOM CLEARED!
    → Announce "[OK] Room cleared: Cave Entrance!"
    → Grant loot to all players (if any)
    → "[LOOT] PlayerName received iron_longsword x1!"

Auto-advance to next room
  → Reset wave counter
  → Spawn room 1 mobs
  → HUD updates (room name, mob count)

Boss room (isBoss=true)
  → "[BOSS] BOSS INCOMING: Goblin Duke!" announcement
  → Boss wave spawns
  → All die → "[BOSS] BOSS DEFEATED!"

Last room cleared
  → "DUNGEON COMPLETE!" with elapsed time
  → Grant final loot
  → Teleport all players back to saved positions
  → Remove HUDs
  → Clean up instance

Timer expires
  → "TIME'S UP!" announcement
  → Despawn all remaining mobs
  → Teleport all players back
  → Instance marked FAILED

Warnings fire at: 5min, 1min, 30sec, 10sec remaining
```

---

## BUILD INSTRUCTIONS

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd plugin
./gradlew clean build
```

Output JAR: `plugin/build/libs/DungeonPlugin-0.1.0.jar`

**Deployment:** Copy JAR + `Server/DungeonConfigs/` folder to:
`<HytaleUserData>/Saves/<WorldName>/mods/com.howlstudio_DungeonPlugin/`

**World config.json:**
```json
{"Mods": {"com.howlstudio:DungeonPlugin": {"Enabled": true}}}
```

---

## WHAT TO BUILD

Write ALL Java source files from scratch. The existing code should be replaced entirely — build clean using the patterns and API signatures above. Every file must compile against `libs/HytaleServer.jar` (Java 21).

**File count target:** ~20-25 well-organized files
**Line count target:** ~3,000-4,000 lines total (clean, documented)

Prioritize:
1. COMPILABILITY — must build with zero errors
2. Correct API usage — follow the exact signatures above
3. Clean architecture — manager/spawner/tick/commands/ui separation
4. All features working end-to-end per the gameflow above
