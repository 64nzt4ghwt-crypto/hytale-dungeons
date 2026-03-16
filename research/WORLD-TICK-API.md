# Hytale World System, Teleportation & Scheduling API Research

> Decompiled from `HytaleServer.jar` using `javap -p` on 2026-02-07.
> All method signatures are **exact** from bytecode.

---

## Table of Contents

1. [Universe - World Management](#1-universe---world-management)
2. [World - The Ticking Thread](#2-world---the-ticking-thread)
3. [TransformComponent - Full Teleportation API](#3-transformcomponent---full-teleportation-api)
4. [Entity.loadIntoWorld() - Cross-World Movement](#4-entityloadintoworld---cross-world-movement)
5. [TickingThread - The Tick Loop](#5-tickingthread---the-tick-loop)
6. [Scheduling & Task Execution](#6-scheduling--task-execution)
7. [World Creation & Instancing](#7-world-creation--instancing)
8. [InstancesPlugin - Built-in Instance System](#8-instancesplugin---built-in-instance-system)
9. [World Deletion & Unloading](#9-world-deletion--unloading)
10. [Chunk Loading & Spawn Points](#10-chunk-loading--spawn-points)
11. [Event System](#11-event-system)
12. [Practical Patterns for Dungeon Plugin](#12-practical-patterns-for-dungeon-plugin)

---

## 1. Universe - World Management

**Package:** `com.hypixel.hytale.server.core.universe`

Universe is a singleton that manages ALL worlds. It extends JavaPlugin (it's a built-in plugin itself).

```java
public class Universe extends JavaPlugin implements IMessageReceiver, MetricProvider {

    // === SINGLETON ===
    public static Universe get();

    // === WORLD RETRIEVAL ===
    public World getWorld(String name);                // Get loaded world by name (null if not loaded)
    public World getWorld(UUID uuid);                  // Get loaded world by UUID
    public World getDefaultWorld();                    // Get the default/main world
    public Map<String, World> getWorlds();             // All loaded worlds (unmodifiable)

    // === WORLD LOADING (from disk) ===
    public boolean isWorldLoadable(String name);       // Check if world exists on disk
    public CompletableFuture<World> loadWorld(String name);  // Load existing world from disk

    // === WORLD CREATION ===
    // Add a world by name (loads or creates)
    public CompletableFuture<World> addWorld(String name);
    // Add world with worldgen type and environment
    public CompletableFuture<World> addWorld(String name, String worldGenType, String environment);
    // Create world with full config control
    public CompletableFuture<World> makeWorld(String name, Path savePath, WorldConfig config);
    public CompletableFuture<World> makeWorld(String name, Path savePath, WorldConfig config, boolean startTicking);

    // === WORLD REMOVAL ===
    public boolean removeWorld(String name);           // Remove world (returns success)
    public void removeWorldExceptionally(String name); // Remove with error handling

    // === PLAYERS (global) ===
    public List<PlayerRef> getPlayers();
    public PlayerRef getPlayer(UUID uuid);
    public PlayerRef getPlayer(String name, NameMatching matching);
    public int getPlayerCount();

    // === UTILITIES ===
    public Path getPath();                             // Universe save path
    public static Path getWorldGenPath();              // Path to worldgen configs
    public void shutdownAllWorlds();
    public void disconnectAllPLayers();                // [sic] typo in actual API
    public CompletableFuture<Void> runBackup();

    // === PLAYER LIFECYCLE ===
    public CompletableFuture<PlayerRef> addPlayer(Channel, String, ProtocolVersion, UUID, String, PlayerAuthentication, int, PlayerSkin);
    public void removePlayer(PlayerRef);
    public CompletableFuture<PlayerRef> resetPlayer(PlayerRef);
    public CompletableFuture<PlayerRef> resetPlayer(PlayerRef, Holder<EntityStore>);
    public CompletableFuture<PlayerRef> resetPlayer(PlayerRef, Holder<EntityStore>, World, Transform);

    // === CONFIG ===
    public WorldConfigProvider getWorldConfigProvider();
}
```

### Key Fields
```java
private final Map<String, World> worlds;               // name → World
private final Map<UUID, World> worldsByUuid;            // UUID → World
private final Map<UUID, PlayerRef> players;             // UUID → PlayerRef
private static Universe instance;                       // singleton
```

### Important Notes:
- `getWorld(name)` returns `null` if not loaded — always null-check!
- `loadWorld()` returns `CompletableFuture<World>` — world loading is **async**
- `makeWorld()` gives full control over WorldConfig (worldgen, spawn, ticking, etc.)
- `addWorld()` is simpler — loads existing or creates with defaults
- Universe itself is a JavaPlugin registered via `MANIFEST`

---

## 2. World - The Ticking Thread

**Package:** `com.hypixel.hytale.server.core.universe.world`

**Critical insight:** World extends TickingThread AND implements Executor. Each world runs on its **own thread** with its own tick loop.

```java
public class World extends TickingThread implements Executor, ... {

    // === CONSTRUCTOR ===
    public World(String name, Path savePath, WorldConfig worldConfig) throws IOException;
    public CompletableFuture<World> init();

    // === TICK STATE ===
    public long getTick();                             // Current tick number
    public boolean isTicking();
    public void setTicking(boolean ticking);
    public boolean isPaused();
    public void setPaused(boolean paused);

    // From TickingThread:
    public void setTps(int tps);                       // Change TPS (default 20)
    public int getTps();

    // === TASK EXECUTION (world thread) ===
    // ⚠️ THIS IS THE KEY METHOD FOR SCHEDULING!
    public void execute(Runnable task);                // Post task to world thread queue
    public void consumeTaskQueue();                    // Internal: processes queued tasks

    // === ENTITY METHODS ===
    public <T extends Entity> T spawnEntity(T entity, Vector3d position, Vector3f rotation);
    public <T extends Entity> T addEntity(T entity, Vector3d position, Vector3f rotation, AddReason reason);
    public Entity getEntity(UUID uuid);
    public Ref<EntityStore> getEntityRef(UUID uuid);

    // === PLAYER METHODS ===
    public List<Player> getPlayers();
    public int getPlayerCount();
    public Collection<PlayerRef> getPlayerRefs();
    public CompletableFuture<PlayerRef> addPlayer(PlayerRef ref);
    public CompletableFuture<PlayerRef> addPlayer(PlayerRef ref, Transform spawn);
    public CompletableFuture<PlayerRef> addPlayer(PlayerRef ref, Transform spawn, Boolean flag1, Boolean flag2);
    public CompletableFuture<Void> drainPlayersTo(World targetWorld);  // Move ALL players to another world

    // === WORLD INFO ===
    public String getName();
    public boolean isAlive();
    public WorldConfig getWorldConfig();
    public Path getSavePath();
    public EventRegistry getEventRegistry();           // Per-world event registry!

    // === STORES ===
    public ChunkStore getChunkStore();
    public EntityStore getEntityStore();

    // === CHUNK ACCESS ===
    public WorldChunk loadChunkIfInMemory(long key);
    public WorldChunk getChunkIfInMemory(long key);
    public WorldChunk getChunkIfLoaded(long key);
    public WorldChunk getChunkIfNonTicking(long key);
    public CompletableFuture<WorldChunk> getChunkAsync(long key);
    public CompletableFuture<WorldChunk> getNonTickingChunkAsync(long key);

    // === FEATURES ===
    public void registerFeature(ClientFeature feature, boolean enabled);
    public boolean isFeatureEnabled(ClientFeature feature);

    // === LIFECYCLE ===
    public void stopIndividualWorld();
    public void validateDeleteOnRemove();

    // === TIME DILATION ===
    public static void setTimeDilation(float factor, ComponentAccessor<EntityStore> accessor);
}
```

### Key Fields
```java
private final String name;
private final Path savePath;
private final WorldConfig worldConfig;
private final ChunkStore chunkStore;
private final EntityStore entityStore;
private final Deque<Runnable> taskQueue;               // Task queue for world.execute()
private final AtomicBoolean alive;
private final AtomicBoolean acceptingTasks;
private final EventRegistry eventRegistry;             // Per-world events!
private boolean isTicking;
private boolean isPaused;
private long tick;                                     // Current tick counter
private final Random random;
```

---

## 3. TransformComponent - Full Teleportation API

**Package:** `com.hypixel.hytale.server.core.modules.entity.component`

```java
public class TransformComponent implements Component<EntityStore> {

    // === CONSTRUCTORS ===
    public TransformComponent();
    public TransformComponent(Vector3d position, Vector3f rotation);

    // === POSITION ===
    public Vector3d getPosition();
    public void setPosition(Vector3d position);            // Move (smooth/interpolated? — likely server-side only)
    public void teleportPosition(Vector3d position);       // Teleport (instant, sends packet to client)

    // === ROTATION ===
    public Vector3f getRotation();
    public void setRotation(Vector3f rotation);            // Set rotation
    public void teleportRotation(Vector3f rotation);       // Teleport rotation (instant)

    // === COMBINED ===
    public Transform getTransform();                       // Get position + rotation as Transform

    // === CHUNK TRACKING ===
    public WorldChunk getChunk();                          // Which chunk the entity is in
    public Ref<ChunkStore> getChunkRef();
    public void setChunkLocation(Ref<ChunkStore> ref, WorldChunk chunk);
    public void markChunkDirty(ComponentAccessor<EntityStore> accessor);

    // === CLONE ===
    public TransformComponent clone();

    // === COMPONENT TYPE ===
    public static ComponentType<EntityStore, TransformComponent> getComponentType();
}
```

### Key Fields
```java
private final Vector3d position;
private final Vector3f rotation;
private final ModelTransform sentTransform;      // Last transform sent to client
private WorldChunk chunk;                         // Current chunk
private Ref<ChunkStore> chunkRef;                 // Current chunk reference
```

### Difference: setPosition vs teleportPosition
- `setPosition(pos)` — updates server-side position, likely used for movement interpolation
- `teleportPosition(pos)` — instant teleport, sends teleport packet to client
- Same pattern for `setRotation` vs `teleportRotation`

### Usage Pattern
```java
TransformComponent tc = entity.getTransformComponent();

// Read
Vector3d pos = tc.getPosition();
Vector3f rot = tc.getRotation();

// Teleport
tc.teleportPosition(new Vector3d(x, y, z));
tc.teleportRotation(new Vector3f(pitch, yaw, roll));
```

---

## 4. Entity.loadIntoWorld() - Cross-World Movement

From Entity class:
```java
public abstract class Entity implements Component<EntityStore> {
    public void loadIntoWorld(World world);     // Move entity to another world
    public void unloadFromWorld();               // Remove from current world
    public World getWorld();                     // Current world
}
```

### How loadIntoWorld Works:
1. `loadIntoWorld(World)` moves the entity between worlds
2. It does **NOT** set a position — it just handles the world transfer
3. **You must set position separately** after loading into the new world
4. For players, the proper pattern uses `World.addPlayer()` with a `Transform`

### Player World Transfer (Proper Way):
The `World.addPlayer()` methods are the correct way to move players between worlds:
```java
// Simple — uses world's spawn point
CompletableFuture<PlayerRef> addPlayer(PlayerRef ref);

// With explicit spawn position
CompletableFuture<PlayerRef> addPlayer(PlayerRef ref, Transform spawn);

// With extra flags
CompletableFuture<PlayerRef> addPlayer(PlayerRef ref, Transform spawn, Boolean flag1, Boolean flag2);
```

### For Bulk Transfers:
```java
// Move ALL players from one world to another
CompletableFuture<Void> drainPlayersTo(World targetWorld);
```

---

## 5. TickingThread - The Tick Loop

**Package:** `com.hypixel.hytale.server.core.util.thread`

```java
public abstract class TickingThread implements Runnable {

    // === CONSTANTS ===
    public static final int NANOS_IN_ONE_MILLI;
    public static final int NANOS_IN_ONE_SECOND;
    public static final int TPS;                       // Default TPS (20)
    public static long SLEEP_OFFSET;

    // === CONSTRUCTOR ===
    public TickingThread(String threadName);
    public TickingThread(String threadName, int tps, boolean daemon);

    // === LIFECYCLE ===
    public CompletableFuture<Void> start();
    public boolean interrupt();
    public void stop();

    // === TICK ===
    protected abstract void tick(float delta);          // Called every tick with delta time
    protected void onStart();
    protected abstract void onShutdown();
    protected boolean isIdle();

    // === TPS CONTROL ===
    public void setTps(int tps);
    public int getTps();
    public int getTickStepNanos();

    // === THREAD SAFETY ===
    public boolean isInThread();                       // Are we on this tick thread?
    public boolean isStarted();
    public void debugAssertInTickingThread();           // Assert we're on the right thread
}
```

### Tickable Interface
```java
// Simple interface for anything that ticks
public interface Tickable {
    void tick(float delta);    // delta = time since last tick in seconds
}
```

### How World's Tick Works:
World overrides `tick(float delta)` from TickingThread. During each tick:
1. Task queue is consumed (`consumeTaskQueue()`) — runs all Runnables posted via `execute()`
2. ECS systems tick (entity movement, AI, combat, block ticking, etc.)
3. Chunk loading/unloading
4. Player chunk tracker updates
5. Time advances

---

## 6. Scheduling & Task Execution

### ⚠️ NO BUILT-IN TICK SCHEDULER! Here are your options:

### Option A: World.execute() — Run on Next Tick
```java
// Post a Runnable to the world's task queue
// Executes on the world's thread during the NEXT tick
world.execute(() -> {
    // This runs on the world thread
    // Safe to access world entities, chunks, etc.
});
```

### Option B: HytaleServer.SCHEDULED_EXECUTOR — Java ScheduledExecutorService
```java
// Global scheduled executor — standard Java scheduling
// ⚠️ Runs on a DIFFERENT thread — NOT the world thread!
HytaleServer.SCHEDULED_EXECUTOR.schedule(runnable, delay, TimeUnit.MILLISECONDS);
HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(runnable, initialDelay, period, TimeUnit.MILLISECONDS);
```

**Pattern for tick-safe scheduled tasks:**
```java
// Schedule on executor, but delegate to world thread:
HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
    world.execute(() -> {
        // This is tick-safe! Runs on world thread.
        doPeriodicWork();
    });
}, 0, 1000, TimeUnit.MILLISECONDS);  // every 1 second
```

### Option C: TaskRegistry — Plugin Task Management
```java
// From PluginBase (your plugin has this):
public TaskRegistry getTaskRegistry();

// TaskRegistry methods:
public TaskRegistration registerTask(CompletableFuture<Void> task);
public TaskRegistration registerTask(ScheduledFuture<Void> task);
```
This lets you register tasks that get cleaned up when the plugin shuts down. Good hygiene!

### Option D: ECS TickingSystem — For Per-Entity Ticking
```java
// For code that runs on EVERY entity with certain components each tick:
public abstract class EntityTickingSystem<ECS_TYPE> extends ArchetypeTickingSystem<ECS_TYPE> {
    public abstract void tick(float delta, int index, ArchetypeChunk<ECS_TYPE> chunk,
                              Store<ECS_TYPE> store, CommandBuffer<ECS_TYPE> cmd);
}

// Base:
public abstract class TickingSystem<ECS_TYPE> extends System<ECS_TYPE> {
    public abstract void tick(float delta, int tickCount, Store<ECS_TYPE> store);
}
```

### Option E: DIY Tick Counter (Simplest for Dungeons)
```java
// In a command or world.execute() context:
private long lastCheckTick = 0;
private static final long CHECK_INTERVAL = 20; // every second at 20 TPS

void onTick(World world) {
    long currentTick = world.getTick();
    if (currentTick - lastCheckTick >= CHECK_INTERVAL) {
        lastCheckTick = currentTick;
        checkDungeonState();
    }
}
```

### Recommended for Dungeon Plugin:
Use `HytaleServer.SCHEDULED_EXECUTOR` + `world.execute()` for periodic dungeon checks. Register tasks via `getTaskRegistry()` for cleanup.

---

## 7. World Creation & Instancing

### WorldConfig - Full Configuration
```java
public class WorldConfig {
    // === CORE ===
    public UUID getUuid();
    public void setUuid(UUID uuid);
    public String getDisplayName();
    public void setDisplayName(String name);
    public long getSeed();
    public void setSeed(long seed);

    // === WORLD GENERATION ===
    public IWorldGenProvider getWorldGenProvider();
    public void setWorldGenProvider(IWorldGenProvider provider);
    // Available providers:
    //   VoidWorldGenProvider — empty void world
    //   FlatWorldGenProvider — flat world with configurable layers
    //   (default provider)  — normal terrain generation

    // === SPAWN ===
    public ISpawnProvider getSpawnProvider();
    public void setSpawnProvider(ISpawnProvider provider);
    // Available providers:
    //   GlobalSpawnProvider  — single fixed spawn point for everyone
    //   IndividualSpawnProvider — per-player spawn points
    //   FitToHeightMapSpawnProvider — spawn on surface

    // === TICKING/GAMEPLAY ===
    public boolean isTicking();
    public void setTicking(boolean ticking);
    public boolean isBlockTicking();
    public void setBlockTicking(boolean ticking);
    public boolean isPvpEnabled();
    public void setPvpEnabled(boolean pvp);
    public boolean isFallDamageEnabled();
    public GameMode getGameMode();
    public void setGameMode(GameMode mode);

    // === NPC SPAWNING ===
    public boolean isSpawningNPC();
    public void setSpawningNPC(boolean spawning);      // Disable natural mob spawning!
    public boolean isAllNPCFrozen();
    public void setIsAllNPCFrozen(boolean frozen);

    // === SAVING ===
    public boolean isSavingPlayers();
    public void setSavingPlayers(boolean saving);
    public boolean canSaveChunks();
    public void setCanSaveChunks(boolean canSave);
    public boolean shouldSaveNewChunks();
    public void setSaveNewChunks(boolean save);
    public boolean canUnloadChunks();
    public void setCanUnloadChunks(boolean canUnload);

    // === DELETION FLAGS ===
    public boolean isDeleteOnUniverseStart();
    public void setDeleteOnUniverseStart(boolean delete);
    public boolean isDeleteOnRemove();
    public void setDeleteOnRemove(boolean delete);       // ⚡ Delete world files when removed!

    // === CHUNKS ===
    public ChunkConfig getChunkConfig();
    public void setChunkConfig(ChunkConfig config);

    // === CHUNK STORAGE ===
    public IChunkStorageProvider getChunkStorageProvider();
    public void setChunkStorageProvider(IChunkStorageProvider provider);

    // === TIME ===
    public boolean isGameTimePaused();
    public void setGameTimePaused(boolean paused);
    public Instant getGameTime();
    public void setGameTime(Instant time);

    // === PLUGIN CONFIG ===
    public MapKeyMapCodec.TypeMap<Object> getPluginConfig();
    // Used by instances plugin to store InstanceWorldConfig

    // === PERSISTENCE ===
    public static CompletableFuture<WorldConfig> load(Path path);
    public static CompletableFuture<Void> save(Path path, WorldConfig config);
}
```

### ChunkConfig
```java
public class WorldConfig.ChunkConfig {
    public Box2D getPregenerateRegion();                // Pre-generate chunks in this region
    public void setPregenerateRegion(Box2D region);
    public Box2D getKeepLoadedRegion();                 // Keep chunks loaded in this region
    public void setKeepLoadedRegion(Box2D region);
}
```

### Creating a Dungeon World Programmatically
```java
WorldConfig config = new WorldConfig();
config.setUuid(UUID.randomUUID());
config.setDisplayName("Dungeon Instance");
config.setSeed(System.currentTimeMillis());

// Void world — we'll paste the dungeon structure ourselves
config.setWorldGenProvider(new VoidWorldGenProvider());

// Fixed spawn point
config.setSpawnProvider(new GlobalSpawnProvider(
    new Transform(new Vector3d(0, 64, 0), new Vector3f(0, 0, 0))
));

// Dungeon settings
config.setSpawningNPC(false);          // No natural spawning — we control mobs
config.setBlockTicking(false);         // No random block ticks
config.setPvpEnabled(false);           // No PvP in dungeons
config.setSavingPlayers(false);        // Don't save player data in instance
config.setCanSaveChunks(false);        // Don't persist chunks
config.setDeleteOnRemove(true);        // Clean up when done
config.setCanUnloadChunks(false);      // Keep all chunks loaded

// Create
Universe.get().makeWorld("dungeon_" + UUID.randomUUID(), savePath, config)
    .thenAccept(world -> {
        // World is ready!
    });
```

---

## 8. InstancesPlugin - Built-in Instance System

**Package:** `com.hypixel.hytale.builtin.instances`

⚡ **Hytale has a BUILT-IN instance system!** This is exactly what we need for dungeons.

```java
public class InstancesPlugin extends JavaPlugin {

    // === SINGLETON ===
    public static InstancesPlugin get();
    public static final String INSTANCE_PREFIX;        // Prefix for instance world names
    public static final String CONFIG_FILENAME;

    // === SPAWN AN INSTANCE ===
    // Clone an instance template world and start it
    public CompletableFuture<World> spawnInstance(String templateName, World returnWorld, Transform returnPoint);
    public CompletableFuture<World> spawnInstance(String templateName, String customName,
                                                   World returnWorld, Transform returnPoint);

    // === TELEPORT PLAYER TO INSTANCE ===
    // Teleport to a loading instance (waits for world to be ready)
    public static void teleportPlayerToLoadingInstance(
        Ref<EntityStore> ref,
        ComponentAccessor<EntityStore> accessor,
        CompletableFuture<World> loadingWorld,
        Transform spawnPoint
    );

    // Teleport to an already-loaded instance
    public static void teleportPlayerToInstance(
        Ref<EntityStore> ref,
        ComponentAccessor<EntityStore> accessor,
        World instanceWorld,
        Transform spawnPoint
    );

    // === EXIT INSTANCE ===
    // Returns player to their saved return point
    public static void exitInstance(
        Ref<EntityStore> ref,
        ComponentAccessor<EntityStore> accessor
    );

    // === REMOVE INSTANCE ===
    public static void safeRemoveInstance(String name);
    public static void safeRemoveInstance(UUID worldUuid);
    public static void safeRemoveInstance(World world);

    // === INSTANCE TEMPLATE MANAGEMENT ===
    public static Path getInstanceAssetPath(String templateName);
    public static boolean doesInstanceAssetExist(String templateName);
    public static CompletableFuture<World> loadInstanceAssetForEdit(String templateName);
    public List<String> getInstanceAssets();
}
```

### Instance World Config (per-instance settings)
```java
public class InstanceWorldConfig {
    public static InstanceWorldConfig get(WorldConfig config);
    public static InstanceWorldConfig ensureAndGet(WorldConfig config);

    public RemovalCondition[] getRemovalConditions();
    public void setRemovalConditions(RemovalCondition... conditions);

    public WorldReturnPoint getReturnPoint();           // Where to send players when they exit
    public void setReturnPoint(WorldReturnPoint point);

    public boolean shouldPreventReconnection();         // Prevent reconnecting to instance

    public InstanceDiscoveryConfig getDiscovery();
    public void setDiscovery(InstanceDiscoveryConfig config);
}
```

### World Return Point
```java
public class WorldReturnPoint {
    public WorldReturnPoint();
    public WorldReturnPoint(UUID world, Transform returnPoint, boolean returnOnReconnect);

    public UUID getWorld();
    public void setWorld(UUID world);
    public Transform getReturnPoint();
    public void setReturnPoint(Transform point);
    public boolean isReturnOnReconnect();
    public void setReturnOnReconnect(boolean returnOnReconnect);
}
```

### Instance Entity Config (per-player instance data, stored as ECS component)
```java
public class InstanceEntityConfig implements Component<EntityStore> {
    public WorldReturnPoint getReturnPoint();
    public void setReturnPoint(WorldReturnPoint point);
    public WorldReturnPoint getReturnPointOverride();
    public void setReturnPointOverride(WorldReturnPoint point);
}
```

### Removal Conditions
```java
// When to auto-remove an instance world:
public interface RemovalCondition {
    boolean shouldRemoveWorld(Store<ChunkStore> store);
}

// Implementations:
WorldEmptyCondition    — remove when no players are in the world (with timeout)
TimeoutCondition       — remove after N seconds regardless
IdleTimeoutCondition   — remove after N seconds of no activity
```

### Complete Instance Usage Pattern:
```java
// 1. Spawn instance from template
CompletableFuture<World> instanceFuture = InstancesPlugin.get().spawnInstance(
    "dungeon_template",           // template name (from instance assets)
    player.getWorld(),            // return world
    player.getTransformComponent().getTransform()  // return position
);

// 2. Teleport player to loading instance
InstancesPlugin.teleportPlayerToLoadingInstance(
    playerRef.getReference(),
    accessor,                     // ComponentAccessor from command/tick context
    instanceFuture,
    new Transform(0, 64, 0)       // spawn point in instance
);

// 3. Later: remove instance when done
InstancesPlugin.safeRemoveInstance(instanceWorld);
```

---

## 9. World Deletion & Unloading

### Remove World from Universe
```java
// Stops the world thread and removes from Universe's world map
Universe.get().removeWorld("worldName");    // returns boolean

// Or with error handling
Universe.get().removeWorldExceptionally("worldName");
```

### Safe Instance Removal
```java
// Instance-aware: drains players first, then removes
InstancesPlugin.safeRemoveInstance(world);
InstancesPlugin.safeRemoveInstance("worldName");
InstancesPlugin.safeRemoveInstance(worldUuid);
```

### Auto-Deletion via WorldConfig
```java
config.setDeleteOnRemove(true);             // Delete world files when removeWorld() is called
config.setDeleteOnUniverseStart(true);      // Delete on next server start (cleanup leftover instances)
```

### Drain Players Before Removing
```java
// Move all players to another world before removing
world.drainPlayersTo(targetWorld).thenRun(() -> {
    Universe.get().removeWorld(world.getName());
});
```

### Stop World Thread
```java
world.stopIndividualWorld();  // Stops ticking, but doesn't remove from Universe
world.stop();                 // TickingThread.stop() — stops the thread
```

---

## 10. Chunk Loading & Spawn Points

### ChunkTracker (Per-Player Chunk Loading)
```java
public class ChunkTracker implements Component<EntityStore> {

    // === CONSTANTS ===
    public static final int MAX_CHUNKS_PER_SECOND_LOCAL;
    public static final int MAX_CHUNKS_PER_SECOND_LAN;
    public static final int MAX_CHUNKS_PER_SECOND;
    public static final int MAX_CHUNKS_PER_TICK;
    public static final int MIN_LOADED_CHUNKS_RADIUS;
    public static final int MAX_HOT_LOADED_CHUNKS_RADIUS;

    // === CONFIGURATION ===
    public void setMaxChunksPerSecond(int max);
    public void setMaxChunksPerTick(int max);
    public void setMinLoadedChunksRadius(int radius);
    public void setMaxHotLoadedChunksRadius(int radius);

    // === CHUNK STATE ===
    public boolean isLoaded(long chunkKey);
    public boolean shouldBeVisible(long chunkKey);
    public ChunkVisibility getChunkVisibility(long chunkKey);
    public int getLoadedChunksCount();
    public int getLoadingChunksCount();
    public boolean isReadyForChunks();
    public void setReadyForChunks(boolean ready);

    // === LOADING ===
    public void tryLoadChunkAsync(ChunkStore store, PlayerRef ref, long chunkKey,
                                   TransformComponent tc, ComponentAccessor<EntityStore> accessor);
    public void unloadAll(PlayerRef ref);
    public void clear();
}
```

### Chunk Pre-Loading via ChunkConfig
```java
WorldConfig.ChunkConfig chunkConfig = new WorldConfig.ChunkConfig();

// Pre-generate chunks in a region (on world creation)
chunkConfig.setPregenerateRegion(new Box2D(minX, minZ, maxX, maxZ));

// Keep chunks loaded even without players
chunkConfig.setKeepLoadedRegion(new Box2D(minX, minZ, maxX, maxZ));

config.setChunkConfig(chunkConfig);
```

### Spawn Provider System
```java
public interface ISpawnProvider {
    // Get spawn point for a specific entity
    Transform getSpawnPoint(World world, UUID playerUuid);

    // Get all spawn points
    Transform[] getSpawnPoints();

    // Check distance from spawn
    boolean isWithinSpawnDistance(Vector3d position, double distance);
}

// === IMPLEMENTATIONS ===

// Everyone spawns at the same point
public class GlobalSpawnProvider implements ISpawnProvider {
    public GlobalSpawnProvider(Transform spawnPoint);
    // ...
}

// Per-player spawn points
public class IndividualSpawnProvider implements ISpawnProvider { ... }

// Spawn on the terrain surface
public class FitToHeightMapSpawnProvider implements ISpawnProvider { ... }
```

### Transform (Position + Rotation)
```java
public class Transform {
    public Transform();
    public Transform(double x, double y, double z);
    public Transform(double x, double y, double z, float pitch, float yaw, float roll);
    public Transform(Vector3d position);
    public Transform(Vector3d position, Vector3f rotation);

    public Vector3d getPosition();
    public void setPosition(Vector3d pos);
    public Vector3f getRotation();
    public void setRotation(Vector3f rot);
    public Vector3d getDirection();

    // Relative transform support
    public static final int X_IS_RELATIVE;
    public static final int Y_IS_RELATIVE;
    public static final int Z_IS_RELATIVE;
    public static final int YAW_IS_RELATIVE;
    public static final int PITCH_IS_RELATIVE;
    public static final int ROLL_IS_RELATIVE;
    public static final int RELATIVE_TO_BLOCK;
}
```

---

## 11. Event System

### Two Levels of Event Registration

**1. Global EventBus** (HytaleServer level — receives ALL events from ALL worlds)
```java
EventBus eventBus = HytaleServer.get().getEventBus();
// Via PluginBase:
getEventRegistry().register(PlayerReadyEvent.class, event -> { ... });
```

**2. Per-World EventRegistry** (only events from that specific world)
```java
World world = ...;
world.getEventRegistry().register(EntityRemoveEvent.class, event -> { ... });
```

### Key Events for Dungeons

```java
// Player finished loading into world
PlayerReadyEvent
    getPlayer() -> Player

// Player being added to a world
AddPlayerToWorldEvent implements IEvent<String>
    getHolder() -> Holder<EntityStore>
    getWorld() -> World
    shouldBroadcastJoinMessage() -> boolean
    setBroadcastJoinMessage(boolean)

// Player being drained (moved) from a world
DrainPlayerFromWorldEvent implements IEvent<String>
    getHolder() -> Holder<EntityStore>
    getWorld() -> World          // can be changed!
    getTransform() -> Transform  // can be changed!
    setWorld(World)
    setTransform(Transform)

// Entity removed from world
EntityRemoveEvent extends EntityEvent<Entity, String>
    // From EntityEvent: getEntity()

// Player connected to server
PlayerConnectEvent
// Player disconnected
PlayerDisconnectEvent
```

### Registration Patterns
```java
// Simple registration (void key)
eventRegistry.register(PlayerReadyEvent.class, event -> { ... });

// With priority
eventRegistry.register(EventPriority.HIGH, PlayerReadyEvent.class, event -> { ... });

// Async event handling
eventRegistry.registerAsync(SomeAsyncEvent.class, future -> {
    return future.thenCompose(event -> {
        // async processing
        return CompletableFuture.completedFuture(event);
    });
});

// Global registration (catches ALL events of type, regardless of key)
eventRegistry.registerGlobal(EntityRemoveEvent.class, event -> { ... });
```

---

## 12. Practical Patterns for Dungeon Plugin

### Pattern 1: Create Dungeon Instance
```java
// Option A: Use built-in InstancesPlugin (RECOMMENDED)
CompletableFuture<World> instanceFuture = InstancesPlugin.get().spawnInstance(
    "my_dungeon_template",
    player.getWorld(),          // return world
    tc.getTransform()           // return position
);

// Option B: Manual world creation
WorldConfig config = new WorldConfig();
config.setWorldGenProvider(new VoidWorldGenProvider());
config.setSpawnProvider(new GlobalSpawnProvider(new Transform(0, 64, 0)));
config.setSpawningNPC(false);
config.setBlockTicking(false);
config.setDeleteOnRemove(true);
config.setSavingPlayers(false);
config.setCanSaveChunks(false);

Universe.get().makeWorld("dungeon_" + id, savePath, config);
```

### Pattern 2: Teleport Player to Dungeon
```java
// With InstancesPlugin (handles chunk loading, return points, etc.)
InstancesPlugin.teleportPlayerToLoadingInstance(
    playerRef.getReference(), accessor, instanceFuture,
    new Transform(0, 64, 0)
);

// Manual approach
World dungeonWorld = Universe.get().getWorld("dungeon_123");
dungeonWorld.addPlayer(playerRef, new Transform(0, 64, 0)).thenAccept(ref -> {
    // Player is now in the dungeon world
});
```

### Pattern 3: Periodic Dungeon Tick (Check Room Clear, Timers)
```java
// Register a repeating task
ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
    if (dungeonWorld.isAlive()) {
        dungeonWorld.execute(() -> {
            // Safe: running on world thread
            long tick = dungeonWorld.getTick();
            checkRoomClear(dungeonWorld);
            updateTimers(dungeonWorld);
            spawnNextWave(dungeonWorld);
        });
    }
}, 0, 50, TimeUnit.MILLISECONDS);  // Every tick (50ms = 20 TPS)

// Register with TaskRegistry for cleanup
getTaskRegistry().registerTask(future);
```

### Pattern 4: Teleport Within Same World
```java
TransformComponent tc = player.getTransformComponent();
tc.teleportPosition(new Vector3d(x, y, z));
tc.teleportRotation(new Vector3f(0, yaw, 0));
```

### Pattern 5: Clean Up Dungeon
```java
// Using InstancesPlugin (handles draining, cleanup)
InstancesPlugin.safeRemoveInstance(dungeonWorld);

// Manual
dungeonWorld.drainPlayersTo(hubWorld).thenRun(() -> {
    Universe.get().removeWorld(dungeonWorld.getName());
});
```

### Pattern 6: Listen for Entity Death in Dungeon
```java
// Register on the dungeon world's event registry
dungeonWorld.getEventRegistry().registerGlobal(EntityRemoveEvent.class, event -> {
    Entity removed = event.getEntity();
    if (removed instanceof NPCEntity npc) {
        onMobKilled(npc);
    }
});
```

---

## Package Reference

| Class | Full Package |
|-------|-------------|
| `Universe` | `com.hypixel.hytale.server.core.universe` |
| `World` | `com.hypixel.hytale.server.core.universe.world` |
| `WorldConfig` | `com.hypixel.hytale.server.core.universe.world` |
| `WorldConfig.ChunkConfig` | `com.hypixel.hytale.server.core.universe.world` |
| `TransformComponent` | `com.hypixel.hytale.server.core.modules.entity.component` |
| `Transform` | `com.hypixel.hytale.math.vector` |
| `TickingThread` | `com.hypixel.hytale.server.core.util.thread` |
| `Tickable` | `com.hypixel.hytale.common.thread.ticking` |
| `TickingSystem` | `com.hypixel.hytale.component.system.tick` |
| `EntityTickingSystem` | `com.hypixel.hytale.component.system.tick` |
| `HytaleServer` | `com.hypixel.hytale.server.core` |
| `InstancesPlugin` | `com.hypixel.hytale.builtin.instances` |
| `InstanceWorldConfig` | `com.hypixel.hytale.builtin.instances.config` |
| `InstanceEntityConfig` | `com.hypixel.hytale.builtin.instances.config` |
| `WorldReturnPoint` | `com.hypixel.hytale.builtin.instances.config` |
| `RemovalCondition` | `com.hypixel.hytale.builtin.instances.removal` |
| `WorldEmptyCondition` | `com.hypixel.hytale.builtin.instances.removal` |
| `TimeoutCondition` | `com.hypixel.hytale.builtin.instances.removal` |
| `IdleTimeoutCondition` | `com.hypixel.hytale.builtin.instances.removal` |
| `ISpawnProvider` | `com.hypixel.hytale.server.core.universe.world.spawn` |
| `GlobalSpawnProvider` | `com.hypixel.hytale.server.core.universe.world.spawn` |
| `IWorldGenProvider` | `com.hypixel.hytale.server.core.universe.world.worldgen.provider` |
| `VoidWorldGenProvider` | `com.hypixel.hytale.server.core.universe.world.worldgen.provider` |
| `FlatWorldGenProvider` | `com.hypixel.hytale.server.core.universe.world.worldgen.provider` |
| `ChunkTracker` | `com.hypixel.hytale.server.core.modules.entity.player` |
| `PlayerRef` | `com.hypixel.hytale.server.core.universe` |
| `TaskRegistry` | `com.hypixel.hytale.server.core.task` |
| `EventRegistry` | `com.hypixel.hytale.event` |
| `EventBus` | `com.hypixel.hytale.event` |
| `TimeResource` | `com.hypixel.hytale.server.core.modules.time` |
| `WorldTimeResource` | `com.hypixel.hytale.server.core.modules.time` |

---

## Key Takeaways for Dungeon Plugin

1. **Use InstancesPlugin!** — Hytale already has a full instance system. Don't reinvent it.
2. **No tick scheduler** — Use `HytaleServer.SCHEDULED_EXECUTOR` + `world.execute()` for periodic tasks.
3. **World = Thread** — Each world runs on its own thread. Use `world.execute()` for thread safety.
4. **TransformComponent** has both `setPosition` (smooth) and `teleportPosition` (instant).
5. **Entity.loadIntoWorld()** only changes the world, NOT position. Use `World.addPlayer(ref, transform)` for players.
6. **WorldConfig** controls everything: worldgen, spawn, ticking, saving, deletion.
7. **VoidWorldGenProvider** is perfect for dungeon instances (empty world, we populate it).
8. **ChunkConfig.keepLoadedRegion** keeps chunks loaded without players — important for dungeons.
9. **deleteOnRemove** auto-cleans world files — essential for transient instances.
10. **Per-world EventRegistry** lets you listen for events only in the dungeon world.

---

*Generated 2026-02-07 from HytaleServer.jar analysis*
