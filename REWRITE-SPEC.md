# Dungeon Plugin Rewrite - Using Real Hytale APIs

## Why Rewrite?
Kimi's original scaffold (2,654 lines) was written BEFORE we had real API docs. 
Now we have actual decompiled source + community documentation confirming the real APIs.

## Key Discovery: InstancesPlugin EXISTS
Hytale has a NATIVE instance system. We don't need to build our own.

## Real APIs to Use

### Instance Management
```java
// Spawn instance from template
World instanceWorld = InstancesPlugin.get()
    .spawnInstance("dungeon_template_name", currentWorld, returnPoint).join();

// Teleport player to loading instance
CompletableFuture<World> future = InstancesPlugin.get()
    .spawnInstance(templateName, world, returnPoint);
InstancesPlugin.teleportPlayerToLoadingInstance(playerRef, store, future, null);

// Exit instance (returns to previous world automatically)
InstancesPlugin.exitInstance(playerRef, store);

// Cleanup
InstancesPlugin.safeRemoveInstance(instanceWorld);
```

### Entity Spawning (for mobs)
```java
Store<EntityStore> store = world.getEntityStore().getStore();
Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

// Load mob model
ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset("Skeleton_Knight");
Model model = Model.createScaledModel(modelAsset, 1.0f);

// Required components
holder.addComponent(TransformComponent.getComponentType(), 
    new TransformComponent(position, rotation));
holder.addComponent(PersistentModel.getComponentType(), 
    new PersistentModel(model.toReference()));
holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
holder.addComponent(BoundingBox.getComponentType(), 
    new BoundingBox(model.getBoundingBox()));
holder.addComponent(NetworkId.getComponentType(), 
    new NetworkId(store.getExternalData().takeNextNetworkId()));

holder.ensureComponent(UUIDComponent.getComponentType());

// MUST run in world thread
world.execute(() -> store.addEntity(holder, AddReason.SPAWN));
```

### ECS Custom Components
```java
// Register in plugin setup()
ComponentType<EntityStore, DungeonStateComponent> type = 
    this.getEntityStoreRegistry()
        .registerComponent(DungeonStateComponent.class, DungeonStateComponent::new);

// Register systems
this.getEntityStoreRegistry().registerSystem(new DungeonTickSystem(type));
```

### Events to Listen For
- PlayerReadyEvent - player loaded
- PlayerDisconnectEvent - handle disconnect during dungeon
- BreakBlockEvent / PlaceBlockEvent - prevent griefing in dungeons
- Damage (CancellableEcsEvent) - custom damage modifiers
- DiscoverInstanceEvent - discovery notifications
- TreasureChestOpeningEvent - loot events
- PlayerMouseButtonEvent - interaction

### Death Handling
```java
public class DungeonDeathSystem extends DeathSystems.OnDeathSystem {
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
    
    @Override
    public void onComponentAdded(Ref ref, DeathComponent component, 
            Store store, CommandBuffer commandBuffer) {
        // Check if player is in a dungeon instance
        // Handle respawn/wipe logic
    }
}
```

### Available Mob Types for Dungeons
- Skeleton (many variants: Archer, Knight, Mage, Frost, Sand, Pirate, etc.)
- Zombie (Aberrant, Burnt, Frost, Sand variants)
- Goblin (Boss, Duke, Lobber, Miner, Ogre, Scrapper, Thief)
- Spider, Spider_Cave
- Dragon_Fire, Dragon_Frost, Dragon_Void (BOSSES!)
- Scarak (Broodmother, Defender, Fighter, Seeker)
- Golem variants (Crystal Earth/Flame/Frost/Sand/Thunder, Firesteel)
- Shadow_Knight, Necromancer_Void, Wraith
- Eye_Void (ranged)

## Architecture

### Plugin Class: DungeonPlugin extends JavaPlugin
```
setup():
  - Register DungeonStateComponent
  - Register DungeonTickSystem  
  - Register DungeonDeathSystem
  - Register commands: /dungeon enter, /dungeon leave, /dungeon list
  - Register events: PlayerDisconnect, BreakBlock, PlaceBlock
  
start():
  - Load dungeon configs from JSON
  - Initialize dungeon registry
```

### DungeonStateComponent (on Player entities)
```java
- currentDungeonId: String
- currentRoom: int
- mobsRemaining: int
- startTime: long
- deathCount: int
```

### DungeonTickSystem (EntityTickingSystem)
- Queries players with DungeonStateComponent
- Checks room clear conditions
- Triggers room transitions
- Spawns next wave of mobs

### DungeonConfig (JSON-based)
```json
{
  "id": "forest_crypt",
  "displayName": "The Forest Crypt",
  "instanceTemplate": "Dungeon_Forest_Crypt",
  "rooms": [
    {
      "roomIndex": 0,
      "mobs": [
        {"type": "Skeleton", "count": 3, "spawnDelay": 0},
        {"type": "Skeleton_Archer", "count": 2, "spawnDelay": 5.0}
      ],
      "lootTable": "forest_crypt_common"
    },
    {
      "roomIndex": 1, 
      "mobs": [
        {"type": "Skeleton_Knight", "count": 2},
        {"type": "Skeleton_Mage", "count": 1}
      ],
      "boss": false
    },
    {
      "roomIndex": 2,
      "mobs": [
        {"type": "Shadow_Knight", "count": 1}
      ],
      "boss": true,
      "lootTable": "forest_crypt_boss"
    }
  ],
  "maxPlayers": 4,
  "timeLimit": 1800,
  "difficulty": "normal"
}
```

### Commands
- `/dungeon enter <id>` - Creates instance, teleports party
- `/dungeon leave` - Exits instance, returns to hub
- `/dungeon list` - Shows available dungeons
- `/dungeon info` - Shows current dungeon status

### manifest.json
```json
{
  "Name": "DungeonPlugin",
  "Version": "1.0.0",
  "Main": "com.misa.dungeons.DungeonPlugin",
  "Dependencies": {
    "Hytale:EntityModule": "*",
    "Hytale:BlockModule": "*"
  }
}
```

## File Structure
```
src/main/java/com/misa/dungeons/
├── DungeonPlugin.java          (main plugin, registration)
├── components/
│   └── DungeonStateComponent.java (player dungeon state)
├── systems/
│   ├── DungeonTickSystem.java  (room progression logic)
│   └── DungeonDeathSystem.java (death/wipe handling)
├── commands/
│   ├── DungeonEnterCommand.java
│   ├── DungeonLeaveCommand.java
│   └── DungeonListCommand.java
├── config/
│   ├── DungeonConfig.java      (JSON deserialization)
│   └── RoomConfig.java
├── manager/
│   ├── DungeonRegistry.java    (all dungeon configs)
│   ├── DungeonSession.java     (active instance tracking)
│   └── MobSpawner.java         (entity spawning helper)
└── events/
    └── DungeonEventHandler.java (grief prevention, etc.)
```

## IMPORTANT NOTES
- All world operations MUST be inside `world.execute(() -> {})`
- Instance templates go in `Server/Instances/[Name]/` with `instance.bson`
- Prefabs can define room layouts (built in-game with /editprefab)
- ECS systems need proper group ordering (damage pipeline)
- manifest.json needs EntityModule + BlockModule dependencies
