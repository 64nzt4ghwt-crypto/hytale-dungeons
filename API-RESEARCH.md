# Hytale Dungeon System - API Research

*Compiled from hytaledocs.dev - 2026-02-05*

## Key APIs We Need

### 1. World/Instance Management

**Universe class** (`com.hypixel.hytale.server.core.universe`)
```java
// Load a dungeon world
CompletableFuture<World> loadWorld(String name);

// Unload when party leaves
CompletableFuture<Void> unloadWorld(World world);

// Get all loaded worlds
Collection<World> getWorlds();

// Get specific world
World getWorld(String name);
```

**Pattern for Instances:**
- Copy dungeon template world to new name (e.g., `ice_temple_12345`)
- Load the copy as a separate World
- Unload + delete when party finishes

---

### 2. Player Teleportation

**Player class** (`com.hypixel.hytale.server.core.player`)
```java
// Teleport within same world
void teleport(Vector3d position);

// Teleport to different world (async!)
CompletableFuture<Void> teleport(World world, Vector3d position);

// Get current position/world
Vector3d getPosition();
World getWorld();
```

**Portal Implementation:**
1. Player enters portal trigger zone
2. Get/create dungeon instance
3. `player.teleport(dungeonWorld, spawnPoint)`
4. Track player in dungeon party

---

### 3. Entity Spawning (ECS)

**Store class** - Entity Component System
```java
// Create entity with components
Holder<ECS_TYPE> holder = new Holder<>()
    .with(POSITION_TYPE, new Position(x, y, z))
    .with(HEALTH_TYPE, new Health(100))
    .with(AI_TYPE, new SkeletonAI());

Ref<ECS_TYPE> entityRef = store.addEntity(holder);

// Remove entity
store.removeEntity(entityRef, RemoveReason.LOGIC);
```

**World Entity Access:**
```java
// Get entity store for spawning
Store<?> store = world.getEntityStore();

// Find entities in area (for room clear detection!)
Collection<?> getEntitiesInRadius(Vector3d center, double radius);
Collection<?> getEntitiesInBox(Vector3d min, Vector3d max);
```

---

### 4. Command Registration

**CommandManager class** (`com.hypixel.hytale.server.core.command`)
```java
// Register command
void registerCommand(ICommand command);

// Unregister  
boolean unregisterCommand(String name);

// Execute
CompletableFuture<?> execute(ICommandSender sender, String commandLine);
```

**ICommand Interface:**
```java
public interface ICommand {
    String getName();
    String[] getAliases();
    String getDescription();
    String getUsage();
    boolean hasPermission(ICommandSender sender);
    void execute(ICommandSender sender, String[] args);
    List<String> tabComplete(ICommandSender sender, String[] args);
}
```

---

### 5. Event System (for triggers)

**Event Registration:**
```java
IEventBus eventBus = server.getEventBus();

// Player enters area
eventBus.register(PlayerMoveEvent.class, event -> {
    if (isInRoomTrigger(event.getPlayer().getPosition())) {
        activateRoom(event.getPlayer());
    }
});

// Mob dies (for clear detection)
eventBus.register(EntityDeathEvent.class, event -> {
    checkRoomCleared(event.getEntity());
});

// Block interaction (for puzzles)
eventBus.register(BlockInteractEvent.class, event -> {
    handlePuzzleBlock(event);
});
```

**Cancellable Events:**
```java
// Prevent player from leaving room
eventBus.register(PlayerMoveEvent.class, event -> {
    if (isRoomLocked(event.getPlayer()) && 
        isLeavingRoom(event.getFrom(), event.getTo())) {
        event.setCancelled(true);
    }
});
```

---

### 6. Block Manipulation (barriers)

**World Block API:**
```java
// Get block state
BlockState getBlock(Vector3i position);

// Set block (for barriers appearing/disappearing)
void setBlock(Vector3i position, BlockState state);
void setBlock(Vector3i position, BlockState state, int flags);
```

**Barrier Pattern:**
```java
// Room locked - place barrier blocks
for (Vector3i pos : barrierPositions) {
    world.setBlock(pos, BARRIER_BLOCK);
}

// Room cleared - remove barriers
for (Vector3i pos : barrierPositions) {
    world.setBlock(pos, AIR_BLOCK);
}
```

---

### 7. Packets (for HUD)

**Player Packet API:**
```java
// Send custom packet
player.sendPacket(IPacket packet);
```

**Relevant Packets:**
- Custom HUD data would need a mod-defined packet
- Or use existing UI packets if available

**HUD Strategy:**
- Create custom packet type for dungeon HUD
- Register on client mod side
- Send updates when dungeon state changes

---

## Data Structures Needed

### DungeonDefinition (JSON)
```json
{
    "id": "ice_temple",
    "displayName": "Ice Temple",
    "worldFile": "dungeons/ice_temple",
    "difficulty": 2,
    "minPlayers": 1,
    "maxPlayers": 4,
    "spawnPoint": [0, 64, 0],
    "rooms": [...],
    "lootTables": {...}
}
```

### RoomDefinition (JSON)
```json
{
    "id": "entrance_hall",
    "type": "combat",
    "bounds": {
        "min": [-10, 60, -10],
        "max": [10, 75, 10]
    },
    "triggerZone": {...},
    "barrierPositions": [...],
    "spawnPoints": [
        {
            "entity": "skeleton",
            "position": [0, 64, 5],
            "wave": 1,
            "amount": 3,
            "radius": 2
        }
    ],
    "nextRooms": ["corridor_1"]
}
```

### DungeonInstance (Runtime)
```java
class DungeonInstance {
    UUID instanceId;
    String dungeonId;
    World world;
    Set<Player> party;
    Map<String, RoomState> roomStates;
    long startTime;
    int currentWave;
}
```

---

## Implementation Priority

### Phase 1: Core Framework
1. DungeonManager singleton
2. World loading/unloading
3. Basic teleportation
4. Instance tracking

### Phase 2: Room System  
1. Room state machine
2. Trigger zone detection
3. Mob spawning
4. Clear detection

### Phase 3: Progression
1. Barrier placement/removal
2. Room unlocking
3. Wave progression

### Phase 4: Polish
1. HUD packets
2. Loot system
3. Admin commands
4. Debug visualization

---

## Notes

- Hytale uses **ECS** (Entity Component System) - different from Minecraft's OOP entities
- World loading is **async** - need CompletableFuture handling
- Events are **priority-based** - can control execution order
- **KD-Tree** spatial system enables efficient proximity queries
- Plugin system uses **bytecode transformation** - powerful but complex

## Questions to Answer When Playing

1. How are custom entities defined? (need to spawn custom dungeon mobs)
2. What UI/HUD APIs exist? (for dungeon status display)
3. How does world copying work? (for instance creation)
4. Are there built-in particle/effect systems? (for visual feedback)
