# Tamekind in-game test plan

Two layers cover 0.2.0. **Run the automated one first:**

```
gradlew test           # 61 unit tests, no world
gradlew runGametest    # 15 in-world tests on a headless server
```

`runGametest` boots a real server, spawns real animals and asserts on real state, so it
already covers goal attachment, alpha consensus, sentinel rotation, isolation, the tamed
predator guard, attachment round-tripping, danger expiry and the scale pipeline. It is not
part of `check`, because it boots Minecraft.

What follows is the manual layer: the things a headless server cannot judge, chiefly
whether the behaviour *reads* correctly to a person watching it. Anything below that the
game tests already assert is marked `[AUTO]`.

Work top to bottom: T1 and T2 are load-bearing. If a mixin fails to bind, the server
crashes on start and every later test is meaningless.

## Setup

```
config/tamekind.properties
  debugLogs=true            # logs the goal table + collisions once per config load
  conditionEnabled=true     # off by default; turn on only for T10
```

Then `/tamekind reload` re-reads the file and re-logs the goal table, so you do not need
a restart to flip `debugLogs`.

Useful throughout:

| Command | Shows |
|---|---|
| `/tamekind goals` | live goal table + same-priority flag collisions |
| `/tamekind dump` | full state of the nearest animal |
| `/tamekind leader` | alpha, sentinel, herd size, shared shelter |
| `/tamekind list` | nearby animal count by AI level-of-detail |

---

## T1: Mixins bind (blocker) `[AUTO]`

`tamekind.mixins.json` sets `injectors.defaultRequire: 1`, so a moved target is a hard
crash at load, not a silent no-op. Three of the six mixins are new or rewritten in 0.2.0.

1. Start a dev server. **Expected: it reaches "Done" with no mixin error.**
2. Search the log for `Mixin apply failed` or `InjectionError`. Expected: none.

| Mixin | Target | Risk |
|---|---|---|
| `AnimalBreedingMixin` | `Animal.finalizeSpawnChildFromBreeding`, `Animal.canMate` | **new in 0.2.0** |
| `WolfHuntMixin` / `FoxHuntMixin` | `registerGoals` | **rewritten in 0.2.0** |
| `AnimalMemoryMixin` | `Animal.addAdditionalSaveData` | unchanged |
| `ChickenNestMixin` | `Chicken.aiStep` | unchanged |

If this fails, stop and report the stack trace: nothing else can be trusted.

## T2: Goal table and collisions (blocker)

1. Stand next to a **sheep** (densest vanilla goal table) and run `/tamekind goals`.
2. Expected shape: priorities 0–8 mixing vanilla and tamekind rows, then tamekind rows
   at 9–15, then flagless tickers at 30–35.
3. **Expected collisions: exactly these six, no more:**

```
COLLISION prio 1: PanicGoal vs PanicGoal share MOVE
COLLISION prio 2: AlertFreezeGoal vs BreedGoal share MOVE+LOOK
COLLISION prio 3: LostBabyGoal vs TemptGoal share MOVE+LOOK
COLLISION prio 5: HabitatShelterGoal vs EatBlockGoal share MOVE
COLLISION prio 7: SentinelWatchGoal vs LookAtPlayerGoal share LOOK
COLLISION prio 8: GrazeRestGoal vs RandomLookAroundGoal share MOVE+LOOK
```

Any collision **not** on that list is new and wants investigating: most likely another
mod claiming the same priority. Report the line verbatim.

4. Confirm `HerdFollowGoal` is at **priority 4** and nothing else shares it. At 6 it tied
   `WaterAvoidingRandomStrollGoal`, so herd following won a coin flip instead of leading.
5. Repeat on a **cow, pig, chicken, horse and wolf**: goal tables differ per species,
   and horses/llamas populate more priorities than sheep.

---

## T3: Herd following actually leads (the priority fix)

The behaviour that the priority-4 move was meant to repair.

