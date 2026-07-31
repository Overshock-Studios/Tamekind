# API

Tamekind exposes a stable, **read-only** API in `com.tamekind.api`. Everything outside that package is internal and may change in any release.

The reason it exists: Tamekind is the only mod in its niche with a herd layer, so "who is this animal's alpha" is a question nobody else can answer. If you want your mod to react to a herd, you should not have to wait for a release here.

## Soft dependency setup

```gradle
dependencies {
    modCompileOnly "maven.modrinth:tamekind:<version>"
}
```

Compile against it, guard every call, and your mod still works when Tamekind is absent:

```java
if (FabricLoader.getInstance().isModLoaded("tamekind")) {
    Animal alpha = TamekindAPI.alphaOf(cow);
}
```

## Read-only, on purpose

There is no setter anywhere in this API. Tamekind owns its own state, and a mod that could rewrite a herd's alpha or force a trust value would make every behaviour here unpredictable, including for the player.

**If you want to influence rather than observe, use the [datapack tags](Datapack-Tags).** They are the supported route, they need no code at all, and they work for any mod or pack. Adding your animal to `tamekind:herdable` gives it the whole herd layer.

## Accessors

All methods take an `Animal` and are safe to call on the server thread for any animal. Queries about animals Tamekind does not manage return neutral answers rather than throwing, so you never have to call `isManaged` first unless you want to.

### Coverage

| Method | Returns |
|---|---|
| `isManaged(animal)` | whether Tamekind runs its behaviour on this animal at all |
| `isHerdable(animal)` | whether its type is in `tamekind:herdable` |

`isManaged` is false for animals opted out by the `tamekind:disabled` tag, by `/tamekind disable`, or by the farm-respect rules: leashed, mounted, named, breeding and tamed animals keep their vanilla AI. **Check this before assuming Tamekind is the reason an animal is doing something.**

### Herd

| Method | Returns |
|---|---|
| `alphaOf(animal)` | the herd alpha, possibly the animal itself, or `null` |
| `isAlpha(animal)` | whether this animal is its own herd's alpha |
| `sentinelOf(animal)` | the herd-mate currently on watch, or `null` |
| `isOnWatch(animal)` | whether this animal is the one on watch |
| `herdSize(animal)` | herd size including itself; 1 means alone |
| `isIsolated(animal)` | whether a herdable animal has no herd-mates in range |

Election runs over herd-mates inside the configured search radius, so `alphaOf` is a live answer that changes as a herd moves. `sentinelOf` changes by design even when the herd is still, because the watch rotates on a timer.

### Individual state

| Method | Returns |
|---|---|
| `temperamentOf(animal)` | `SKITTISH`, `STEADY`, `BOLD` or `CURIOUS` |
| `trustOf(animal, playerUuid)` | 0 to 1; trust decays, so this is point-in-time |
| `conditionOf(animal)` | `OptionalDouble` 0 to 1, **empty when the condition system is off** |
| `inheritedScaleOf(animal)` | `OptionalDouble`, empty if the animal was not bred |

`conditionOf` is empty rather than 1.0 so you can tell "healthy" apart from "not tracked". Same for `inheritedScaleOf`: empty means a wild roll, which is derived from the entity UUID rather than stored.

### Threat state

| Method | Returns |
|---|---|
| `isAlarmed(animal)` | whether it holds a live danger memory |
| `alarmTicksRemaining(animal)` | ticks left on that memory, or 0 |
| `isStandingGround(animal)` | whether it has stopped fleeing after repeated culling |
| `isFullySimulated(animal)` | whether Tamekind is running full AI for it right now |

A danger memory outlasts the threat being visible, so `isAlarmed` stays true for a while after the wolf leaves.

`isFullySimulated` is worth using if your mod does per-tick work on animals. If Tamekind has decided an animal is too far from any player to simulate closely, the same is probably true for you.

`isStandingGround` animals **never retaliate with damage**. Only their panic is suppressed.

## Events

Fabric `Event` objects, fired server-side. Listeners run inside the behaviour that fired them, so keep them cheap and do not mutate the herd from inside one.

```java
TamekindEvents.ALARMED.register((animal, dangerPos) -> {
    // an animal became frightened, first-hand or by a herd-mate's warning
});

TamekindEvents.TRUST_CHANGED.register((animal, playerUuid, newTrust) -> {
    // gained by feeding or idle bonding, lost to a hit
});

TamekindEvents.ALPHA_CHANGED.register((animal, isAlpha) -> {
    // a real handover, not every frame of the election
});
```

| Event | Fires |
|---|---|
| `ALARMED` | once per animal per broadcast, so a startled herd of ten produces ten calls |
| `TRUST_CHANGED` | on every trust path, carrying the score **after** the change |
| `ALPHA_CHANGED` | on a hysteresis-guarded handover, not on election flicker |

There are only three events, and each exists because there is a real point in the code where it can honestly fire. `ALPHA_CHANGED` is worth explaining: election is computed on demand rather than stored, so the raw answer flickers as a herd drifts. The event fires from the point where the alpha's size bonus is actually applied or withdrawn, which is guarded against flicker and is therefore a transition worth reacting to.

## Stability

`com.tamekind.api` is the contract. The `Temperament` enum is deliberately separate from the internal one: the internal type carries the multipliers each temperament applies, and those are tuning that will change.

Everything else, including `com.tamekind.ai`, `com.tamekind.config` and every mixin, is internal. If you need something that is not here, open an issue rather than reaching into the internals, because they will move.
