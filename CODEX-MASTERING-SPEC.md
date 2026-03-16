# CODEX-MASTERING-SPEC.md — Dungeon Plugin Final Polish

## MISSION
Make this plugin feel **finished, polished, and impressive** to show a friend.
No dead buttons. No crashes. No "dev build" vibes. Ship quality.

## ABSOLUTE RULES
1. **DO NOT delete or rewrite existing files** — surgical edits ONLY
2. **DO NOT touch .ui files** — they are confirmed working and fragile
3. **BUILD MUST PASS**: `cd /Users/misa/.openclaw/workspace/projects/hytale-dungeons/plugin && JAVA_HOME=/Users/misa/java/jdk-21.0.10+7/Contents/Home ./gradlew build`
4. **No new .ui files** — HUD is disabled on purpose (crashes client). All new feedback goes through chat messages (player.sendMessage)
5. Study existing patterns in DungeonPlayMenu.java, ActiveRunsPanel.java, DungeonMainMenu.java — match their code style exactly

## WORKING STATE (as of now)
- BUILD SUCCESSFUL — 64 Java files, 8,286 lines
- 5 .ui pages: Main, Play, Manage, Active, Wizard — ALL working in-game
- Wizard creates dungeons via GUI form (name, description, difficulty, players, time limit)
- Play menu browses templates and joins active runs
- Manage menu edits/deletes templates (admin only)
- Active runs panel shows live dungeon status with auto-refresh
- HUD is disabled (DungeonHud.build() is commented out) — action bar text used instead via refreshHud()
- Completion broadcasts: "DUNGEON COMPLETE! Time: XX:XX" + kills/deaths
- Time warnings at 5min, 1min, 30sec, 10sec
- Boss announcements, wave progression, room clear messages
- Loot drops on room clear and final clear
- Disconnect/reconnect handling (60s timeout)
- Party system (join/leave), portals, barriers between rooms

## WHAT NEEDS FIXING

### 1. Add `/dhelp` Command
Create `DungeonHelpCommand.java` in the `commands` package.
Command name: `dhelp` (no args needed).

Output should be a clean help screen:
```
========= DUNGEON MASTER =========
/dungeon        Open the dungeon menu
/dcreate        Create a new dungeon (admin)
/djoin <id>     Join an active dungeon run
/dleave         Leave your current dungeon
/dlist          List available dungeon templates
/dinfo <id>     Show dungeon details
/dstatus        Show your current dungeon status
/dbrowse        Browse dungeons
/dhelp          Show this help
===================================
```

Register it in `DungeonPlugin.registerCommands()`.

Extend `BaseDungeonCommand` like all other commands do.

### 2. Enhanced Completion Summary
In `DungeonManager.completeInstance()`, after the existing "DUNGEON COMPLETE!" broadcast, add a detailed summary:

```java
// After existing broadcasts, add:
Msg.broadcast(players, Msg.title("=== RUN SUMMARY ==="));
Msg.broadcast(players, Msg.info("Dungeon: " + template.getName()));
Msg.broadcast(players, Msg.info("Difficulty: " + worldRegistry.getDifficulty(instance.getTemplateId()).toUpperCase()));
Msg.broadcast(players, Msg.info("Rooms cleared: " + (instance.getCurrentRoom() + 1) + "/" + template.getRooms().size()));
Msg.broadcast(players, Msg.info("Total kills: " + instance.getTotalKills()));
Msg.broadcast(players, Msg.info("Total deaths: " + instance.getTotalDeaths()));

// Room clear times
long[] roomTimes = instance.getRoomClearTimesMs();
for (int i = 0; i < roomTimes.length && i < template.getRooms().size(); i++) {
    DungeonTemplate.RoomTemplate r = template.getRooms().get(i);
    if (r != null && roomTimes[i] > 0) {
        Msg.broadcast(players, Msg.info("  " + r.getName() + ": " + formatTime((int)(roomTimes[i] / 1000L))));
    }
}

// Loot collected summary
Map<String, Integer> loot = instance.getLootCollected();
if (!loot.isEmpty()) {
    Msg.broadcast(players, Msg.loot("Loot collected:"));
    for (Map.Entry<String, Integer> entry : loot.entrySet()) {
        Msg.broadcast(players, Msg.loot("  " + entry.getKey() + " x" + entry.getValue()));
    }
}

Msg.broadcast(players, Msg.title("==================="));
```

### 3. Enhanced Fail Summary
In `DungeonManager.failInstance(UUID, String)`, after the existing fail broadcast, add:

```java
// After existing broadcasts, add:
Msg.broadcast(players, Msg.title("=== RUN FAILED ==="));
Msg.broadcast(players, Msg.info("Reached room " + (instance.getCurrentRoom() + 1) + "/" + template.getRooms().size()));
Msg.broadcast(players, Msg.info("Kills: " + instance.getTotalKills() + " | Deaths: " + instance.getTotalDeaths()));

long elapsedSeconds = instance.elapsedMillis() / 1000L;
Msg.broadcast(players, Msg.info("Time survived: " + formatTime((int) elapsedSeconds)));
Msg.broadcast(players, Msg.title("=================="));
```

(Note: `failInstance` needs to get the template for room count — add `DungeonTemplate template = templatesById.get(instance.getTemplateId());` if not already present.)

