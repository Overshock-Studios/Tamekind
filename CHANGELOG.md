# Changelog

## 0.2.0

Targets 26.1.2. 26.2 is not source-compatible; see `docs/PORTING-26.2.md`.

### Fixed

- Fixed the herd alpha never being elected, which silently disabled five advertised
  features at once. `HerdCoordinator.leaderFor` scored only an animal's *neighbours*,
  never the animal itself, so `leader == animal` could not be true anywhere. That one
  omission meant the alpha size bonus never applied, and leaders never published a
  shared shelter, graze or water position — so "leader-driven shelter and graze scans"
  and "follower piggybacking" did nothing. It also made the election disagree with
  itself: in a herd of three, A named B as leader while B named A, so pairs of animals
  followed each other in circles instead of forming one group. The candidate pool now
  includes the animal itself, which makes the election unanimous across the herd.
- Fixed wolves and foxes never hunting tagged prey in a released jar. Both hunt mixins
  reflected on the field name `"targetSelector"`, but string literals are not remapped,
  so the lookup resolved in a dev run and threw `NoSuchFieldException` in production —
  where it was swallowed by `catch (Throwable ignored)`. The two-way predator food-web
  was therefore dev-only. Both now use a `tamekind$targetSelector()` accessor mixin,
  matching the pattern Warband already uses, and no longer swallow failures.
- Fixed threat scanning forcing threats into a permanent crouch. `ThreatScanner`
  called `setShiftKeyDown(true)` on whatever it found and never cleared it, so every
  predator near an animal looked like it was sneaking. This also leaked across mods:
  Warband treats crouching as a signal that *reduces* detection range, so Tamekind was
  quietly making Warband's mobs stealthier. A read-only scan no longer mutates what it
  scans.
- Fixed breeding crowd control refusing to let you heal a tame pet or a wounded
  animal. Feeding a wolf is a heal, not a courtship, but crowd control saw only "food
  was used" and could refuse it once eight of the same type were nearby. It now applies
  only to a feed that would actually start breeding.
- Fixed `config/tamekind.properties` regenerating without the new keys on upgrade —
  every new setting is written into the commented template.
- Fixed tamed predators hunting their owner's livestock. `predators_of/minecraft/sheep`
  lists wolf, and the hunt goal is added to *every* wolf, so a pet wolf would work
  through its owner's own flock. This was latent — the reflection bug above meant the
  goal never actually attached in a released jar, so repairing that mixin is what would
  have exposed it. `TagHunting` now refuses to let a tamed predator hunt on its own,
  matching vanilla, where taming a wolf stops it hunting sheep.
- Fixed the first alpha trail point being dropped on a young world. The rate limiter
  compared against a zero-initialised timestamp, so at low game times the check
  swallowed the opening point and followers had no route to walk.

### Added

- **Temperament.** Every animal has a personality — skittish, steady, bold or curious —
  derived from its UUID, so it costs no save data and never changes across reloads. It
  scales alert radius, how long the animal freezes when startled, and how quickly it
  learns to trust you. Two cows in the same field now behave measurably differently.
  Toggle with `temperamentEnabled`.
- **Sentinel watch.** While the herd grazes, the alpha keeps its head up instead of
  eating: it holds position, sweeps its gaze around, scans at 1.5× the normal alert
  radius, and broadcasts danger to the whole herd the moment it spots something. The
  herd now reacts earlier than any single animal could, and the one animal not feeding
  is the visible reason why. Tuned with `sentinelEnabled`, `sentinelWatchTicks` and
  `sentinelAlertRadiusMultiplier`.
- **Approach etiquette.** Feeding while crouched builds trust faster
  (`crouchFeedTrustMultiplier`). Body language now reads the same way across both mods:
  crouch to be less alarming, sprint to be more.
- **Sentinel rotation.** The watch rotates through the herd's adults on a shift timer,
  so the alpha is no longer condemned to stand guard while everyone else eats. Derived
  from game time rather than stored, so it needs no save data and no handoff.
  `sentinelRotationEnabled`.
