# Hytale NPC Roles - Complete Reference
## Extracted from Assets.zip → Server/NPC/Roles/

> **Generated:** 2026-02-07 | **Source:** `Assets.zip` (build-7) + `HytaleServer.jar` analysis
> **Total role JSON files:** 414 (including patrol/wander/dungeon variants)
> **Unique spawnable roles (excl. _Core/Templates):** ~270

---

## How Roles Work

### Role Name = JSON Filename (minus .json)
The string you pass to `setRoleName()` is the **filename without extension**. The game resolves the role by scanning `Server/NPC/Roles/` recursively.

```java
npc.setRoleName("Skeleton_Fighter");     // → Server/NPC/Roles/Undead/Skeleton/Skeleton/Skeleton_Fighter.json
npc.setRoleName("Trork_Warrior");        // → Server/NPC/Roles/Intelligent/Aggressive/Trork/Trork_Warrior.json
npc.setRoleName("Dragon_Fire");          // → Server/NPC/Roles/Boss/Dragon_Fire.json
```

### Role JSON Structure
Every role is a **Variant** that references a template and overrides/adds properties:

```json
{
  "Type": "Variant",
  "Reference": "Template_Intelligent",   // base template (defines AI state machine)
  "Parameters": { ... },                  // configurable params with defaults
  "Modify": {
    "MaxHealth": 36,                      // ← health
    "MaxSpeed": 6,                        // ← movement speed
    "Appearance": "Skeleton_Fighter",     // ← model asset key (for ModelAsset.getAssetMap())
    "DropList": "Drop_Skeleton_Fighter",  // ← loot table reference
    "Attack": "Root_NPC_Skeleton_Fighter_Attack",  // ← attack interaction reference
    "AttackDistance": 2.5,                // ← melee range
    "ViewRange": 15,                      // ← AI sight range
    "HearingRange": 10,                   // ← AI hearing range
    "LeashDistance": 40,                  // ← max chase distance from home
    "Weapons": ["Weapon_Sword_Steel"],    // ← held items
    "_InteractionVars": {                 // ← damage definition
      "Melee_Damage": {
        "Interactions": [{
          "Parent": "NPC_Attack_Melee_Damage",
          "DamageCalculator": {
            "BaseDamage": { "Physical": 23 }
          }
        }]
      }
    }
  }
}
```

### Base Templates (AI Behavior)
| Template | Behavior | Used By |
|----------|----------|---------|
| `Template_Intelligent` | Full AI: idle/patrol/alert/combat/flee/sleep | Skeletons, Goblins, Scaraks, Outlanders, Trorks |
| `Template_Predator` | Hunt/threaten/chase/combat | Bears, Wolves, Crocodiles, Emberwulf, Void Crawlers |
| `Template_Animal_Neutral` | Passive until provoked, flee | Bison, Boar, Cow, Moose, etc. |
| `Template_Beasts_Passive_Critter` | Passive small creatures | Frogs, Gecko, Mouse, Squirrel |
| `Template_Beasts_Passive_Cactee` | Passive mythic beast | Cactee |
| `Template_Birds_Passive` | Flying passive AI | All birds |
| `Template_Swimming_Passive` | Passive aquatic | Fish, Jellyfish |
| `Template_Swimming_Aggressive` | Aggressive aquatic | Sharks, Piranhas |
| `Template_Spirit` | Elemental spirit AI | Spirit_Ember, Spirit_Frost, etc. |
| `Template_Aggressive_Zombies` | Zombie-specific aggro AI | All zombies |
| `Template_Temple` | Temple safe-zone NPCs | Temple_ variants |
| `Template_Placeholder` | Minimal/stub | Dragons (WIP bosses) |
| `Template_Trork_Melee` | Trork melee specialist | Trork_Warrior, Trork_Guard, etc. |
| `Template_Trork_Ranger` | Trork ranged specialist | Trork_Hunter, Trork_Sentry |
| `Template_Trork_Mage` | Trork magic user | Trork_Shaman, Trork_Doctor_Witch |

