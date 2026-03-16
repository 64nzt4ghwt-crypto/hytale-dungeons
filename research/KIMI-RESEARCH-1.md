# Hytale Modding API Research - Dungeon Plugin Improvements

**Research Date:** 2026-02-08  
**Researcher:** Kimi K2.5 (OpenClaw subagent)  
**Focus Areas:** Dungeon plugin enhancements for NPC AI, visual effects, sound, and API updates

---

## 1. LATEST HYTALE MODDING API DOCS/UPDATES

### Primary Documentation Source
- **Official Docs:** `https://hytaledocs.dev/` — Comprehensive API reference
- **GitHub Modding Org:** `https://github.com/Hytale-Modding/`
  - 25-star plugin template
  - Gradle plugin for mod development
  - Kotlin library bindings

### Recent API Discoveries (Feb 2025)

#### Core Architecture
Hytale Server is built on a sophisticated modular architecture combining:

| System | Classes | Description |
|--------|---------|-------------|
| Entity Component System (ECS) | ~100 | Game entity management |
| Event-Driven Architecture | 23 | Decoupled communication |
| Plugin System | 3 | Early bytecode transformation |
| Asset Registry | ~50 | Content management & hot-reloading |
| Network Protocol | 737 | Binary QUIC-based, 268 packet types |
| Builtin Modules | 1,757 | 43 gameplay modules |

#### Package Structure
```
com.hypixel.hytale/
├── assetstore/     # Asset loading, registries, packs (~50 classes)
├── builtin/        # 43 builtin gameplay modules (1,757 classes)
├── codec/          # Serialization framework (165 classes)
├── component/      # ECS implementation (~100 classes)
├── event/          # Event bus system (23 classes)
├── server/         # Server implementation (3,807 classes)
├── protocol/       # Network packets (737 classes)
├── procedurallib/  # Noise & procedural generation (257 classes)
└── plugin/         # Early plugin loader (3 classes)
```

#### Key Entry Points
- `HytaleServer` — Main server class
- `Universe` — World container singleton
- `EventBus` — Event system
- `ComponentRegistry` — ECS registry
- `CommandManager` — Command system
- `PacketRegistry` — Network packets

---

## 2. ADVANCED NPC AI BEHAVIORS

### Currently AvailableNPC System

#### NPCEntity Class
```java
// Core spawning pattern (confirmed working)
NPCEntity npc = new NPCEntity(world);
npc.setRoleName(roleName);  // e.g., "Skeleton_Fighter"
world.spawnEntity(npc, new Vector3d(x, y, z), new Vector3f(0, yaw, 0));
```

#### Role-Based AI System
NPCs use a **Role-based AI** system defined in JSON assets:

```
BuilderRole - defines NPC behavior from JSON assets
  ├── appearance (ModelAsset reference)
  ├── maxHealth
  ├── startState / defaultSubState (state machine)
  ├── collision parameters
  ├── combat support
  ├── movement controllers
  └── components (sensor, entity filter, timer, etc.)
```

#### Available NPC Methods
```java
// Role management
void setRoleName(String roleName)
Role getRole()
void setRole(Role role)
void setRoleIndex(int index)

// Appearance
static boolean setAppearance(Ref<EntityStore> ref, String modelName, ComponentAccessor<EntityStore> accessor)
void setAppearance(Ref<EntityStore> ref, ModelAsset model, ComponentAccessor<EntityStore> accessor)

// Animation
void playAnimation(Ref<EntityStore> ref, AnimationSlot slot, String animation, ComponentAccessor<EntityStore> accessor)

// Despawn management
void setToDespawn()                    // Mark for despawn
void setDespawnTime(float seconds)     // Set despawn timer
void setDespawning(boolean isDespawning)
boolean isDespawning()

// Position/Scaling
Vector3d getLeashPoint()
void setLeashPoint(Vector3d point)
void setInitialModelScale(float scale)

// Inventory
void setInventorySize(int a, int b, int c)
```

#### Role Names Available (from research)
Common role identifiers found:
- `Skeleton_Fighter` / `Skeleton_Archer` / `Skeleton_Knight`
- `Trork_Warrior` / `Trork_Shaman`
- `Golem` variants

