# Paired A/B summary — `20260930-1931`

Arms (baseline first): `async-stock`, `fidelity2`

- `async-stock`: sdk_commit `published-2.46.10`, 2 reps per case
- `fidelity2`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 2 reps per case

**8 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/small-get, v2-async/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 129.0 | 105.7 | -18.0% | ±3.3% | 2/2 |
| v2-async | small-get | 235.0 | 245.4 | +4.4% | ±1.7% | 0/2 |
| v2-sync | small-put | 128.4 | 97.8 | -23.8% | ±1.0% | 2/2 |
| v2-async | small-put | 224.8 | 232.2 | +3.3% | ±2.6% | 0/2 |

## mean latency µs

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 163.8 | 140.2 | -14.3% | ±2.8% | 2/2 |
| v2-async | small-get | 252.4 | 253.6 | +0.5% | ±1.8% | 1/2 |
| v2-sync | small-put | 164.7 | 132.8 | -19.3% | ±2.0% | 2/2 |
| v2-async | small-put | 243.4 | 241.5 | -0.8% | ±1.0% | 2/2 |

## wall µs/op (1/throughput)

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 164.1 | 140.4 | -14.3% | ±2.9% | 2/2 |
| v2-async | small-get | 262.3 | 262.5 | +0.1% | ±1.8% | 1/2 |
| v2-sync | small-put | 164.9 | 132.9 | -19.3% | ±2.0% | 2/2 |
| v2-async | small-put | 252.7 | 249.4 | -1.3% | ±1.2% | 2/2 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | async-stock mean | fidelity2 mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 131.8 | 109.1 | -17.2% | ±3.0% | 2/2 |
| v2-async | small-get | 255.7 | 259.7 | +1.6% | ±4.0% | 1/2 |
| v2-sync | small-put | 131.9 | 101.5 | -23.0% | ±1.8% | 2/2 |
| v2-async | small-put | 242.2 | 244.7 | +1.0% | ±0.8% | 0/2 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `async-stock` | 160.0 | 164.1 | 168.1 | 5.1% |
| v2-sync | small-get | `fidelity2` | 140.4 | 140.4 | 140.5 | 0.1% |
| v2-async | small-get | `async-stock` | 260.8 | 262.3 | 263.8 | 1.2% |
| v2-async | small-get | `fidelity2` | 260.6 | 262.5 | 264.3 | 1.4% |
| v2-sync | small-put | `async-stock` | 163.4 | 164.9 | 166.3 | 1.8% |
| v2-sync | small-put | `fidelity2` | 131.8 | 132.9 | 134.1 | 1.7% |
| v2-async | small-put | `async-stock` | 250.4 | 252.7 | 255.0 | 1.8% |
| v2-async | small-put | `fidelity2` | 249.3 | 249.4 | 249.6 | 0.1% |