---

## Complete Role List by Category

### 🐉 BOSSES (Server/NPC/Roles/Boss/)
| Role Name | HP | Notes |
|-----------|-----|-------|
| `Dragon_Fire` | 400 | Fire dragon (Template_Placeholder - WIP) |
| `Dragon_Frost` | 400 | Frost dragon (Template_Placeholder - WIP) |

### 🗡️ Boss-Tier / Mini-Boss NPCs
| Role Name | HP | Category | Notes |
|-----------|-----|----------|-------|
| `Goblin_Duke` | 226 | Goblin | Multi-phase boss (has Phase_2, Phase_3_Fast, Phase_3_Slow) |
| `Goblin_Ogre` | ~170+ | Goblin | Heavy goblin |
| `Shadow_Knight` | 400 | Undead | 119 physical dmg, uses Hound_Bleached anims |
| `Risen_Knight` | ~150+ | Undead/Skeleton | Elite skeleton |
| `Scarak_Broodmother` | ~145+ | Scarak | Insect queen |
| `Dungeon_Scarak_Broodmother` | 145 | Scarak | Dungeon version |
| `Rex_Cave` | ~200+ | Reptile | Cave T-Rex |
| `Yeti` | ~150+ | Mythic | Snow beast |
| `Emberwulf` | 193 | Mythic | Fire wolf |
| `Golem_Guardian_Void` | ~150+ | Elemental | Void guardian |

### 💀 UNDEAD — Skeletons (Standard)
| Role Name | HP | Weapons | Type |
|-----------|-----|---------|------|
| `Skeleton` | - | - | Base skeleton (generic) |
| `Skeleton_Fighter` | 36 | Random (droplist) | Melee, speed 6 |
| `Skeleton_Fighter_Patrol` | - | - | Patrol variant |
| `Skeleton_Fighter_Wander` | - | - | Wander variant |
| `Skeleton_Knight` | 74 | Sword + Shield | Melee tank |
| `Skeleton_Archer` | ~36 | Bow | Ranged |
| `Skeleton_Ranger` | ~49 | Bow | Ranged |
| `Skeleton_Mage` | ~36 | Staff | Magic ranged |
| `Skeleton_Archmage` | ~49 | Staff | Elite magic |
| `Skeleton_Scout` | ~29 | Light | Fast/stealth |
| `Skeleton_Soldier` | ~49 | Sword | Standard melee |
| `Skeleton_Praetorian` | - | Heavy | Elite (see Balancing/CAE_Skeleton_Praetorian) |

> Each type has `_Patrol` and `_Wander` variants (same mob, different idle behavior).

### 💀 UNDEAD — Skeleton Variants (Biome-Specific)
**Burnt Skeletons** (Zone 3 / Lava):
- `Skeleton_Burnt_Alchemist`, `Skeleton_Burnt_Archer`, `Skeleton_Burnt_Gunner`
- `Skeleton_Burnt_Knight`, `Skeleton_Burnt_Lancer`, `Skeleton_Burnt_Praetorian`
- `Skeleton_Burnt_Soldier`, `Skeleton_Burnt_Wizard`
- (each has `_Patrol` and `_Wander` variants)

**Frost Skeletons** (Zone 3 / Ice):
- `Skeleton_Frost_Archer`, `Skeleton_Frost_Archmage`, `Skeleton_Frost_Fighter`
- `Skeleton_Frost_Knight`, `Skeleton_Frost_Mage`, `Skeleton_Frost_Ranger`
- `Skeleton_Frost_Scout`, `Skeleton_Frost_Soldier`
- (each has `_Patrol` and `_Wander` variants)

