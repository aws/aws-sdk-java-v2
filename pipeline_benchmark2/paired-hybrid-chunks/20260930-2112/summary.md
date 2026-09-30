# Paired A/B summary — `20260930-2112`

Arms (baseline first): `hybridsign`, `async-stock`

- `hybridsign`: sdk_commit `3e6ea7bb6c3-plus-hybrid-signing`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**5 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/batch-get, v2-async/small-get, v2-async/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

**WARNING: arms were built from different harness commits (3e6ea7bb6c3, c1d88972e18). The comparison is not clean — rebuild both jars from the same harness before drawing conclusions.**

## application cpu µs/op

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 101.9 | 135.0 | +32.5% | ±0.0% | 0/1 |
| v2-async | small-get | 221.5 | 235.4 | +6.3% | ±0.0% | 0/1 |
| v2-sync | small-put | 86.3 | 126.3 | +46.3% | ±0.0% | 0/1 |
| v2-async | small-put | 207.5 | 233.0 | +12.3% | ±0.0% | 0/1 |
| v2-sync | batch-get | 465.5 | 558.8 | +20.0% | ±0.0% | 0/1 |
| v2-async | batch-get | 635.4 | 650.6 | +2.4% | ±0.0% | 0/1 |
| v2-sync | batch-put | 405.5 | 803.4 | +98.1% | ±0.0% | 0/1 |
| v2-async | batch-put | 511.9 | 845.5 | +65.2% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 145.5 | 173.1 | +19.0% | ±0.0% | 0/1 |
| v2-async | small-get | 229.3 | 254.7 | +11.1% | ±0.0% | 0/1 |
| v2-sync | small-put | 120.0 | 164.2 | +36.8% | ±0.0% | 0/1 |
| v2-async | small-put | 214.2 | 251.8 | +17.6% | ±0.0% | 0/1 |
| v2-sync | batch-get | 510.7 | 607.0 | +18.9% | ±0.0% | 0/1 |
| v2-async | batch-get | 642.1 | 700.7 | +9.1% | ±0.0% | 0/1 |
| v2-sync | batch-put | 441.7 | 843.8 | +91.0% | ±0.0% | 0/1 |
| v2-async | batch-put | 556.2 | 907.0 | +63.1% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 145.6 | 173.2 | +19.0% | ±0.0% | 0/1 |
| v2-async | small-get | 237.8 | 264.5 | +11.2% | ±0.0% | 0/1 |
| v2-sync | small-put | 120.1 | 164.3 | +36.8% | ±0.0% | 0/1 |
| v2-async | small-put | 222.4 | 262.0 | +17.8% | ±0.0% | 0/1 |
| v2-sync | batch-get | 511.0 | 607.1 | +18.8% | ±0.0% | 0/1 |
| v2-async | batch-get | 651.0 | 710.5 | +9.1% | ±0.0% | 0/1 |
| v2-sync | batch-put | 442.0 | 844.1 | +91.0% | ±0.0% | 0/1 |
| v2-async | batch-put | 565.0 | 917.3 | +62.4% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | hybridsign mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 106.8 | 139.0 | +30.1% | ±0.0% | 0/1 |
| v2-async | small-get | 231.4 | 250.8 | +8.4% | ±0.0% | 0/1 |
| v2-sync | small-put | 89.6 | 129.0 | +44.0% | ±0.0% | 0/1 |
| v2-async | small-put | 217.8 | 248.8 | +14.2% | ±0.0% | 0/1 |
| v2-sync | batch-get | 474.0 | 566.8 | +19.6% | ±0.0% | 0/1 |
| v2-async | batch-get | 669.0 | 702.0 | +4.9% | ±0.0% | 0/1 |
| v2-sync | batch-put | 411.0 | 808.2 | +96.6% | ±0.0% | 0/1 |
| v2-async | batch-put | 534.8 | 882.2 | +65.0% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `hybridsign` | 145.6 | 145.6 | 145.6 | 0.0% |
| v2-sync | small-get | `async-stock` | 173.2 | 173.2 | 173.2 | 0.0% |
| v2-async | small-get | `hybridsign` | 237.8 | 237.8 | 237.8 | 0.0% |
| v2-async | small-get | `async-stock` | 264.5 | 264.5 | 264.5 | 0.0% |
| v2-sync | small-put | `hybridsign` | 120.1 | 120.1 | 120.1 | 0.0% |
| v2-sync | small-put | `async-stock` | 164.3 | 164.3 | 164.3 | 0.0% |
| v2-async | small-put | `hybridsign` | 222.4 | 222.4 | 222.4 | 0.0% |
| v2-async | small-put | `async-stock` | 262.0 | 262.0 | 262.0 | 0.0% |
| v2-sync | batch-get | `hybridsign` | 511.0 | 511.0 | 511.0 | 0.0% |
| v2-sync | batch-get | `async-stock` | 607.1 | 607.1 | 607.1 | 0.0% |
| v2-async | batch-get | `hybridsign` | 651.0 | 651.0 | 651.0 | 0.0% |
| v2-async | batch-get | `async-stock` | 710.5 | 710.5 | 710.5 | 0.0% |
| v2-sync | batch-put | `hybridsign` | 442.0 | 442.0 | 442.0 | 0.0% |
| v2-sync | batch-put | `async-stock` | 844.1 | 844.1 | 844.1 | 0.0% |
| v2-async | batch-put | `hybridsign` | 565.0 | 565.0 | 565.0 | 0.0% |
| v2-async | batch-put | `async-stock` | 917.3 | 917.3 | 917.3 | 0.0% |

