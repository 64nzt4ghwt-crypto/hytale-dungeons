# Hytale Dungeon System - Implementation Plan

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     DungeonPlugin                            │
├─────────────────────────────────────────────────────────────┤
│  DungeonManager (singleton)                                  │
│  ├── DungeonRegistry (loaded from JSON)                      │
│  ├── InstanceManager (active instances)                      │
│  ├── CommandHandler (/dungeon commands)                      │
│  └── EventHandler (triggers, deaths, etc.)                   │
├─────────────────────────────────────────────────────────────┤
│  DungeonInstance                                             │
│  ├── World reference                                         │
│  ├── Party tracking                                          │
│  ├── Room states                                             │
│  └── Timer/progress                                          │
├─────────────────────────────────────────────────────────────┤
│  RoomController                                              │
│  ├── State machine (INACTIVE → ACTIVE → CLEARED)            │
│  ├── Mob spawning                                            │
│  ├── Barrier management                                      │
│  └── Clear detection                                         │
└─────────────────────────────────────────────────────────────┘
```

## File Structure

```
dungeons-plugin/
├── src/main/java/com/ourserver/dungeons/
│   ├── DungeonPlugin.java           # Main entry point
│   ├── manager/
│   │   ├── DungeonManager.java      # Central coordinator
│   │   ├── InstanceManager.java     # Instance lifecycle
│   │   └── DungeonRegistry.java     # Definition loading
│   ├── model/
│   │   ├── DungeonDefinition.java   # Dungeon config
│   │   ├── RoomDefinition.java      # Room config
│   │   ├── SpawnPoint.java          # Mob spawn config
│   │   └── LootTable.java           # Rewards config
│   ├── instance/
│   │   ├── DungeonInstance.java     # Active instance
│   │   ├── RoomController.java      # Room state machine
│   │   └── PartyManager.java        # Party tracking
│   ├── command/
│   │   ├── DungeonCommand.java      # Main command
│   │   └── subcommands/
│   │       ├── RegisterCommand.java
│   │       ├── SetPortalCommand.java
│   │       ├── MobAddCommand.java
│   │       └── LootCommand.java
│   ├── event/
│   │   ├── PortalHandler.java       # Portal entry
│   │   ├── RoomTriggerHandler.java  # Room activation
│   │   ├── MobDeathHandler.java     # Clear detection
│   │   └── PlayerLeaveHandler.java  # Cleanup
│   └── util/
│       ├── WorldCopier.java         # Instance world creation
│       └── RegionDetector.java      # Spatial queries
├── src/main/resources/
│   ├── dungeons/                    # Dungeon definitions
│   │   └── example_dungeon.json
│   └── META-INF/services/
│       └── com.hypixel.hytale.plugin.early.ClassTransformer
└── build.gradle
```

## Room State Machine

```
     ┌──────────────────┐
     │     INACTIVE     │
     │  (not yet entered) │
     └────────┬─────────┘
              │ Player enters trigger zone
              ▼
     ┌──────────────────┐
     │     ACTIVE       │
     │  (mobs spawning)  │
     │  Barriers UP      │
     └────────┬─────────┘
              │ All mobs dead
              ▼
     ┌──────────────────┐
     │     CLEARED      │
     │  Barriers DOWN   │
     │  Loot spawned    │
     └──────────────────┘
```

## Key Algorithms

### 1. Instance Creation
```
1. Copy world folder: dungeons/ice_temple → instances/ice_temple_<uuid>
2. Load world: universe.loadWorld("ice_temple_<uuid>")
3. Create DungeonInstance object
4. Link portal to instance
5. Teleport party to spawn point
```

### 2. Room Activation
```
1. PlayerMoveEvent fires
2. Check if player in any room's trigger zone
3. If room INACTIVE:
   a. Set room ACTIVE
   b. Place barrier blocks
   c. Spawn wave 1 mobs
4. Track mobs in room
```

### 3. Clear Detection
```
1. EntityDeathEvent fires
2. If entity was dungeon mob:
   a. Remove from room mob list
   b. If room mob list empty:
      - If more waves: spawn next wave
      - If no waves: mark room CLEARED
3. On CLEARED:
   a. Remove barrier blocks
   b. Spawn loot
   c. Unlock next rooms
```

### 4. Instance Cleanup
```
1. When all players leave OR dungeon complete:
   a. Teleport any remaining players to hub
   b. Save statistics
   c. Unload world
   d. Delete instance folder
```

## Command Examples

```
/dungeon register ice_temple
  → Registers dungeons/ice_temple as available dungeon

/dungeon setportal ice_temple
  → Links block player is looking at as portal

/dungeon mob add ice_temple skeleton player amount 3 wave 1
  → Adds spawn point at player's position

/dungeon loot add entrance_hall diamond_sword 0.1
  → 10% chance for diamond sword in entrance_hall

/dungeon reload
  → Reloads all dungeon definitions

/dungeon forceend
  → Force-ends current dungeon instance
```

## JSON Configuration Examples

### Dungeon Definition
```json
{
  "id": "ice_temple",
  "displayName": "§b❄ Ice Temple",
  "description": "An ancient temple frozen in time",
  "difficulty": 2,
  "recommendedLevel": 15,
  "minPlayers": 1,
  "maxPlayers": 4,
  "timeLimit": 1800,
  "worldTemplate": "dungeons/ice_temple",
  "spawnPoint": {"x": 0, "y": 65, "z": 0},
  "exitPoint": {"x": 0, "y": 65, "z": 100},
  "rooms": [
    {
      "id": "entrance",
      "type": "combat",
      ...
    }
  ]
}
```

### Spawn Point Definition
```json
{
  "entity": "ice_skeleton",
  "position": {"x": 5, "y": 65, "z": 10},
  "wave": 1,
  "amount": 3,
  "radius": 2,
  "chance": 1.0,
  "facing": 180.0
}
```

## HUD Design (conceptual)

```
┌─────────────────────────────┐
│ ❄ Ice Temple      15:23    │
│ Room: Entrance Hall        │
│ Wave: 2/3                  │
│ Mobs: 5 remaining          │
│ ████████░░ 80%             │
└─────────────────────────────┘
```

## Development Phases

### Week 1: Foundation
- [ ] Project setup with Gradle
- [ ] DungeonManager skeleton
- [ ] JSON definition loading
- [ ] Basic /dungeon command

### Week 2: Instances
- [ ] World copying mechanism
- [ ] Instance creation/deletion
- [ ] Portal teleportation
- [ ] Party tracking

### Week 3: Rooms
- [ ] Room state machine
- [ ] Trigger zone detection
- [ ] Mob spawning
- [ ] Clear detection

### Week 4: Polish
- [ ] Barrier blocks
- [ ] Loot system
- [ ] HUD (if API allows)
- [ ] Testing & debugging

## Open Questions

1. **World copying**: Does Hytale have a native world copy API, or do we need to copy files?
2. **Custom entities**: Can we spawn custom mobs or only vanilla?
3. **HUD API**: Is there a mod-accessible HUD system?
4. **Particle effects**: How do we add visual feedback?

## Testing Checklist

- [ ] Create dungeon → success
- [ ] Enter portal → teleports to instance
- [ ] Trigger room → mobs spawn, barriers appear
- [ ] Kill mobs → room clears, barriers disappear
- [ ] Complete dungeon → exit, instance cleanup
- [ ] Multiple parties → separate instances
- [ ] Player disconnect → handled gracefully
- [ ] /dungeon reload → doesn't break active instances
