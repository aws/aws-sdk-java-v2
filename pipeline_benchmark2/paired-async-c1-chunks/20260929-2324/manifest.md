# Paired A/B timing comparison 20260929-2324

## Arms

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

## Environment

- Date: 2026-09-29T23:24:20Z (UTC)
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

- [1] rep 1, arm `bridge`, v2-async/small-get — ok, started 2026-09-29T23:24:20Z, log `logs/bridge_v2-async_small-get_rep1.log`
- [2] rep 1, arm `stock`, v2-async/small-get — ok, started 2026-09-29T23:24:59Z, log `logs/stock_v2-async_small-get_rep1.log`
- [3] rep 1, arm `bridge`, v2-async-netty/small-get — ok, started 2026-09-29T23:25:38Z, log `logs/bridge_v2-async-netty_small-get_rep1.log`
- [4] rep 1, arm `stock`, v2-async-netty/small-get — ok, started 2026-09-29T23:26:39Z, log `logs/stock_v2-async-netty_small-get_rep1.log`
- [5] rep 1, arm `bridge`, v2-sync/small-get — ok, started 2026-09-29T23:27:54Z, log `logs/bridge_v2-sync_small-get_rep1.log`
- [6] rep 1, arm `stock`, v2-sync/small-get — ok, started 2026-09-29T23:28:25Z, log `logs/stock_v2-sync_small-get_rep1.log`
- [7] rep 1, arm `bridge`, v2-async/small-put — ok, started 2026-09-29T23:28:59Z, log `logs/bridge_v2-async_small-put_rep1.log`
- [8] rep 1, arm `stock`, v2-async/small-put — ok, started 2026-09-29T23:29:54Z, log `logs/stock_v2-async_small-put_rep1.log`
- [9] rep 1, arm `bridge`, v2-async-netty/small-put — ok, started 2026-09-29T23:30:30Z, log `logs/bridge_v2-async-netty_small-put_rep1.log`
- [10] rep 1, arm `stock`, v2-async-netty/small-put — ok, started 2026-09-29T23:31:11Z, log `logs/stock_v2-async-netty_small-put_rep1.log`
- [11] rep 1, arm `bridge`, v2-sync/small-put — ok, started 2026-09-29T23:32:09Z, log `logs/bridge_v2-sync_small-put_rep1.log`
- [12] rep 1, arm `stock`, v2-sync/small-put — ok, started 2026-09-29T23:32:34Z, log `logs/stock_v2-sync_small-put_rep1.log`
- [13] rep 1, arm `bridge`, v2-async/batch-get — ok, started 2026-09-29T23:33:09Z, log `logs/bridge_v2-async_batch-get_rep1.log`
- [14] rep 1, arm `stock`, v2-async/batch-get — ok, started 2026-09-29T23:34:24Z, log `logs/stock_v2-async_batch-get_rep1.log`
- [15] rep 1, arm `bridge`, v2-async-netty/batch-get — ok, started 2026-09-29T23:35:42Z, log `logs/bridge_v2-async-netty_batch-get_rep1.log`
- [16] rep 1, arm `stock`, v2-async-netty/batch-get — ok, started 2026-09-29T23:37:05Z, log `logs/stock_v2-async-netty_batch-get_rep1.log`
- [17] rep 1, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-29T23:38:55Z, log `logs/bridge_v2-sync_batch-get_rep1.log`
- [18] rep 1, arm `stock`, v2-sync/batch-get — ok, started 2026-09-29T23:39:51Z, log `logs/stock_v2-sync_batch-get_rep1.log`
- [19] rep 1, arm `bridge`, v2-async/batch-put — ok, started 2026-09-29T23:41:18Z, log `logs/bridge_v2-async_batch-put_rep1.log`
- [20] rep 1, arm `stock`, v2-async/batch-put — ok, started 2026-09-29T23:42:26Z, log `logs/stock_v2-async_batch-put_rep1.log`
- [21] rep 1, arm `bridge`, v2-async-netty/batch-put — ok, started 2026-09-29T23:44:06Z, log `logs/bridge_v2-async-netty_batch-put_rep1.log`
- [22] rep 1, arm `stock`, v2-async-netty/batch-put — ok, started 2026-09-29T23:45:42Z, log `logs/stock_v2-async-netty_batch-put_rep1.log`
- [23] rep 1, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-29T23:47:38Z, log `logs/bridge_v2-sync_batch-put_rep1.log`
- [24] rep 1, arm `stock`, v2-sync/batch-put — ok, started 2026-09-29T23:48:32Z, log `logs/stock_v2-sync_batch-put_rep1.log`

## Summary

- finished: 2026-09-29T23:50:21Z
- runs: 24, failures: 0
