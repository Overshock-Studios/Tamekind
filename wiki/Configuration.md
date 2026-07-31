# Configuration

`config/tamekind.properties`, generated on first run with **every key commented in place**. That file is the reference, not this page: it is generated from the code, so it can never drift out of date the way a wiki table would.

Changes apply on world reload, `/tamekind reload`, or restart.

## Profiles

The fastest way to configure Tamekind is not to. Pick a profile:

```
/tamekind profile vanilla+
/tamekind profile realism
/tamekind profile simulation
```

| Profile | Character |
|---|---|
| `vanilla+` | herds, alerts, panic and trust only. No habitat, no stampede, no daily rhythm. Shorter ranges, cheaper |
| `realism` | the default shape. Everything on at moderate ranges |
| `simulation` | everything on, wider ranges, more frequent level-of-detail recalculation |

A profile sets a coherent group of values at once. Individual keys in the file still win on next load.

## The keys worth knowing

Out of a hundred-odd keys, these are the ones that change the experience most.

### Master switches

`enabled` is the kill switch. Then one per subsystem: `herdEnabled`, `alertEnabled`, `panicEnabled`, `habitatEnabled`, `trustEnabled`, `stampedeEnabled`, `dailyRhythmEnabled`, `babyAnchoringEnabled`, `parentGuardEnabled`, `homeReturnEnabled`.

If another mod already does panic or shelter better, turn off that one subsystem rather than the whole mod.

### Farm safety

`respectLeashedAnimals`, `respectMountedAnimals`, `respectNamedAnimals`, `respectBreedingAnimals`, `respectTamedAnimals`. All true by default. **Leave them true** unless you specifically want Tamekind moving animals a player has claimed.

### Performance

`fullAiRange` (48) is the big one: most behaviour only runs inside it. `simpleAiRange` and `hibernateRange` handle the tiers beyond. `shelterSearchRadius` (15) scales cubically and is the most expensive single scan in the mod.

### Opt-in systems

Off by default, because each changes the game in a way not everyone wants:

| Key | What it adds |
|---|---|
| `conditionEnabled` | body condition that drains and is restored by grazing and drinking. **Non-lethal and floored**: a neglected animal gets slow and declines to breed, it never starves |
| `seasonalBreedingEnabled` | gates unattended breeding to a season |
| `stampedeCropDamageEnabled` | lets a panicking herd trample crops |

### Debugging

`debugLogs` logs the goal table with collisions once per config load, narrates alpha handovers with the resulting scale, and names the source of every hit an animal takes. See [Troubleshooting](Troubleshooting).

## Why there is no full table here

Tamekind has over a hundred keys and the properties file documents each one where it lives. Duplicating that here would create a second source of truth that goes stale, so the wiki covers the shape and the file covers the detail.
