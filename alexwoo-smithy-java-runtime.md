# Re-hosting the AWS SDK for Java v2 on smithy-java: prototype report

**Status:** prototype complete for its stated scope; report current as of 2026-09-30 (async client; schema traits and request overrides, ledger §17).
**Branch:** `smithy-java-bridge-alexwoo-full`.
**Versions:** smithy-java 1.6.1 / smithy 1.73.0 / AWS SDK for Java v2 2.46.11-SNAPSHOT, measured against
published v2 2.46.10.

This is the single report for the prototype: what was built, what it costs, and where it cannot reproduce
v2's behavior. It summarizes and links to the three detailed documents rather than replacing them —
`compatability_issues.md` (the 95-entry ledger), `pipeline_benchmark2/RESULTS.md` (the measurements), and
`test/standalone-e2e-benchmarks/README.md` (how to reproduce them).

It is a companion to `RFC-smithy-java-runtime.md`, which is the *proposal* and is left as originally
written. Where the prototype's evidence bears on that proposal — including one place where it changes the
argument — this document says so explicitly (§4).

---

## 1. The six findings that matter

1. **Performance is not the risk. The bridged pipeline is 19-43% cheaper in app CPU than stock v2's own
   pipeline**, across four DynamoDB scenarios, 4/4 paired wins each, against a noise floor of ±3-4%. The
   earlier full-bridge POC was 2-3x *slower*; bridging a minimal set of components and letting smithy-java
   own serde, signing and the call pipeline reverses the sign. Native smithy-java is faster still (−45% to
   −56%), so the remaining gap is the v2 veneer, not the bridges.

2. **The v2 API surface costs about three times what the bridges cost.** Of the ~80 µs between stock v2
   and native smithy on a small operation, ~5-6% of the bridged client is component bridging and roughly
   three times that is v2 model objects plus the `SdkPojo` serde adapters. Optimizing the bridges further
   has little left to give; the veneer is where the remaining gap lives.

3. **Every compatibility gap caused by dropped metadata or unwired configuration turned out to be
   addressable** — checksums, trailers, chunked signing, host prefixes, idempotency tokens, error mapping,
   response validation, every request- and client-level override, both timeouts — almost always by running
   v2's *own* code (its signer, its checksum rules, its interceptors) where smithy-java would run its
   equivalent. The fault sweep went from 19 behavioral differences to **1** (a Jackson 2-vs-3 cause type).
   Even the most severe divergence, **transport failures are never retried** (smithy-java decides retries
   inside `deserialize`, so a failure thrown from the transport never reaches it — a native smithy-java
   client has the same bug), is fixed in the bridge by deferring the failure into the attempt; the upstream
   fix is still the right one (ledger 3.6, §17).

4. **The prototype derives smithy `Schema`s from C2J-generated `SdkField`s, which is exactly the path the
   RFC argues is unsafe — and the prototype's own bug history is the strongest available evidence that the
   RFC is right.** Five separate silent wire-corruption bugs (§12.1-12.5) were each a trait C2J had
   flattened away and the translator had to guess. See §4; this is the most consequential thing in this
   report.

4b. **That fidelity has a price: about 20 µs per call.** Signing with v2's signer (needed for S3's checksums
   and signing rules) takes the sync small-operation win from −36% to about −20% against stock, and async
   small operations from −12% to parity. `stripSigner` recovers all of it; a hybrid that uses smithy's
   signer where v2's adds nothing would recover it for DynamoDB, pending a signature-equivalence check
   (`RESULTS.md`, "Fidelity cost").

5. **The async client works on a synchronous smithy-java, and is cheaper than stock async too — by less
   than sync.** One virtual thread per call, parked on response headers, over Netty and CRT: 0 carrier
   pins, byte-identical to stock async on the diffed operations, multipart unmodified through the public
   builder. At concurrency 1 it is 4-42% cheaper in app CPU and 10-41% faster, 4/4 in every cell — roughly
   half the sync client's margin on small operations. It also found a smithy-java bug that breaks every
   publisher-backed response (ledger 15.6), and it costs one real compatibility property: thread-bound
   context no longer reaches interceptors (16.1).

