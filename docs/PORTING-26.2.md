# Porting Tamekind to Minecraft 26.2

Status: **not started.** 0.1.0 targets 26.1.2.

26.2 is **not source-compatible** with 26.1.2, for the same reason as Warband (see
`../Warband/docs/PORTING-26.2.md`): the break is at the *owner class* level, and the
replacement classes do not exist in 26.1.2, so a single jar cannot serve both without
source preprocessing (Stonecutter) or reflection. Not worth it for a two-version window.

Verified by cross-compiling:

```
gradlew compileJava -Pminecraft_version=26.2 -Pfabric_api_version=0.156.0+26.2
```

against Loom 1.17.17 / Gradle 9.6.1 / loader 0.19.3. Result: **102 errors, in 3 files.**
Every one is mechanical, and they fall into exactly two buckets. Taxonomy below with
the confirmed 26.2 replacement.

**This port is much smaller than Warband's**, despite the higher raw error count: 100
of the 102 errors are in the two datagen providers, and the entire runtime AI package
compiles clean. The count is high because tag providers name constants in bulk.

Re-verified after the 0.2.0 feature pass: the count moved 100 → 102, and both new
errors are one added `EntityType.WOLF` reference in the entity tag provider. No new
error *category* appeared — every new AI class (`Disposition`, `SizeVariance`,
`AnimalTemperament`, `SentinelWatchGoal`, `HerdTrailGoal`, `ConditionGoal`,
`AnimalBreedingMixin`) compiles against 26.2 unchanged.

## 1. `EntityType.*` constants moved to a new `EntityTypes` class — 68 errors

```java
net.minecraft.world.entity.EntityType.PIG    // 26.1.2
net.minecraft.world.entity.EntityTypes.PIG   // 26.2
```

`EntityType` still exists in 26.2 (it remains the generic type you hold), but it
retains only 3 static fields — every registry constant moved to the new sibling class
`net.minecraft.world.entity.EntityTypes`.

**`EntityTypes` does not exist in 26.1.2**, so unlike Warband's `getCenter()` item
this cannot be done ahead of the port. It must happen on the branch.

Affected symbols: `CAT`, `CHICKEN`, `COW`, `CREEPER`, `DONKEY`, `FOX`, `GOAT`,
`HORSE`, `HUSK`, `LLAMA`, `MULE`, `OCELOT`, `PARROT`, `PIG`, `PILLAGER`,
`POLAR_BEAR`, `RABBIT`, `SHEEP`, `SKELETON`, `STRAY`, `VINDICATOR`, `WOLF`, `ZOMBIE`.

Affected files: `TamekindEntityTagProvider` (68), `WallowGoal` (2).

This is a pure find-and-replace of `EntityType.` → `EntityTypes.` at those sites, plus
one import. Note `WallowGoal` is the only *runtime* file in the whole port.

## 2. Wool constants folded into a `ColorCollection` — 32 errors

```java
Blocks.WHITE_WOOL                        // 26.1.2
Blocks.WOOL.pick(DyeColor.WHITE)         // 26.2
Blocks.WOOL.white()                      // 26.2, record accessor
```

`Blocks.WOOL` in 26.2 is a `ColorCollection<Block>` — a 16-field record keyed by
`DyeColor`. The per-colour `*_WOOL` fields are gone from `Blocks` entirely.

All 32 errors are the 16 wool blocks listed twice (error + note) in
`TamekindBlockTagProvider.addTags`, which adds every wool colour to
`tamekind:comfort_blocks`.

**This one is a simplification, not just a fix.** The provider currently spells out
all 16 colours across four source lines. `ColorCollection` exposes `asList()`, so the
port should collapse it:

```java
// 26.2
addBlocks(TamekindTags.COMFORT_BLOCKS, Blocks.HAY_BLOCK, Blocks.MOSS_BLOCK,
        Blocks.MOSS_CARPET, Blocks.PALE_MOSS_CARPET);
Blocks.WOOL.forEach(wool -> builder(TamekindTags.COMFORT_BLOCKS)
        .add(BuiltInRegistries.BLOCK.getResourceKey(wool).orElseThrow()));
```

