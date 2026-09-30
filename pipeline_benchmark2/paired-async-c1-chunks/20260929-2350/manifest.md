# Paired A/B timing comparison 20260929-2350

## Arms

### stock

- jar: `/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-async-stock-published-2.46.10-dirty.jar`
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

- jar: `/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-async-bridge-c1d88972e18-plus-async-wip-dirty.jar`
- provenance:
    - phase=async-bridge
    - git.commit=c1d88972e189046a6d13fc8c185002672112402f
    - git.branch=smithy-java-bridge-alexwoo-full
    - git.dirty.files=true
    - sdk.commit=c1d88972e18-plus-async-wip
    - build.time=2026-09-29T22:46:18Z
    - sdk.v2.version=2.46.11-SNAPSHOT
    - sdk.v1.version=1.12.797
    - smithy.java.version=1.6.1

## Environment

- Date: 2026-09-29T23:50:47Z (UTC)
- Host: dev-dsk-alexwoo-2b-ee3cc828.us-west-2.amazon.com, Linux x86_64
- Hardware: Intel(R) Xeon(R) Platinum 8124M CPU @ 3.00GHz, 8 logical cores
- Java: openjdk version "21.0.9" 2025-10-21 LTS

## Parameters

- iterations: 50000, warmup: 30000
- reps of the whole pair: 1
- clients: v2-async,v2-async-netty,v2-sync
- scenarios: small-get,small-put,batch-get,batch-put
- concurrency: 1, async mode: inflight
- pinning: client=[0,1] server=[4,5,6]
- client jvm args: (none)
- server jvm args: (none)
- server port: 19080 (fresh out-of-process mock server per run)
- total JVM runs: 24

## Design

Arms alternate within each repetition, and the arm order reverses on even repetitions, so
neither arm systematically occupies the warmer or colder position. Only timing is measured;
profiling perturbs it and belongs in a separate collect.sh run.

## Runs

- [1] rep 1, arm `stock`, v2-async/small-get — ok, started 2026-09-29T23:50:47Z, log `logs/stock_v2-async_small-get_rep1.log`
- [2] rep 1, arm `bridge`, v2-async/small-get — ok, started 2026-09-29T23:51:27Z, log `logs/bridge_v2-async_small-get_rep1.log`
- [3] rep 1, arm `stock`, v2-async-netty/small-get — ok, started 2026-09-29T23:52:06Z, log `logs/stock_v2-async-netty_small-get_rep1.log`
- [4] rep 1, arm `bridge`, v2-async-netty/small-get — ok, started 2026-09-29T23:53:25Z, log `logs/bridge_v2-async-netty_small-get_rep1.log`
- [5] rep 1, arm `stock`, v2-sync/small-get — ok, started 2026-09-29T23:54:26Z, log `logs/stock_v2-sync_small-get_rep1.log`
- [6] rep 1, arm `bridge`, v2-sync/small-get — ok, started 2026-09-29T23:55:10Z, log `logs/bridge_v2-sync_small-get_rep1.log`
- [7] rep 1, arm `stock`, v2-async/small-put — ok, started 2026-09-29T23:55:37Z, log `logs/stock_v2-async_small-put_rep1.log`
- [8] rep 1, arm `bridge`, v2-async/small-put — ok, started 2026-09-29T23:56:16Z, log `logs/bridge_v2-async_small-put_rep1.log`
- [9] rep 1, arm `stock`, v2-async-netty/small-put — ok, started 2026-09-29T23:56:55Z, log `logs/stock_v2-async-netty_small-put_rep1.log`
- [10] rep 1, arm `bridge`, v2-async-netty/small-put — ok, started 2026-09-29T23:58:09Z, log `logs/bridge_v2-async-netty_small-put_rep1.log`
- [11] rep 1, arm `stock`, v2-sync/small-put — ok, started 2026-09-29T23:59:10Z, log `logs/stock_v2-sync_small-put_rep1.log`
- [12] rep 1, arm `bridge`, v2-sync/small-put — ok, started 2026-09-29T23:59:44Z, log `logs/bridge_v2-sync_small-put_rep1.log`
- [13] rep 1, arm `stock`, v2-async/batch-get — ok, started 2026-09-30T00:00:14Z, log `logs/stock_v2-async_batch-get_rep1.log`
- [14] rep 1, arm `bridge`, v2-async/batch-get — ok, started 2026-09-30T00:01:37Z, log `logs/bridge_v2-async_batch-get_rep1.log`
- [15] rep 1, arm `stock`, v2-async-netty/batch-get — ok, started 2026-09-30T00:02:51Z, log `logs/stock_v2-async-netty_batch-get_rep1.log`
- [16] rep 1, arm `bridge`, v2-async-netty/batch-get — ok, started 2026-09-30T00:04:39Z, log `logs/bridge_v2-async-netty_batch-get_rep1.log`
- [17] rep 1, arm `stock`, v2-sync/batch-get — ok, started 2026-09-30T00:06:00Z, log `logs/stock_v2-sync_batch-get_rep1.log`
- [18] rep 1, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-30T00:07:28Z, log `logs/bridge_v2-sync_batch-get_rep1.log`
- [19] rep 1, arm `stock`, v2-async/batch-put — ok, started 2026-09-30T00:08:23Z, log `logs/stock_v2-async_batch-put_rep1.log`
- [20] rep 1, arm `bridge`, v2-async/batch-put — ok, started 2026-09-30T00:10:13Z, log `logs/bridge_v2-async_batch-put_rep1.log`
- [21] rep 1, arm `stock`, v2-async-netty/batch-put — ok, started 2026-09-30T00:11:22Z, log `logs/stock_v2-async-netty_batch-put_rep1.log`
- [22] rep 1, arm `bridge`, v2-async-netty/batch-put — ok, started 2026-09-30T00:13:16Z, log `logs/bridge_v2-async-netty_batch-put_rep1.log`
- [23] rep 1, arm `stock`, v2-sync/batch-put — ok, started 2026-09-30T00:14:49Z, log `logs/stock_v2-sync_batch-put_rep1.log`
- [24] rep 1, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-30T00:16:35Z, log `logs/bridge_v2-sync_batch-put_rep1.log`

## Summary

- finished: 2026-09-30T00:17:25Z
- runs: 24, failures: 0
