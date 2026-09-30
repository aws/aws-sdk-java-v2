# Paired A/B summary — `merged`

Arms (baseline first): `async-stock`, `fidelity2`

- `async-stock`: sdk_commit `published-2.46.10`, 4 reps per case
- `fidelity2`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 4 reps per case

**17 of 32 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/small-get, v2-async/small-put, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 135.0 | 105.5 | -21.5% | ±5.1% | 4/4 |
| v2-async | small-get | 235.4 | 242.0 | +2.8% | ±2.1% | 0/4 |
| v2-sync | small-put | 126.6 | 101.6 | -19.7% | ±4.9% | 4/4 |
| v2-async | small-put | 226.1 | 229.8 | +1.7% | ±2.6% | 1/4 |

## mean latency µs

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 172.1 | 140.0 | -18.3% | ±5.6% | 4/4 |
| v2-async | small-get | 256.5 | 250.7 | -2.2% | ±3.3% | 3/4 |
| v2-sync | small-put | 162.5 | 139.2 | -14.3% | ±6.7% | 4/4 |
| v2-async | small-put | 246.6 | 238.6 | -3.2% | ±3.5% | 4/4 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 172.3 | 140.1 | -18.3% | ±5.6% | 4/4 |
| v2-async | small-get | 266.2 | 259.2 | -2.6% | ±3.3% | 3/4 |
| v2-sync | small-put | 162.7 | 139.3 | -14.3% | ±6.8% | 4/4 |
| v2-async | small-put | 256.2 | 246.7 | -3.6% | ±3.5% | 4/4 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 138.8 | 108.8 | -21.3% | ±6.0% | 4/4 |
| v2-async | small-get | 256.4 | 255.6 | -0.3% | ±3.5% | 3/4 |
| v2-sync | small-put | 129.4 | 106.3 | -17.8% | ±6.7% | 4/4 |
| v2-async | small-put | 244.3 | 241.9 | -0.9% | ±2.7% | 2/4 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 160.0 | 172.3 | 190.9 | 19.3% |
| v2-sync | small-get | `fidelity2` | 137.8 | 140.1 | 141.8 | 2.9% |
| v2-async | small-get | `async-stock` | 260.8 | 266.2 | 272.6 | 4.5% |
| v2-async | small-get | `fidelity2` | 251.6 | 259.2 | 264.3 | 5.0% |
| v2-sync | small-put | `async-stock` | 156.6 | 162.7 | 166.3 | 6.2% |
| v2-sync | small-put | `fidelity2` | 131.8 | 139.3 | 155.7 | 18.1% |
| v2-async | small-put | `async-stock` | 250.4 | 256.2 | 266.8 | 6.5% |
| v2-async | small-put | `fidelity2` | 243.6 | 246.7 | 249.6 | 2.5% |