### 4. Welcome Message on Join
In `DungeonManager.joinInstance()`, after the successful join, send the joining player a welcome:

```java
// After player.sendMessage(Msg.success(...)) in joinInstance, add:
player.sendMessage(Msg.info("Difficulty: " + worldRegistry.getDifficulty(template.getId()).toUpperCase()));
player.sendMessage(Msg.info("Players: " + instance.getPlayers().size() + "/" + template.getMaxPlayers()));
player.sendMessage(Msg.info("Time limit: " + formatTime(template.getTimeLimitSeconds())));
player.sendMessage(Msg.info("Type /dleave to exit. Type /dstatus for your current dungeon info."));
```

### 5. Welcome Message on Create
In `DungeonManager.createInstance()`, after the successful creation broadcast, add:

```java
// After the creator is notified, add:
creator.sendMessage(Msg.info("Type /dstatus for live dungeon info."));
creator.sendMessage(Msg.info("Others can join with: /djoin " + instance.getId()));
```

### 6. Room Entry Announcement  
In `DungeonManager.advanceRoom()`, when entering a new room, enhance the message. Currently it just spawns the wave. After advancing and before spawning, add:

```java
DungeonTemplate.RoomTemplate newRoom = getCurrentRoom(template, instance);
if (newRoom != null) {
    String roomType = newRoom.isBoss() ? "BOSS" : newRoom.getRoomType().toUpperCase();
    Msg.broadcast(resolvePlayers(instance), Msg.title("--- ROOM " + (instance.getCurrentRoom() + 1) + ": " + newRoom.getName() + " ---"));
    Msg.broadcast(resolvePlayers(instance), Msg.info("Type: " + roomType + " | Waves: " + newRoom.getWaveCount()));
}
```

### 7. `/dungeon` Opens GUI (No Args Shortcut)
Check `DungeonCreateCommand.java` — if the player types `/dungeon` with no args, it should open the main GUI menu instead of showing usage text. This makes the GUI the primary interface.

Look at how DungeonCreateCommand handles the base `/dungeon` command. If it shows "Usage: ..." when no subcommand is given, change it to open the DungeonMainMenu instead:

```java
// In the handler for no-args /dungeon:
player.getPageManager().openCustomPage(ref, store, new DungeonMainMenu(playerRef, manager, wizardManager));
```

If this isn't possible due to command structure (each command is separate), then add a note explaining why and skip this task.

### 8. Action Bar HUD Text (Replace Disabled CustomUI HUD)
The `DungeonHud.build()` method is commented out because HUD .ui files crash the client. But `DungeonHudService.showOrUpdate()` still calls `hud.refresh()` which calls the empty `build()`.

Replace the HUD approach: instead of trying to render a CustomUI HUD, use the **action bar** (the text that appears above the hotbar) to show dungeon status. 

In `DungeonHudService.showOrUpdate()`, instead of using `DungeonHud`, send an action bar message to the player:

```java
// Replace the computeIfAbsent + setData + refresh pattern with:
String statusLine = roomName + " | Mobs: " + mobsLeft + " | " + timer + " | Party: " + partySize;
if (isBoss) statusLine = "[BOSS] " + statusLine;
player.sendActionBarMessage(Message.raw(statusLine).color(Msg.PRIMARY));
```

**IMPORTANT**: Check if `Player.sendActionBarMessage()` exists in the Hytale API. If not, check for `Player.setActionBar()` or `Player.getActionBarMessage()`. If NO action bar API exists, keep the existing pattern but use `player.sendMessage()` with a throttle (only send every 3 seconds to avoid spam).

If using sendMessage as fallback, add a `Map<UUID, Long> lastMessageTime` to DungeonHudService and only send if 3+ seconds have passed since last message for that player.

## FILE INVENTORY (do not modify files not listed here)

### New files to create:
- `src/main/java/com/howlstudio/dungeons/commands/DungeonHelpCommand.java`

### Files to surgically edit:
- `src/main/java/com/howlstudio/dungeons/DungeonPlugin.java` — register DungeonHelpCommand
- `src/main/java/com/howlstudio/dungeons/manager/DungeonManager.java` — enhanced completion/fail summaries, welcome messages, room announcements
- `src/main/java/com/howlstudio/dungeons/ui/DungeonHudService.java` — action bar text instead of broken HUD overlay

### DO NOT TOUCH:
- Any .ui files in resources/
- manifest.json
- WizardState.java (it works, don't break it)
- DungeonCreationWizard.java (it works, don't break it)
- DungeonMainMenu.java (it works, don't break it)
- DungeonPlayMenu.java (it works, don't break it)
- DungeonManageMenu.java (it works, don't break it)
- ActiveRunsPanel.java (it works, don't break it)

## VERIFICATION
After all changes, run:
```bash
cd /Users/misa/.openclaw/workspace/projects/hytale-dungeons/plugin && JAVA_HOME=/Users/misa/java/jdk-21.0.10+7/Contents/Home ./gradlew build
```

Must output `BUILD SUCCESSFUL`. If it fails, fix compilation errors before finishing.

## CODE STYLE
- Match existing patterns exactly (see DungeonPlayMenu.java for reference)
- Use `Msg.*` helpers for all player messages
- Use `synchronized` on DungeonManager methods
- Null-check everything — Hytale API returns null freely
- Keep imports organized (java.*, com.hypixel.*, com.howlstudio.*)
