# Hytale Dungeon Plugin V2 — Codex Build Spec

> **Project:** `com.howlstudio.dungeons` (DungeonPlugin)
> **Base:** Existing plugin at `plugin/` — 35 Java files, ~3,500 lines, Gradle build
> **Target:** Hytale Early Access server plugin (JavaPlugin API)
> **Reference:** Decompiled RPGLeveling v0.2.7 by Zuxaw (73 files, 14,755 lines) — patterns for UI, HUD, ECS, configs
> **Author:** Howl Studio

---

## CRITICAL RULES FOR CODEX

1. **DO NOT delete or rewrite existing files.** Add new files. Modify existing files surgically.
2. **DO NOT touch `build.gradle.kts` unless adding a new dependency.** The build already works.
3. **Preserve ALL existing functionality.** Every current command, service, and system must still work after changes.
4. **Use the existing code patterns.** Look at `DungeonManager.java`, `DungeonAdminCommand.java`, `DungeonHudService.java` for style/patterns.
5. **Hytale API types are NOT available as source** — they come from `libs/HytaleServer.jar`. Import them; don't create stubs.
6. **Test: `./gradlew build` must succeed with zero errors.**
7. **All new files go under `src/main/java/com/howlstudio/dungeons/`.**

---

## ARCHITECTURE OVERVIEW (existing)

```
com.howlstudio.dungeons/
  DungeonPlugin.java          ← Entry point (setup/start/shutdown)
  commands/                   ← All /dungeon commands + shortcuts
  config/                     ← DungeonTemplate + loader (JSON configs)
  events/                     ← Entity remove listener
  loot/                       ← LootService (room + final loot)
  manager/                    ← Core: DungeonManager, Instance, Portal, World, SpawnPoint
  spawning/                   ← MobSpawner (NPC spawning + tracking)
  systems/                    ← DungeonTickSystem + DungeonTicker
  ui/                         ← DungeonHud, HudService, ListPage, StatusPage
  util/                       ← Msg (themed messages), Players (resolver)
```

### Key patterns from existing code:
- **Services are plain classes** instantiated in `DungeonPlugin.setup()` and passed via constructor
- **Commands** extend `BaseDungeonCommand` which provides `resolvePlayer()`, `parseExtraArgs()`, etc.
- **ECS systems** implement `ISystem` from Hytale API and are registered via `getEntityStoreRegistry().registerSystem()`
- **UI pages** extend Hytale's page API (`player.getPageManager().openCustomPage()`)
- **HUD** uses `player.getHudManager().setCustomHud()` + `UICommandBuilder` for data binding
- **Configs** are loaded from `DungeonConfigs/` directory as JSON via Gson
- **All state is synchronized** in `DungeonManager` with `synchronized` methods and `ConcurrentHashMap`

### Key patterns from RPGLeveling (reference for new features):
- **ECS components**: `getEntityStoreRegistry().registerComponent(MyData.class, "MyData", MyData.CODEC)`
- **Event hooks**: `getEventRegistry().registerGlobal(EventType.class, handler)`
- **Stat modifiers**: `statMap.putModifier(statId, "key", new StaticModifier(...))`
- **Config codec**: `this.withConfig("ConfigName", MyConfig.CODEC)` in plugin setup
- **Scheduled tasks**: `HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(task, 0, interval, unit)`
- **UI panels**: RPGLeveling uses `StatsGUIPage` (880 lines) with full clickable stat allocation — reference for our dungeon browser GUI

---

## FEATURE 1: Enhanced Dungeon World Management

### Current state:
- ✅ `/dungeon register <world> [dungeonId] [auto] [roomSize] [layout] [difficulty] [theme]` — works
- ✅ `/dungeon unregister <world>` — works
- ✅ `/dungeon enable <world>` / `disable` — works
- ✅ `DungeonWorldRegistry` persists to `dungeonWorlds.json`
- ✅ `AutoGenerationSettings` data class exists (roomSize, layoutType, difficulty, theme)

### TODO — New:
- [ ] **World folder drop detection**: Watch `DungeonConfigs/worlds/` directory for new `.zip` or folder entries. On detection, auto-register as a dungeon world.
- [ ] **World import command**: `/dungeon import <worldFile>` — copies world file to server's world directory and registers it.

### Implementation:

