# Hytale Dungeon Plugin V3 — GUI Wizard Overhaul

> **Goal:** Replace command-driven setup with a guided in-game GUI wizard. One command (`/dungeon`) opens everything. Click-through, no typing needed except dungeon/room names.
> **Base:** Existing V2 plugin (56 files, 5,870 lines, builds clean)
> **Target user:** Server admin who wants easy visual setup. Players who want to click "Play" and go.

---

## CRITICAL RULES FOR CODEX

1. **DO NOT delete or rewrite existing files wholesale.** Make surgical edits. Add new files.
2. **All existing commands MUST still work.** The GUI is an overlay, not a replacement.
3. **All new files go under `plugin/src/main/java/com/howlstudio/dungeons/`**
4. **Build must pass:** `cd plugin && ./gradlew build` with JAVA_HOME=/Users/misa/java/jdk-21.0.10+7/Contents/Home
5. **Use existing patterns.** Look at `DungeonHudService.java`, `DungeonBrowserPage.java`, `StatsGUIPage.java` (from RPGLeveling decompile) for how Hytale UI pages work.
6. **Hytale UI API reference** (from existing code):
   - `player.getPageManager().openCustomPage(ref, store, page)` — opens a UI page
   - `UICommandBuilder` — sets data bindings for UI elements
   - `DungeonHud extends CustomHud` — HUD overlay pattern
   - Pages can have clickable buttons that trigger server-side callbacks

---

## ARCHITECTURE: Main Menu → Sub-Menus → Wizards

### Entry Point
**`/dungeon`** (no arguments) → Opens `DungeonMainMenu` page

All other `/dungeon <sub>` commands still work as before for power users.

### Menu Tree
```
/dungeon → DungeonMainMenu
  ├── [🏗️ Create Dungeon] → DungeonCreationWizard (7 steps)
  ├── [🎮 Play Dungeon]   → DungeonPlayMenu (browse + join)
  ├── [⚙️ Manage]         → DungeonManageMenu (edit/delete/toggle)
  ├── [📊 Active Runs]    → ActiveRunsPanel (live instances)
  └── [❌ Close]
```

---

## FILE: `ui/DungeonMainMenu.java` (NEW)

```
package com.howlstudio.dungeons.ui;

/**
 * Main menu page opened by /dungeon (no args).
 * Shows 4 large buttons in a centered panel:
 *
 * ┌─────────────────────────────┐
 * │      DUNGEON MASTER         │
 * │                             │
 * │  [🏗️ Create Dungeon]       │
 * │  [🎮 Play Dungeon]         │
 * │  [⚙️ Manage Dungeons]      │  ← only visible to admins/ops
 * │  [📊 Active Runs]          │
 * │                             │
 * │  [❌ Close]                 │
 * └─────────────────────────────┘
 *
 * Each button opens the corresponding sub-page.
 * "Manage Dungeons" only visible if player has admin permission.
 *
 * Implementation:
 * - Extend appropriate Hytale page base class
 * - Use UICommandBuilder for button labels and visibility
 * - Button click callbacks call player.getPageManager().openCustomPage() for sub-pages
 * - Store reference to DungeonManager for data access
 */
```

---

## FILE: `ui/DungeonPlayMenu.java` (NEW)

