# Paired A/B summary — `20260930-1828`

Arms (baseline first): `errbridge-traits3`, `async-stock`

- `errbridge-traits3`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**6 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/small-get, v2-async/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 109.0 | 130.4 | +19.6% | ±0.0% | 0/1 |
| v2-async | small-get | 235.3 | 233.4 | -0.8% | ±0.0% | 1/1 |
| v2-sync | small-put | 101.5 | 127.8 | +25.9% | ±0.0% | 0/1 |
| v2-async | small-put | 226.1 | 231.5 | +2.4% | ±0.0% | 0/1 |
| v2-sync | batch-get | 489.5 | 551.0 | +12.6% | ±0.0% | 0/1 |
| v2-async | batch-get | 652.9 | 667.0 | +2.2% | ±0.0% | 0/1 |
| v2-sync | batch-put | 471.0 | 813.2 | +72.7% | ±0.0% | 0/1 |
| v2-async | batch-put | 537.2 | 830.5 | +54.6% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 144.8 | 167.4 | +15.6% | ±0.0% | 0/1 |
| v2-async | small-get | 249.8 | 254.9 | +2.0% | ±0.0% | 0/1 |
| v2-sync | small-put | 140.7 | 165.5 | +17.6% | ±0.0% | 0/1 |
| v2-async | small-put | 246.5 | 257.4 | +4.4% | ±0.0% | 0/1 |
| v2-sync | batch-get | 554.0 | 597.2 | +7.8% | ±0.0% | 0/1 |
| v2-async | batch-get | 652.3 | 735.1 | +12.7% | ±0.0% | 0/1 |
| v2-sync | batch-put | 515.9 | 857.0 | +66.1% | ±0.0% | 0/1 |
| v2-async | batch-put | 574.4 | 894.9 | +55.8% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 145.0 | 167.5 | +15.5% | ±0.0% | 0/1 |
| v2-async | small-get | 257.7 | 264.5 | +2.6% | ±0.0% | 0/1 |
| v2-sync | small-put | 140.8 | 165.7 | +17.7% | ±0.0% | 0/1 |
| v2-async | small-put | 254.6 | 267.8 | +5.2% | ±0.0% | 0/1 |
| v2-sync | batch-get | 554.6 | 597.3 | +7.7% | ±0.0% | 0/1 |
| v2-async | batch-get | 661.9 | 744.4 | +12.5% | ±0.0% | 0/1 |
| v2-sync | batch-put | 516.3 | 857.2 | +66.0% | ±0.0% | 0/1 |
| v2-async | batch-put | 582.7 | 906.1 | +55.5% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | errbridge-traits3 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 113.8 | 133.6 | +17.4% | ±0.0% | 0/1 |
| v2-async | small-get | 246.0 | 250.0 | +1.6% | ±0.0% | 0/1 |
| v2-sync | small-put | 107.2 | 132.6 | +23.7% | ±0.0% | 0/1 |
| v2-async | small-put | 242.2 | 248.8 | +2.7% | ±0.0% | 0/1 |
| v2-sync | batch-get | 509.2 | 558.4 | +9.7% | ±0.0% | 0/1 |
| v2-async | batch-get | 681.6 | 727.2 | +6.7% | ±0.0% | 0/1 |
| v2-sync | batch-put | 480.6 | 819.6 | +70.5% | ±0.0% | 0/1 |
| v2-async | batch-put | 557.4 | 869.4 | +56.0% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `errbridge-traits3` | 145.0 | 145.0 | 145.0 | 0.0% |
| v2-sync | small-get | `async-stock` | 167.5 | 167.5 | 167.5 | 0.0% |
| v2-async | small-get | `errbridge-traits3` | 257.7 | 257.7 | 257.7 | 0.0% |
| v2-async | small-get | `async-stock` | 264.5 | 264.5 | 264.5 | 0.0% |
| v2-sync | small-put | `errbridge-traits3` | 140.8 | 140.8 | 140.8 | 0.0% |
| v2-sync | small-put | `async-stock` | 165.7 | 165.7 | 165.7 | 0.0% |
| v2-async | small-put | `errbridge-traits3` | 254.6 | 254.6 | 254.6 | 0.0% |
| v2-async | small-put | `async-stock` | 267.8 | 267.8 | 267.8 | 0.0% |
| v2-sync | batch-get | `errbridge-traits3` | 554.6 | 554.6 | 554.6 | 0.0% |
| v2-sync | batch-get | `async-stock` | 597.3 | 597.3 | 597.3 | 0.0% |
| v2-async | batch-get | `errbridge-traits3` | 661.9 | 661.9 | 661.9 | 0.0% |
| v2-async | batch-get | `async-stock` | 744.4 | 744.4 | 744.4 | 0.0% |
| v2-sync | batch-put | `errbridge-traits3` | 516.3 | 516.3 | 516.3 | 0.0% |
| v2-sync | batch-put | `async-stock` | 857.2 | 857.2 | 857.2 | 0.0% |
| v2-async | batch-put | `errbridge-traits3` | 582.7 | 582.7 | 582.7 | 0.0% |
| v2-async | batch-put | `async-stock` | 906.1 | 906.1 | 906.1 | 0.0% |

