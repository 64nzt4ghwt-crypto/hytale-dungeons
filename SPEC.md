# Hytale Dungeon System - Specification

*From Howl's partner - the vision for our RPG server*

## Overview
Minigames → RPG server with instanced dungeons, progression, loot, and party support.

---

## 1. Dungeon Creation & World Management

A feature that allows admins to drop world files into a designated folder and register them as dungeons.

- Worlds can be manually assigned as dungeons and linked to portals
- Support for auto-generated dungeons with customizable parameters:
  - Room size, layout type, difficulty, themes, etc.
- Commands to register, unregister, enable, or disable dungeon worlds:
  - `/dungeon register <world>`
  - `/dungeon unregister <world>`
  - `/dungeon setportal <dungeon>`

---

## 2. Portal System

- Configurable dungeon portals that teleport players into specific dungeon instances
- Each portal can be linked to a manual dungeon world or an auto-generated dungeon
- Option to reset or regenerate the dungeon when all players leave or after completion

---

## 3. Dungeon GUI & HUD

A dungeon HUD displayed on the top-right of the screen showing:
- Dungeon name
- Time elapsed or remaining
- Difficulty level (optional)

Optional GUI menu showing:
- Room progress
- Remaining mobs
- Boss status

---

## 4. Mob Spawn System

### Command: Add Mob Spawn Point
```
/dungeon mob add <dungeon> <entity> <position>
```

**Position Options:**
- `player` → Uses the command sender's current position
- `coords <x> <y> <z>` → Uses specific world coordinates
- `facing` → Uses the block the player is currently looking at

**Examples:**
```
/dungeon mob add ice_temple skeleton player
/dungeon mob add ice_temple skeleton coords 120 64 -32
/dungeon mob add ice_temple skeleton facing
```

**Optional Parameters:**
- `amount <number>` - how many to spawn
- `radius <number>` - spread radius
- `wave <number>` - which wave this spawn belongs to
- `chance <percentage>` - spawn probability

**Examples with options:**
```
/dungeon mob add ice_temple skeleton player amount 3 wave 1
/dungeon mob add ice_temple zombie coords 120 64 -32 radius 4
/dungeon mob add ice_temple ice_golem facing chance 50
```

**Behavior:**
- Spawn points are saved per dungeon and per room
- Mobs spawn when the room is activated
- Spawn points can be reused across waves
- If "player" is used, the position is locked at command execution time

**Alternative (GUI-based setup):**
1. Admin opens Dungeon Editor GUI
2. Clicks "Add Spawn Point"
3. Selects entity
4. Clicks in-world to place the spawn location
5. Adjusts amount, waves, and conditions via sliders or inputs

---

## 5. Room & Progression System

**IMPORTANT FEATURES:**
- Rooms can be locked until all mobs are defeated
- Walls or barriers disappear once a room is cleared

**Room Types:**
- Combat rooms (clear all mobs)
- Timed rooms (complete within time limit)
- Puzzle rooms (alternative to combat)
- Boss rooms

---

## 6. Loot & Reward System

Reference: Better LootBox mod (or create new)

- Customizable loot tables per room, dungeon, or boss
- Loot can be configured via JSON, GUI editor, or commands:
  - `/dungeon loot add <room> <item> <chance>`
- Supports rarity tiers and random rolls

---

## 7. Dungeon Instance System

- Each dungeon runs in its own instance
- Multiple parties can run the same dungeon simultaneously
- Automatic cleanup and reset after completion or abandonment

---

## 8. Admin & Debug Commands

Full admin command suite:
- `/dungeon create`
- `/dungeon edit`
- `/dungeon reload`
- `/dungeon forceend`

Debug mode to visualize:
- Spawn points
- Room boundaries
- Trigger zones

---

## 9. Mod Compatibility & Extensibility

- Compatibility with other Hytale mods
- API hooks for custom mobs, loot, and room mechanics
- Configurable settings for mod integration or overrides

---

## Implementation Priority

1. **Core:** World loading, instancing, portal teleportation
2. **Rooms:** Trigger zones, mob spawning, clear detection
3. **Progression:** Barrier removal, room locking
4. **HUD:** Basic dungeon info display
5. **Loot:** Configurable drops
6. **Polish:** GUI editor, debug tools, auto-generation
