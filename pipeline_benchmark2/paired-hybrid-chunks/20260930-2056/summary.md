# Paired A/B summary — `20260930-2056`

Arms (baseline first): `async-stock`, `hybridsign`

- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case
- `hybridsign`: sdk_commit `3e6ea7bb6c3-plus-hybrid-signing`, 1 reps per case

**6 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/batch-get, v2-async/small-get, v2-async/small-put, v2-sync/small-get. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

**WARNING: arms were built from different harness commits (3e6ea7bb6c3, c1d88972e18). The comparison is not clean — rebuild both jars from the same harness before drawing conclusions.**

## application cpu µs/op

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 138.8 | 90.1 | -35.1% | ±0.0% | 1/1 |
| v2-async | small-get | 215.4 | 217.7 | +1.1% | ±0.0% | 0/1 |
| v2-sync | small-put | 128.0 | 87.5 | -31.6% | ±0.0% | 1/1 |
| v2-async | small-put | 230.0 | 208.0 | -9.6% | ±0.0% | 1/1 |
| v2-sync | batch-get | 555.2 | 492.3 | -11.3% | ±0.0% | 1/1 |
| v2-async | batch-get | 658.6 | 634.0 | -3.7% | ±0.0% | 1/1 |
| v2-sync | batch-put | 775.7 | 419.3 | -45.9% | ±0.0% | 1/1 |
| v2-async | batch-put | 833.6 | 523.6 | -37.2% | ±0.0% | 1/1 |

## mean latency µs

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 179.1 | 123.2 | -31.2% | ±0.0% | 1/1 |
| v2-async | small-get | 239.5 | 227.1 | -5.2% | ±0.0% | 1/1 |
| v2-sync | small-put | 165.0 | 125.4 | -24.0% | ±0.0% | 1/1 |
| v2-async | small-put | 251.7 | 214.4 | -14.8% | ±0.0% | 1/1 |
| v2-sync | batch-get | 603.5 | 540.2 | -10.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 717.9 | 644.0 | -10.3% | ±0.0% | 1/1 |
| v2-sync | batch-put | 815.7 | 455.1 | -44.2% | ±0.0% | 1/1 |
| v2-async | batch-put | 883.1 | 563.2 | -36.2% | ±0.0% | 1/1 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 179.3 | 123.4 | -31.2% | ±0.0% | 1/1 |
| v2-async | small-get | 248.2 | 235.6 | -5.1% | ±0.0% | 1/1 |
| v2-sync | small-put | 165.1 | 125.6 | -23.9% | ±0.0% | 1/1 |
| v2-async | small-put | 261.7 | 222.1 | -15.1% | ±0.0% | 1/1 |
| v2-sync | batch-get | 603.8 | 540.5 | -10.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 727.5 | 652.9 | -10.3% | ±0.0% | 1/1 |
| v2-sync | batch-put | 815.9 | 455.5 | -44.2% | ±0.0% | 1/1 |
| v2-async | batch-put | 893.5 | 572.8 | -35.9% | ±0.0% | 1/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | hybridsign mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 142.4 | 93.8 | -34.1% | ±0.0% | 1/1 |
| v2-async | small-get | 237.8 | 227.2 | -4.5% | ±0.0% | 1/1 |
| v2-sync | small-put | 131.0 | 89.6 | -31.6% | ±0.0% | 1/1 |
| v2-async | small-put | 251.0 | 221.4 | -11.8% | ±0.0% | 1/1 |
| v2-sync | batch-get | 565.2 | 501.2 | -11.3% | ±0.0% | 1/1 |
| v2-async | batch-get | 713.8 | 668.2 | -6.4% | ±0.0% | 1/1 |
| v2-sync | batch-put | 780.6 | 423.0 | -45.8% | ±0.0% | 1/1 |
| v2-async | batch-put | 857.4 | 543.8 | -36.6% | ±0.0% | 1/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 179.3 | 179.3 | 179.3 | 0.0% |
| v2-sync | small-get | `hybridsign` | 123.4 | 123.4 | 123.4 | 0.0% |
| v2-async | small-get | `async-stock` | 248.2 | 248.2 | 248.2 | 0.0% |
| v2-async | small-get | `hybridsign` | 235.6 | 235.6 | 235.6 | 0.0% |
| v2-sync | small-put | `async-stock` | 165.1 | 165.1 | 165.1 | 0.0% |
| v2-sync | small-put | `hybridsign` | 125.6 | 125.6 | 125.6 | 0.0% |
| v2-async | small-put | `async-stock` | 261.7 | 261.7 | 261.7 | 0.0% |
| v2-async | small-put | `hybridsign` | 222.1 | 222.1 | 222.1 | 0.0% |
| v2-sync | batch-get | `async-stock` | 603.8 | 603.8 | 603.8 | 0.0% |
| v2-sync | batch-get | `hybridsign` | 540.5 | 540.5 | 540.5 | 0.0% |
| v2-async | batch-get | `async-stock` | 727.5 | 727.5 | 727.5 | 0.0% |
| v2-async | batch-get | `hybridsign` | 652.9 | 652.9 | 652.9 | 0.0% |
| v2-sync | batch-put | `async-stock` | 815.9 | 815.9 | 815.9 | 0.0% |
| v2-sync | batch-put | `hybridsign` | 455.5 | 455.5 | 455.5 | 0.0% |
| v2-async | batch-put | `async-stock` | 893.5 | 893.5 | 893.5 | 0.0% |
| v2-async | batch-put | `hybridsign` | 572.8 | 572.8 | 572.8 | 0.0% |

