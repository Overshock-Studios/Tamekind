# Tamekind

**A vanilla+ passive-mob AI overhaul for animals that feel aware of the world.**

Tamekind is the passive-side counterpart to hostile AI overhauls. Animals herd, flee intelligently, seek habitats, remember danger and trust, and create living-world behavior without breaking vanilla farms.

## What it does today

### Awareness
- **Threat scanning** with per-prey predator pairings (`tamekind:predators_of/<entity>`) and a flat fallback (`tamekind:predators`). Tamed/sitting wolves, foxes, cats and ocelots are not threats.
- **Trust-aware player fear**: sprinting players scare nearby animals; feeding them builds trust that reduces flee distance, hitting them tears trust down. Trust holds for half its window then decays linearly to zero.
- **Damage and explosions** register a danger position even when blast damage is zero. Danger spreads to nearby same-type herd-mates with a cooldown to avoid spam.

### Reactions
- **Alert / freeze / drift** state before panic with a short audible cue at start. Animals freeze and stare at distant threats, then drift slowly away in the second half of the alert. Animals in `tamekind:freezers` (rabbit, chicken, parrot by default) freeze longer and skip the drift. Babies freeze for half as long.
- **Panic escape selection** scores candidates against distance from threat, water neighbors (drowning-shore avoidance), boxed-in dead ends, soft-avoid blocks (snow, crops, farmland), grazing surfaces, rain exposure, and at night, block-light brightness. Cliff drops are checked up to a configurable depth. Panic transition plays a sound cue.
- **Fire / lava proximity** registers a danger position with no damage required (3-block radius scan).
- **Babies** use reduced panic radius and slower panic speed.
- **Low-HP limp**: under 30% health, panic speed drops and shelter is sought even on clear days.
- **Stampede knockback** scales with herd size and skips leashed, mounted, named, breeding, vehicled, and baby entities. Optional config-gated stampede crop trampling.
- **Protective parents**: when a baby of the same type is hit, nearby adults are flagged as guarding and re-direct their panic toward the baby instead of fleeing.
- **Hit forgiveness**: a sufficiently-trusted player's hit drains trust but doesn't trigger panic or danger memory.
- **Crouch-stalk**: detected hostile threats are flagged as sneaking while they approach.
- **Jockey assembly safe**: baby hostile mobs within mount range are not treated as threats, so vanilla and Warband jockey formations can finish mounting before normal threat scoring resumes.

### Habitat and rhythm
- **Shelter** during rain, darkness, low health, or midday heat exposure for animals in `tamekind:heat_sensitive`. Scoring prefers tagged shelter blocks, comfort blocks underfoot, lit positions at night, and penalises snow/soft-avoid surfaces. Only true overhead `shelter_blocks` count as covered. Thunderstorms shorten the scan cooldown and add a 1.4× approach speed.
- **Graze / rest / sleep** at full LOD, biased toward grazing or comfort blocks; rest sessions last 3× longer at night, 1.5× longer in `comfortable_in/<species>` biomes. At night, herd-mates cluster toward each other when picking rest spots. Grazing animals occasionally convert grass under them to dirt (vanilla sheep behavior, extended).
- **Drink** at water-edge positions occasionally.
- **Home position** is set on graze completion. `HomeReturnGoal` drifts animals back when they wander past `homeReturnMaxDistance`. Babies inherit home from the adult they anchor to.
- **Wallowing pigs** on mud or beside water briefly stop and emit splash particles.
- **Nesting**: chickens postpone egg laying until adjacent to `tamekind:nest_blocks` (hay/leaves by default).
- **Mother-calf bonding**: adults periodically slow-walk over to their nearest same-type calf and look at it.
- **Mating displays**: animals in `tamekind:mating_displays` (sheep, goat) approach same-sex breeding-ready rivals briefly.
- **Per-spawn size variance**: small visual scale variation (±10%) at spawn, deterministic per-UUID so it persists.

### Herd coordination
- **Herdable** animals are tagged via `tamekind:herdable`.
- **Leaders** are picked deterministically per herd. Leaders run expensive scans for shelter, graze and water targets and write the result to memory; followers piggyback within the same window instead of re-scanning. Followers also abort herd-follow if the leader has an active danger memory (let panic spread take over).

### Breeding and farm safety
- **Crowd control**: between `breedingCrowdSoftLimit` and `breedingCrowdHardLimit` same-type animals nearby, refusal probability ramps linearly. Hard limit is absolute.
- **Calmer breeding**: feeding from an already-trusted player instantly triggers love mode at extended duration.
- **Seasonal breeding gate** *(opt-in)*: `BreedingSeason` derives spring/summer/autumn/winter from game-day count and `seasonLengthDays`; `seasonalBreedingEnabled` lets mod-added natural reproduction gate on `breedingSeason`. Player feeding-to-breed is unaffected.
- **Movement-goal skip** for leashed, mounted, ridden, in-love (breeding), name-tagged, vehicled animals, and any entity type in `tamekind:disabled` or the runtime disable set.

