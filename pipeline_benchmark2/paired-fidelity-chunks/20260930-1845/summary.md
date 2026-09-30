# Paired A/B summary — `20260930-1845`

Arms (baseline first): `async-stock`, `errbridge-traits3`

- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case
- `errbridge-traits3`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 1 reps per case

**7 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 130.5 | 106.1 | -18.7% | ±0.0% | 1/1 |
| v2-async | small-get | 235.2 | 241.6 | +2.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 127.3 | 103.9 | -18.4% | ±0.0% | 1/1 |
| v2-async | small-put | 228.5 | 233.0 | +2.0% | ±0.0% | 0/1 |
| v2-sync | batch-get | 551.2 | 470.6 | -14.6% | ±0.0% | 1/1 |
| v2-async | batch-get | 675.0 | 643.3 | -4.7% | ±0.0% | 1/1 |
| v2-sync | batch-put | 771.0 | 445.1 | -42.3% | ±0.0% | 1/1 |
| v2-async | batch-put | 846.8 | 536.4 | -36.7% | ±0.0% | 1/1 |

## mean latency µs

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 169.2 | 141.7 | -16.3% | ±0.0% | 1/1 |
| v2-async | small-get | 255.5 | 250.3 | -2.0% | ±0.0% | 1/1 |
| v2-sync | small-put | 163.7 | 142.4 | -13.0% | ±0.0% | 1/1 |
| v2-async | small-put | 249.2 | 240.3 | -3.6% | ±0.0% | 1/1 |
| v2-sync | batch-get | 601.0 | 513.9 | -14.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 722.4 | 638.3 | -11.6% | ±0.0% | 1/1 |
| v2-sync | batch-put | 810.8 | 481.3 | -40.6% | ±0.0% | 1/1 |
| v2-async | batch-put | 906.8 | 579.4 | -36.1% | ±0.0% | 1/1 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 169.3 | 141.9 | -16.2% | ±0.0% | 1/1 |
| v2-async | small-get | 264.3 | 258.7 | -2.1% | ±0.0% | 1/1 |
| v2-sync | small-put | 163.9 | 142.6 | -13.0% | ±0.0% | 1/1 |
| v2-async | small-put | 258.6 | 248.5 | -3.9% | ±0.0% | 1/1 |
| v2-sync | batch-get | 601.1 | 514.3 | -14.4% | ±0.0% | 1/1 |
| v2-async | batch-get | 732.1 | 647.3 | -11.6% | ±0.0% | 1/1 |
| v2-sync | batch-put | 811.1 | 481.6 | -40.6% | ±0.0% | 1/1 |
| v2-async | batch-put | 918.4 | 588.5 | -35.9% | ±0.0% | 1/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | errbridge-traits3 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 134.4 | 111.0 | -17.4% | ±0.0% | 1/1 |
| v2-async | small-get | 262.6 | 253.4 | -3.5% | ±0.0% | 1/1 |
| v2-sync | small-put | 130.0 | 108.4 | -16.6% | ±0.0% | 1/1 |
| v2-async | small-put | 246.0 | 243.2 | -1.1% | ±0.0% | 1/1 |
| v2-sync | batch-get | 559.2 | 478.8 | -14.4% | ±0.0% | 1/1 |
| v2-async | batch-get | 715.2 | 663.2 | -7.3% | ±0.0% | 1/1 |
| v2-sync | batch-put | 775.6 | 451.2 | -41.8% | ±0.0% | 1/1 |
| v2-async | batch-put | 881.8 | 567.0 | -35.7% | ±0.0% | 1/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 169.3 | 169.3 | 169.3 | 0.0% |
| v2-sync | small-get | `errbridge-traits3` | 141.9 | 141.9 | 141.9 | 0.0% |
| v2-async | small-get | `async-stock` | 264.3 | 264.3 | 264.3 | 0.0% |
| v2-async | small-get | `errbridge-traits3` | 258.7 | 258.7 | 258.7 | 0.0% |
| v2-sync | small-put | `async-stock` | 163.9 | 163.9 | 163.9 | 0.0% |
| v2-sync | small-put | `errbridge-traits3` | 142.6 | 142.6 | 142.6 | 0.0% |
| v2-async | small-put | `async-stock` | 258.6 | 258.6 | 258.6 | 0.0% |
| v2-async | small-put | `errbridge-traits3` | 248.5 | 248.5 | 248.5 | 0.0% |
| v2-sync | batch-get | `async-stock` | 601.1 | 601.1 | 601.1 | 0.0% |
| v2-sync | batch-get | `errbridge-traits3` | 514.3 | 514.3 | 514.3 | 0.0% |
| v2-async | batch-get | `async-stock` | 732.1 | 732.1 | 732.1 | 0.0% |
| v2-async | batch-get | `errbridge-traits3` | 647.3 | 647.3 | 647.3 | 0.0% |
| v2-sync | batch-put | `async-stock` | 811.1 | 811.1 | 811.1 | 0.0% |
| v2-sync | batch-put | `errbridge-traits3` | 481.6 | 481.6 | 481.6 | 0.0% |
| v2-async | batch-put | `async-stock` | 918.4 | 918.4 | 918.4 | 0.0% |
| v2-async | batch-put | `errbridge-traits3` | 588.5 | 588.5 | 588.5 | 0.0% |

