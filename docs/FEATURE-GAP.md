# Feature gap analysis: Tamekind vs the passive-animal mods

Written for the 0.1.0 → 0.2 planning pass, mirroring the method used in
`../Warband/docs/FEATURE-GAP.md`. Every "Tamekind status" line below was checked
against the current source, not recalled.

## Baseline: the niche splits in two, and neither half reaches 26.x

The comparison set is not one competitor but two disjoint groups.

**Group A: behaviour mods.** Same goal as Tamekind: make vanilla animals act alive.

| | Herd Hysteria | HerdsPanic | Animals herding | Big Brain | Advanced Animals | Tamekind |
|---|---|---|---|---|---|---|
| Loaders | Fabric | Fabric | Fabric | Fabric/Forge | Forge | Fabric |
| Max MC version | 1.21.11 | 1.21.1 | ~1.20 | 1.21.1 | ~1.20 | **26.1.2** |
| 26.x support | none | none | none | none | none | **yes** |
| Herd panic spread | yes | yes | no | yes | yes | yes |
| Herd following | no | no | yes | no | no | yes |
| Shelter seeking | no | no | no | yes | no | yes |
| Elected alpha | no | no | no | no | no | **yes** |
| Trust / memory | no | no | no | no | no | **yes** |
| Datapack-driven | no | no | no | no | no | **yes** |
| AI level-of-detail | no | no | no | no | no | **yes** |

**Group B: livestock sims.** Adjacent, much heavier, and content-shaped: they add
items, GUIs, genetics and replacement entities.

| | Genetic Animals | Animal Husbandry | Animal Wellness |
|---|---|---|---|
| Loaders | Forge only, explicitly no Fabric planned | Forge/NeoForge | Forge/NeoForge |
| 26.x support | none | none | none |
| Adds items/blocks | yes | yes (Brush, Animal Medicine) | yes (structures) |
| Replaces vanilla entities | yes | no | no |

Two conclusions, and they point opposite ways.

Against Group A, **Tamekind is already ahead on every axis** and is the only mod of
its kind on Fabric 26.x. Herd Hysteria, the closest active competitor, is one
mechanic (hurt one, all flee) with 484 downloads. There is nothing to take.

Against Group B there is plenty to take, but almost all of it violates the mod's
own constraints. The useful move is not parity: it is extracting the *feeling* those
mods sell (animals are individuals you manage, not spawner output) and delivering it
through AI alone. That is what the three additions below do.

## Where Tamekind already leads

Not gaps: worth knowing so they don't get "fixed" into parity:

- **Elected alpha with a shared blackboard.** A deterministic lowest-UUID adult per
  herd publishes shelter, graze and water positions its followers reuse. No mod in
  either group has a leader concept at all.
- **Trust and danger memory per player**, persisted through `Animal`'s save data,
  with linear decay, hit-forgiveness and herd trust sharing.
- **A real datapack surface.** 7 entity tags, 7 block tags, a per-species biome tag,
  and `predators_of/<prey>`: modpacks reshape the food-web without touching code.
- **AI level-of-detail** (FULL/SIMPLE/SLEEP/HIBERNATE) with a per-entity cache, so
  dense farms stay cheap. Every Group A mod runs full AI on every animal forever.
- **Two-way predator tags.** Prey flees the predator *and* wolves and foxes hunt it.
- **Farm-respect guarantees.** Leashed, mounted, named, breeding and tamed animals
  opt out wholesale. Group B mods deliberately break vanilla farming; Tamekind's
  whole pitch is that it does not.

## The gap table

