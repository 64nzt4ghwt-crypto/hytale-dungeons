# Codex Task: Hytale Dungeon Plugin Scaffold

Read these files for context:
- SPEC.md (feature requirements)
- API-RESEARCH.md (confirmed Hytale server APIs)
- IMPLEMENTATION-PLAN.md (full architecture + file structure)

Create the Java project at src/main/java/com/ourserver/dungeons/ following the file structure in IMPLEMENTATION-PLAN.md.

## Priority Files (create these first):
1. DungeonPlugin.java - main entry, init managers, register commands
2. manager/DungeonManager.java - singleton coordinator
3. manager/InstanceManager.java - instance lifecycle (create/destroy worlds)
4. model/DungeonDefinition.java + RoomDefinition.java - data classes
5. instance/DungeonInstance.java - active dungeon run
6. instance/RoomController.java - state machine (INACTIVE→ACTIVE→CLEARED→LOOTING)
7. instance/PartyManager.java - party tracking
8. command/DungeonCommand.java - /dungeon command handler
9. build.gradle - build config

## Key APIs (from API-RESEARCH.md):
- Universe.loadWorld()/unloadWorld() for instancing
- Player.teleport(world, position) for cross-world tp
- World.getEntityStore() + ECS pattern for mobs
- World.setBlock() for barriers
- CommandManager.registerCommand() for commands

## Rules:
- Add detailed comments explaining architecture decisions
- Use TODO comments for things needing Hytale-specific testing
- Follow the state machine pattern from IMPLEMENTATION-PLAN.md
- Keep it compilable against Hytale's plugin API structure
