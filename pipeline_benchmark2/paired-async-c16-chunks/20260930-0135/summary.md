# Paired A/B summary — `20260930-0135`

Arms (baseline first): `async-stock`, `async-bridge`

- `async-stock`: sdk_commit `published-2.46.10`, 1 reps per case
- `async-bridge`: sdk_commit `c1d88972e18-plus-async-wip`, 1 reps per case

**24 of 24 runs were not steady-state** (JIT still compiling inside the measured window): v2-async-netty/batch-get, v2-async-netty/batch-put, v2-async-netty/small-get, v2-async-netty/small-put, v2-async/batch-get, v2-async/batch-put, v2-async/small-get, v2-async/small-put, v2-sync/batch-get, v2-sync/batch-put, v2-sync/small-get, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 167.1 | 134.5 | -19.5% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 222.0 | 166.7 | -24.9% | ±0.0% | 1/1 |
| v2-sync | small-get | 236.4 | 97.1 | -58.9% | ±0.0% | 1/1 |
| v2-async | small-put | 156.1 | 123.6 | -20.8% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 218.4 | 165.7 | -24.1% | ±0.0% | 1/1 |
| v2-sync | small-put | 174.8 | 97.4 | -44.3% | ±0.0% | 1/1 |
| v2-async | batch-get | 626.9 | 553.5 | -11.7% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 707.9 | 734.2 | +3.7% | ±0.0% | 0/1 |
| v2-sync | batch-get | 629.5 | 502.6 | -20.2% | ±0.0% | 1/1 |
| v2-async | batch-put | 882.1 | 467.3 | -47.0% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 938.0 | 614.3 | -34.5% | ±0.0% | 1/1 |
| v2-sync | batch-put | 817.5 | 450.9 | -44.8% | ±0.0% | 1/1 |

