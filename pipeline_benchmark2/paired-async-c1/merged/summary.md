# Paired A/B summary — `merged`

Arms (baseline first): `async-stock`, `async-bridge`

- `async-stock`: sdk_commit `published-2.46.10`, 4 reps per case
- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 4 reps per case

**48 of 96 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/batch-put, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-get, v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 233.9 | 207.0 | -11.5% | ±3.3% | 4/4 |
| v2-async-netty | small-get | 362.1 | 339.6 | -6.2% | ±1.6% | 4/4 |
| v2-sync | small-get | 134.4 | 86.1 | -35.9% | ±1.7% | 4/4 |
| v2-async | small-put | 230.2 | 198.9 | -13.6% | ±2.7% | 4/4 |
| v2-async-netty | small-put | 361.1 | 340.1 | -5.8% | ±1.7% | 4/4 |
| v2-sync | small-put | 124.0 | 78.6 | -36.6% | ±1.4% | 4/4 |
| v2-async | batch-get | 656.9 | 624.7 | -4.9% | ±2.1% | 4/4 |
| v2-async-netty | batch-get | 834.6 | 797.2 | -4.5% | ±2.5% | 4/4 |
| v2-sync | batch-get | 572.5 | 465.5 | -18.7% | ±1.7% | 4/4 |
| v2-async | batch-put | 857.4 | 501.2 | -41.5% | ±2.1% | 4/4 |
| v2-async-netty | batch-put | 1,001.6 | 694.9 | -30.6% | ±2.3% | 4/4 |
| v2-sync | batch-put | 778.9 | 413.5 | -46.9% | ±1.8% | 4/4 |