| Group B feature | Tamekind status | Verdict |
|---|---|---|
| Per-animal personality traits (glutton, grumpy, energetic) | **Was absent.** Now `AnimalTemperament`: skittish/steady/bold/curious, UUID-derived | **Taken** |
| Herd lookout / sentinel while others feed | **Was absent** everywhere in the niche | **Taken** (from wildlife sims, not from a mod) |
| Calm-approach bonding | **Was absent.** Now `crouchFeedTrustMultiplier` | **Taken** |
| Hunger / thirst as a survival stat with damage | Now `conditionEnabled`: opt-in and **non-lethal**; the damaging version stays declined | **Taken, defanged** |
| Sickness and medicine | Absent | **Decline**: needs items |
| Grooming / happiness meters | Absent | **Decline**: needs items and a GUI |
| Inherited genetics (colour, size, yield) | Now `heritableSizeEnabled`: a calf is its parents' midpoint. Size only | **Taken** |
| Gendered animals, pregnancy, egg fertility | Absent | **Decline** |
| Isolation stress (a lone penned animal) | Now `isolationStressEnabled`: jumpier, slower to settle | **Taken** |
| Territorial retaliation after repeated culling | Now `territorialRetaliationEnabled`: survivors stop fleeing | **Taken** |
| Predator-vs-predator turf conflict | Now shipped as a tag entry; the system already supported it | **Taken** |
| Replacement entity models per breed | Absent | **Decline** |

## What was taken, and where from

Ordered by player-visible impact per unit of risk. None of the three adds an item,
a block, an entity or a packet.

1. **Temperament** (`AnimalTemperament`, `temperamentEnabled`). Derived from the
   entity UUID, so it costs no storage and no codec and is stable across reloads.
   Scales alert radius, freeze length and trust gain. This is Animal Husbandry's
   personality traits and Genetic Animals' per-animal variation, delivered as pure
   AI. Seeded from the *high* UUID bits because size variance already uses the low
   bits: sharing a seed would make every skittish cow the same size.

2. **Sentinel watch** (`SentinelWatchGoal`, `sentinelEnabled`). While the herd
   grazes, the alpha holds position, sweeps its head, scans at
   `sentinelAlertRadiusMultiplier × alertRadius`, and broadcasts danger the moment it
   sees something. Lifted from open-world wildlife sims, where a lookout is standard
   and where the readable silhouette, one animal with its head up, is the whole
   tell. **No mod in either group models this.** It is the clearest differentiator
   available, and it only became possible once the alpha election actually worked.

3. **Approach etiquette** (`crouchFeedTrustMultiplier`). Feeding while crouched
   bonds faster. Chosen over a bespoke taming minigame because it reuses the trust
   system and because it mirrors Warband's central perception rule: crouching
   *subtracts* threat. The two mods now share one legible body-language grammar:
   crouch to be less alarming, sprint to be more, across passive and hostile AI.

4. **Sentinel rotation** (`sentinelRotationEnabled`). The watch rotates through the
   herd's adults on a shift timer derived from game time, so the alpha eventually gets
   to eat. Derived rather than stored, so it needs no save data and no handoff message;
   members at the edge of their scan radius can briefly disagree, and two lookouts is
   harmless.

5. **Heritable size** (`heritableSizeEnabled`). A bred calf's base scale is the
   midpoint of its parents plus a small jitter, persisted so it compounds instead of
   resetting each birth: selective breeding for size finally pays off. Clamped to the
   same envelope as a wild roll, because 200 generations of unclamped jitter produces
   unusable animals (there is a test for exactly that). Wild spawns still roll from
   their UUID, so only descent is ever stored.

6. **Isolation stress** (`isolationStressEnabled`). A herd animal with no herd-mates in
   range is jumpier (`isolationAlertMultiplier`) and settles down to graze less readily.
   The roadmap also promised "breeds slower"; that half was **dropped as vacuous**:
   breeding needs a partner inside the very radius that defines isolation, so an
   isolated animal cannot breed regardless.

7. **Territorial retaliation** (`territorialRetaliationEnabled`). Adults that witness
   enough herd-mates killed nearby inside `cullMemoryTicks` stop fleeing and hold
   ground facing the threat. They **never fight back**, only panic is suppressed,
   which keeps passive mobs passive while still making a culled herd feel like it
   noticed. The count resets when the window lapses, so a herd farmed slowly over hours
   never turns defiant. Thematically this is Warband's grudge system reflected onto
   passive mobs.

8. **Predator turf conflict.** This needed no new system: `predators_of/<prey>` already
   drives both flight (`ThreatScanner`) and hunting (`TagHunting`), and foxes are
   `Animal`s, so a single tag entry, wolf in `predators_of/minecraft/fox`, makes wolves
   contest foxes in both directions at once. Shipped as data, fully overridable.

