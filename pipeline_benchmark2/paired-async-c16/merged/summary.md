# Paired A/B summary — `merged`

Arms (baseline first): `async-stock`, `async-bridge`

- `async-stock`: sdk_commit `published-2.46.10`, 4 reps per case
- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 4 reps per case

**95 of 96 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/batch-put, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-get, v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/batch-get, v2-sync/batch-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 163.2 | 135.6 | -16.9% | ±3.1% | 4/4 |
| v2-async-netty | small-get | 215.1 | 168.2 | -21.8% | ±2.2% | 4/4 |
| v2-sync | small-get | 218.8 | 99.9 | -53.9% | ±5.5% | 4/4 |
| v2-async | small-put | 160.4 | 126.9 | -20.9% | ±2.9% | 4/4 |
| v2-async-netty | small-put | 220.4 | 165.0 | -25.1% | ±1.1% | 4/4 |
| v2-sync | small-put | 189.7 | 96.0 | -49.0% | ±5.9% | 4/4 |
| v2-async | batch-get | 633.0 | 556.4 | -12.1% | ±2.2% | 4/4 |
| v2-async-netty | batch-get | 694.0 | 728.1 | +4.9% | ±1.5% | 0/4 |
| v2-sync | batch-get | 628.0 | 494.1 | -21.3% | ±2.2% | 4/4 |
| v2-async | batch-put | 828.9 | 471.3 | -43.0% | ±2.8% | 4/4 |
| v2-async-netty | batch-put | 943.9 | 622.7 | -34.0% | ±0.7% | 4/4 |
| v2-sync | batch-put | 821.3 | 456.3 | -44.3% | ±4.2% | 4/4 |