### Pets and mounts
- **Trust-based follow without leash**: animals with trust ≥ `followTrustedMinTrust` slow-follow the trusted player when far.
- **Mount loyalty extension**: feeding any `TamableAnimal` or mount-type entity multiplies the trust duration by `mountFoodTrustMultiplier` (default 4×).
- **Mount obedience**: a trusted rider gets a transient movement-speed bonus on the mount (`+0.15 × trust`).
- **Mount-aware herd**: ridden leaders don't broadcast shared shelter/graze/water targets — the rider is in charge.
- **Pet idle-bond**: pets passively gain a small trust tick when near their owner, with no danger memory active.
- **Pet danger relay**: `TamableAnimal` pets near their owner look toward nearby non-tame animals with active danger memory.

### Integrations
- **Serene Seasons** *(optional, detected via FabricLoader)*: `BreedingSeason` reads SS season state via reflection when SS is loaded; falls back to internal day-count seasons otherwise. Winter shortens graze duration and triggers shelter-seek even on clear days; spring extends graze; summer keeps heat-sensitive species seeking shade beyond just midday.
- **Warband / vanilla raids**: an active raid within 32 blocks triggers shelter-seek regardless of weather. Active raiders count as direct threats in `ThreatScanner` (works with vanilla raids and with Warband's enhanced raid AI without a hard dependency).
- **Two-way predator tag**: wolves and foxes are mixed in with a target goal that hunts any animal listing them in `tamekind:predators_of/<prey>`. The tag becomes a contract — adding a predator to a prey's tag now causes both flight (prey side) and pursuit (predator side).

### Persistence
- Animal memory (danger, home, guard, all three shared positions, trust map, danger-spread cooldown) is round-tripped through NBT.

## Tags

Entity tags
- `tamekind:herdable`, `tamekind:predators`, `tamekind:predators_of/<namespace>/<path>`, `tamekind:disabled`, `tamekind:freezers`, `tamekind:mating_displays`, `tamekind:heat_sensitive`

Biome tags
- `tamekind:comfortable_in/<namespace>/<path>` per species

Block tags
- `tamekind:grazing_blocks`, `tamekind:shelter_blocks`, `tamekind:comfort_blocks`, `tamekind:water_blocks`, `tamekind:nest_blocks`, `tamekind:avoid_blocks`, `tamekind:soft_avoid_blocks`

All tags are `replace: false`, so datapacks and modpacks can append modded entries.

## Commands (`/tamekind ...`)

- `animal` — report the nearest animal's LOD, herd info, danger, trust toward the caller, home, guarding flag, leader
- `dump` — multi-line state dump of the nearest animal
- `list` — count animals within 64 blocks broken down by LOD
- `leader` — show the nearest animal's herd leader and current shared shelter
- `trust` — show the nearest animal's trust score toward the calling player
- `trust map <player>` — count of animals near the caller that trust the given player, plus average trust
- `season` — report current internal season, breeding allowed status, gating state
- `home set` / `home clear` — manage the nearest animal's home position
- `forget` — wipe danger/home/guard/shared positions for the nearest animal
- `disable <minecraft:type>` — toggle a runtime-only disable for an entity type (complements `tamekind:disabled` tag)
- `config` — print active config snapshot
- `profile` / `profile <vanilla+|realism|simulation>` — show or apply a config profile
- `reload` — re-read `config/tamekind.properties` from disk

## Config profiles

- **vanilla+** — quiet, tight ranges, no stampede or daily rhythm, longer LOD cache
- **realism** — defaults
- **simulation** — wider ranges, shorter LOD cache, all features on

Per-field overrides live in `config/tamekind.properties` (auto-generated on first run).

## Design pillars

- **Performance first.** AI LOD (`FULL` near players, `SIMPLE` mid-range, `SLEEP` far) gates expensive goals. Per-animal LOD cache avoids repeated player-distance scans.
- **Shared herd decisions.** Leaders pay for scans; followers reuse the result. Same idea for danger memory: one event, broadcast to the herd with cooldown.
- **Data-driven compatibility.** Behavior is steered by tags. Modded animals join the system by being added to `herdable`, `predators`, or the various block tags.
- **Vanilla farms still work.** Multiple opt-outs (leash, mount, name tag, breeding, vehicle, tag, runtime list) keep pens and farms predictable.

## Planned

### Integrations
- **Hearthfolk (villager companion mod)** — *planned*: villagers known to the village boost nearby animals' trust similar to the owner-player.

_(All originally planned items implemented.)_

## Development

### Datagen
Tag JSONs can be regenerated via Fabric datagen:

```
./gradlew runDatagen
```

Output goes to `src/main/generated/data/tamekind/tags/`. Providers live in `com.tamekind.datagen`. Hand-written JSONs under `src/main/resources/data/tamekind/tags/` are still present and take precedence; remove them if you want a pure-datagen pipeline.

### Tests
Pure-Java JUnit 5 tests for season cycling and config profiles:

```
./gradlew test
```

Reports land in `build/reports/tests/test/`.

## Requirements

- Fabric Loader 0.19.2+
- Fabric API
- Minecraft 26.1.x
- Java 25+

## License

MIT.
