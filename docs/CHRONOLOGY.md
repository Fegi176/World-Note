# Chronology contract

Fictional years are signed 64-bit integers and portable JSON decimal **strings**. Zero is an authored origin. `Present` starts at 0 and never follows the phone clock. Timestamps on records describe real editing, not history.

| Expression | Meaning |
|---|---|
| UNKNOWN | Valid unresolved time with optional explanation |
| EXACT | A whole canonical year |
| APPROXIMATE | Stated center and optional nonnegative tolerance; absent tolerance supplies no fabricated bounds |
| RANGE | Uncertain occurrence window, not a duration |
| OFFSET | Minimum/maximum signed offsets from fixed Present or an event/period occurrence/start/end |
| LOCAL_YEAR | Period start + (local year − 1); remains unresolved when start is unknown |
| WITHIN | A possible occurrence within period bounds; never a manufactured midpoint |

Period intervals use `[start,end)`. Exact within-period years range from `start` through `end−1`. Event duration uses separate start/end expressions and a duration flag; occurrence uncertainty remains separately authored. Unused expressions are preserved.

An opening/closing event reference resolves an otherwise unknown period boundary dynamically. Its selected event boundary is start/end for a duration event, occurrence otherwise. Editing that event recomputes the period view without copied dates.

Resolution returns low/high bounds, an explicitly stated/derived estimate, status, provenance, and diagnostics. Offset bounds propagate with checked arithmetic. Overflows, empty half-open periods, missing/cyclic anchors, reversed spans, hierarchy cycles, and contradictory known year order are diagnosed. Two events in the same year can have a valid before/after sequence.

Ordering constraints use typed boundaries and a topological presentation order. An undated record may be presented before its known successor without acquiring that successor's year. Ordinary lore graph cycles remain allowed. Cross-track overlapping ages remain allowed. An exact child outside its parent requires correction or an explicit `disputed`/`incomplete` association status.

Overview is clearly labeled **Not to scale**. Scaled rendering subtracts an integer viewport origin before conversion to drawing coordinates, uses overlap lanes, distinguishes uncertainty and duration, and gives unresolvable records a separate tray. Precision tests include a one-year delta near `Long.MIN_VALUE` and billion-year chronology.

Dependency/hierarchy depth is bounded at 128 during validation. Day/month/calendar conversion is extended scope and not implemented.
