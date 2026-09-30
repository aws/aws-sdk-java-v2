# Paired A/B summary — `20260930-0044`

Arms (baseline first): `async-stock`, `async-bridge`

- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case
- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 1 reps per case

**24 of 24 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/batch-put, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-get, v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/batch-get, v2-sync/batch-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 163.6 | 132.2 | -19.2% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 211.5 | 169.9 | -19.7% | ±0.0% | 1/1 |
| v2-sync | small-get | 239.6 | 99.7 | -58.4% | ±0.0% | 1/1 |
| v2-async | small-put | 158.2 | 131.5 | -16.9% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 223.0 | 164.8 | -26.1% | ±0.0% | 1/1 |
| v2-sync | small-put | 172.6 | 97.1 | -43.7% | ±0.0% | 1/1 |
| v2-async | batch-get | 630.5 | 571.9 | -9.3% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 685.8 | 710.3 | +3.6% | ±0.0% | 0/1 |
| v2-sync | batch-get | 610.2 | 495.5 | -18.8% | ±0.0% | 1/1 |
| v2-async | batch-put | 828.0 | 471.6 | -43.0% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 958.6 | 627.4 | -34.6% | ±0.0% | 1/1 |
| v2-sync | batch-put | 797.7 | 458.9 | -42.5% | ±0.0% | 1/1 |

## mean latency µs

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 497.9 | 1,157.1 | +132.4% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 1,366.1 | 1,578.0 | +15.5% | ±0.0% | 0/1 |
| v2-sync | small-get | 2,060.2 | 827.7 | -59.8% | ±0.0% | 1/1 |
| v2-async | small-put | 439.2 | 1,144.0 | +160.5% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 1,062.9 | 1,554.4 | +46.2% | ±0.0% | 0/1 |
| v2-sync | small-put | 1,540.8 | 845.1 | -45.2% | ±0.0% | 1/1 |
| v2-async | batch-get | 5,378.4 | 4,888.4 | -9.1% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 6,645.9 | 6,177.7 | -7.0% | ±0.0% | 1/1 |
| v2-sync | batch-get | 5,402.3 | 4,129.2 | -23.6% | ±0.0% | 1/1 |
| v2-async | batch-put | 934.0 | 4,034.8 | +332.0% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 1,139.6 | 5,410.1 | +374.7% | ±0.0% | 0/1 |
| v2-sync | batch-put | 7,003.0 | 3,867.7 | -44.8% | ±0.0% | 1/1 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 94.5 | 73.2 | -22.5% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 135.4 | 99.1 | -26.8% | ±0.0% | 1/1 |
| v2-sync | small-get | 134.0 | 55.4 | -58.7% | ±0.0% | 1/1 |
| v2-async | small-put | 97.2 | 72.1 | -25.8% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 133.1 | 97.7 | -26.6% | ±0.0% | 1/1 |
| v2-sync | small-put | 98.9 | 54.6 | -44.8% | ±0.0% | 1/1 |
| v2-async | batch-get | 337.7 | 306.0 | -9.4% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 416.5 | 386.8 | -7.1% | ±0.0% | 1/1 |
| v2-sync | batch-get | 345.8 | 266.9 | -22.8% | ±0.0% | 1/1 |
| v2-async | batch-put | 728.7 | 253.1 | -65.3% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 738.8 | 339.1 | -54.1% | ±0.0% | 1/1 |
| v2-sync | batch-put | 443.5 | 246.4 | -44.4% | ±0.0% | 1/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 181.6 | 140.6 | -22.6% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 256.8 | 182.0 | -29.1% | ±0.0% | 1/1 |
| v2-sync | small-get | 266.6 | 107.4 | -59.7% | ±0.0% | 1/1 |
| v2-async | small-put | 183.6 | 139.8 | -23.9% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 249.8 | 178.4 | -28.6% | ±0.0% | 1/1 |
| v2-sync | small-put | 196.4 | 105.2 | -46.4% | ±0.0% | 1/1 |
| v2-async | batch-get | 653.6 | 591.2 | -9.5% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 721.0 | 741.4 | +2.8% | ±0.0% | 0/1 |
| v2-sync | batch-get | 686.0 | 518.0 | -24.5% | ±0.0% | 1/1 |
| v2-async | batch-put | 858.8 | 486.8 | -43.3% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 1,002.0 | 642.2 | -35.9% | ±0.0% | 1/1 |
| v2-sync | batch-put | 882.8 | 479.8 | -45.7% | ±0.0% | 1/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-stock` | 94.5 | 94.5 | 94.5 | 0.0% |
| v2-async | small-get | `async-bridge` | 73.2 | 73.2 | 73.2 | 0.0% |
| v2-async-netty | small-get | `async-stock` | 135.4 | 135.4 | 135.4 | 0.0% |
| v2-async-netty | small-get | `async-bridge` | 99.1 | 99.1 | 99.1 | 0.0% |
| v2-sync | small-get | `async-stock` | 134.0 | 134.0 | 134.0 | 0.0% |
| v2-sync | small-get | `async-bridge` | 55.4 | 55.4 | 55.4 | 0.0% |
| v2-async | small-put | `async-stock` | 97.2 | 97.2 | 97.2 | 0.0% |
| v2-async | small-put | `async-bridge` | 72.1 | 72.1 | 72.1 | 0.0% |
| v2-async-netty | small-put | `async-stock` | 133.1 | 133.1 | 133.1 | 0.0% |
| v2-async-netty | small-put | `async-bridge` | 97.7 | 97.7 | 97.7 | 0.0% |
| v2-sync | small-put | `async-stock` | 98.9 | 98.9 | 98.9 | 0.0% |
| v2-sync | small-put | `async-bridge` | 54.6 | 54.6 | 54.6 | 0.0% |
| v2-async | batch-get | `async-stock` | 337.7 | 337.7 | 337.7 | 0.0% |
| v2-async | batch-get | `async-bridge` | 306.0 | 306.0 | 306.0 | 0.0% |
| v2-async-netty | batch-get | `async-stock` | 416.5 | 416.5 | 416.5 | 0.0% |
| v2-async-netty | batch-get | `async-bridge` | 386.8 | 386.8 | 386.8 | 0.0% |
| v2-sync | batch-get | `async-stock` | 345.8 | 345.8 | 345.8 | 0.0% |
| v2-sync | batch-get | `async-bridge` | 266.9 | 266.9 | 266.9 | 0.0% |
| v2-async | batch-put | `async-stock` | 728.7 | 728.7 | 728.7 | 0.0% |
| v2-async | batch-put | `async-bridge` | 253.1 | 253.1 | 253.1 | 0.0% |
| v2-async-netty | batch-put | `async-stock` | 738.8 | 738.8 | 738.8 | 0.0% |
| v2-async-netty | batch-put | `async-bridge` | 339.1 | 339.1 | 339.1 | 0.0% |
| v2-sync | batch-put | `async-stock` | 443.5 | 443.5 | 443.5 | 0.0% |
| v2-sync | batch-put | `async-bridge` | 246.4 | 246.4 | 246.4 | 0.0% |

