# Hytale Modding API Research - Dungeon Plugin Improvements

> **Research Date:** 2026-02-08  
> **Focus Areas:** Loot tables, World generation, Custom UI/HUD, Multiplayer sync  
> **Sources:** HytaleServer.jar decompilation, Assets.zip analysis, existing codebase examination

---

## Table of Contents

1. [Loot Table Systems](#1-loot-table-systems)
2. [World Generation Hooks](#2-world-generation-hooks)
3. [Custom UI/HUD System](#3-custom-uihud-system)
4. [Multiplayer State Synchronization](#4-multiplayer-state-synchronization)

---

## 1. Loot Table Systems

### 1.1 Hytale's JSON-Based Loot Configuration

Hytale uses a **declarative JSON system** for loot tables. The YUNG's dungeon example shows the structure at:
- `Server/Drops/Prefabs/Yungs_HyDungeons_Skeleton_Dungeon_*.json`

**Core Container Types:**

| Type | Description | Use Case |
|------|-------------|----------|
| `Single` | One specific item | Guaranteed drops |
| `Choice` | Weighted random selection from children | Common loot pools |
| `Multiple` | Rolls multiple sub-containers | Complex loot tables |
| `Empty` | No drop (for "no loot" outcomes) | Rarity control |

### 1.2 Loot Table JSON Structure

```json
{
  "Container": {
    "Type": "Multiple",
    "Containers": [
      {
        "Type": "Choice",
        "RollsMin": 2,
        "RollsMax": 4,
        "Containers": [
          { 
            "Type": "Single", 
            "Weight": 10, 
            "Item": { 
              "ItemId": "Ingredient_Fabric_Scrap_Linen", 
              "QuantityMin": 4, 
              "QuantityMax": 8 
            } 
          },
          { 
            "Type": "Single", 
            "Weight": 1,  // Rare item
            "Item": { 
              "ItemId": "Ingredient_Bar_Gold", 
              "QuantityMin": 1, 
              "QuantityMax": 2 
            } 
          },
          { "Type": "Empty", "Weight": 130 }  // No drop chance
        ]
      }
    ]
  }
}
```

### 1.3 Loot Scaling Patterns

**Current Implementation (YUNG's Dungeons):**
- Static loot tables per dungeon type
- Weight-based rarity (Weight: 1-130 range observed)
- Quantity ranges (QuantityMin/QuantityMax)

**API Research Findings - Dynamic Loot Scaling:**

Based on `INVENTORY-API.md` and role analysis, here's how to implement **runtime loot scaling**:

```java
// From decompiled code: NPC roles reference DropList IDs
// In role JSON: "DropList": "Drop_Skeleton_Fighter"

// To implement party scaling, we need to:
// 1. Create per-instance loot table variants
// 2. Modify quantities based on party size
// 3. Inject rare items based on difficulty

// Pattern: Dynamic DropList Selection
public class DungeonLootManager {
    
    // Base drop list ID from role config
    private static final String BASE_DROPLIST = "Drop_Skeleton_Fighter";
    
    // Generate scaled variant at runtime
    public String getScaledDropList(int partySize, int difficulty) {
        // Hytale doesn't support runtime DropList modification
        // Strategy: Pre-generate variants with different weights
        
        if (partySize >= 4 && difficulty >= 3) {
            return "Drop_Skeleton_Fighter_Hard_4P";  // More drops, better loot
        } else if (partySize >= 2) {
            return "Drop_Skeleton_Fighter_Medium_2P";
        }
        return BASE_DROPLIST;
    }
}
```

### 1.4 Rare Item Mechanics

**From NPC Role Analysis (`NPC-ROLES.md`):**

```json
// Role JSON structure shows damage/cooldown modifiers
"_InteractionVars": {
  "Melee_Damage": {
    "Interactions": [{
      "Parent": "NPC_Attack_Melee_Damage",
      "DamageCalculator": {
        "BaseDamage": { "Physical": 23 },
        "RandomPercentageModifier": 0.1  // ±10% variance
      }
    }]
  }
}
```

**Applying to Loot:**
- `RandomPercentageModifier` pattern could apply to loot quantities
- Boss roles (like `Goblin_Duke`) have multi-phase loot potential
- Dungeon variants (`Dungeon_Scarak_*`) suggest pre-tuned loot tables per difficulty

### 1.5 Programmatic Item Creation (API)

From `INVENTORY-API.md`:

```java
// Creating items with scaled quantities
ItemStack reward = new ItemStack(itemId, quantity);

// With metadata for dungeon tag
BsonDocument meta = new BsonDocument()
    .put("dungeon_source", new BsonString(instanceId))
    .put("difficulty", new BsonInt32(difficulty));

ItemStack taggedReward = new ItemStack(itemId, quantity, meta);

// Add to player
ItemContainer combined = player.getInventory().getCombinedHotbarFirst();
ItemStackTransaction tx = combined.addItemStack(taggedReward);

if (!tx.succeeded()) {
    ItemStack overflow = tx.getRemainder();
    // Handle overflow (drop on ground or stash for later)
}
```

### 1.6 Dungeon-Specific Loot Tables

**Recommended Implementation Strategy:**

```java
public class DungeonLootTable {
    
    // Instead of modifying DropList JSON at runtime,
    // use the Container pattern directly
    
    public List<ItemStack> generateLoot(String baseDropListId, 
                                         int playerCount,
                                         int difficulty,
                                         double luckBonus) {
        List<ItemStack> drops = new ArrayList<>();
        
        // Roll count scales with party size
        int rollCount = Math.min(1 + (playerCount / 2), 4);
        
        for (int i = 0; i < rollCount; i++) {
            // Weighted choice based on difficulty
            double roll = Math.random();
            double rareThreshold = 0.95 - (difficulty * 0.05) - luckBonus;
            
            if (roll > rareThreshold) {
                drops.add(createRareDrop(difficulty));
            } else {
                drops.add(createCommonDrop(difficulty));
            }
        }
        
        return drops;
    }
}
```

**Key Finding:** Hytale's DropList system is **static JSON** - you cannot dynamically modify weights at runtime. Solutions:
1. Pre-create variant DropLists for each difficulty/party size combo
2. Use code-based loot generation instead of DropList references
3. Post-process drops (multiply quantities based on party size)

---

## 2. World Generation Hooks

### 2.1 HytaleGenerator System

From `WorldStructures/Yungs_HyDungeons_Skeleton_Dungeon.json`:

```json
{
  "Type": "NoiseRange",
  "Biomes": [],
  "DefaultBiome": "Yungs_HyDungeons_Skeleton_Dungeon",
  "BiomeTransitions": [],
  "DefaultTransitionDistance": 128,
  "Density": {
    "Type": "Imported",
    "Name": "Biome-Map"
  },
  "ContentFields": [
    {
      "Type": "BaseHeight",
      "Name": "Base",
      "Y": 100
    },
    {
      "Type": "BaseHeight",
      "Name": "Water",
      "Y": 100
    }
  ]
}
```

### 2.2 Biome-Specific Dungeon Spawning

**HytaleGenerator Configuration:**

The `HytaleGenerator` system allows dungeons to register as **biome types**:

```json
// Server/HytaleGenerator/Biomes/Yungs_HyDungeons_Skeleton_Dungeon.json
{
  "Type": "NoiseRange",
  "Biomes": [
    "Zone2_Desert",      // Spawn in desert biomes
    "Zone2_Badlands"     // Also in badlands
  ],
  "DefaultBiome": "Yungs_HyDungeons_Skeleton_Dungeon",
  "Density": {
    "Type": "PerlinNoise",
    "Frequency": 0.01,
    "Threshold": 0.7  // Only spawn in 30% of eligible areas
  }
}
```

**Key Fields:**

| Field | Purpose |
|-------|---------|
| `Biomes` | List of biome IDs where dungeon can spawn |
| `DefaultBiome` | Biome ID for this dungeon (for transitions) |
| `Density` | Spawn probability/distribution |
| `ContentFields` | Y-level placement hints |

### 2.3 World Generation Hooks (API)

From `WORLD-TICK-API.md`:

```java
// WorldConfig controls world generation
WorldConfig config = new WorldConfig();

// Void world - for instances where we place structures manually
config.setWorldGenProvider(new VoidWorldGenProvider());

// Flat world - for flat dungeons
config.setWorldGenProvider(new FlatWorldGenProvider(layers));

// Normal terrain - for overworld-dungeons
config.setWorldGenProvider(null);  // default provider
```

### 2.4 Procedural Placement via InstancesPlugin

From `WORLD-TICK-API.md`: **Hytale has a built-in instance system!**

```java
public class InstancesPlugin extends JavaPlugin {
    
    // Clone an instance template world and start it
    public CompletableFuture<World> spawnInstance(
        String templateName,     // Source world to clone
        World returnWorld,       // Where to send players on exit
        Transform returnPoint    // Exit location
    );
    
    // Template management
    public static Path getInstanceAssetPath(String templateName);
    public static List<String> getInstanceAssets();
}
```

### 2.5 Placing Dungeons in Overworld

**Strategy for Open-World Dungeons (not instanced):**

```java
// Hook into world generation via structures
// HytaleGenerator supports custom structures:

// Server/HytaleGenerator/WorldStructures/DungeonEntrance.json
{
  "Type": "Scatter",
  "Biomes": ["Zone1_Plains", "Zone2_Desert"],
  "Structures": [
    {
      "Prefab": "Dungeon_Entrance_Skeleton",
      "Weight": 10,
      "MinCountPerZone": 2,
      "MaxCountPerZone": 5
    }
  ]
}
```

### 2.6 Prefab-Based Dungeon Construction

From `yungs-dungeons` example files:

```java
// Prefabs are at: Server/Prefabs/<Group>/<Dungeon>/<Type>/<Name>.prefab.json
// Example: Skeleton_Dungeon_Corner1_0.prefab.json

// Prefabs can be placed via:
// 1. HytaleGenerator during worldgen
// 2. Plugin code at runtime (for instances)
```

### 2.7 Recommended Dungeon Generation Strategy

| Type | Method | Use Case |
|------|--------|----------|
| **Instanced Dungeons** | `InstancesPlugin.spawnInstance()` | Party-based, private dungeons |
| **Open World Dungeons** | `HytaleGenerator` + Structures | Shared overworld dungeons |
| **Hybrid** | Entrance spawned via HytaleGenerator, interior via instance | Best of both worlds |

---

## 3. Custom UI/HUD System

### 3.1 Hytale UI Architecture

From `DungeonHud.java`, `DungeonListPage.java`, and decompiled API:

```
Hierarchy:
├── CustomUIPage (modal popup, dismissable)
│   └── DungeonListPage, DungeonStatusPage
│
└── CustomUIHud (persistent overlay)
    └── DungeonHud (always visible during dungeon)
```

### 3.2 CustomUIPage API

```java
public class DungeonListPage extends CustomUIPage {
    
    public DungeonListPage(Player player, PlayerRef playerRef, World world) {
        super(playerRef, CustomPageLifetime.CanDismiss);
    }
    
    @Override
    public void build(Ref<EntityStore> ref, 
                      UICommandBuilder ui, 
                      UIEventBuilder events, 
                      Store<EntityStore> store) {
        
        // Set UI elements
        ui.set("title", Message.raw("Dungeon Browser").color(Msg.GOLD));
        ui.clear("dungeonList");
        ui.append("dungeonList");
        ui.set("dungeonList[0].name", Message.raw("Ice Temple").color(Msg.AQUA));
        
        // Add event bindings
        events.addEventBinding(CustomUIEventBindingType.Activating, "entry_0");
        events.addEventBinding(CustomUIEventBindingType.Activating, "close_button");
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, 
                                Store<EntityStore> store, 
                                String data) {
        // Handle button clicks
        if ("close_button".equals(data)) {
            close();
        }
    }
}
```

### 3.3 CustomUIHud API

```java
public class DungeonHud extends CustomUIHud {
    
    @Override
    protected void build(UICommandBuilder ui) {
        // HUD elements persist and update
        ui.set("dungeonHud.visible", true);
        ui.set("dungeonHud.name", Message.raw(name).color(Msg.GOLD).bold(true));
        ui.set("dungeonHud.timer", Message.raw("5:00").color(Msg.RED));
    }
    
    public void refresh() {
        UICommandBuilder commands = new UICommandBuilder();
        build(commands);
        update(true, commands);  // Force refresh
    }
}
```

### 3.4 Opening UI Elements

```java
// Open modal page
DungeonListPage page = new DungeonListPage(player, playerRef, world);
player.getPageManager().openCustomPage(ref, store, page);

// Set persistent HUD
DungeonHud hud = new DungeonHud(playerRef, playerUuid, instanceId);
player.getHudManager().setCustomHud(playerRef, hud);

// Update HUD periodically (in tick system)
hud.refresh();
```

### 3.5 Message API (Colors & Formatting)

```java
Message msg = Message.raw("Dungeon Complete!")
    .color(java.awt.Color.GOLD)
    .bold(true)
    .italic(false);

// Predefined colors in Msg utility
java.awt.Color GOLD = new java.awt.Color(255, 215, 0);
java.awt.Color AQUA = new java.awt.Color(0, 255, 255);
java.awt.Color RED = new java.awt.Color(255, 0, 0);
java.awt.Color GREEN = new java.awt.Color(0, 255, 0);
```

### 3.6 Health Bars Over Mobs (Investigation)

**Finding:** No built-in "health bar over entity" API found in decompilation.

**Potential Implementations:**

1. **Native UI Extension:** Would require client mod (not server API)
2. **Nameplate Hack:** Modify entity nameplate to show health (limited length)
3. **Boss Bar:** Use built-in boss UI for major enemies only
4. **Custom Packet:** Send health data to client, custom renderer (client mod required)

**Server-Side Approach (Boss Only):**
```java
// For boss fights, use the built-in boss UI
// From NPC roles: Bosses like Goblin_Duke have special handling
// EntityStatMap tracks health - can be queried for boss UI updates

EntityStatMap stats = EntityStatsModule.get(entity);
EntityStatValue health = stats.get(DefaultEntityStatTypes.getHealth());
float hpPercent = health.asPercentage();

// Send to boss bar HUD (if Hytale has this)
// ui.set("bossBar.value", hpPercent);
```

### 3.7 Damage Numbers (Investigation)

**Finding:** Damage display appears to be **client-rendered** based on Damage event.

From `EVENTS-DAMAGE-API.md`:
```java
Damage damage = new Damage(source, cause, amount);
damage.getMetaStore();  // Can set IMPACT_PARTICLES, IMPACT_SOUND_EFFECT

// No explicit damage number control found
// Meta keys available:
// - HIT_LOCATION (Vector4d)
// - HIT_ANGLE (Float)
// - IMPACT_PARTICLES
// - IMPACT_SOUND_EFFECT
// - CAMERA_EFFECT
// - DEATH_ICON
```

**Conclusion:** Damage numbers are likely hardcoded client-side based on damage events. Server can influence via `Damage.Particles` metadata.

### 3.8 Minimap Integration (Investigation)

**Finding:** No server-side minimap API found.

Hytale's minimap is likely a **client-side feature** with server providing:
- Zone discovery events (`DiscoverZoneEvent`)
- Chunk visibility
- Points of interest via metadata

**Dungeon Minimap Strategy:**
```java
// Server cannot directly draw on minimap
// Can send zone data:

// When player enters dungeon
player.registerFeature(ClientFeature.MinimapOverride, true);

// Or send discovery events
// eventBus.dispatch(DiscoverZoneEvent.class);
```

### 3.9 UI Limitations Summary

| Feature | Server API Available | Notes |
|---------|---------------------|-------|
| Modal popups | ✅ Yes | `CustomUIPage` |
| Persistent HUD | ✅ Yes | `CustomUIHud` |
| Health bars over mobs | ❌ No | Client mod required |
| Damage numbers | ❌ No | Client-rendered, server can set particles |
| Minimap drawing | ❌ No | Server provides data, client renders |
| Boss bar | ⚠️ Partial | May exist for Dragon/Goblin_Duke |

---

## 4. Multiplayer State Synchronization

### 4.1 Core Architecture

From `WORLD-TICK-API.md` and decompiled code:

```
Multiplayer State Flow:
├── Universe (global, singleton)
│   ├── World (per world, own thread)
│   │   ├── EntityStore (ECS storage)
│   │   ├── ChunkStore (block data)
│   │   └── EventRegistry (per-world events)
│   │
│   └── Players (global registry)
│
├── InstancesPlugin (built-in)
│   └── Instance isolation via separate Worlds
│
└── Player data via ECS components
```

### 4.2 Instance Isolation

**Built-in Instance System:**

```java
// From WORLD-TICK-API.md
public class InstancesPlugin extends JavaPlugin {
    
    // Creates ISOLATED world from template
    public CompletableFuture<World> spawnInstance(
        String templateName,
        World returnWorld, 
        Transform returnPoint
    );
    
    // Per-instance return point (where players go on exit)
    public static void teleportPlayerToLoadingInstance(
        Ref<EntityStore> ref,
        ComponentAccessor<EntityStore> accessor,
        CompletableFuture<World> loadingWorld,
        Transform spawnPoint
    );
    
    // Exit cleanup
    public static void exitInstance(Ref<EntityStore> ref, 
                                     ComponentAccessor<EntityStore> accessor);
    
    // Safe removal (drains players first)
    public static void safeRemoveInstance(World world);
}
```

### 4.3 Dungeon Party Synchronization Pattern

From existing `DungeonPlugin.java` and analysis:

```java
public class DungeonInstance {
    private final UUID instanceId;
    private final World world;  // Isolated world
    private final Set<UUID> partyMembers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, InstanceEntityConfig> playerData = new ConcurrentHashMap<>();
    
    // State synchronized to all party members
    private volatile int currentRoom = 0;
    private volatile int remainingMobs = 0;
    private volatile boolean roomCleared = false;
    private volatile long timeRemaining;
    
    // Sync method - called on state change
    public void syncStateToParty() {
        for (UUID playerUuid : partyMembers) {
            Player player = getPlayer(playerUuid);
            if (player != null) {
                // Update their HUD
                updatePlayerHud(player);
                // Send any custom packets if needed
            }
        }
    }
}
```

### 4.4 ECS Component Storage for Dungeon Data

```java
// From DungeonPlugin.java
public class DungeonData implements Component<EntityStore> {
    private UUID currentInstanceId;
    private String dungeonId;
    private int currentRoom;
    private long enterTime;
    
    public static final BuilderCodec<DungeonData> CODEC = 
        MapBuilderCodec.builder(...)
            .field(...)
            .build();
}

// Registration
dungeonDataType = getEntityStoreRegistry().registerComponent(
    DungeonData.class,
    "DungeonData",
    DungeonData.CODEC
);
```

### 4.5 Cross-World Player Tracking

From `Universe` class:

```java
public class Universe extends JavaPlugin {
    // Global player access
    public List<PlayerRef> getPlayers();
    public PlayerRef getPlayer(UUID uuid);
    
    // Move all players from one world to another
    public CompletableFuture<Void> drainPlayersTo(World targetWorld);
}

// Per-world player list
public class World extends TickingThread {
    public List<Player> getPlayers();
    public int getPlayerCount();
    public CompletableFuture<PlayerRef> addPlayer(PlayerRef ref, Transform spawn);
}
```

### 4.6 State Sync via HUD Updates

From `DungeonHud.java`:

```java
// Each player's HUD is independent but shows shared state
public class DungeonHud extends CustomUIHud {
    
    @Override
    protected void build(UICommandBuilder ui) {
        DungeonInstance inst = dungeonManager.getInstance(instanceId);
        
        // All party members see same data from DungeonInstance
        ui.set("dungeonHud.room", 
            Message.raw(currentRoom + "/" + totalRooms));
        
        ui.set("dungeonHud.mobs", 
            Message.raw(remaining + "/" + total));
        
        ui.set("dungeonHud.timer", 
            Message.raw(formatTime(inst.getRemainingSeconds())));
        
        ui.set("dungeonHud.party", 
            Message.raw(inst.getPlayerCount() + " players"));
    }
}

// Tick system calls refresh() on each player's HUD
// State appears synchronized because all read from same DungeonInstance
```

### 4.7 Multiplayer Event Handling

```java
// From DungeonEventListener pattern
public class DungeonEventListener {
    
    // Player disconnect - remove from party
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        UUID playerUuid = event.getPlayer().getUuid();
        DungeonInstance instance = manager.getInstanceForPlayer(playerUuid);
        if (instance != null) {
            instance.removePlayer(playerUuid);
            // Check if party is now empty
            if (instance.getPlayerCount() == 0) {
                manager.scheduleInstanceCleanup(instance);
            }
        }
    }
    
    // Entity death - update mob count
    public void onEntityRemove(EntityRemoveEvent event) {
        Entity entity = event.getEntity();
        // Check if it's a tracked dungeon mob
        // Update instance remaining count
    }
}
```

### 4.8 Sync Patterns Summary

| Pattern | Implementation | Use Case |
|---------|---------------|----------|
| **Shared World + ECS** | `DungeonInstance` tracks state, `DungeonHud` reads from it | Room progress, mob counts, timer |
| **Instance Isolation** | `InstancesPlugin.spawnInstance()` | Prevent cross-dungeon interference |
| **Component Persistence** | `DungeonData` component on player | Track which dungeon player is in |
| **Event Broadcasting** | `EventRegistry.registerGlobal()` | React to deaths, disconnects |
| **Per-World Events** | `world.getEventRegistry()` | Dungeon-specific event handling |

### 4.9 State Isolation Guarantees

```java
// InstancesPlugin provides:

// 1. World-level isolation
World instanceWorld = InstancesPlugin.get().spawnInstance("template", ...);
// This is a SEPARATE World with its own:
// - EntityStore (no entity ID conflicts with overworld)
// - ChunkStore (separate chunk storage)
// - EventRegistry (events don't leak)

// 2. Automatic cleanup
InstanceWorldConfig config = InstanceWorldConfig.ensureAndGet(worldConfig);
config.setRemovalConditions(
    new WorldEmptyCondition(30),  // Remove 30s after last player leaves
    new TimeoutCondition(3600)    // Or after 1 hour max
);

// 3. Return point safety
WorldReturnPoint returnPoint = new WorldReturnPoint(
    overworldUuid,
    returnTransform,
    true  // Return on reconnect if server crashes
);
```

---

## 5. Implementation Recommendations

### 5.1 Loot System

**Implementation:**
```java
// Create pre-tuned DropList variants for party sizes
// Server/Drops/Dungeon_Skeleton_1P.json (single player)
// Server/Drops/Dungeon_Skeleton_2P.json (duo - +50% quantity)
// Server/Drops/Dungeon_Skeleton_4P.json (full party - +100% quantity)

public String selectDropList(String base, int partySize) {
    return base + "_" + Math.min(partySize, 4) + "P";
}
```

### 5.2 World Generation

**Hybrid Approach:**
1. Use `HytaleGenerator` to place dungeon entrances in overworld
2. Use `InstancesPlugin` for dungeon interiors
3. Portal at entrance teleports to instance

### 5.3 UI

**Server-Side HUD:**
- Use `CustomUIHud` for persistent dungeon status
- Use `CustomUIPage` for modals (browser, confirmation)
- Don't attempt health bars over mobs (requires client mod)

### 5.4 Multiplayer Sync

**Architecture:**
```
PlayerA ──┐
PlayerB ──┼──> DungeonInstance (state holder) ──> CustomUIHud
PlayerC ──┘         │
                    └──> World (isolated instance)
                         └──> ECS Entities
```

---

## 6. Open Questions for Further Research

1. **Loot:** Can we hot-reload DropList JSON at runtime? Or must restart?
2. **WorldGen:** Can we generate prefab layouts procedurally instead of placing pre-builts?
3. **UI:** Is there a boss health bar API for major encounters?
4. **Sync:** What happens if two players in same instance get out of sync (lag)?
5. **Persistence:** How do we save dungeon instance state for server restart recovery?

---

*Research compiled by Kimi subagent for Dungeon Plugin improvements.*
*Sources: HytaleServer.jar decompilation, Assets.zip, existing codebase, hytaledocs.dev*
