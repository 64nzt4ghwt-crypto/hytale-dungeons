# Dungeon Plugin Build Spec

## Project: Instanced Dungeons Plugin for Hytale

Build a server plugin for Hytale that creates instanced dungeon experiences. The plugin should feel hand-crafted, not AI-generated. Write code like an experienced Java developer who's made Minecraft plugins before and is now building for Hytale.

## Project Setup

Create a standard Gradle project at `/Users/misa/.openclaw/workspace/projects/hytale-dungeons/plugin/`

### gradle.properties
```properties
maven_group=com.howlstudio.dungeons
java_version=25
patchline=release
```

### build.gradle.kts
```kotlin
plugins {
    id("java")
}

group = "com.howlstudio.dungeons"
version = "0.1.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
```

### manifest.json (src/main/resources/)
```json
{
    "Group": "com.howlstudio",
    "Name": "DungeonPlugin",
    "Version": "0.1.0",
    "Description": "Instanced dungeon system with room progression, mob spawning, loot, and party support.",
    "Authors": [{"Name": "Howl"}],
    "ServerVersion": "*",
    "Dependencies": {
        "Hytale:EntityModule": "*"
    },
    "Main": "com.howlstudio.dungeons.DungeonPlugin"
}
```

### libs/
Symlink HytaleServer.jar:
```bash
mkdir -p libs
ln -s "$HOME/Library/Application Support/Hytale/install/release/package/game/latest/Server/HytaleServer.jar" libs/HytaleServer.jar
```

## Architecture

Package: `com.howlstudio.dungeons`

### Core Classes

**DungeonPlugin.java** - Main plugin entry. Extends `JavaPlugin`. Registers commands, events, systems, and the DungeonData component type in `start()`. Keep it clean - just wiring, no logic.

**DungeonManager.java** - Singleton managing active dungeon instances. Tracks which players are in which dungeons. Handles instance lifecycle (create, populate, cleanup). Uses ConcurrentHashMap for thread safety.

**DungeonInstance.java** - Represents one active dungeon run. Holds state: current room index, spawned mob refs, loot generated, party members, start time, difficulty tier.

**DungeonConfig.java** - Loaded from JSON config files via Hytale's Config/Codec system. Defines dungeon templates: room sequence, mob types per room, loot tables, time limits, difficulty scaling.

### Components (ECS)

**DungeonData.java** - Persistent player component (like Orbis's RaceData). Tracks: dungeons completed, current dungeon ID, total clears, best clear time. Uses BuilderCodec for serialization. Implements Component<EntityStore> with clone().

### Commands

**DungeonCommand.java** - `/dungeon create <template>` - Creates a new dungeon instance
**DungeonJoinCommand.java** - `/dungeon join <player>` - Join another player's dungeon  
**DungeonLeaveCommand.java** - `/dungeon leave` - Leave current dungeon
**DungeonListCommand.java** - `/dungeon list` - Show available dungeon templates
**DungeonInfoCommand.java** - `/dungeon info` - Show current dungeon status (room, mobs remaining, time)

All commands extend `AbstractPlayerCommand`. Follow the pattern from the example plugins.

### Events

**DungeonEventListener.java** - Listens for `PlayerReadyEvent` to restore dungeon state if player reconnects mid-dungeon. Also handles player disconnect cleanup.

### Systems

**DungeonTickSystem.java** - ECS system that ticks active dungeons. Checks: are all mobs in current room dead? If yes, advance to next room. Is time limit exceeded? If yes, fail the dungeon. Runs in the entity tick group.

### Config (JSON)

Create sample dungeon configs at `src/main/resources/Server/DungeonConfigs/`:

**skeleton_crypt.json**
```json
{
    "id": "skeleton_crypt",
    "name": "Skeleton Crypt",
    "description": "A dark crypt crawling with undead",
    "minPlayers": 1,
    "maxPlayers": 4,
    "timeLimitSeconds": 600,
    "rooms": [
        {
            "name": "Entrance Hall",
            "mobSpawns": [
                {"type": "hytale:skeleton", "count": 3}
            ],
            "loot": []
        },
        {
            "name": "Bone Gallery",
            "mobSpawns": [
                {"type": "hytale:skeleton", "count": 5},
                {"type": "hytale:skeleton_archer", "count": 2}
            ],
            "loot": [
                {"item": "hytale:gold_coin", "count": [5, 15], "chance": 0.8}
            ]
        },
        {
            "name": "Crypt Lord's Chamber",
            "isBoss": true,
            "mobSpawns": [
                {"type": "hytale:skeleton_king", "count": 1},
                {"type": "hytale:skeleton", "count": 4}
            ],
            "loot": [
                {"item": "hytale:enchanted_blade", "count": [1, 1], "chance": 0.3},
                {"item": "hytale:gold_coin", "count": [20, 50], "chance": 1.0}
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

**goblin_den.json** - Similar structure, 4 rooms, goblin themed.

## Code Style Rules

1. NO generic AI comments like "// This method does X". Comments should explain WHY, not WHAT.
2. Variable names should be specific: `activeInstances` not `map`, `ticksSinceLastSpawn` not `counter`
3. Use early returns to reduce nesting
4. Null checks without being paranoid - trust the API where reasonable
5. Portuguese/Spanish comments are fine for personality (the reference mods mix languages)
6. Keep methods under 40 lines. Extract helpers.
7. Use `System.out.println` for logging (Hytale doesn't have a logging framework exposed)
8. Handle edge cases: player disconnects mid-dungeon, server restart during dungeon, empty party

## Reference Code

Study these files for API patterns (they're in the same repo):
- `reference-plugin/` - Basic command + event registration
- `plugin-examples/` - Camera control, chat formatting, config, events
- `orbis-dungeons/` - Full RPG system: ECS components, damage systems, stat modifiers, UI pages, persistent data

Key API imports you'll need:
```java
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.codec.*;
import com.hypixel.hytale.event.EventRegistry;
```

## What NOT to do

- Don't add unnecessary abstractions or interfaces "for extensibility"
- Don't create a util class with one method
- Don't write javadoc on every getter/setter
- Don't use Optional everywhere - nullable is fine
- Don't create builder patterns for simple objects
- Don't add TODO comments about "future improvements"

## Output

Create the full project structure with all Java files, build files, configs, and a README.md. Make it compile-ready (assuming HytaleServer.jar is in libs/).