1. Spawn 6 cows in open flat ground. Wait for them to settle.
2. `/tamekind leader` → note the alpha.
3. Lead the alpha away by ~30 blocks (push it, or let it wander).
4. **Expected:** followers string out and travel after it fairly consistently.
   **Before the fix:** they would drift off wandering roughly half the time.
5. `/tamekind dump` on the alpha → `trailPoints` should be **non-zero while it moves**
   and reset to 0 when it stops leading.
6. **Expected:** followers walk roughly the alpha's route, not a straight line at it. On
   broken terrain the herd should form a line rather than a clump.

## T4: Alpha election is unanimous (0.2.0 fix) `[AUTO]`

The bug: `leaderFor` never considered the animal itself, so nothing could elect itself.

1. Spawn 5 sheep together.
2. Run `/tamekind dump` on several of them.
3. **Expected: exactly one reports `isAlpha=true`,** and all of them name the *same*
   animal as `leader`.
   **Before the fix:** every animal reported `isAlpha=false` and pairs named each other.
4. Confirm the alpha is visibly larger (`alphaScaleBonus`, default 8%).
5. Kill the alpha. **Expected:** a different animal becomes alpha within ~2 seconds.

## T5a: Shelter seeking [EYES, not automated]

Automating this failed. Building valid terrain inside a game test proved harder than the
behaviour is worth: the test structure sits at an odd Y, an animal falls if the floor is
not laid under it, and five runs went on terrain rather than on the mod. The shelter search
itself is exhaustive and unchanged in behaviour, so this is a manual check.

1. `habitatEnabled=true`. Build a simple 5x5 roofed shelter in an open field.
2. Stand near a cow so it is at `FULL` level-of-detail (`/tamekind dump`).
3. `/weather rain`. **Expected:** the cow walks under the roof.
4. Wound a cow to below 30% health in clear weather. **Expected:** it also seeks shelter.
5. Remove the roof and repeat. **Expected:** it gives up and stays put rather than pathing
   to a position that does not exist.

## T5: Shared shelter and graze publish

These were dead for the same reason as T4.

1. 5 cows, `habitatEnabled=true`. Wait for rain, or `/weather rain`.
2. `/tamekind leader` → **expected `sharedShelter` becomes a real position**, not `none`.
3. `/tamekind dump` a follower → `sharedGraze` / `sharedWater` populate over time.

## T6: Sentinel watch and rotation

1. 6 cows in the open, daytime, no threats.
2. Watch for ~30 seconds. **Expected:** at any moment one animal stands with its head up
   sweeping around while others graze.
3. `/tamekind dump` on that animal → `onWatch=true`.
4. Keep watching ~1 minute. **Expected:** the animal on watch **changes**: the shift
   rotates (`sentinelWatchTicks`, default 8s). If one animal guards forever, rotation is
   broken.
5. Walk a wolf into range. **Expected:** the whole herd reacts noticeably sooner than a
   lone animal would, because the lookout broadcasts.

## T7: Predator food-web, and pets do NOT hunt (0.2.0 fix) `[AUTO]`

The hunt goal never attached in a released jar before; repairing it exposed a
farm-breaking case.

1. **Wild wolf + sheep.** Expected: the wolf hunts the sheep.
2. **Wild wolf + fox.** Expected: the wolf harasses the fox (new turf-conflict tag), and
   the fox flees the wolf.
3. **Tame a wolf, then put it next to your own sheep.**
   **Expected: it does NOT attack them.** This is the regression guard: a pet wolf
   working through its owner's flock breaks the farm-respect guarantee.
4. Tamed wolf + fox → also expected: no autonomous attack.

## T8: Temperament varies per animal

1. Spawn ~10 cows. `/tamekind dump` several.
2. **Expected:** a mix of `skittish` / `steady` / `bold` / `curious`, roughly 30/40/15/15.
3. Note one skittish and one bold animal. Approach each while sprinting.
   **Expected:** the skittish one reacts from noticeably further away.
4. Reload the world. **Expected:** every animal reports the **same** temperament as
   before: it is derived from the UUID, so it must never drift.

## T9: Heritable size

