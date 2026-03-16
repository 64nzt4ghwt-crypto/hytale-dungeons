# Hytale Modding Research: Dungeon/RPG Systems

**Research Date:** 2026-02-08  
**Focus Areas:**
1. Hypixel Skyblock Dungeon System
2. MythicMobs / MythicDungeons
3. Hytale Community Mods on GitHub
4. Roguelike/Procedural Dungeon Generation

---

## 1. Hypixel Skyblock Dungeon System

### Overview
The **Catacombs** are the only dungeon type in Hypixel SkyBlock, consisting of randomly-generated rooms with mobs, secrets, traps, blessings, puzzles, and more. Players must defeat a specific boss at the end to earn rewards.

Source: https://wiki.hypixel.net/Catacombs

### Class System (5 Classes)

Each class has unique passive abilities, Dungeon Orb abilities, and ghost abilities (for when dead). **Key mechanic:** Having only one player of each class in a run DOUBLES their buffs.

#### Berserk (Melee DPS)
- **Role**: Melee damage dealer
- **Base Bonuses (solo doubled)**:
  - +40% melee damage (+80% when solo)
  - +30 speed (+60 when solo)
- **Passives**:
  - **Bloodlust**: Next hit after kill deals 20-40% more damage
  - **Lust for Blood**: Each hit increases damage to that enemy 15-45% (stackable)
  - **Weapon Master**: Hits up to 5 enemies in frontal cone
  - **Indomitable**: Gain defense as % of strength
- **Orb Abilities**:
  - **Throwing Axe**: 10s cooldown, deals same damage as highest hit in last minute
  - **Ragnarok**: 60s cooldown, grants +100 attack speed, +400 speed, 50% damage boost for 15s, summons 3 zombie minions
- **Ghost Abilities**: Buff Potion (+30 strength), Ghost Axe

#### Mage (Magic DPS)
- **Role**: Magic/ability damage
- **Base Bonuses (solo doubled)**:
  - +250 intelligence (+500 when solo)
  - +25-50% shorter ability cooldowns
- **Passives**:
  - **Mage Staff**: Converts melee to ranged attacks (30% damage), up to 15 blocks
  - **Efficient Spells**: Cooldown reduction 25-50%
- **Orb Abilities**:
  - **Guided Sheep**: 30s cooldown, acts as Superboom TNT
  - **Thunderstorm**: 500s cooldown, strikes enemies in 10-block radius for 15s
- **Ghost Abilities**: Instant Wall (5x3 barrier), Fireball

#### Healer (Support)
- **Role**: Healing and team support
- **Passives**:
  - **Renew**: +50-101% healing effectiveness
  - **Healing Aura**: Heals teammates within 8 blocks 1-2% HP/sec
  - **Revive**: Spawns fairy that revives dead teammates (100s cooldown)
  - **Soul Tether**: Create tether to teammate, both heal from hits
  - **Overheal**: Converts overhealing into absorption shield (20-40%)
- **Orb Abilities**:
  - **Healing Circle**: 2s cooldown, 2% HP/sec healing in area
  - **Wish**: 120s cooldown, heals group to full + shield

#### Archer (Ranged DPS)
- **Role**: Bow damage
- **Base Bonuses (solo doubled)**:
  - +150% arrow damage (+200% when solo)
  - -25% melee damage (trade-off)
- **Passives**:
  - **Doubleshot**: 50-100% chance to fire second arrow
  - **Bone Plating**: Reduces next hit by 100-600 damage
  - **Bouncy Arrows**: 0-100% chance to bounce to additional target
- **Orb Abilities**:
  - **Explosive Shot**: 40s cooldown, 3 explosive arrows, acts as Superboom TNT
  - **Rapid Fire**: 100s cooldown, 5 arrows/sec for 5-10 seconds

#### Tank (Defense)
- **Role**: Damage mitigation and aggro
- **Base Bonuses (solo doubled)**:
  - +50 defense (+100 when solo)
  - +100 HP (+200 when solo)
- **Passives**:
  - **Protective Barrier**: +25-35% permanent defense, shield when below 50% HP
  - **Taunt**: Increases mob target chance
  - **Diversion**: Takes 80% of damage for teammates within 30 blocks
  - **Defensive Stance**: Immunity to knockback
