# Tamekind

**A vanilla+ passive-mob AI overhaul where animals herd, panic, trust, and remember.**

Tamekind makes cows, sheep, pigs, horses, goats, llamas, rabbits, chickens, wolves, and foxes feel aware of the world. Herds elect alphas, packs share danger, parents shield calves, prey freezes or flees, and players who feed and care for animals earn a trust that survives world reloads: while leashes, name tags, breeding, mounts, and farms still behave exactly as vanilla expects.

- Herd logic with deterministic alphas (visibly larger), leader-driven shelter and graze scans, follower piggybacking, panic spread with cooldown, and stampede knockback that respects farms and babies
- Sentinel watch: while the herd grazes, the animal on duty keeps its head up, sweeps its gaze around, scans at a widened radius, and broadcasts danger to the herd the moment it spots something, and the shift rotates so nobody guards forever
- Per-animal temperament (skittish / steady / bold / curious) derived from the entity UUID, no save data, stable across reloads, scaling alert radius, freeze length and how fast the animal learns to trust you
- Heritable size: a bred calf is the midpoint of its parents, so selective breeding compounds across generations instead of rerolling every birth
- Trail-following: followers walk the alpha's recorded route instead of beelining at it, so herds string out along walkable ground rather than clumping
- Isolation stress for animals with no herd-mates in range, and territorial retaliation: a herd that has watched too many of its own die stops fleeing and holds its ground (it never fights back)
- Predator turf conflict: wolves contest foxes, expressed entirely through the same `predators_of/<prey>` tags a datapack can rewrite
- Optional body condition (off by default): drains slowly, restored by grazing and drinking, slows an animal and makes it decline to breed: floored and strictly non-lethal, so livestock can never starve while you're away
- Alert → freeze → drift → panic state machine with sound cues, baby-slowed panic, dead-end avoidance, water-shore avoidance, soft-avoid penalties for crops/snow, and night-time light-seeking escapes
- Trust and danger memory per player, with linear decay, hit-forgiveness for trusted players, idle-bond passive trust, herd trust sharing, farm-friendly trust boosts on tame mounts and pets, and an approach-etiquette bonus for feeding while crouched
- Habitat behavior: shelter in rain/storms, lit-area preference at night, midday shade for heat-sensitive species, water-edge drinking, grazing that eats grass, mud-wallowing pigs, nest-block chicken laying, mother-calf bonding, and group-sleep huddles
- Per-spawn size variance (±25% by default) plus smooth age-based growth so calves visibly grow into adults, layered with an alpha pride bonus so the pack leader stands out
- Two-way predator tag: prey listed in `tamekind:predators_of/<prey>` flees the predator AND wolves & foxes actively hunt them, turning datapack edits into a real food-web
- Storm reactions: panic when caught in the rain with no cover, lightning strikes scare nearby herds, thunderstorms accelerate shelter-seek and bump approach speed
- Pets & mounts: tame pets keep their owner-following AI, trusted mounts get a speed bonus, trusted feeding triggers extended love-mode, pets relay nearby danger toward their owner
- Serene Seasons & Warband-friendly integrations (optional, no hard deps): winter shelter-seek, summer all-day shade, season-gated auto-breeding, raid-aware shelter & threat scanning
- AI level-of-detail (FULL / SIMPLE / SLEEP / HIBERNATE) with per-entity LOD cache so mob-dense farms and modded biomes stay performant
- Debug commands and three config profiles (`vanilla+`, `realism`, `simulation`) with a fully-commented `config/tamekind.properties`

## What's in the tag system

Tamekind is data-driven: modpacks and datapacks can extend everything via `replace: false` tags.

- **Entity**: `herdable`, `predators`, `predators_of/<namespace>/<path>`, `disabled`, `freezers`, `mating_displays`, `heat_sensitive`
- **Block**: `grazing_blocks`, `shelter_blocks`, `comfort_blocks`, `water_blocks`, `nest_blocks`, `avoid_blocks`, `soft_avoid_blocks`
- **Biome**: `comfortable_in/<namespace>/<path>` per species

## Commands (`/tamekind ...`)

- `animal`: LOD, herd info, danger, trust, home, guarding flag, leader of the nearest animal
- `dump`: full multi-line state of the nearest animal
- `list`: animal count within 64 blocks broken down by LOD
- `leader`: nearest animal's pack alpha and current shared shelter
- `trust` / `trust map <player>`: trust toward you / footprint of a player across nearby animals
- `animal` / `dump` / `leader` also report `isAlpha` and the animal's `temperament`
- `season`: current season and breeding-allowed status
- `home set` / `home clear`: manage the nearest animal's home position
- `forget`: wipe danger / home / guard / shared positions
- `disable <type>`: runtime entity-type opt-out (complements the `tamekind:disabled` tag)
- `config` / `profile` / `reload`: inspect, switch profile, or hot-reload `config/tamekind.properties`

## Requirements

- Fabric API

Tamekind is server-side and can be added to an existing world.

## Compatibility

- **[Serene Seasons](https://modrinth.com/mod/serene-seasons)**: auto-detected. When loaded, Tamekind reads SS season state via reflection: winter triggers shelter-seek even in clear weather, spring extends graze, summer keeps heat-sensitive species seeking shade all day.
- **Warband / vanilla raids**: auto-detected. An active raid within 32 blocks pulls pasture animals into shelter, and active raiders count as direct threats: works with vanilla raids out of the box and with Warband's enhanced raid AI without a hard dependency.
- **Vanilla jockey assembly**: baby hostile mobs at mount range are exempt from panic so chicken-jockeys and Warband-spawned jockey formations finish mounting cleanly.
- **Tamed pets** (wolves, cats, foxes, parrots, ocelots): excluded from Tamekind movement goals by default so they keep their vanilla owner-following AI.

## For mod developers

Tamekind exposes a stable, read-only API in `com.tamekind.api`: herd and alpha queries,
temperament, trust, alarm state, and three server-side Fabric events. Use it as a soft
dependency with `modCompileOnly` and guard calls with `isModLoaded("tamekind")`.

Tamekind is the only mod in this niche with a herd layer, so "who is this animal's alpha"
is a question nobody else can answer. If you want to *influence* rather than observe, the
datapack tags are the supported route and need no code at all.

See the [wiki](../../wiki) for the API reference, the tag list and the compatibility notes.

## License

MIT.
