# Tamekind

**Your animals start acting like they want to live.**

A cow lifts its head while the rest of the herd grazes, because something is moving at the treeline. A rabbit freezes instead of bolting. A sheep that you have hand-fed for a week no longer scatters when you sprint past. The herd that watched you cull three of its own stops running from you at all.

Tamekind is a vanilla+ AI overhaul for passive mobs: cows, sheep, pigs, etc.

## Why you might want it

**Your farms keep working.** Leashed, mounted, name-tagged, breeding and tamed animals opt out of everything Tamekind does and keep their vanilla AI. Breeding pens, lead-and-boat runs and named pets behave the way you built them to.

**Nobody has to install anything.** Server-side. Players connect with a vanilla client and still see all of it. Works in singleplayer too, and you can add it to a world you already have.

## What changes

**Herds get a leader.** One animal is visibly the alpha, and the rest reuse the shelter and grazing spots it finds instead of each searching alone. Followers walk its route rather than beelining, so a moving herd strings out instead of clumping.

**Someone keeps watch.** While the herd feeds, one adult stands with its head up scanning further than the others, and warns the herd before any individual would notice. The shift rotates.

**Fear has stages.** Notice, freeze, back away, then run. Animals avoid dead ends and water edges, keep off your crops when they can, and head for lit ground at night.

**Animals remember you.** Feed one and it learns to trust you, faster if you crouch. Hit one and it remembers that too, and tells the herd. Trust decays, so it has to be kept up.

**Every animal is a bit different.** Skittish, steady, bold or curious, fixed for that animal's life. Sizes vary, and a calf inherits its parents' build, so breeding for size actually compounds.

**Predators are real.** Wolves and foxes hunt what the datapack says they hunt, and the prey flees them. Your tamed wolf will not touch your own flock.

## Documentation

Everything else is in the **[wiki](../../wiki)**:

- **[How It Works](../../wiki/How-It-Works)** the systems in detail
- **[Commands](../../wiki/Commands)** and **[Configuration](../../wiki/Configuration)**
- **[Datapack Tags](../../wiki/Datapack-Tags)** reshape the food web and habitat without code
- **[Mod Compatibility](../../wiki/Mod-Compatibility)** what works untouched, what needs a tag
- **[API](../../wiki/API)** read-only accessors and events for mod authors
- **[Troubleshooting](../../wiki/Troubleshooting)**

## Compatibility

Most animal mods work with no action at all. [Serene Seasons](https://modrinth.com/mod/serene-seasons) and vanilla or [Warband](https://modrinth.com/mod/warband) raids are auto-detected with no hard dependency. Modded animals, blocks and predators can be added through datapack tags.

## Requirements

Fabric API.

## License

MIT.
