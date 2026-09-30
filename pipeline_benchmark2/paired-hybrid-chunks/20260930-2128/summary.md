# Paired A/B summary — `20260930-2128`

Arms (baseline first): `async-stock`, `hybridsign`

- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case
- `hybridsign`: sdk_commit `3e6ea7bb6c3-plus-hybrid-signing`, 1 reps per case

**4 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/small-get, v2-async/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

**WARNING: arms were built from different harness commits (3e6ea7bb6c3, c1d88972e18). The comparison is not clean — rebuild both jars from the same harness before drawing conclusions.**

## application cpu µs/op

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 133.7 | 90.5 | -32.3% | ±0.0% | 1/1 |
| v2-async | small-get | 236.3 | 218.8 | -7.4% | ±0.0% | 1/1 |
| v2-sync | small-put | 128.6 | 90.3 | -29.8% | ±0.0% | 1/1 |
| v2-async | small-put | 227.0 | 216.9 | -4.4% | ±0.0% | 1/1 |
| v2-sync | batch-get | 577.4 | 467.8 | -19.0% | ±0.0% | 1/1 |
| v2-async | batch-get | 635.6 | 629.2 | -1.0% | ±0.0% | 1/1 |
| v2-sync | batch-put | 760.9 | 416.9 | -45.2% | ±0.0% | 1/1 |
| v2-async | batch-put | 776.3 | 522.8 | -32.7% | ±0.0% | 1/1 |

## mean latency µs

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 169.2 | 122.9 | -27.4% | ±0.0% | 1/1 |
| v2-async | small-get | 256.9 | 224.9 | -12.5% | ±0.0% | 1/1 |
| v2-sync | small-put | 164.6 | 123.9 | -24.7% | ±0.0% | 1/1 |
| v2-async | small-put | 248.3 | 227.3 | -8.5% | ±0.0% | 1/1 |
| v2-sync | batch-get | 624.9 | 509.1 | -18.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 702.5 | 642.1 | -8.6% | ±0.0% | 1/1 |
| v2-sync | batch-put | 800.3 | 452.3 | -43.5% | ±0.0% | 1/1 |
| v2-async | batch-put | 828.0 | 564.4 | -31.8% | ±0.0% | 1/1 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 169.4 | 123.0 | -27.4% | ±0.0% | 1/1 |
| v2-async | small-get | 266.4 | 233.4 | -12.4% | ±0.0% | 1/1 |
| v2-sync | small-put | 164.8 | 124.0 | -24.8% | ±0.0% | 1/1 |
| v2-async | small-put | 258.3 | 236.0 | -8.6% | ±0.0% | 1/1 |
| v2-sync | batch-get | 625.1 | 509.4 | -18.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 711.3 | 651.7 | -8.4% | ±0.0% | 1/1 |
| v2-sync | batch-put | 800.4 | 452.6 | -43.5% | ±0.0% | 1/1 |
| v2-async | batch-put | 838.8 | 573.4 | -31.6% | ±0.0% | 1/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 137.2 | 93.4 | -31.9% | ±0.0% | 1/1 |
| v2-async | small-get | 249.8 | 230.0 | -7.9% | ±0.0% | 1/1 |
| v2-sync | small-put | 131.6 | 93.0 | -29.3% | ±0.0% | 1/1 |
| v2-async | small-put | 243.0 | 229.0 | -5.8% | ±0.0% | 1/1 |
| v2-sync | batch-get | 586.2 | 474.4 | -19.1% | ±0.0% | 1/1 |
| v2-async | batch-get | 694.6 | 665.0 | -4.3% | ±0.0% | 1/1 |
| v2-sync | batch-put | 766.8 | 422.0 | -45.0% | ±0.0% | 1/1 |
| v2-async | batch-put | 804.0 | 546.2 | -32.1% | ±0.0% | 1/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 169.4 | 169.4 | 169.4 | 0.0% |
| v2-sync | small-get | `hybridsign` | 123.0 | 123.0 | 123.0 | 0.0% |
| v2-async | small-get | `async-stock` | 266.4 | 266.4 | 266.4 | 0.0% |
| v2-async | small-get | `hybridsign` | 233.4 | 233.4 | 233.4 | 0.0% |
| v2-sync | small-put | `async-stock` | 164.8 | 164.8 | 164.8 | 0.0% |
| v2-sync | small-put | `hybridsign` | 124.0 | 124.0 | 124.0 | 0.0% |
| v2-async | small-put | `async-stock` | 258.3 | 258.3 | 258.3 | 0.0% |
| v2-async | small-put | `hybridsign` | 236.0 | 236.0 | 236.0 | 0.0% |
| v2-sync | batch-get | `async-stock` | 625.1 | 625.1 | 625.1 | 0.0% |
| v2-sync | batch-get | `hybridsign` | 509.4 | 509.4 | 509.4 | 0.0% |
| v2-async | batch-get | `async-stock` | 711.3 | 711.3 | 711.3 | 0.0% |
| v2-async | batch-get | `hybridsign` | 651.7 | 651.7 | 651.7 | 0.0% |
| v2-sync | batch-put | `async-stock` | 800.4 | 800.4 | 800.4 | 0.0% |
| v2-sync | batch-put | `hybridsign` | 452.6 | 452.6 | 452.6 | 0.0% |
| v2-async | batch-put | `async-stock` | 838.8 | 838.8 | 838.8 | 0.0% |
| v2-async | batch-put | `hybridsign` | 573.4 | 573.4 | 573.4 | 0.0% |