```
package com.howlstudio.dungeons.ui;

/**
 * Player-facing dungeon browser. Shows available dungeons to play.
 *
 * ┌──────────────────────────────────────┐
 * │  🎮 PLAY DUNGEON                     │
 * │                                      │
 * │  ┌──────────────────────┐            │
 * │  │ Goblin Den           │            │
 * │  │ ⚔ Normal | 1-4 ppl  │            │
 * │  │ "Dark cave of gobs"  │            │
 * │  │ [▶ Play]  [ℹ Info]   │            │
 * │  └──────────────────────┘            │
 * │                                      │
 * │  ┌──────────────────────┐            │
 * │  │ Ice Temple           │            │
 * │  │ 💀 Hard | 2-4 ppl   │            │
 * │  │ "Frozen halls..."    │            │
 * │  │ [▶ Play]  [ℹ Info]   │            │
 * │  └──────────────────────┘            │
 * │                                      │
 * │  ── Active Runs ──                   │
 * │  Goblin Den (2/4 players) [Join]     │
 * │                                      │
 * │  [← Back]                            │
 * └──────────────────────────────────────┘
 *
 * "Play" button → creates new instance, teleports player in
 * "Join" button → joins existing instance
 * "Info" button → shows dungeon details (rooms, loot preview, estimated time)
 * "Back" → returns to DungeonMainMenu
 *
 * Data source: DungeonManager.getTemplateList() for templates,
 *              DungeonManager.getActiveInstances() for joinable runs
 */
```

---

## FILE: `ui/DungeonCreationWizard.java` (NEW)

