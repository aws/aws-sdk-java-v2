# Standalone E2E DynamoDB Benchmarks

Fixed-iteration DynamoDB benchmarks comparing client stacks against an **out-of-process**
mock HTTP server:

| Client     | SDK                                   | HTTP transport                | Call model |
|------------|---------------------------------------|-------------------------------|------------|
| `v1`       | AWS SDK for Java V1 (1.12.797)        | Apache HttpClient 4.x         | Blocking, N threads |
| `v2-sync`  | AWS SDK for Java V2                   | **Apache5** (`Apache5HttpClient`) | Blocking, N threads |
| `v2-async` | AWS SDK for Java V2                   | **AWS CRT**                   | N in flight from one thread |
| `v2-async-netty` | AWS SDK for Java V2             | **Netty** (`NettyNioAsyncHttpClient`) | N in flight from one thread |
| `smithy`   | smithy-java (1.6.1), generated client | SmithyHttpClient (HTTP/1.1)   | Blocking, N threads |

On a jar built against the **smithy-java bridge** SDK, `v2-sync` is the V2 API running on the
smithy-java pipeline, and additional arms measure what that bridging costs by replacing bridged
components with smithy-java's own:

| Client | Replaces |
|---|---|
| `v2-sync-stripped` | all of the below at once — leaves the HTTP transport as the only bridged piece |
| `v2-sync-strip-endpoints` | the bridged V2 endpoint rules engine → a fixed endpoint |
| `v2-sync-strip-retries` | the bridged V2 retry strategy → smithy's `StandardRetryStrategy` |
| `v2-sync-strip-interceptors` | the V2 interceptor chain → nothing |
| `v2-sync-strip-errors` | the per-attempt V2 error enricher → nothing |

The **S3 streaming** scenarios have their own arm family, because they need a different client
(`S3Client`, path-style, TLS) and only answer the `*-object-*` scenarios:

| Client | SDK |
|---|---|
| `s3-v2-sync` | V2 `S3Client` on Apache5 over TLS |
| `s3-v2-sync-stripped`, `s3-v2-sync-strip-{endpoints,retries,interceptors,errors}` | the same strip axes as above |

`strip-interceptors` is more interesting here than for DynamoDB: S3 installs about a dozen execution
interceptors, so the arm measures a chain that actually does work rather than an empty one.

One arm goes the other way, restoring cost instead of removing it:

| Client | Restores |
|---|---|
| `v2-sync-inert-interceptors` | bridging the three interceptors `AwsDefaultClientBuilder` puts on every AWS client, which `V2ConfigTranslator.inert()` now drops because none of them can act (`compatability_issues.md` 2.1) |

Each of the strip arms is a **different SDK, not a faster one**: they drop real behavior (see
`compatability_issues.md` 10.6). On a baseline (unbridged) SDK the properties they set are simply
unread, so the arm measures the same thing as `v2-sync` — which is what makes a paired A/B between a
baseline jar and a bridge jar attribute the difference to bridging alone.

**Apache5 for sync and CRT for async** are the transports V2 is standardizing on, so they are the
ones this benchmark measures. Both are **pinned explicitly** in `Workloads`, and the transport in use
is printed in the run header and recorded in the `transport` results column.

Pinning matters here rather than being pedantry. `dynamodb` pulls in `apache5-client` and
`netty-nio-client` transitively, so more than one `SdkHttpService` can sit on the classpath, and V2
does not fail on that — `ClasspathSdkHttpServiceProvider` picks by an internal priority table. That
made the transport an invisible variable, and it had already gone wrong: earlier versions of this
README claimed `v2-sync` used Apache 4.x when Apache5 was what actually ran. Note also that V2's
current *async* resolution would pick Netty (priority 1), so `v2-async` is deliberately measuring the
intended long-term default rather than today's fallback.

This package supersedes the profiling setup in `test/benchmark-smithy-java` /
`pipeline_benchmark/` for DynamoDB, fixing its fairness issues:

1. **Retries** — previously smithy-java ran `RetryStrategy.noRetries()` while V2 ran its default
   strategy (per-call token acquisition and attempt accounting). Now every client is explicitly
   configured for standard token-bucket retries with **3 max attempts**: V2 uses
   `RetryMode.STANDARD`, smithy-java uses `StandardRetryStrategy.builder().maxAttempts(3)`, and
   V1 caps its default DynamoDB retry policy at `maxErrorRetry=2`. The server always returns 200,
   so no client actually retries — but all of them pay comparable retry bookkeeping per call.
2. **Payload wrapping** — the old S3 workload copied a 1 MB payload per call via
   `RequestBody.fromBytes` while smithy-java's `DataStream.ofBytes` only wrapped. This package is
   DynamoDB-only, where no raw-payload wrapper exists: request bodies are marshalled from model
   objects by each SDK on every call, and that marshalling is part of what we measure. The
   asymmetry does not apply here (see caveats).
3. **In-JVM mock server** — previously Jetty ran inside the benchmark JVM and contributed 32–60%
   of small-op allocation samples. The server now runs in its own JVM (`MockDdbServer`), launched
   and torn down by `scripts/benchmark.sh`, so client profiles contain only client work.

## Mock server vs. DynamoDB Local

Both were evaluated as the out-of-process target:

| | Canned-response mock server (chosen) | DynamoDB Local |
|---|---|---|
| Response bytes | Byte-identical for every SDK and every run, precomputed at startup | Depend on stored state; require a seeding step (create table, put items) |
| Server-side cost | ~zero (header lookup + fixed byte write), Jetty easily sustains the small-op request rate | Real parsing, SQLite storage engine (JNI), response synthesis — competes for cores with the client on the same box and adds run-to-run variance |
| Protocol fidelity | Hand-written wire JSON; does not validate requests | Real request validation and real error behavior |
| Setup | Same jar as the runner, `READY` line + `/ping` probe | Separate artifact with native libraries, table setup before each run |

