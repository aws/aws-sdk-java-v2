# Paired A/B summary — `20260930-0201`

Arms (baseline first): `async-bridge`, `async-stock`

- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**24 of 24 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/batch-put, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-get, v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/batch-get, v2-sync/batch-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 138.8 | 159.3 | +14.8% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 168.6 | 213.0 | +26.3% | ±0.0% | 0/1 |
| v2-sync | small-get | 97.4 | 188.8 | +93.8% | ±0.0% | 0/1 |
| v2-async | small-put | 125.0 | 160.7 | +28.6% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 165.4 | 218.0 | +31.8% | ±0.0% | 0/1 |
| v2-sync | small-put | 98.1 | 205.5 | +109.5% | ±0.0% | 0/1 |
| v2-async | batch-get | 548.4 | 642.0 | +17.1% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 723.6 | 684.9 | -5.3% | ±0.0% | 1/1 |
| v2-sync | batch-get | 495.5 | 649.0 | +31.0% | ±0.0% | 0/1 |
| v2-async | batch-put | 475.6 | 802.9 | +68.8% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 629.9 | 941.9 | +49.5% | ±0.0% | 0/1 |
| v2-sync | batch-put | 448.9 | 893.3 | +99.0% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 1,193.8 | 662.4 | -44.5% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 1,528.0 | 1,515.2 | -0.8% | ±0.0% | 1/1 |
| v2-sync | small-get | 834.4 | 1,644.9 | +97.1% | ±0.0% | 0/1 |
| v2-async | small-put | 1,068.9 | 381.2 | -64.3% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 1,516.4 | 1,109.5 | -26.8% | ±0.0% | 1/1 |
| v2-sync | small-put | 842.1 | 1,807.9 | +114.7% | ±0.0% | 0/1 |
| v2-async | batch-get | 4,709.8 | 5,577.3 | +18.4% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 6,211.5 | 6,689.8 | +7.7% | ±0.0% | 0/1 |
| v2-sync | batch-get | 4,115.7 | 5,793.4 | +40.8% | ±0.0% | 0/1 |
| v2-async | batch-put | 4,036.4 | 920.9 | -77.2% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 5,424.1 | 1,099.5 | -79.7% | ±0.0% | 1/1 |
| v2-sync | batch-put | 3,700.6 | 7,780.8 | +110.3% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 75.3 | 94.4 | +25.4% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 96.0 | 134.4 | +40.0% | ±0.0% | 0/1 |
| v2-sync | small-get | 54.0 | 105.2 | +94.8% | ±0.0% | 0/1 |
| v2-async | small-put | 67.6 | 97.2 | +43.8% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 95.3 | 126.8 | +33.1% | ±0.0% | 0/1 |
| v2-sync | small-put | 53.8 | 115.2 | +114.1% | ±0.0% | 0/1 |
| v2-async | batch-get | 294.8 | 349.9 | +18.7% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 388.9 | 419.2 | +7.8% | ±0.0% | 0/1 |
| v2-sync | batch-get | 266.2 | 365.3 | +37.2% | ±0.0% | 0/1 |
| v2-async | batch-put | 253.6 | 707.8 | +179.1% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 339.9 | 727.4 | +114.0% | ±0.0% | 0/1 |
| v2-sync | batch-put | 239.2 | 495.2 | +107.0% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 146.6 | 183.4 | +25.1% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 180.0 | 256.8 | +42.7% | ±0.0% | 0/1 |
| v2-sync | small-get | 104.6 | 209.4 | +100.2% | ±0.0% | 0/1 |
| v2-async | small-put | 131.2 | 181.2 | +38.1% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 176.0 | 236.6 | +34.4% | ±0.0% | 0/1 |
| v2-sync | small-put | 104.0 | 229.4 | +120.6% | ±0.0% | 0/1 |
| v2-async | batch-get | 567.0 | 673.2 | +18.7% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 743.8 | 722.0 | -2.9% | ±0.0% | 1/1 |
| v2-sync | batch-get | 516.8 | 726.8 | +40.6% | ±0.0% | 0/1 |
| v2-async | batch-put | 491.0 | 839.2 | +70.9% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 646.2 | 974.0 | +50.7% | ±0.0% | 0/1 |
| v2-sync | batch-put | 466.0 | 987.2 | +111.8% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-bridge` | 75.3 | 75.3 | 75.3 | 0.0% |
| v2-async | small-get | `async-stock` | 94.4 | 94.4 | 94.4 | 0.0% |
| v2-async-netty | small-get | `async-bridge` | 96.0 | 96.0 | 96.0 | 0.0% |
| v2-async-netty | small-get | `async-stock` | 134.4 | 134.4 | 134.4 | 0.0% |
| v2-sync | small-get | `async-bridge` | 54.0 | 54.0 | 54.0 | 0.0% |
| v2-sync | small-get | `async-stock` | 105.2 | 105.2 | 105.2 | 0.0% |
| v2-async | small-put | `async-bridge` | 67.6 | 67.6 | 67.6 | 0.0% |
| v2-async | small-put | `async-stock` | 97.2 | 97.2 | 97.2 | 0.0% |
| v2-async-netty | small-put | `async-bridge` | 95.3 | 95.3 | 95.3 | 0.0% |
| v2-async-netty | small-put | `async-stock` | 126.8 | 126.8 | 126.8 | 0.0% |
| v2-sync | small-put | `async-bridge` | 53.8 | 53.8 | 53.8 | 0.0% |
| v2-sync | small-put | `async-stock` | 115.2 | 115.2 | 115.2 | 0.0% |
| v2-async | batch-get | `async-bridge` | 294.8 | 294.8 | 294.8 | 0.0% |
| v2-async | batch-get | `async-stock` | 349.9 | 349.9 | 349.9 | 0.0% |
| v2-async-netty | batch-get | `async-bridge` | 388.9 | 388.9 | 388.9 | 0.0% |
| v2-async-netty | batch-get | `async-stock` | 419.2 | 419.2 | 419.2 | 0.0% |
| v2-sync | batch-get | `async-bridge` | 266.2 | 266.2 | 266.2 | 0.0% |
| v2-sync | batch-get | `async-stock` | 365.3 | 365.3 | 365.3 | 0.0% |
| v2-async | batch-put | `async-bridge` | 253.6 | 253.6 | 253.6 | 0.0% |
| v2-async | batch-put | `async-stock` | 707.8 | 707.8 | 707.8 | 0.0% |
| v2-async-netty | batch-put | `async-bridge` | 339.9 | 339.9 | 339.9 | 0.0% |
| v2-async-netty | batch-put | `async-stock` | 727.4 | 727.4 | 727.4 | 0.0% |
| v2-sync | batch-put | `async-bridge` | 239.2 | 239.2 | 239.2 | 0.0% |
| v2-sync | batch-put | `async-stock` | 495.2 | 495.2 | 495.2 | 0.0% |

