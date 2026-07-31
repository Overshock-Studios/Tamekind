---
name: tamekind-audit
description: Run the Tamekind codebase audit and interpret its findings. Use before any release, before committing a batch of behaviour changes, after adding or reprioritising an animal goal, after touching a mixin, after adding a config key, and after adding a tag. Also use when asked to check code quality, look for dead code, or find goal-priority conflicts.
---

# Tamekind audit

```bash
python scripts/audit.py            # full report
python scripts/audit.py --quiet    # errors only, for a pre-commit gate
```

Exit code 1 on any ERROR. WARNINGs never fail the run: they are judgement calls.

**Run it before every release, and after any change to goals, mixins, tags, or config.**
It is not a general linter; every rule exists because that exact mistake shipped at least
once, here or in a sibling mod.

A clean audit means the code is tidy, not that the mod works. For that, see
`docs/TESTING.md`.

## ERRORS: fix before shipping

| Rule | Why it exists |
|---|---|
| Unused imports | Two shipped in `MountObedienceGoal`, left by an edit that deleted the last usage. |
| Unused private constants | Dead tuning knobs read as live config and mislead the next reader. |
| Mixin helper without `@Unique` | A `tamekind$` prefix is convention only; `@Unique` makes the compiler enforce it. |
| Mixin not listed in `tamekind.mixins.json` | An unlisted mixin never applies, silently, and the behaviour it implements simply does not exist. |
| `defaultRequire` not 1 | It must stay 1 so a moved target is fatal at load rather than a mod that quietly does nothing. |
| **Reflection on a Minecraft member name** | **The worst bug this codebase has shipped.** See below. |
| `BlockPos.getCenter()` | Removed in 26.2. `Vec3.atCenterOf(pos)` works in both, so there is no reason to write the doomed one. |
| Plural datapack tag directory | `tags/blocks/` and `tags/entity_types/` shipped as dead copies Minecraft could never load. Folders are singular since 1.21. |
| Missing tag file | A `TagKey` with no JSON resolves to an empty tag, silently, and the behaviour it drives stops applying to anything. |
| Client class reference | Tamekind is server-side. `net.minecraft.client.*` compiles in dev and throws `NoClassDefFoundError` on a dedicated server. |
| Config key/arg misalignment | `toPropertiesText()` interpolates over a hundred args positionally. A mismatch throws `MissingFormatArgumentException` **at runtime**, so the config silently stops saving. |
| Config field not wired | A knob that is not parsed, not in the template, or read by nobody looks configurable and is not. |
| Raw `addGoal(<int>, ...)` | A priority chosen without looking at vanilla's table. Use a `GoalPriorities` constant. |
| Movement goal ignoring `skipMovementGoals` | Breaks the farm guarantee: a pen of name-tagged cows starts wandering. |
| Hardcoded display text | `Component.literal("some English")` cannot be translated. |
| Bare `Component.translatable` | Shows a vanilla client the raw key. |
| Lang key missing from `en_us.json` | The fallback hides it in English, so it only ever shows up as untranslatable text in someone else's language. |
| Em dash | House style. See below. |

## Never reflect on a Minecraft member name

This is the rule with the worst track record. `WolfHuntMixin` and `FoxHuntMixin` both did:

```java
PathfinderMob.class.getSuperclass().getDeclaredField("targetSelector")
```

wrapped in `catch (Throwable ignored)`. String literals are **not remapped**, so the lookup
resolves in a dev run and throws `NoSuchFieldException` against the intermediary names a
released jar runs on. The catch swallowed it. Result: the two-way predator food-web, a
headline README feature, **never worked in a single real installation** and nothing in the
logs said so. It was found by dumping the built jar and seeing the unremapped literal still
sitting in the constant pool.

Use an `@Accessor` mixin. The mixin processor remaps the field name for you, which is the
entire point of `MobGoalSelectorAccessor`.

Reflecting into *another mod* is the opposite case and is how soft compat has to work: that
mod's own API names are not remapped either, and the class may be absent entirely.
`SereneSeasonsCompat` is the sanctioned example, and the audit reports it as a warning
rather than an error so the coupling stays visible.

## Goal priorities

**The recurring bug class in this genre**, and the reason `GoalPriorities` exists as a
separate class with vanilla's table written into it.

Tamekind injects into the *same* selector vanilla already populated at priorities 0 to 8.
A goal may only take a conflicting flag from a goal of **strictly lower** priority
(`WrappedGoal.canBeReplacedBy` compares `<`), so:

- equal priority plus a shared flag is a coin flip, not precedence
- to reliably beat a vanilla goal you must sit at a strictly lower number

