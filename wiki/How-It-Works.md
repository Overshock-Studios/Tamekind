# How It Works

## Herds

Any animal whose type is in `tamekind:herdable` joins a herd made of the same species within `herdSearchRadius` (16 blocks by default).

**The alpha** is the lowest-UUID adult among the animal and its herd-mates. Because every member scores the same candidate set, they all elect the same animal, which is what makes it usable as a shared reference. The alpha is visibly larger, and it publishes shelter, grazing and water positions into a shared memory the rest of the herd reads instead of each animal running its own search. That is both a behaviour and a performance decision.

**The watch** rotates. While a herd grazes, one adult holds its head up, sweeps its gaze around and scans at a wider radius than normal, broadcasting danger the moment it sees something. The shift is derived from game time, so nobody guards forever and no handover state has to be stored. The animal on watch is the one not eating, which is the visible tell.

**Followers walk the alpha's route** rather than making a beeline for it, so a moving herd strings out along ground already known to be walkable instead of clumping and shoving itself through terrain the alpha went around.

## The flight response

Fear has stages rather than a single panic flag:

1. **Alert.** The animal notices a threat further out than the panic radius, stops, and looks at it.
2. **Freeze.** It holds still. Rabbits and chickens (`tamekind:freezers`) freeze far longer.
3. **Drift.** Partway through the freeze, it starts backing away slowly.
4. **Panic.** Inside the panic radius, it runs.

Escape positions are scored, not random. An animal avoids dead ends, water edges, and blocks in `avoid_blocks`; it penalises crops and snow; and at night it prefers lit ground. Babies panic slower and at shorter range. A wounded animal limps.

**Danger spreads.** One animal that sees a threat tells nearby herd-mates, with a cooldown so a herd cannot echo itself into a permanent panic. A danger memory outlasts the threat being visible, so animals stay skittish for a while afterwards.

**A culled herd stops running.** Adults that witness enough herd-mates killed nearby stop fleeing and hold their ground facing the threat. They never fight back; only the panic is suppressed. The count decays, so a herd farmed slowly over hours never turns defiant.

## Trust

Feed an animal and it remembers you, per player, persisted across reloads. Crouch while feeding and it bonds faster, since approaching calmly reads as less threatening. Trust decays linearly.

Trust does things:

- a trusted player sprinting nearby frightens the animal less
- above a threshold, hits stop creating danger memories at all
- trusted animals slow-follow a nearby trusted player
- feeding a well-trusted animal can extend love mode
- trust spreads at a reduced rate to the herd around the animal you fed

Hitting an animal costs trust and creates a danger memory it shares with its herd.

## Habitat and the daily rhythm

Animals shelter from rain and storms, seek lit ground at night, take midday shade if they are heat-sensitive, drink at water edges, graze (actually eating the grass), wallow in mud if they are pigs, lay eggs near nest blocks if they are chickens, and huddle together to sleep.

**Mothers and calves.** Babies anchor to a nearby adult, calves that lose their parent call out and search, and an adult whose baby was hurt enters a guarding state where it moves toward the baby instead of fleeing.

## Individuality

**Temperament** is one of skittish, steady, bold or curious, derived from the entity UUID. It costs no storage, never changes, and scales how far the animal notices threats, how long it freezes, and how quickly it learns to trust you. Two cows in one field behave measurably differently.

**Size** varies per spawn, and a bred calf inherits the midpoint of its parents rather than rolling fresh, so breeding for size compounds across generations. Babies grow smoothly into adults. The alpha carries a small extra bonus.

## The predator food web

`tamekind:predators_of/<prey>` drives both halves at once: the prey flees the predator, and the predator hunts the prey. A datapack edit is a real change to the food web, not a one-way flinch.

Tamed predators never hunt on their own, matching vanilla, so your pet wolf will not work through your own flock.

## Level of detail

Every animal sits in one of four tiers by distance from the nearest player: `FULL`, `SIMPLE`, `SLEEP`, `HIBERNATE`. Most behaviour only runs at `FULL`. The tier is cached per entity with jitter so a large farm does not recalculate everything on the same tick.

This is why Tamekind can afford a herd layer at all, and it is worth knowing when a behaviour "does not work" from across the map. `/tamekind list` shows the breakdown.

## What Tamekind never touches

Leashed, mounted, named, breeding and tamed animals opt out of every movement behaviour and keep their vanilla AI entirely. Loot tables, drops and breeding mechanics are untouched. There are no new items, blocks, entities or recipes.

The whole design rests on this: a vanilla farm has to keep working exactly as its builder expects.