1. `sizeVarianceEnabled=true`, `heritableSizeEnabled=true`.
2. Find two visibly **large** cows (`/tamekind dump` → `inheritedScale=wild` for wild ones).
3. Breed them. **Expected:** the calf's `inheritedScale` is a number near the parents'
   average, not `wild`.
4. Breed large descendants for 3–4 generations. **Expected:** size trends upward and then
   stops at the `sizeVarianceRange` ceiling (default 1.25) rather than growing forever.
5. Breed two small animals. Expected: trends down, floors at 0.75.

## T10: Body condition is non-lethal (opt-in)

Only meaningful with `conditionEnabled=true`.

1. Pen a cow with **no grass and no water**. Stay nearby (condition only drains at FULL
   level-of-detail).
2. `/tamekind dump` periodically. **Expected:** `condition` falls, then **stops at
   `conditionFloor` (default 0.25)**.
3. **Expected: the animal never takes damage and never dies.** This is the guarantee that
   makes the system shippable: if it can starve, that is a bug, not a tuning issue.
4. At low condition: expected slower movement, and it declines to breed
   (`conditionBreedThreshold`, default 0.5).
5. Give it grass and water. Expected: condition recovers on a completed graze/drink.

## T11: Territorial retaliation

1. 6 cows together. `territorialRetaliationEnabled=true`.
2. Kill three of them within ~5 minutes (`cullMemoryTicks`).
3. `/tamekind dump` a survivor → `cullsWitnessed` should climb, then
   `standsGround=true` once it hits `cullVengeanceThreshold` (default 3).
4. Approach sprinting. **Expected:** it holds position and faces you instead of fleeing.
5. **Expected: it never attacks you.** Only panic is suppressed. If a cow deals damage,
   that is a bug.
6. Wait out `cullMemoryTicks` without killing anything, then kill one.
   **Expected:** `cullsWitnessed` restarts at 1: a herd farmed slowly must not
   accumulate into permanent defiance.

## T12: Isolation stress

1. One cow alone in open ground, no herd-mates within `herdSearchRadius` (16).
2. `/tamekind dump` → `isolated=true`.
3. **Expected:** it spooks from further away than the same temperament would in a herd,
   and grazes noticeably less often.
4. Add 3 more cows. Expected: `isolated=false` and it settles.

## T13: Farm-respect guarantees (non-negotiable)

Every one of these must show Tamekind movement goals inert.

| Setup | Expected |
|---|---|
| Leashed cow | no Tamekind movement; stays put |
| Name-tagged cow | no Tamekind movement |
| Cow in love mode | no Tamekind movement |
| Player riding a horse | no Tamekind movement |
| Tamed wolf / cat | keeps vanilla owner-following |
| Cow in a 1×1 pen | does not escape or suffocate |

Also: **build a normal breeding farm and confirm it still works.** Feed two cows, get a
calf. Then confirm feeding a **wounded** or **tamed** animal is never refused by crowd
control (0.2.0 fix): the message "This pen is too crowded" must only appear for a feed
that would actually start breeding.

## T14: Performance sanity

Never profiled. `HabitatShelterGoal.findShelter` scans ~8,600 blocks per invocation and
`DrinkGoal` ~1,200.

1. Spawn 100+ animals in one area.
2. Watch server tick time (F3, or `/tick query` if available).
3. `/tamekind list` → confirm animals fall into `SIMPLE`/`SLEEP` as you walk away, i.e.
   the level-of-detail system is actually shedding load.
4. Note any stutter when rain starts: that triggers shelter scans herd-wide at once,
   the most expensive moment in the mod.

---

## Reporting back

For each failure, the most useful bundle is:

1. The test number.
2. `/tamekind dump` output for the animal involved.
3. `/tamekind goals` output if it looks like a behaviour never triggers.
4. The `[Tamekind]` log lines (`debugLogs=true`).

A behaviour that "never happens" is usually a priority problem rather than a logic
problem: check `/tamekind goals` for a lower-numbered goal holding `MOVE` before
suspecting the goal's own conditions.
