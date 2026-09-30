# Paired A/B summary — `merged`

Arms (baseline first): `async-stock`, `errbridge-traits3`

- `async-stock`: sdk_commit `published-2.46.10`, 4 reps per case
- `errbridge-traits3`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 4 reps per case

**22 of 64 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 132.7 | 108.1 | -18.5% | ±2.7% | 4/4 |
| v2-async | small-get | 235.6 | 239.1 | +1.5% | ±1.5% | 1/4 |
| v2-sync | small-put | 127.8 | 102.5 | -19.8% | ±1.1% | 4/4 |
| v2-async | small-put | 230.4 | 229.6 | -0.3% | ±2.0% | 2/4 |
| v2-sync | batch-get | 564.0 | 488.2 | -13.4% | ±1.6% | 4/4 |
| v2-async | batch-get | 661.4 | 642.0 | -2.9% | ±1.4% | 4/4 |
| v2-sync | batch-put | 803.2 | 446.0 | -44.4% | ±2.9% | 4/4 |
| v2-async | batch-put | 841.3 | 532.7 | -36.7% | ±2.1% | 4/4 |

## mean latency µs

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 170.8 | 144.4 | -15.5% | ±2.5% | 4/4 |
| v2-async | small-get | 256.8 | 250.6 | -2.4% | ±1.9% | 4/4 |
| v2-sync | small-put | 165.2 | 140.6 | -14.9% | ±1.3% | 4/4 |
| v2-async | small-put | 253.0 | 242.3 | -4.2% | ±0.5% | 4/4 |
| v2-sync | batch-get | 614.5 | 538.8 | -12.3% | ±3.4% | 4/4 |
| v2-async | batch-get | 722.3 | 643.5 | -10.9% | ±1.6% | 4/4 |
| v2-sync | batch-put | 845.5 | 487.8 | -42.3% | ±2.5% | 4/4 |
| v2-async | batch-put | 903.8 | 574.0 | -36.5% | ±1.5% | 4/4 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 171.0 | 144.5 | -15.4% | ±2.5% | 4/4 |
| v2-async | small-get | 266.4 | 258.7 | -2.9% | ±1.8% | 4/4 |
| v2-sync | small-put | 165.4 | 140.8 | -14.9% | ±1.3% | 4/4 |
| v2-async | small-put | 263.5 | 250.4 | -5.0% | ±0.8% | 4/4 |
| v2-sync | batch-get | 614.6 | 539.2 | -12.2% | ±3.4% | 4/4 |
| v2-async | batch-get | 732.2 | 652.7 | -10.8% | ±1.6% | 4/4 |
| v2-sync | batch-put | 845.7 | 488.1 | -42.3% | ±2.5% | 4/4 |
| v2-async | batch-put | 915.0 | 582.7 | -36.3% | ±1.5% | 4/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 136.6 | 112.5 | -17.6% | ±3.1% | 4/4 |
| v2-async | small-get | 254.8 | 252.7 | -0.8% | ±2.2% | 2/4 |
| v2-sync | small-put | 130.8 | 107.1 | -18.2% | ±1.2% | 4/4 |
| v2-async | small-put | 250.7 | 242.5 | -3.2% | ±1.9% | 4/4 |
| v2-sync | batch-get | 573.0 | 499.4 | -12.8% | ±2.7% | 4/4 |
| v2-async | batch-get | 709.5 | 668.2 | -5.8% | ±1.6% | 4/4 |
| v2-sync | batch-put | 809.2 | 454.8 | -43.8% | ±2.6% | 4/4 |
| v2-async | batch-put | 879.8 | 556.6 | -36.7% | ±1.9% | 4/4 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 167.5 | 171.0 | 176.3 | 5.3% |
| v2-sync | small-get | `errbridge-traits3` | 141.9 | 144.5 | 147.7 | 4.1% |
| v2-async | small-get | `async-stock` | 264.3 | 266.4 | 271.4 | 2.7% |
| v2-async | small-get | `errbridge-traits3` | 256.8 | 258.7 | 261.7 | 1.9% |
| v2-sync | small-put | `async-stock` | 162.3 | 165.4 | 169.6 | 4.5% |
| v2-sync | small-put | `errbridge-traits3` | 137.0 | 140.8 | 142.7 | 4.2% |
| v2-async | small-put | `async-stock` | 258.6 | 263.5 | 268.7 | 3.9% |
| v2-async | small-put | `errbridge-traits3` | 244.9 | 250.4 | 254.6 | 4.0% |
| v2-sync | batch-get | `async-stock` | 597.3 | 614.6 | 633.8 | 6.1% |
| v2-sync | batch-get | `errbridge-traits3` | 514.3 | 539.2 | 554.6 | 7.8% |
| v2-async | batch-get | `async-stock` | 703.1 | 732.2 | 749.3 | 6.6% |
| v2-async | batch-get | `errbridge-traits3` | 643.6 | 652.7 | 661.9 | 2.8% |
| v2-sync | batch-put | `async-stock` | 811.1 | 845.7 | 869.4 | 7.2% |
| v2-sync | batch-put | `errbridge-traits3` | 477.3 | 488.1 | 516.3 | 8.2% |
| v2-async | batch-put | `async-stock` | 906.1 | 915.0 | 924.0 | 2.0% |
| v2-async | batch-put | `errbridge-traits3` | 568.1 | 582.7 | 591.6 | 4.1% |