**File: `manager/DungeonWorldWatcher.java`** (NEW)
```java
package com.howlstudio.dungeons.manager;

/**
 * Watches DungeonConfigs/worlds/ for new world folders/zips.
 * On detection, auto-registers them via DungeonWorldRegistry.
 * Runs as a scheduled task (check every 30 seconds).
 *
 * Constructor: DungeonWorldWatcher(Path watchDir, DungeonWorldRegistry registry)
 * Method: start() — begins polling
 * Method: stop() — stops polling
 * Method: scanOnce() — manual scan (called by /dungeon reload)
 *
 * Detection: any new subdirectory or .zip file not already registered.
 * For .zip: extract to server worlds dir, then register.
 * For directory: register directly.
 * Auto-registration uses directory name as both worldName and dungeonId.
 */
```

**Modify: `DungeonPlugin.java`**
- In `setup()`: create `DungeonWorldWatcher` and store as field
- In `start()`: call `worldWatcher.start()`
- In `shutdown()`: call `worldWatcher.stop()`

**Modify: `DungeonAdminCommand.java`**
- Add `import` subcommand in the switch statement
- `handleImport(player, extras)` — accepts world name, triggers registration

---

## FEATURE 2: Enhanced Portal System

### Current state:
- ✅ `/dungeon setportal <dungeon> [radius] [resetMode]` — works
- ✅ `PortalManager` ticks every cycle, checks player proximity
- ✅ `PortalResetMode`: `ON_EMPTY`, `ON_COMPLETE`, `NEVER`
- ✅ Portals saved to `portals.json`

### TODO — New:
- [ ] **Portal linking to auto-generated dungeons**: When portal is triggered and dungeon has `autoGenerated=true`, generate a fresh instance with random parameters from `AutoGenerationSettings`.
- [ ] **Portal visual indicator**: Place a particle effect or block marker at portal location (use scheduled task to repeatedly show particles to nearby players).
- [ ] **Portal cooldown**: Configurable per-portal cooldown (prevent spam entry). Default 10 seconds.

### Implementation:

**Modify: `manager/PortalManager.java`**
- Add `cooldownMs` field to `DungeonPortal` (default 10000)
- Add `lastUsedMs` tracking per portal
- In `tickPortals()`: skip portal if cooldown not expired
- Add `particleTick()` method — for each portal, send particle packets to nearby players (if Hytale API supports particle packets; otherwise skip — add TODO comment)

**Modify: `manager/DungeonPortal.java`**
- Add fields: `cooldownMs` (long), `lastUsedMs` (long, transient)
- Add getters/setters

**Modify: `DungeonManager.java`**
- In `createInstanceForPortal()`: check and respect cooldown

---

## FEATURE 3: Enhanced Dungeon GUI & HUD

### Current state:
- ✅ HUD shows: dungeon name, room name + difficulty, mobs left, timer, party size
- ✅ `DungeonHudService` manages per-player HUD lifecycle
- ✅ `DungeonListPage` — basic dungeon browser (opens as page)
- ✅ `DungeonStatusPage` — live dungeon status (opens as page)

### TODO — New:
- [ ] **Enhanced HUD**: Add to top-right overlay:
  - Current room number / total rooms (e.g., "Room 2/5")
  - Current wave / total waves (e.g., "Wave 1/3")
  - Boss indicator (skull icon or "⚔ BOSS" text when in boss room)
  - Difficulty level display
- [ ] **Status GUI panel**: Full interactive panel showing:
  - Room progress bar (visual)
  - Remaining mobs list (by type + count)
  - Boss health bar (if boss room)
  - Party member list with status
  - Timer (prominent)
- [ ] **Dungeon Browser GUI**: Enhanced interactive panel:
  - List all available dungeons with name, description, difficulty, player count range
  - "Create" button per dungeon (triggers instance creation)
  - Show active instances that can be joined
  - "Join" button per active instance

### Implementation:

**Modify: `ui/DungeonHudService.java`**
- Update `showOrUpdate()` signature to include: `int currentRoom, int totalRooms, int currentWave, int totalWaves, boolean isBoss, String difficulty`
- Update `UICommandBuilder` bindings:
  ```
  commands.set("dungeon.roomProgress", (currentRoom + 1) + "/" + totalRooms);
  commands.set("dungeon.waveProgress", (currentWave + 1) + "/" + totalWaves);
  commands.set("dungeon.isBoss", isBoss);
  commands.set("dungeon.difficulty", difficulty);
  ```

**Modify: `DungeonManager.java` → `refreshHud()`**
- Pass the new fields to `hudService.showOrUpdate()`

