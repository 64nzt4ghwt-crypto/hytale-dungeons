# Hytale Server API Deep Dive
## Compiled from HytaleServer.jar analysis (16,268 classes)

### Entity Hierarchy
```
Entity (abstract) - base class, has TransformComponent, World reference
  └── LivingEntity (abstract) - has Inventory, health
       ├── Player - command sender, permissions, HUD
       └── NPCEntity - AI-driven entities (mobs, animals, NPCs)
            - has Role (behavior/AI definition)
            - has ModelAsset (appearance)
            - has PathManager (navigation)
            - has AlarmStore, DamageData
            - setAppearance(ref, ModelAsset, accessor) 
            - setRole(Role) / setRoleName(String) / setRoleIndex(int)
```

### How to Spawn an NPC (The Pattern)
```java
// 1. Create NPCEntity
NPCEntity npc = new NPCEntity(world);

// 2. Set its role (defines AI, health, combat behavior)
npc.setRoleName("skeleton_warrior");  // or by index
// Roles are loaded from JSON asset files by the NPC system

// 3. Set appearance (model + texture)
// Static: NPCEntity.setAppearance(ref, modelAssetKey, accessor)
// Instance: npc.setAppearance(ref, modelAsset, accessor)
// ModelAsset looked up via: ModelAsset.getAssetMap().getAsset(key)

// 4. Spawn into world
world.spawnEntity(npc, new Vector3d(x, y, z), new Vector3f(0, yaw, 0));
// Returns the entity back (same reference)
```

### Key Classes for Mob Spawning

#### World (com.hypixel.hytale.server.core.universe.world.World)
```java
<T extends Entity> T spawnEntity(T entity, Vector3d pos, Vector3f rot);
<T extends Entity> T addEntity(T entity, Vector3d pos, Vector3f rot, AddReason reason);
Entity getEntity(UUID id);
Ref<EntityStore> getEntityRef(UUID id);
List<Player> getPlayers();
Store<EntityStore> getEntityStore();
CompletableFuture<World> loadWorld(String name);  // on Universe
World getWorld(String name);  // on Universe
```

#### NPCEntity (com.hypixel.hytale.server.npc.entities.NPCEntity)
```java
// Construction
new NPCEntity()
new NPCEntity(World world)

// Role (AI behavior)
void setRole(Role role)
void setRoleName(String name)
void setRoleIndex(int index)
Role getRole()
String getRoleName()

// Appearance
static boolean setAppearance(Ref<EntityStore> ref, String modelKey, ComponentAccessor<EntityStore> accessor)
void setAppearance(Ref<EntityStore> ref, ModelAsset model, ComponentAccessor<EntityStore> accessor)

// Equipment
// via RoleUtils:
RoleUtils.setHotbarItems(npc, String[] itemIds)
RoleUtils.setOffHandItems(npc, String[] itemIds)
RoleUtils.setItemInHand(npc, String itemId)
RoleUtils.setArmor(npc, String armorId)

// Lifecycle
void setToDespawn()
void setDespawnTime(float seconds)
boolean isDespawning()

// Combat
DamageData getDamageData()
boolean getCanCauseDamage(ref, accessor)
void clearDamageData()

// Inventory
void setInventorySize(int rows, int cols, int stacks)
```

#### ModelAsset (com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset)
```java
static AssetStore<String, ModelAsset, ...> getAssetStore()
static DefaultAssetMap<String, ModelAsset> getAssetMap()
String getId()
String getModel()
String getTexture()
float getEyeHeight()
Box getBoundingBox()
PhysicsValues getPhysicsValues()
```

#### DefaultAssetMap<K, T>
```java
T getAsset(K key)                // Get by key (e.g. "skeleton")
T getAsset(String pack, K key)   // Get by pack + key
Set<K> getKeys(Path path)        // All keys
int getAssetCount()              // Total count
Map<K, T> getAssetMap()          // Full map
```

### Entity Component System (ECS)
```java
// Stores
Store<EntityStore> store = world.getEntityStore();

// Components
TransformComponent tc = entity.getTransformComponent();
Vector3d pos = tc.getPosition();
tc.teleportPosition(new Vector3d(x, y, z));
tc.setRotation(new Vector3f(pitch, yaw, roll));

// Entity operations
entity.loadIntoWorld(targetWorld);
entity.getWorld();
entity.getUuid();  // deprecated but works
entity.getReference();

// Holders (for creating entities via ECS)
Holder<EntityStore> holder = new Holder<>()
    .with(componentType, component);
Ref<EntityStore> ref = store.addEntity(holder);
```

### Vector Math (com.hypixel.hytale.math.vector)
```java
// NOT org.joml! Hytale has its own
Vector3d(double x, double y, double z)  // position
Vector3f(float x, float y, float z)     // rotation
Vector3i(int x, int y, int z)           // block pos

// Vector3d has public fields: x, y, z
// Also getters: getX(), getY(), getZ()
```

