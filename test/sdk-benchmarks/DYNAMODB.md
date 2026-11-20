# DynamoDB Performance Benchmarks

Manual-first DynamoDB Tier C (mocked full SDK pipeline) extension in `sdk-benchmarks`.
New classes under `software.amazon.awssdk.benchmark.dynamodb.pipeline` are **not** selected by
`BenchmarkRunner`, shards, baselines, or CI.

Tier A (micro/protocol) and Tier B (mapper isolation) already exist elsewhere in this module and
are unchanged by this extension. Results across tiers answer different questions and must **not**
be compared as one score.

## Measurement context

| Tier | Status in this module | What it measures | What it excludes |
|---|---|---|---|
| **A** — Pure micro / protocol | Existing (unchanged) | Marshalling, JSON protocol, EnhancedDocument JSON conversion, cold start | Full client pipeline, network |
| **B** — Mapper isolation | Existing (unchanged) | Enhanced mapping via stub `V2TestDynamoDb*Client` | Marshalling, signing, HTTP, network |
| **C** — Mocked pipeline | Partial existing + this extension | Full sync/async SDK client path with deterministic mock HTTP | Network and DynamoDB service latency |

Existing partial Tier C coverage: `EnhancedClientGetOverheadBenchmark` /
`EnhancedClientPutOverheadBenchmark` (sync LOW/TYPED Get/Put with mocked HTTP).

This extension adds a shared fixture/mock structure and common measurement boundary across
LOW / DOCUMENT / TYPED, plus DOCUMENT table Get/Put, TYPED Query, and async LOW/TYPED Get.

Package layout:

- Tier C (new): `software.amazon.awssdk.benchmark.dynamodb.pipeline`
- Shared: `…dynamodb.fixture`, `…dynamodb.mock`, `DynamoDbBenchmarkConstant`

## Client layers (LOW / DOCUMENT / TYPED)

These labels are an analysis taxonomy for the suite, not official AWS client product names:

| Layer | API surface | What the timed path emphasizes |
|---|---|---|
| **LOW** | `DynamoDbClient` / `DynamoDbAsyncClient` | Direct DynamoDB request/response (pre-built low-level requests in sync Get/Put) |
| **DOCUMENT** | `DynamoDbTable<EnhancedDocument>` | Enhanced Document model over the same logical item |
| **TYPED** | `DynamoDbTable<BenchmarkItem>` (bean mapping) | Enhanced typed mapping to/from the shared fixture bean |

LOW, DOCUMENT, and TYPED reuse the same logical fixture and keys so cross-layer comparisons stay fair.

## Inventory (this extension)

| Tier | Layer | Sync/Async | Operations |
|---|---|---|---|
| C | LOW | Sync | GetItem, PutItem |
| C | DOCUMENT | Sync | GetItem, PutItem |
| C | TYPED | Sync | GetItem, PutItem, Query (first page) |
| C | LOW | Async | GetItem (`.join()`) |
| C | TYPED | Async | GetItem (`.join()`) |

Deferred by design: DOCUMENT async, LOW Query, async Put/Query, concurrency, Batch/Transact,
Update/Delete/Scan on the pipeline boundary, CI/shards/baselines.

## JMH mode

| Family | Mode | Unit | Defaults |
|---|---|---|---|
| Tier C (`pipeline`) | `AverageTime` | µs/op | warmup 5 / measurement 5 / forks 2 |

Reduced CLI overrides (`-wi 1 -i 1 -f 1`) are fine for smoke checks. Use class defaults for meaningful comparisons.

## Manual build and run

From the repository root:

```bash
mvn clean install -P quick -pl :sdk-benchmarks --am
cd test/sdk-benchmarks
```

List DynamoDB pipeline benchmarks:

```bash
# Windows (cmd / PowerShell)
java -jar target/benchmarks.jar -l | findstr /i "dynamodb.pipeline"

# Unix-like (macOS / Linux / Git Bash)
java -jar target/benchmarks.jar -l | grep -i dynamodb.pipeline
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

### JSON output and profiling

```bash
java -jar target/benchmarks.jar LowLevelGetItemBenchmark -rf json -rff results.json
java -jar target/benchmarks.jar LowLevelGetItemBenchmark -prof gc
```

## Interpretation

- **Tier C** is deterministic SDK-side cost (mock HTTP). Best signal for SDK regressions.
- **Async** single-op benches include mock/async executor scheduling and `.join()` completion.
- Untimed smoke calls (pipeline warm on mocked) are **not** JMH warmup iterations — JMH still runs
  its own warmup afterward.

## Automation invariant

Do **not** add these classes to `BenchmarkRunner`, shards, `baseline.json`, or CI without an explicit
follow-up design. `mvn package` / `exec:exec` behavior for existing suites is unchanged.