- **Orb Abilities**:
  - **Seismic Wave**: 15s cooldown, deals 20,000-1,420,000 + 10% per 50 defense
  - **Castle of Stone**: 150s cooldown, 70% damage reduction for 20s, aggros all enemies in 10 blocks

### Floor/Progression System
- **Floors I-VII**: Increasing difficulty with unique bosses
- **Master Mode**: Harder version unlocked per floor after completion
- **Dungeon Sizes**:
  - Tiny (4x4): Entrance, Floor I-II
  - Small (5x5): Floor III-IV
  - Medium (5x6, 6x6): Floor V-VI
  - Large (6x6): Floor VII

### Scoring System (4 Categories)

**Skill Score (20-100)**:
- Base 20 + 80*(rooms completed/total) - (10*failed puzzles) - (2*deaths)

**Exploration Score (0-100)**:
- 60*(rooms completed/total) + 40*(secrets found/secrets needed)
- Secrets needed: Floor I: 30%, Floor II: 40%, Floor III: 50%, Floor IV: 60%, Floor V: 70%, Floor VI: 85%, Floor VII: 100%

**Speed Score (0-100)**:
- Time limits vary by floor (8-14 mins for full score)
- Gradual point loss for going over time

**Bonus Score (0-17)**:
- Killing Crypt Undeads: +1 per kill (max +5)
- Killing Mimics: +2
- Mayor perks: +10

**Rankings**:
- S+: ≥300 points
- S: ≥270 points
- A: ≥230 points
- B: ≥160 points
- C: ≥100 points
- D: <100 points

### Puzzle System

**10 Puzzle Types**:
1. **Creeper Beams** - Connect Sea Lanterns to create beams through a Creeper (4 beams to solve)
2. **Three Weirdos** - Logic puzzle with lying/telling truth NPCs (choose correct chest)
3. **Tic Tac Toe** - Must TIE against AI (AI starts, you must respond correctly)
4. **Water Board** - 5 colored gates, 6 levers, control water flow to open all gates
5. **Teleport Maze** - 3x3 grid of sections, find path through teleport pads to reach chest
6. **Higher or Lower** - Kill 10 blazes in ascending/descending health order
7. **Boulder** - Push boxes in 7x6 grid to create path to chest (sokoban-style)
8. **Ice Fill** - Fill ice path without breaking pattern (3 layers)
9. **Ice Path** - Push invulnerable silverfish with TNT through ice maze
10. **Quiz** - 3 SkyBlock trivia questions

**Mechanics**:
- 2+ puzzles per run
- No duplicate puzzles in same run
- Failable puzzles can be reset with Architect's First Draft
- Higher floors have harder puzzle variants

### Secret System

**Types of Secrets**:
- Chests (containing items or Blessings)
- Ground items
- Redstone keys (unlock new areas)
- Bats (drop items when killed)
- Wither Essence (collectibles)

**Rewards Include**: Blessings, Spirit Leap (teleport to teammates), Decoy, Traps, Dungeon Chest Keys, Revive Stones

### Boss Mechanics (Floor VII Example - Wither Lords)

**4-Phase Boss Fight**:
1. **Maxor** - Fast, high damage
2. **Storm** - Lightning attacks
3. **Goldor** - Defensive, arrow puzzle mechanic
4. **Necron** - Final phase

Master Mode adds **Wither King** as final boss after Necron.

---

## 2. MythicMobs / MythicDungeons (Minecraft)

### Overview
MythicMobs is a premium Paper/Spigot plugin that enables creation of custom mobs and bosses with advanced skills, attributes, equipment, and AI behaviors.

Source: https://mythiccraft.io, https://git.lumine.io/mythiccraft/MythicMobs

### Key Features

**Custom Mob System**:
- YAML-based configuration
- Custom models, sounds, particles
- Complex AI behaviors
- Random spawn conditions
- Skill-based combat systems

**Skill System** (YAML Configuration):
```yaml
Skills:
  - skill{s=Fireball} @target ~onAttack
  - throw{v=10;h=5} @target
  - effect:particles{p=flame;amount=50} @self
  - damage{a=50} @target
  - message{m="&cThe boss prepares to attack!"} @playersinradius{r=30}
```

