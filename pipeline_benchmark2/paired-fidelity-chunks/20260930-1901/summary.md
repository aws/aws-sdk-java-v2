# Paired A/B summary — `20260930-1901`

Arms (baseline first): `errbridge-traits3`, `async-stock`

- `errbridge-traits3`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**5 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/batch-put, v2-async/small-get, v2-async/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 110.9 | 132.9 | +19.8% | ±0.0% | 0/1 |
| v2-async | small-get | 238.1 | 238.8 | +0.3% | ±0.0% | 0/1 |
| v2-sync | small-put | 100.2 | 126.5 | +26.2% | ±0.0% | 0/1 |
| v2-async | small-put | 231.9 | 230.4 | -0.6% | ±0.0% | 1/1 |
| v2-sync | batch-get | 498.6 | 577.4 | +15.8% | ±0.0% | 0/1 |
| v2-async | batch-get | 634.5 | 644.6 | +1.6% | ±0.0% | 0/1 |
| v2-sync | batch-put | 441.6 | 805.3 | +82.4% | ±0.0% | 0/1 |
| v2-async | batch-put | 548.9 | 845.9 | +54.1% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 147.6 | 170.6 | +15.6% | ±0.0% | 0/1 |
| v2-async | small-get | 248.6 | 261.8 | +5.3% | ±0.0% | 0/1 |
| v2-sync | small-put | 136.8 | 162.1 | +18.5% | ±0.0% | 0/1 |
| v2-async | small-put | 236.7 | 248.8 | +5.1% | ±0.0% | 0/1 |
| v2-sync | batch-get | 542.7 | 626.1 | +15.4% | ±0.0% | 0/1 |
| v2-async | batch-get | 634.4 | 693.5 | +9.3% | ±0.0% | 0/1 |
| v2-sync | batch-put | 477.1 | 845.0 | +77.1% | ±0.0% | 0/1 |
| v2-async | batch-put | 582.5 | 901.5 | +54.8% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 147.7 | 170.8 | +15.6% | ±0.0% | 0/1 |
| v2-async | small-get | 256.8 | 271.4 | +5.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 137.0 | 162.3 | +18.5% | ±0.0% | 0/1 |
| v2-async | small-put | 244.9 | 259.0 | +5.8% | ±0.0% | 0/1 |
| v2-sync | batch-get | 543.1 | 626.2 | +15.3% | ±0.0% | 0/1 |
| v2-async | batch-get | 643.6 | 703.1 | +9.2% | ±0.0% | 0/1 |
| v2-sync | batch-put | 477.4 | 845.2 | +77.0% | ±0.0% | 0/1 |
| v2-async | batch-put | 591.6 | 911.7 | +54.1% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 114.4 | 136.4 | +19.2% | ±0.0% | 0/1 |
| v2-async | small-get | 255.6 | 255.2 | -0.2% | ±0.0% | 1/1 |
| v2-sync | small-put | 104.4 | 129.0 | +23.6% | ±0.0% | 0/1 |
| v2-async | small-put | 242.4 | 256.8 | +5.9% | ±0.0% | 0/1 |
| v2-sync | batch-get | 506.6 | 585.6 | +15.6% | ±0.0% | 0/1 |
| v2-async | batch-get | 661.2 | 685.8 | +3.7% | ±0.0% | 0/1 |
| v2-sync | batch-put | 445.4 | 811.2 | +82.1% | ±0.0% | 0/1 |
| v2-async | batch-put | 567.2 | 883.8 | +55.8% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `errbridge-traits3` | 147.7 | 147.7 | 147.7 | 0.0% |
| v2-sync | small-get | `async-stock` | 170.8 | 170.8 | 170.8 | 0.0% |
| v2-async | small-get | `errbridge-traits3` | 256.8 | 256.8 | 256.8 | 0.0% |
| v2-async | small-get | `async-stock` | 271.4 | 271.4 | 271.4 | 0.0% |
| v2-sync | small-put | `errbridge-traits3` | 137.0 | 137.0 | 137.0 | 0.0% |
| v2-sync | small-put | `async-stock` | 162.3 | 162.3 | 162.3 | 0.0% |
| v2-async | small-put | `errbridge-traits3` | 244.9 | 244.9 | 244.9 | 0.0% |
| v2-async | small-put | `async-stock` | 259.0 | 259.0 | 259.0 | 0.0% |
| v2-sync | batch-get | `errbridge-traits3` | 543.1 | 543.1 | 543.1 | 0.0% |
| v2-sync | batch-get | `async-stock` | 626.2 | 626.2 | 626.2 | 0.0% |
| v2-async | batch-get | `errbridge-traits3` | 643.6 | 643.6 | 643.6 | 0.0% |
| v2-async | batch-get | `async-stock` | 703.1 | 703.1 | 703.1 | 0.0% |
| v2-sync | batch-put | `errbridge-traits3` | 477.4 | 477.4 | 477.4 | 0.0% |
| v2-sync | batch-put | `async-stock` | 845.2 | 845.2 | 845.2 | 0.0% |
| v2-async | batch-put | `errbridge-traits3` | 591.6 | 591.6 | 591.6 | 0.0% |
| v2-async | batch-put | `async-stock` | 911.7 | 911.7 | 911.7 | 0.0% |