**Sand Skeletons** (Zone 2 / Desert):
- `Skeleton_Sand_Archer`, `Skeleton_Sand_Archmage`, `Skeleton_Sand_Assassin`
- `Skeleton_Sand_Guard`, `Skeleton_Sand_Mage`, `Skeleton_Sand_Ranger`
- `Skeleton_Sand_Scout`, `Skeleton_Sand_Soldier`
- (each has `_Patrol`, `_Wander`, and some have `Dungeon/Dungeon_` variants)

**Incandescent Skeletons** (special glowing variant):
- `Skeleton_Incandescent_Fighter`, `Skeleton_Incandescent_Footman`
- `Skeleton_Incandescent_Head`, `Skeleton_Incandescent_Mage`
- (each has `_Patrol` and `_Wander` variants)

**Pirate Skeletons**:
- `Skeleton_Pirate_Captain`, `Skeleton_Pirate_Gunner`, `Skeleton_Pirate_Striker`
- (each has `_Patrol` and `_Wander` variants)

### 💀 UNDEAD — Zombies
| Role Name | HP | Damage | Notes |
|-----------|-----|--------|-------|
| `Zombie` | 49 | 18 phys | Standard zombie |
| `Zombie_Burnt` | ~49 | - | Fire variant |
| `Zombie_Frost` | ~49 | - | Ice variant |
| `Zombie_Sand` | ~49 | - | Desert variant |
| `Zombie_Aberrant` | ~61 | - | Mutated zombie |
| `Zombie_Aberrant_Big` | ~81 | - | Large mutant |
| `Zombie_Aberrant_Small` | ~36 | - | Small mutant |

### 💀 UNDEAD — Other
| Role Name | HP | Notes |
|-----------|-----|-------|
| `Ghoul` | ~74 | Undead creature |
| `Werewolf` | ~103 | Undead beast |
| `Wraith` | ~61 | Ghost-type |
| `Wraith_Lantern` | ~74 | Ghost with lantern |
| `Hound_Bleached` | ~61 | Undead dog |
| `Horse_Skeleton` | ~103 | Skeleton mount |
| `Horse_Skeleton_Armored` | ~126 | Armored skeleton mount |
| `Chicken_Undead` | 61 | Undead chicken (speed 10!) |
| `Cow_Undead` | 124 | Undead cow (speed 10!) |
| `Pig_Undead` | ~61 | Undead pig |
| `Risen_Knight` | - | Elite undead knight |
| `Shadow_Knight` | 400 | Mini-boss, 119 damage |

### 👹 GOBLINS (Intelligent/Aggressive/Goblin/)
| Role Name | HP | Notes |
|-----------|-----|-------|
| `Goblin_Duke` | 226 | Boss, multi-phase |
| `Goblin_Duke_Phase_2` | - | Phase 2 variant |
| `Goblin_Duke_Phase_3_Fast` | - | Phase 3 fast variant |
| `Goblin_Duke_Phase_3_Slow` | - | Phase 3 slow variant |
| `Goblin_Ogre` | ~170 | Heavy brute |
| `Goblin_Hermit` | - | Reclusive goblin |
| `Goblin_Lobber` | - | Ranged (throws) |
| `Goblin_Lobber_Patrol` | - | Patrol variant |
| `Goblin_Miner` | - | Mining goblin |
| `Goblin_Miner_Patrol` | - | Patrol variant |
| `Goblin_Scavenger` | - | Loot-seeking goblin |
| `Goblin_Scavenger_Battleaxe` | - | Battleaxe variant |
| `Goblin_Scavenger_Sword` | - | Sword variant |
| `Goblin_Scrapper` | - | Melee fighter |
| `Goblin_Scrapper_Patrol` | - | Patrol variant |
| `Goblin_Thief` | - | Stealth goblin |
| `Goblin_Thief_Patrol` | - | Patrol variant |
| `Edible_Goblin_Scrapper` | 100 | Special (food item related?) |
| `Edible_Rat` | 100 | Goblin camp rat |