**Popular Mechanics**:
- Trigger-based skills (onAttack, onDamaged, onSpawn, onDeath, onTimer)
- Target selection (@target, @self, @playersinradius, @randomplayer)
- Movement abilities (jump, dash, teleport, leap)
- Area effects (AOE damage, zone control)
- Summoning mechanics (minion waves)
- Phase transitions (health thresholds change behavior)

### What Makes MythicMobs Popular

1. **Easy Configuration**: YAML format with extensive documentation
2. **Visual Feedback**: Particles, sounds, title messages
3. **Flexibility**: Works with custom models (ModelEngine, ItemsAdder)
4. **Skill Library**: Hundreds of pre-built skill effects
5. **Integration**: Hooks with other plugins (WorldGuard, Factions, etc.)
6. **Performance**: Optimized for large-scale mob encounters

---

## 3. Hytale Community Mods on GitHub

### Found Repositories (Search Results)

**Launcher/Mod Managers**:
- **HyPrism** (yyyumeniku/HyPrism) - TypeScript, 457 stars - Hytale launcher with mod management
- **Hytale Mod Manager** (andretini/Hytale_Mod_Manager) - Rust, 26 stars - Dioxus tool for Linux
- **HyUI** (Elliesaur/HyUI) - Java, 60 stars - UI library for Hytale modding
- **Butter Launcher** (vZylev/Butter-Launcher) - TypeScript - Multi-version manager
- **HyTaLauncher** (MerryJoyKey-Studio/HyTaLauncher) - C# - Unofficial launcher with mods manager
- **HyLauncher** (ArchDevs/HyLauncher) - Go - Unofficial launcher

**Server/Plugin Development**:
- **Hytale-Template** (OwnerAli/Hytale-Template) - Java, 13 stars - Mod/plugin template
- **Hytale-Plugin-Examples** (sammwyy/Hytale-Plugin-Examples) - Java - Practical plugin examples
- **FancyInnovations/HytalePlugins** - Java - Plugins and libraries
- **lithium-server** (lithium-clr/lithium-server) - C# - High-performance server rewrite
- **Hytale-Server-Unpacked** (Ranork/Hytale-Server-Unpacked) - Java - Unpacked server API docs

**Docker/Hosting**:
- **hytale-server-docker** (Hybrowse/hytale-server-docker) - Shell, 21 stars - Production-ready Docker with auto-updates
- **hytale-docker** (Slowline/hytale-docker) - Shell - Dockerized server with persistent storage
- **hytale-server-container** (deinfreu/hytale-server-container) - Shell - Alpine-based container

**Documentation/Community**:
- **Hytale-Modding-Docs** (Frontier-Modding/Hytale-Modding-Docs) - 34 stars - Community documentation
- **modtale** (Modtale/modtale) - TypeScript, 34 stars - Hytale Community Repository
- **HytaleModding/site** - MDX - Modding guides and docs website

**Gameplay Mods**:
- **HyFine** (Temxs27/HyFine) - Java, 10 stars - Optimization mod
- **HytaleHungerMod** (Aex12/HytaleHungerMod) - Java, 2 stars - Hunger system
- **hytale-basic-uis** (trouble-dev/hytale-basic-uis) - Java - Basic UI implementations
- **hytale-ui-plugin** (BungeeDEV/hytale-ui-plugin) - Java - IntelliJ plugin for .ui files

### Code Patterns Observed

