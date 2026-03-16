# Hytale Server API - Exact Method Signatures

> Decompiled from `plugin/libs/HytaleServer.jar` using `javap -p` on 2026-02-07.
> All signatures are **exact** — use these to fix compile errors.

---

## Table of Contents

1. [CustomUIPage (abstract)](#1-customuipage)
2. [InteractiveCustomUIPage\<T\> (abstract)](#2-interactivecustomuipaget)
3. [BasicCustomUIPage (abstract)](#3-basiccustomuipage)
4. [World](#4-world)
5. [Entity](#5-entity)
6. [NPCEntity](#6-npcentity)
7. [Player](#7-player)
8. [PageManager](#8-pagemanager)
9. [UICommandBuilder](#9-uicommandbuilder)
10. [UIEventBuilder](#10-uieventbuilder)
11. [CustomPageLifetime (enum)](#11-custompagelifetime)
12. [CustomUIEventBindingType (enum)](#12-customuieventbindingtype)
13. [Supporting Types](#13-supporting-types)

---

## 1. CustomUIPage

**Package:** `com.hypixel.hytale.server.core.entity.entities.player.pages`

```java
public abstract class CustomUIPage {

    // Fields
    protected final PlayerRef playerRef;
    protected CustomPageLifetime lifetime;

    // Constructor
    public CustomUIPage(PlayerRef playerRef, CustomPageLifetime lifetime);

    // Getters/Setters
    public void setLifetime(CustomPageLifetime lifetime);
    public CustomPageLifetime getLifetime();

    // === CORE BUILD METHOD (abstract) ===
    public abstract void build(
        Ref<EntityStore> ref,
        UICommandBuilder commands,
        UIEventBuilder events,
        Store<EntityStore> store
    );

    // === EVENT HANDLING ===
    // NOTE: Takes a raw String, NOT a typed event object
    public void handleDataEvent(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        String data        // <-- raw JSON string
    );

    // Lifecycle
    protected void rebuild();
    protected void sendUpdate();
    protected void sendUpdate(UICommandBuilder commands);
    protected void sendUpdate(UICommandBuilder commands, boolean clear);
    protected void close();
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store);
}
```

**Key imports:**
```java
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
```

---

## 2. InteractiveCustomUIPage\<T\>

**Package:** `com.hypixel.hytale.server.core.entity.entities.player.pages`

```java
public abstract class InteractiveCustomUIPage<T> extends CustomUIPage {

    // Fields
    protected final BuilderCodec<T> eventDataCodec;

    // Constructor — requires a BuilderCodec for deserializing event data
    public InteractiveCustomUIPage(
        PlayerRef playerRef,
        CustomPageLifetime lifetime,
        BuilderCodec<T> eventDataCodec
    );

    // === TYPED EVENT HANDLING ===
    // Called with the deserialized typed data
    public void handleDataEvent(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        T data              // <-- typed event data (deserialized from JSON via codec)
    );

    // Also overrides the String version (deserializes then calls typed version)
    @Override
    public void handleDataEvent(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        String data
    );

    // Extended sendUpdate with event builder
    protected void sendUpdate(
        UICommandBuilder commands,
        UIEventBuilder events,
        boolean clear
    );

    // Inherits abstract build() from CustomUIPage
}
```

**Key additional import:**
```java
import com.hypixel.hytale.codec.builder.BuilderCodec;
```

### Usage Pattern (from RespawnPage example):
```java
public class RespawnPage extends InteractiveCustomUIPage<RespawnPage.RespawnPageEventData> {
    public RespawnPage(PlayerRef playerRef, ...) {
        super(playerRef, CustomPageLifetime.CantClose, RespawnPageEventData.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        // Build UI
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, RespawnPageEventData data) {
        // Handle typed event
    }
}
```

---

## 3. BasicCustomUIPage

**Package:** `com.hypixel.hytale.server.core.entity.entities.player.pages`

```java
public abstract class BasicCustomUIPage extends CustomUIPage {

    // Constructor
    public BasicCustomUIPage(PlayerRef playerRef, CustomPageLifetime lifetime);

    // Implements the 4-arg build by delegating to simplified 1-arg version
    @Override
    public void build(
        Ref<EntityStore> ref,
        UICommandBuilder commands,
        UIEventBuilder events,
        Store<EntityStore> store
    );

    // === SIMPLIFIED BUILD — just commands, no events ===
    public abstract void build(UICommandBuilder commands);
}
```

**Use this when:** You only need to set UI data and don't need event bindings.

---

## 4. World

**Package:** `com.hypixel.hytale.server.core.universe.world`

### Key Methods:

```java
public class World extends TickingThread implements Executor, ... {

    // === GET ENTITY BY UUID ===
    // YES, this exists! Returns Entity directly (or null)
    public Entity getEntity(UUID uuid);

    // Also available: get a Ref instead
    public Ref<EntityStore> getEntityRef(UUID uuid);

    // === SPAWN ENTITY ===
    public <T extends Entity> T spawnEntity(
        T entity,                  // pre-constructed entity instance
        Vector3d position,         // com.hypixel.hytale.math.vector.Vector3d
        Vector3f rotation          // com.hypixel.hytale.math.vector.Vector3f
    );

    // Lower-level add with reason
    public <T extends Entity> T addEntity(
        T entity,
        Vector3d position,
        Vector3f rotation,
        AddReason reason           // com.hypixel.hytale.component.AddReason
    );

    // === PLAYERS ===
    public List<Player> getPlayers();
    public int getPlayerCount();
    public Collection<PlayerRef> getPlayerRefs();

    // === WORLD INFO ===
    public String getName();
    public boolean isAlive();
    public WorldConfig getWorldConfig();
    public EntityStore getEntityStore();
    public long getTick();

    // === TASK EXECUTION (world thread) ===
    public void execute(Runnable task);
}
```

**Key imports:**
```java
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.component.AddReason;
```

---

## 5. Entity

**Package:** `com.hypixel.hytale.server.core.entity`

```java
public abstract class Entity implements Component<EntityStore> {

    // Fields
    protected int networkId;
    protected UUID legacyUuid;
    protected World world;
    protected Ref<EntityStore> reference;

    // Constructors
    public Entity(World world);
    public Entity();

    // === REMOVAL — THIS IS HOW YOU REMOVE ENTITIES ===
    public boolean remove();    // <-- returns boolean

    // Getters
    public int getNetworkId();
    public UUID getUuid();
    public World getWorld();
    public boolean wasRemoved();
    public Ref<EntityStore> getReference();

    // Movement
    public void moveTo(Ref<EntityStore> ref, double x, double y, double z, ComponentAccessor<EntityStore> accessor);

    // World lifecycle
    public void loadIntoWorld(World world);
    public void unloadFromWorld();
}
```

### ⚠️ Entity Removal Summary

There are **multiple ways** to remove/despawn entities:

| Method | Class | Description |
|--------|-------|-------------|
| `entity.remove()` | `Entity` | **Immediate removal** from the world. Returns boolean. |
| `npcEntity.setToDespawn()` | `NPCEntity` | Marks NPC for despawn (may play animation). No params. |
| `npcEntity.setDespawnTime(float)` | `NPCEntity` | Sets despawn timer in seconds. |
| `npcEntity.setDespawning(boolean)` | `NPCEntity` | Sets despawn flag directly. |
| `DespawnComponent` | ECS component | Add to entity for timed despawn via ECS system. |

**For immediate removal:** `entity.remove()`
**For NPC despawn with animation:** `npcEntity.setToDespawn()`

---

## 6. NPCEntity

**Package:** `com.hypixel.hytale.server.npc.entities`

```java
public class NPCEntity extends LivingEntity implements INonPlayerCharacter {

    // Constructors
    public NPCEntity();
    public NPCEntity(World world);

    // === ROLE NAME ===
    public String getRoleName();
    public void setRoleName(String roleName);

    // === DESPAWN METHODS ===
    public void setToDespawn();                           // no params — marks for despawn
    public void setDespawnTime(float seconds);            // set despawn timer
    public double getDespawnTime();
    public void setDespawning(boolean isDespawning);
    public boolean isDespawning();
    public void setDespawnRemainingSeconds(float seconds);
    public boolean tickDespawnRemainingSeconds(float delta);
    public void setPlayingDespawnAnim(boolean playing);
    public boolean isPlayingDespawnAnim();
    public void setDespawnAnimationRemainingSeconds(float seconds);
    public boolean tickDespawnAnimationRemainingSeconds(float delta);
    public void setDespawnCheckRemainingSeconds(float seconds);
    public boolean tickDespawnCheckRemainingSeconds(float delta);

    // Role
    public Role getRole();
    public void setRole(Role role);
    public int getRoleIndex();
    public void setRoleIndex(int index);

    // Appearance
    public static boolean setAppearance(Ref<EntityStore> ref, String modelName, ComponentAccessor<EntityStore> accessor);
    public void setAppearance(Ref<EntityStore> ref, ModelAsset model, ComponentAccessor<EntityStore> accessor);

    // NPC Type info (from INonPlayerCharacter)
    public String getNPCTypeId();
    public int getNPCTypeIndex();

    // Animation
    public void playAnimation(Ref<EntityStore> ref, AnimationSlot slot, String animation, ComponentAccessor<EntityStore> accessor);

    // Leash/Position
    public Vector3d getLeashPoint();
    public void setLeashPoint(Vector3d point);
    public void setInitialModelScale(float scale);

    // Inventory
    public void setInventorySize(int a, int b, int c);

    // Component type registration
    public static ComponentType<EntityStore, NPCEntity> getComponentType();

    // Reservations (for quest/interaction systems)
    public void addReservation(UUID uuid);
    public void removeReservation(UUID uuid);
    public boolean isReserved();
    public boolean isReservedBy(UUID uuid);
}
```

**Inheritance chain:** `NPCEntity → LivingEntity → Entity`

---

## 7. Player

**Package:** `com.hypixel.hytale.server.core.entity.entities`

```java
public class Player extends LivingEntity implements CommandSender, PermissionHolder, MetricProvider {

    // === PAGE MANAGER ===
    public PageManager getPageManager();

    // === HUD MANAGER ===
    public HudManager getHudManager();

    // === WINDOW MANAGER ===
    public WindowManager getWindowManager();

    // Other useful methods
    public PlayerRef getPlayerRef();
    public PacketHandler getPlayerConnection();
    public GameMode getGameMode();
    public String getDisplayName();
    public boolean hasPermission(String permission);
    public boolean hasPermission(String permission, boolean defaultValue);
    public void sendMessage(Message message);

    // Static methods
    public static ComponentType<EntityStore, Player> getComponentType();
    public static void setGameMode(Ref<EntityStore> ref, GameMode mode, ComponentAccessor<EntityStore> accessor);
}
```

---

## 8. PageManager

**Package:** `com.hypixel.hytale.server.core.entity.entities.player.pages`

```java
public class PageManager {

    public PageManager();
    public void init(PlayerRef playerRef, WindowManager windowManager);

    // === GET CURRENT PAGE ===
    public CustomUIPage getCustomPage();

    // === OPEN CUSTOM PAGE ===
    public void openCustomPage(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        CustomUIPage page
    );

    // Open custom page WITH inventory windows
    public boolean openCustomPageWithWindows(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        CustomUIPage page,
        Window... windows
    );

    // Set built-in page
    public void setPage(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        Page page
    );

    public void setPage(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        Page page,
        boolean force
    );

    // Set built-in page with windows
    public boolean setPageWithWindows(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        Page page,
        boolean force,
        Window... windows
    );

    // Update page packet
    public void updateCustomPage(CustomPage customPage);

    // Handle incoming event from client
    public void handleEvent(
        Ref<EntityStore> ref,
        Store<EntityStore> store,
        CustomPageEvent event
    );
}
```

**⚠️ IMPORTANT:** `openCustomPage` needs `Ref<EntityStore>` and `Store<EntityStore>`, NOT just the page. You must obtain these from the ECS context.

---

## 9. UICommandBuilder

**Package:** `com.hypixel.hytale.server.core.ui.builder`

```java
public class UICommandBuilder {

    public UICommandBuilder();

    // === SET (multiple overloads) ===
    public UICommandBuilder set(String selector, String value);
    public UICommandBuilder set(String selector, boolean value);
    public UICommandBuilder set(String selector, float value);
    public UICommandBuilder set(String selector, int value);
    public UICommandBuilder set(String selector, double value);
    public UICommandBuilder set(String selector, Message value);     // styled text
    public <T> UICommandBuilder set(String selector, Value<T> value);
    public <T> UICommandBuilder set(String selector, T[] value);
    public <T> UICommandBuilder set(String selector, List<T> value);
    public UICommandBuilder setNull(String selector);
    public UICommandBuilder setObject(String selector, Object value);

    // === APPEND (add child elements) ===
    public UICommandBuilder append(String selector);                    // append empty
    public UICommandBuilder append(String selector, String data);       // append with BSON data
    public UICommandBuilder appendInline(String selector, String data);

    // === INSERT ===
    public UICommandBuilder insertBefore(String selector, String data);
    public UICommandBuilder insertBeforeInline(String selector, String data);

    // === REMOVE & CLEAR ===
    public UICommandBuilder clear(String selector);
    public UICommandBuilder remove(String selector);

    // === GET RESULT ===
    public CustomUICommand[] getCommands();

    // Static
    public static final CustomUICommand[] EMPTY_COMMAND_ARRAY;
}
```

---

## 10. UIEventBuilder

**Package:** `com.hypixel.hytale.server.core.ui.builder`

```java
public class UIEventBuilder {

    public UIEventBuilder();

    // === ADD EVENT BINDING (4 overloads) ===
    public UIEventBuilder addEventBinding(
        CustomUIEventBindingType type,
        String selector
    );

    public UIEventBuilder addEventBinding(
        CustomUIEventBindingType type,
        String selector,
        boolean locksInterface
    );

    public UIEventBuilder addEventBinding(
        CustomUIEventBindingType type,
        String selector,
        EventData eventData
    );

    public UIEventBuilder addEventBinding(
        CustomUIEventBindingType type,
        String selector,
        EventData eventData,
        boolean locksInterface
    );

    // === GET RESULT ===
    public CustomUIEventBinding[] getEvents();

    // Static
    public static final CustomUIEventBinding[] EMPTY_EVENT_BINDING_ARRAY;
}
```

### EventData

```java
// Record class
public final record EventData(Map<String, String> events) {

    public EventData();                                          // empty
    public EventData(Map<String, String> events);                // from map

    // Fluent builder methods
    public EventData append(String key, String value);
    public <T extends Enum<T>> EventData append(String key, T value);
    public EventData put(String key, String value);

    // Static factory
    public static EventData of(String key, String value);
}
```

---

## 11. CustomPageLifetime

**Package:** `com.hypixel.hytale.protocol.packets.interface_`

```java
public enum CustomPageLifetime {
    CantClose,                          // Player cannot close the page
    CanDismiss,                         // Player can dismiss/close
    CanDismissOrCloseThroughInteraction  // Can dismiss or close via interaction
}
```

---

## 12. CustomUIEventBindingType

**Package:** `com.hypixel.hytale.protocol.packets.interface_`

```java
public enum CustomUIEventBindingType {
    Activating,
    RightClicking,
    DoubleClicking,
    MouseEntered,
    MouseExited,
    ValueChanged,
    ElementReordered,
    Validating,
    Dismissing,
    FocusGained,
    FocusLost,
    KeyDown,
    MouseButtonReleased,
    SlotClicking,
    SlotDoubleClicking,
    SlotMouseEntered,
    SlotMouseExited,
    DragCancelled,
    Dropped,
    SlotMouseDragCompleted,
    SlotMouseDragExited,
    SlotClickReleaseWhileDragging,
    SlotClickPressWhileDragging,
    SelectedTabChanged
}
```

---

## 13. Supporting Types

### CustomUICommandType (enum)

```java
public enum CustomUICommandType {
    Append,
    AppendInline,
    InsertBefore,
    InsertBeforeInline,
    Remove,
    Set,
    Clear
}
```

### CustomPageEventType (enum)

```java
public enum CustomPageEventType {
    Acknowledge,
    Data,
    Dismiss
}
```

### CustomPage (protocol packet)

```java
// The actual packet sent to the client
public class CustomPage implements Packet {
    public String key;
    public boolean isInitial;
    public boolean clear;
    public CustomPageLifetime lifetime;
    public CustomUICommand[] commands;
    public CustomUIEventBinding[] eventBindings;

    public CustomPage(String key, boolean isInitial, boolean clear,
                     CustomPageLifetime lifetime,
                     CustomUICommand[] commands,
                     CustomUIEventBinding[] eventBindings);
}
```

### CustomUIHud (abstract)

```java
// For persistent HUD overlays (separate from pages)
public abstract class CustomUIHud {
    public CustomUIHud(PlayerRef playerRef);
    public void show();
    public void update(boolean force, UICommandBuilder commands);
    public PlayerRef getPlayerRef();
    protected abstract void build(UICommandBuilder commands);
}
```

### HudManager

```java
public class HudManager {
    public CustomUIHud getCustomHud();
    public void setCustomHud(PlayerRef ref, CustomUIHud hud);
    public void resetHud(PlayerRef ref);
    // Visibility control
    public void showHudComponents(PlayerRef ref, HudComponent... components);
    public void hideHudComponents(PlayerRef ref, HudComponent... components);
}
```

### DespawnComponent (ECS)

```java
// For timed despawn via ECS system
public class DespawnComponent implements Component<EntityStore> {
    public DespawnComponent();
    public DespawnComponent(Instant timeToDespawnAt);
    public void setDespawn(Instant time);
    public Instant getDespawn();

    // Factory methods
    public static DespawnComponent despawnInSeconds(TimeResource time, int seconds);
    public static DespawnComponent despawnInSeconds(TimeResource time, float seconds);
    public static DespawnComponent despawnInMilliseconds(TimeResource time, long millis);
}
```

---

## Quick Reference: Common Patterns

### Opening a Custom UI Page
```java
Player player = ...;
PlayerRef playerRef = player.getPlayerRef();
Ref<EntityStore> ref = playerRef.getReference();
Store<EntityStore> store = player.getWorld().getEntityStore();

CustomUIPage page = new MyCustomPage(playerRef, CustomPageLifetime.CanDismiss);
player.getPageManager().openCustomPage(ref, store, page);
```

### Spawning an NPC Entity
```java
World world = player.getWorld();
NPCEntity npc = new NPCEntity(world);
npc.setRoleName("dungeon_keeper");
world.spawnEntity(npc, new Vector3d(x, y, z), new Vector3f(0, yaw, 0));
```

### Removing an Entity
```java
// Immediate removal:
entity.remove();

// NPC despawn (may play animation):
npcEntity.setToDespawn();
```

### Getting an Entity by UUID
```java
Entity entity = world.getEntity(uuid);  // returns Entity or null
```

---

## Package Summary

| Class | Full Package |
|-------|-------------|
| `CustomUIPage` | `com.hypixel.hytale.server.core.entity.entities.player.pages` |
| `InteractiveCustomUIPage<T>` | `com.hypixel.hytale.server.core.entity.entities.player.pages` |
| `BasicCustomUIPage` | `com.hypixel.hytale.server.core.entity.entities.player.pages` |
| `PageManager` | `com.hypixel.hytale.server.core.entity.entities.player.pages` |
| `UICommandBuilder` | `com.hypixel.hytale.server.core.ui.builder` |
| `UIEventBuilder` | `com.hypixel.hytale.server.core.ui.builder` |
| `EventData` | `com.hypixel.hytale.server.core.ui.builder` |
| `CustomPageLifetime` | `com.hypixel.hytale.protocol.packets.interface_` |
| `CustomUIEventBindingType` | `com.hypixel.hytale.protocol.packets.interface_` |
| `CustomUICommandType` | `com.hypixel.hytale.protocol.packets.interface_` |
| `CustomPageEventType` | `com.hypixel.hytale.protocol.packets.interface_` |
| `CustomPage` | `com.hypixel.hytale.protocol.packets.interface_` |
| `World` | `com.hypixel.hytale.server.core.universe.world` |
| `Entity` | `com.hypixel.hytale.server.core.entity` |
| `LivingEntity` | `com.hypixel.hytale.server.core.entity` |
| `Player` | `com.hypixel.hytale.server.core.entity.entities` |
| `NPCEntity` | `com.hypixel.hytale.server.npc.entities` |
| `PlayerRef` | `com.hypixel.hytale.server.core.universe` |
| `CustomUIHud` | `com.hypixel.hytale.server.core.entity.entities.player.hud` |
| `HudManager` | `com.hypixel.hytale.server.core.entity.entities.player.hud` |
| `DespawnComponent` | `com.hypixel.hytale.server.core.modules.entity` |
| `Ref<EntityStore>` | `com.hypixel.hytale.component` |
| `Store<EntityStore>` | `com.hypixel.hytale.component` |
| `EntityStore` | `com.hypixel.hytale.server.core.universe.world.storage` |
| `Vector3d` | `com.hypixel.hytale.math.vector` |
| `Vector3f` | `com.hypixel.hytale.math.vector` |
| `BuilderCodec<T>` | `com.hypixel.hytale.codec.builder` |
