# How this directory was assembled

The original 4-rep paired run (`../20260929-2253/`) was killed by a 30-minute background time limit after
29 of 96 runs. Rep 1 (its first 24 result rows) was complete and is kept; its 5 partial rep-2 rows are
discarded. Reps 2-4 were then run as one-rep invocations of `paired-ab.sh` with identical parameters,
alternating the `--jars` order so the arm-order reversal of a normal 4-rep run is preserved:

| rep | source | arm order |
|---|---|---|
| 1 | `../20260929-2253/results.csv` rows 1-24 | stock, bridge |
| 2 | `../../paired-async-c1-chunks/20260929-2324/` | bridge, stock |
| 3 | `../../paired-async-c1-chunks/20260929-2350/` | stock, bridge |
| 4 | `../../paired-async-c1-chunks/20260930-0017/` | bridge, stock |

`paired_ab_summary.py` infers reps from row order per (phase, client, scenario), so concatenation in rep
order is all the merge needs. `manifest.md` is rep 1's; the chunks' manifests record identical parameters.
