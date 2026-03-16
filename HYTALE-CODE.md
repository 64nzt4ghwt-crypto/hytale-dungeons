# HYTALE-CODE.md — Hytale Server Plugin API Knowledge Base

## Plugin Structure
```java
// Entry point: extends JavaPlugin
public class MyPlugin extends JavaPlugin {
    public MyPlugin(JavaPluginInit init) { super(init); }
    
    @Override
    protected void start() {
        // Register components, commands, events, systems
    }
}
```

### manifest.json (src/main/resources/)
```json
{
    "Group": "com.howlstudio",
    "Name": "DungeonPlugin",
    "Version": "0.1.0",
    "Main": "com.howlstudio.dungeons.DungeonPlugin",
    "Dependencies": { "Hytale:EntityModule": "*" }
}
```

## Message Formatting (NO § codes!)
Hytale does NOT use Minecraft `§` color codes. Use the `Message` API:
```java
Message.raw("text")           // Plain text
    .color(new Color(r,g,b))  // java.awt.Color
    .bold(true)                // Bold
    .italic(true)              // Italic  
    .monospace(true)           // Monospace

// Compose messages
Message.raw("").insert(Message.raw("label: ").color(GOLD)).insert(Message.raw("value").color(WHITE));
```

### Event Titles (big screen text)
```java
EventTitleUtil.showEventTitleToPlayer(playerRef, titleMsg, subtitleMsg, showAnimation);
```

## Commands
```java
// Extend AbstractPlayerCommand for player-only commands
public class MyCmd extends AbstractPlayerCommand {
    public MyCmd() { super("cmdname", "description", false); }
    
    // Add typed arguments
    RequiredArg<String> arg = withRequiredArg("name", "desc", ArgTypes.STRING);
    
    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, 
                          Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        String val = arg.get(ctx);
        ctx.sendMessage(Message.raw("response"));
    }
}

// Register in plugin start()
getCommandRegistry().registerCommand(new MyCmd());
```

## ECS (Entity Component System)

### Components — Persistent data on entities
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

### Systems — Per-tick logic
```java
// Register: getEntityStoreRegistry().registerSystem(new MySystem());
```

## Events
```java
EventRegistry events = getEventRegistry();

// Player ready (confirmed working)
events.registerGlobal(PlayerReadyEvent.class, event -> {
    Player player = event.getPlayer();
});

// Note: PlayerMoveEvent, EntityDeathEvent, BlockInteractEvent — may exist, unconfirmed
```

## World & Teleportation (from API research, needs testing)
```java
// Universe singleton
Universe universe = Universe.get();

// Load world
CompletableFuture<World> future = universe.loadWorld("world_name");

// Teleport player
player.teleport(targetWorld, new Vector3d(x, y, z));

// Get player position
Vector3d pos = player.getPosition();
World world = player.getWorld();
```

## Entity Spawning (from API research, needs testing)
```java
Store<?> store = world.getEntityStore();
// Create entity with components via Holder
// Remove via store.removeEntity(ref, RemoveReason.LOGIC)
// Query via getEntitiesInRadius/getEntitiesInBox
```

## Block Manipulation (for barriers)
```java
world.setBlock(new Vector3i(x,y,z), blockState);
// AIR to remove barriers, solid blocks to create them
```

## UI Pages (Custom GUI)
```java
// Extend InteractiveCustomUIPage<EventDataClass>
// Use .ui files in resources for layout
// cmd.append("Pages/my_page.ui") to load
// Handle events via onEvent()
```

### Custom Page Lifetimes
- `CustomPageLifetime.CantClose` — forced open
- Other lifetimes available

## Player API
```java
Player player = ...;
player.sendMessage(msg);       // Chat message
player.getUuid();              // UUID
player.getPlayerRef();         // PlayerRef (has username)
player.getPageManager();       // For custom UI pages
player.getPosition();          // Vector3d position
player.getWorld();             // Current world

// PlayerRef
playerRef.getUsername();
playerRef.getUuid();
```

## Name Resolution
```java
Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
```

## Stats & Modifiers
```java
EntityStatMap stats = player.getStatMap(); // or similar
StaticModifier mod = new StaticModifier("id", value);
// Apply via EntityStatsModule
```

## File I/O
```java
// Plugin data directory
Path dataDir = getDataDirectory();  // per-world: mods/<group>_<name>/
// Use Gson for JSON config files
```

## Build
- Gradle + Java 21 (compile with openjdk@21, NOT 25)
- `compileOnly(files("libs/HytaleServer.jar"))` for server API
- Output JAR goes to: `~/Library/Application Support/Hytale/UserData/Mods/`
- World plugin data at: `~/Library/Application Support/Hytale/UserData/Saves/<world>/mods/<group>_<name>/`

## Key Gotchas
1. **No § color codes** — use Message.color()/bold()/italic()
2. **No Unicode symbols** — Hytale font only supports basic ASCII  
3. **Java 21 for building** — Java 25 breaks Gradle 8.13
4. **Async world loading** — use CompletableFuture
5. **ECS not OOP** — entities are refs+components, not objects
6. **`start()` not `setup()`** — override start() for initialization (setup() also works but start() is standard)
7. **`getCommandRegistry()`** — not `getServer().getCommandManager()`

## Confirmed Working
- [x] JavaPlugin lifecycle (start/stop)
- [x] Command registration (AbstractPlayerCommand)
- [x] Component registration (ComponentType, BuilderCodec)
- [x] Event registration (PlayerReadyEvent)
- [x] System registration
- [x] Message formatting (color/bold/italic/insert)
- [x] Config file I/O from data directory
- [x] Player name resolution
- [x] Custom UI pages (.ui files)

## Needs In-Game Testing
- [ ] World loading/unloading (Universe API)
- [ ] Player teleportation (cross-world)
- [ ] Entity spawning via ECS
- [ ] Block manipulation
- [ ] EntityDeathEvent / PlayerMoveEvent
- [ ] Area queries (getEntitiesInRadius)
- [ ] Packet sending for HUD