**Recommendation: the canned-response mock server.** The goal is to isolate and compare
*client-side* pipeline cost (marshalling, signing, HTTP client, unmarshalling); a server that does
real database work adds noise without adding information about the clients. DynamoDB Local remains
useful as an occasional *correctness* cross-check that every SDK round-trips real responses: the
runner takes `--endpoint`, so you can point it at a running DynamoDB Local instance (create
`benchmark-table` with hash key `pk` first; note the response sizes will then differ from the
canned ones, so don't compare those numbers against mock-server runs).

## Prerequisites

- **JDK 21+** (smithy-java requires 21; tested on 25)
- **Maven**
- **SDK V2 installed locally** — the benchmark pom uses a `-SNAPSHOT` version that must be in
  your `~/.m2/repository`. If you're prototyping optimizations, build the modules you changed:

  ```bash
  # From the repo root; builds the DDB client, the HTTP clients, and their transitive deps.
  # codegen-maven-plugin is excluded from the reactor (its descriptor goal fails on recent JDKs)
  # and resolved from ~/.m2 instead.
  mvn clean install -pl ':dynamodb,:apache-client,:apache5-client,:aws-crt-client,!:codegen-maven-plugin' \
      --am -P quick -Dmaven.test.skip=true
  ```

  Install a *consistent* module set: installing a single core module on its own can leave `~/.m2`
  desynchronized and produce `VerifyError`s at benchmark runtime. `scripts/build-jar.sh` runs
  exactly this command for you.

  The `scripts/collect.sh` script auto-detects the version from the repo root pom and patches
  the benchmark pom if they drift apart, so you generally don't have to think about it.
- The generated smithy-java DynamoDB client published to `~/.m2` (one-time):

  ```bash
  cd ../benchmark-smithy-java/smithy-java-dynamodb-client
  ./gradlew publishToMavenLocal
  ```
- For `--profile cpu|alloc|wall`: async-profiler (`brew install async-profiler`, or set
  `ASYNC_PROFILER_LIB` to the library path)

## Quick start

```bash
cd test/standalone-e2e-benchmarks

# Smoke test (~2 s): all four scenarios, small iteration count
./scripts/benchmark.sh --client v2-sync --iterations 200 --warmup 50

# Robust run of one scenario
./scripts/benchmark.sh --client smithy --scenario small-get --iterations 200000

# S3 streaming (TLS is switched on automatically for *-object-* scenarios)
./scripts/benchmark.sh --client s3-v2-sync --scenario put-object-8m --iterations 1000 --warmup 250

# All clients, collected into one CSV for a comparison table
for c in v1 v2-sync v2-async smithy; do
  ./scripts/benchmark.sh --client $c --iterations 100000 \
      --append-to-results-file results.csv | grep RESULT
done
```

`scripts/benchmark.sh` builds on first use, starts the mock server as a child process, waits for its
`/ping` readiness probe, runs the client JVM, and kills the server on exit.

## Concurrency

`--concurrency N` keeps N operations in flight. It buys samples per second of wall clock and a more
realistic workload than one-at-a-time. The two client families reach it differently, on purpose:

- **Blocking clients** (`v1`, `v2-sync`, `smithy`) run **N caller threads**, each in its own closed
  loop. N threads is the only way a blocking client can have N operations outstanding.
- **The async client** (`v2-async`) keeps **N outstanding from a single submitting thread**, driven by
  completions. This is the shape async exists for. `--async-mode join` instead runs it on the
  blocking driver — N threads each blocking on its own future — which is what earlier collections
  here measured, and is a property of how the client is *used* rather than of the transport.
  Comparing the two modes separates those.

Every client's connection pool is sized to exactly N, so no client is measured waiting on its own
pool and none gets a bigger pool than another.

**Read the right metric.** Above concurrency 1, `avg_us_per_op` (wall / iterations) is the reciprocal
of throughput, *not* a latency. Use `mean_lat_us` and the percentiles for latency, `ops_per_wall_sec`
for throughput, and **`app_cpu_us_per_op`** for efficiency. At concurrency 1 `avg_us_per_op` and
`mean_lat_us` agree, which is a useful self-check that the latency recording is sound.

### Choosing a level: `scripts/concurrency-sweep.sh`

```bash
./scripts/concurrency-sweep.sh --clients v2-sync,v2-async --levels 1,2,4,8,16,32
```

Runs one scenario across concurrency levels and prints throughput, scaling, per-operation application
and server CPU, total core demand, and latency percentiles. Flags split into *hard* — where the run
stops measuring the client (`SERVER-SATURATED`, `OVERSUBSCRIBED`, `NOT-STEADY`) — and *soft*
(`CPU-DRIFT`), which is real contention cost but applies to both arms of a fixed-concurrency
comparison and so shifts absolute numbers without biasing a delta. Output goes to
`pipeline_benchmark2/sweeps/<runid>/` (gitignored).

A sweep on a 14-core M4 Pro, `small-get`, 300k operations per point (`app us/op` is application CPU
per operation — see the CPU section below):

| client | conc | ops/s | app µs/op | server µs/op | cores | flags |
|--------|-----:|------:|----------:|-------------:|------:|-------|
| `v2-sync` (apache5) | 1 | 11,628 | 39.2 | 43.5 | 1.0 | |
| | 2 | 22,046 | 40.0 | 46.2 | 1.9 | |
| | 4 | 36,408 | 46.0 | 58.8 | 3.8 | CPU-DRIFT |
| | 8 | 46,504 | 46.6 | 62.1 | 5.1 | CPU-DRIFT |
| `v2-async` (crt) | 1 | 8,057 | 77.8 | 45.8 | 1.0 | |
| | 2 | 15,007 | 83.4 | 47.3 | 2.0 | |
| | 4 | 25,015 | 101.6 | 58.3 | 4.0 | NOT-STEADY CPU-DRIFT |
| | 8 | 38,054 | 113.7 | 66.8 | 7.0 | NOT-STEADY CPU-DRIFT |
| `smithy` | 1 | 14,822 | 26.4 | 38.1 | 1.0 | |
| | 2 | 25,426 | 29.4 | 43.1 | 1.9 | CPU-DRIFT |
| | 8 | 47,901 | 33.3 | 57.9 | 4.4 | CPU-DRIFT |

Every client stays steady-state up to concurrency 2, and the async client stops settling at 4 and
above. **But concurrency defaults to 1**, because more samples turned out to cost more than they buy
on a box where the server shares the client's cores — see the null experiment below.

### The mock server *is* the ceiling above ~48k ops/s

Three very different clients all flatten near 48k ops/s while total CPU demand is only ~5 of 14
cores. That is the shape of a shared limit, and a direct test confirms it: pointing **two independent
client processes** at one server produced 52,569 ops/s against 46,407 for one — 1.13×, so a whole
extra client bought almost nothing.

Note what this means for the `server_saturated` flag: it stayed `false` throughout. The flag reports
Jetty's *handler pool* (`queue_size`, `low_on_threads`), and the pool really is idle — 9 busy threads
of 200, empty queue. The limit is below it, in the socket layer: over that test the server burned
**66.6 s of system time against 34.2 s of user time**, i.e. mostly kernel socket work on loopback.

Consequences:

- `server_saturated` is necessary but not sufficient. A `false` there does not mean the server has
  headroom; it means its thread pool does.
- Below concurrency ~4 this does not bite — the server costs 43–47 µs/op and total demand is ~2
  cores. At concurrency 8+ the client spends time waiting on the server, and per-operation CPU picks
  up contention that has nothing to do with the SDK.
- Raising the ceiling means getting the server off the box, which is what the separate-host plan is
  for. Until then, treat ~48k ops/s as the apparatus limit rather than a client result.

## How small a change can this actually resolve?

Run a **null experiment** to find out: the same SDK in both arms of a paired comparison, so every
difference it reports is noise. Build two jars from one `~/.m2` state with different phase labels
(verify with `unzip -v` that the entries match apart from the provenance stamp), then run
`paired-ab.sh` at the settings a real comparison would use.

Results on a 14-core M4 Pro, 200k operations, 4 repetitions, paired — application CPU per operation:

| case | concurrency | reported delta | spread of pairs | verdict |
|------|-----------:|---------------:|----------------:|---------|
| `v2-sync` / small-get | 1 | −0.6% | **±2.0%** | usable |
| `v2-sync` / small-get | 2 | +4.0% | ±8.4% | 4× worse |
| `v2-sync` / batch-put | 2 | −2.0% | ±4.6% | marginal |
| `v2-async` / small-get | 1 | −3.9% | ±11.3% | not usable |
| `v2-async` / small-get | 2 | −1.2% | ±16.0% | not usable |
| `v2-async` / batch-put | 2 | +6.3% | ±6.3% | marginal |

Read this as the floor beneath which a result is indistinguishable from nothing:

- **`v2-sync` at concurrency 1 is good** — ±2.0%, so 4 repetitions give a standard error near 1% and a
  3–4% change is a real signal. This is the configuration to use for CPU and latency claims.
- **Concurrency costs precision faster than it adds samples here.** Doubling throughput is worth √2 in
  precision; it cost about 4×. The cause is the co-resident server, which is also the throughput
  ceiling. On a host where the server does not share the client's cores this may well reverse — re-run
  the null experiment there rather than assuming.
- **`v2-async` is not resolvable at this scale.** ±11% at best means only changes above roughly 20%
  are visible. It needs many more repetitions (±11% takes ~57 reps to reach a 1.5% standard error) or
  a quieter host.
- **Allocation is unaffected by all of this** and reproduces to within 0.7%, which is why it remains
  the acceptance metric for a change.

Also useful: compare `spread of pairs` against the per-arm spread the summary prints below it. Per-arm
spread ran 3.5–43% against paired spreads of 2–16%, so pairing is doing real work — an unpaired
comparison on this machine cannot see anything under ~20%.

## Running on a dedicated benchmark host

The jar-based flow means the host needs nothing but a JDK and async-profiler — no repo, no Maven, no
`~/.m2` to keep in sync. Only the scripts and the artifacts under test travel.

```bash
export RACECAR_REMOTE_TARGET=ec2-user@my-host
export RACECAR_REMOTE_KEY=~/my-key.pem

# One-time on the host: JDK 25, async-profiler, git, perf
#   sudo dnf install -y java-25-amazon-corretto-devel git perf
#   curl -sSL <async-profiler linux-arm64 tarball> | tar xz && sudo mv async-profiler-* /opt/async-profiler
#   sudo ln -sf /opt/async-profiler/bin/{asprof,jfrconv} /usr/local/bin/

./scripts/deploy-remote.sh --target "$RACECAR_REMOTE_TARGET" --key "$RACECAR_REMOTE_KEY" \
    --jar ../../pipeline_benchmark2/jars/racecar-phase0-<sha>.jar \
    --jar ../../pipeline_benchmark2/jars/racecar-phaseD1-<sha>.jar

./scripts/remote-run.sh start ./my-run.sh   # detached; returns in seconds
./scripts/remote-run.sh status              # RUNNING / NOT-RUNNING, plus load and newest run dirs
./scripts/remote-run.sh log 40
./scripts/remote-run.sh wait 7200
./scripts/remote-run.sh fetch paired        # lands in pipeline_benchmark2/paired/host-<runid>/
./scripts/remote-run.sh stop
```

`deploy-remote.sh` deliberately does not ship `build-jar.sh`: building remotely needs the repo, and
the result would not be traceable to a commit the way a deployed jar is. Build locally, deploy the
artifact.

### Pinning and JVM settings that matter

Pinning is the main reason to use a dedicated Linux host. On a 64-core machine:

```bash
--pin-server 0-15 --pin-client 32-47 \
--jvm-args        "-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4" \
--server-jvm-args "-Xms2g -Xmx2g -XX:+AlwaysPreTouch -XX:ParallelGCThreads=4 -XX:CICompilerCount=4"
```

The JVM's defaults on 64 cores are **18 compiler threads and 43 GC threads**, for a benchmark that
runs one or two application threads. That is a lot of scheduling noise, and a fixed pre-touched heap
also keeps heap growth and page faults out of the measured window. `taskset` is Linux-only; the pin
flags fail with a clear message elsewhere.

### What the host bought

Null experiment (identical SDK in both arms), 200k operations, 5 reps, concurrency 1, application CPU
per operation:

| case | laptop (M4 Pro, unpinned) | c6g.metal (pinned, tuned) |
|------|--------------------------:|--------------------------:|
| `v2-sync` / small-get | ±2.0% | ±2.7% |
| `v2-async` / small-get | ±11.3% | **±2.5%** |

Sync was already fine on the laptop; **async is the unlock**, going from resolving only >20% changes
to resolving a few percent. Absolute costs are ~4× higher on Graviton2 (Neoverse-N1 at a fixed clock)
than on an M4 Pro, so allow longer runs for the same iteration counts.

Two host-specific notes. Application-CPU accounting matters even more here: with a short fixed warmup
one run showed process CPU of 995 µs/op against 227 µs/op of application CPU, a 4.4× inflation from
those 18 compiler threads. And `v2-async` still trips the `steady_state` check on ~30% of runs at 200k
iterations, because the slower cores stretch the window's share of residual compilation — raise
`--iterations` for async, or read those runs as latency-only.

## Scenarios

| Scenario    | Operation      | Request                             | Canned response                 |
|-------------|----------------|-------------------------------------|---------------------------------|
| `small-get` | GetItem        | 1 key (~66 B body)                  | `{"Item": ...}`, 470 B          |
| `small-put` | PutItem        | 12-attribute mixed item (~0.5 KB)   | `{}`, 2 B                       |
| `batch-get` | BatchGetItem   | 25 keys (~1.5 KB)                   | 25 × ~2 KB items, ~38 KB        |
| `batch-put` | BatchWriteItem | 25 × ~2 KB items (~50 KB)           | `{"UnprocessedItems":{}}`, 23 B |

Item content is deterministic (seeded), defined once in `BenchmarkItems` and converted to each
SDK's model types plus the wire-format JSON the server returns — so all SDKs marshal/unmarshal
structurally identical data.

### S3 streaming scenarios

| Scenario | Operation | Body |
|---|---|---|
| `get-object-8k` / `put-object-8k` | GetObject / PutObject | 8 KiB |
| `get-object-256k` / `put-object-256k` | GetObject / PutObject | 256 KiB |
| `get-object-8m` / `put-object-8m` | GetObject / PutObject | 8 MiB |

They need an `s3-*` client arm and they are **not part of `--scenario all`**, which still means the
four DynamoDB scenarios — folding them in would change the meaning of every existing collection
command. Three sizes rather than one: 8 KiB isolates per-call pipeline cost from byte-shuffling,
8 MiB is where an extra copy, hash or buffer would dominate, and 256 KiB sits where per-chunk costs
show up without the total being all payload. The size is part of the object key
(`/benchmark-bucket/objects/<bytes>`), so the server stays stateless and needs no reconfiguration
between scenarios.

Two things about these runs differ from the DynamoDB ones, and both are deliberate:

- **They run over HTTPS**, and `scripts/benchmark.sh` turns TLS on automatically for any
  `*-object-*` scenario (`--tls` / `--no-tls` override it). Not a preference: over plain HTTP with a
  request body, `ChecksumUtil.isPayloadSigning` forces payload signing on regardless of
  `PAYLOAD_SIGNING_ENABLED`, so stock V2 aws-chunk-signs a `PutObject` while the bridge refuses it
  outright (`compatability_issues.md` 13.2) — the two arms would not be doing the same work. Over
  HTTPS both send `UNSIGNED-PAYLOAD` and the raw bytes, which is what a real `PutObject` does. The
  server presents a committed self-signed key pair (`BenchmarkTls`) that the client trusts through a
  real trust manager, not a trust-all one, so certificate validation stays inside the measurement.
- **Checksums are off in both arms** (`requestChecksumCalculation`/`responseChecksumValidation` set
  to `WHEN_REQUIRED`), which is the one departure from a default `S3Client`. With them on, the
  comparison would be measuring a missing bridge feature (6.1, 12.9) at a cost that scales with
  object size, rather than measuring the streaming pipeline. Read these numbers as "the streaming
  pipeline, checksums off on both sides."

## CLI reference

### Runner options (passed through by `scripts/benchmark.sh`)

```
--client X            SDK under test (see the client table above; required)
--scenario X[,Y...]   small-get, small-put, batch-get, batch-put, or all (default: all);
                      plus get-object-{8k,256k,8m} / put-object-{8k,256k,8m}, which need an
                      s3-* client arm and are NOT included in all
--iterations N        measured operations per scenario (default: 10000)
--warmup N            unmeasured warmup operations per scenario (default: min(2000, iterations))
--concurrency N       operations kept in flight (default: 2)
--async-mode X        inflight | join (default: inflight), async clients only
--warmup-mode X       quiesce | fixed (default: quiesce)
--warmup-max-seconds N
                      ceiling on warmup wall time in quiesce mode (default: 60)
--endpoint URL        server endpoint (default: http://127.0.0.1:19080)
--metrics             collect SDK-internal metrics, print per-scenario summary to stdout
--metrics-file PATH   write metric summaries to PATH instead of stdout (implies --metrics)
--progress-seconds N  progress/ETA print interval; 0 disables (default: 10, minimum 10)
--cpu-source X        auto | oshi | procfs | mxbean (default: auto)
--append-to-results-file PATH
                      append one CSV row per RESULT line to PATH; creates the file
                      with a header row if it doesn't exist
```

### Launcher-only options (`scripts/benchmark.sh`)

```
--port N            port for the auto-launched mock server (default: 19080)
--no-server         don't launch a server (requires --endpoint)
--tls / --no-tls    serve HTTPS instead of HTTP (default: on for *-object-* scenarios, off
                    otherwise — see "S3 streaming scenarios")
--profile MODE      jfr | cpu | alloc | wall
--profile-out DIR   profiler output directory (default: ./profiles)
--jvm-args "..."    extra JVM args for the client JVM
--jar PATH          run from a shaded benchmark jar instead of the local build; skips the
                    build entirely and reads nothing from ~/.m2 (see "Reproducible jars")
```

### Full collection (`scripts/collect.sh`)

Runs the whole matrix (every client x every scenario) and writes raw data to
`pipeline_benchmark2/raw/<runid>/` (runid = `yyyymmdd-HHMM`, directory is gitignored):

```bash
./scripts/collect.sh                      # defaults: 200k iterations, 20k warmup, 3 timing reps
./scripts/collect.sh --iterations 1000 --warmup 200 --reps 1   # quick smoke collection
```

Per case (`<client>_<scenario>`, e.g. `smithy_small-get`), four kinds of isolated JVM runs:

| Kind | Output | Notes |
|------|--------|-------|
| clean timing (x reps) | rows in shared `results.csv`, `<caseid>/timing-rep<N>.log` | the only runs that feed results.csv |
| async-profiler CPU | `<caseid>/cpu.jfr`, `cpu.log` | JFR format; convert with `asprof`/`jfrconv` |
| async-profiler alloc | `<caseid>/alloc.jfr`, `alloc.log` | |
| SDK metrics | `<caseid>/metrics.txt`, `metrics.log` | |

Timing reps are interleaved (rep 1 of every case, then rep 2, ...) so machine drift spreads
across cases rather than accumulating on one client. Profiler and metrics runs are separate JVMs
because they perturb timing — never compare their RESULT lines against `results.csv`.
`manifest.md` in the run directory records the commit, environment, parameters, and the exact
command, timestamps and status of every run.

Options: `--iterations`, `--warmup`, `--warmup-max-seconds`, `--reps`, `--timing-only`, `--clients`,
`--scenarios`, `--concurrency`, `--async-mode`, `--port`, `--out`, `--jar`.

With `--jar`, no build runs at all and the jar's embedded provenance (phase, commit, SDK versions)
is recorded in `manifest.md` under *Artifact provenance* — so a collection can be reproduced from
the archived jar alone.

`--timing-only` stops after the timing phase. The cpu and alloc phases need async-profiler, which is
not installed on every benchmark host; without it they fail 2/3 of the runs and leave nothing useful
behind, so a timing-only collection is the right shape whenever the question is "which of these
clients is faster" rather than "where does the time go".

### Comparing clients within one jar (`client_sweep_summary.py`)

`paired-ab.sh` compares two *jars*. When the variable is a client-construction knob instead — the
`awssdk.bridge.strip*` properties below, or sync vs async — the comparison lives inside one jar, and
`collect.sh --timing-only` already produces the right data: it interleaves reps across clients, so rep
*k* of each client saw the same machine conditions.

```bash
./scripts/collect.sh --jar ../../pipeline_benchmark2/jars/bridge-components-<sha>-dirty.jar \
    --clients v2-sync,v2-sync-strip-endpoints,v2-sync-strip-retries \
    --scenarios small-get,small-put --iterations 60000 --warmup 40000 --reps 4 --timing-only \
    --pin-client 0,1 --pin-server 4,5,6 --out ../../pipeline_benchmark2/components
python3 ../../pipeline_benchmark2/analysis/scripts/client_sweep_summary.py \
    ../../pipeline_benchmark2/components/<runid> > .../summary.md
```

The summary pairs each client against the first one in `--clients` by rep index and reports the mean
of the per-rep ratios, the same statistic `paired_ab_summary.py` uses. It warns if the rows span more
than one artifact, since that would confound the client difference with an SDK difference.

### Paired A/B comparison (`scripts/paired-ab.sh`)

Compares two jars by alternating them inside one session, which is the right shape for judging a
change on a machine with drift. Sequential collections taken hours apart are confounded by whatever
the machine did in between — that has been the dominant error term here, with one collection's first
repetition running 5.6× slow. Alternating arms makes drift hit both nearly equally, so the
*difference* survives noise that swamps either arm's absolute number.

```bash
./scripts/paired-ab.sh --jars \
    "phase0=../../pipeline_benchmark2/jars/racecar-phase0-10f88f7bffd.jar,\
phaseD1=../../pipeline_benchmark2/jars/racecar-phaseD1-d1524d7f46d.jar" \
    --iterations 100000 --warmup 30000 --reps 5
```

The arm order reverses on even repetitions, so neither arm systematically occupies the warmer
position. Output goes to `pipeline_benchmark2/paired/<runid>/` (gitignored): `results.csv`,
`manifest.md` with both arms' full provenance, per-run logs, and `summary.md` from
`pipeline_benchmark2/analysis/scripts/paired_ab_summary.py`.

The summary's headline is the mean of the **per-repetition ratios** between arms, not the ratio of
the means, since the ratio is what pairing stabilizes. It also prints each arm's standalone spread
for comparison: when the paired spread is much tighter, pairing is doing real work and unpaired
numbers from this machine can't be trusted at that resolution. Arms are identified by the jar's
stamped `phase` label (so each jar needs a distinct one), and the summary refuses to draw conclusions
if the two jars were built from different *harness* commits.

Timing only — profiling perturbs timing, so allocation and CPU profiles stay in `collect.sh`.

### Cold start (`scripts/cold-start.sh`)

Every other mode here measures steady state, which is the measure that most flatters a pipeline with a
large class graph: loading and compiling it is paid once and then divided by 60,000 operations. For a
Lambda invocation, a CLI, or a container that serves a handful of requests, that cost is the whole
story. This mode reports it un-amortized — one JVM per data point, `--ops` operations, then exit.

```bash
./scripts/cold-start.sh --jar ../../pipeline_benchmark2/jars/bridge-components-<sha>-dirty.jar \
    --clients v2-sync,v2-sync-stripped,smithy --reps 10 --pin-client 0,1 --pin-server 4,5,6
```

Output: `pipeline_benchmark2/coldstart/<runid>/coldstart.csv` plus a per-JVM log. One mock server
serves the whole sweep, so the client under test is the only cold thing in the picture, and clients
are interleaved within each repetition.

`build_plus_first_ms` is the number to quote: client construction plus the first call, i.e. everything
from the first touch of an SDK class to the first parsed response. `jvm_to_first_response_ms` is
recorded for scale but also contains JVM startup and the harness's own readiness probe. Expect much
more run-to-run spread than a timing collection — a single scheduler hiccup lands entirely in one
measurement rather than being averaged away — which is why the default is 10 reps.

### Error and retry behavior (`scripts/error-behavior.sh`)

Not a timing mode. Retry bridging and error bridging both measure as *free* in a timing collection,
which is correct and uninformative — neither does anything on a call that succeeds. This mode supplies
the failures: it arms a fault on the mock server, makes one call, and records what the SDK threw and how
many attempts it took.

```bash
./scripts/error-behavior.sh \
    --jars baseline=../../pipeline_benchmark2/jars/bridge-errbase-published-2.46.10-dirty.jar,\
bridge=../../pipeline_benchmark2/jars/bridge-errbridge-<sha>-dirty.jar \
    --reps 2 --pin-client 0,1 --pin-server 4,5,6
```

Output: `pipeline_benchmark2/errors/<runid>/errors.csv`, one log per arm, and **`diff.md`** — the script
runs `analysis/scripts/error_behavior_diff.py` itself, so the arm comparison is never a hand count. Where a
run also has a `summary.md`, that is hand-written narrative and `diff.md` is what it cites.

`diff.md` reports **behavioral** differences separately from **message-wording** ones, and the behavioral
number is the one to quote: SDKs reword messages without changing behavior, so folding the two together
buries the rows that matter under wording churn. `--faults` takes a comma list of modes from `Faults.java`
or `all`; the no-fault control is added automatically and passing `none` is an error.

Each fault runs twice: a
**persistent** case (every attempt fails — shows the attempt ceiling, the backoff, and the final
exception) and a **transient** case (two failures then success — separates "retried and recovered" from
"gave up" from "never retried"). A fresh client per case, because v2's standard retry strategy holds a
token bucket that would otherwise make each case depend on the ones before it.

The fault catalogue is `Faults.java`, one fault per axis along which a bridged pipeline can plausibly
differ: modeled-and-retryable, unmodeled-but-retryable, retryable-from-status-only, `Retry-After`
present, modeled-and-not-retryable, modeled-by-the-service-but-not-the-operation, a 200 with truncated
JSON, a 5xx with an empty body, a body torn mid-read (`truncated-stream`), and a socket closed before any
response at all (`connection-reset`).

Those last two look like one axis and are two, which is worth knowing before pruning either. Bodies are
streamed, so the HTTP call returns once the headers arrive: a torn body fails *after* the transport and is
retried normally, while only a pre-header failure fails *inside* it — and that is the one case smithy-java
never retries (ledger 3.6). Adding `connection-reset` is what found the largest behavioral difference on
the branch, and no timing measurement would have.

Read the diff as a compatibility report, not a benchmark: `wall_ms` is only meaningful as a backoff
signal (a retried case against an unretried one), and any difference in a strictly compared field —
exception class, error code, status code, request ID, `retryable()`, `isThrottlingException()` — is a
ledger entry.

#### Measuring retry *timing*

By default `wall_ms` is not a measurement, because v2's backoff has full jitter: a throttled 3-attempt
call is uniform on roughly `[0, 3 s)`, so two arms running identical policy differ by 4x on one sample in
either direction. `--backoff` removes the jitter and `--warmup` moves connection setup and first-call
class loading (~200 ms) outside the timed region. Then
`pipeline_benchmark2/analysis/scripts/retry_cost_summary.py RUNDIR` reads the run as a timing; it refuses
a `standard`-backoff run outright, so jitter cannot be presented as a number by accident.

```bash
# does the customer's configured backoff survive bridging? (few reps: the answer is an offset, not a distribution)
./scripts/error-behavior.sh --jars ... --faults internal-error,throughput-exceeded \
    --reps 5 --backoff fixed:200 --warmup

# what does retrying cost, with no sleep in it? (many reps, µs resolution, pinned)
./scripts/error-behavior.sh --jars ... --faults internal-error,unavailable,malformed-body \
    --reps 60 --backoff immediate --warmup --pin-client 0,1 --pin-server 4,5,6
```

Two things to know before reading the output. `wall_us` exists because with `immediate` the whole quantity
is 2–3 ms, which integer milliseconds quantize into two adjacent buckets; the summary prefers it when
present. And the arm-vs-arm statistics are rank-based (`P(faster)`, bootstrap interval on the median
difference) rather than sd-based, because these samples are heavy-tailed — a single GC pause lands 20x
above the median and inflates the sd enough to hide a difference that every rep shows. Use `per extra
attempt` for cross-fault comparison, since faults retry different numbers of times.

The probe pins `RetryMode.STANDARD`, so these runs measure v2 standard backoff and say nothing about
`DynamoDbRetryPolicy`'s overrides or the deprecated `retryPolicy(...)` path (ledger 3.4).

### Standalone server

```bash
./scripts/server.sh [--jar PATH] [--port N] [--threads N]   # prints "READY port=..." when up
curl -s http://127.0.0.1:19080/stats                        # counters, CPU, Jetty pool depth
curl -s 'http://127.0.0.1:19080/faults?mode=throttling&count=2'   # arm a fault; mode=none disables
```

Useful for running the server on a separate machine/core set, or keeping one server up across
many client runs (`benchmark.sh --no-server --endpoint http://host:port`). Arming a fault also zeroes
`requests` and `faults_served`, so the attempt count for one API call can be read straight off
`/stats`.

## Reproducible jars

`scripts/build-jar.sh` produces a single self-contained, provenance-stamped uber-jar containing
both SDKs, smithy-java, the mock server and the runner. That makes the jar the unit of
measurement: one file to `scp` to a benchmark host, and one file per phase kept in an archive so
any earlier measurement can be re-run later without rebuilding the SDK.

```bash
# Rebuild + install the SDK modules, then shade everything into a phase-labelled jar
./scripts/build-jar.sh phaseD
#   built:    target/racecar-phaseD.jar
#   archived: ../../pipeline_benchmark2/jars/racecar-phaseD-<commit>[-dirty].jar

./scripts/build-jar.sh phaseD --skip-sdk-build       # use whatever is already in ~/.m2
./scripts/build-jar.sh phaseD --archive /tmp/jars    # archive somewhere else

# Run anything from the jar; no build, no ~/.m2 involvement
./scripts/benchmark.sh --jar target/racecar-phaseD.jar --client v2-sync --scenario small-get
./scripts/collect.sh   --jar target/racecar-phaseD.jar --clients v2-sync,v2-async
./scripts/server.sh    --jar target/racecar-phaseD.jar
```

The SDK modules are rebuilt by default because the benchmark resolves the SDK from `~/.m2` at
*build* time — baking a stale SDK into a phase-labelled jar is the easiest way to record a wrong
measurement. `build-jar.sh` builds a consistent module set
(`:dynamodb,:apache-client,:apache5-client,:aws-crt-client`, excluding `:codegen-maven-plugin`);
installing a single core module on its own has previously desynchronized `~/.m2` and produced
runtime `VerifyError`s.

Every jar embeds `benchmark-provenance.properties` (phase label, git commit/branch/dirty flag,
build timestamp, and the V2/V1/smithy-java versions), so a renamed or copied artifact can still be
traced to a commit:

```bash
unzip -p target/racecar-phaseD.jar benchmark-provenance.properties
```

The same values are printed in the run header (`=== build: phase=... commit=... sdkCommit=...`) and
in the `phase`, `commit` and `sdk_commit` columns of every `results.csv` row. "Dirty" means *tracked*
files differ from the recorded commit — i.e. the jar can't be rebuilt from it — and adds a `-dirty`
suffix to the archive filename. Untracked scratch files don't count, but untracked files under a
module's `src/` are called out, since those do get compiled in.

**Two commits, because they can differ.** `commit` is the harness build; `sdk_commit` is the SDK
*inside* the jar. Comparing two jars is only sound if the harness is identical, so a baseline jar is
built by installing an older SDK and then shading it with today's harness:

```bash
git checkout <baseline-sha>
mvn clean install -pl ':dynamodb,:apache-client,:apache5-client,:aws-crt-client,!:codegen-maven-plugin' \
    --am -P quick -Dmaven.test.skip=true
git checkout <working-branch>
./scripts/build-jar.sh phase0 --skip-sdk-build --sdk-commit <baseline-sha>
```

The archive filename carries `sdk_commit`, since that is the variable under test. Using
`--skip-sdk-build` *without* `--sdk-commit` records `sdk.commit=unrecorded` rather than guessing:
whatever is in `~/.m2` cannot be attributed to a revision.

Shading notes, in case the dependency tree changes: nothing is relocated (V2 already relocates its
Jackson to `software.amazon.awssdk.thirdparty.jackson`, and V1/V2 packages are disjoint), but the
`ServicesResourceTransformer` is mandatory — V2 discovers `SdkHttpService`/`SdkAsyncHttpService`
through `META-INF/services`, and without merging, HTTP client resolution fails at runtime rather
than at build time. All platform natives (including non-host ones) are kept in the jar on purpose,
so the same binary runs on a laptop and on a benchmark host for cross-checks.

## Output

Progress lines print at the configured interval — every 10 s by default, and no more often than that
even if asked, so a run's log stays a handful of lines:

```
progress small-get 98,431/200,000 (49.2%) 9843 ops/s eta 10s
```

`scripts/collect.sh` passes `--progress-seconds 0`, so its per-run logs contain only the header and
the RESULT line.

One `RESULT` line per scenario:

```
=== warmup small-get: ops=55,000 wall=6.7s jit=6826ms settled=true (compilation quiet for 3000ms)
RESULT client=v2-sync transport=apache5 scenario=small-get iterations=300000 concurrency=1 \
       async_mode=n/a wall_ms=25628 ops_per_wall_sec=11706.9 cpu_ms=11910 cpu_user_ms=10450 \
       cpu_sys_ms=1460 ops_per_cpu_sec=25188.9 ops_per_user_cpu_sec=28708.9 cpu_us_per_op=39.7 \
       app_cpu_ms=11700 app_cpu_us_per_op=39.0 unattributed_ms=210 avg_us_per_op=85.4 \
       mean_lat_us=85.4 p50_us=82.0 p90_us=110.0 p99_us=194.0 p999_us=520.0 max_us=5145.0 \
       jit_ms=12 gc_ms=32 gc_count=41 steady_state=true warmup_ops=55000 warmup_settled=true \
       server_cpu_ms=13050 server_requests=300000
```

- `ops_per_wall_sec` — iterations / elapsed wall clock.
- `ops_per_cpu_sec` — iterations / **whole-process** CPU seconds (user + system, all threads
  including event loops, GC and JIT), which is the right measure when SDKs differ in thread
  usage. `ops_per_user_cpu_sec` uses user time only.
- CPU time is read through the `CpuTimeSource` interface; exactly one implementation is probed
  and bound at startup (`--cpu-source`, default `auto` = first available of):
  - `oshi` — [OSHI](https://github.com/oshi/oshi) `OSProcess` user/kernel time. User/system
    split on both Linux (`/proc/self/stat` with the real `USER_HZ`) and macOS (`proc_pidinfo`),
    millisecond resolution.
  - `procfs` — direct `/proc/self/stat` parse (Linux only). Useful to cross-check oshi:
    `--cpu-source procfs` on the same workload should agree with the default.
  - `mxbean` — `OperatingSystemMXBean.getProcessCpuTime()`. Total only, no user/system split
    (`ops_per_user_cpu_sec` and the split fields are omitted).

  The bound source and whether it provides the split are printed in the run header. Forcing an
  unavailable source (e.g. `procfs` on macOS) fails fast at startup.

With `--append-to-results-file PATH`, each RESULT line is also appended to a CSV file (columns
mirror the RESULT fields, plus `phase`, `commit` and `sdk_commit` from the build provenance; the
header row is written when the file is first created). Runs with a no-split CPU source leave the
`cpu_user_ms`/`cpu_sys_ms`/`ops_per_user_cpu_sec` cells empty.

The run header also identifies the artifact under test, so a stray log file can be traced back to a
build:

```
=== build: phase=phaseD1 commit=d8cbc5ac789 sdkCommit=d8cbc5ac789 branch=feature/poc/racecar \
    dirty=false built=2026-08-28T17:18:32Z sdkV2=2.54.4-SNAPSHOT sdkV1=1.12.797 smithy=1.5.1
```

With `--metrics`, each SDK's native metric facility reports per-phase timings as `METRIC` lines
(V2 `MetricPublisher` CoreMetrics, V1 `AWSRequestMetrics`, smithy-java OTel
`OperationMetricsPlugin`). Metrics collection itself costs a little per call, so leave it off for
profiling runs.

## Profiling

```bash
# JFR (built into the JDK)
./scripts/benchmark.sh --client v2-sync --scenario small-get --iterations 100000 --profile jfr

# async-profiler: CPU, allocation, or wall-clock flame graphs (HTML)
./scripts/benchmark.sh --client smithy --scenario batch-put --iterations 100000 --profile cpu
./scripts/benchmark.sh --client v1 --scenario small-put --iterations 100000 --profile alloc
```

Output lands in `./profiles/<client>-[mode-]<timestamp>.{jfr,html}`. Because the server is a
separate process, profiles contain only client-side work. Profile one client and one scenario per
run for clean recordings. For anything the flags don't cover, attach manually: the run header
prints the client `pid`, and `--jvm-args` accepts arbitrary JVM options.

Inspect JFR recordings with `jfr print`, JDK Mission Control, or IntelliJ.

## Implementation details, assumptions, caveats

- **Closed-loop.** Operations are issued back-to-back with no think time, at whatever concurrency is
  configured (default 1, i.e. one at a time). This measures pipeline cost and closed-loop throughput,
  not behavior under an open-loop arrival process.
- **Request objects are prebuilt** once per workload and reused every iteration (identical policy
  for all SDKs). Marshalling model → wire bytes still happens per call in every SDK; request
  *builder* allocation does not.
- **Retry parity, not retry absence.** All clients run standard token-bucket retries capped at 3
  attempts (see table above). Since the server never fails, the measured cost is the per-call
  accounting only.
- **No raw payload wrappers.** DynamoDB has no `RequestBody`-style streaming input, so fairness
  issue #2 from the previous round doesn't arise. Every SDK serializes the same item graph per
  call; per-SDK differences in serializer allocation behavior are part of the measurement, by
  design.
- **The server doesn't parse requests.** It reads and discards the body, routes on the
  `X-Amz-Target` header, and writes precomputed bytes. Malformed-but-signed requests would still
  get a 200; use a DynamoDB Local run if you need a correctness check. Unknown targets get a 400
  so misrouted calls fail loudly.
- **No `x-amz-crc32` response header.** V1 validates it when present but V2/smithy-java do not,
  so omitting it keeps response handling symmetric.
- **Nothing prints from inside the measured loop, ever.** Progress is reported by a separate thread
  reading worker-local counters, so it costs the measured path nothing, and `--progress-seconds 0`
  (what `scripts/collect.sh` passes) turns it off entirely. This used to be an in-loop clock check,
  and a sentinel-overflow bug made it print once per operation: a formatted line plus an auto-flushed
  write, tens of microseconds, the same order as a whole small-get. The interval also has a 10 s
  floor now — a full collection is hundreds of JVM invocations, and anything printed every few
  seconds across all of them adds up to log files nobody reads.
- **Use `app_cpu_us_per_op`, not `cpu_us_per_op`.** `cpu_ms` is **whole-process** CPU: it includes the
  C1/C2 compiler threads, the VM thread and GC. That cost is roughly fixed per JVM rather than per
  operation, so dividing it by the operation count produces a number that shrinks as the window grows
  instead of converging. On an unchanged client (`v2-sync`/`small-get`, concurrency 1, fixed 20k
  warmup) it fell by a factor of 2.3 for no reason but a longer window:

  | iterations | `mean_lat_us` | `cpu_us_per_op` | client cores |
  |-----------:|--------------:|----------------:|-------------:|
  | 40,000  | 94.5 µs | 114.1 | 1.21 |
  | 100,000 | 89.9 µs | 77.4  | 0.86 |
  | 300,000 | 84.3 µs | 48.5  | 0.57 |

  A single-threaded blocking client reporting 1.21 cores is the tell — that CPU cannot be the caller
  thread. This distorted two real comparisons badly enough that both vanished when re-measured at a
  longer window, one reversing sign.

  `app_cpu_ms` fixes it by summing per-thread CPU over Java threads only (`ThreadMXBean`), which
  excludes compiler, VM and GC threads by construction. Combined with quiescence warmup, the same
  experiment now holds still:

  | iterations | `cpu_us_per_op` | `app_cpu_us_per_op` | `jit_ms` in window |
  |-----------:|----------------:|--------------------:|-------------------:|
  | 40,000  | 41.1 | 39.5 | 14 |
  | 100,000 | 41.0 | 39.5 | 9  |
  | 300,000 | 39.7 | 39.0 | 12 |

  Spread across window lengths: **1.3%** for `app_cpu_us_per_op`, down from 135%.

  One accounting subtlety worth knowing: a thread's CPU vanishes when the thread dies, so every
  thread the harness creates folds its total into a retired counter on the way out. Threads created
  *and* destroyed by the SDK inside a window are the remaining gap, which is why `unattributed_cpu_ms`
  (process minus application) is reported rather than assumed to be zero.

  The procfs source assumes 100 ticks/s (override with `--jvm-args "-DclkTck=N"`).
- **Warmup runs until the JIT settles** (`--warmup-mode quiesce`, the default). A fixed count is a
  guess, and 20k was the wrong guess here: warmup actually needs 55k–315k operations and 6–10 s
  depending on client, during which the JVM spends 5–7 s compiling. Quiesce mode runs `--warmup`
  operations and then keeps going in chunks until total compilation time stops growing for 3 s,
  capped by `--warmup-max-seconds`. `--warmup-mode fixed` reproduces older collections.

  The gate is evidence, not proof — compilation tails off in bursts, so the runner independently
  checks how much compiling happened *inside* the measured window and reports `steady_state`, warning
  when it is false. The async client at concurrency 4+ still compiles ~400–550 ms per window and is
  correctly flagged; treat per-operation CPU from a `steady_state=false` run as unusable. Short windows
  are disproportionately affected, so the warning suggests raising `--iterations` below 5 s.
- **Server-side accounting.** `MockDdbServer` exposes `GET /stats` (`key=value` lines: request and
  error counts, its own CPU, and Jetty pool depth). The runner samples it either side of the measured
  window and reports `server_cpu_ms`, `server_requests` and a saturation flag. `server_requests` is a
  cross-check that the server served exactly the operations the client thinks it issued — a mismatch
  means retries, dropped connections, or a stray second client. It earns its keep: it is what revealed
  that an experiment had run against a stale server from an older build. Pool size is `--threads N`
  (default 200). Remember the saturation flag only covers the handler pool, not the socket layer — see
  the ceiling discussion under Concurrency.
- **`benchmark.sh` verifies the server is *its own*.** Readiness comes from the launched child's
  `READY` line, not from `/ping` answering, because a leftover `MockDdbServer` on the same port answers
  `/ping` instantly while the new one dies with "Address already in use". That is not hypothetical: a
  server from a two-hour-old jar served an entire experiment before the request-count mismatch exposed
  it. A port conflict now fails in about a second with the `lsof` command to find the squatter.
- **Localhost transport.** All traffic is plain HTTP over loopback: no DNS, TLS, or real network.
  Numbers are pipeline overhead, not end-to-end AWS latency. Client and server share the host's
  cores; on a small machine consider running the server on separate cores (`taskset`/separate
  host with `--no-server --endpoint`).
- **Dependency pins.** V2 uses the local `-SNAPSHOT` version from the repo (the `<aws.sdk.v2.version>`
  property in the benchmark pom must match the repo root's `<version>`; `scripts/collect.sh` patches
  it automatically). smithy-java 1.5.1 from Maven Central; V1 via
  `com.amazonaws:aws-java-sdk-dynamodb:1.12.797`, which pulls `aws-java-sdk-core` transitively.
  (This used to be the monolithic `aws-java-sdk` artifact "as customers use it", but the benchmark
  only touches `dynamodbv2` plus core, so runtime behavior is identical and the leaner tree is what
  makes shading practical.) The smithy-java client comes from mavenLocal (see prerequisites). Netty
  is pinned to 4.2.0.Final for smithy-java compatibility.
- **Two ways to run.** `package` produces both a shaded uber-jar (`target/racecar-<phase>.jar`, see
  "Reproducible jars") and the resolved runtime classpath in `target/classpath.txt`. The scripts
  default to the classpath and switch to the jar with `--jar`; the classpath path is kept as the
  reference to validate the shaded jar against. Run classes manually with
  `java -cp "target/classes:$(cat target/classpath.txt)" ...`.
- **JDK 24+ warnings.** Netty (pulled in by smithy-java) triggers a `sun.misc.Unsafe` deprecation
  warning; harmless. `--enable-native-access=ALL-UNNAMED` is always passed for the CRT client and
  async-profiler.
- **This module is not part of the repo's Maven reactor** — it benchmarks released artifacts, not
  the working tree.