6. **Cold start is the one loss, and it is entirely client construction** (+95 ms, +22%). The bridged
   client's *first call* is already 20% faster than stock v2's. Two pipelines' class graphs get loaded, and
   114 of the extra classes come from the Smithy **model** library, pulled onto the runtime path by
   generated `SCHEMAS` static initializers.

---

## 2. What was built

Working v2 sync **and async** clients whose entire execution path is smithy-java, for two services and two
protocols.

```
   v2 public API        DynamoDbClient / S3Client and their *AsyncClient twins, builders, POJOs,
   (unchanged)          modeled exceptions, ExecutionInterceptor, ClientOverrideConfiguration
        │
        │  ~5,600 lines, core/smithy-java-bridge, 29 classes
        ▼
   the bridge           serde (SdkField→Schema, SdkPojo↔smithy struct)   ·  config translation
                        transport · identity · endpoints · retry · errors · interceptors · streaming
                        async envelope (one virtual thread per call, parked until response headers)
        │
        ▼
   smithy-java 1.6.1    ClientPipeline · ClientInterceptor · schema-driven JSON/XML codecs
   (the guts)           AwsJson1Protocol / RestXmlClientProtocol · SigV4 · rules engine · retries
```

**Gated per service** on the `generateSmithyJavaSerde` customization flag, currently on for **DynamoDB**
(awsJson1_0) and **S3** (rest-xml). `SyncClientClass` and `AsyncClientClass` both consult it; the async
client keeps event-stream operations on the stock pipeline (S3 `SelectObjectContent`, ledger 16.6), so
105 of S3's 106 async operations and all of DynamoDB's run on smithy-java.

| Bridge component | Status | Ledger |
|---|---|---|
| serde — `SdkField` → smithy `Schema`, `SdkPojo` ↔ `SerializableStruct` | works, C2J-derived (§4) | 1.2, 11.1, 12.x |
| transport — v2 `SdkHttpClient` ⇄ smithy `ClientTransport`, both directions | `BRIDGED` | 3.6, 10.6 |
| async transport — v2 `SdkAsyncHttpClient` (Netty, CRT) → smithy `ClientTransport`, returns on headers | `BRIDGED`, bodies stay reactive | 15.x |
| async envelope — blocking `Client#call` on a virtual thread → v2-shaped `CompletableFuture` | works; 0 carrier pins measured | 16.x |
| config — `SdkClientConfiguration` read once at construction | `TRANSLATED`, 8 options honored, ~15 ignored | 9 |
| identity/auth — `AwsCredentialsProvider` → `IdentityResolver`; SigV4 | `BRIDGED` / `TRANSLATED` | 4.x |
| endpoints — v2 `EndpointProvider` driven from smithy | `BRIDGED`, re-resolved per attempt | 5.1, 3.3 |
| retry — v2 strategy wrapped via `aws-sdkv2-retries` | `BRIDGED`, classification recomputed | 3.1, 3.2 |
| errors — modeled + unmodeled enrichment through a `ModeledException` shim | `BRIDGED`, one `BLOCKED` root cause | 1.1, 1.3 |
| interceptors — v2 `ExecutionInterceptor` ⇄ smithy `ClientInterceptor` | `BRIDGED` lazily; 6 of 20 hooks | 2.1 |
| streaming — bodies travel beside the shape in a per-call override config; async bodies via `FlowAdapters` | works, no buffering either way | 13.1, 16.4 |

Two things in `test/` exist only to make claims checkable, and are not part of the SDK:
`test/standalone-e2e-benchmarks` (the A/B harness, mock server, fault injector) and `test/wire-diff` (the
byte-diff and behavioral suites, including the `SyncBackedS3AsyncClient` façade that multipart ran on
before the async client existed — kept as the record of what a sync-backed façade costs, ledger §14).

---

## 3. Performance

Full detail, raw data pointers and provenance: `pipeline_benchmark2/RESULTS.md`.