`HerdFollowGoal` shipped at 6, exactly where vanilla puts `WaterAvoidingRandomStrollGoal`,
both holding `MOVE`. Herd following won a coin flip against random wandering instead of
leading the herd. It now sits at 4, the one genuinely free slot, because vanilla's
`FollowParentGoal` there declares no flags at all.

Note the near miss: `BabyAnchorGoal` shares priority 0 with `FloatGoal` and that is **fine**,
because FloatGoal declares only `JUMP`. Do not "fix" it. Check the flags before assuming a
shared priority is a conflict, and read them from the real class rather than memory:

```bash
javap -p -c -classpath <deobf-mc-jar> net.minecraft.world.entity.animal.sheep.Sheep \
  | sed -n '/registerGoals/,/^  [a-z]/p' | grep -E "iconst_|bipush|class net/minecraft"
```

The audit only sees Tamekind's own goals. For ties against vanilla and other mods, use
`/tamekind goals` in a live world: it reads the real selector and prints every
same-priority flag collision. `GoalPrioritiesTest` pins the six accepted vanilla ties, so
a new one fails the build.

## Recognising other mods' content

**Use entity-type tags, not id matching in Java.** `isMountType` used to be a chain of
`getPath().contains("horse")` comparisons, which recognised exactly the ten vanilla ids
somebody typed in. A mount from another mod silently missed the trust bonus and could only
be added by a code change and a release. It is now `tamekind:mounts`, shipped with the same
ten so the refactor was behaviour-neutral.

A tag refactor should stay behaviour-neutral unless the change is the point: widening a tag
silently changes gameplay.

## Displayed text

**Always `Component.translatableWithFallback(key, english)`.** Never `Component.literal` for
anything a player reads, and never `Component.translatable` alone.

The fallback is not optional politeness. Tamekind is **server-side with no client mod**, so
the player is normally on a vanilla client that has never heard of this mod.
`Component.translatable` alone would show them `tamekind.breeding.crowded`. Sending both
means a vanilla client renders the English and a client with a Tamekind language file
renders the translation, with no branching on either side.

Exempt: `com.tamekind.command`, operator diagnostics whose labels deliberately match the
field names in `/tamekind dump`. The audit skips that whole package, so a player-facing
message added there will not be caught. Put it elsewhere.

## No em dashes

A house style rule rather than a correctness one, but a mechanical one, so it is checked
mechanically instead of being remembered. Use a comma for an aside, a colon before an
explanation, a semicolon between two full clauses, or parentheses.

Do not substitute blindly. A comma is wrong about a quarter of the time and gives you a
comma splice; when the dash introduced an explanation the answer is usually a colon, and
when the text after it starts with a conjunction the answer is usually a comma.

## WARNINGS: each needs a deliberate answer, not a reflex

- **Mixin target / accessor to re-verify.** Not a defect. `defaultRequire: 1` makes a rename
  fatal at load, so this list is the porting checklist for every Minecraft update. Printed
  so it gets read, not so it fails.
- **Soft-compat reflection.** Legitimate, but re-check when that mod updates.
- **Goal priority tie within Tamekind.** Acceptable when the goals are mutually exclusive in
  practice; a bug when both can be eligible for the same animal at the same moment.
- **Flagless goal moves the animal.** No flags means no arbitration, so it must yield on its
  own terms. The bookkeeping tickers (`HerdTrailGoal`, `ConditionGoal`, `AgeScaleGoal`) are
  flagless on purpose and move nothing.
- **Inline fully-qualified names.** Cosmetic, but it reads as unfinished. Pre-existing
  instances are fine to leave; do not add new ones.
- **Possibly unused private method.** Heuristic: it counts `name(` calls and `::name`
  references, so it can miss reflective or mixin-injected use. Confirm before deleting.
- **Registry path matching.** See "Recognising other mods' content" above.
- **Unused lang key.** A key can legitimately be staged ahead of the code that uses it.

## Deliberately not copied from the sibling audits

- **Every config key must appear in the README** (CullTag). That mod has a handful of knobs;
  Tamekind has over a hundred and its README points at the fully-commented properties file
  on purpose rather than duplicating it. Warband, the closest sibling by config size, does
  not carry the check either. `check_config_fields_wired` covers the part that matters.
- **`environment: server`** (CullTag). Tamekind keeps `*` deliberately, so the jar can sit on
  a server alone without refusing to load in singleplayer. The audit asserts `*` instead.

## What the audit cannot catch

It is static analysis on a behaviour mod. It says nothing about whether a goal *fires*,
whether the herd reads as alive, or whether tuning feels right. For those:

- `debugLogs=true` logs the live goal table and every same-priority collision once per
  config load
- `/tamekind goals` and `/tamekind dump` in a live world
- `docs/TESTING.md` is the in-game plan, ordered so the blockers come first
