# Paired A/B summary — `20260930-1942`

Arms (baseline first): `fidelity2`, `async-stock`

- `fidelity2`: sdk_commit `c1d88972e18-plus-traits-overrides-wip`, 2 reps per case
- `async-stock`: sdk_commit `published-2.46.10`, 2 reps per case

**9 of 16 runs were not steady-state** (JIT still compiling inside the measured window): v2-async/small-get, v2-async/small-put, v2-sync/small-put. Per-operation CPU for those cases is unreliable; raise --iterations or treat them as latency-only.

Harness build identical across arms (commit `c1d88972e18`), so the SDK is the only difference.

## application cpu µs/op

| client | scenario | fidelity2 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 105.5 | 140.9 | +33.6% | ±7.8% | 0/2 |
| v2-async | small-get | 238.6 | 235.8 | -1.2% | ±0.0% | 2/2 |
| v2-sync | small-put | 105.3 | 124.8 | +18.6% | ±3.0% | 0/2 |
| v2-async | small-put | 227.5 | 227.4 | -0.0% | ±1.6% | 1/2 |

## mean latency µs

| client | scenario | fidelity2 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 139.7 | 180.4 | +29.0% | ±8.0% | 0/2 |
| v2-async | small-get | 247.8 | 260.6 | +5.2% | ±1.2% | 0/2 |
| v2-sync | small-put | 145.5 | 160.3 | +10.5% | ±6.7% | 0/2 |
| v2-async | small-put | 235.7 | 249.8 | +6.0% | ±4.1% | 0/2 |

## wall µs/op (1/throughput)

| client | scenario | fidelity2 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 139.8 | 180.5 | +29.0% | ±7.9% | 0/2 |
| v2-async | small-get | 255.9 | 270.1 | +5.6% | ±1.1% | 0/2 |
| v2-sync | small-put | 145.8 | 160.5 | +10.5% | ±6.9% | 0/2 |
| v2-async | small-put | 243.9 | 259.7 | +6.5% | ±3.9% | 0/2 |

## process cpu µs/op (includes JIT/GC/VM)

| client | scenario | fidelity2 mean | async-stock mean | paired delta | spread of pairs | wins |
|--------|----------|----:|----:|----:|----:|----:|
| v2-sync | small-get | 108.4 | 145.7 | +34.4% | ±10.5% | 0/2 |
| v2-async | small-get | 251.5 | 257.2 | +2.3% | ±2.5% | 0/2 |
| v2-sync | small-put | 111.1 | 126.9 | +14.5% | ±6.0% | 0/2 |
| v2-async | small-put | 239.2 | 246.4 | +3.0% | ±2.6% | 0/2 |

## Per-arm run-to-run spread

How noisy each arm was on its own. Where this is much larger than the paired spread
above, pairing is doing real work and unpaired numbers from this machine can't be
trusted at that resolution.

| client | scenario | arm | min | mean | max | spread |
|--------|----------|-----|----:|----:|----:|-------:|
| v2-sync | small-get | `fidelity2` | 137.8 | 139.8 | 141.8 | 2.9% |
| v2-sync | small-get | `async-stock` | 170.1 | 180.5 | 190.9 | 12.2% |
| v2-async | small-get | `fidelity2` | 251.6 | 255.9 | 260.1 | 3.4% |
| v2-async | small-get | `async-stock` | 267.7 | 270.1 | 272.6 | 1.8% |
| v2-sync | small-put | `fidelity2` | 135.8 | 145.8 | 155.7 | 14.7% |
| v2-sync | small-put | `async-stock` | 156.6 | 160.5 | 164.4 | 5.0% |
| v2-async | small-put | `fidelity2` | 243.6 | 243.9 | 244.2 | 0.2% |
| v2-async | small-put | `async-stock` | 252.6 | 259.7 | 266.8 | 5.6% |

