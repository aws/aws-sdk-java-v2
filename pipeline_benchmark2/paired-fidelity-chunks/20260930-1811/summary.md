# Paired A/B summary — `20260930-1811`

Arms (baseline first): `async-stock`, `errbridge-traits3`

- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case
- `errbridge-traits3`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 1 reps per case

**4 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/small-get, v2-async/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 136.9 | 106.5 | -22.2% | ±0.0% | 1/1 |
| v2-async | small-get | 235.0 | 241.4 | +2.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 129.6 | 104.2 | -19.6% | ±0.0% | 1/1 |
| v2-async | small-put | 231.4 | 227.5 | -1.7% | ±0.0% | 1/1 |
| v2-sync | batch-get | 576.3 | 494.3 | -14.2% | ±0.0% | 1/1 |
| v2-async | batch-get | 658.9 | 637.1 | -3.3% | ±0.0% | 1/1 |
| v2-sync | batch-put | 823.5 | 426.2 | -48.2% | ±0.0% | 1/1 |
| v2-async | batch-put | 841.9 | 508.4 | -39.6% | ±0.0% | 1/1 |

## mean latency µs

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 176.2 | 143.4 | -18.6% | ±0.0% | 1/1 |
| v2-async | small-get | 255.1 | 253.5 | -0.6% | ±0.0% | 1/1 |
| v2-sync | small-put | 169.4 | 142.6 | -15.8% | ±0.0% | 1/1 |
| v2-async | small-put | 256.7 | 245.8 | -4.2% | ±0.0% | 1/1 |
| v2-sync | batch-get | 633.6 | 544.4 | -14.1% | ±0.0% | 1/1 |
| v2-async | batch-get | 738.3 | 649.0 | -12.1% | ±0.0% | 1/1 |
| v2-sync | batch-put | 869.1 | 477.0 | -45.1% | ±0.0% | 1/1 |
| v2-async | batch-put | 911.8 | 559.6 | -38.6% | ±0.0% | 1/1 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 176.3 | 143.5 | -18.6% | ±0.0% | 1/1 |
| v2-async | small-get | 265.3 | 261.7 | -1.4% | ±0.0% | 1/1 |
| v2-sync | small-put | 169.6 | 142.7 | -15.9% | ±0.0% | 1/1 |
| v2-async | small-put | 268.7 | 253.8 | -5.5% | ±0.0% | 1/1 |
| v2-sync | batch-get | 633.8 | 544.8 | -14.0% | ±0.0% | 1/1 |
| v2-async | batch-get | 749.3 | 658.1 | -12.2% | ±0.0% | 1/1 |
| v2-sync | batch-put | 869.4 | 477.3 | -45.1% | ±0.0% | 1/1 |
| v2-async | batch-put | 924.0 | 568.1 | -38.5% | ±0.0% | 1/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 141.8 | 110.8 | -21.9% | ±0.0% | 1/1 |
| v2-async | small-get | 251.6 | 255.8 | +1.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 131.8 | 108.2 | -17.9% | ±0.0% | 1/1 |
| v2-async | small-put | 251.2 | 242.2 | -3.6% | ±0.0% | 1/1 |
| v2-sync | batch-get | 588.6 | 503.2 | -14.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 709.8 | 666.6 | -6.1% | ±0.0% | 1/1 |
| v2-sync | batch-put | 830.2 | 441.8 | -46.8% | ±0.0% | 1/1 |
| v2-async | batch-put | 884.4 | 534.8 | -39.5% | ±0.0% | 1/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 176.3 | 176.3 | 176.3 | 0.0% |
| v2-sync | small-get | `errbridge-traits3` | 143.5 | 143.5 | 143.5 | 0.0% |
| v2-async | small-get | `async-stock` | 265.3 | 265.3 | 265.3 | 0.0% |
| v2-async | small-get | `errbridge-traits3` | 261.7 | 261.7 | 261.7 | 0.0% |
| v2-sync | small-put | `async-stock` | 169.6 | 169.6 | 169.6 | 0.0% |
| v2-sync | small-put | `errbridge-traits3` | 142.7 | 142.7 | 142.7 | 0.0% |
| v2-async | small-put | `async-stock` | 268.7 | 268.7 | 268.7 | 0.0% |
| v2-async | small-put | `errbridge-traits3` | 253.8 | 253.8 | 253.8 | 0.0% |
| v2-sync | batch-get | `async-stock` | 633.8 | 633.8 | 633.8 | 0.0% |
| v2-sync | batch-get | `errbridge-traits3` | 544.8 | 544.8 | 544.8 | 0.0% |
| v2-async | batch-get | `async-stock` | 749.3 | 749.3 | 749.3 | 0.0% |
| v2-async | batch-get | `errbridge-traits3` | 658.1 | 658.1 | 658.1 | 0.0% |
| v2-sync | batch-put | `async-stock` | 869.4 | 869.4 | 869.4 | 0.0% |
| v2-sync | batch-put | `errbridge-traits3` | 477.3 | 477.3 | 477.3 | 0.0% |
| v2-async | batch-put | `async-stock` | 924.0 | 924.0 | 924.0 | 0.0% |
| v2-async | batch-put | `errbridge-traits3` | 568.1 | 568.1 | 568.1 | 0.0% |

