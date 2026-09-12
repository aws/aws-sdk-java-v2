# DynamoDB Performance Benchmarks

Manual-first DynamoDB suite in `sdk-benchmarks`. New Tier C / Tier D classes are **not** selected by
`BenchmarkRunner`, shards, baselines, or CI.

Results across tiers answer different questions and must **not** be compared as one score.

## Measurement tiers

| Tier | What it measures | What it excludes |
|---|---|---|
| **A** — Pure micro / protocol | Marshalling, JSON protocol, schema/JSON conversion, cold start | Full client pipeline, network |
| **B** — Mapper isolation | Enhanced mapping via stub `V2TestDynamoDb*Client` | Marshalling, signing, HTTP, network |
| **C** — Mocked pipeline *(new)* | Full sync/async SDK client path with deterministic mock HTTP | Network and DynamoDB service latency |
| **D** — Live DynamoDB *(new)* | End-to-end path: SDK + HTTP transport + **network latency** + **DynamoDB service latency** + response handling | Table provisioning, seeding, client/fixture construction (those stay in `@Setup` / `@TearDown`, outside the timed `@Benchmark` method) |

Packages:

- Tier C: `software.amazon.awssdk.benchmark.dynamodb.pipeline`
- Tier D: `software.amazon.awssdk.benchmark.dynamodb.live`
- Shared: `…dynamodb.fixture`, `…dynamodb.mock`, `DynamoDbBenchmarkConstant`

## Client layers (LOW / DOCUMENT / TYPED)

These labels are an analysis taxonomy for the suite, not official AWS client product names:

| Layer | API surface | What the timed path emphasizes |
|---|---|---|
| **LOW** | `DynamoDbClient` / `DynamoDbAsyncClient` | Direct DynamoDB request/response (pre-built low-level requests in sync Get/Put) |
| **DOCUMENT** | `DynamoDbTable<EnhancedDocument>` | Enhanced Document model over the same logical item |
| **TYPED** | `DynamoDbTable<BenchmarkItem>` (bean mapping) | Enhanced typed mapping to/from the shared fixture bean |

LOW, DOCUMENT, and TYPED reuse the same logical fixture and keys so cross-layer comparisons stay fair within a tier.

## Inventory (implemented)

| Tier | Layer | Sync/Async | Operations |
|---|---|---|---|
| A | Protocol / micro | Sync (existing) | Existing marshaller / protocol / cold-start benches |
| B | TYPED mapper isolation | Sync (existing) | Get/Put/Query/Update/Delete/Scan via stub clients |
| C | LOW | Sync | GetItem, PutItem |
| C | DOCUMENT | Sync | GetItem, PutItem |
| C | TYPED | Sync | GetItem, PutItem, Query (first page) |
| C | LOW | Async | GetItem (`.join()`) |
| C | TYPED | Async | GetItem (`.join()`) |
| D | LOW | Sync | GetItem, PutItem |
| D | TYPED | Sync | GetItem, PutItem, Query (first page) |

Deferred by design: DOCUMENT async, LOW Query, async Put/Query, concurrency, Batch/Transact, CI/shards/baselines.

## JMH modes

| Family | Mode | Unit | Defaults |
|---|---|---|---|
| Tier C (`pipeline`) | `AverageTime` | µs/op | warmup 5 / measurement 5 / forks 2 |
| Tier D (`live`) | `SampleTime` | ms/op | warmup 3 / measurement 5 / fork 1 |

Reduced CLI overrides (`-wi 1 -i 1 -f 1`) are fine for smoke checks. Use class defaults for meaningful comparisons.

## Manual build and run

From the repository root:

```bash
mvn clean install -P quick -pl :sdk-benchmarks --am
cd test/sdk-benchmarks
```

List DynamoDB-related benchmarks:

```bash
# Windows (cmd / PowerShell)
java -jar target/benchmarks.jar -l | findstr /i dynamodb

# Unix-like (macOS / Linux / Git Bash)
java -jar target/benchmarks.jar -l | grep -i dynamodb
```

### Tier C (mocked — no AWS)

```bash
# All Tier C pipeline benches
java -jar target/benchmarks.jar "software.amazon.awssdk.benchmark.dynamodb.pipeline"

# LOW
java -jar target/benchmarks.jar ".*pipeline.LowLevel"

# DOCUMENT
java -jar target/benchmarks.jar ".*pipeline.Document"

# TYPED
java -jar target/benchmarks.jar ".*pipeline.Typed"

# Async Get only
java -jar target/benchmarks.jar ".*pipeline.*Async"

# Single class
java -jar target/benchmarks.jar LowLevelGetItemBenchmark
```

### Tier D (live — opt-in required)

```bash
# PowerShell
$env:DYNAMODB_BENCHMARK_LIVE="true"
$env:AWS_REGION="us-east-1"   # or rely on the default AWS region chain / DYNAMODB_BENCHMARK_REGION

java -jar target/benchmarks.jar "software.amazon.awssdk.benchmark.dynamodb.live"

# Equivalent system property
java -Ddynamodb.benchmark.live=true -jar target/benchmarks.jar "software.amazon.awssdk.benchmark.dynamodb.live"
```

Region override (optional):

```bash
$env:DYNAMODB_BENCHMARK_REGION="us-west-2"
# or: -Ddynamodb.benchmark.region=us-west-2
```

### JSON output and profiling

```bash
java -jar target/benchmarks.jar LowLevelGetItemBenchmark -rf json -rff results.json
java -jar target/benchmarks.jar LowLevelGetItemBenchmark -prof gc
```

## Live safety

- AWS credentials are required (`DefaultCredentialsProvider`).
- Opt-in is mandatory: `DYNAMODB_BENCHMARK_LIVE=true` or `-Ddynamodb.benchmark.live=true`.
  Without it, `@Setup` aborts **before** credential resolution, client construction, or any AWS call.
  (System property takes precedence over the environment variable if both are set.)
- Each trial creates a **unique** table: `sdk-java-ddb-perf-{op}-{8hex}`.
- Billing mode: **PAY_PER_REQUEST**.
- Tables are tagged (`sdk-java-ddb-perf-benchmark=owned`, `Purpose=aws-sdk-java-v2-dynamodb-live-benchmark`) so orphans from interrupted runs can be identified.
- Teardown deletes **only** the exact table created by that trial (`createdByThisTrial`).
- Interrupted runs (kill/OOM) may leave orphaned tagged tables — clean up manually if needed.
- Live runs incur DynamoDB request cost and service/network variance.

Retry observation (default on): a lightweight `ExecutionInterceptor` counts attempts. Disable with
`DYNAMODB_BENCHMARK_LIVE_RETRY_OBSERVE=false` / `-Ddynamodb.benchmark.live.retryObserve=false`.
Default SDK retry policy is unchanged. Runs with retries print a warning — interpret scores carefully.

## Interpretation

- **Tier C** is deterministic SDK-side cost (mock HTTP). Best signal for SDK regressions.
- **Tier D** includes network and DynamoDB latency. Use for directional E2E checks, not µs SDK diffs.
- **LOW vs TYPED** under Tier D is directional only (service variance dominates).
- **Async** single-op benches include mock/async executor scheduling and `.join()` completion.
- Untimed smoke calls (DNS/TLS/pool init on live; pipeline warm on mocked) are **not** JMH warmup
  iterations — JMH still runs its own warmup afterward.

## Automation invariant

Do **not** add these classes to `BenchmarkRunner`, shards, `baseline.json`, or CI without an explicit
follow-up design. `mvn package` / `exec:exec` behavior for existing suites is unchanged.
