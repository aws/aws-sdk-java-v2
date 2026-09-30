# Paired A/B summary — `merged`

Arms (baseline first): `async-stock`, `hybridsign`

- `async-stock`: sdk_commit `published-2.46.10`, 4 reps per case
- `hybridsign`: sdk_commit `3e6ea7bb6c3-plus-hybrid-signing`, 4 reps per case

**19 of 64 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/batch-get, v2-async/small-get, v2-async/small-put, v2-sync/small-get. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

**WARNING: arms were built from different harness commits (3e6ea7bb6c3, c1d88972e18). The comparison is not clean — rebuild both jars from the same harness before drawing conclusions.**

## application cpu µs/op

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 135.5 | 93.5 | -31.0% | ±4.5% | 4/4 |
| v2-async | small-get | 230.4 | 219.2 | -4.7% | ±3.9% | 3/4 |
| v2-sync | small-put | 127.0 | 88.2 | -30.6% | ±1.3% | 4/4 |
| v2-async | small-put | 229.9 | 208.9 | -9.1% | ±3.2% | 4/4 |
| v2-sync | batch-get | 562.1 | 468.6 | -16.6% | ±3.7% | 4/4 |
| v2-async | batch-get | 650.6 | 631.8 | -2.9% | ±1.5% | 4/4 |
| v2-sync | batch-put | 765.2 | 413.1 | -45.9% | ±2.7% | 4/4 |
| v2-async | batch-put | 826.4 | 519.6 | -37.0% | ±3.1% | 4/4 |

## mean latency µs

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 173.1 | 129.4 | -25.2% | ±6.5% | 4/4 |
| v2-async | small-get | 251.1 | 226.2 | -9.8% | ±3.3% | 4/4 |
| v2-sync | small-put | 163.7 | 123.2 | -24.7% | ±1.6% | 4/4 |
| v2-async | small-put | 251.8 | 216.2 | -14.1% | ±4.1% | 4/4 |
| v2-sync | batch-get | 609.6 | 513.5 | -15.7% | ±3.7% | 4/4 |
| v2-async | batch-get | 711.3 | 638.9 | -10.2% | ±2.3% | 4/4 |
| v2-sync | batch-put | 805.2 | 449.5 | -44.1% | ±2.7% | 4/4 |
| v2-async | batch-put | 882.1 | 561.2 | -36.3% | ±3.2% | 4/4 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 173.3 | 129.5 | -25.2% | ±6.5% | 4/4 |
| v2-async | small-get | 260.4 | 234.7 | -9.8% | ±3.3% | 4/4 |
| v2-sync | small-put | 163.8 | 123.3 | -24.7% | ±1.6% | 4/4 |
| v2-async | small-put | 262.0 | 224.2 | -14.4% | ±4.2% | 4/4 |
| v2-sync | batch-get | 609.8 | 513.9 | -15.7% | ±3.7% | 4/4 |
| v2-async | batch-get | 720.6 | 648.0 | -10.1% | ±2.3% | 4/4 |
| v2-sync | batch-put | 805.5 | 449.8 | -44.1% | ±2.7% | 4/4 |
| v2-async | batch-put | 892.6 | 570.3 | -36.0% | ±3.1% | 4/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 139.1 | 97.1 | -30.2% | ±4.8% | 4/4 |
| v2-async | small-get | 247.0 | 229.8 | -6.9% | ±1.7% | 4/4 |
| v2-sync | small-put | 130.2 | 91.1 | -30.1% | ±1.3% | 4/4 |
| v2-async | small-put | 248.3 | 220.0 | -11.4% | ±4.0% | 4/4 |
| v2-sync | batch-get | 570.7 | 476.9 | -16.4% | ±3.6% | 4/4 |
| v2-async | batch-get | 706.6 | 663.5 | -6.1% | ±2.1% | 4/4 |
| v2-sync | batch-put | 770.5 | 417.7 | -45.7% | ±2.6% | 4/4 |
| v2-async | batch-put | 858.1 | 541.5 | -36.8% | ±3.4% | 4/4 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 169.4 | 173.3 | 179.3 | 5.8% |
| v2-sync | small-get | `hybridsign` | 123.0 | 129.5 | 145.6 | 18.4% |
| v2-async | small-get | `async-stock` | 248.2 | 260.4 | 266.4 | 7.3% |
| v2-async | small-get | `hybridsign` | 232.1 | 234.7 | 237.8 | 2.5% |
| v2-sync | small-put | `async-stock` | 161.2 | 163.8 | 165.1 | 2.4% |
| v2-sync | small-put | `hybridsign` | 120.1 | 123.3 | 125.6 | 4.6% |
| v2-async | small-put | `async-stock` | 258.3 | 262.0 | 265.9 | 2.9% |
| v2-async | small-put | `hybridsign` | 216.4 | 224.2 | 236.0 | 9.1% |
| v2-sync | batch-get | `async-stock` | 603.2 | 609.8 | 625.1 | 3.6% |
| v2-sync | batch-get | `hybridsign` | 494.5 | 513.9 | 540.5 | 9.3% |
| v2-async | batch-get | `async-stock` | 710.5 | 720.6 | 733.2 | 3.2% |
| v2-async | batch-get | `hybridsign` | 636.3 | 648.0 | 652.9 | 2.6% |
| v2-sync | batch-put | `async-stock` | 761.4 | 805.5 | 844.1 | 10.9% |
| v2-sync | batch-put | `hybridsign` | 442.0 | 449.8 | 455.5 | 3.1% |
| v2-async | batch-put | `async-stock` | 838.8 | 892.6 | 920.7 | 9.8% |
| v2-async | batch-put | `hybridsign` | 565.0 | 570.3 | 573.4 | 1.5% |