```
package com.howlstudio.dungeons.ui;

/**
 * 7-step guided wizard for creating a dungeon. Admin only.
 * Each step is a page/panel. Navigation: [← Back] [Next →] at bottom.
 * Progress indicator at top: "Step 2 of 7 — Select World"
 *
 * === STEP 1: Basic Info ===
 * ┌─────────────────────────────────┐
 * │  Step 1 of 7 — Basic Info       │
 * │                                 │
 * │  Dungeon Name: [______________] │
 * │  Description:  [______________] │
 * │  Difficulty:   [Easy ▼]         │  ← dropdown: Easy/Normal/Hard/Extreme
 * │  Min Players:  [1 ▼]            │  ← dropdown: 1-8
 * │  Max Players:  [4 ▼]            │  ← dropdown: 1-8
 * │  Time Limit:   [10:00 ▼]        │  ← dropdown: 5/10/15/20/30 min
 * │                                 │
 * │  [Cancel]              [Next →] │
 * └─────────────────────────────────┘
 *
 * === STEP 2: Select World ===
 * ┌─────────────────────────────────┐
 * │  Step 2 of 7 — Select World     │
 * │                                 │
 * │  Available Worlds:              │
 * │  ○ goblin_cave_world            │  ← radio buttons
 * │  ○ ice_temple_world             │
 * │  ○ dark_forest_world            │
 * │  ● (current world)              │  ← default selected
 * │                                 │
 * │  [← Back]              [Next →] │
 * └─────────────────────────────────┘
 *
 * World list from: Universe.get().getWorlds() or registered worlds
 *
 * === STEP 3: Define Rooms ===
 * ┌─────────────────────────────────────┐
 * │  Step 3 of 7 — Define Rooms         │
 * │                                     │
 * │  Rooms:                             │
 * │  1. Entrance Hall    [Normal] [✏️ ❌]│
 * │  2. Skeleton Crypt   [Normal] [✏️ ❌]│
 * │  3. Boss Chamber     [Boss]   [✏️ ❌]│
 * │                                     │
 * │  [+ Add Room]                       │
 * │                                     │
 * │  [← Back]              [Next →]     │
 * └─────────────────────────────────────┘
 *
 * "Add Room" opens inline editor:
 *   Room Name: [____________]
 *   Type: [Normal ▼]  (Normal / Boss / Timed / Puzzle)
 *   Waves: [1 ▼]  (1-5)
 *   Time Limit: [0 ▼]  (0 = none, or seconds)
 *   [Save Room]
 *
 * === STEP 4: Place Spawn Points ===
 * ┌────────────────────────────────────────┐
 * │  Step 4 of 7 — Mob Spawn Points       │
 * │                                        │
 * │  Room: [Entrance Hall ▼]               │
 * │  Wave: [Wave 1 ▼]                      │
 * │                                        │
 * │  Current Spawns:                       │
 * │  • Goblin_Scavenger x3 at (10,64,5)   │
 * │  • Goblin_Miner x2 at (12,64,3)       │
 * │                                        │
 * │  [+ Add Spawn Point]                   │
 * │                                        │
 * │  ── Add Spawn ──                       │
 * │  Entity: [____________] (type name)    │
 * │  Amount: [1 ▼]  (1-20)                │
 * │  Position: [📍 Use My Position]        │ ← click = captures player pos
 * │  Radius: [2.0 ▼]                      │
 * │  Chance: [100% ▼]                     │
 * │  [Save Spawn]                          │
 * │                                        │
 * │  [← Back]              [Next →]        │
 * └────────────────────────────────────────┘
 *
 * "Use My Position" captures player's current XYZ at click time.
 * Admin walks to where they want mobs, clicks the button.
 *
 * === STEP 5: Configure Loot ===
 * ┌────────────────────────────────────────┐
 * │  Step 5 of 7 — Loot Tables            │
 * │                                        │
 * │  Room: [Entrance Hall ▼]               │
 * │                                        │
 * │  Current Loot:                         │
 * │  • iron_ore x2-5 (80%) [Common]       │
 * │  • emerald x1 (20%) [Rare]            │
 * │                                        │
 * │  [+ Add Loot]                          │
 * │                                        │
 * │  ── Add Loot ──                        │
 * │  Item ID: [____________]               │
 * │  Min Count: [1 ▼]  Max: [5 ▼]        │
 * │  Drop Chance: [80% ▼]                 │
 * │  Rarity: [Common ▼]                   │
 * │  [Save Loot]                           │
 * │                                        │
 * │  Boss Loot (for boss rooms):           │
 * │  • ogre_club x1 (100%) [Epic]         │
 * │  [+ Add Boss Loot]                     │
 * │                                        │
 * │  [← Back]              [Next →]        │
 * └────────────────────────────────────────┘
 *
 * === STEP 6: Set Portal Location ===
 * ┌─────────────────────────────────────┐
 * │  Step 6 of 7 — Portal Entrance      │
 * │                                     │
 * │  Walk to where you want the         │
 * │  dungeon portal and click below.    │
 * │                                     │
 * │  Current: (not set)                 │
 * │                                     │
 * │  [📍 Set Portal Here]              │
 * │                                     │
 * │  Portal Radius: [2.5 ▼]            │
 * │  Reset Mode: [When Empty ▼]         │
 * │    (When Empty / On Complete / Never)│
 * │                                     │
 * │  [← Back]              [Next →]     │
 * └─────────────────────────────────────┘
 *
 * === STEP 7: Review & Create ===
 * ┌─────────────────────────────────────┐
 * │  Step 7 of 7 — Review               │
 * │                                     │
 * │  Name: Goblin Den                   │
 * │  Difficulty: Normal                 │
 * │  Players: 1-4                       │
 * │  Time: 10:00                        │
 * │  World: goblin_cave_world           │
 * │  Rooms: 3                           │
 * │    1. Entrance Hall (2 waves)       │
 * │    2. Skeleton Crypt (1 wave)       │
 * │    3. Boss Chamber ⚔️ (1 wave)      │
 * │  Spawn Points: 12                   │
 * │  Loot Entries: 8                    │
 * │  Portal: (10, 64, -5)              │
 * │                                     │
 * │  [← Back]        [✅ Create!]       │
 * └─────────────────────────────────────┘
 *
 * "Create" button:
 * 1. Saves template JSON to DungeonConfigs/
 * 2. Registers world in DungeonWorldRegistry
 * 3. Sets portal in PortalManager
 * 4. Saves spawn points in SpawnPointManager
 * 5. Reloads DungeonManager templates
 * 6. Shows success message: "Dungeon 'Goblin Den' created! Players can now enter via portal."
 * 7. Closes wizard, returns to main menu
 *
 * IMPLEMENTATION NOTES:
 * - Store wizard state in a WizardState object per player (map in DungeonPlugin or a WizardManager)
 * - Each step renders as a page; "Next" validates current step before advancing
 * - WizardState holds: name, description, difficulty, minPlayers, maxPlayers, timeLimitSeconds,
 *   worldName, List<RoomDraft>, List<SpawnDraft>, List<LootDraft>, portalPosition, portalRadius, resetMode
 * - RoomDraft: name, type, waves, timedSeconds
 * - SpawnDraft: roomIndex, wave, entityRole, position, amount, radius, chance
 * - LootDraft: roomIndex, itemId, minCount, maxCount, chance, rarity, isBossLoot
 * - On final "Create": convert drafts to DungeonTemplate + save all configs
 */
```

