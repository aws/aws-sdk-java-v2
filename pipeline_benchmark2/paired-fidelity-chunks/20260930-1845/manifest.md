# Paired A/B timing comparison 20260930-1845

## Arms

### stock

- jar: `/local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-async-stock-published-2.46.10-dirty.jar`
- provenance:
    - phase=async-stock
    - git.commit=c1d88972e189046a6d13fc8c185002672112402f
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=published-2.46.10
    - build.time=2026-09-29T22:46:05Z
    - sdk.v2.version=2.46.10
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

### bridge

- jar: `/local/home/alexwoo/tmpws/javav2-smithy-java-bridge/test/standalone-e2e-benchmarks/target/bridge-errbridge-traits3.jar`
- provenance:
    - phase=errbridge-traits3
    - git.commit=c1d88972e189046a6d13fc8c185002672112402f
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=c1d88972e18-plus-traits-overrides-wip
    - build.time=2026-09-30T18:08:25Z
    - sdk.v2.version=2.46.11-SNAPSHOT
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

## Environment

- Date: 2026-09-30T18:45:30Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64
- Hardware: Intel(R) Xeon(R) Platinum 8124M CPU @ 3.00GHz, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS

## Parameters

- iterations: 50000, warmup: 30000
- reps of the whole pair: 1
- clients: v2-sync,v2-async
- scenarios: small-get,small-put,batch-get,batch-put
- concurrency: 1, async mode: inflight
- pinning: client=[0,1] server=[4,5,6]
- client jvm args: (none)
- server jvm args: (none)
- server port: 19080 (fresh out-of-process mock server per run)
- total JVM runs: 16

## Design

Arms alternate within each repetition, and the arm order reverses on even repetitions, so
neither arm systematically occupies the warmer or colder position. Only timing is measured;
profiling perturbs it and belongs in a separate collect.sh run.

## Runs

- [1] rep 1, arm `stock`, v2-sync/small-get — ok, started 2026-09-30T18:45:30Z, log `logs/stock_v2-sync_small-get_rep1.log`
- [2] rep 1, arm `bridge`, v2-sync/small-get — ok, started 2026-09-30T18:46:07Z, log `logs/bridge_v2-sync_small-get_rep1.log`
- [3] rep 1, arm `stock`, v2-async/small-get — ok, started 2026-09-30T18:46:42Z, log `logs/stock_v2-async_small-get_rep1.log`
- [4] rep 1, arm `bridge`, v2-async/small-get — ok, started 2026-09-30T18:47:19Z, log `logs/bridge_v2-async_small-get_rep1.log`
- [5] rep 1, arm `stock`, v2-sync/small-put — ok, started 2026-09-30T18:48:03Z, log `logs/stock_v2-sync_small-put_rep1.log`
- [6] rep 1, arm `bridge`, v2-sync/small-put — ok, started 2026-09-30T18:48:36Z, log `logs/bridge_v2-sync_small-put_rep1.log`
- [7] rep 1, arm `stock`, v2-async/small-put — ok, started 2026-09-30T18:49:06Z, log `logs/stock_v2-async_small-put_rep1.log`
- [8] rep 1, arm `bridge`, v2-async/small-put — ok, started 2026-09-30T18:49:43Z, log `logs/bridge_v2-async_small-put_rep1.log`
- [9] rep 1, arm `stock`, v2-sync/batch-get — ok, started 2026-09-30T18:50:25Z, log `logs/stock_v2-sync_batch-get_rep1.log`
- [10] rep 1, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-30T18:51:55Z, log `logs/bridge_v2-sync_batch-get_rep1.log`
- [11] rep 1, arm `stock`, v2-async/batch-get — ok, started 2026-09-30T18:52:53Z, log `logs/stock_v2-async_batch-get_rep1.log`
- [12] rep 1, arm `bridge`, v2-async/batch-get — ok, started 2026-09-30T18:54:18Z, log `logs/bridge_v2-async_batch-get_rep1.log`
- [13] rep 1, arm `stock`, v2-sync/batch-put — ok, started 2026-09-30T18:55:30Z, log `logs/stock_v2-sync_batch-put_rep1.log`
- [14] rep 1, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-30T18:57:14Z, log `logs/bridge_v2-sync_batch-put_rep1.log`
- [15] rep 1, arm `stock`, v2-async/batch-put — ok, started 2026-09-30T18:58:08Z, log `logs/stock_v2-async_batch-put_rep1.log`
- [16] rep 1, arm `bridge`, v2-async/batch-put — ok, started 2026-09-30T18:59:45Z, log `logs/bridge_v2-async_batch-put_rep1.log`

## Summary

- finished: 2026-09-30T19:00:54Z
- runs: 16, failures: 0
