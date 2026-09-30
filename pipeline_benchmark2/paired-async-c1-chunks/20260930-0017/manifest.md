# Paired A/B timing comparison 20260930-0017

## Arms

### bridge

- jar: `/local/home/alexwoo/tmpws/javav2-smithy-java-bridge/pipeline_benchmark2/jars/bridge-async-bridge-c1d88972e18-plus-async-wip-dirty.jar`
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

## Environment

- Date: 2026-09-30T00:17:49Z (UTC)
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

- [1] rep 1, arm `bridge`, v2-async/small-get — ok, started 2026-09-30T00:17:49Z, log `logs/bridge_v2-async_small-get_rep1.log`
- [2] rep 1, arm `stock`, v2-async/small-get — ok, started 2026-09-30T00:18:27Z, log `logs/stock_v2-async_small-get_rep1.log`
- [3] rep 1, arm `bridge`, v2-async-netty/small-get — ok, started 2026-09-30T00:19:05Z, log `logs/bridge_v2-async-netty_small-get_rep1.log`
- [4] rep 1, arm `stock`, v2-async-netty/small-get — ok, started 2026-09-30T00:20:18Z, log `logs/stock_v2-async-netty_small-get_rep1.log`
- [5] rep 1, arm `bridge`, v2-sync/small-get — ok, started 2026-09-30T00:21:37Z, log `logs/bridge_v2-sync_small-get_rep1.log`
- [6] rep 1, arm `stock`, v2-sync/small-get — ok, started 2026-09-30T00:22:03Z, log `logs/stock_v2-sync_small-get_rep1.log`
- [7] rep 1, arm `bridge`, v2-async/small-put — ok, started 2026-09-30T00:22:37Z, log `logs/bridge_v2-async_small-put_rep1.log`
- [8] rep 1, arm `stock`, v2-async/small-put — ok, started 2026-09-30T00:23:14Z, log `logs/stock_v2-async_small-put_rep1.log`
- [9] rep 1, arm `bridge`, v2-async-netty/small-put — ok, started 2026-09-30T00:23:51Z, log `logs/bridge_v2-async-netty_small-put_rep1.log`
- [10] rep 1, arm `stock`, v2-async-netty/small-put — ok, started 2026-09-30T00:24:55Z, log `logs/stock_v2-async-netty_small-put_rep1.log`
- [11] rep 1, arm `bridge`, v2-sync/small-put — ok, started 2026-09-30T00:25:52Z, log `logs/bridge_v2-sync_small-put_rep1.log`
- [12] rep 1, arm `stock`, v2-sync/small-put — ok, started 2026-09-30T00:26:22Z, log `logs/stock_v2-sync_small-put_rep1.log`
- [13] rep 1, arm `bridge`, v2-async/batch-get — ok, started 2026-09-30T00:26:55Z, log `logs/bridge_v2-async_batch-get_rep1.log`
- [14] rep 1, arm `stock`, v2-async/batch-get — ok, started 2026-09-30T00:28:09Z, log `logs/stock_v2-async_batch-get_rep1.log`
- [15] rep 1, arm `bridge`, v2-async-netty/batch-get — ok, started 2026-09-30T00:29:28Z, log `logs/bridge_v2-async-netty_batch-get_rep1.log`
- [16] rep 1, arm `stock`, v2-async-netty/batch-get — ok, started 2026-09-30T00:31:14Z, log `logs/stock_v2-async-netty_batch-get_rep1.log`
- [17] rep 1, arm `bridge`, v2-sync/batch-get — ok, started 2026-09-30T00:33:04Z, log `logs/bridge_v2-sync_batch-get_rep1.log`
- [18] rep 1, arm `stock`, v2-sync/batch-get — ok, started 2026-09-30T00:34:07Z, log `logs/stock_v2-sync_batch-get_rep1.log`
- [19] rep 1, arm `bridge`, v2-async/batch-put — ok, started 2026-09-30T00:35:35Z, log `logs/bridge_v2-async_batch-put_rep1.log`
- [20] rep 1, arm `stock`, v2-async/batch-put — ok, started 2026-09-30T00:36:43Z, log `logs/stock_v2-async_batch-put_rep1.log`
- [21] rep 1, arm `bridge`, v2-async-netty/batch-put — ok, started 2026-09-30T00:38:23Z, log `logs/bridge_v2-async-netty_batch-put_rep1.log`
- [22] rep 1, arm `stock`, v2-async-netty/batch-put — ok, started 2026-09-30T00:39:33Z, log `logs/stock_v2-async-netty_batch-put_rep1.log`
- [23] rep 1, arm `bridge`, v2-sync/batch-put — ok, started 2026-09-30T00:41:28Z, log `logs/bridge_v2-sync_batch-put_rep1.log`
- [24] rep 1, arm `stock`, v2-sync/batch-put — ok, started 2026-09-30T00:42:18Z, log `logs/stock_v2-sync_batch-put_rep1.log`

## Summary

- finished: 2026-09-30T00:44:06Z
- runs: 24, failures: 0