**File: `ui/DungeonBrowserPage.java`** (NEW)
```java
package com.howlstudio.dungeons.ui;

/**
 * Interactive dungeon browser GUI.
 * Shows available dungeon templates as a scrollable list.
 * Each entry shows: name, description, difficulty, player range, estimated time.
 * "Create" button starts a new instance.
 * Also shows "Active Dungeons" section with joinable instances.
 * "Join" button joins an existing instance.
 *
 * Uses Hytale's page API (extends appropriate base page class).
 * Reference: RPGLeveling's StatsGUIPage for interactive page patterns.
 *
 * Constructor: DungeonBrowserPage(PlayerRef playerRef, DungeonManager manager)
 * Data binding via UICommandBuilder for reactive updates.
 *
 * Page sections:
 * 1. Header: "Dungeon Browser" title
 * 2. Template list: for each template → name, desc, difficulty badge, player range, [Create] button
 * 3. Active instances: for each joinable instance → template name, players, room progress, [Join] button
 * 4. Footer: close button
 */
```

**File: `ui/DungeonStatusPanel.java`** (NEW)
```java
package com.howlstudio.dungeons.ui;

/**
 * Live dungeon status panel (replaces basic DungeonStatusPage).
 * Shows detailed run information:
 * - Dungeon name + difficulty badge
 * - Room progress: visual bar showing rooms cleared vs total
 * - Current room name + wave indicator
 * - Mob list: grouped by type with count (e.g., "Skeleton x3, Ogre x1")
 * - Boss health bar (if boss room — tracks boss entity HP)
 * - Timer: large, prominent countdown
 * - Party list: player names with online indicators
 * - Loot collected this run
 *
 * Updates every tick cycle via DungeonManager.refreshHud() → refreshStatusPanel()
 *
 * Constructor: DungeonStatusPanel(PlayerRef playerRef, DungeonInstance instance, DungeonManager manager)
 */
```

**Modify: `commands/DungeonBrowseCommand.java`**
- Use new `DungeonBrowserPage` instead of current `DungeonListPage`

**Modify: `commands/DungeonStatusCommand.java`**
- Use new `DungeonStatusPanel` instead of current `DungeonStatusPage`

---

## FEATURE 4: Enhanced Mob Spawn System

### Current state:
- ✅ `/dungeon mob add <dungeon> <entity> <position>` — works with `player`, `coords`, `facing`
- ✅ `SpawnPointManager` persists to `spawnPoints.json`
- ✅ `MobSpawnPoint` has: dungeonId, roomIndex, entityRole, position, wave, count, radius, chance
- ✅ Spawn points are loaded per dungeon and spawn on room activation

### TODO — New:
- [ ] **Position modes** (enhance existing):
  - `player` → current player position (already works)
  - `coords <x> <y> <z>` → specific coordinates (already works)
  - `facing` → raycast to block player is looking at (needs implementation)
- [ ] **Optional parameters** (enhance existing):
  - `amount <number>` → mob count (maps to existing `count` field)
  - `radius <number>` → spawn radius (maps to existing `radius` field)
  - `wave <number>` → which wave to spawn in (maps to existing `wave` field)
  - `chance <percentage>` → spawn probability 0-100 (maps to existing `chance` field, convert to 0.0-1.0)
- [ ] **Spawn point visualization** (debug mode):
  - When debug mode is on, show spawn points as colored particles
  - Different color per wave number
- [ ] **GUI-based spawn editor** (alternative to commands):
  - Admin opens editor, clicks in-world to place spawn points
  - Sliders for amount, wave, radius, chance

### Implementation:

**Modify: `commands/DungeonAdminCommand.java` → `handleMob()`**
The existing `handleMob()` already handles `add` with `player`/`coords`. Enhance:
- Add `facing` mode: use player's look direction to raycast. Hytale API: `player.getTransformComponent().getLookDirection()` + `world.raycast(origin, direction, maxDistance)` to find target block position.
- Parse optional params: loop through remaining `extras` looking for keyword pairs:
  ```java
  int amount = 1; double radius = 2.0; int wave = 0; double chance = 1.0;
  for (int i = startIndex; i < extras.length - 1; i += 2) {
      switch (extras[i].toLowerCase()) {
          case "amount" -> amount = parseInt(extras[i+1], 1);
          case "radius" -> radius = parseDouble(extras[i+1], 2.0);
          case "wave" -> wave = parseInt(extras[i+1], 0);
          case "chance" -> chance = parseInt(extras[i+1], 100) / 100.0;
      }
  }
  ```
- Create `MobSpawnPoint` with all fields populated