9. **Body condition** (`conditionEnabled`, **off by default**). A 0..1 stat that drains
   only at FULL level-of-detail and is restored by a graze or drink the animal actually
   reached. Low condition slows movement and makes an animal decline to mate. Floored at
   `conditionFloor` and it never deals damage, so livestock cannot starve while a player
   is away. The damaging version stays declined: see below.

## Explicit declines, with reasons

- **Items, blocks, GUIs, structures.** Brushes, medicine and wellness buildings are
  what make Group B a different genre. Tamekind is server-side with no client
  entrypoint; adding content would forfeit "drop it on a server, add it to an
  existing world": the property that makes it adoptable at all.
- **Replacing vanilla entities.** Genetic Animals swaps cows for its own entity.
  That breaks every other mod's cow, every datapack loot table and every farm. It is
  the single largest source of complaints about that mod and directly contradicts the
  farm-respect guarantee.
- **Genders, pregnancy, egg fertility.** Doubles the breeding surface, invalidates
  every vanilla breeding tutorial, and the payoff is realism nobody asked Tamekind
  for. Seasonal gating (`seasonalBreedingEnabled`, off by default) already covers
  "breeding should not be infinite" with a fraction of the blast radius.
- **Hunger and thirst as *damaging* stats.** Converting condition into a meter that
  kills livestock while a player is away is the animal equivalent of "I logged off and
  my base had holes in it": the objection Warband's siege-mining guardrail exists to
  answer. The shipped `conditionEnabled` system is the honest version of this: opt-in,
  floored, non-lethal, and only draining where the animal is actually simulated. The
  lethal variant stays out permanently, not just for now.

10. **Trail-following.** Followers walk the alpha's recorded route (the oldest
    still-nearby point of an 8-deep trail) instead of making a beeline for it, so a herd
    moves in a line along ground already known to be walkable rather than converging into
    a clump and shoving itself through terrain the leader went around. The trail is
    deliberately **transient**: persisting it would path followers at coordinates the
    leader left minutes ago. Mirrors the trail-scent idea in Warband's roadmap.

## Roadmap after this pass

Everything previously listed here has shipped. Remaining ideas, in rough order of fit:

- **Heritable temperament.** A calf's temperament is currently rolled from its own UUID
  like any wild spawn. Blending the parents' dispositions the way size now blends would
  make a calm line of animals a real breeding goal.
- **Condition-linked produce timing.** A well-conditioned animal reaching its wool or
  egg timer slightly sooner. Deliberately *timing only*: touching drop tables would
  break the farm-respect guarantee.
- **Sentinel posture cues.** A head-up idle animation or particle so a lookout reads at
  a distance without needing `/tamekind dump`. Needs care: Tamekind is server-side, so
  this must ride existing vanilla entity events rather than a custom packet.
- **Predator hierarchy.** Turf conflict is currently symmetric via tags. A real pecking
  order (wolf displaces fox, fox displaces cat) would want a weight per predator rather
  than a boolean tag.

## Sources

- [Herd Hysteria](https://modrinth.com/mod/herd-hysteria): Fabric, max 1.21.11, 484 downloads
- [HerdsPanic](https://modrinth.com/mod/herdspanic): Fabric, max 1.21.1
- [Animals herding](https://modrinth.com/mod/animals-herding)
- [Big Brain](https://modrinth.com/mod/big-brain): shelter + herd alert
- [Advanced Animals](https://modrinth.com/mod/advanced-animals)
- [Genetic Animals](https://www.curseforge.com/minecraft/mc-mods/genetic-animals): Forge only, no Fabric planned
- [Animal Husbandry](https://www.curseforge.com/minecraft/mc-mods/animal-husbandry): traits, sickness, grooming
- [Animal Wellness](https://www.curseforge.com/minecraft/mc-mods/animal-wellness)
- Tamekind source, verified by direct search for `leaderFor`, `getDeclaredField`,
  `setShiftKeyDown`, `getCenter`, and the `tags/blocks` vs `tags/block` directory split
