# Paired A/B summary — `20260930-0111`

Arms (baseline first): `async-bridge`, `async-stock`

- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**23 of 24 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/batch-put, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-get, v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/batch-get, v2-sync/batch-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 136.9 | 162.8 | +18.9% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 167.4 | 213.9 | +27.8% | ±0.0% | 0/1 |
| v2-sync | small-get | 105.4 | 210.3 | +99.5% | ±0.0% | 0/1 |
| v2-async | small-put | 127.4 | 166.7 | +30.8% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 164.2 | 222.3 | +35.4% | ±0.0% | 0/1 |
| v2-sync | small-put | 91.5 | 206.0 | +125.1% | ±0.0% | 0/1 |
| v2-async | batch-get | 551.9 | 632.5 | +14.6% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 744.2 | 697.2 | -6.3% | ±0.0% | 1/1 |
| v2-sync | batch-get | 482.7 | 623.4 | +29.1% | ±0.0% | 0/1 |
| v2-async | batch-put | 470.8 | 802.7 | +70.5% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 619.1 | 937.2 | +51.4% | ±0.0% | 0/1 |
| v2-sync | batch-put | 466.4 | 776.8 | +66.6% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 1,187.7 | 531.4 | -55.3% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 1,533.3 | 1,265.8 | -17.4% | ±0.0% | 1/1 |
| v2-sync | small-get | 882.0 | 1,799.8 | +104.1% | ±0.0% | 0/1 |
| v2-async | small-put | 1,109.2 | 379.3 | -65.8% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 1,558.1 | 1,225.3 | -21.4% | ±0.0% | 1/1 |
| v2-sync | small-put | 818.3 | 1,844.7 | +125.4% | ±0.0% | 0/1 |
| v2-async | batch-get | 4,754.4 | 5,426.2 | +14.1% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 6,413.8 | 6,855.8 | +6.9% | ±0.0% | 0/1 |
| v2-sync | batch-get | 3,932.3 | 5,543.5 | +41.0% | ±0.0% | 0/1 |
| v2-async | batch-put | 4,047.4 | 923.9 | -77.2% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 5,348.8 | 1,141.4 | -78.7% | ±0.0% | 1/1 |
| v2-sync | batch-put | 3,795.3 | 6,696.2 | +76.4% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 75.0 | 95.7 | +27.6% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 96.4 | 129.8 | +34.6% | ±0.0% | 0/1 |
| v2-sync | small-get | 56.9 | 118.5 | +108.3% | ±0.0% | 0/1 |
| v2-async | small-put | 69.9 | 107.3 | +53.5% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 98.3 | 132.4 | +34.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 52.4 | 117.3 | +123.9% | ±0.0% | 0/1 |
| v2-async | batch-get | 297.7 | 340.4 | +14.3% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 401.5 | 429.6 | +7.0% | ±0.0% | 0/1 |
| v2-sync | batch-get | 257.0 | 355.1 | +38.2% | ±0.0% | 0/1 |
| v2-async | batch-put | 254.0 | 705.7 | +177.8% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 335.1 | 701.2 | +109.3% | ±0.0% | 0/1 |
| v2-sync | batch-put | 244.8 | 426.8 | +74.3% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 146.0 | 185.0 | +26.7% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 177.8 | 242.2 | +36.2% | ±0.0% | 0/1 |
| v2-sync | small-get | 112.4 | 235.8 | +109.8% | ±0.0% | 0/1 |
| v2-async | small-put | 135.8 | 204.6 | +50.7% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 179.0 | 242.6 | +35.5% | ±0.0% | 0/1 |
| v2-sync | small-put | 96.8 | 232.8 | +140.5% | ±0.0% | 0/1 |
| v2-async | batch-get | 571.8 | 657.6 | +15.0% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 765.2 | 739.0 | -3.4% | ±0.0% | 1/1 |
| v2-sync | batch-get | 501.2 | 704.6 | +40.6% | ±0.0% | 0/1 |
| v2-async | batch-put | 489.8 | 827.8 | +69.0% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 635.0 | 980.0 | +54.3% | ±0.0% | 0/1 |
| v2-sync | batch-put | 481.4 | 847.6 | +76.1% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-bridge` | 75.0 | 75.0 | 75.0 | 0.0% |
| v2-async | small-get | `async-stock` | 95.7 | 95.7 | 95.7 | 0.0% |
| v2-async-netty | small-get | `async-bridge` | 96.4 | 96.4 | 96.4 | 0.0% |
| v2-async-netty | small-get | `async-stock` | 129.8 | 129.8 | 129.8 | 0.0% |
| v2-sync | small-get | `async-bridge` | 56.9 | 56.9 | 56.9 | 0.0% |
| v2-sync | small-get | `async-stock` | 118.5 | 118.5 | 118.5 | 0.0% |
| v2-async | small-put | `async-bridge` | 69.9 | 69.9 | 69.9 | 0.0% |
| v2-async | small-put | `async-stock` | 107.3 | 107.3 | 107.3 | 0.0% |
| v2-async-netty | small-put | `async-bridge` | 98.3 | 98.3 | 98.3 | 0.0% |
| v2-async-netty | small-put | `async-stock` | 132.4 | 132.4 | 132.4 | 0.0% |
| v2-sync | small-put | `async-bridge` | 52.4 | 52.4 | 52.4 | 0.0% |
| v2-sync | small-put | `async-stock` | 117.3 | 117.3 | 117.3 | 0.0% |
| v2-async | batch-get | `async-bridge` | 297.7 | 297.7 | 297.7 | 0.0% |
| v2-async | batch-get | `async-stock` | 340.4 | 340.4 | 340.4 | 0.0% |
| v2-async-netty | batch-get | `async-bridge` | 401.5 | 401.5 | 401.5 | 0.0% |
| v2-async-netty | batch-get | `async-stock` | 429.6 | 429.6 | 429.6 | 0.0% |
| v2-sync | batch-get | `async-bridge` | 257.0 | 257.0 | 257.0 | 0.0% |
| v2-sync | batch-get | `async-stock` | 355.1 | 355.1 | 355.1 | 0.0% |
| v2-async | batch-put | `async-bridge` | 254.0 | 254.0 | 254.0 | 0.0% |
| v2-async | batch-put | `async-stock` | 705.7 | 705.7 | 705.7 | 0.0% |
| v2-async-netty | batch-put | `async-bridge` | 335.1 | 335.1 | 335.1 | 0.0% |
| v2-async-netty | batch-put | `async-stock` | 701.2 | 701.2 | 701.2 | 0.0% |
| v2-sync | batch-put | `async-bridge` | 244.8 | 244.8 | 244.8 | 0.0% |
| v2-sync | batch-put | `async-stock` | 426.8 | 426.8 | 426.8 | 0.0% |