### 🪖 TRORKS (Intelligent/Aggressive/Trork/)
| Role Name | HP | Damage | Notes |
|-----------|-----|--------|-------|
| `Trork_Warrior` | 61 | 23 phys | Battleaxe melee |
| `Trork_Warrior_Patrol` | - | - | Patrol variant |
| `Trork_Brawler` | ~74 | - | Unarmed fighter |
| `Trork_Guard` | ~81 | - | Defensive melee |
| `Trork_Mauler` | ~81 | - | Heavy melee |
| `Trork_Chieftain` | ~103 | - | Leader/mini-boss |
| `Trork_Hunter` | ~49 | - | Ranged hybrid |
| `Trork_Sentry` | ~49 | - | Ranged guard |
| `Trork_Sentry_Patrol` | - | - | Patrol variant |
| `Trork_Shaman` | ~36 | - | Magic/healer |
| `Trork_Doctor_Witch` | ~49 | - | Magic variant |
| `Trork_Unarmed` | ~29 | - | Civilian trork |
| `Wolf_Trork_Hunter` | ~61 | - | Trork's companion wolf |
| `Wolf_Trork_Shaman` | ~61 | - | Shaman's companion wolf |

### ⚔️ OUTLANDERS (Intelligent/Aggressive/Outlander/)
| Role Name | HP | Damage | Notes |
|-----------|-----|--------|-------|
| `Outlander_Berserker` | 103 | 27 phys | Dual axes, aggressive |
| `Outlander_Brute` | ~126 | - | Heavy tank |
| `Outlander_Cultist` | ~81 | - | Dark magic user |
| `Outlander_Hunter` | ~74 | - | Ranged/melee hybrid |
| `Outlander_Marauder` | ~81 | - | Raider |
| `Outlander_Peon` | ~49 | - | Foot soldier |
| `Outlander_Priest` | ~61 | - | Healer/support |
| `Outlander_Sorcerer` | ~49 | - | Magic ranged |
| `Outlander_Stalker` | ~74 | - | Stealth assassin |
| `Wolf_Outlander_Priest` | ~61 | - | Priest's wolf companion |
| `Wolf_Outlander_Sorcerer` | ~61 | - | Sorcerer's wolf companion |

### 🦂 SCARAKS (Intelligent/Aggressive/Scarak/)
**Overworld:**
| Role Name | HP | Notes |
|-----------|-----|-------|
| `Scarak_Broodmother` | ~145 | Queen/boss |
| `Scarak_Defender` | ~103 | Tank |
| `Scarak_Defender_Patrol` | - | Patrol variant |
| `Scarak_Fighter` | ~81 | Standard melee |
| `Scarak_Fighter_Patrol` | - | Patrol variant |
| `Scarak_Fighter_Royal_Guard` | ~103 | Elite fighter |
| `Scarak_Louse` | ~21 | Tiny swarmer |
| `Scarak_Seeker` | ~61 | Scout |
| `Scarak_Seeker_Patrol` | - | Patrol variant |