## mean latency µs

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 254.2 | 213.1 | -16.1% | ±3.5% | 4/4 |
| v2-async-netty | small-get | 316.5 | 259.3 | -18.1% | ±1.8% | 4/4 |
| v2-sync | small-get | 178.1 | 119.5 | -32.5% | ±5.3% | 4/4 |
| v2-async | small-put | 252.4 | 211.8 | -16.1% | ±4.6% | 4/4 |
| v2-async-netty | small-put | 312.9 | 267.8 | -14.4% | ±2.0% | 4/4 |
| v2-sync | small-put | 159.8 | 111.1 | -30.5% | ±1.8% | 4/4 |
| v2-async | batch-get | 702.0 | 634.3 | -9.6% | ±2.4% | 4/4 |
| v2-async-netty | batch-get | 816.8 | 715.6 | -12.4% | ±3.8% | 4/4 |
| v2-sync | batch-get | 620.5 | 507.8 | -18.1% | ±1.5% | 4/4 |
| v2-async | batch-put | 921.6 | 540.8 | -41.3% | ±2.1% | 4/4 |
| v2-async-netty | batch-put | 979.2 | 637.2 | -34.9% | ±2.2% | 4/4 |
| v2-sync | batch-put | 818.2 | 449.9 | -45.0% | ±1.6% | 4/4 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 264.0 | 221.2 | -16.1% | ±3.5% | 4/4 |
| v2-async-netty | small-get | 324.6 | 265.9 | -18.1% | ±1.9% | 4/4 |
| v2-sync | small-get | 178.3 | 119.6 | -32.5% | ±5.3% | 4/4 |
| v2-async | small-put | 262.6 | 220.2 | -16.1% | ±4.6% | 4/4 |
| v2-async-netty | small-put | 322.0 | 273.6 | -15.0% | ±1.7% | 4/4 |
| v2-sync | small-put | 160.0 | 111.3 | -30.4% | ±1.7% | 4/4 |
| v2-async | batch-get | 712.1 | 643.9 | -9.6% | ±2.4% | 4/4 |
| v2-async-netty | batch-get | 830.1 | 726.5 | -12.5% | ±4.0% | 4/4 |
| v2-sync | batch-get | 620.6 | 508.0 | -18.1% | ±1.5% | 4/4 |
| v2-async | batch-put | 932.0 | 549.6 | -41.0% | ±2.0% | 4/4 |
| v2-async-netty | batch-put | 989.9 | 644.8 | -34.8% | ±2.1% | 4/4 |
| v2-sync | batch-put | 818.5 | 450.3 | -44.9% | ±1.6% | 4/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 249.8 | 218.3 | -12.5% | ±3.0% | 4/4 |
| v2-async-netty | small-get | 384.5 | 351.5 | -8.6% | ±1.5% | 4/4 |
| v2-sync | small-get | 138.0 | 90.4 | -34.4% | ±1.7% | 4/4 |
| v2-async | small-put | 250.2 | 213.1 | -14.8% | ±4.1% | 4/4 |
| v2-async-netty | small-put | 384.4 | 351.9 | -8.4% | ±2.5% | 4/4 |
| v2-sync | small-put | 126.6 | 82.3 | -35.0% | ±1.1% | 4/4 |
| v2-async | batch-get | 696.1 | 662.0 | -4.9% | ±2.3% | 4/4 |
| v2-async-netty | batch-get | 890.1 | 835.6 | -6.1% | ±3.3% | 4/4 |
| v2-sync | batch-get | 581.0 | 472.8 | -18.6% | ±1.6% | 4/4 |
| v2-async | batch-put | 900.2 | 521.8 | -42.0% | ±2.2% | 4/4 |
| v2-async-netty | batch-put | 1,051.0 | 719.8 | -31.5% | ±2.2% | 4/4 |
| v2-sync | batch-put | 784.5 | 419.8 | -46.4% | ±1.7% | 4/4 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-stock` | 255.8 | 264.0 | 271.4 | 6.1% |
| v2-async | small-get | `async-bridge` | 215.4 | 221.2 | 225.9 | 4.9% |
| v2-async-netty | small-get | `async-stock` | 318.1 | 324.6 | 327.7 | 3.0% |
| v2-async-netty | small-get | `async-bridge` | 259.2 | 265.9 | 275.4 | 6.2% |
| v2-sync | small-get | `async-stock` | 167.1 | 178.3 | 207.5 | 24.2% |
| v2-sync | small-get | `async-bridge` | 116.0 | 119.6 | 123.9 | 6.8% |
| v2-async | small-put | `async-stock` | 257.8 | 262.6 | 274.1 | 6.3% |
| v2-async | small-put | `async-bridge` | 207.4 | 220.2 | 236.0 | 13.8% |
| v2-async-netty | small-put | `async-stock` | 311.6 | 322.0 | 329.8 | 5.8% |
| v2-async-netty | small-put | `async-bridge` | 270.6 | 273.6 | 276.6 | 2.2% |
| v2-sync | small-put | `async-stock` | 156.6 | 160.0 | 162.3 | 3.6% |
| v2-sync | small-put | `async-bridge` | 108.9 | 111.3 | 115.4 | 6.0% |
| v2-async | batch-get | `async-stock` | 705.0 | 712.1 | 721.9 | 2.4% |
| v2-async | batch-get | `async-bridge` | 622.2 | 643.9 | 660.8 | 6.2% |
| v2-async-netty | batch-get | `async-stock` | 821.3 | 830.1 | 846.3 | 3.0% |
| v2-async-netty | batch-get | `async-bridge` | 700.7 | 726.5 | 774.8 | 10.6% |
| v2-sync | batch-get | `async-stock` | 617.0 | 620.6 | 625.3 | 1.3% |
| v2-sync | batch-get | `async-bridge` | 499.9 | 508.0 | 514.4 | 2.9% |
| v2-async | batch-put | `async-stock` | 912.5 | 932.0 | 953.4 | 4.5% |
| v2-async | batch-put | `async-bridge` | 541.6 | 549.6 | 556.4 | 2.7% |
| v2-async-netty | batch-put | `async-stock` | 943.2 | 989.9 | 1,026.7 | 8.9% |
| v2-async-netty | batch-put | `async-bridge` | 628.4 | 644.8 | 664.0 | 5.7% |
| v2-sync | batch-put | `async-stock` | 781.2 | 818.5 | 846.6 | 8.4% |
| v2-sync | batch-put | `async-bridge` | 437.2 | 450.3 | 457.0 | 4.5% |