**Java Plugin Structure**:
- Most mods use Java (expected for Hytale's Java-based server)
- Annotation-based command frameworks (blade by vaperion)
- Plugin.yml configuration standard

**UI Development**:
- .ui file format for interface definitions
- HyUI library for consistent UI components
- IntelliJ plugin available for syntax highlighting

**Server Architecture**:
- Plugin API expected to support Bukkit-style hooks
- C# rewrite projects showing interest in performance

---

## 4. Roguelike/Procedural Dungeon Generation

### Core Algorithms

#### Room-and-Corridor (Most Common)
1. Place rooms randomly (prevents overlap)
2. Connect rooms with corridors using spanning tree
3. Validate connectivity

Source: https://pcg.wikidot.com/pcg-algorithm:dungeon-generation

#### Binary Space Partitioning (BSP)
1. Recursively split space into regions
2. Place room in each leaf node
3. Connect sibling rooms

#### Drunkard's Walk (Cellular Automata)
1. Start at center, randomly walk
2. Carve out space as you go
3. Stop when target fullness reached

#### Cellular Automata (Cave Generation)
1. Start with random noise
2. Apply rules (e.g., wall if <4 wall neighbors, floor if ≥5)
3. Iterate until stable cave-like structure emerges

### Voxel-Specific Considerations

**3D Grid Navigation**:
- Use Bresenham/DDA for line drawing in 3D
- Amanatides-Woo algorithm for fast voxel traversal
- Source: https://www.redblobgames.com/grids/line-drawing.html

**Room Templates**:
- Hand-designed rooms with connection points
- Rotation/reflection to expand variety
- Mark "impassable" grids to control corridor entry points

**Verticality**:
- Stairs, ladders, holes between floors
- Multiple Z-levels increase complexity
- Consider accessibility (can player return?)

### Key Implementation Tips

**Connectivity Verification**:
- Spanning tree algorithm ensures all rooms reachable
- Flood-fill to validate every floor tile reachable
- Dead-end detection and removal

**Gameplay Integration**:
- Spawn points must be valid (player can stand)
- Loot distribution (rare items in hard-to-reach areas)
- Difficulty scaling with depth/progression
- Backtracking prevention (one-way doors, teleporters)

**Performance**:
- Chunk-based generation
- Lazy loading of distant areas
- Deterministic seeds for reproducibility

### Algorithm Comparison Table

| Algorithm | Pros | Cons | Best For |
|-----------|------|------|----------|
| Room-and-Corridor | Simple, controllable | May have disconnected rooms | Traditional RPG dungeons |
| BSP | Tidy, hierarchical | Predictable, less organic | Structured buildings |
| Cellular Automata | Organic, cave-like | Unpredictable shape | Natural caves, mines |
| Drunkard's Walk | Fast, simple | Irregular, messy | Quick testing, mazes |

---

## Key Takeaways for Hytale Dungeon Implementation

### From Hypixel Skyblock:
1. **Class variety is crucial** - 5 distinct roles creates replayability
2. **Solo bonuses incentivize diverse parties** - Double buffs when class is unique
3. **Puzzles add engagement** - 10 different puzzle types prevent monotony
4. **Scoring drives optimization** - Players will replay to get S+ ranks
5. **Secrets reward exploration** - Hidden chests, essences, keys
6. **Floor progression** - Difficulty tiers with unique bosses

### From MythicMobs:
1. **YAML configuration** - Human-readable, modder-friendly
2. **Trigger-based skills** - onAttack, onTimer, onPhaseChange
3. **Visual feedback** - Particles, sounds, title messages
4. **Phase transitions** - Boss behavior changes at HP thresholds
5. **Minion waves** - Adds complexity to boss fights

### From GitHub Analysis:
1. **Java ecosystem** - Expected primary language for plugins
2. **Template projects available** - Fork from Hytale-Template
3. **UI system exists** - .ui files with plugin support
4. **Server architecture flexible** - C# rewrites show performance concerns

### From Procedural Generation:
1. **Room templates with rotation** - Scales content variety
2. **Connectivity validation** - Never strand player
3. **Difficulty scaling** - Reward risk with better loot
4. **Deterministic seeds** - Enables speedrunning/leaderboards
5. **Verticality matters** - 3D space enables complex designs

---

## References

1. Hypixel SkyBlock Wiki: https://wiki.hypixel.net/Catacombs
2. Classes Overview: https://wiki.hypixel.net/Classes
3. Catacomb Puzzles: https://wiki.hypixel.net/Catacombs_Puzzles
4. MythicCraft: https://mythiccraft.io/
5. GitHub Hytale Topic: https://github.com/topics/hytale
6. Hytale Modding Docs: https://github.com/Frontier-Modding/Hytale-Modding-Docs
7. PCG Wiki Dungeon Generation: https://pcg.wikidot.com/pcg-algorithm:dungeon-generation
8. Red Blob Games Grid Algorithms: https://www.redblobgames.com/grids/line-drawing.html
9. RogueBasin Algorithms: http://www.roguebasin.com/

---

*Research compiled for Hytale Dungeon Mod Project*