**Dungeon Variants** (pre-made for Hytale's built-in dungeons):
- `Dungeon_Scarak_Broodmother` (145 HP)
- `Dungeon_Scarak_Broodmother_Young` (124 HP)
- `Dungeon_Scarak_Defender` (103 HP)
- `Dungeon_Scarak_Defender_Patrol` (158 HP)
- `Dungeon_Scarak_Fighter` (81 HP, speed 7)
- `Dungeon_Scarak_Fighter_Patrol`
- `Dungeon_Scarak_Louse` (21 HP)
- `Dungeon_Scarak_Seeker` (61 HP)
- `Dungeon_Scarak_Seeker_Patrol` (63 HP)

### 🌿 HEDERA
| Role Name | HP | Notes |
|-----------|-----|-------|
| `Hedera` | ~81 | Plant creature, aggressive |

### 🌿 NEUTRAL INTELLIGENT

**Ferans** (cat-folk):
- `Feran_Burrower` (61 HP), `Feran_Civilian` (49 HP), `Feran_Cub` (29 HP)
- `Feran_Longtooth` (~74 HP), `Feran_Sharptooth` (~61 HP), `Feran_Windwalker` (~61 HP)

**Kweebecs** (tree-folk):
- `Kweebec_Elder` (103 HP), `Kweebec_Merchant`, `Kweebec_Prisoner`
- `Kweebec_Razorleaf` (~61 HP), `Kweebec_Razorleaf_Patrol`
- `Kweebec_Rootling`, `Kweebec_Sapling`, `Kweebec_Sapling_Orange`, `Kweebec_Sapling_Pink`
- `Kweebec_Seedling`, `Kweebec_Sproutling`, `Kweebec_Sproutling_Patrol`

**Bramblekin:**
- `Bramblekin` (103 HP), `Bramblekin_Shaman` (103 HP)

**Tuluk:**
- `Tuluk_Fisherman`

### 🏪 PASSIVE INTELLIGENT (NPCs/Merchants)
- `Klops_Gentleman`, `Klops_Merchant`, `Klops_Merchant_Patrol`, `Klops_Merchant_Wandering`
- `Klops_Miner`, `Klops_Miner_Patrol`
- `Quest_Master`
- `Slothian`

### 🏛️ TEMPLE VARIANTS (peaceful versions for safe zones)
- `Temple_Bluebird`, `Temple_Bunny`, `Temple_Deer_Doe`, `Temple_Deer_Stag`
- `Temple_Duck`, `Temple_Feran`, `Temple_Feran_Longtooth`
- `Temple_Finch_Green`, `Temple_Frog_Blue`, `Temple_Frog_Green`, `Temple_Frog_Orange`
- `Temple_Klops`, `Temple_Klops_Merchant`
- `Temple_Kweebec`, `Temple_Kweebec_Elder`, `Temple_Kweebec_Merchant`
- `Temple_Kweebec_Razorleaf`, `Temple_Kweebec_Razorleaf_Patrol` (+ Patrol1-5)
- `Temple_Kweebec_Rootling_Static`, `Temple_Kweebec_Seedling`, `Temple_Kweebec_Seedling_Static`
- `Temple_Kweebec_Static`, `Temple_Mithril_Guard`
- `Temple_Owl_Brown`, `Temple_Squirrel`

### 🐺 CREATURES — Mammals (Predators/Neutral)
| Role Name | HP | Speed | Behavior |
|-----------|-----|-------|----------|
| `Bear_Grizzly` | 124 | 6 | Predator |
| `Bear_Polar` | 103 | 7 | Predator |
| `Wolf_Black` | ~74 | - | Predator/pack |
| `Wolf_White` | ~49 | - | Predator/pack |
| `Hyena` | ~74 | - | Predator |
| `Fox` | ~49 | - | Predator |
| `Leopard_Snow` | ~103 | - | Predator |
| `Tiger_Sabertooth` | ~126 | - | Predator |
| `Antelope` | 81 | 8 | Neutral/flee |
| `Deer_Doe` | 81 | 10 | Prey |
| `Deer_Stag` | 103 | 10 | Prey |
| `Moose_Bull` | ~126 | - | Neutral/defensive |
| `Moose_Cow` | ~103 | - | Neutral |
| `Mosshorn` | ~81 | - | Neutral mythic |
| `Mosshorn_Plain` | ~61 | - | Neutral mythic variant |
| `Armadillo` | 103 | - | Passive |

### 🐄 CREATURES — Livestock
- `Bison` / `Bison_Calf`, `Boar` / `Boar_Piglet`, `Bunny`
- `Camel` / `Camel_Calf`, `Chicken` / `Chicken_Chick`
- `Chicken_Desert` / `Chicken_Desert_Chick`
- `Cow` / `Cow_Calf`, `Goat` / `Goat_Kid`
- `Horse` / `Horse_Foal`, `Mouflon` / `Mouflon_Lamb`
- `Pig` / `Pig_Piglet`, `Pig_Wild` / `Pig_Wild_Piglet`
- `Rabbit`, `Ram` / `Ram_Lamb`, `Sheep` / `Sheep_Lamb`
- `Skrill` / `Skrill_Chick`, `Turkey` / `Turkey_Chick`
- `Warthog` / `Warthog_Piglet`

### 🦎 CREATURES — Mythic
| Role Name | HP | Speed | Notes |
|-----------|-----|-------|-------|
| `Cactee` | 61 | - | Cactus creature |
| `Emberwulf` | 193 | 8 | Fire wolf (mini-boss tier) |
| `Fen_Stalker` | 74 | 10 | Swamp predator |
| `Hatworm` | - | - | Passive/ambient |
| `Snapdragon` | ~126 | - | Aggressive plant-dragon |
| `Spark_Living` | - | - | Living spark |
| `Trillodon` | - | - | Triceratops-like |
| `Yeti` | ~150 | - | Snow beast (mini-boss tier) |

### 🦎 CREATURES — Reptiles
- `Crocodile` (145 HP, speed 4), `Lizard_Sand`
- `Raptor_Cave` (~81 HP), `Rex_Cave` (~200+ HP, mini-boss)
- `Toad_Rhino` (~103 HP), `Toad_Rhino_Magma` (~103 HP)
- `Tortoise`

### 🕷️ CREATURES — Vermin
- `Spider` (~61 HP), `Spider_Cave` (~74 HP)
- `Scorpion` (~61 HP), `Rat` (~29 HP), `Molerat` (~49 HP)
- `Snake_Cobra`, `Snake_Marsh`, `Snake_Rattle`
- `Larva_Silk`, `Slug_Magma`, `Snail_Frost`, `Snail_Magma`

### 🐸 CREATURES — Critters (tiny passive)
- `Frog_Blue`, `Frog_Green`, `Frog_Orange`
- `Gecko`, `Meerkat`, `Mouse`, `Squirrel`

### ⚗️ ELEMENTALS
**Golems:**
- `Golem_Crystal_Earth`, `Golem_Crystal_Flame`, `Golem_Crystal_Frost`
- `Golem_Crystal_Sand`, `Golem_Crystal_Thunder`
- `Golem_Firesteel`, `Golem_Guardian_Void`

**Spirits:**
- `Spirit_Ember`, `Spirit_Frost`, `Spirit_Root`, `Spirit_Thunder`

### 🌀 VOID CREATURES
| Role Name | HP | Speed | Notes |
|-----------|-----|-------|-------|
| `Crawler_Void` | 74 | 8 | Fast void predator |
| `Eye_Void` | 61 | - | Flying void eye |
| `Larva_Void` | - | - | Small void creature |
| `Spawn_Void` | - | - | Void spawn point |
| `Spectre_Void` | - | - | Ghost-like void entity |

### 🐟 AQUATIC
**Freshwater:** Bluegill, Catfish, Frostgill, Minnow, Pike, Piranha, Piranha_Black, Salmon, Snapjaw, Trout_Rainbow
**Marine:** Clownfish, Crab, Jellyfish (Blue/Cyan/Green/Man_Of_War/Red/Yellow), Lobster, Pufferfish, Tang (Blue/Chevron/Lemon_Peel/Sailfin)
**Abyssal:** Eel_Moray, Shark_Hammerhead, Shellfish_Lava, Trilobite, Trilobite_Black, Whale_Humpback

### 🐦 AVIAN
**Aerial:** Bat, Bat_Ice, Bluebird, Crow, Finch_Green, Flamingo, Owl_Brown, Owl_Snow, Parrot, Penguin, Raven, Sparrow, Woodpecker
**Fowl:** Duck, Pigeon
**Raptor:** Archaeopteryx, Hawk, Pterodactyl, Tetrabird, Vulture

### 📦 SPECIAL
- `Empty_Role` — Blank role (HP 100, no AI, no appearance)

---

## Key Role Properties Explained

### What `setRoleName()` Configures
When you call `npc.setRoleName("Skeleton_Fighter")`, the game loads:
1. **Health** (`MaxHealth`) — how much HP the mob spawns with
2. **Speed** (`MaxSpeed`) — movement speed
3. **AI Behavior** — from the template chain (idle, patrol, alert, combat, flee, sleep states)
4. **Appearance** (`Appearance`) — which `ModelAsset` to use (model + texture)
5. **Drops** (`DropList`) — what items drop on death
6. **Combat** — attack type, damage, range, cooldowns
7. **Perception** — `ViewRange`, `HearingRange`, `ViewSector`
8. **Leashing** — `LeashDistance`, `HardLeashDistance` (how far they chase)
9. **Group behavior** — `CombatMessageTargetGroups`, `AttitudeGroup`, flock arrays

### Appearance ↔ Model Asset Mapping
The `Appearance` field maps to a `ModelAsset` key. Models live at:
```
Common/NPC/<Category>/<Name>/Models/Model.blockymodel
Common/NPC/<Category>/<Name>/Models/Texture.png
```
Example: `Appearance: "Skeleton_Fighter"` → `Common/NPC/Undead/Skeleton/Models/` (shared skeleton model with variant textures)

### Damage System
Damage is defined in `_InteractionVars.Melee_Damage` or via `_CombatConfig` (CAE system):
```json
"_InteractionVars": {
  "Melee_Damage": {
    "Interactions": [{
      "Parent": "NPC_Attack_Melee_Damage",
      "DamageCalculator": {
        "BaseDamage": { "Physical": 23 },
        "RandomPercentageModifier": 0.1  // ±10% variance
      }
    }]
  }
}
```

### Known Damage Values
| Role | Physical Damage |
|------|----------------|
| Shadow_Knight | 119 |
| Outlander_Berserker | 27 |
| Trork_Warrior | 23 |
| Zombie | 18 |

---

## Dungeon-Relevant Roles (Recommended for Plugin)

### Best Combat Mobs for Dungeon Waves
**Tier 1 — Easy (Trash Mobs):**
- `Skeleton_Fighter` (36 HP) — basic melee skeleton
- `Skeleton_Scout` (~29 HP) — fast, low HP
- `Zombie` (49 HP) — slow, hits hard
- `Scarak_Louse` (~21 HP) — tiny swarmer
- `Rat` (~29 HP) — filler mob

**Tier 2 — Medium:**
- `Skeleton_Soldier` (~49 HP) — standard fighter
- `Skeleton_Archer` (~36 HP) — ranged
- `Skeleton_Mage` (~36 HP) — magic ranged
- `Trork_Warrior` (61 HP, 23 dmg) — solid melee
- `Goblin_Scrapper` — melee goblin
- `Scarak_Fighter` (~81 HP) — bug melee

**Tier 3 — Hard:**
- `Skeleton_Knight` (74 HP, sword+shield) — tanky
- `Skeleton_Archmage` (~49 HP) — elite caster
- `Trork_Guard` (~81 HP) — defensive melee
- `Outlander_Berserker` (103 HP, 27 dmg) — aggressive dual-axe
- `Scarak_Defender` (~103 HP) — bug tank

**Tier 4 — Mini-Boss:**
- `Trork_Chieftain` (~103 HP) — trork leader
- `Goblin_Ogre` (~170 HP) — heavy brute
- `Scarak_Broodmother` (~145 HP) — insect queen
- `Emberwulf` (193 HP) — fire wolf
- `Bear_Grizzly` (124 HP) — nature boss

**Tier 5 — Boss:**
- `Shadow_Knight` (400 HP, 119 dmg) — undead boss
- `Goblin_Duke` (226 HP, multi-phase) — goblin king
- `Dragon_Fire` (400 HP) — dragon (WIP/placeholder AI)
- `Dragon_Frost` (400 HP) — dragon (WIP/placeholder AI)

### Pre-Made Dungeon Roles (Hytale's Own!)
The game already has dungeon-specific variants! These are tuned differently from overworld versions:
- `Dungeon_Scarak_Fighter` (81 HP, speed 7)
- `Dungeon_Scarak_Defender` (103 HP)
- `Dungeon_Scarak_Defender_Patrol` (158 HP — beefier!)
- `Dungeon_Scarak_Broodmother` (145 HP)
- `Dungeon_Scarak_Broodmother_Young` (124 HP)
- `Dungeon_Scarak_Louse` (21 HP)
- `Dungeon_Scarak_Seeker` (61 HP)
- `Dungeon_Scarak_Seeker_Patrol` (63 HP)
- `Dungeon_Scarak_Fighter_Patrol`
- `Dungeon_Skeleton_Sand_Archer` (61 HP)
- `Dungeon_Skeleton_Sand_Assassin` (61 HP)
- `Dungeon_Skeleton_Sand_Mage` (49 HP)
- `Dungeon_Skeleton_Sand_Soldier` (61 HP)

### Biome-Themed Skeleton Sets (for themed dungeons)
| Theme | Roles Available |
|-------|----------------|
| **Standard** | Skeleton_Fighter, _Knight, _Archer, _Mage, _Archmage, _Ranger, _Scout, _Soldier |
| **Desert/Sand** | Skeleton_Sand_Archer, _Archmage, _Assassin, _Guard, _Mage, _Ranger, _Scout, _Soldier |
| **Frost/Ice** | Skeleton_Frost_Archer, _Archmage, _Fighter, _Knight, _Mage, _Ranger, _Scout, _Soldier |
| **Burnt/Fire** | Skeleton_Burnt_Alchemist, _Archer, _Gunner, _Knight, _Lancer, _Praetorian, _Soldier, _Wizard |
| **Incandescent** | Skeleton_Incandescent_Fighter, _Footman, _Head, _Mage |
| **Pirate** | Skeleton_Pirate_Captain, _Gunner, _Striker |

---

## Usage in Plugin Code

```java
// Spawn a skeleton fighter
NPCEntity mob = new NPCEntity(world);
mob.setRoleName("Skeleton_Fighter");
world.spawnEntity(mob, position, rotation);

// Spawn a dungeon scarak (uses built-in dungeon tuning)
NPCEntity scarak = new NPCEntity(world);
scarak.setRoleName("Dungeon_Scarak_Fighter");
world.spawnEntity(scarak, position, rotation);

// Spawn a boss
NPCEntity boss = new NPCEntity(world);
boss.setRoleName("Goblin_Duke");
world.spawnEntity(boss, bossSpawnPos, rotation);

// Spawn with custom equipment via RoleUtils
NPCEntity trork = new NPCEntity(world);
trork.setRoleName("Trork_Warrior");
RoleUtils.setItemInHand(trork, "Weapon_Battleaxe_Stone_Trork");
world.spawnEntity(trork, position, rotation);
```

---

## Open Questions
- [ ] Can we create custom role JSONs and have the server load them? (would enable fully custom mobs)
- [ ] Does `setRoleName()` accept the full path or just the filename? (evidence suggests filename only)
- [ ] How does the Patrol vs Wander vs base variant affect `setRoleName()`? (likely same base name with suffix)
- [ ] Can we dynamically modify MaxHealth after spawning? (for difficulty scaling)
- [ ] What's the exact damage formula when `_CombatConfig` (CAE) is used vs `_InteractionVars`?

---

*Research by subagent, 2026-02-07. Source: Assets.zip build-7, HytaleServer.jar decompilation.*