**⚠️ Limitations Discovered:**
- **NO direct AI scripting** — AI is role-based via JSON assets
- **NO patrol path APIs** — Not exposed in current API
- **NO aggro radius control** — Defined in role config, not programmatically
- **NO leash programming** — Leash point exists but controlled by role state

#### Spawning System Architecture
```java
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
```

---

## 3. PARTICLE EFFECTS & VISUAL FEEDBACK

### Current State
**Hytale's particle system is NOT directly accessible via server-side plugins.**

**Why:** Particles are client-side rendered effects. Server plugins can only:
1. Trigger particles via **packets** (if particle packet types exist)
2. Use **client mods** to display effects based on custom packets

### What We CAN Do (Server-Side)

#### Event Titles (Big Screen Text)
```java
// Show dramatic screen messages
EventTitleUtil.showEventTitleToPlayer(
    playerRef,
    titleMsg,      // Main title
    subtitleMsg,   // Subtitle
    showAnimation  // Whether to animate
);
```

#### Custom UI HUD
```java
// Persistent dungeon status overlay
public class DungeonHud extends CustomUIHud {
    @Override
    protected void build(UICommandBuilder commands) {
        commands.set(".room-name", roomName)
                .set(".timer", timerText)
                .set(".mobs", mobsLeft + " remaining");
    }
}
```

#### Animated UI Pages
```java
// Boss room entrance screen
public class BossIntroPage extends InteractiveCustomUIPage<BossEventData> {
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, 
                     UIEventBuilder events, Store<EntityStore> store) {
        commands.set(".boss-name", bossName)
                .set(".intro-text", "The ancient evil approaches...")
                .set(".health-bar", 100);  // Visual bar
    }
}
```

#### Barrier Blocks for Visual Room Boundaries
```java
// Show room lock status
world.setBlock(new Vector3i(x,y,z), BARRIER_BLOCK);  // Locked
world.setBlock(new Vector3i(x,y,z), AIR_BLOCK);       // Unlocked
```

### Community Discovered Limitations
Based on Reddit research (`/r/hytale`):
- **"Turns out that the mods I wanted to make are impossible"** — Some server-side visual features unavailable
- Custom particles require **client-side modding**
- Server can only trigger existing particle effects via protocol packets (limited access)

---

## 4. SOUND/AUDIO INTEGRATION

### Current API Status
**Server-side sound control is EXTREMELY LIMITED.**

#### Confirmed Capabilities
1. **NO direct sound API** in server plugin framework
2. **NO music control** for dungeon ambiance
3. **NO ambient sound API**

#### Potential Workarounds

##### Option A: Client Mod + Custom Packets
```java
// Server: Send custom packet
player.sendPacket(new DungeonMusicPacket(
    "boss_battle_theme.ogg",
    MusicAction.PLAY,
    1.0f  // volume
));
```
- Requires players to install client-side mod
- Music handled by client mod receiving custom packets

##### Option B: Use Event Titles for Text-Based Drama
```java
// Dramatic text announcements as audio substitute
Message bossRoar = Message.raw("[The ground shakes with terrible footsteps...]")
    .color(DARK_RED)
    .bold(true)
    .italic(true);
Msg.broadcast(players, bossRoar);
```

### Sound System Architecture (Inferred)
Hytale uses a client-side audio engine:
- Audio assets loaded by client
- Server can only trigger via packets
- No server-side "SoundManager" or "AudioPlayer"

**⚠️ For dungeon music:** Would need **client mod** that:
1. Registers custom packet handler for music events
2. Has audio files packaged with mod
3. Plays music when server sends dungeon state changes

---

## 5. ECS (ENTITY COMPONENT SYSTEM) DEEP DIVE

### Component System
```java
// Define component class
public class MyData {
    public String value;
    public static final BuilderCodec<MyData> CODEC = BuilderCodec.builder(...)...build();
}

// Register in plugin start()
ComponentType<EntityStore, MyData> type = getEntityStoreRegistry().registerComponent(
    MyData.class, "MyData", MyData.CODEC
);

// Access on entity
Store<EntityStore> store = ...;
Ref<EntityStore> ref = ...;
MyData data = store.getComponent(ref, type);
```

