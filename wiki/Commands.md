# Commands

All under `/tamekind`, operator-only, and all read the **nearest animal** unless stated otherwise. Labels are deliberately terse and match the log output, because these are diagnostics rather than player-facing text.

## Diagnostics

| Command | Shows |
|---|---|
| `animal` | one-line summary: level-of-detail, baby, herdable, herd size, goal count, health, position, danger, trust, alpha, temperament |
| `dump` | the full multi-line state of one animal, including scale, sentinel, isolation, inherited scale, trail length, witnessed culls, stand-ground and condition |
| `goals` | the live goal table with priorities, flags and running state, plus every same-priority flag collision |
| `list` | animal count within 64 blocks, broken down by level-of-detail |
| `leader` | the nearest animal's alpha, whether it is one, herd size and the shared shelter |
| `trust` | how much the nearest animal trusts you |
| `trust map <player>` | that player's trust footprint across nearby animals |
| `season` | current season and whether breeding is allowed |
| `config` | every config value as loaded |
| `profile` | the active profile |

**`goals` is the one to reach for first** when a behaviour never fires. See [Troubleshooting](Troubleshooting).

## Changing things

| Command | Effect |
|---|---|
| `home set` | pins the nearest animal's home to its current position |
| `home clear` | removes it |
| `forget` | wipes danger, home, guard and shared positions for the nearest animal |
| `disable <type>` | toggles a runtime opt-out for an entity type, complementing the `tamekind:disabled` tag |
| `profile <name>` | switches to `vanilla+`, `realism` or `simulation` |
| `reload` | re-reads `config/tamekind.properties` and re-logs the goal table |

`reload` does not need a restart, which makes it the fast way to turn `debugLogs` on mid-session.

`forget` deliberately keeps an animal's inherited scale. Descent is not a memory it can be talked out of.