## mean latency µs

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 764.6 | 1,162.2 | +52.0% | ±0.0% | 0/1 |
| v2-async-netty | small-get | 1,369.7 | 1,502.2 | +9.7% | ±0.0% | 0/1 |
| v2-sync | small-get | 2,107.3 | 851.8 | -59.6% | ±0.0% | 1/1 |
| v2-async | small-put | 402.8 | 1,129.9 | +180.5% | ±0.0% | 0/1 |
| v2-async-netty | small-put | 1,187.0 | 1,563.1 | +31.7% | ±0.0% | 0/1 |
| v2-sync | small-put | 1,567.5 | 822.5 | -47.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 5,454.7 | 4,748.7 | -12.9% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 6,943.7 | 6,355.4 | -8.5% | ±0.0% | 1/1 |
| v2-sync | batch-get | 5,623.5 | 4,248.5 | -24.5% | ±0.0% | 1/1 |
| v2-async | batch-put | 983.0 | 3,988.9 | +305.8% | ±0.0% | 0/1 |
| v2-async-netty | batch-put | 1,108.6 | 5,330.0 | +380.8% | ±0.0% | 0/1 |
| v2-sync | batch-put | 7,104.3 | 3,706.3 | -47.8% | ±0.0% | 1/1 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 103.1 | 73.4 | -28.8% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 146.3 | 94.5 | -35.4% | ±0.0% | 1/1 |
| v2-sync | small-get | 135.7 | 54.7 | -59.7% | ±0.0% | 1/1 |
| v2-async | small-put | 93.2 | 71.4 | -23.4% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 134.7 | 98.2 | -27.1% | ±0.0% | 1/1 |
| v2-sync | small-put | 100.3 | 54.0 | -46.2% | ±0.0% | 1/1 |
| v2-async | batch-get | 342.4 | 297.3 | -13.2% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 435.4 | 397.9 | -8.6% | ±0.0% | 1/1 |
| v2-sync | batch-get | 358.7 | 271.0 | -24.4% | ±0.0% | 1/1 |
| v2-async | batch-put | 783.6 | 250.4 | -68.0% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 727.2 | 334.0 | -54.1% | ±0.0% | 1/1 |
| v2-sync | batch-put | 451.5 | 238.1 | -47.3% | ±0.0% | 1/1 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | async-bridge mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-async | small-get | 192.0 | 142.4 | -25.8% | ±0.0% | 1/1 |
| v2-async-netty | small-get | 280.0 | 176.2 | -37.1% | ±0.0% | 1/1 |
| v2-sync | small-get | 269.4 | 105.8 | -60.7% | ±0.0% | 1/1 |
| v2-async | small-put | 177.8 | 138.8 | -21.9% | ±0.0% | 1/1 |
| v2-async-netty | small-put | 249.4 | 180.0 | -27.8% | ±0.0% | 1/1 |
| v2-sync | small-put | 200.0 | 105.0 | -47.5% | ±0.0% | 1/1 |
| v2-async | batch-get | 655.2 | 573.0 | -12.5% | ±0.0% | 1/1 |
| v2-async-netty | batch-get | 751.4 | 756.0 | +0.6% | ±0.0% | 0/1 |
| v2-sync | batch-get | 712.8 | 525.4 | -26.3% | ±0.0% | 1/1 |
| v2-async | batch-put | 913.4 | 483.0 | -47.1% | ±0.0% | 1/1 |
| v2-async-netty | batch-put | 988.4 | 630.4 | -36.2% | ±0.0% | 1/1 |
| v2-sync | batch-put | 899.4 | 467.4 | -48.0% | ±0.0% | 1/1 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-async | small-get | `async-stock` | 103.1 | 103.1 | 103.1 | 0.0% |
| v2-async | small-get | `async-bridge` | 73.4 | 73.4 | 73.4 | 0.0% |
| v2-async-netty | small-get | `async-stock` | 146.3 | 146.3 | 146.3 | 0.0% |
| v2-async-netty | small-get | `async-bridge` | 94.5 | 94.5 | 94.5 | 0.0% |
| v2-sync | small-get | `async-stock` | 135.7 | 135.7 | 135.7 | 0.0% |
| v2-sync | small-get | `async-bridge` | 54.7 | 54.7 | 54.7 | 0.0% |
| v2-async | small-put | `async-stock` | 93.2 | 93.2 | 93.2 | 0.0% |
| v2-async | small-put | `async-bridge` | 71.4 | 71.4 | 71.4 | 0.0% |
| v2-async-netty | small-put | `async-stock` | 134.7 | 134.7 | 134.7 | 0.0% |
| v2-async-netty | small-put | `async-bridge` | 98.2 | 98.2 | 98.2 | 0.0% |
| v2-sync | small-put | `async-stock` | 100.3 | 100.3 | 100.3 | 0.0% |
| v2-sync | small-put | `async-bridge` | 54.0 | 54.0 | 54.0 | 0.0% |
| v2-async | batch-get | `async-stock` | 342.4 | 342.4 | 342.4 | 0.0% |
| v2-async | batch-get | `async-bridge` | 297.3 | 297.3 | 297.3 | 0.0% |
| v2-async-netty | batch-get | `async-stock` | 435.4 | 435.4 | 435.4 | 0.0% |
| v2-async-netty | batch-get | `async-bridge` | 397.9 | 397.9 | 397.9 | 0.0% |
| v2-sync | batch-get | `async-stock` | 358.7 | 358.7 | 358.7 | 0.0% |
| v2-sync | batch-get | `async-bridge` | 271.0 | 271.0 | 271.0 | 0.0% |
| v2-async | batch-put | `async-stock` | 783.6 | 783.6 | 783.6 | 0.0% |
| v2-async | batch-put | `async-bridge` | 250.4 | 250.4 | 250.4 | 0.0% |
| v2-async-netty | batch-put | `async-stock` | 727.2 | 727.2 | 727.2 | 0.0% |
| v2-async-netty | batch-put | `async-bridge` | 334.0 | 334.0 | 334.0 | 0.0% |
| v2-sync | batch-put | `async-stock` | 451.5 | 451.5 | 451.5 | 0.0% |
| v2-sync | batch-put | `async-bridge` | 238.1 | 238.1 | 238.1 | 0.0% |

