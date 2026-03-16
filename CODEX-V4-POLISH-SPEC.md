# Dungeon Plugin V4 — Polish Build

## Context
The Hytale dungeon plugin is working but incomplete. The UI framework is proven (CustomUI pages load, text fields work, buttons work, events work). This build fills in all gameplay gaps.

## CRITICAL RULES
1. **DO NOT delete or rewrite existing files** — surgical edits only
2. **Build must pass**: `cd plugin && ./gradlew build`
3. **No HUD .ui files in asset pack** — Common/UI/Custom/HUD/ must NOT exist in resources
4. **.ui file rules**: Keep under 300 lines each. Use `$C = "../Common.ui"` for TextFields. Match TroubleDEV patterns (see reference below).
5. **EventData**: Use `new EventData().append(key, value)` — NOT `EventData.of()` with more than 2 args
6. **UI refresh**: Use inherited `sendUpdate()` method to refresh InteractiveCustomUIPage
7. **Imports**: DungeonTemplate is in `com.howlstudio.dungeons.config`, NOT `.manager`

## Reference: Working TroubleDEV Patterns
```java
// Event binding with input capture:
eventBuilder.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#SaveButton",
    new EventData()
        .append("Action", "Save")
        .append("@PlayerName", "#NameInput.Value")
);

// Simple button:
evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));

// UI refresh after state change:
sendUpdate(); // inherited from InteractiveCustomUIPage — rebuilds the page

// Page class:
public class MyPage extends InteractiveCustomUIPage<MyPage.MyEventData> {
    public MyPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, MyEventData.CODEC);
    }
}
```

## What's Working
- Main menu with 5 buttons (Create, Play, Manage, Active Runs, Close)
- Create Dungeon wizard (name, description, difficulty, players, time limit)
- Play menu shows templates, Play button starts instances
- Manage menu lists templates with edit/toggle/delete
- Active Runs panel
- All slash commands (/dcreate, /djoin, /dleave, /dlist, /dinfo, /dstatus, /dbrowse, /dadmin)
- Template save/load to JSON files
- Instance lifecycle (create, join, leave)

## What Needs Fixing/Adding

### 1. Wizard Buttons Not Updating UI
The difficulty/time/player +/- buttons call `sendUpdate()` but it might not rebuild properly. Verify `sendUpdate()` triggers a full `build()` call. If not, use `player.getPageManager().openCustomPage()` to reopen with updated state.

### 2. Create Dungeon → Auto-Leave Previous
When clicking Play on a dungeon, if the player is already in one, auto-leave first instead of requiring manual /dleave.

### 3. Dungeon Gameplay Loop
Currently starting a dungeon does nothing visible. Need:
- When instance starts, send player welcome messages (dungeon name, difficulty, room count)
- Room progression: `/dungeon next` or automatic room advance when mobs cleared
- Timer display via chat messages (since HUD is disabled)
- Mob spawning per room based on template config
- Completion detection (all rooms cleared → victory message + teleport back)
- Failure detection (timer runs out → fail message + teleport back)
- Loot drops on completion

### 4. Room/Mob/Loot Commands Need Polish
The admin commands for adding rooms/mobs/loot to templates need to work properly:
- `/dadmin room add <dungeon> <name>` — add a room to template
- `/dadmin room list <dungeon>` — show rooms
- `/dadmin mob add <dungeon> <entity> [room] [wave] [count]` — add mob spawn
- `/dadmin mob list <dungeon>` — show mob spawns  
- `/dadmin loot add <dungeon> <item> [room] [min] [max] [chance]` — add loot
- `/dadmin loot list <dungeon>` — show loot table
- `/dadmin reload` — reload all templates from disk

### 5. Play Menu Improvements
- Show dungeon difficulty, player count, room count in the template listing
- Show "IN PROGRESS" badge next to dungeons with active runs
- Join button should work for joining other players' runs

### 6. Instance Tick System
The dungeon needs a tick loop that:
- Checks room clear conditions each tick
- Manages mob wave spawning (wave 1 first, when cleared → wave 2, etc.)
- Tracks timer countdown
- Sends periodic timer warnings (5 min, 1 min, 30 sec)
- Handles player disconnect/reconnect during a run

### 7. Dungeon World Handling
Currently uses "default" world. Should:
- Create instances in the player's current world
- Save return position on dungeon start
- Teleport back to return position on completion/failure/leave
- The return position save/restore already exists in DungeonInstance — make sure it's used

## File Locations
- Plugin root: `/Users/misa/.openclaw/workspace/projects/hytale-dungeons/plugin/`
- Java source: `src/main/java/com/howlstudio/dungeons/`
- UI files: `src/main/resources/Common/UI/Custom/Pages/`
- Config templates: `src/main/resources/Server/DungeonConfigs/`
- Build: `./gradlew build` → `build/libs/DungeonPlugin-0.1.0.jar`

## Package Structure
```
com.howlstudio.dungeons/
├── DungeonPlugin.java          # Main plugin, registers commands + systems
├── commands/                   # Slash commands
│   ├── BaseDungeonCommand.java
│   ├── DungeonCreateCommand.java (/dcreate)
│   ├── DungeonJoinCommand.java   (/djoin)
│   ├── DungeonLeaveCommand.java  (/dleave)
│   ├── DungeonListCommand.java   (/dlist)
│   ├── DungeonInfoCommand.java   (/dinfo)
│   ├── DungeonStatusCommand.java (/dstatus)
│   ├── DungeonBrowseCommand.java (/dbrowse)
│   └── DungeonAdminCommand.java  (/dadmin)
├── config/
│   ├── DungeonTemplate.java      # Template data model
│   └── DungeonTemplateLoader.java
├── manager/
│   ├── DungeonManager.java       # Core manager (instances, lifecycle)
│   ├── DungeonInstance.java       # Active instance state
│   ├── DungeonPortal.java
│   ├── DungeonPortalManager.java
│   ├── MobSpawnPoint.java
│   ├── SpawnPointManager.java
│   └── WorldRegistry.java
├── systems/                      # ECS-style systems
│   ├── DungeonTickSystem.java    # Main tick loop
│   ├── MobScalingSystem.java
│   ├── CombatEventSystem.java
│   ├── TrapSystem.java
│   └── BossMechanicsSystem.java
├── ui/                           # GUI pages
│   ├── DungeonMainMenu.java
│   ├── DungeonPlayMenu.java
│   ├── DungeonManageMenu.java
│   ├── DungeonCreationWizard.java
│   ├── ActiveRunsPanel.java
│   ├── DungeonHud.java           # HUD (build() commented out — no .ui file)
│   ├── DungeonUiSupport.java
│   ├── WizardManager.java
│   └── WizardState.java
├── loot/
│   ├── LootTable.java
│   └── LootTableManager.java
├── events/
│   └── DungeonEventBus.java
├── compat/
│   └── RPGLevelingIntegration.java
├── api/
│   └── DungeonAPI.java
└── util/
    └── Msg.java
```

## Priority Order
1. Fix auto-leave on Play (quick win, improves UX immediately)
2. Fix wizard +/- buttons (reopen page if sendUpdate doesn't work)
3. Implement DungeonTickSystem gameplay loop (room progression, timer, completion)
4. Polish admin commands (room/mob/loot add/list/remove)
5. Improve Play menu display
6. Add chat-based dungeon status (replace HUD until we fix .ui HUD loading)