- **Heritable size.** A bred calf's size is now the midpoint of its parents plus a small
  wobble, persisted, so breeding for size compounds across generations instead of
  resetting every birth. Clamped to the same envelope as a wild roll so a line cannot
  drift into unusable extremes. Wild spawns still roll from their UUID.
  `heritableSizeEnabled`, `heritableSizeJitter`.
- **Isolation stress.** A herd animal with no herd-mates in range is jumpier and settles
  down to graze less readily. `isolationStressEnabled`, `isolationAlertMultiplier`.
- **Territorial retaliation.** Adults that watch enough herd-mates die nearby stop
  fleeing and hold their ground facing the threat. They never fight back — only panic is
  suppressed — so passive mobs stay passive. The count decays, so a herd farmed slowly
  over hours never turns defiant. `territorialRetaliationEnabled`, `cullMemoryTicks`,
  `cullVengeanceThreshold`, `cullVengeanceTicks`, `cullWitnessRadius`.
- **Trail-following.** Herd followers now walk the alpha's recorded route rather than
  making a beeline at it, so a moving herd strings out along ground that is known to be
  walkable instead of clumping and shoving itself into terrain the leader went around.
  The trail is transient by design.
- **Predator turf conflict.** Wolves now contest foxes. This needed no new system: the
  prey-keyed `predators_of/<prey>` tag already drives both flight and hunting, so
  rivalry is one shipped tag entry and remains fully datapack-overridable.
  `predatorTurfConflictEnabled`.
- **Body condition, opt-in and non-lethal** (`conditionEnabled`, **off by default**). A
  0..1 stat that drains only where the animal is actually simulated and is restored by a
  graze or drink it actually reached. Low condition slows an animal and makes it decline
  to mate. It is floored and **never deals damage** — livestock cannot starve while you
  are away, which is the failure mode that sinks every hunger system in this genre.
  Tuned with `conditionDecayIntervalTicks`, `conditionDecayPerInterval`,
  `conditionFloor`, `conditionGrazeRestore`, `conditionDrinkRestore`,
  `conditionBreedThreshold`, `conditionSpeedPenalty`.
- `/tamekind animal`, `dump` and `leader` now report `isAlpha` and `temperament`;
  `dump` additionally reports the current sentinel, isolation, inherited scale, trail
  length, witnessed culls, stand-ground state and condition.
- `docs/FEATURE-GAP.md` — competitive analysis against the herd-behaviour mods and the
  livestock sims, with explicit declines and reasons.
- `docs/PORTING-26.2.md` — the 26.2 migration, verified by cross-compiling.
- `CLAUDE.md` — graphify wiring plus the project's non-negotiables.
- Unit tests for temperament, heritable-size descent (including a 200-generation drift
  check) and the new memory state — retaliation decay, condition flooring, trail bounds
  (43 tests total).

### Changed

- Bumped to the newest 26.1.2 stack, matching Warband: Fabric Loader 0.19.3
  (was 0.19.2), Fabric API 0.155.2+26.1.2 (was 0.149.0), Loom 1.17.17
  (was 1.16-SNAPSHOT), Gradle 9.6.1 (was 9.4.1).
- `PanicGoal` uses `Vec3.atCenterOf(pos)` instead of `BlockPos.getCenter()`. Both work
  on 26.1.2, only the former survives 26.2, so this shrinks the port ahead of time.
- `DrinkGoal` checks `isFaceSturdy(..., Direction.UP)` rather than the deprecated
  `isSolid()`. The build is now warning-clean.

### Removed

- Deleted `data/tamekind/tags/blocks/` and `data/tamekind/tags/entity_types/` — 13
  files. Datapack tag directories have been singular (`block`, `entity_type`) since
  1.21, confirmed absent from the 26.1.2 vanilla jar, so these plural copies were dead
  weight that could never load and would drift out of sync with the real ones.

## 0.1.0

Initial release. Herds, panic, trust, habitat behaviour, predator tags, AI
level-of-detail, three config profiles, Serene Seasons and Warband soft compat.