Conditions: `dev-dsk-alexwoo-2b-ee3cc828`, Xeon 8124M @ 3.0 GHz, 8 cores, JDK 21.0.9, concurrency 1,
loopback mock server, client and server pinned to disjoint CPUs. Both arms carry the **same harness
commit**, so the SDK is the only difference; arms alternate within each rep and reverse on even reps. Two
null experiments put the paired noise floor at **±3-4%**.

### Steady state, DynamoDB — app CPU µs/op

| arm | small-get | small-put | batch-get | batch-put |
|---|---:|---:|---:|---:|
| stock v2 2.46.10 | 136.6 | 132.3 | 580.3 | 749.6 |
| **v2 API on smithy pipeline** | **92.3 (−32%)** | **87.8 (−34%)** | **469.8 (−19%)** | **424.6 (−43%)** |
| same, bridges stripped | 82.1 (−40%) | 79.5 (−40%) | 464.4 (−20%) | 396.9 (−47%) |
| native smithy-java client | 60.0 (−56%) | 59.7 (−55%) | 319.9 (−45%) | 335.2 (−55%) |

Mean latency agrees throughout (−27%/−27%/−18%/−42%). These numbers **predate** the inert-interceptor fix
below, which took another 7-8% off the bridged row on small ops; they are left as measured because the
conservative statement is the honest one.

The batch asymmetry is worth chasing: batch-**put** (serialization-heavy) is −43% while batch-**get**
(deserialization-heavy) is only −19%, which points at `SdkPojoDeserializer`/`BridgeStruct` — building v2
model objects out of smithy's deserializer — as the most expensive bridge on large payloads.

### Where the bridging tax went

A per-component sweep (one jar, six clients, each removing exactly one bridge) found the **interceptor
bridge was the most expensive** at 5.9-8.4% — which was a surprise, since it was designed to be inert.
`AwsDefaultClientBuilder.awsInterceptors()` adds three interceptors to *every* AWS client, and one of them
implements only `modifyException`, a hook the bridge never invokes, so it could not have had an effect at
all. Filtering interceptors that override none of the six bridged hooks recovered **all** of it, verified
with `-verbose:class`: `V2InterceptorBridge` is now not even class-loaded on a default DynamoDB client.

Remaining bridging tax on a small operation: **~5-6% of app CPU**, down from ~14%. The endpoint bridge is
now the largest single piece.

That finding generalizes past the prototype: those three interceptors run on stock v2 too, doing nothing.
The bridge only made them expensive enough to notice.

### S3 streaming — the win is a fixed offset

| scenario | stock v2 | bridged | delta | absolute saving | wins |
|---|---:|---:|---:|---:|---:|
| `get-object-8k` | 164.5 | 110.0 | −33.1% | 54.5 µs | 5/5 |
| `put-object-8k` | 174.0 | 118.8 | −31.7% | 55.2 µs | 5/5 |
| `get-object-256k` | 462.6 | 414.2 | −10.5% | 48.4 µs | 5/5 |
| `put-object-256k` | 567.9 | 499.0 | −12.1% | 68.9 µs | 5/5 |
| `get-object-8m` | 10,892.2 | 10,795.0 | −0.9% | 97 µs | 3/5 |
| `put-object-8m` | 12,989.9 | 12,614.1 | −2.9% | 376 µs | 5/5 |

**Read the saving column, not the percentage.** `GetObject` is ~50 µs/call cheaper regardless of size —
the same fixed amount saved on a DynamoDB `GetItem` (136.6 → 92.3 = 44 µs). The percentage collapses
because the denominator grows; `get-object-8m` is honest **parity**, which is the correct answer when both
arms are moving 8 MiB over TLS at ~700 MB/s. `PutObject` additionally gains ~0.04 µs/KiB, the only
measured effect on this branch that scales with payload size; the cause is a hypothesis (one fewer buffer
touch), and an allocation profile would settle it.