### NPC AI System
```
BuilderRole - defines NPC behavior from JSON assets
  ├── appearance (ModelAsset reference)
  ├── maxHealth
  ├── startState / defaultSubState (state machine)
  ├── collision parameters
  ├── combat support
  ├── movement controllers
  └── components (sensor, entity filter, timer, etc.)

Role - runtime NPC behavior
  ├── CombatSupport
  ├── StateSupport (state machine)
  ├── MarkedEntitySupport
  ├── WorldSupport
  ├── EntitySupport
  └── PositionCache
```

### Spawning System
```java
// Built-in spawn command: /npc spawn <role>
// NPCSpawnCommand extends AbstractPlayerCommand

// Spawn managers
SpawnManager (abstract) - tracks spawn wrappers
BeaconSpawnManager - beacon-based spawning
SpawnController - controls spawn logic
SpawnJobSystem - processes spawn jobs

// Spawn configs (JSON)
NPCSpawn - base spawn config
  ├── RoleSpawnParameters[] npcs  // what to spawn
  ├── environments               // where valid
  ├── dayTimeRange              // when valid
  └── despawnParameters         // when to remove

RoleSpawnParameters
  ├── id (role identifier)
  ├── weight (spawn probability)
  ├── spawnBlockSet
  └── flockDefinitionId
```

### Command System
```java
AbstractPlayerCommand - requires player context
  execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world)

AbstractWorldCommand - just needs world
  execute(CommandContext ctx, World world, Store<EntityStore> store)

ArgTypes:
  STRING, INTEGER, FLOAT, DOUBLE, BOOLEAN, UUID
  PLAYER_UUID, PLAYER_REF, WORLD
  MODEL_ASSET, ITEM_ASSET, BLOCK_TYPE_ASSET
  RELATIVE_POSITION, RELATIVE_BLOCK_POSITION
  ENTITY_ID
  // NO GREEDY_STRING - use STRING
```

### Message API
```java
Message.raw(String text)          // create message
  .color(Color c)                // java.awt.Color
  .bold(boolean)
  .italic(boolean)
  .insert(Message other)          // append another message

player.sendMessage(Message msg)
ctx.sendMessage(Message msg)      // command context

// Hytale font: ASCII only! No Unicode symbols.
```

### Event System
```java
// Player events
PlayerReadyEvent - player finished loading
  getPlayer() -> Player

// Entity events  
LivingEntityInventoryChangeEvent

// Block events
// via NPC blackboard views

// Plugin registration
// Events registered via InstancesPlugin pattern
```

### Instances Plugin (builtin/instances)
```
InstancesPlugin - manages game instances (minigames, dungeons)
  ├── config/ - instance configuration
  ├── command/ - instance commands
  ├── event/ - instance events
  ├── interactions/ - instance interactions
  ├── page/ - instance UI pages
  └── removal/ - cleanup
```

### Key Patterns

**Spawn an NPC at position:**
```java
NPCEntity npc = new NPCEntity(world);
npc.setRoleName("trork_warrior");  // role from game data
world.spawnEntity(npc, new Vector3d(x, y, z), new Vector3f(0, 0, 0));
```

**Teleport player between worlds:**
```java
Universe universe = Universe.get();
World targetWorld = universe.getWorld(worldName);
if (targetWorld == null) {
    universe.loadWorld(worldName).thenAccept(loaded -> {
        player.loadIntoWorld(loaded);
        TransformComponent tc = player.getTransformComponent();
        tc.teleportPosition(new Vector3d(x, y, z));
    });
} else {
    player.loadIntoWorld(targetWorld);
    player.getTransformComponent().teleportPosition(new Vector3d(x, y, z));
}
```

**Get entities near position:**
```java
// Via Store queries and spatial lookups
// World likely has spatial query methods (check decompiled)
```

### File Locations
- Game install: `~/Library/Application Support/Hytale/install/release/package/game/latest/`
- World saves: `~/Library/Application Support/Hytale/UserData/Saves/`
- Mods per world: `<save>/mods/<package_modname>/`
- Mod config: `<save>/config.json` → `{"Mods": {"com.howlstudio:DungeonPlugin": {"Enabled": true}}}`
- Plugin JAR: goes in mod folder (exact mechanism TBD - needs testing)

### What We Need for Dungeon Mob Spawning
1. Create `NPCEntity` instances with appropriate roles
2. Spawn them into the dungeon world at configured positions
3. Track spawned entities for room clear detection (check if dead)
4. Despawn remaining mobs when dungeon ends
5. Support waves (spawn more mobs after wave 1 cleared)

### Open Questions
- [ ] What role names are available in the base game? (trork, skeleton, etc.)
- [ ] How to detect when an NPCEntity dies? (damage system events)
- [ ] How to query entities in an area? (for room clear detection)
- [ ] Does `world.spawnEntity()` handle all ECS registration automatically?
- [ ] Can we create custom roles, or only use existing ones?

---
*Generated 2026-02-07 by Misa from HytaleServer.jar (build-7)*