**File: `ui/SpawnEditorPage.java`** (NEW)
```java
package com.howlstudio.dungeons.ui;

/**
 * GUI-based spawn point editor for admins.
 * Opened via /dungeon edit spawns or /dungeon mob editor.
 *
 * Flow:
 * 1. Select dungeon from dropdown
 * 2. Select room (index or name)
 * 3. Select entity role from list (or type custom)
 * 4. Click "Place Mode" → player clicks in-world → position captured
 * 5. Adjust sliders: amount (1-20), wave (0-10), radius (0.0-20.0), chance (0-100%)
 * 6. Click "Save" → adds to SpawnPointManager
 * 7. Preview: show particle at saved position
 *
 * Reference: RPGLeveling's StatsGUIPage for slider/button interaction patterns.
 */
```

**Modify: `manager/SpawnPointManager.java`**
- Add `getSpawnPointsForDebug(String dungeonId)` → returns all spawn points for visualization
- Add `removeSpawnPoint(String dungeonId, int roomIndex, int pointIndex)` → for editor delete

---

## FEATURE 5: Room & Progression System Enhancement

### Current state:
- ✅ Rooms lock until all mobs defeated (message says "Room locked until all mobs are defeated")
- ✅ Wave progression: waves advance automatically when all mobs in wave die
- ✅ Room advancement: moves to next room when all waves cleared
- ✅ Boss room announcements ("BOSS INCOMING", "BOSS DEFEATED")
- ✅ Completion: dungeon completes when last room cleared

