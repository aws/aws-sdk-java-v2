# Paired A/B summary — `20260930-2143`

Arms (baseline first): `hybridsign`, `async-stock`

- `hybridsign`: sdk_commit `3e6ea7bb6c3-plus-hybrid-signing`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**4 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/small-get, v2-async/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

**WARNING: arms were built from different harness commits (3e6ea7bb6c3, c1d88972e18). The comparison is not clean — rebuild both jars from the same harness before drawing conclusions.**

## application cpu µs/op

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 91.6 | 134.5 | +46.8% | ±0.0% | 0/1 |
| v2-async | small-get | 218.8 | 234.3 | +7.1% | ±0.0% | 0/1 |
| v2-sync | small-put | 88.5 | 124.9 | +41.1% | ±0.0% | 0/1 |
| v2-async | small-put | 203.3 | 229.5 | +12.9% | ±0.0% | 0/1 |
| v2-sync | batch-get | 448.8 | 557.2 | +24.2% | ±0.0% | 0/1 |
| v2-async | batch-get | 628.4 | 657.5 | +4.6% | ±0.0% | 0/1 |
| v2-sync | batch-put | 410.8 | 720.9 | +75.5% | ±0.0% | 0/1 |
| v2-async | batch-put | 520.1 | 850.3 | +63.5% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 126.0 | 171.2 | +35.9% | ±0.0% | 0/1 |
| v2-async | small-get | 223.6 | 253.3 | +13.3% | ±0.0% | 0/1 |
| v2-sync | small-put | 123.6 | 161.1 | +30.3% | ±0.0% | 0/1 |
| v2-async | small-put | 208.7 | 255.4 | +22.4% | ±0.0% | 0/1 |
| v2-sync | batch-get | 494.2 | 603.0 | +22.0% | ±0.0% | 0/1 |
| v2-async | batch-get | 627.4 | 724.1 | +15.4% | ±0.0% | 0/1 |
| v2-sync | batch-put | 448.9 | 761.2 | +69.6% | ±0.0% | 0/1 |
| v2-async | batch-put | 561.0 | 910.2 | +62.2% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 126.1 | 171.3 | +35.8% | ±0.0% | 0/1 |
| v2-async | small-get | 232.1 | 262.6 | +13.1% | ±0.0% | 0/1 |
| v2-sync | small-put | 123.7 | 161.2 | +30.3% | ±0.0% | 0/1 |
| v2-async | small-put | 216.4 | 265.9 | +22.9% | ±0.0% | 0/1 |
| v2-sync | batch-get | 494.5 | 603.2 | +22.0% | ±0.0% | 0/1 |
| v2-async | batch-get | 636.3 | 733.2 | +15.2% | ±0.0% | 0/1 |
| v2-sync | batch-put | 449.2 | 761.4 | +69.5% | ±0.0% | 0/1 |
| v2-async | batch-put | 570.0 | 920.7 | +61.5% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 94.4 | 137.6 | +45.8% | ±0.0% | 0/1 |
| v2-async | small-get | 230.4 | 249.4 | +8.2% | ±0.0% | 0/1 |
| v2-sync | small-put | 92.2 | 129.4 | +40.3% | ±0.0% | 0/1 |
| v2-async | small-put | 211.8 | 250.4 | +18.2% | ±0.0% | 0/1 |
| v2-sync | batch-get | 458.2 | 564.6 | +23.2% | ±0.0% | 0/1 |
| v2-async | batch-get | 652.0 | 716.2 | +9.8% | ±0.0% | 0/1 |
| v2-sync | batch-put | 414.8 | 726.6 | +75.2% | ±0.0% | 0/1 |
| v2-async | batch-put | 541.0 | 888.8 | +64.3% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `hybridsign` | 126.1 | 126.1 | 126.1 | 0.0% |
| v2-sync | small-get | `async-stock` | 171.3 | 171.3 | 171.3 | 0.0% |
| v2-async | small-get | `hybridsign` | 232.1 | 232.1 | 232.1 | 0.0% |
| v2-async | small-get | `async-stock` | 262.6 | 262.6 | 262.6 | 0.0% |
| v2-sync | small-put | `hybridsign` | 123.7 | 123.7 | 123.7 | 0.0% |
| v2-sync | small-put | `async-stock` | 161.2 | 161.2 | 161.2 | 0.0% |
| v2-async | small-put | `hybridsign` | 216.4 | 216.4 | 216.4 | 0.0% |
| v2-async | small-put | `async-stock` | 265.9 | 265.9 | 265.9 | 0.0% |
| v2-sync | batch-get | `hybridsign` | 494.5 | 494.5 | 494.5 | 0.0% |
| v2-sync | batch-get | `async-stock` | 603.2 | 603.2 | 603.2 | 0.0% |
| v2-async | batch-get | `hybridsign` | 636.3 | 636.3 | 636.3 | 0.0% |
| v2-async | batch-get | `async-stock` | 733.2 | 733.2 | 733.2 | 0.0% |
| v2-sync | batch-put | `hybridsign` | 449.2 | 449.2 | 449.2 | 0.0% |
| v2-sync | batch-put | `async-stock` | 761.4 | 761.4 | 761.4 | 0.0% |
| v2-async | batch-put | `hybridsign` | 570.0 | 570.0 | 570.0 | 0.0% |
| v2-async | batch-put | `async-stock` | 920.7 | 920.7 | 920.7 | 0.0% |