### Event System
```java
EventRegistry events = getEventRegistry();

// Player ready (confirmed working)
events.registerGlobal(PlayerReadyEvent.class, event -> {
    Player player = event.getPlayer();
});

// Note: PlayerMoveEvent, EntityDeathEvent, BlockInteractEvent — may exist, unconfirmed
```

### System Registration (Per-Tick Logic)
```java
getEntityStoreRegistry().registerSystem(new DungeonTickSystem());

public final class DungeonTickSystem implements ISystem<EntityStore> {
    // Empty interface — actual tick logic handled elsewhere
}
// Alternative: Use DungeonTicker with manual scheduling
```

---

## 6. KEY FINDINGS FOR DUNGEON PLUGIN

### What's Already Implemented ✅
1. **Basic mob spawning** via `NPCEntity.setRoleName()`
2. **World teleportation** via `player.loadIntoWorld()` + `teleportPosition()`
3. **UI HUD** via `CustomUIHud` + `HudManager`
4. **Event titles** for dramatic announcements
5. **Entity tracking** via `world.getEntity(uuid)`
6. **NPC despawn** via `setToDespawn()` or `entity.remove()`

### What's IMPOSSIBLE/Not Exposed ❌
1. **Custom AI behaviors** (patrol, leashing, aggro radii) — Role-based only
2. **Particle effects** — Client-side only, no server API
3. **Sound/Music** — Client-side only, requires client mod
4. **Custom entity types** — Can only use existing role names

### What CAN Be Added with Current API 🔧
1. **Better wave management** — Use current spawn system with improved state machine
2. **Timed events** — Use `CompletableFuture` for delays
3. **Room state visual feedback** — Custom HUD with visual state indicators
4. **Boss entrance cinematics** — Event titles + UI pages
5. **Dynamic difficulty** — Adjust mob counts based on party size

---

## 7. RECOMMENDED IMPROVEMENTS (Current API Constraints)

### A. Enhanced Wave System
```java
// Add spawn sequences with delays
public class WaveSequence {
    List<SpawnBatch> batches;  // [{role, count, delayMs}, ...]
    
    // Spawn batch 1 immediately
    // Wait 2 seconds
    // Spawn batch 2
    // etc.
}
```

### B. State-Based Visual Feedback
```java
// Room states shown via HUD color coding
enum RoomState {
    LOCKED,        // Red HUD border
    IN_PROGRESS,   // Yellow with mob counter
    CLEARED,       // Green briefly
    BOSS_INCOMING  // Pulsing red
}
```

### C. Pre-Boss Dramatic Sequence
```java
// 3-second cinematic before boss room
// 1. Event title: "[The earth trembles...]"
// 2. HUD fade out
// 3. Wait 1 second
// 4. Event title: "BOSS NAME appears!"
// 5. Boss spawn
// 6. HUD return with boss health tracking
```

### D. Client-Sound Mod (Separate Project)
If audio is critical, create companion client mod:
```java
// Client mod receives custom packets
@EventHandler
public void onDungeonMusicPacket(DungeonMusicPacket packet) {
    SoundManager.play(packet.getTrack(), packet.getVolume());
}
```

---

## 8. COMMUNITY RESOURCES

### Active Communities
- **Hytale Discord:** `https://discord.gg/hytale` (official)
- **GitHub Modding Org:** `https://github.com/Hytale-Modding`
- **r/hytale subreddit** — Mod development discussions

### Documentation
- **hytaledocs.dev** — Most comprehensive API reference
- **API-SIGNATURES.md** — Exact method signatures from decompilation
- **HYTALE-CODE.md** — Plugin development quick reference

---

## CONCLUSION

The **DungeonPlugin** is well-architected for current API constraints. The main limitation is that **visual/sensory effects require client-side modifications**. For a server-side-only plugin:

1. ✅ **Keep:** Current mob spawning, room management, HUD system
2. ✅ **Enhance:** Wave sequences, state-based visual feedback, timed events
3. ❌ **Accept limitation:** Particles, custom AI, sound require client mod
4. 🔮 **Future opportunity:** Create companion client mod for immersive effects

The plugin can deliver a **fun, functional dungeon experience** with current APIs, but **true immersion** (particles, music, advanced AI) requires client-side modding beyond server plugin scope.

---

*Research compiled by Kimi K2.5 for Howl (Misa)*
