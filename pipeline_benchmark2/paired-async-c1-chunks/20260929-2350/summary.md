# Paired A/B summary — `20260929-2350`

Arms (baseline first): `async-stock`, `async-bridge`

- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case
- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 1 reps per case

**12 of 24 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-get, v2-async/small-get, v2-async/small-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 232.3 | 202.5 | -12.8% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 357.9 | 334.5 | -6.5% | ±0.0% | 1/1 |
| v2-sync | small-get | 142.0 | 87.9 | -38.1% | ±0.0% | 1/1 |
| v2-async | small-put | 227.2 | 205.0 | -9.8% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 350.7 | 338.2 | -3.6% | ±0.0% | 1/1 |
| v2-sync | small-put | 125.4 | 78.0 | -37.8% | ±0.0% | 1/1 |
| v2-async | batch-get | 653.0 | 613.5 | -6.0% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 821.4 | 770.2 | -6.2% | ±0.0% | 1/1 |
| v2-sync | batch-get | 578.0 | 457.4 | -20.9% | ±0.0% | 1/1 |
| v2-async | batch-put | 835.2 | 503.9 | -39.7% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 1,002.6 | 714.1 | -28.8% | ±0.0% | 1/1 |
| v2-sync | batch-put | 742.9 | 403.1 | -45.7% | ±0.0% | 1/1 |

## mean latency µs

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 250.2 | 211.3 | -15.5% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 310.2 | 252.6 | -18.6% | ±0.0% | 1/1 |
| v2-sync | small-get | 207.2 | 123.8 | -40.3% | ±0.0% | 1/1 |
| v2-async | small-put | 250.2 | 226.9 | -9.3% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 302.1 | 266.6 | -11.8% | ±0.0% | 1/1 |
| v2-sync | small-put | 161.5 | 109.4 | -32.3% | ±0.0% | 1/1 |
| v2-async | batch-get | 696.0 | 613.0 | -11.9% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 805.4 | 690.3 | -14.3% | ±0.0% | 1/1 |
| v2-sync | batch-get | 625.1 | 499.6 | -20.1% | ±0.0% | 1/1 |
| v2-async | batch-put | 902.4 | 544.1 | -39.7% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 982.7 | 656.9 | -33.2% | ±0.0% | 1/1 |
| v2-sync | batch-put | 781.0 | 436.8 | -44.1% | ±0.0% | 1/1 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 259.4 | 219.2 | -15.5% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 318.1 | 259.2 | -18.5% | ±0.0% | 1/1 |
| v2-sync | small-get | 207.5 | 123.9 | -40.3% | ±0.0% | 1/1 |
| v2-async | small-put | 260.8 | 236.0 | -9.5% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 311.6 | 272.2 | -12.6% | ±0.0% | 1/1 |
| v2-sync | small-put | 161.6 | 109.6 | -32.2% | ±0.0% | 1/1 |
| v2-async | batch-get | 705.8 | 622.2 | -11.8% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 821.3 | 700.7 | -14.7% | ±0.0% | 1/1 |
| v2-sync | batch-get | 625.3 | 499.9 | -20.1% | ±0.0% | 1/1 |
| v2-async | batch-put | 912.5 | 552.6 | -39.4% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 993.8 | 664.0 | -33.2% | ±0.0% | 1/1 |
| v2-sync | batch-put | 781.2 | 437.2 | -44.0% | ±0.0% | 1/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 246.8 | 215.2 | -12.8% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 378.4 | 345.6 | -8.7% | ±0.0% | 1/1 |
| v2-sync | small-get | 147.6 | 93.2 | -36.9% | ±0.0% | 1/1 |
| v2-async | small-put | 244.8 | 223.4 | -8.7% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 369.2 | 351.6 | -4.8% | ±0.0% | 1/1 |
| v2-sync | small-put | 128.0 | 81.4 | -36.4% | ±0.0% | 1/1 |
| v2-async | batch-get | 693.4 | 642.2 | -7.4% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 882.0 | 810.0 | -8.2% | ±0.0% | 1/1 |
| v2-sync | batch-get | 585.2 | 464.2 | -20.7% | ±0.0% | 1/1 |
| v2-async | batch-put | 877.8 | 525.0 | -40.2% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 1,053.0 | 737.0 | -30.0% | ±0.0% | 1/1 |
| v2-sync | batch-put | 749.0 | 407.2 | -45.6% | ±0.0% | 1/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-stock` | 259.4 | 259.4 | 259.4 | 0.0% |
| v2-async | small-get | `async-bridge` | 219.2 | 219.2 | 219.2 | 0.0% |
| v2-async-netty | small-get | `async-stock` | 318.1 | 318.1 | 318.1 | 0.0% |
| v2-async-netty | small-get | `async-bridge` | 259.2 | 259.2 | 259.2 | 0.0% |
| v2-sync | small-get | `async-stock` | 207.5 | 207.5 | 207.5 | 0.0% |
| v2-sync | small-get | `async-bridge` | 123.9 | 123.9 | 123.9 | 0.0% |
| v2-async | small-put | `async-stock` | 260.8 | 260.8 | 260.8 | 0.0% |
| v2-async | small-put | `async-bridge` | 236.0 | 236.0 | 236.0 | 0.0% |
| v2-async-netty | small-put | `async-stock` | 311.6 | 311.6 | 311.6 | 0.0% |
| v2-async-netty | small-put | `async-bridge` | 272.2 | 272.2 | 272.2 | 0.0% |
| v2-sync | small-put | `async-stock` | 161.6 | 161.6 | 161.6 | 0.0% |
| v2-sync | small-put | `async-bridge` | 109.6 | 109.6 | 109.6 | 0.0% |
| v2-async | batch-get | `async-stock` | 705.8 | 705.8 | 705.8 | 0.0% |
| v2-async | batch-get | `async-bridge` | 622.2 | 622.2 | 622.2 | 0.0% |
| v2-async-netty | batch-get | `async-stock` | 821.3 | 821.3 | 821.3 | 0.0% |
| v2-async-netty | batch-get | `async-bridge` | 700.7 | 700.7 | 700.7 | 0.0% |
| v2-sync | batch-get | `async-stock` | 625.3 | 625.3 | 625.3 | 0.0% |
| v2-sync | batch-get | `async-bridge` | 499.9 | 499.9 | 499.9 | 0.0% |
| v2-async | batch-put | `async-stock` | 912.5 | 912.5 | 912.5 | 0.0% |
| v2-async | batch-put | `async-bridge` | 552.6 | 552.6 | 552.6 | 0.0% |
| v2-async-netty | batch-put | `async-stock` | 993.8 | 993.8 | 993.8 | 0.0% |
| v2-async-netty | batch-put | `async-bridge` | 664.0 | 664.0 | 664.0 | 0.0% |
| v2-sync | batch-put | `async-stock` | 781.2 | 781.2 | 781.2 | 0.0% |
| v2-sync | batch-put | `async-bridge` | 437.2 | 437.2 | 437.2 | 0.0% |