Relevant `ColorCollection<T>` API, confirmed against the 26.2 jar:
`asList()`, `forEach(Consumer<T>)`, `pick(DyeColor)`, `map(Function)`, plus one
record accessor per colour (`white()`, `orange()`, …).

Also worth knowing: the shipped `data/tamekind/tags/block/comfort_blocks.json` already
lists the wool colours literally and is version-neutral. The datagen providers are a
parallel source of truth for the same tags — see the warning below.

## 3. What did NOT break

Recorded because it is the useful half of a cross-compile:

- **`Vec3.atCenterOf`** — already migrated off `BlockPos.getCenter()` on the 26.1.2
  tree, which is why zero errors appear for it here. Warband's doc flagged this as
  the version-neutral prep item; Tamekind has done it.
- **Animal class foldering.** 26.2 continues moving entities into per-species packages
  (`animal.cow.Cow`, `animal.pig.Pig`, `animal.polarbear.PolarBear`,
  `animal.sheep.Sheep`, `monster.zombie.Zombie`). Tamekind only ever imports
  `Animal`, `animal.wolf.Wolf`, `animal.fox.Fox` and `animal.chicken.Chicken`, all of
  which were already foldered in 26.1 — **so there is no import churn at all.** This
  is the main reason this port is lighter than Warband's, which imports `Slime` and
  `MagmaCube` directly.
- **Save data.** `ValueInput` / `ValueOutput` and the `addAdditionalSaveData` /
  `readAdditionalSaveData` signatures on `Animal` are unchanged, so
  `AnimalMemoryMixin` and `AnimalMemory.save/load` need no work.
- **Attributes, goals, tags, commands, `Identifier`, `isFaceSturdy`** — all clean.

## Recommended order

1. Branch `port/26.2`.
2. Do (1): mechanical `EntityType.` → `EntityTypes.` plus imports. Two files.
3. Do (2), taking the `ColorCollection.forEach` simplification rather than a
   16-way `pick(...)` translation.
4. Bump `depends.minecraft` in `fabric.mod.json` from `~26.1` to `~26.2`.
5. Re-verify the mixin targets in `tamekind.mixins.json` still bind. The compile check
   does **not** catch mixin refactor breakage, and `injectors.defaultRequire` is 1, so
   a moved target is a hard crash at load rather than a silent no-op. `ChickenNestMixin`
   (`Chicken.aiStep`, plus the public `eggTime` / `isChickenJockey` fields),
   `AnimalMemoryMixin` (`Animal.addAdditionalSaveData`), `AnimalBreedingMixin`
   (`Animal.finalizeSpawnChildFromBreeding` and `Animal.canMate`), and the
   `registerGoals` injections in `WolfHuntMixin` / `FoxHuntMixin` all target classes
   Mojang is actively reorganising. **Launch a dev server, not just a build.**
6. Confirm in-game that the two hunt mixins actually fire. They previously used
   reflection on a mapped field name, which worked in dev and failed silently in a
   released jar; they now use `MobGoalSelectorAccessor.tamekind$targetSelector()`.
   Keep it that way — never reintroduce `getDeclaredField` on a Minecraft member.

## Standing warning: datagen vs shipped resources

`build.gradle` registers `src/main/generated` as a resource source dir, and the
datagen providers write the *same* tag paths that are already committed by hand under
`src/main/resources/data/tamekind/tags/`. `src/main/generated` is currently absent, so
nothing collides — but running the `datagen` task would produce two resource roots
claiming the same files. Decide one owner before the port; the hand-written resources
are what actually ships today.

## Handy commands

```bash
# cross-compile check
gradlew compileJava -Pminecraft_version=26.2 -Pfabric_api_version=0.156.0+26.2

# group remaining errors by kind
gradlew compileJava -q -Pminecraft_version=26.2 -Pfabric_api_version=0.156.0+26.2 2>&1 \
  | grep -E "^\s+symbol:" | sed -E 's/^\s*symbol:\s*//' | sort | uniq -c | sort -rn

# find the new owner of a relocated constant
gradlew compileJava -q -Pminecraft_version=26.2 -Pfabric_api_version=0.156.0+26.2 2>&1 \
  | grep -E "^\s+location:" | sort | uniq -c
```
