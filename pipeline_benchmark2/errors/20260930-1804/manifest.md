# Error behavior sweep 20260930-1804

- Date: 2026-09-30T18:04:10Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS
- faults: all, reps: 2, backoff: standard, warmup: no
- pinning: client=[0,1] server=[4,5,6], port 19082

## Arms

- `baseline`: ../../pipeline_benchmark2/jars/bridge-async-stock-published-2.46.10-dirty.jar — phase=async-stock git.commit=c1d88972e189046a6d13fc8c185002672112402f git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=published-2.46.10 build.time=2026-09-29T22:46:05Z sdk.v2.version=2.46.10 sdk.v1.version=1.12.797 smithy.java.version=1.6.1 
- `bridge`: /local/home/alexwoo/tmpws/javav2-smithy-java-bridge/test/standalone-e2e-benchmarks/target/bridge-errbridge-traits2.jar — phase=errbridge-traits2 git.commit=c1d88972e189046a6d13fc8c185002672112402f git.branch=smithy-java-bridge-alexwoo-full git.dirty.files=true sdk.commit=c1d88972e18-plus-traits-overrides-wip build.time=2026-09-30T18:03:57Z sdk.v2.version=2.46.11-SNAPSHOT sdk.v1.version=1.12.797 smithy.java.version=1.6.1 

The mock server comes from `baseline` and is shared by every arm; the fault catalogue
is a shared class, so the arms cannot disagree about what a mode means.

## Summary

- finished: 2026-09-30T18:05:04Z
- arms: 2, rows: 92, failures: 0
- data: `errors.csv`
