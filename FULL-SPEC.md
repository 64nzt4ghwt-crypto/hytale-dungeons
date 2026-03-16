# Dungeon Plugin Full Spec (from _uPsycho, #boss Discord 2/2/26)

## Dungeon Creation & World Management
- Admins drop world files into designated folder → register as dungeons
- Worlds manually assigned as dungeons and linked to portals
- Auto-generated dungeons with customizable params (room size, layout type, difficulty, themes)
- Commands:
  - `/dungeon register <world>`
  - `/dungeon unregister <world>`
  - `/dungeon setportal <dungeon>`

## Portal System
- Configurable dungeon portals that teleport players into specific dungeon instances
- Each portal linked to a manual dungeon world or auto-generated dungeon
- Option to reset/regenerate dungeon when all players leave or after completion

## Room & Progression System
- Rooms can be LOCKED until all mobs are defeated **(Important)**
- Walls or barriers DISAPPEAR once a room is cleared **(Important)**
- Supports timed rooms, puzzle rooms (alternative if mob rooms not possible), and boss rooms

## Loot & Reward System
- Customizable loot tables per room, dungeon, or boss
- Loot configured via JSON, GUI editor, or commands:
  - `/dungeon loot add <room> <item> <chance>`
- Supports rarity tiers and random rolls
- Integrate with "Better LootBox" mod or create new

## Dungeon Instance System
- Each dungeon runs in its own instance
- Multiple parties can run the same dungeon simultaneously
- Automatic cleanup and reset after completion or abandonment

## Admin & Debug Commands
- Full admin command suite:
  - `/dungeon create`
  - `/dungeon edit`
  - `/dungeon reload`
  - `/dungeon forceend`
- Debug mode to visualize spawn points, room boundaries, and trigger zones

## Dungeon GUI & HUD
- HUD displayed on top-right of screen showing:
  - Dungeon name
  - Time elapsed or remaining
  - Difficulty level (optional)
  - Optional GUI menu showing room progress, remaining mobs, and boss status

## Command: Add Mob Spawn Point
- `/dungeon mob add <dungeon> <entity> <position>`
- Position options:
  - `player` → uses command sender's current position
  - `coords <x> <y> <z>` → specific world coordinates
  - `facing` → uses block the player is looking at
- Optional parameters:
  - `amount <number>` — how many to spawn
  - `radius <number>` — spread radius
  - `wave <number>` — which wave
  - `chance <percentage>` — spawn probability
- Examples:
  - `/dungeon mob add ice_temple skeleton player amount 3 wave 1`
  - `/dungeon mob add ice_temple zombie coords 120 64 -32 radius 4`
  - `/dungeon mob add ice_temple ice_golem facing chance 50`

## Spawn Behavior
- Spawn points saved per dungeon and per room
- Mobs spawn when room is activated
- Spawn points reusable across waves
- If "player" is used, position locked at command execution time

## Alternative: GUI-based Setup
- Admin opens Dungeon Editor GUI
- Clicks "Add Spawn Point"
- Selects entity
- Clicks in-world to place spawn location
- Adjusts amount, waves, conditions via sliders/inputs

## Mod Compatibility & Extensibility
- Compatibility with other Hytale mods
- API hooks for custom mobs, loot, and room mechanics
- Configurable settings for mod integration or overrides
