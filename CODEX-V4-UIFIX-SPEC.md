# CODEX V4 — UI Fix Spec (CRASH FIX)

## THE PROBLEM
The game crashes with: `Selected element in CustomUI command was not found. Selector: dungeon.main.title`

**Root cause:** The Java GUI pages use `UICommandBuilder.set("dungeon.main.title", ...)` but:
1. There are NO `.ui` template files defining these elements
2. Hytale uses `#ElementId` selectors in `.ui` files, NOT dotted paths

## THE FIX

### Part 1: Create `.ui` template files

All `.ui` files go in: `plugin/src/main/resources/Common/UI/Custom/Pages/`

Hytale `.ui` format reference (from RPGLeveling mod):
- `$C = "../Common.ui";` — import common styles
- `Group #MyId { ... }` — container with ID
- `Label #MyLabel { Text: "hello"; Style: (FontSize: 14, TextColor: #ffffff); }` — text
- `$C.@TextButton #MyButton { Text: "Click"; Style: @CustomButtonStyle; }` — button
- `$C.@PageOverlay { ... }` — fullscreen page overlay
- `$C.@DecoratedContainer { ... }` — bordered panel
- `LayoutMode: Top;` — vertical stack
- `LayoutMode: Left;` — horizontal stack
- `LayoutMode: TopScrolling;` — scrollable vertical
- `Anchor: (Width: 900, Height: 700);` — fixed size
- `Padding: (Full: 20);` — padding
- `Background: #1a1a1a(0.95);` — background with alpha
- `Visible: true/false;` — visibility toggle

Button styles:
```
@CustomButtonStyle = TextButtonStyle(
  Default: (Background: $C.@DefaultSquareButtonDefaultBackground, LabelStyle: $C.@DefaultButtonLabelStyle),
  Hovered: (Background: $C.@DefaultSquareButtonHoveredBackground, LabelStyle: $C.@DefaultButtonLabelStyle),
  Pressed: (Background: $C.@DefaultSquareButtonPressedBackground, LabelStyle: $C.@DefaultButtonLabelStyle),
  Disabled: (Background: $C.@DefaultSquareButtonDisabledBackground, LabelStyle: $C.@DefaultButtonDisabledLabelStyle),
  Sounds: $C.@ButtonSounds,
);
```

### Part 2: Fix Java UICommandBuilder selectors

The Java code must use `#ElementId` format matching the `.ui` file element IDs.

Example — WRONG:
```java
commands.set("dungeon.main.title", "DUNGEON MASTER");
```

RIGHT:
```java
commands.set("#MainTitle", "DUNGEON MASTER");  // matches Label #MainTitle in .ui
```

Or if Hytale's UICommandBuilder uses plain IDs without #:
```java
commands.set("MainTitle", "DUNGEON MASTER");
```

Check the existing working HUD code in `DungeonHudService.java` to see which format works — it already successfully sets values. Use that same pattern.

### Part 3: Register UI pages properly

Look at how RPGLeveling registers its pages — the `.ui` file path must be passed to the page constructor or HUD constructor. Check the decompiled RPGLeveling source at `../../hytale-mod/decompiled/` for:
- `StatsGUIPage.java` — how it references its `.ui` file
- `LevelProgressHud.java` — how HUD references its `.ui` file
- `LevelProgressHudManager.java` — how HUD is managed

The key pattern from RPGLeveling's decompiled HUD:
```java
// The .ui file path is relative to Common/UI/Custom/
// Passed in constructor or set via a method
```

## FILES TO CREATE

### `plugin/src/main/resources/Common/UI/Custom/Pages/Dungeon_Main.ui`
Main menu with 4 buttons: Create, Play, Manage, Active Runs, Close.
Simple centered panel, big clear buttons.

### `plugin/src/main/resources/Common/UI/Custom/Pages/Dungeon_Play.ui`
List of available dungeons with Play/Info buttons. Active runs section with Join buttons.

### `plugin/src/main/resources/Common/UI/Custom/Pages/Dungeon_Wizard.ui`
7-step wizard. Progress indicator at top. Content area changes per step.
Back/Next buttons at bottom.

### `plugin/src/main/resources/Common/UI/Custom/Pages/Dungeon_Manage.ui`
List of dungeons with Edit/Enable/Disable/Delete buttons.

### `plugin/src/main/resources/Common/UI/Custom/Pages/Dungeon_Active.ui`
List of active dungeon runs with Spectate/Force End buttons.

### `plugin/src/main/resources/Common/UI/Custom/HUD/DungeonHud.ui`
Already partially exists in code. Make sure it has elements matching what DungeonHudService.java sets.

## FILES TO MODIFY

All Java files in `plugin/src/main/java/com/howlstudio/dungeons/ui/` that use `UICommandBuilder`:
- `DungeonMainMenu.java`
- `DungeonPlayMenu.java`
- `DungeonCreationWizard.java`
- `DungeonManageMenu.java`
- `ActiveRunsPanel.java`
- `DungeonHudService.java`
- `DungeonStatusPanel.java`
- `DungeonBrowserPage.java`

Fix ALL selectors to match the `.ui` file element IDs.

## CRITICAL
- `cd plugin && ./gradlew build` MUST pass
- The `.ui` files must be in `src/main/resources/` so they get bundled into the JAR
- Element IDs in `.ui` files must EXACTLY match what Java code references
- Test that the existing DungeonHud (which was working before V3) still works
- Use the RPGLeveling `.ui` files as direct style reference — dark backgrounds, clean fonts, clear buttons