Forced conditions: HTTPS only (ledger 13.2 makes an HTTP `PutObject` incomparable) and checksums off on
both sides (13.3, 6.1). So this is the streaming pipeline with checksums off, not a default `S3Client`.

### Async — cheaper than stock async, by less than sync

Collections `paired-async-c1/merged`, `paired-async-c16/merged`; same conditions and same harness commit
in both arms; `v2-sync` re-measured as a control and reproduces the table above to within 4 points.

| concurrency 1, app CPU/op | small-get | small-put | batch-get | batch-put |
|---|---:|---:|---:|---:|
| async, CRT | −11.5% | −13.6% | −4.9% | −41.5% |
| async, Netty | −6.2% | −5.8% | −4.5% | −30.6% |
| sync (control) | −35.9% | −36.6% | −18.7% | −46.9% |

Latency moves the same way (async −10% to −41%), 4/4 paired wins in every cell. At concurrency 16 the
async client is −7% to −65% in wall time per op, with one CPU loss — Netty batch-get, +4.9%, 0/4. The
large concurrency-16 wins are partly the harness: its async driver submits from one thread, and stock
v2 marshals on the caller's thread, so stock never reaches 16 in flight (~1.3 on batch-put, by Little's
law). Moving that work off the caller is the same change that costs context propagation (ledger 16.1).
Why async keeps only half of sync's saving is not profiled; the suspects, all bridge-side, are listed in
`RESULTS.md`.

### Cold start — the one loss

| | baseline v2 | bridged | delta | wins |
|---|---:|---:|---:|---:|
| client construction (ms) | 507 | 603 | **+22.3%** | 0/10 |
| first call (ms) | 192 | 155 | −20.5% | 9/10 |
| **build + first call (ms)** | **693** | **757** | **+10.1%** | 0/10 |

All of the penalty is construction; the first call is already faster. With bridges stripped the net is
**+0.4%, break-even**; native smithy is 13% *below* stock v2. So this is the cost of initializing two
builder chains, not something inherent to smithy-java. For a Lambda making a handful of calls the 64 ms is
real; calls 2-5 in the same JVM are already faster on the bridge (2.9 ms vs 4.6 ms by call 5).

The class-load diff (4,534 vs 4,354) locates it, and turns up something worth fixing on its own: **114
classes from the Smithy *model* library** on the runtime path, pulled in because generated `SCHEMAS`
constants build `ShapeId`s and trait objects in static initializers (ledger 10.7).

### Retry cost

With jitter removed, the configured backoff **survives bridging exactly** — 203.0 ms per extra attempt in
every arm and every case. But the bridge's advantage is front-loaded: it saves ~441 µs once per execution
and only ~97 µs per attempt, so **about a fifth of its per-call advantage recurs on a retry** and its
relative advantage decays with attempt count. Leading candidate is the endpoint bridge running the V2
rules engine per *attempt* rather than per execution (ledger 3.3); memoization is the untried fix.

### What the timings could not measure

