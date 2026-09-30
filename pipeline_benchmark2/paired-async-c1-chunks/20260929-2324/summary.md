# Paired A/B summary — `20260929-2324`

Arms (baseline first): `async-bridge`, `async-stock`

- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 1 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case

**13 of 24 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/batch-put, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/small-get. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 211.7 | 226.9 | +7.2% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 337.9 | 363.9 | +7.7% | ±0.0% | 0/1 |
| v2-sync | small-get | 84.4 | 132.3 | +56.8% | ±0.0% | 0/1 |
| v2-async | small-put | 192.0 | 228.9 | +19.2% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 341.2 | 367.5 | +7.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 81.1 | 125.8 | +55.1% | ±0.0% | 0/1 |
| v2-async | batch-get | 638.6 | 656.7 | +2.8% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 794.5 | 847.7 | +6.7% | ±0.0% | 0/1 |
| v2-sync | batch-get | 469.5 | 567.9 | +21.0% | ±0.0% | 0/1 |
| v2-async | batch-put | 495.2 | 875.0 | +76.7% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 700.4 | 1,029.7 | +47.0% | ±0.0% | 0/1 |
| v2-sync | batch-put | 417.1 | 805.7 | +93.2% | ±0.0% | 0/1 |

## mean latency µs

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 217.9 | 246.2 | +13.0% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 257.2 | 319.3 | +24.1% | ±0.0% | 0/1 |
| v2-sync | small-get | 115.9 | 168.6 | +45.5% | ±0.0% | 0/1 |
| v2-async | small-put | 199.7 | 248.0 | +24.2% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 268.1 | 320.9 | +19.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 115.3 | 162.1 | +40.6% | ±0.0% | 0/1 |
| v2-async | batch-get | 650.5 | 695.3 | +6.9% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 708.3 | 835.6 | +18.0% | ±0.0% | 0/1 |
| v2-sync | batch-get | 510.9 | 616.8 | +20.7% | ±0.0% | 0/1 |
| v2-async | batch-put | 538.9 | 942.9 | +75.0% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 645.0 | 1,016.0 | +57.5% | ±0.0% | 0/1 |
| v2-sync | batch-put | 456.8 | 846.4 | +85.3% | ±0.0% | 0/1 |

## wall µs/op (1/throughput)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 225.9 | 255.8 | +13.2% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 263.7 | 327.5 | +24.2% | ±0.0% | 0/1 |
| v2-sync | small-get | 116.0 | 168.8 | +45.5% | ±0.0% | 0/1 |
| v2-async | small-put | 207.4 | 257.8 | +24.3% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 274.9 | 329.8 | +20.0% | ±0.0% | 0/1 |
| v2-sync | small-put | 115.4 | 162.3 | +40.6% | ±0.0% | 0/1 |
| v2-async | batch-get | 660.8 | 705.0 | +6.7% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 716.8 | 846.3 | +18.1% | ±0.0% | 0/1 |
| v2-sync | batch-get | 511.2 | 617.0 | +20.7% | ±0.0% | 0/1 |
| v2-async | batch-put | 547.7 | 953.4 | +74.1% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 652.6 | 1,026.7 | +57.3% | ±0.0% | 0/1 |
| v2-sync | batch-put | 457.0 | 846.6 | +85.3% | ±0.0% | 0/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-bridge mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 223.2 | 243.6 | +9.1% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 350.4 | 388.2 | +10.8% | ±0.0% | 0/1 |
| v2-sync | small-get | 89.2 | 135.2 | +51.6% | ±0.0% | 0/1 |
| v2-async | small-put | 201.0 | 245.4 | +22.1% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 353.0 | 389.2 | +10.3% | ±0.0% | 0/1 |
| v2-sync | small-put | 84.0 | 128.2 | +52.6% | ±0.0% | 0/1 |
| v2-async | batch-get | 674.2 | 687.8 | +2.0% | ±0.0% | 0/1 |
| v2-async-netty | batch-get | 830.6 | 904.4 | +8.9% | ±0.0% | 0/1 |
| v2-sync | batch-get | 475.8 | 576.8 | +21.2% | ±0.0% | 0/1 |
| v2-async | batch-put | 518.2 | 929.6 | +79.4% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 728.2 | 1,100.0 | +51.1% | ±0.0% | 0/1 |
| v2-sync | batch-put | 425.8 | 812.2 | +90.7% | ±0.0% | 0/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-bridge` | 225.9 | 225.9 | 225.9 | 0.0% |
| v2-async | small-get | `async-stock` | 255.8 | 255.8 | 255.8 | 0.0% |
| v2-async-netty | small-get | `async-bridge` | 263.7 | 263.7 | 263.7 | 0.0% |
| v2-async-netty | small-get | `async-stock` | 327.5 | 327.5 | 327.5 | 0.0% |
| v2-sync | small-get | `async-bridge` | 116.0 | 116.0 | 116.0 | 0.0% |
| v2-sync | small-get | `async-stock` | 168.8 | 168.8 | 168.8 | 0.0% |
| v2-async | small-put | `async-bridge` | 207.4 | 207.4 | 207.4 | 0.0% |
| v2-async | small-put | `async-stock` | 257.8 | 257.8 | 257.8 | 0.0% |
| v2-async-netty | small-put | `async-bridge` | 274.9 | 274.9 | 274.9 | 0.0% |
| v2-async-netty | small-put | `async-stock` | 329.8 | 329.8 | 329.8 | 0.0% |
| v2-sync | small-put | `async-bridge` | 115.4 | 115.4 | 115.4 | 0.0% |
| v2-sync | small-put | `async-stock` | 162.3 | 162.3 | 162.3 | 0.0% |
| v2-async | batch-get | `async-bridge` | 660.8 | 660.8 | 660.8 | 0.0% |
| v2-async | batch-get | `async-stock` | 705.0 | 705.0 | 705.0 | 0.0% |
| v2-async-netty | batch-get | `async-bridge` | 716.8 | 716.8 | 716.8 | 0.0% |
| v2-async-netty | batch-get | `async-stock` | 846.3 | 846.3 | 846.3 | 0.0% |
| v2-sync | batch-get | `async-bridge` | 511.2 | 511.2 | 511.2 | 0.0% |
| v2-sync | batch-get | `async-stock` | 617.0 | 617.0 | 617.0 | 0.0% |
| v2-async | batch-put | `async-bridge` | 547.7 | 547.7 | 547.7 | 0.0% |
| v2-async | batch-put | `async-stock` | 953.4 | 953.4 | 953.4 | 0.0% |
| v2-async-netty | batch-put | `async-bridge` | 652.6 | 652.6 | 652.6 | 0.0% |
| v2-async-netty | batch-put | `async-stock` | 1,026.7 | 1,026.7 | 1,026.7 | 0.0% |
| v2-sync | batch-put | `async-bridge` | 457.0 | 457.0 | 457.0 | 0.0% |
| v2-sync | batch-put | `async-stock` | 846.6 | 846.6 | 846.6 | 0.0% |

