# Tamekind

A vanilla+ passive-mob AI overhaul. Cows, sheep, pigs, horses, goats, llamas, rabbits, chickens, wolves and foxes act aware of the world: herds elect an alpha, one animal keeps watch while the rest graze, prey freezes before it runs, and animals remember which players fed them and which hit them.

Tamekind is **server-side**. Players do not need to install anything, it works in singleplayer, and it adds **no items, blocks, entities or recipes**. Everything is behaviour.

## Start here

- **[How It Works](How-It-Works)**: herds, the flight response, trust, habitat, and the level-of-detail system that keeps it cheap
- **[Commands](Commands)**: `/tamekind dump`, `goals`, `leader`, `trust`, and the rest
- **[Configuration](Configuration)**: profiles, and the knobs worth knowing about
- **[Datapack Tags](Datapack-Tags)**: reshape the food web and the habitat without touching code
- **[Mod Compatibility](Mod-Compatibility)**: what works out of the box, and what needs a tag
- **[API](API)**: read-only accessors and events for other mod authors
- **[Troubleshooting](Troubleshooting)**: when a behaviour never seems to fire

## The short version

1. **Herds elect an alpha.** It is visibly larger, and it publishes shelter, grazing and water positions the rest of the herd reuses instead of each animal searching alone.
2. **One animal keeps watch.** While the herd grazes, the animal on duty holds its head up and scans further than normal, so the herd reacts before any individual could. The shift rotates.
3. **Fear has stages.** Alert, freeze, drift away, then run. Animals avoid dead ends, water edges and open ground at night.
4. **Animals remember you.** Feed one and it learns to trust you, faster if you crouch. Hit one and it remembers that too, and tells the herd.
5. **Farms are untouched.** Leashed, mounted, named, breeding and tamed animals keep their vanilla AI entirely.

## Requirements

Fabric API. Minecraft 26.1.2. Can be added to an existing world.
