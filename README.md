# Dungeon Plugin for Hytale

A full-featured dungeon system for Hytale servers. Create multi-room dungeons with mob spawning, boss fights, loot drops, and party support.

## Features

- **Multi-room dungeons** with sequential room progression
- **Wave spawning** - multiple waves of mobs per room
- **Boss rooms** with special indicators and announcements
- **Loot system** - per-room drops with configurable chance and count
- **Party system** - invite friends, scale difficulty
- **Dungeon HUD** - persistent overlay showing progress, mobs, timer
- **UI panels** - clickable dungeon browser and live status panels
- **Portal system** - proximity-based dungeon entry points
- **Return teleport** - saves player position, restores on exit
- **Timer** with 5min/1min/30s/10s warnings
- **Death handling** - solo = fail, party = respawn at entrance
- **Hot-reload** - JSON configs reloadable without restart
- **Admin tools** - register worlds, add mobs/loot, force end

## Commands

### Player Commands
| Command | Description |
|---------|-------------|
| `/dcreate <template>` | Create and enter a dungeon |
| `/dlist` | List available dungeon templates |
| `/dbrowse` | Open dungeon browser UI panel |
| `/djoin <player>` | Join another player's dungeon |
| `/dleave` | Leave current dungeon |
| `/dinfo` | Show dungeon status (text) |
| `/dstatus` | Open dungeon status UI panel |
| `/dungeon help` | Full command reference |

### Admin Commands
| Command | Description |
|---------|-------------|
| `/dungeon register <world>` | Register a world as a dungeon |
| `/dungeon unregister <world>` | Unregister a dungeon world |
| `/dungeon mob add <dungeon> <entity> [x y z]` | Add mob spawn point |
| `/dungeon loot add <dungeon> <room> <item> <chance>` | Add loot drop |
| `/dungeon setportal <dungeon>` | Set portal at your location |
| `/dungeon reload` | Reload all configs from disk |
| `/dungeon forceend` | Force-end all active dungeons |

## Configuration

### Template-based Dungeons (JSON)

Place `.json` files in your mod's `DungeonConfigs/` directory:

```json
{
  "id": "goblin_den",
  "name": "Goblin Den",
  "description": "A dark cave infested with goblins.",
  "minPlayers": 1,
  "maxPlayers": 4,
  "timeLimitSeconds": 600,
  "rooms": [
    {
      "name": "Entrance Cavern",
      "isBoss": false,
      "waves": 1,
      "mobSpawns": [
        { "type": "Goblin_Scavenger", "count": 3 },
        { "type": "Goblin_Miner", "count": 2 }
      ],
      "loot": [
        { "item": "iron_ore", "minCount": 2, "maxCount": 5, "chance": 0.8 }
      ]
    },
    {
      "name": "Ogre's Lair",
      "isBoss": true,
      "waves": 1,
      "mobSpawns": [
        { "type": "Goblin_Ogre", "count": 1 }
      ],
      "loot": [
        { "item": "ogre_club", "minCount": 1, "maxCount": 1, "chance": 0.25 }
      ]
    }
  ],
  "difficulty": {
    "baseHealth": 1.0,
    "baseDamage": 1.0,
    "perPlayerHealthScale": 0.3,
    "perPlayerDamageScale": 0.1
  }
}
```

### Wave Spawning

For multi-wave rooms, use `waveSpawns` instead of `mobSpawns`:

```json
{
  "name": "Hall of Bones",
  "waves": 2,
  "waveSpawns": [
    [
      { "type": "Skeleton_Fighter", "count": 3 }
    ],
    [
      { "type": "Skeleton_Fighter", "count": 4 },
      { "type": "Skeleton_Archer", "count": 2 }
    ]
  ]
}
```

### World-based Dungeons

Register existing worlds as dungeons using admin commands:

```
/dungeon register my_dungeon_world
/dungeon mob add my_dungeon_world Skeleton_Fighter 10 64 20
/dungeon loot add my_dungeon_world room1 gold_ingot 0.5
/dungeon setportal my_dungeon_world
```

## Architecture

```
com.howlstudio.dungeons/
  DungeonPlugin.java          -- Entry point, wires everything
  commands/
    DungeonCommand.java        -- Root /dungeon <sub> router
    DungeonCreateCommand.java  -- /dcreate shortcut
    DungeonListCommand.java    -- /dlist shortcut
    DungeonInfoCommand.java    -- /dinfo shortcut
    DungeonBrowseCommand.java  -- /dbrowse (UI panel)
    DungeonStatusCommand.java  -- /dstatus (UI panel)
    DungeonJoinCommand.java    -- /djoin shortcut
    DungeonLeaveCommand.java   -- /dleave shortcut
    sub/                       -- Admin subcommand handlers
  config/
    DungeonConfig.java         -- JSON template loader
    DungeonTemplate.java       -- Template data model
    DungeonWorldConfig.java    -- World registration + persistence
    SpawnPointConfig.java      -- Spawn point data
  manager/
    DungeonManager.java        -- Core singleton, instance lifecycle
    DungeonInstance.java       -- Active dungeon run state
    RoomState.java             -- Room progression enum
  spawning/
    MobSpawner.java            -- NPC spawning + tracking + cleanup
  portal/
    PortalManager.java         -- Proximity-based portal activation
  systems/
    DungeonTickSystem.java     -- Per-tick processing (3s interval)
  events/
    DungeonEventListener.java  -- Player ready/reconnect handler
    DungeonDeathHandler.java   -- Death handling in dungeons
  components/
    DungeonData.java           -- Persistent player data (ECS)
  ui/
    DungeonListPage.java       -- Clickable dungeon browser
    DungeonStatusPage.java     -- Live dungeon progress panel
    DungeonHud.java            -- Persistent overlay HUD
  util/
    Msg.java                   -- Themed message formatting
```

## Building

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd plugin
./gradlew build
```

Output: `build/libs/DungeonPlugin-0.1.0.jar`

## Installation

1. Copy JAR to: `<world>/mods/com.howlstudio_DungeonPlugin/`
2. Copy sample configs to: `<world>/mods/com.howlstudio_DungeonPlugin/DungeonConfigs/`
3. Enable in world's `config.json`:
   ```json
   {"Mods": {"com.howlstudio:DungeonPlugin": {"Enabled": true}}}
   ```
4. Restart Hytale

## Credits

Built by Misa Kuromi for Howl Studio, 2026.
