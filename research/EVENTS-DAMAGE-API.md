# Hytale Events, Damage & Death API Research

> Decompiled from `HytaleServer.jar` (build-7) using `javap -p` on 2026-02-07.
> All method signatures are **exact**.

---

## Table of Contents

1. [Event Bus System (Global Events)](#1-event-bus-system-global-events)
2. [ECS Event System (Per-Entity Events)](#2-ecs-event-system-per-entity-events)
3. [How a Plugin Subscribes to Events](#3-how-a-plugin-subscribes-to-events)
4. [Available Game Events (Full Catalog)](#4-available-game-events-full-catalog)
5. [Damage System](#5-damage-system)
6. [Death System](#6-death-system)
7. [Entity Health / Stats System](#7-entity-health--stats-system)
8. [Death Detection Strategies for Dungeons](#8-death-detection-strategies-for-dungeons)

---

## 1. Event Bus System (Global Events)

Hytale has a **dual event system**: a global `EventBus` for lifecycle/player/world events, and an ECS `EntityEventSystem` for per-entity events (like damage).

### EventBus Hierarchy

```
com.hypixel.hytale.event.IBaseEvent<KeyType>       ← marker interface
├── IEvent<KeyType>                                 ← synchronous events
└── IAsyncEvent<KeyType>                            ← async (CompletableFuture-based)

com.hypixel.hytale.event.ICancellable               ← for cancellable events
  boolean isCancelled()
  void setCancelled(boolean)
```

### EventBus (`com.hypixel.hytale.event.EventBus`)

The central EventBus lives on `HytaleServer`:

```java
// Access:
HytaleServer.get().getEventBus()   // → EventBus

// EventBus implements IEventBus which extends IEventRegistry
```

#### Key Registration Methods

```java
// === Simple registration (Void key = global) ===
<EventType extends IBaseEvent<Void>> EventRegistration<Void, EventType> 
    register(Class<? super EventType>, Consumer<EventType>)

// === With priority ===
<EventType extends IBaseEvent<Void>> EventRegistration<Void, EventType> 
    register(EventPriority, Class<? super EventType>, Consumer<EventType>)

// === With raw priority value ===
<EventType extends IBaseEvent<Void>> EventRegistration<Void, EventType> 
    register(short, Class<? super EventType>, Consumer<EventType>)

// === Keyed registration (event has a key type) ===
<KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> 
    register(Class<? super EventType>, KeyType, Consumer<EventType>)

// === Global listener (fires for ALL keys) ===
<KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> 
    registerGlobal(Class<? super EventType>, Consumer<EventType>)

// === Unhandled listener (fires only if no key-specific handler matched) ===
<KeyType, EventType extends IBaseEvent<KeyType>> EventRegistration<KeyType, EventType> 
    registerUnhandled(Class<? super EventType>, Consumer<EventType>)

// === Async variants ===
<EventType extends IAsyncEvent<Void>> EventRegistration<Void, EventType>
    registerAsync(Class<? super EventType>, Function<CompletableFuture<EventType>, CompletableFuture<EventType>>)
// (all register variants have async counterparts)
```

#### Dispatching

```java
// From IEventBus:
<KeyType, EventType extends IEvent<KeyType>> EventType 
    dispatch(Class<EventType>)                           // dispatch no-arg event

<KeyType, EventType extends IEvent<KeyType>> IEventDispatcher<EventType, EventType> 
    dispatchFor(Class<? super EventType>, KeyType)       // get dispatcher for key

<KeyType, EventType extends IAsyncEvent<KeyType>> IEventDispatcher<EventType, CompletableFuture<EventType>> 
    dispatchForAsync(Class<? super EventType>, KeyType)  // async dispatch
```

### EventPriority (`com.hypixel.hytale.event.EventPriority`)

```java
public enum EventPriority {
    FIRST,      // runs first
    EARLY,
    NORMAL,     // default
    LATE,
    LAST        // runs last
}
// Each has a short getValue()
```

### EventRegistration (`com.hypixel.hytale.event.EventRegistration`)

```java
// Extends Registration (has enable/disable)
// Returned from register() calls - keep it to unregister later
public class EventRegistration<KeyType, EventType extends IBaseEvent<KeyType>> {
    Class<EventType> getEventClass();
    // Inherited: enable(), disable(), isEnabled()

    // Combine multiple registrations
    static <K, E> EventRegistration<K, E> combine(EventRegistration<K, E>, EventRegistration<K, E>...)
}
```

### IEventDispatcher (`com.hypixel.hytale.event.IEventDispatcher`)

```java
public interface IEventDispatcher<EventType extends IBaseEvent, ReturnType> {
    default boolean hasListener();       // check if anyone is listening
    ReturnType dispatch(EventType);      // fire the event
}
```

---

## 2. ECS Event System (Per-Entity Events)

Damage is NOT a global EventBus event. It's an **ECS entity event** dispatched through the Entity Component System.

### EcsEvent Hierarchy

```
com.hypixel.hytale.component.system.EcsEvent              ← base class
├── CancellableEcsEvent                                    ← can be cancelled
│   implements ICancellableEcsEvent
│       boolean isCancelled()
│       void setCancelled(boolean)
│
└── (concrete events: Damage, BreakBlockEvent, DamageBlockEvent, etc.)
```

### EntityEventSystem (`com.hypixel.hytale.component.system.EntityEventSystem`)

```java
// Per-entity event handler - registered as an ECS system
public abstract class EntityEventSystem<ECS_TYPE, EventType extends EcsEvent> 
    extends EventSystem<EventType> 
    implements QuerySystem<ECS_TYPE> {
    
    protected EntityEventSystem(Class<EventType>);
    
    // Override this to handle events:
    public abstract void handle(
        int index, 
        ArchetypeChunk<ECS_TYPE>, 
        Store<ECS_TYPE>, 
        CommandBuffer<ECS_TYPE>, 
        EventType
    );
}
```

### WorldEventSystem (`com.hypixel.hytale.component.system.WorldEventSystem`)

```java
// World-level event handler
public abstract class WorldEventSystem<ECS_TYPE, EventType extends EcsEvent>
    extends EventSystem<EventType>
    implements ISystem<ECS_TYPE> {
    
    protected WorldEventSystem(Class<EventType>);
    
    public abstract void handle(
        Store<ECS_TYPE>,
        CommandBuffer<ECS_TYPE>,
        EventType
    );
}
```

### DamageEventSystem (`com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem`)

```java
// Base class for listening to Damage events on entities
public abstract class DamageEventSystem 
    extends EntityEventSystem<EntityStore, Damage> {
    
    protected DamageEventSystem();
    // Inherits: handle(int, ArchetypeChunk<EntityStore>, Store<EntityStore>, CommandBuffer<EntityStore>, Damage)
}
```

### EventSystemType Registration

```java
// ECS events are registered via EntityEventType / WorldEventType on the ComponentRegistry
public class EntityEventType<ECS_TYPE, Event extends EcsEvent> 
    extends EventSystemType<ECS_TYPE, Event, EntityEventSystem<ECS_TYPE, Event>> {
    
    public EntityEventType(
        ComponentRegistry<ECS_TYPE>,
        Class<? super EntityEventSystem<ECS_TYPE, Event>>,
        Class<Event>,
        int                                               // priority/ordering
    );
}

public class WorldEventType<ECS_TYPE, Event extends EcsEvent>
    extends EventSystemType<ECS_TYPE, Event, WorldEventSystem<ECS_TYPE, Event>> {
    
    public WorldEventType(
        ComponentRegistry<ECS_TYPE>,
        Class<? super WorldEventSystem<ECS_TYPE, Event>>,
        Class<Event>,
        int
    );
}
```

---

## 3. How a Plugin Subscribes to Events

### Via PluginBase.getEventRegistry()

Every plugin (extending `JavaPlugin` → `PluginBase`) has an `EventRegistry`:

```java
public class PluginBase {
    // Access to your plugin's EventRegistry (wraps the global EventBus)
    public EventRegistry getEventRegistry();
    
    // Also available:
    public ComponentRegistryProxy<EntityStore> getEntityStoreRegistry();   // for ECS components/systems
    public CommandRegistry getCommandRegistry();
    public TaskRegistry getTaskRegistry();
    // ...
}
```

### Pattern: Subscribe to Global Events

```java
public class MyPlugin extends JavaPlugin {
    public MyPlugin(JavaPluginInit init) {
        super(init);
    }
    
    @Override
    protected void setup() {
        EventBus eventBus = HytaleServer.get().getEventBus();
        
        // Listen for player connect (Void key = no-arg registration)
        eventBus.register(PlayerConnectEvent.class, event -> {
            Player player = event.getPlayer();
            getLogger().info("Player connected: " + player.getDisplayName());
        });
        
        // Listen for player ready with priority
        eventBus.register(EventPriority.EARLY, PlayerReadyEvent.class, event -> {
            // ...
        });
        
        // Listen for entity removal (keyed by entity type string)
        eventBus.registerGlobal(EntityRemoveEvent.class, event -> {
            Entity entity = event.getEntity();
            // Fires for ALL entity types
        });
        
        // Async example: chat event
        eventBus.registerAsync(PlayerChatEvent.class, future -> {
            return future.thenApply(event -> {
                // Modify or cancel
                return event;
            });
        });
    }
}
```

### Pattern: Subscribe to ECS Entity Events (Damage)

```java
@Override
protected void setup() {
    // Register an ECS system that handles Damage events
    getEntityStoreRegistry().registerSystem(
        DamageModule.get().getInspectDamageGroup(),  // system group for damage inspection
        new DamageEventSystem() {
            @Override
            public Query<EntityStore> getQuery() {
                // Define which entities this system matches
                return Query.builder()
                    .with(NPCEntity.getComponentType())  // only NPCs
                    .build();
            }
            
            @Override
            public void handle(int index, ArchetypeChunk<EntityStore> chunk, 
                             Store<EntityStore> store, CommandBuffer<EntityStore> cmd, 
                             Damage damage) {
                // damage.getAmount() - how much damage
                // damage.getCause() - DamageCause
                // damage.getSource() - who/what caused it
                // damage.isCancelled() / damage.setCancelled(true) - cancel
            }
        }
    );
}
```

---

## 4. Available Game Events (Full Catalog)

### Global Events (EventBus - `IEvent<KeyType>`)

#### Lifecycle Events (Void key)
| Event | Package | Key Type | Description |
|-------|---------|----------|-------------|
| `BootEvent` | `server.core.event.events` | `Void` | Server finished booting |
| `ShutdownEvent` | `server.core.event.events` | `Void` | Server shutting down |
| `PrepareUniverseEvent` | `server.core.event.events` | `Void` | Universe loading |

#### Player Events
| Event | Key Type | Description |
|-------|----------|-------------|
| `PlayerConnectEvent` | `Void` | Player connecting (can set world) |
| `PlayerDisconnectEvent` | `Void` | Player disconnecting |
| `PlayerReadyEvent` | `String` | Player finished loading into world |
| `PlayerChatEvent` | `String` (async, cancellable) | Player sent chat message |
| `PlayerInteractEvent` | `String` (cancellable) | Player interaction (target entity/block) |
| `PlayerMouseButtonEvent` | `String` | Mouse button input |
| `PlayerMouseMotionEvent` | `String` | Mouse motion input |
| `PlayerCraftEvent` | `String` | Player crafted item |
| `AddPlayerToWorldEvent` | `String` | Player being added to world |
| `DrainPlayerFromWorldEvent` | `String` | Player being removed from world |
| `PlayerSetupConnectEvent` | ? | Setup phase connect |
| `PlayerSetupDisconnectEvent` | ? | Setup phase disconnect |

#### Entity Events
| Event | Key Type | Description |
|-------|----------|-------------|
| `EntityRemoveEvent` | `String` | Entity removed from world |
| `LivingEntityInventoryChangeEvent` | ? | Inventory changed |
| `LivingEntityUseBlockEvent` | ? | Entity used a block |

#### World Events
| Event | Key Type | Description |
|-------|----------|-------------|
| `AddWorldEvent` | ? | World added to universe |
| `RemoveWorldEvent` | ? | World removed |
| `StartWorldEvent` | ? | World started |
| `AllWorldsLoadedEvent` | ? | All worlds loaded |
| `ChunkEvent` | ? | Chunk loaded/unloaded |

#### Plugin Events
| Event | Key Type | Description |
|-------|----------|-------------|
| `PluginSetupEvent` | `Class<? extends PluginBase>` | Plugin setup phase |

### ECS Events (EntityEventSystem / WorldEventSystem)

| Event | Type | Cancellable | Description |
|-------|------|-------------|-------------|
| `Damage` | Entity | **Yes** | Entity taking damage |
| `BreakBlockEvent` | Entity | **Yes** | Block broken |
| `DamageBlockEvent` | Entity | **Yes** | Block being damaged |
| `PlaceBlockEvent` | Entity | **Yes** | Block placed |
| `DropItemEvent` | Entity | Yes? | Item dropped |
| `CraftRecipeEvent` | Entity | Yes? | Recipe crafted |
| `ChangeGameModeEvent` | Entity | Yes? | Game mode change |
| `DiscoverZoneEvent` | Entity | ? | Zone discovered |
| `SwitchActiveSlotEvent` | Entity | ? | Active hotbar slot changed |
| `UseBlockEvent` | Entity | ? | Block used/interacted with |
| `InteractivelyPickupItemEvent` | Entity | ? | Item picked up |

### ⚠️ KEY FINDING: No Explicit Death Event!

**There is NO `EntityDeathEvent`, `LivingEntityDeathEvent`, or `PlayerDeathEvent`** in the global EventBus.

Death in Hytale is handled by the **ECS system** via:
1. `Damage` ECS event reduces health stat to 0
2. `DeathComponent` is added to the entity by `DeathSystems`
3. `DeferredCorpseRemoval` handles body cleanup
4. For players: `RespawnPage` UI is shown, `RespawnSystems` handles respawn

**To detect death, you must either:**
- Listen for `DeathComponent` being added (ECS HolderSystem)
- Listen for `EntityRemoveEvent` on the global EventBus
- Poll entities for `DeathComponent` presence
- Monitor health stat reaching min value

---

## 5. Damage System

### Damage (`com.hypixel.hytale.server.core.modules.entity.damage.Damage`)

The core damage event. Extends `CancellableEcsEvent`.

```java
public class Damage extends CancellableEcsEvent implements IMetaStore<Damage> {
    
    // Constructors
    public Damage(Damage.Source source, DamageCause cause, float amount);
    public Damage(Damage.Source source, int damageCauseIndex, float amount);
    
    // === Core Properties ===
    public float getAmount();
    public void setAmount(float);              // modify damage amount
    public float getInitialAmount();           // original amount before modification
    
    public DamageCause getCause();             // what kind of damage
    public int getDamageCauseIndex();
    public void setDamageCauseIndex(int);
    
    public Damage.Source getSource();          // who/what caused it
    public void setSource(Damage.Source);
    
    // === From CancellableEcsEvent ===
    public boolean isCancelled();
    public void setCancelled(boolean);         // cancel the damage!
    
    // === Death Message ===
    public Message getDeathMessage(Ref<EntityStore>, ComponentAccessor<EntityStore>);
    
    // === Meta Properties ===
    public IMetaStoreImpl<Damage> getMetaStore();
    
    // Static meta keys:
    public static final MetaKey<Vector4d> HIT_LOCATION;
    public static final MetaKey<Float> HIT_ANGLE;
    public static final MetaKey<Damage.Particles> IMPACT_PARTICLES;
    public static final MetaKey<Damage.SoundEffect> IMPACT_SOUND_EFFECT;
    public static final MetaKey<Damage.SoundEffect> PLAYER_IMPACT_SOUND_EFFECT;
    public static final MetaKey<Damage.CameraEffect> CAMERA_EFFECT;
    public static final MetaKey<String> DEATH_ICON;
    public static final MetaKey<Boolean> BLOCKED;
    public static final MetaKey<Float> STAMINA_DRAIN_MULTIPLIER;
    public static final MetaKey<Boolean> CAN_BE_PREDICTED;
    public static final MetaKey<KnockbackComponent> KNOCKBACK_COMPONENT;
    
    // Null source constant
    public static final Damage.Source NULL_SOURCE;
}
```

### Damage.Source (interface)

```java
public interface Damage.Source {
    // Default: returns a generic death message
    default Message getDeathMessage(Damage, Ref<EntityStore>, ComponentAccessor<EntityStore>);
}
```

### DamageCause (`com.hypixel.hytale.server.core.modules.entity.damage.DamageCause`)

```java
public class DamageCause implements JsonAssetWithMap<String, ...> {
    
    // Built-in causes:
    public static DamageCause PHYSICAL;          // melee attacks
    public static DamageCause PROJECTILE;        // ranged attacks
    public static DamageCause COMMAND;           // /damage command
    public static DamageCause DROWNING;
    public static DamageCause ENVIRONMENT;       // environmental (lava, etc.)
    public static DamageCause FALL;              // fall damage
    public static DamageCause OUT_OF_WORLD;      // void
    public static DamageCause SUFFOCATION;
    
    // Properties:
    public String getId();
    public boolean isDurabilityLoss();
    public boolean isStaminaLoss();
    public boolean doesBypassResistances();
    public String getAnimationId();
    public String getDeathAnimationId();
    
    // Asset lookup:
    public static AssetStore<String, DamageCause, ...> getAssetStore();
    public static IndexedLookupTableAssetMap<String, DamageCause> getAssetMap();
}
```

### DamageSystems (executing damage)

```java
public class DamageSystems {
    public static final float DEFAULT_DAMAGE_DELAY;
    
    // Execute damage on an entity:
    public static void executeDamage(
        Ref<EntityStore> targetRef, 
        ComponentAccessor<EntityStore> accessor, 
        Damage damage
    );
    
    public static void executeDamage(
        int index, 
        ArchetypeChunk<EntityStore> chunk, 
        CommandBuffer<EntityStore> cmd, 
        Damage damage
    );
    
    public static void executeDamage(
        Ref<EntityStore> targetRef, 
        CommandBuffer<EntityStore> cmd, 
        Damage damage
    );
}
```

### DamageModule (getting system groups)

```java
public class DamageModule extends JavaPlugin {
    public static DamageModule get();              // singleton access
    
    // System groups for registering damage handlers:
    public SystemGroup<EntityStore> getGatherDamageGroup();    // damage collection phase
    public SystemGroup<EntityStore> getFilterDamageGroup();    // damage filtering/modification
    public SystemGroup<EntityStore> getInspectDamageGroup();   // damage inspection (post-filter)
    
    // Component types:
    public ComponentType<EntityStore, DeathComponent> getDeathComponentType();
    public ComponentType<EntityStore, DeferredCorpseRemoval> getDeferredCorpseRemovalComponentType();
}
```

### DamageDataComponent (combat tracking)

```java
// Package: com.hypixel.hytale.server.core.entity.damage
public class DamageDataComponent implements Component<EntityStore> {
    
    public static ComponentType<EntityStore, DamageDataComponent> getComponentType();
    
    public Instant getLastCombatAction();
    public void setLastCombatAction(Instant);
    public Instant getLastDamageTime();
    public void setLastDamageTime(Instant);
    public Instant getLastChargeTime();
    public void setLastChargeTime(Instant);
    public WieldingInteraction getCurrentWielding();
    public void setCurrentWielding(WieldingInteraction);
}
```

### DamageData (NPC damage tracking)

```java
// Package: com.hypixel.hytale.server.npc.util
public class DamageData {
    // Kill tracking
    public boolean haveKill();
    public boolean haveKilled(Ref<EntityStore>);
    public Ref<EntityStore> getAnyKilled();
    public Vector3d getKillPosition(Ref<EntityStore>);
    
    // Damage tracking
    public void onInflictedDamage(Ref<EntityStore>, double);
    public void onSufferedDamage(CommandBuffer<EntityStore>, Damage);
    public void onKill(Ref<EntityStore>, Vector3d);
    
    public double getMaxDamageInflicted();
    public double getMaxDamageSuffered();
    public double getDamage(DamageCause);
    public boolean hasSufferedDamage(DamageCause);
    
    // Entity tracking
    public Ref<EntityStore> getMostDamagedVictim();
    public Ref<EntityStore> getMostDamagingAttacker();
    public Ref<EntityStore> getAnyAttacker();
    
    public void reset();
    public DamageData clone();
}
```

---

## 6. Death System

### DeathComponent (`com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent`)

When an entity dies, a `DeathComponent` is added to it.

```java
public class DeathComponent implements Component<EntityStore> {
    
    public static ComponentType<EntityStore, DeathComponent> getComponentType();
    
    // Construction (internal — created by DeathSystems)
    protected DeathComponent(Damage deathInfo);
    protected DeathComponent();
    
    // === Death Information ===
    public DamageCause getDeathCause();
    public Damage getDeathInfo();                          // full damage event that killed
    public Message getDeathMessage();
    public void setDeathMessage(Message);
    
    // === UI Control ===
    public boolean isShowDeathMenu();
    public void setShowDeathMenu(boolean);
    public boolean displayDataOnDeathScreen();
    public void setDisplayDataOnDeathScreen(boolean);
    
    // === Item Loss ===
    public ItemStack[] getItemsLostOnDeath();
    public void setItemsLostOnDeath(List<ItemStack>);
    public double getItemsAmountLossPercentage();
    public void setItemsAmountLossPercentage(double);
    public double getItemsDurabilityLossPercentage();
    public void setItemsDurabilityLossPercentage(double);
    public DeathConfig.ItemsLossMode getItemsLossMode();
    public void setItemsLossMode(DeathConfig.ItemsLossMode);
    public DeathItemLoss getDeathItemLoss();
    
    // === Interaction Chain ===
    public InteractionChain getInteractionChain();
    public void setInteractionChain(InteractionChain);
    
    // === Adding DeathComponent to an entity ===
    public static void tryAddComponent(
        CommandBuffer<EntityStore>, 
        Ref<EntityStore>, 
        Damage
    );
    
    public static void tryAddComponent(
        Store<EntityStore>, 
        Ref<EntityStore>, 
        Damage
    );
    
    // === Respawn ===
    public static CompletableFuture<Void> respawn(
        ComponentAccessor<EntityStore>, 
        Ref<EntityStore>
    );
}
```

### DeferredCorpseRemoval

```java
// Added after death for timed body cleanup
public class DeferredCorpseRemoval implements Component<EntityStore> {
    public static ComponentType<EntityStore, DeferredCorpseRemoval> getComponentType();
    
    public DeferredCorpseRemoval(double timeRemaining);
    public boolean tick(float delta);   // returns true when time is up
}
```

### DeathSystems

```java
public class DeathSystems {
    // Internal: plays death animation
    private static void playDeathAnimation(
        Ref<EntityStore>, DeathComponent, ModelComponent, 
        MovementStatesComponent, ComponentAccessor<EntityStore>
    );
}
```

### RespawnSystems

```java
public class RespawnSystems {
    // (class body is empty in decompilation — logic may be in lambdas/inner classes)
}
```

### RespawnPage (Player Death UI)

```java
public class RespawnPage extends InteractiveCustomUIPage<RespawnPage.RespawnPageEventData> {
    
    public RespawnPage(
        PlayerRef playerRef, 
        Message deathReason, 
        boolean displayDataOnDeathScreen, 
        DeathItemLoss deathItemLoss
    );
    
    // Shows death screen, handles respawn button click
    public void build(Ref<EntityStore>, UICommandBuilder, UIEventBuilder, Store<EntityStore>);
    public void handleDataEvent(Ref<EntityStore>, Store<EntityStore>, RespawnPageEventData);
    public void onDismiss(Ref<EntityStore>, Store<EntityStore>);
}
```

### RespawnController (interface)

```java
public interface RespawnController {
    CompletableFuture<Void> respawnPlayer(
        World world, 
        Ref<EntityStore> playerRef, 
        ComponentAccessor<EntityStore> accessor
    );
}
```

### Death Flow Summary

```
1. Entity takes damage (Damage ECS event)
   └── DamageSystems.executeDamage() fires Damage event through ECS pipeline
       ├── GatherDamage group → collects damage
       ├── FilterDamage group → modifies/cancels damage
       └── InspectDamage group → post-damage inspection

2. If health stat reaches 0:
   └── DeathComponent.tryAddComponent() is called
       ├── DeathComponent added to entity
       ├── DeathSystems plays death animation
       └── For players: RespawnPage UI shown

3. After death:
   ├── DeferredCorpseRemoval ticks down
   ├── For NPCs: entity.remove() or flock cleanup
   └── For players: respawn via DeathComponent.respawn()

4. Detection points:
   ├── EntityRemoveEvent (global EventBus) — fires when entity removed
   ├── DeathComponent presence on entity — check via ECS
   └── Health stat at minimum value — check EntityStatMap
```

---

## 7. Entity Health / Stats System

Hytale uses a **generic stat system** — health is just one of several entity stats (health, oxygen, stamina, mana, signature energy, ammo).

### DefaultEntityStatTypes

```java
// Package: com.hypixel.hytale.server.core.modules.entitystats.asset
public abstract class DefaultEntityStatTypes {
    // Returns int indices into EntityStatMap
    public static int getHealth();
    public static int getOxygen();
    public static int getStamina();
    public static int getMana();
    public static int getSignatureEnergy();
    public static int getAmmo();
    
    public static void update();   // refreshes indices from asset store
}
```

### EntityStatType (stat definition)

```java
public class EntityStatType {
    // Asset lookup:
    public static AssetStore<String, EntityStatType, ...> getAssetStore();
    public static IndexedLookupTableAssetMap<String, EntityStatType> getAssetMap();
    
    public String getId();                       // e.g. "health", "stamina"
    public float getInitialValue();              // starting value
    public float getMin();                       // minimum (usually 0)
    public float getMax();                       // maximum (e.g. 100)
    public boolean isShared();                   // visible to other players
    public boolean getIgnoreInvulnerability();
    
    // Regeneration config:
    public EntityStatType.Regenerating[] getRegenerating();
    
    // Effects at min/max:
    public EntityStatType.EntityStatEffects getMinValueEffects();   // death effects etc.
    public EntityStatType.EntityStatEffects getMaxValueEffects();
    
    public EntityStatResetBehavior getResetBehavior();
}
```

### EntityStatMap (per-entity stats - the key class!)

```java
// Package: com.hypixel.hytale.server.core.modules.entitystats
// This is a Component<EntityStore> — attached to entities in the ECS
public class EntityStatMap implements Component<EntityStore> {
    
    public static ComponentType<EntityStore, EntityStatMap> getComponentType();
    
    // === GET stat value by index ===
    public EntityStatValue get(int index);      // by stat index (use DefaultEntityStatTypes.getHealth())
    public EntityStatValue get(String id);      // by stat id string (e.g. "health")
    public int size();
    
    // === SET stat value ===
    public float setStatValue(int index, float value);          // set absolute value
    public float addStatValue(int index, float amount);         // add to current
    public float subtractStatValue(int index, float amount);    // subtract from current
    public float minimizeStatValue(int index);                  // set to min
    public float maximizeStatValue(int index);                  // set to max
    public float resetStatValue(int index);                     // reset to initial
    
    // Predictable variants (for client prediction):
    public float setStatValue(EntityStatMap.Predictable, int, float);
    public float addStatValue(EntityStatMap.Predictable, int, float);
    public float subtractStatValue(EntityStatMap.Predictable, int, float);
    public float minimizeStatValue(EntityStatMap.Predictable, int);
    public float maximizeStatValue(EntityStatMap.Predictable, int);
    public float resetStatValue(EntityStatMap.Predictable, int);
    
    // === MODIFIERS ===
    public Modifier getModifier(int statIndex, String modifierName);
    public Modifier putModifier(int statIndex, String modifierName, Modifier);
    public Modifier removeModifier(int statIndex, String modifierName);
    
    // Network sync:
    public void update();
    public void clearUpdates();
}
```

### EntityStatValue (single stat)

```java
public class EntityStatValue {
    public String getId();           // e.g. "health"
    public int getIndex();           // numeric index
    public float get();              // current value
    public float asPercentage();     // current / max (0.0 - 1.0)
    public float getMin();           // minimum value
    public float getMax();           // maximum value
    public boolean getIgnoreInvulnerability();
    
    // Regeneration:
    public RegeneratingValue[] getRegeneratingValues();
    
    // Modifiers:
    public Modifier getModifier(String name);
    public Map<String, Modifier> getModifiers();
}
```

### EntityStatsModule (access helper)

```java
public class EntityStatsModule extends JavaPlugin {
    public static EntityStatsModule get();             // singleton
    
    // === Quick access to entity's stat map ===
    public static EntityStatMap get(Entity entity);    // EntityStatsModule.get(entity)
    
    // Component type for ECS queries:
    public ComponentType<EntityStore, EntityStatMap> getEntityStatMapComponentType();
}
```

### How to Check Health / Check if Dead

```java
// Get the health stat index
int healthIndex = DefaultEntityStatTypes.getHealth();

// Option 1: Via EntityStatsModule helper
EntityStatMap stats = EntityStatsModule.get(entity);
if (stats != null) {
    EntityStatValue health = stats.get(healthIndex);
    float currentHealth = health.get();
    float maxHealth = health.getMax();
    float healthPercent = health.asPercentage();
    boolean isDead = currentHealth <= health.getMin();
}

// Option 2: Check for DeathComponent
Ref<EntityStore> ref = entity.getReference();
ComponentAccessor<EntityStore> accessor = /* from ECS context */;
DeathComponent death = accessor.get(ref, DeathComponent.getComponentType());
boolean isDead = (death != null);

// Option 3: Check entity removal
boolean wasRemoved = entity.wasRemoved();
```

### AliveCondition (internal check)

```java
// Hytale's internal "is entity alive" condition (used in stat regen):
public class AliveCondition extends Condition {
    public boolean eval0(
        ComponentAccessor<EntityStore>, 
        Ref<EntityStore>, 
        Instant
    );
    // Checks for absence of DeathComponent
}
```

---

## 8. Death Detection Strategies for Dungeons

### Strategy 1: EntityRemoveEvent (Simplest — Recommended)

Listen for entities being removed from the world. Track spawned mob UUIDs and cross-reference.

```java
// In plugin setup:
EventBus eventBus = HytaleServer.get().getEventBus();

Set<UUID> dungeonMobUuids = ConcurrentHashMap.newKeySet();

eventBus.registerGlobal(EntityRemoveEvent.class, event -> {
    Entity entity = event.getEntity();
    UUID uuid = entity.getUuid();
    if (dungeonMobUuids.remove(uuid)) {
        // This dungeon mob was removed (probably died)
        checkRoomCleared();
    }
});

// When spawning:
NPCEntity npc = new NPCEntity(world);
npc.setRoleName("trork_warrior");
world.spawnEntity(npc, pos, rot);
dungeonMobUuids.add(npc.getUuid());
```

**Pros:** Simple, uses global EventBus, catches all removal reasons.
**Cons:** Fires for ANY removal (death, despawn, manual remove). Can't distinguish cause.

### Strategy 2: DamageEventSystem (Most Precise)

Register an ECS DamageEventSystem to intercept damage on dungeon mobs.

```java
// Register in setup():
getEntityStoreRegistry().registerSystem(
    DamageModule.get().getInspectDamageGroup(),
    new DamageEventSystem() {
        @Override
        public Query<EntityStore> getQuery() {
            return Query.builder()
                .with(NPCEntity.getComponentType())
                .with(EntityStatMap.getComponentType())
                .build();
        }
        
        @Override
        public void handle(int idx, ArchetypeChunk<EntityStore> chunk,
                         Store<EntityStore> store, CommandBuffer<EntityStore> cmd,
                         Damage damage) {
            // Get entity reference
            Ref<EntityStore> ref = chunk.getRef(idx);
            Entity entity = store.getEntity(ref);
            
            if (entity instanceof NPCEntity npc) {
                UUID uuid = npc.getUuid();
                if (dungeonMobUuids.contains(uuid)) {
                    // Check if this damage will kill
                    EntityStatMap stats = EntityStatsModule.get(npc);
                    int healthIdx = DefaultEntityStatTypes.getHealth();
                    EntityStatValue health = stats.get(healthIdx);
                    
                    if (health.get() - damage.getAmount() <= health.getMin()) {
                        // This mob is about to die!
                        onDungeonMobKilled(npc, damage);
                    }
                }
            }
        }
    }
);
```

**Pros:** Know exact cause of death, can modify damage, detect attacker.
**Cons:** More complex ECS setup. Damage might not always directly equal death (armor, resistance).

### Strategy 3: Polling (Simplest but Least Efficient)

Periodically check if tracked mobs still exist or have DeathComponent.

```java
// In a scheduled task:
for (UUID uuid : dungeonMobUuids) {
    Entity entity = world.getEntity(uuid);
    if (entity == null || entity.wasRemoved()) {
        dungeonMobUuids.remove(uuid);
        checkRoomCleared();
    }
}
```

**Pros:** Dead simple, no event registration needed.
**Cons:** Not real-time, wastes CPU on polling.

### Strategy 4: Combined (Recommended for Production)

Use EntityRemoveEvent for mob-clear detection + DamageEventSystem for scoring/attribution.

```java
// Track both events:
// 1. EntityRemoveEvent → dungeon room clear logic
// 2. DamageEventSystem → damage tracking, kill attribution, score
// 3. Health stat checks → boss health bars, UI updates
```

---

## Package Reference

| Class | Full Package |
|-------|-------------|
| `EventBus` | `com.hypixel.hytale.event` |
| `IEventBus` | `com.hypixel.hytale.event` |
| `IEvent` | `com.hypixel.hytale.event` |
| `IBaseEvent` | `com.hypixel.hytale.event` |
| `IAsyncEvent` | `com.hypixel.hytale.event` |
| `ICancellable` | `com.hypixel.hytale.event` |
| `EventPriority` | `com.hypixel.hytale.event` |
| `EventRegistration` | `com.hypixel.hytale.event` |
| `EventRegistry` | `com.hypixel.hytale.event` |
| `EcsEvent` | `com.hypixel.hytale.component.system` |
| `CancellableEcsEvent` | `com.hypixel.hytale.component.system` |
| `EntityEventSystem` | `com.hypixel.hytale.component.system` |
| `WorldEventSystem` | `com.hypixel.hytale.component.system` |
| `EntityEventType` | `com.hypixel.hytale.component.event` |
| `WorldEventType` | `com.hypixel.hytale.component.event` |
| `Damage` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `Damage.Source` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DamageCause` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DamageModule` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DamageEventSystem` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DamageSystems` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DamageCalculatorSystems` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DeathComponent` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DeathSystems` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `DeferredCorpseRemoval` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `RespawnSystems` | `com.hypixel.hytale.server.core.modules.entity.damage` |
| `KillFeedEvent` | `com.hypixel.hytale.server.core.modules.entity.damage.event` |
| `DamageDataComponent` | `com.hypixel.hytale.server.core.entity.damage` |
| `DamageData` | `com.hypixel.hytale.server.npc.util` |
| `EntityStatMap` | `com.hypixel.hytale.server.core.modules.entitystats` |
| `EntityStatValue` | `com.hypixel.hytale.server.core.modules.entitystats` |
| `EntityStatsModule` | `com.hypixel.hytale.server.core.modules.entitystats` |
| `EntityStatType` | `com.hypixel.hytale.server.core.modules.entitystats.asset` |
| `DefaultEntityStatTypes` | `com.hypixel.hytale.server.core.modules.entitystats.asset` |
| `AliveCondition` | `com.hypixel.hytale.server.core.modules.entitystats.asset.condition` |
| `DeathConfig` | `com.hypixel.hytale.server.core.asset.type.gameplay` |
| `RespawnConfig` | `com.hypixel.hytale.server.core.asset.type.gameplay` |
| `RespawnController` | `com.hypixel.hytale.server.core.asset.type.gameplay.respawn` |
| `RespawnPage` | `com.hypixel.hytale.server.core.entity.entities.player.pages` |
| `HytaleServer` | `com.hypixel.hytale.server.core` |
| `JavaPlugin` | `com.hypixel.hytale.server.core.plugin` |
| `PluginBase` | `com.hypixel.hytale.server.core.plugin` |
| `EntityRemoveEvent` | `com.hypixel.hytale.server.core.event.events.entity` |
| `PlayerConnectEvent` | `com.hypixel.hytale.server.core.event.events.player` |
| `PlayerReadyEvent` | `com.hypixel.hytale.server.core.event.events.player` |
| `PlayerDisconnectEvent` | `com.hypixel.hytale.server.core.event.events.player` |
| `PlayerInteractEvent` | `com.hypixel.hytale.server.core.event.events.player` |
| `PlayerChatEvent` | `com.hypixel.hytale.server.core.event.events.player` |

---

*Generated 2026-02-07 from HytaleServer.jar decompilation. All signatures verified via `javap -p`.*