Every number above is from a call that succeeded, which makes the retry strategy and the error enricher
look free — true and useless, since neither does anything on a success. A fault-injecting sweep (11 faults
× persistent/transient variants, attempt counts read from the server's own counter) closed that gap and
was the highest-yield measurement on the branch:

**87 behavioral differences on the first sweep, down to 2 after six rounds of fix-and-remeasure.** The
first sweep found a client that looked fine and did not retry server errors at all. The last two rounds
found finding #3 above (ledger 3.6), which required two faults that look redundant — a torn body and a
connection reset — to separate the retried case from the unretried one.

The 2 that remain are both `connection-reset`, i.e. 3.6, which is upstream. The other differences are
metadata and wording: `rawResponse()` never populated (10 cases), the `(SDK Attempt Count: N)` suffix
absent (14), `ConditionalCheckFailedException` arriving as `DynamoDbException` (2). Cosmetic for control
flow — but they will fail customer tests that assert on exception messages.

---

## 4. Where the prototype diverges from the RFC — and what that implies

RFC §2 argues, correctly and at length, that **C2J cannot drive a schema-based runtime**: C2J is a lossy
down-projection of Smithy, a schema-driven codec has no per-shape escape hatch, so any trait the converter
guesses wrong is silently wire-incorrect output. Its conclusion is to adopt canonical Smithy models and
delete C2J.

**The prototype does the opposite.** `SdkSchemaFactory` builds smithy `Schema`s at runtime from v2's
C2J-generated `SdkField`s, reconstructing `@httpLabel`, `@httpQuery`, `@httpHeader`, `@httpPayload`,
`@httpPrefixHeaders`, `@jsonName`, `@xmlName`, `@xmlFlattened` and `@timestampFormat` from v2's
`LocationTrait`/`MarshallingType` metadata. This was the right call for a prototype — it needs no model
pipeline and no codegen replacement — but it means the prototype is **not** a scaled-down version of the
RFC's architecture. It is the architecture the RFC rejects.

That makes the prototype an unintentionally good experiment, because it ran the rejected design for two
protocols and recorded what broke. Every one of the five silent wire-corruption bugs in ledger §12 is a
trait C2J flattened away and the translator guessed wrong:

| Ledger | The guess that was wrong | What reached the wire |
|---|---|---|
| 12.1 | `@xmlFlattened` on a list | a spurious `<member>` wrapper, in both directions |
| 12.2 | `@xmlName` on list/map elements | smithy's defaults instead of the modeled names |
| 12.3 | synthetic nested shape IDs | internal `ShapeId`s as XML element names |
| 12.4 | a codegen member rename | an unbound URI label |
| 12.5 | `@httpPrefixHeaders` | header maps dropped entirely |

All five were found by a **byte-level diff against a stock build**, not by a test suite. Four were
`BLOCKED`-class: the request was simply wrong. Two more remain open for the same reason (12.6 — a
customization-injected member is indistinguishable from a modeled one in `SDK_FIELDS`; 12.13 —
`@xmlNamespace`).

So: five silent wire bugs in two services, all in the trait-fidelity layer, all requiring byte-diff to
find. Extrapolating to 426 services is exactly the "unacceptable silent failure surface" the RFC
predicted. **The prototype's bug history should be cited as evidence in RFC §2.2, and the C2J-derived
schema path should not be carried forward past the prototype.**

Three other RFC positions the prototype can now speak to:

- **§10.4, "interceptors likely need a two-way bridge, direction TBD"** — resolved: a one-way bridge (v2
  `ExecutionInterceptor` → smithy `ClientInterceptor`), lazily installed, covering 6 of smithy's 20 hooks.
  `modifyException` is never invoked, which is why S3's `ExceptionTranslationInterceptor` cannot work
  (ledger 2.1).
- **§10.7, "shim over smithy-java's transport and move on"** — done and bidirectional, and it produced
  finding #3. The transport boundary is where the retry model breaks.
- **§10.8, "`services-custom/` out of scope"** — partially reversed. S3 multipart runs **unmodified** on
  the bridged async client through the public `multipartEnabled(true)` builder, over Netty and CRT, 20 MiB
  in 4 parts reassembled byte-exactly in both directions (ledger §16). Multipart is not a porting problem;
  it inherits whatever the async client does.

---

## 5. Compatibility

Full detail: `compatability_issues.md`, 95 entries across 17 sections, each recording what a customer
would observe rather than the internal difference. The status counts below predate §17, which resolved or
partly resolved 22 entries — every `MISSING` entry about checksums, host prefixes, request overrides and
timeouts, and the `BLOCKED` 3.6 — and added two (3.7, 16.8) plus its own eleven-part write-up. §17.11 is the
current list of what remains and whether each is a real limit.

| Status | Count | Meaning |
|---|---:|---|
| `MISSING` | 23 | v2 feature has no effect at all on this path |
| `DEGRADED` | 18 | works, observably differs |
| fixed / resolved | 12 | was blocking or missing, now correct |
| `BRIDGED` | 5 | v2 component adapted and driven by smithy |
| `BLOCKED` | 3 | cannot be represented without an upstream change |
| `DIFFERENT` | 3 | a deliberate behavioral difference, neither better nor worse by itself (async, §15-16) |
| same as stock | 3 | investigated and found to match stock v2, including one former `DEGRADED` (14.3) |
| `TRANSLATED` | 2 | v2 config read once and mapped to a smithy equivalent |
| `FRAGILE` | 2 | matches today, but rests on a coincidence nothing enforces |
| `IMPROVED` | 1 | differs from stock, in the caller's favor (16.4) |

The async work moved four §14 entries: 14.1, 14.2 and 14.4 are resolved, and 14.3 turned out to be stock
behavior that the façade had reproduced faithfully — `AsyncRequestBody.split` makes one-shot parts in
stock v2 too — so it was never a bridge degradation.

### The three `BLOCKED` entries

- **3.6 — transport failures are never retried.** Finding #3. Affects native smithy-java. The fix is in
  `ClientPipeline`, not in a bridge; marking the exception retryable was implemented and measured to have
  no effect.
- **1.1 — v2 modeled exceptions cannot be smithy `ModeledException`s.** Java has no multiple inheritance
  and `ModeledException` is an abstract class, while every v2 error already extends `SdkException`.
  Worked around with a wrapper shim; the residual is that anything inspecting error types *inside* the
  pipeline sees the shim.
- **10.1 — Java baseline.** smithy-java mandates JDK 21; already resolved as RFC Phase 0.

### Themes, for scoping rather than triage

- **Nothing in the observability surface works.** All of v2's metrics (7.1), business metrics / user-agent
  feature IDs (7.2), `requestId`/`rawResponse` on responses and errors (1.4). Part of the CPU win is this
  work not being done — quantified in `RESULTS.md` caveat 4, and it is a small part.
- **Both timeouts are absent** (8.1, 8.2). `apiCallTimeout` has no smithy concept at all; whether the
  bridge owns it or smithy gains one is open question 3.
- **Checksums, compression and content encoding are absent** (6.1-6.3, 12.9, 13.3) — and 12.9 is the one
  that makes operations outright *fail*, not merely differ, since S3 requires checksums on some.
- **~15 configuration options are silently ignored** (§9), of which the sharpest is
  `overrideConfiguration().retryPolicy(...)` being **silently downgraded** to smithy's default strategy
  (3.4). Silent is the problem, not the gap.
- **The interceptor surface is 6 of 20 hooks** (2.1), plus `SdkPlugin` (2.2), request-level override
  config (2.3) and `executionAttributes()` (2.4) doing nothing.
- **Paginators, waiters and utilities are not generated** (10.5).
- **Async: thread-bound context does not follow the call** (16.1). Stock v2 async runs `beforeExecution`,
  `modifyRequest`, marshalling and credential resolution on the *caller's* thread; the bridge runs them on
  the call's virtual thread. The caller no longer blocks on a slow credentials provider — but an
  interceptor that reads an MDC, tracing or security `ThreadLocal` sees an empty one. Cancellation reaches
  the future and not the exchange (16.5).
- **Five smithy-java defects found, all worked around in the bridge and worth filing:** transport failures
  never retried (3.6); an interceptor that doesn't override the error-path hook silently disables every one
  after it (3.7); a publisher-backed `DataStream` cannot be read with `asByteBuffer()` (15.6); the one-shot-body
  retry guard only sees modeled stream members (16.4); `RequestOverrideConfig.toBuilder()` drops the context
  (16.8).

### Two `FRAGILE` entries worth a guard

1.7 — the error registry is keyed on shape ID, so any codegen rename silently disables modeled-error
deserialization. 3.5 — v2 classifies a torn response body by its Jackson exception type, which changed in
Jackson 3. Both match today by coincidence and will regress without noise.

---

## 6. What is verified, and how

Four independent mechanisms, deliberately: each catches a class the others structurally cannot.

| Mechanism | Where | Scale | Catches |
|---|---|---|---|
| Byte diff vs stock build | `test/wire-diff/S3WireDiffTest` | 9 S3 operations, one parameterized case each | silent wire corruption — found all five §12 bugs |
| Behavioral streaming tests | `test/wire-diff/S3StreamingTest` | 7 tests, 1 GiB under a 256 MiB heap | buffering, truncation, reordering — a byte diff of a 38-byte body cannot |
| Multipart tests (façade) | `test/wire-diff/S3MultipartTest` | 7 tests, 20 MiB / 4 parts | reassembly, part loss, façade caveats |
| Async byte diff vs **stock async** | `test/wire-diff/S3AsyncWireDiffTest` | the same 9 operations via `S3AsyncClient`, goldens from published 2.46.10 | async-path wire corruption; 8 identical, 1 ledgered skip |
| Async clients over real transports | `test/wire-diff/BridgedAsyncClientsTest` | 19 tests, **Netty and CRT** each, public builders only, S3 over HTTPS | streaming both ways, multipart via `multipartEnabled(true)`, error and future shape, part-retry rules, DynamoDB |
| Async transport | `test/wire-diff/V2AsyncTransportBridgeTest` | 18 tests, Netty and CRT | returns on headers, 200 concurrent calls, framing, the 15.6 workaround |
| Carrier pinning | JFR `jdk.VirtualThreadPinned`, 0 ms threshold | 458 virtual threads over the async suites | a `synchronized` block parking an envelope — **0 found** |
| Fault injection | `standalone-e2e-benchmarks` + `error-behavior.sh` | 11 faults × 2 variants | retry and error behavior — 87 → 2 differences |
| Paired A/B benchmark | `standalone-e2e-benchmarks` | 10 scenarios, 4 clients + strip arms | cost, with null experiments bounding the noise floor |

Two methodology points that earned their keep and would be worth keeping in a real port:

- **Every benchmark claim is paired and carries a null experiment.** The `smithy` arm is identical code in
  both jars; `v2-sync-stripped` on the baseline jar is the same client as `v2-sync`. Those two put the
  noise floor at ±3-4% *by construction* rather than by assertion, which is what lets a −32% claim stand.
- **The fault catalogue needed apparently-redundant faults.** A torn body and a connection reset look like
  the same test. One is retried and one is not, and only having both revealed finding #3.

Currently green: 70/70 in `test/wire-diff` (2 skipped: the same named known difference, `copy-object`, in
the sync and async diffs). The wire-diff suite runs
against the locally-installed bridged SDK, with a guard test that fails if it ever resolves a published
one instead.

---

## 7. Scope boundaries — what this prototype does not cover

- **Async is built, but on a synchronous smithy-java.** The async clients park a virtual thread per call
  because smithy-java 1.6.1 has no async API. That is cheap (bodies stay on the event loops, 0 pins
  measured) but it is a bridge-side design, not smithy-java's; an upstream `callAsync` would change it.
  `S3TransferManager` was not exercised, though it is built on the same `S3AsyncClient` that multipart
  now runs on.
- **Two services, two protocols.** DynamoDB (awsJson1_0) and S3 (rest-xml). restJson1, awsQuery and
  rpcv2 are untouched.
- **Multipart works through the public builder.** v2's `MultipartS3AsyncClient` runs **unmodified** on
  the bridged async client, and a 20 MiB 4-part upload and download reassemble byte-exactly over Netty and
  CRT. A retryable 500 on one part still fails the whole upload, as it does on stock v2 (split parts are
  one-shot in both, 14.3/16.4); a `BufferedSplittableAsyncRequestBody` part is retried and succeeds.
- **Async measured for DynamoDB only.** Not measured at all: async S3 streaming and multipart performance, checksum paths, event streams,
  presigning, paginators, waiters, SigV4a, endpoint discovery.
- **Not covered by fault injection:** client-side failures — `apiCallTimeout`/`apiCallAttemptTimeout`
  expiry, credential-resolution failure, DNS failure. A mock server cannot inject them.
- **Single-threaded, loopback, mock server.** No real network latency, no throttling, and TLS only in the
  S3 streaming collection.

---

## 8. What to do next

Ordered by what each would settle, not by effort.

**If the question is "should we do this":**

1. **Take finding #4 to the RFC.** The C2J-derived schema path produced five silent wire bugs in two
   services. Either the canonical-Smithy cutover is the plan (RFC §2.3) or the trait-fidelity problem
   needs an answer that isn't "guess"; the prototype has made this concrete and it should not stay in a
   §12 subsection.
2. **File the five smithy-java defects upstream** (3.6, 3.7, 15.6, 16.4, 16.8). All are worked around
   here; 3.6 in particular affects native smithy-java clients and the fix is small — `V2TransportFailures` is
   the argument for how small.
3. **Decide who owns the async envelope.** The bridged async client parks one virtual thread per call on
   a synchronous smithy-java (§2, ledger §16). It works, and it is cheap, but it means context
   propagation (16.1), cancellation (16.5) and `apiCallTimeout` (8.1) all need a bridge-side answer that a
   native `callAsync` in smithy-java would give for free. Worth raising with the smithy-java team
   alongside 3.6, and filing 15.6, the one-line `asByteBuffer` fix, at the same time.

**If the question is "how fast could it be":**

0. **Hybrid signing.** Prove smithy-java's and v2's signers agree on requests with default signer
   properties (a fixed-clock comparison), then sign those with smithy's. It recovers the ~20 µs/call that
   §17 costs every DynamoDB call.

4. **Allocation profile of `batch-get`**, to test the `SdkPojoDeserializer`/`BridgeStruct` hypothesis —
   the largest remaining performance question, since finding #2 says the veneer is where the gap lives.
   Blocked on this host: async-profiler is not installed; JFR is the fallback.
5. **Memoize the endpoint bridge** (3.3). Now the largest bridging component, and the retry-cost
   measurement is its before-picture.
6. **Keep `smithy-model` off the runtime path** (10.7). 114 classes of build-time library loaded by a
   runtime client, and the largest identified piece of the cold-start regression.
7. **Allocation profile of `put-object-8m`**, to explain the only size-scaling effect measured (~0.04
   µs/KiB).

**If the question is "what else is broken":**

8. **Client-side failure axes** — timeouts, credential resolution, DNS. The fault sweep found 87
   differences on its first run in the axis it *does* cover; there is no reason to expect this axis is
   clean, and nothing has looked.
9. **Guard the two `FRAGILE` entries** (1.7, 3.5) before they regress silently.
10. **Signature equivalence** (open question 1): does `SigV4Signer`'s canonical-header exclusion list
    match v2's exactly? A fixed-clock, fixed-credentials comparison would settle it. Currently unknown,
    and a mismatch is a production auth failure.
11. **Broaden the byte diff past 9 operations** (open question 9). The checksum-required set alone is
    larger, and 200-with-error-body, `modifyException` and virtual-host addressing are unexercised.

---

## 9. Where to read the detail

| File | Contents |
|---|---|
| `RFC-smithy-java-runtime.md` | The proposal: why re-host, module disposition, phased plan, locked decisions. Predates all measurement. |
| `compatability_issues.md` | The ledger. 95 entries in 17 sections + 10 open questions (3 now answered). §§1-11 pipeline and config, §12 rest-xml/S3 wire, §13 sync streaming, §14 multipart (via the façade), §15 async transport, §16 async client, §17 schema traits and request overrides — start at §17.11 for what remains. |
| `pipeline_benchmark2/RESULTS.md` | The measurements. 13 sections, raw-data pointers per collection, null experiments, 7 caveats, next measurements. |
| `test/standalone-e2e-benchmarks/README.md` | How to reproduce: arms, scenarios, CLI, fault injection, provenance stamping. |
| `pipeline_benchmark2/*/` | Raw data — manifests, `results.csv`, per-run logs, generated `summary.md`, archived jars. |

Reading order for someone new: this file, then `RESULTS.md` for the numbers behind §3, then the ledger
section matching whatever they are about to touch.