### TODO — New:
- [ ] **Physical barriers**: When room is locked, place barrier blocks (or use Hytale's barrier entity) at room exits. Remove barriers when room is cleared. Message: "Room barrier removed."
- [ ] **Timed rooms**: Optional `timedSeconds` field per room. If set, room must be cleared within time limit or dungeon fails. Separate from overall dungeon timer.
- [ ] **Puzzle rooms**: Room type `puzzle` — no mobs. Instead, a condition check (configurable). Room clears when condition met. For V1: simple "interact with block at position" trigger. Extensible for future puzzle types.
- [ ] **Room transition delay**: Configurable delay (default 3s) between room clear and next room start. Shows "Advancing in 3... 2... 1..." countdown.

### Implementation:

**Modify: `config/DungeonTemplate.java` → `RoomTemplate`**
Add fields:
```java
private String roomType = "combat";  // "combat", "boss", "timed", "puzzle"
private int timedSeconds = 0;        // 0 = no room timer
private double transitionDelay = 3.0; // seconds between rooms
private List<BarrierPoint> barriers;  // barrier block positions
private PuzzleTrigger puzzleTrigger;  // for puzzle rooms
```

**File: `manager/BarrierManager.java`** (NEW)
```java
package com.howlstudio.dungeons.manager;

/**
 * Manages physical barriers between rooms.
 * When a room starts: place barrier blocks at configured positions.
 * When room clears: remove barrier blocks.
 *
 * Barrier placement uses Hytale's World.setBlock() API.
 * Barrier positions are defined per room in the template JSON.
 *
 * If barrier positions are not configured, use a fallback:
 * place invisible collision entities at room exit points.
 *
 * Methods:
 * - placeBarriers(DungeonInstance, RoomTemplate) — called on room start
 * - removeBarriers(DungeonInstance, RoomTemplate) — called on room clear
 * - removeAllBarriers(DungeonInstance) — called on dungeon end/fail
 */
```

**File: `manager/PuzzleTrigger.java`** (NEW)
```java
package com.howlstudio.dungeons.manager;

/**
 * Simple puzzle room trigger.
 * V1: "interact" type — player interacts with block at target position.
 *
 * JSON config:
 * {
 *   "type": "interact",
 *   "targetPosition": { "x": 10, "y": 64, "z": -5 },
 *   "targetBlock": "Lever"  // optional, any block interaction if null
 * }
 *
 * Checked via event listener or tick system.
 * When triggered: room is marked as cleared, barriers removed.
 *
 * Future types: "pressure_plate", "sequence" (interact in order), "timer_survive"
 */
```

**Modify: `DungeonManager.java`**
- In `spawnCurrentWave()`: call `barrierManager.placeBarriers()`
- In `handleWaveOrRoomCleared()`: call `barrierManager.removeBarriers()`, add transition delay:
  ```java
  HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
      advanceToNextRoom(instanceId);
  }, (long)(room.getTransitionDelay() * 1000), TimeUnit.MILLISECONDS);
  ```
- Before advancing: send countdown messages ("Advancing in 3... 2... 1...")
- In `cleanupInstance()`: call `barrierManager.removeAllBarriers()`

---

## FEATURE 6: Enhanced Loot & Reward System

### Current state:
- ✅ `LootService` grants loot per room and on completion
- ✅ Loot entries: itemId, minCount, maxCount, chance
- ✅ `/dungeon loot add <dungeon> <room> <item> <chance>` — works
- ✅ Duplicate grant prevention via `lootGranted` set

### TODO — New:
- [ ] **Rarity tiers**: Each loot entry has a `rarity` field: `common`, `uncommon`, `rare`, `epic`, `legendary`. Affects drop chance multiplier and message color.
- [ ] **Weighted random rolls**: Support `weight` field for weighted selection from a loot pool (pick N items from pool based on weight).
- [ ] **Loot table JSON**: Support external loot table files in `DungeonConfigs/loot/`. Can be referenced by name from room configs.
- [ ] **Boss loot**: Boss rooms use a separate `bossLoot` list with guaranteed drops + bonus rolls.
- [ ] **Loot GUI**: Show loot popup when items are granted (styled message or small panel).

### Implementation:

**File: `loot/LootTable.java`** (NEW)
```java
package com.howlstudio.dungeons.loot;

/**
 * External loot table definition. Loaded from DungeonConfigs/loot/*.json.
 *
 * JSON format:
 * {
 *   "id": "goblin_loot",
 *   "rolls": 2,
 *   "entries": [
 *     { "itemId": "gold_ingot", "minCount": 1, "maxCount": 3, "weight": 50, "rarity": "common" },
 *     { "itemId": "emerald", "minCount": 1, "maxCount": 1, "weight": 20, "rarity": "uncommon" },
 *     { "itemId": "diamond_sword", "minCount": 1, "maxCount": 1, "weight": 5, "rarity": "rare" }
 *   ]
 * }
 *
 * Weight-based selection: total weight = sum of all weights. Each roll picks random 0..totalWeight.
 * "rolls" = how many items are picked from the pool per grant.
 */
```

**File: `loot/LootTableLoader.java`** (NEW)
```java
package com.howlstudio.dungeons.loot;

/**
 * Loads loot tables from DungeonConfigs/loot/*.json.
 * Provides lookup by ID.
 * Called during DungeonPlugin.setup() and /dungeon reload.
 */
```

**File: `loot/Rarity.java`** (NEW)
```java
package com.howlstudio.dungeons.loot;

/**
 * Enum: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
 * Each has: name, color (for Msg formatting), drop multiplier (1.0 for common down to 0.1 for legendary)
 */
```

**Modify: `config/DungeonTemplate.java` → `LootEntry`**
Add fields:
```java
private String rarity = "common";  // common, uncommon, rare, epic, legendary
private int weight = 10;           // for weighted pool selection
```

**Modify: `config/DungeonTemplate.java` → `RoomTemplate`**
Add fields:
```java
private List<LootEntry> bossLoot;   // separate boss loot pool
private String lootTableRef;         // reference to external loot table ID
```

**Modify: `loot/LootService.java`**
- Add weighted selection logic
- Add rarity-based message coloring (use `Msg` color patterns)
- Add boss loot granting (after boss room clear, grant from `bossLoot` list)
- Add loot table reference resolution (look up from `LootTableLoader`)

---

## FEATURE 7: Instance System Enhancement

### Current state:
- ✅ Each dungeon runs in own instance (per `DungeonInstance`)
- ✅ Multiple parties can run same template simultaneously
- ✅ Cleanup on completion, failure, or abandonment
- ✅ Return teleport (saves position, restores on exit)

### TODO — New:
- [ ] **Instance isolation**: If Hytale supports world instancing (copy world per run), use it. If not, use world-sharing with spawn point offset per instance.
- [ ] **Auto-cleanup timer**: After dungeon ends (complete or fail), instance data is cleaned after 30 seconds. During this grace period, players can see final stats.
- [ ] **Instance stats**: Track per-instance: total kills, total damage dealt, time per room, deaths, loot collected. Display on completion screen.
- [ ] **Reconnect handling**: If player disconnects mid-dungeon, hold their slot for 60 seconds. If they reconnect, teleport back to dungeon. If timeout, treat as leave.

### Implementation:

**Modify: `manager/DungeonInstance.java`**
Add fields:
```java
private final Map<UUID, Long> disconnectedPlayers = new ConcurrentHashMap<>(); // uuid → disconnect timestamp
private int totalKills;
private int totalDeaths;
private long[] roomClearTimesMs;  // time to clear each room
private long roomStartMs;         // when current room started
private long cleanupAtMs;         // when to auto-cleanup (0 = not scheduled)
```
Add methods:
```java
public void trackKill() { totalKills++; }
public void trackDeath() { totalDeaths++; }
public void startRoomTimer() { roomStartMs = System.currentTimeMillis(); }
public long roomElapsedMs() { return System.currentTimeMillis() - roomStartMs; }
public void recordRoomClearTime(int roomIndex) { ... }
public void handleDisconnect(UUID playerUuid) { disconnectedPlayers.put(playerUuid, System.currentTimeMillis()); }
public boolean tryReconnect(UUID playerUuid) { return disconnectedPlayers.remove(playerUuid) != null; }
public List<UUID> getExpiredDisconnects(long timeoutMs) { ... }
```

**Modify: `DungeonPlugin.java`**
- Register `PlayerDisconnectEvent` listener → call `dungeonManager.handlePlayerDisconnect()`
- Register `PlayerReadyEvent` listener → call `dungeonManager.handlePlayerReconnect()`

**Modify: `DungeonManager.java`**
- Add `handlePlayerDisconnect(UUID playerUuid)` — marks as disconnected, starts timeout
- Add `handlePlayerReconnect(Player player)` — if within timeout, teleport back to dungeon
- In `tickInstance()`: check for expired disconnects, treat as leave
- On completion/fail: schedule cleanup with 30s delay, show stats to players

---

## FEATURE 8: Admin & Debug Commands

### Current state:
- ✅ `/dungeon reload` — reloads all configs
- ✅ `/dungeon forceend` — force-ends all instances
- ✅ `/dungeon debug` — toggles debug mode
- ✅ `/dungeon list` — lists registered worlds
- ✅ `/dungeon help` — command reference

### TODO — New:
- [ ] **Debug visualization**: When debug mode on, show:
  - Spawn point particles (colored by wave)
  - Room boundary outlines (if positions configured)
  - Portal radius circles
  - Trigger zone indicators (for puzzle rooms)
- [ ] **Admin teleport**: `/dungeon tp <instance>` — teleport to active instance without joining
- [ ] **Instance inspection**: `/dungeon inspect <instance>` — show full instance details (players, mobs, room, timers)
- [ ] **Spawn point list**: `/dungeon mob list <dungeon>` — show all configured spawn points
- [ ] **Spawn point remove**: `/dungeon mob remove <dungeon> <room> <index>` — remove a spawn point

### Implementation:

**File: `systems/DebugVisualizationTask.java`** (NEW)
```java
package com.howlstudio.dungeons.systems;

/**
 * Scheduled task (runs every 500ms when debug mode is on).
 * For each admin with debug mode active:
 * - Draw particles at all spawn points in their current dungeon world
 * - Draw particles at portal locations
 * - Draw particles at room boundaries (if configured)
 *
 * Uses Hytale particle API or chat-based coordinate display as fallback.
 * Disabled when debug mode is off (manager.isDebugMode()).
 */
```

**Modify: `commands/DungeonAdminCommand.java`**
Add subcommands in switch:
- `"tp"` → `handleTeleport(player, extras)` — find instance, teleport admin to dungeon world
- `"inspect"` → `handleInspect(player, extras)` — print full instance info to chat
- `"mob list"` → already partially in `handleMob()`, add listing
- `"mob remove"` → `handleMobRemove(player, extras)` — remove by index

---

## FEATURE 9: Mod Compatibility & API

### Current state:
- ✅ Plugin is self-contained, no external dependencies beyond Hytale API
- ✅ `manifest.json` has empty `Dependencies`

### TODO — New:
- [ ] **Public API class**: `DungeonAPI` — singleton providing hooks for other mods:
  - `getDungeonManager()` → access instance/template data
  - `registerCustomRoomType(String type, RoomHandler handler)` → custom room logic
  - `registerCustomLootProvider(String id, LootProvider provider)` → custom loot sources
  - `registerCustomMobModifier(String id, MobModifier modifier)` → modify mobs on spawn
  - Events: `onDungeonStart`, `onDungeonComplete`, `onRoomClear`, `onPlayerDeath`
- [ ] **RPGLeveling compatibility**: If RPGLeveling is present, scale dungeon difficulty by player level. Check for RPGLeveling plugin at startup, use its API if available.

### Implementation:

**File: `api/DungeonAPI.java`** (NEW)
```java
package com.howlstudio.dungeons.api;

/**
 * Public API for other Hytale mods to interact with the dungeon system.
 * Singleton accessed via DungeonAPI.get().
 *
 * Listener registration:
 *   DungeonAPI.get().onDungeonComplete(event -> { ... });
 *   DungeonAPI.get().onRoomClear(event -> { ... });
 *
 * Custom extensions:
 *   DungeonAPI.get().registerRoomType("puzzle_sequence", new SequencePuzzleHandler());
 *   DungeonAPI.get().registerLootProvider("boss_chest", new BossChestLootProvider());
 *
 * Reference: RPGLeveling's RPGLevelingAPI pattern (singleton, event listeners, typed getters).
 */
```

**File: `api/DungeonEvent.java`** (NEW — base event class)
**File: `api/DungeonStartEvent.java`** (NEW)
**File: `api/DungeonCompleteEvent.java`** (NEW)
**File: `api/RoomClearEvent.java`** (NEW)
**File: `api/PlayerDeathInDungeonEvent.java`** (NEW)

**File: `api/RoomHandler.java`** (NEW — interface for custom room types)
**File: `api/LootProvider.java`** (NEW — interface for custom loot)
**File: `api/MobModifier.java`** (NEW — interface for mob modification on spawn)

**Modify: `DungeonPlugin.java`**
- In `setup()`: initialize `DungeonAPI` singleton
- In `setup()`: check for RPGLeveling plugin presence:
  ```java
  // Optional RPGLeveling integration
  try {
      RPGLevelingAPI rpg = RPGLevelingAPI.get();
      if (rpg != null) {
          dungeonManager.setRpgIntegration(new RPGLevelingIntegration(rpg));
      }
  } catch (NoClassDefFoundError e) {
      // RPGLeveling not installed, skip integration
  }
  ```

**File: `compat/RPGLevelingIntegration.java`** (NEW)
```java
package com.howlstudio.dungeons.compat;

/**
 * Optional integration with Zuxaw's RPGLeveling plugin.
 * If RPGLeveling is installed:
 * - Scale dungeon mob HP and damage by average party level
 * - Grant XP for dungeon kills (using RPGLevelingAPI.addXP())
 * - Show player levels in party display
 * - Scale loot quality by dungeon difficulty + player level
 *
 * Loaded dynamically — no hard dependency. If RPGLeveling classes are missing,
 * this class is never instantiated (caught by NoClassDefFoundError in DungeonPlugin).
 */
```

---

## CONFIG FILE FORMATS

### DungeonConfigs/templates/goblin_den.json (enhanced)
```json
{
  "id": "goblin_den",
  "name": "Goblin Den",
  "description": "A dark cave infested with goblins.",
  "minPlayers": 1,
  "maxPlayers": 4,
  "timeLimitSeconds": 600,
  "difficulty": "normal",
  "rooms": [
    {
      "name": "Entrance Cavern",
      "roomType": "combat",
      "isBoss": false,
      "waves": 2,
      "timedSeconds": 0,
      "transitionDelay": 3.0,
      "spawnCenter": { "x": 10, "y": 64, "z": 5 },
      "spawnRadius": 5.0,
      "waveSpawns": [
        [
          { "role": "Goblin_Scavenger", "count": 3 },
          { "role": "Goblin_Miner", "count": 2 }
        ],
        [
          { "role": "Goblin_Fighter", "count": 4 }
        ]
      ],
      "barriers": [
        { "x": 15, "y": 64, "z": 0, "blockType": "barrier" },
        { "x": 15, "y": 65, "z": 0, "blockType": "barrier" }
      ],
      "loot": [
        { "itemId": "iron_ore", "minCount": 2, "maxCount": 5, "chance": 0.8, "rarity": "common", "weight": 50 }
      ],
      "lootTableRef": null
    },
    {
      "name": "Ogre's Lair",
      "roomType": "boss",
      "isBoss": true,
      "waves": 1,
      "timedSeconds": 120,
      "transitionDelay": 5.0,
      "spawnCenter": { "x": 30, "y": 64, "z": 5 },
      "spawnRadius": 3.0,
      "waveSpawns": [
        [
          { "role": "Goblin_Ogre", "count": 1 }
        ]
      ],
      "bossLoot": [
        { "itemId": "ogre_club", "minCount": 1, "maxCount": 1, "chance": 1.0, "rarity": "epic" }
      ],
      "loot": [
        { "itemId": "gold_ingot", "minCount": 3, "maxCount": 8, "chance": 1.0, "rarity": "uncommon" }
      ]
    }
  ]
}
```

### DungeonConfigs/loot/dungeon_basics.json (NEW)
```json
{
  "id": "dungeon_basics",
  "rolls": 2,
  "entries": [
    { "itemId": "bread", "minCount": 2, "maxCount": 5, "weight": 40, "rarity": "common" },
    { "itemId": "health_potion", "minCount": 1, "maxCount": 2, "weight": 25, "rarity": "uncommon" },
    { "itemId": "iron_ingot", "minCount": 1, "maxCount": 3, "weight": 20, "rarity": "uncommon" },
    { "itemId": "emerald", "minCount": 1, "maxCount": 1, "weight": 10, "rarity": "rare" },
    { "itemId": "enchanted_bow", "minCount": 1, "maxCount": 1, "weight": 4, "rarity": "epic" },
    { "itemId": "ancient_relic", "minCount": 1, "maxCount": 1, "weight": 1, "rarity": "legendary" }
  ]
}
```

---

## FILE MANIFEST (new + modified)

### New files to create:
| File | Lines (est) | Purpose |
|------|-------------|---------|
| `manager/DungeonWorldWatcher.java` | 80 | World folder auto-detection |
| `manager/BarrierManager.java` | 100 | Physical room barriers |
| `manager/PuzzleTrigger.java` | 60 | Puzzle room trigger system |
| `loot/LootTable.java` | 70 | External loot table definition |
| `loot/LootTableLoader.java` | 80 | Loot table file loader |
| `loot/Rarity.java` | 30 | Rarity tier enum |
| `ui/DungeonBrowserPage.java` | 200 | Interactive dungeon browser GUI |
| `ui/DungeonStatusPanel.java` | 250 | Live dungeon status panel |
| `ui/SpawnEditorPage.java` | 200 | GUI-based spawn point editor |
| `systems/DebugVisualizationTask.java` | 100 | Debug particle display |
| `api/DungeonAPI.java` | 150 | Public mod API |
| `api/DungeonEvent.java` | 20 | Base event class |
| `api/DungeonStartEvent.java` | 25 | Event |
| `api/DungeonCompleteEvent.java` | 25 | Event |
| `api/RoomClearEvent.java` | 25 | Event |
| `api/PlayerDeathInDungeonEvent.java` | 25 | Event |
| `api/RoomHandler.java` | 15 | Interface |
| `api/LootProvider.java` | 15 | Interface |
| `api/MobModifier.java` | 15 | Interface |
| `compat/RPGLevelingIntegration.java` | 80 | Optional RPGLeveling compat |

### Existing files to modify:
| File | Changes |
|------|---------|
| `DungeonPlugin.java` | Add world watcher, barrier manager, API init, event listeners, RPG compat |
| `manager/DungeonManager.java` | Barrier calls, transition delay, reconnect handling, instance stats, cleanup timer |
| `manager/DungeonInstance.java` | Stats tracking, disconnect handling, room timer, cleanup scheduling |
| `manager/PortalManager.java` | Cooldown, particles |
| `manager/DungeonPortal.java` | Cooldown fields |
| `manager/SpawnPointManager.java` | Debug listing, remove method |
| `config/DungeonTemplate.java` | New fields on RoomTemplate + LootEntry (roomType, timedSeconds, barriers, bossLoot, rarity, weight) |
| `loot/LootService.java` | Weighted selection, rarity colors, boss loot, loot table refs |
| `ui/DungeonHudService.java` | Enhanced data (room/wave progress, boss indicator) |
| `commands/DungeonAdminCommand.java` | New subcommands (tp, inspect, mob list/remove, import), facing mode, optional params |
| `commands/DungeonBrowseCommand.java` | Use new BrowserPage |
| `commands/DungeonStatusCommand.java` | Use new StatusPanel |

### Estimated scope:
- **New code:** ~1,500 lines across 20 new files
- **Modified code:** ~500 lines of changes across 12 existing files
- **Total delta:** ~2,000 lines
- **Build must pass:** `./gradlew build` with zero errors

---

## BUILD & TEST

```bash
cd plugin
export JAVA_HOME=/Users/misa/java/jdk-21.0.10+7/Contents/Home
./gradlew build
# Output: build/libs/DungeonPlugin-0.1.0.jar (or versioned)
```

### Pre-flight checks before submission:
1. `./gradlew build` succeeds
2. No new compile warnings (treat warnings as errors)
3. All existing commands still work (don't break backwards compat)
4. New JSON config format is backwards-compatible (new fields have defaults)
5. Manifest.json updated if version changed

---

## PRIORITY ORDER

If token/time budget is limited, implement in this order:
1. **Feature 5** — Room barriers + progression (core gameplay, most impactful)
2. **Feature 4** — Enhanced mob spawns (facing mode, optional params)
3. **Feature 6** — Loot system (rarity, weighted, boss loot)
4. **Feature 3** — Enhanced HUD + GUIs
5. **Feature 7** — Instance stats + reconnect
6. **Feature 8** — Debug visualization
7. **Feature 1** — World folder watching
8. **Feature 2** — Portal enhancements
9. **Feature 9** — API + RPGLeveling compat