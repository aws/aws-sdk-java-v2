# Paired A/B summary — `20260930-0017`

Arms (baseline first): `async-bridge`, `async-stock`

- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**11 of 24 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/small-get, v2-async/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 203.9 | 238.2 | +16.8% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 337.7 | 364.6 | +8.0% | ±0.0% | 0/1 |
| v2-sync | small-get | 86.4 | 130.8 | +51.4% | ±0.0% | 0/1 |
| v2-async | small-put | 197.9 | 229.0 | +15.7% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 338.9 | 357.9 | +5.6% | ±0.0% | 0/1 |
| v2-sync | small-put | 78.6 | 121.4 | +54.5% | ±0.0% | 0/1 |
| v2-async | batch-get | 612.2 | 660.3 | +7.9% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 836.1 | 844.4 | +1.0% | ±0.0% | 0/1 |
| v2-sync | batch-get | 470.0 | 569.5 | +21.2% | ±0.0% | 0/1 |
| v2-async | batch-put | 497.4 | 875.1 | +75.9% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 688.8 | 963.8 | +39.9% | ±0.0% | 0/1 |
| v2-sync | batch-put | 413.7 | 804.6 | +94.5% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 207.5 | 259.0 | +24.8% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 259.0 | 319.5 | +23.4% | ±0.0% | 0/1 |
| v2-sync | small-get | 118.5 | 167.0 | +40.9% | ±0.0% | 0/1 |
| v2-async | small-put | 206.4 | 248.8 | +20.5% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 265.3 | 309.0 | +16.5% | ±0.0% | 0/1 |
| v2-sync | small-put | 111.0 | 156.5 | +41.0% | ±0.0% | 0/1 |
| v2-async | batch-get | 634.5 | 711.2 | +12.1% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 760.8 | 815.8 | +7.2% | ±0.0% | 0/1 |
| v2-sync | batch-get | 514.3 | 617.5 | +20.1% | ±0.0% | 0/1 |
| v2-async | batch-put | 532.8 | 939.0 | +76.2% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 626.4 | 933.1 | +49.0% | ±0.0% | 0/1 |
| v2-sync | batch-put | 450.2 | 843.8 | +87.4% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 215.4 | 269.5 | +25.1% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 265.3 | 327.7 | +23.5% | ±0.0% | 0/1 |
| v2-sync | small-get | 118.6 | 167.1 | +40.9% | ±0.0% | 0/1 |
| v2-async | small-put | 215.0 | 257.8 | +19.9% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 270.6 | 318.2 | +17.6% | ±0.0% | 0/1 |
| v2-sync | small-put | 111.2 | 156.6 | +40.8% | ±0.0% | 0/1 |
| v2-async | batch-get | 644.1 | 721.9 | +12.1% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 774.8 | 830.4 | +7.2% | ±0.0% | 0/1 |
| v2-sync | batch-get | 514.4 | 617.6 | +20.1% | ±0.0% | 0/1 |
| v2-async | batch-put | 541.6 | 949.5 | +75.3% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 634.1 | 943.2 | +48.7% | ±0.0% | 0/1 |
| v2-sync | batch-put | 450.8 | 844.0 | +87.2% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 214.8 | 253.6 | +18.1% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 350.8 | 387.6 | +10.5% | ±0.0% | 0/1 |
| v2-sync | small-get | 89.8 | 133.8 | +49.0% | ±0.0% | 0/1 |
| v2-async | small-put | 211.0 | 252.2 | +19.5% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 348.0 | 387.4 | +11.3% | ±0.0% | 0/1 |
| v2-sync | small-put | 82.2 | 124.2 | +51.1% | ±0.0% | 0/1 |
| v2-async | batch-get | 663.8 | 705.6 | +6.3% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 879.0 | 889.6 | +1.2% | ±0.0% | 0/1 |
| v2-sync | batch-get | 479.0 | 578.4 | +20.8% | ±0.0% | 0/1 |
| v2-async | batch-put | 516.0 | 913.4 | +77.0% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 709.0 | 1,002.6 | +41.4% | ±0.0% | 0/1 |
| v2-sync | batch-put | 420.2 | 809.6 | +92.7% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-bridge` | 215.4 | 215.4 | 215.4 | 0.0% |
| v2-async | small-get | `async-stock` | 269.5 | 269.5 | 269.5 | 0.0% |
| v2-async-netty | small-get | `async-bridge` | 265.3 | 265.3 | 265.3 | 0.0% |
| v2-async-netty | small-get | `async-stock` | 327.7 | 327.7 | 327.7 | 0.0% |
| v2-sync | small-get | `async-bridge` | 118.6 | 118.6 | 118.6 | 0.0% |
| v2-sync | small-get | `async-stock` | 167.1 | 167.1 | 167.1 | 0.0% |
| v2-async | small-put | `async-bridge` | 215.0 | 215.0 | 215.0 | 0.0% |
| v2-async | small-put | `async-stock` | 257.8 | 257.8 | 257.8 | 0.0% |
| v2-async-netty | small-put | `async-bridge` | 270.6 | 270.6 | 270.6 | 0.0% |
| v2-async-netty | small-put | `async-stock` | 318.2 | 318.2 | 318.2 | 0.0% |
| v2-sync | small-put | `async-bridge` | 111.2 | 111.2 | 111.2 | 0.0% |
| v2-sync | small-put | `async-stock` | 156.6 | 156.6 | 156.6 | 0.0% |
| v2-async | batch-get | `async-bridge` | 644.1 | 644.1 | 644.1 | 0.0% |
| v2-async | batch-get | `async-stock` | 721.9 | 721.9 | 721.9 | 0.0% |
| v2-async-netty | batch-get | `async-bridge` | 774.8 | 774.8 | 774.8 | 0.0% |
| v2-async-netty | batch-get | `async-stock` | 830.4 | 830.4 | 830.4 | 0.0% |
| v2-sync | batch-get | `async-bridge` | 514.4 | 514.4 | 514.4 | 0.0% |
| v2-sync | batch-get | `async-stock` | 617.6 | 617.6 | 617.6 | 0.0% |
| v2-async | batch-put | `async-bridge` | 541.6 | 541.6 | 541.6 | 0.0% |
| v2-async | batch-put | `async-stock` | 949.5 | 949.5 | 949.5 | 0.0% |
| v2-async-netty | batch-put | `async-bridge` | 634.1 | 634.1 | 634.1 | 0.0% |
| v2-async-netty | batch-put | `async-stock` | 943.2 | 943.2 | 943.2 | 0.0% |
| v2-sync | batch-put | `async-bridge` | 450.8 | 450.8 | 450.8 | 0.0% |
| v2-sync | batch-put | `async-stock` | 844.0 | 844.0 | 844.0 | 0.0% |

