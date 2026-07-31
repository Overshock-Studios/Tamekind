# Troubleshooting

## A behaviour never seems to fire

**Check the goal table first.** This is the answer far more often than the behaviour's own conditions:

```
/tamekind goals
```

It prints the live goal selector for the nearest animal, including vanilla's goals and other mods', and flags every same-priority collision. A goal at a strictly *lower* priority holding `MOVE` wins every time, so a Tamekind behaviour sitting below it will never get to move the animal.

Then check the animal itself:

```
/tamekind dump
```

That reports level-of-detail, herd size, alpha, sentinel, isolation, temperament, trust, danger, condition, scale and the rest.

Common causes, in the order worth checking:

1. **The animal is opted out.** Leashed, mounted, named, breeding and tamed animals keep their vanilla AI by design. `dump` shows this.
2. **Level-of-detail.** Most behaviours only run at `FULL`, which means near a player. `/tamekind list` shows the breakdown nearby.
3. **The subsystem is off.** Check `/tamekind config` and the active profile.
4. **A tag is empty.** A `TagKey` with no datapack file resolves to an empty tag silently, and the behaviour it drives stops applying to anything.

## Animals sound like they are being hurt

Fixed in 0.2.0. Earlier versions played the generic hurt sound as an alert cue, so walking up to a herd sounded like the animals were being damaged. Nothing was. They now use their own voice, pitched up.

If you are on 0.2.0 or later and still hear it, `debugLogs=true` will name the real damage source:

```
[Tamekind] DAMAGE cow#123 amount=2.00 type=mob attacker=wolf scale=1.180 hp=8.0/10.0
```

Note that **wolves genuinely hunt sheep, chickens and rabbits** now. That is the predator food web working, not a bug. A *tamed* wolf will never hunt its owner's livestock.

## Animals keep changing size

Fixed in 0.2.0. The alpha's size bonus was re-evaluated every two seconds with no hysteresis, and because herd membership shifts as animals graze, the bonus flickered on and off.

`/tamekind dump` reports `scaleBase`, `scaleFinal` and `modifiers`. A steady gap between base and final is normal, since babies and alphas carry modifiers. A gap that keeps moving is not.

## Animals escape a pen, or trample crops

Stampede knockback only applies to a panicking herd above `minStampedeHerdSize`, and it skips babies and any animal opted out of Tamekind movement. Crop trampling is **off by default** (`stampedeCropDamageEnabled`).

If animals are leaving an enclosure, the likely cause is panic escape scoring picking a position outside it. Raise `panicRadius` awareness or set `panicEnabled=false` for a fully static farm.

## Performance on a large farm

`/tamekind list` shows how many nearby animals are at each level-of-detail. If most are `FULL` on a big farm, lower `fullAiRange`.

The most expensive moment in the mod is the start of rain, when shelter scanning runs herd-wide at once. `shelterSearchRadius` is the knob; it scales cubically, so small reductions help a lot.

## Enabling diagnostics

```
config/tamekind.properties
  debugLogs=true
```

Then `/tamekind reload`. No restart needed. This logs the goal table with every collision once per config load, narrates alpha handovers with the resulting scale, and names the source of every hit an animal takes.

## Reporting something

The useful bundle is:

1. `/tamekind dump` for the animal involved
2. `/tamekind goals` if a behaviour never fires
3. The `[Tamekind]` log lines with `debugLogs=true`
4. Your mod list, if another AI mod is installed
