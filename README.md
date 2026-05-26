# Wildsense

**A vanilla+ passive mob AI overhaul for animals that feel aware of the world.**

Wildsense is the passive-side counterpart to hostile AI overhauls: animals should herd, flee intelligently, seek habitats, remember danger, and create living-world behavior without becoming noisy or overcomplicated.

## Feature Ideas

- Herd logic for cows, sheep, pigs, horses, goats, and similar animals, with leaders, stragglers, regrouping, and panic spread
- Predator and danger awareness, so animals react to nearby monsters, fire, explosions, projectiles, wolves, players, and recent damage
- Alert states before panic, so prey can freeze, stare down, or drift away from distant predators before breaking into a full sprint
- Player trust memory, where feeding or protecting animals lowers their flee response toward that specific player over time
- Habitat preferences: grazing fields, shade, water access, shelter in storms, snow avoidance, and biome-specific comfort
- Weather and light reactions, including animals grouping under leaves or roofs during rain and preferring safer lit areas at night
- Daily rhythms: grazing, resting, sleeping, wandering, drinking, and returning to preferred areas
- Parent and offspring behavior, including protective adults, following distance, hiding, and slower panic for young animals
- Stampede hazards for large panicked herds, with light knockback when a frightened group barrels through nearby entities
- Smarter breeding pressure, so animals do not blindly breed into overcrowded pens unless configured to allow it
- Farm-friendly controls that keep vanilla farms working while making free-roaming animals feel more alive
- Debug commands and config profiles for quiet vanilla+, survival realism, and high-simulation servers

## Design Pillars

- **Performance first.** Passive mobs are numerous, so complex behavior should use AI level of detail: full Wildsense logic near active players, cheaper herd updates at medium range, and vanilla/simple behavior when far away.
- **Shared herd decisions.** Herd leaders should make expensive pathing and habitat decisions where possible, while followers use cheap local steering, spacing, and leader trail following.
- **Data-driven compatibility.** Herdable animals, predators, feared events, comfort blocks, shelters, grazing blocks, and biome preferences should be driven by tags so datapacks and modpacks can patch in modded animals.
- **Vanilla farms still work.** Pens, breeding, leads, boats, minecarts, name tags, and player-built farms should remain predictable unless a config profile deliberately makes animals more independent.

## First Implementation Targets

- Shared passive memory component: home position, herd id, danger memory, comfort score
- Generic herd coordinator for nearby same-family animals
- Trust and danger memory for players, predators, projectiles, explosions, fire, and recent damage
- Panic rewrite that flees from the actual threat and avoids cliffs, lava, deep water, and dead ends
- Alert/freeze state that escalates to panic only when a predator closes distance or commits aggression
- Habitat scoring helper reusable across mob families
- AI LOD scheduler so expensive scans and path recalculations are rate-limited by player distance and herd size
- Tag schema for herd membership, predator/prey relationships, grazing blocks, shelter blocks, comfort blocks, and avoid blocks

## Requirements

- Fabric API

## License

MIT.