---

## FILE: `ui/DungeonManageMenu.java` (NEW)

```
package com.howlstudio.dungeons.ui;

/**
 * Admin management panel for existing dungeons.
 *
 * ┌─────────────────────────────────────┐
 * │  ⚙️ MANAGE DUNGEONS                 │
 * │                                     │
 * │  ┌───────────────────────────┐      │
 * │  │ Goblin Den    [✅ Active] │      │
 * │  │ [✏️ Edit] [⏸ Disable] [🗑]│      │
 * │  └───────────────────────────┘      │
 * │                                     │
 * │  ┌───────────────────────────┐      │
 * │  │ Ice Temple    [⏸ Disabled]│      │
 * │  │ [✏️ Edit] [▶ Enable] [🗑] │      │
 * │  └───────────────────────────┘      │
 * │                                     │
 * │  [← Back]                           │
 * └─────────────────────────────────────┘
 *
 * "Edit" → opens DungeonCreationWizard pre-filled with existing data
 * "Disable/Enable" → toggles dungeon availability
 * "Delete" → confirmation prompt → removes template + configs
 * "Back" → returns to main menu
 */
```

---

## FILE: `ui/ActiveRunsPanel.java` (NEW)

```
package com.howlstudio.dungeons.ui;

/**
 * Shows all currently active dungeon instances.
 *
 * ┌──────────────────────────────────────┐
 * │  📊 ACTIVE RUNS                      │
 * │                                      │
 * │  Goblin Den                          │
 * │  Players: Steve, Alex (2/4)          │
 * │  Room 2/3 | Wave 1/2 | 05:32        │
 * │  [👁 Spectate]  [⛔ Force End]       │ ← admin only
 * │                                      │
 * │  Ice Temple                          │
 * │  Players: Bob (1/4)                  │
 * │  Room 1/5 | Wave 1/1 | 08:15        │
 * │  [👁 Spectate]  [⛔ Force End]       │
 * │                                      │
 * │  No active runs? "All quiet..."      │
 * │                                      │
 * │  [← Back]                            │
 * └──────────────────────────────────────┘
 *
 * Auto-refreshes every 3 seconds (via scheduled update).
 * "Spectate" teleports admin to dungeon world without joining.
 * "Force End" kills the instance (with confirmation).
 */
```

---

## FILE: `ui/WizardState.java` (NEW)

```
package com.howlstudio.dungeons.ui;

/**
 * Holds all in-progress data for the dungeon creation wizard.
 * One instance per admin player currently in the wizard.
 *
 * Fields:
 * - int currentStep (1-7)
 * - String name, description
 * - String difficulty ("easy"/"normal"/"hard"/"extreme")
 * - int minPlayers, maxPlayers
 * - int timeLimitSeconds
 * - String worldName
 * - List<RoomDraft> rooms
 * - List<SpawnDraft> spawns
 * - List<LootDraft> loots
 * - Vector3d portalPosition (nullable until set)
 * - double portalRadius
 * - PortalResetMode resetMode
 *
 * Inner classes:
 * - RoomDraft: String name, String type, int waves, int timedSeconds
 * - SpawnDraft: int roomIndex, int wave, String entityRole, Vector3d position, int amount, double radius, double chance
 * - LootDraft: int roomIndex, String itemId, int minCount, int maxCount, double chance, String rarity, boolean isBossLoot
 *
 * Methods:
 * - validate(int step) → String errorMessage or null
 * - toTemplate() → DungeonTemplate (converts drafts to final template)
 * - toSpawnPoints() → List<MobSpawnPoint>
 * - toLootEntries() → Map<Integer, List<LootEntry>> (roomIndex → loot)
 */
```

