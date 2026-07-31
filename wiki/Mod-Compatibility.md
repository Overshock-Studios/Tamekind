# Mod Compatibility

The short answer: **most animal mods work with no action at all**, because Tamekind's behaviour attaches to anything extending vanilla's `Animal` class, and its tuning is driven by datapack tags rather than a list of ids in Java.

## Works with no action

- **Any modded animal extending `Animal`** gets the full goal set, memory and level-of-detail automatically. That is most animal mods.
- **Any mod that adds blocks** you want animals to graze on, shelter under or drink from: add them to the [block tags](Datapack-Tags). No code either side.
- **Any mod that adds predators or prey**: add them to `tamekind:predators_of/<prey>` and the food web works in both directions at once, because the same tag drives prey fleeing and predators hunting.

## Auto-detected

| Mod | What changes |
|---|---|
| **[Serene Seasons](https://modrinth.com/mod/serene-seasons)** | Winter triggers shelter-seeking even in clear weather, spring extends grazing, summer keeps heat-sensitive species in shade all day |
| **Warband / vanilla raids** | An active raid within 32 blocks pulls pasture animals into shelter, and active raiders count as direct threats |

Both are soft: no hard dependency, and Tamekind does not care whether they are installed.

## Needs a tag

| Situation | Fix |
|---|---|
| A modded animal should herd | add its type to `tamekind:herdable` |
| A modded animal should never be touched by Tamekind | add it to `tamekind:disabled` |
| A modded animal is a mount | add it to `tamekind:mounts` |
| A modded predator should hunt vanilla prey | add it to `tamekind:predators_of/<prey>` |

Ship these in **your own** datapack or mod with `"replace": false` and `"required": false` on each entry. `required: false` is what lets a tag name an entity type from a mod that is not installed without breaking tag loading.

## Needs code, and why

Two limits worth knowing:

- **A modded predator needs a mixin to hunt.** Prey fleeing is tag-driven and works for anything, but the hunting half attaches a target goal in `registerGoals`, and that is done per class for wolves and foxes. A modded wolf-alike will flee correctly but will not hunt until Tamekind adds it.
- **Creatures extending `PathfinderMob` directly**, rather than `Animal`, get nothing. Tamekind's injection point is `Animal`.

If either affects your mod, open an issue. These are the cases worth a code change.

## Other AI mods

Tamekind injects goals into the same goal selector vanilla uses, at priorities 0 to 35. Another AI mod claiming the same priorities with the same flags will contend with it, and the goal selector cannot break a tie: whichever goal starts first keeps control.

**`/tamekind goals` reads the live selector** and prints every same-priority flag collision, including goals belonging to vanilla and to other mods. If a behaviour never seems to fire, that command is the first thing to check. See [Troubleshooting](Troubleshooting).

## Mods that overlap

Tamekind deliberately does not touch loot tables, add items, or replace vanilla entities, so it stacks with most husbandry mods rather than fighting them. Where another mod also adds panic or shelter behaviour, expect both to run and the priorities to decide which wins. Turn off the overlapping Tamekind subsystem in the [config](Configuration) if you prefer the other mod's version.