## mean latency µs

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 614.1 | 1,175.2 | +97.0% | ±37.7% | 0/4 |
| v2-async-netty | small-get | 1,379.2 | 1,535.4 | +11.8% | ±8.7% | 0/4 |
| v2-sync | small-get | 1,903.0 | 849.0 | -54.9% | ±5.6% | 4/4 |
| v2-async | small-put | 400.6 | 1,113.0 | +178.5% | ±13.3% | 0/4 |
| v2-async-netty | small-put | 1,146.2 | 1,548.0 | +35.4% | ±8.2% | 0/4 |
| v2-sync | small-put | 1,690.2 | 832.0 | -50.4% | ±4.9% | 4/4 |
| v2-async | batch-get | 5,459.1 | 4,775.3 | -12.5% | ±2.6% | 4/4 |
| v2-async-netty | batch-get | 6,783.8 | 6,289.6 | -7.3% | ±0.9% | 4/4 |
| v2-sync | batch-get | 5,590.7 | 4,106.4 | -26.5% | ±2.9% | 4/4 |
| v2-async | batch-put | 940.5 | 4,026.9 | +328.5% | ±15.4% | 0/4 |
| v2-async-netty | batch-put | 1,122.3 | 5,378.2 | +379.4% | ±10.5% | 0/4 |
| v2-sync | batch-put | 7,146.1 | 3,767.5 | -47.1% | ±4.0% | 4/4 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 96.9 | 74.2 | -23.3% | ±3.8% | 4/4 |
| v2-async-netty | small-get | 136.5 | 96.5 | -29.1% | ±4.3% | 4/4 |
| v2-sync | small-get | 123.3 | 55.2 | -54.7% | ±5.3% | 4/4 |
| v2-async | small-put | 98.7 | 70.2 | -28.6% | ±5.1% | 4/4 |
| v2-async-netty | small-put | 131.8 | 97.4 | -26.1% | ±1.0% | 4/4 |
| v2-sync | small-put | 107.9 | 53.7 | -49.9% | ±5.2% | 4/4 |
| v2-async | batch-get | 342.6 | 298.9 | -12.7% | ±2.6% | 4/4 |
| v2-async-netty | batch-get | 425.2 | 393.8 | -7.4% | ±0.9% | 4/4 |
| v2-sync | batch-get | 356.2 | 265.3 | -25.5% | ±2.3% | 4/4 |
| v2-async | batch-put | 731.5 | 252.8 | -65.4% | ±1.9% | 4/4 |
| v2-async-netty | batch-put | 723.6 | 337.0 | -53.4% | ±0.9% | 4/4 |
| v2-sync | batch-put | 454.2 | 242.1 | -46.5% | ±3.9% | 4/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 185.5 | 143.9 | -22.4% | ±2.5% | 4/4 |
| v2-async-netty | small-get | 258.9 | 179.0 | -30.7% | ±4.5% | 4/4 |
| v2-sync | small-get | 245.3 | 107.5 | -55.7% | ±5.3% | 4/4 |
| v2-async | small-put | 186.8 | 136.4 | -26.8% | ±5.1% | 4/4 |
| v2-async-netty | small-put | 244.6 | 178.3 | -27.1% | ±1.4% | 4/4 |
| v2-sync | small-put | 214.7 | 102.8 | -51.8% | ±5.8% | 4/4 |
| v2-async | batch-get | 659.9 | 575.8 | -12.7% | ±2.6% | 4/4 |
| v2-async-netty | batch-get | 733.4 | 751.6 | +2.5% | ±1.3% | 0/4 |
| v2-sync | batch-get | 707.5 | 515.4 | -27.1% | ±2.1% | 4/4 |
| v2-async | batch-put | 859.8 | 487.6 | -43.2% | ±2.8% | 4/4 |
| v2-async-netty | batch-put | 986.1 | 638.5 | -35.2% | ±1.1% | 4/4 |
| v2-sync | batch-put | 904.2 | 473.6 | -47.4% | ±4.1% | 4/4 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-stock` | 94.4 | 96.9 | 103.1 | 9.2% |
| v2-async | small-get | `async-bridge` | 73.2 | 74.2 | 75.3 | 2.9% |
| v2-async-netty | small-get | `async-stock` | 129.8 | 136.5 | 146.3 | 12.7% |
| v2-async-netty | small-get | `async-bridge` | 94.5 | 96.5 | 99.1 | 4.9% |
| v2-sync | small-get | `async-stock` | 105.2 | 123.3 | 135.7 | 29.0% |
| v2-sync | small-get | `async-bridge` | 54.0 | 55.2 | 56.9 | 5.4% |
| v2-async | small-put | `async-stock` | 93.2 | 98.7 | 107.3 | 15.1% |
| v2-async | small-put | `async-bridge` | 67.6 | 70.2 | 72.1 | 6.7% |
| v2-async-netty | small-put | `async-stock` | 126.8 | 131.8 | 134.7 | 6.2% |
| v2-async-netty | small-put | `async-bridge` | 95.3 | 97.4 | 98.3 | 3.1% |
| v2-sync | small-put | `async-stock` | 98.9 | 107.9 | 117.3 | 18.6% |
| v2-sync | small-put | `async-bridge` | 52.4 | 53.7 | 54.6 | 4.2% |
| v2-async | batch-get | `async-stock` | 337.7 | 342.6 | 349.9 | 3.6% |
| v2-async | batch-get | `async-bridge` | 294.8 | 298.9 | 306.0 | 3.8% |
| v2-async-netty | batch-get | `async-stock` | 416.5 | 425.2 | 435.4 | 4.5% |
| v2-async-netty | batch-get | `async-bridge` | 386.8 | 393.8 | 401.5 | 3.8% |
| v2-sync | batch-get | `async-stock` | 345.8 | 356.2 | 365.3 | 5.6% |
| v2-sync | batch-get | `async-bridge` | 257.0 | 265.3 | 271.0 | 5.4% |
| v2-async | batch-put | `async-stock` | 705.7 | 731.5 | 783.6 | 11.0% |
| v2-async | batch-put | `async-bridge` | 250.4 | 252.8 | 254.0 | 1.4% |
| v2-async-netty | batch-put | `async-stock` | 701.2 | 723.6 | 738.8 | 5.4% |
| v2-async-netty | batch-put | `async-bridge` | 334.0 | 337.0 | 339.9 | 1.8% |
| v2-sync | batch-put | `async-stock` | 426.8 | 454.2 | 495.2 | 16.0% |
| v2-sync | batch-put | `async-bridge` | 238.1 | 242.1 | 246.4 | 3.5% |

