## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Sibling mod

`../Warband` is the hostile-mob counterpart by the same author and targets the same
Minecraft version. Keep conventions aligned with it: the `MobGoalSelectorAccessor`
accessor-mixin pattern for goal injection, `<mod>$`-prefixed mixin method names, a
properties config with named profiles, a `/<modid>` debug command tree, and
reflection-free soft compat classes under `compat/`. Warband owns hostile AI and
Tamekind owns passive AI: do not duplicate systems across the two.

## Non-negotiables

- **Never break vanilla farms.** Leashed, mounted, named, breeding and tamed animals
  opt out of movement goals via `TamekindAnimalRules.skipMovementGoals`. Anything new
  that moves an animal must respect it.
- **Server-side only.** No client entrypoint, no rendering, no packets. `environment`
  stays `*` so the jar can sit on a server alone.
- **No new items, blocks or entities.** Tamekind is a behaviour mod; per-animal
  variation is expressed through AI and attributes, never through content.
- **Never reflect on Minecraft member names.** String literals are not remapped, so
  `getDeclaredField("targetSelector")` works in dev and fails silently in a released
  jar. Use an `@Accessor` mixin.
- **`Vec3.atCenterOf(pos)`, not `pos.getCenter()`**: see `docs/PORTING-26.2.md`.
