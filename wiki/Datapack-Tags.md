# Datapack Tags

Tamekind's tuning lives in tags rather than in Java, so a pack or another mod can reshape it without a code change and without waiting on a release. This is the supported way to influence Tamekind; the [API](API) is read-only on purpose.

Ship your entries with `"replace": false` so you extend rather than clobber, and `"required": false` on any entry naming a mod that might not be installed.

```json
{
  "replace": false,
  "values": [
    { "id": "somemod:capybara", "required": false }
  ]
}
```

## Entity tags

`data/tamekind/tags/entity_type/`

| Tag | Effect |
|---|---|
| `herdable` | joins the whole herd layer: following, alpha election, the watch, shared positions, panic spread |
| `predators` | counts as a threat to any animal that sees it |
| `predators_of/<namespace>/<path>` | **drives both directions at once**: the prey flees this predator, and the predator hunts the prey |
| `disabled` | Tamekind ignores this type entirely |
| `freezers` | freezes far longer when alerted, and skips the drift-away stage. Rabbits and chickens by default |
| `mating_displays` | performs a courtship display before breeding |
| `heat_sensitive` | seeks shade at midday and all summer |
| `mounts` | feeding counts as feeding a mount, which extends how long the trust lasts |

`predators_of` is the interesting one. Because a single tag drives fleeing and hunting, adding wolf to `predators_of/minecraft/fox` makes wolves contest foxes in both directions with no code at all. That is exactly how the shipped wolf-versus-fox rivalry works.

**Caveat:** the hunting half only attaches for wolves and foxes today. A modded predator listed in `predators_of` will be fled from correctly, but will not itself hunt. See [Mod Compatibility](Mod-Compatibility).

## Block tags

`data/tamekind/tags/block/`

| Tag | Effect |
|---|---|
| `grazing_blocks` | animals graze and rest here |
| `shelter_blocks` | counts as cover overhead, even under open sky |
| `comfort_blocks` | strongly preferred for resting and night huddles |
| `nest_blocks` | chickens will only lay near one of these |
| `water_blocks` | animals drink at the edge of these |
| `avoid_blocks` | never walked on or stood in. Lava, fire, cactus, powder snow |
| `soft_avoid_blocks` | avoided when there is a better option. Crops, farmland, snow |

`soft_avoid_blocks` is what keeps a panicking herd off your wheat without making them refuse to cross a field to escape a wolf.

Adding a modded hay-alike to `comfort_blocks` and a modded pond block to `water_blocks` is usually all a pack needs.

## Biome tags

`data/tamekind/tags/worldgen/biome/comfortable_in/<namespace>/<path>`

Per-species. An animal in a biome listed as comfortable for its type grazes longer and settles more readily. Keyed by species so a modded desert creature can be comfortable somewhere a cow is not.

## A tag with no file is silently empty

A `TagKey` whose JSON is missing resolves to an empty tag. Nothing throws and nothing logs, and the behaviour it drives simply stops applying to anything. If a habitat behaviour has stopped working after a datapack change, that is the first thing to check.