---

## FILE: `ui/WizardManager.java` (NEW)

```
package com.howlstudio.dungeons.ui;

/**
 * Manages active wizard sessions per player.
 *
 * Map<UUID, WizardState> activeWizards
 *
 * Methods:
 * - startWizard(UUID playerUuid) → WizardState (creates new)
 * - startWizardFromExisting(UUID playerUuid, DungeonTemplate template) → WizardState (pre-fills for editing)
 * - getWizard(UUID playerUuid) → WizardState or null
 * - cancelWizard(UUID playerUuid)
 * - completeWizard(UUID playerUuid, DungeonManager manager) → boolean
 *   - Converts WizardState to template JSON, saves to disk
 *   - Registers world, portal, spawn points
 *   - Reloads manager templates
 *   - Cleans up wizard state
 */
```

---

## MODIFICATIONS TO EXISTING FILES

### `commands/DungeonAdminCommand.java`
In the `execute()` method, add a case for when `sub` is empty or "menu":
```java
// At the very top of execute(), before the switch:
if (sub.isEmpty() || sub.equals("menu")) {
    openMainMenu(player, ref, store, playerRef);
    return;
}
```

Add method:
```java
private void openMainMenu(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
    DungeonMainMenu menu = new DungeonMainMenu(playerRef, manager, wizardManager);
    player.getPageManager().openCustomPage(ref, store, menu);
}
```

### `DungeonPlugin.java`
- Add `WizardManager wizardManager` field
- In `setup()`: `this.wizardManager = new WizardManager();`
- Pass `wizardManager` to `DungeonAdminCommand`

### Also modify the base `/dungeon` command registration:
When `/dungeon` is called with NO subcommand, open the main menu instead of showing help. This is the key UX change — typing just `/dungeon` gives you the full GUI.

---

## BUTTON CALLBACK PATTERN

Hytale pages support button clicks via event callbacks. The pattern from existing code:

```java
// In page class:
public void onButtonClick(String buttonId, PlayerRef playerRef) {
    switch (buttonId) {
        case "create" -> openWizard(playerRef);
        case "play" -> openPlayMenu(playerRef);
        case "manage" -> openManageMenu(playerRef);
        case "active" -> openActiveRuns(playerRef);
        case "close" -> closePage(playerRef);
    }
}
```

If Hytale's page API doesn't support direct button callbacks, use the alternative pattern from existing code:
- Set a data field via UICommandBuilder
- Player clicks trigger a command or event
- Handle in the command/event system

---

## POSITION CAPTURE PATTERN

For "Use My Position" buttons in the wizard:

```java
// When player clicks "Set Portal Here" or "Use My Position":
Vector3d pos = player.getTransformComponent().getPosition();
wizardState.setPortalPosition(pos);
// Update the UI to show: "Portal: (10, 64, -5) ✅"
```

This is instant — no special mode needed. Player walks to spot, opens wizard step, clicks button.

---

## PRIORITY ORDER

1. `WizardState.java` + `WizardManager.java` — data backbone
2. `DungeonMainMenu.java` — entry point
3. `DungeonPlayMenu.java` — players need this most
4. `DungeonCreationWizard.java` — the big one (7 steps)
5. `DungeonManageMenu.java` — edit/delete
6. `ActiveRunsPanel.java` — monitoring
7. Wire everything in `DungeonPlugin.java` + `DungeonAdminCommand.java`

---

## ESTIMATED SCOPE

- **New files:** 7 (MainMenu, PlayMenu, CreationWizard, ManageMenu, ActiveRuns, WizardState, WizardManager)
- **Modified files:** 2 (DungeonPlugin, DungeonAdminCommand)
- **New code:** ~1,500-2,000 lines
- **Build must pass:** `cd plugin && ./gradlew build`
