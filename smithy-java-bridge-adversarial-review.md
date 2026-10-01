# Adversarial review: why the smithy-java bridge is not a viable path for the AWS SDK for Java v2

Branch: `smithy-java-bridge-alexwoo-full`. Scope: the whole design, not this branch's bugs — every
finding is classified by whether it can be fixed in the bridge, requires an upstream smithy-java
change, or is **fundamental** (inherent to running v2's API on smithy-java's pipeline). Where a fix
exists, its performance cost is evaluated against the project's only motivation: the measured
~32-34% app-CPU win on small operations, which is a fixed ~47-50 µs per call
(`pipeline_benchmark2/RESULTS.md`).

This review goes beyond `compatability_issues.md` (the ledger). Everything cited as new was
verified in source on this branch; `file:line` references are to this tree, smithy-java 1.6.1
(decompiled jars and the 1.6.1 tag), and published v2.

## Verdict

The bridge should be rejected. Not because the ledger's known gaps are unfinished, but because
this review found **five independent disqualifiers**, each sufficient alone:

1. **A hard platform break.** smithy-java requires JDK 21; v2 documents Java 8 support
   (`README.md:152-153`). There is no fallback path in the design — the async client *is* a
   virtual thread per call (`SmithyBridgeClient.java:75`, unconditional `Thread.ofVirtual()`), and
   without virtual threads the envelope model is strictly worse than stock async. (§1)
2. **Pipeline-shape incompatibilities that no amount of bridge code can fix** — smithy-java
   resolves the endpoint *after* the only request-modify hook, resolves identity *per attempt*,
   runs its retry loop *inside* deserialization, feeds the *previous attempt's signed request*
   back into the loop, and ends the "attempt" at response headers. Each of these breaks a
   documented, production-relied-on v2 behavior silently (§3, §4, §6). The branch's own open
   question 4 already concedes the first cannot be papered over.
3. **Silent data-integrity failures found beyond the ledger.** A 200-with-`<Error>`-body
   `CompleteMultipartUpload` returns *success with null fields* on the bridge where stock v2
   throws and retries (§7.1); `S3CrtAsyncClient` — the default Transfer Manager engine — is
   silently rebuilt on the bridged pipeline and loses the execution attributes the CRT
   meta-request client requires (§7.2). The ledger calls itself an "exhaustive inventory"; these
   are both High-severity misses in the two services the prototype *did* audit, which calibrates
   what the other ~423 services would cost to audit (§10).
4. **The performance motivation does not survive the fidelity it still owes.** The measured win
   was taken with metrics dead, business-metric telemetry dead, interceptor fidelity filtered
   off, response metadata unpopulated, and gzip off. The branch's own fidelity work (§17 of the
   ledger) already demonstrated the exchange rate: restoring S3 signing fidelity erased the
   async small-op win entirely (−11.5% → +1.5%) until a hybrid scheme bought part of it back.
   Restoring interceptors costs a measured 6-8% CPU; metrics parity is a second tax of the same
   class that cannot be filtered for any client with a publisher. The honest statement is that
   the bridge is fast *where v2 behavior has been removed*, and converges back toward v2's cost
   as behavior is restored (§11).
5. **Event streams cannot ride the bridge at all**, so every client of the 16 event-stream
   services — including Bedrock Runtime, whose flagship APIs are the streaming ones —
   permanently carries two pipelines with different interceptors, metrics, retries, timeouts and
   threading per operation. The two SDKs put the stream in incompatible places (member of the
   POJO vs beside it), and AWS's rolling per-frame signature cannot be seeded from the bridge's
   signing scheme, whose `SignResult` carries no signature (§8).

The ledger's bottom-up conclusion ("every gap so far was addressable") is true of the gaps it
lists and misleading about the design: each was addressed by re-running v2 code inside smithy's
pipeline, which is exactly the mechanism that re-accumulates v2's cost. What remains are the
gaps that *cannot* be addressed that way, and they are enumerated below.

---

## 0. Method

- Six parallel deep-dives (interceptors; pipeline ordering; metrics; async/threading/cancellation;
  streaming/S3 ecosystem; event streams/HTTP-2/breadth), each against v2 source, bridge source,
  and smithy-java 1.6.1 bytecode/sources. Headline claims were independently re-verified in
  source before inclusion.
- Risk ratings follow `example_risks/risk_descriptions.md`: impact, not probability; High for
  broad blast radius, silent undetectable failure, data loss, or depended-on behavior changing
  without opt-in. Labels are `[§.n][level]`.
- Precedent calibration: the Endpoints-BDD review (`example_risks/Java-v2-API-Surface-Area-Review-Endpoints-BDD.md:127`)
  rated *medium release risk* for reworking endpoint resolution "behind an unchanged API" while
  preserving the interceptor contract exactly, because "the highest-impact failure modes ...
  would be silent." This change reworks the **entire request pipeline** behind the unchanged API
  and does **not** preserve the interceptor contract, the attribute contract, the threading
  contract, the timeout spans, or the telemetry.

---

## 1. Platform baseline — the non-negotiable break

**[1.1][High] JDK 21 required; v2 supports Java 8. Fundamental.**
`README.md:152-153` commits the SDK to Java 8 compatibility; this branch sets
`<jre.version>21</jre.version>` for the whole reactor (`pom.xml:196,243-244`). smithy-java 1.6.1
itself requires 21 (sealed types, records). A Java 8/11/17 service that upgrades gets
`UnsupportedClassVersionError` — loud, but not opt-in: bridging is enabled per service by
codegen (`generateSmithyJavaSerde`), not by the customer. A large fraction of the v2 install
base is excluded by construction, before any behavioral question is reached.

**[1.2][High] The async design is virtual threads or nothing.**
`SmithyBridgeClient.java:75` creates a virtual thread per call unconditionally; the envelope
parks through serialization, identity, signing, connection-acquire and response-header wait. On
platform threads, every queued call is ~1 MB of stack against stock async's pending future —
with Netty's `maxPendingConnectionAcquires` default of 10,000, that is a thread count a JVM
cannot host. So there is no "backport the design to 17" option; the JDK break is loadbearing.

**[1.3][Medium] A runtime client now ships a build-time model library.**
The generated `$SCHEMA` constants pull 114 classes of `smithy.model.shapes`/`smithy.model.traits`
— Smithy's build-time model-processing library — into every customer JVM (ledger 10.7). Client
construction is +22% (507→603 ms, 0/10 paired wins), build-plus-first-call +10%. For Lambda and
CLI workloads — the segment most sensitive to SDK overhead — the bridge is a regression, not a
win (`RESULTS.md`, *Cold start*).

---

## 2. Interceptors

v2's `ExecutionInterceptor` has 18 hooks with precise ordering, cardinality, and mutation rights
(`ExecutionInterceptor.java:136-363`); the bridge maps 13 of them onto 6 smithy hooks and
synthesizes ~8 of the ~35 execution attributes v2 guarantees
(`AwsExecutionContextBuilder.java:95-144` vs `V2InterceptorBridge.java:154-179`). The gaps are
not uniform "missing features" — each one selects a different silent failure in shipping code.

**[2.1][High] `modifyException` is unbridgeable in practice — S3's exception contract breaks.**
S3's `ExceptionTranslationInterceptor.java:58,69` rewrites 404s from `HeadObject`/`HeadBucket`
into `NoSuchKeyException`/`NoSuchBucketException` in `modifyException`. The bridge never invokes
that hook, so `catch (NoSuchKeyException e)` — the documented existence-check idiom, used by
Hadoop S3A, Spring Cloud AWS, and countless applications — silently stops matching. The same
hook loss kills IAM's `GlobalServiceExecutionInterceptor` region hints, and the inert filter
outright **deletes** `HelpfulUnknownHostExceptionInterceptor` from every AWS client
(`V2ConfigTranslator.java:373-377`), removing the "is this endpoint a typo / wrong region"
diagnostics SDK-wide. Bridging it is not mere wiring: at smithy's `modifyBeforeCompletion` the
in-flight error is the smithy shim (`V2ModeledError` et al.), not the v2 exception the
customer's hook is typed against, and smithy-java 1.6.1's error-path chain *fails open* — the
first interceptor not overriding the hook ends the chain (ledger 3.7). The fix stacks a third
unwrap/rewrap pass onto an error path whose correctness is already ordering-coincidental.

**[2.2][High] The resolved endpoint does not exist at the only request-modify hook. Fundamental.**
smithy resolves the endpoint *after* `modifyBeforeSigning` (`ClientPipeline`, §3 table), so the
bridge shows v2 interceptors the client endpoint or the literal `https://unresolved.invalid`,
and discards any host/scheme/port they write (`V2InterceptorBridge.java:134-150,349-363`).
Shipping casualties:
- **MachineLearning `Predict` is misrouted by design.** `PredictEndpointInterceptor.java:37-57`
  replaces the URI with the model's dedicated `predictEndpoint` — the service's only routing
  mechanism. The bridge un-replaces it. Every call goes to the regional endpoint. Silent.
- **EC2/RDS/DocDB/Neptune cross-region copy presigning breaks.**
  `GeneratePreSignUrlInterceptor.java:115-123` derives the destination region by parsing
  `request.host()` — it parses `unresolved.invalid` (throws) or a client endpoint that disagrees
  with the resolved one (FIPS/dual-stack/account-ID), embedding the **wrong region into a
  presigned URL** on a security-sensitive signing path. `RdsPresignInterceptor.java:109-111`
  additionally NPEs on the absent `SELECTED_AUTH_SCHEME`.
- Any customer interceptor that redirects (mirroring, proxy swap-in, mesh rewrite) — legal in v2
  — fails silently.
The branch's own open question 4 concludes this ordering "cannot be papered over in the bridge";
the only fixes are upstream pipeline reordering or a second hook position that v2's chain has no
semantics for. **This is the same class of silent-ordering hazard the endpoints/auth (SRA)
refactor review flagged as its top risk — and that refactor went to lengths to preserve hook
order and attributes exactly; this one cannot.**

**[2.3][High] Request-body replacement is dropped; body-reading interceptors run body-blind.**
A `RequestBody` returned from `modifyHttpContent` is discarded (`V2InterceptorBridge.java:229-235`),
and `InterceptorContext.requestBody()` is never populated. CloudSearchDomain's
`SwitchToPostInterceptor.java:40-66` — which converts `Search` GET→POST and moves the query into
the body — keeps its method/header changes but loses the body: **every search goes out as a
bodyless POST with the query erased**. S3 Control's `PayloadSigningInterceptor` loses its
payload-signing override. `LegacyMd5ExecutionInterceptor` and the checksum interceptors take
their body-absent branch on *every* request. Fixable only by materializing v2 body objects per
attempt on the hot path — the same per-call v2-object construction the inert filter was built to
avoid (measured 6-8% CPU, ledger 2.1).

**[2.4][High] Error-path hooks are handed the wrong exception object.**
`readAfterExecution` passes the smithy-side error into v2's `onExecutionFailure`
(`V2InterceptorBridge.java:334-336`) before the boundary unwrap. `V2ModeledError` extends
smithy's `ModeledException`, **not** `AwsServiceException` (single inheritance, ledger 1.1) — so
every monitoring/alerting/circuit-breaker interceptor doing
`instanceof AwsServiceException` classifies every service failure as "unknown runtime
exception." Silent, fleet-wide for the enterprise population that uses interceptor-based
observability.

**[2.5][High] Interceptors are silently deleted, judged once at construction.**
The inert filter removes any interceptor overriding none of the six bridged hooks
(`V2ConfigTranslator.java:355,388-398`). A customer interceptor implementing only
`beforeTransmission` (per-attempt wire logging — a standard APM pattern) or only
`modifyException` is not "present but ineffective"; it is **removed from the chain with no log
line**. The judgment is made from the class and the environment at build time; the
Endpoints-BDD review rated a mere map-immutability change Medium for breaking interceptor
authors on upgrade — silent deletion of whole interceptors is categorically worse. And the
filter cannot simply be removed: it is what recovers 7-8% CPU (`RESULTS.md`, *Acting on it*).
Fidelity and performance trade against each other *inside this one component*.

**[2.6][Medium] `modifyHttpRequest` runs per attempt, and its retry-time mutations are discarded anyway.**
v2 calls hooks 5-7 once per execution, outside the retry loop (`BaseClientHandler.java:157`);
smithy's `modifyBeforeSigning` is per attempt. Worse, the fix for ledger 17.13 (v2's signer
re-signing a signed request) makes the bridge re-sign every retry **from the first attempt's
remembered unsigned request** — `V2SignerBridge.java:129-132` documents the consequence in its
own javadoc: "a v2 `modifyHttpRequest` that changes the request differently on a retry than on
the first attempt has that change dropped." So the bridge has a third semantics matching neither
pipeline: the hook's *side effects* (counters, log lines) fire N times, while its *mutations*
apply once. A fresh-nonce/timestamp/rotating-credential header observes its own hook running on
the retry while the wire carries the stale value. Undetectable without a wire capture.
Fundamental: it is forced by smithy feeding the signed request back into its loop; the
alternatives are invalid signatures (17.13) or a full per-attempt request rebuild.

**[2.7][Medium] The attribute surface is a behavioral lottery with a proven failure mode.**
Withheld attributes don't fail — they select the interceptor's other branch. The branch already
shipped this bug once: missing `SERVICE_CONFIG` silently set S3's `Expect: 100-continue`
threshold to 0 (ledger 13.5). ~27 attributes are still absent, each a latent branch-flip to be
discovered one production incident at a time. Several (`RESOLVED_ENDPOINT`,
`SELECTED_AUTH_SCHEME` at modify time) **cannot** be truthfully populated at the hook position
smithy provides.

**[2.8][Medium] Blast radius.** 14 service modules ship pipeline-load-bearing interceptors
(s3 ×11, s3control, sqs, ec2, rds ×5, docdb, neptune, glacier, apigateway, cloudsearchdomain,
machinelearning, route53, iam), plus three on every AWS client. These are the SDK's own
correctness mechanisms expressed as interceptors; SDK-wide adoption means porting each to a
smithy-native equivalent forever, or paying §2.3/§2.5's tax.

---

## 3. Pipeline order and error surfacing

Side-by-side (v2 from `AmazonSyncHttpClient.java:181-209` + `BaseClientHandler`; smithy from
`ClientPipeline`, 1.6.1):

| Step | v2 | smithy-java |
|---|---|---|
| auth scheme + identity | **per execution**, in `beforeExecution`, before everything (`DynamoDbAuthSchemeInterceptor.java:132-134`; `SigningStage.java:92` joins the one future) | **per attempt**, inside the loop (`ClientPipeline` afterIdentity) |
| endpoint resolution | per execution, at `modifyRequest` | **per attempt**, *after* `modifyBeforeSigning` |
| marshalling | per execution, then loop | per execution, but hooks differ |
| `modifyHttpRequest` | per execution, outside loop | per attempt, inside loop (§2.6) |
| invocation id / headers / compression / checksum / UA | per execution, before loop (`AmazonSyncHttpClient.java:184-192`) | per attempt or absent |
| attempt timeout span | signing → transmission → **unmarshalling incl. transform** | **`transport.send` only, returns at headers** |
| retry decision | iterative stage, catches everything in the attempt | recursive, **inside `deserialize` only** — transport throws bypass it (ledger 3.6) |
| terminal hooks on failure | always (`ExecutionFailureExceptionReportingStage` wraps the pipeline) | only failures that reach `deserialize` |

**[3.1][High] Credential resolution per attempt changes the SDK's call pattern against AWS auth
infrastructure, exactly during incidents. Fundamental.**
A non-caching or hand-rolled STS/SSO provider is now called once per attempt: a 4-attempt
throttled call is a 4× multiplier on STS volume, arriving when the fleet is already retrying —
a thundering-herd amplifier during outage recovery (the risk rubric flags call-pattern changes
to AWS services even when no request fails). Credentials can also now **change between attempts
of one call** (impossible in v2 by construction — one resolved future serves all attempts), and
a token that expires during backoff fails attempt 3 of a call v2 would complete, un-retried, as
an exception type surfacing from a new position. Memoizing in the bridge re-serializes smithy's
design, must interact correctly with per-request credential overrides and account-ID endpoint
rules (which read the identity), and adds per-call state to the hot path — the fix spends the
margin.

**[3.2][High] `apiCallAttemptTimeout` no longer bounds a mid-body stall.**
v2's attempt timeout covers through unmarshalling and (sync) transform
(`AmazonSyncHttpClient.java:193-200`); the bridge's wraps only `transport.send`
(`V2TransportBridge.send` → `V2Timeouts.attempt`), which returns at **headers**. A server that
sends 200+headers then stalls the body — the canonical case attempt-hedging exists for — is
bounded only by the socket timeout. A latency-sensitive service using short attempt timeouts for
hedging gets 15-30× worse tail latency with zero configuration change and zero errors. The
ledger's "matches stock against a stalled server" (8.2) tested a pre-header stall only.
Fundamental: smithy has no concept of "attempt fully consumed" — the body is drained inside
`deserialize`, interleaved with hooks.

**[3.3][High] The sync interrupt/cancellation contract is broken, including a possible
post-cancellation network attempt.**
v2 documents interrupt → `AbortedException` (`ApiCallTimeoutTrackingStage.java:108-148`,
`InterruptMonitor` checks throughout). smithy's backoff sleep converts interrupt to a **bare
`RuntimeException`** that `SmithyBridgeClient.toV2` passes through unchanged — `catch
(AbortedException)` / `catch (SdkException)` miss, breaking every framework that cancels SDK
calls via `Future.cancel(true)`. Worse: an interrupt landing mid-exchange surfaces as an
`IOException` subtype, which the deferred-transport-failure path **classifies as retryable** —
the bridge can send another network attempt for a call the customer just cancelled.

**[3.4][Medium] Terminal hooks skip signing-stage failures; smithy exception types leak.**
Failures in auth resolution, endpoint resolution, or signing propagate with no
`onExecutionFailure`/`afterExecution` (tracing spans leak); v2 always reports them. And the
boundary translation is enumerated, not total: `SerializationException` (a plain
`RuntimeException`) and any future smithy type outside `CallException` reach callers raw —
v2's public exception contract becomes coupled to smithy-java's internal exception taxonomy.

**[3.5][Medium] Recursive retries change stack-trace shape** (all N attempts' frames in the
final exception; exception-fingerprinting/grouping tools see different data), and per-attempt
re-execution of header merge/UA/checksum work erodes the retry-path advantage — measured: only
0.86× marginal per attempt vs 0.60× per call (`RESULTS.md`, *The cost of retrying*). Note the
trap: bridging `amz-sdk-request` (attempt counter header, ledger 12.10) *conflicts* with §2.6 —
the per-attempt header would be discarded on retries by the re-sign-from-first-request fix. Two
fidelity fixes that are individually correct are mutually exclusive.

---

## 4. Retries and error semantics

Mostly ledgered; restated here because the aggregate is the argument.

**[4.1][High, upstream] smithy-java 1.6.1 does not retry transport failures at all** — the retry
loop is inside `deserialize`, so a connection reset before headers (the most common production
transport failure) is structurally unreachable (ledger 3.6). The bridge's fix is a **fake
HTTP 599 stand-in response** flowed through deserialization so the failure can re-enter the loop
(`V2TransportFailures`). That hack is now load-bearing for every reset, connect timeout, and TLS
failure in a bridged fleet, and H2 stream errors (GOAWAY/RST, §8.4) have never met it.

**[4.2][High] `retryPolicy(...)` (the deprecated but ubiquitous API) is silently downgraded** to
smithy's own defaults: 3 attempts instead of 4, smithy backoff, no token-bucket or
max-backoff settings, DynamoDB's tuned policy lost (ledger 3.4). No warning. Customers who
tuned retries for their workload silently retry fewer times, faster.

**[4.3][Medium] Custom retry conditions are discarded.** `SdkRetryStrategy.of()` replaces
`retryOnException`/`treatAsThrottling` wholesale (ledger 3.2); a customer strategy's added
conditions, and `RetryPolicyAdapter`'s `RetryCondition`s, silently never run.

**[4.4][Medium] Retry classification is pinned to third-party exception taxonomies on both
sides** (v2 retries torn bodies via Jackson 2's `IOException` hierarchy; smithy parses with
Jackson 3 where it is a `RuntimeException` — ledger 3.5). The current equivalence is a
coincidence maintained by hand, with silent divergence as the failure mode.

**[4.5][Medium] Shape-ID keying makes codegen renames a silent, total error-handling failure**
(ledger 1.7): a mismatched registry ID costs the concrete exception type, the error code, and
the retry, with no error and no build-time check. Every one of the ~50 services with renaming
customizations (§10.2) is exposed.

---

## 5. Timeouts and cancellation (async)

**[5.1][High] Cancelling the returned future aborts nothing.**
Stock v2 propagates cancellation end to end: `forwardExceptionTo` chains from the caller future
through every async stage to the HTTP client future, and Netty fails the promise / closes the
channel and returns the connection (`NettyRequestExecutor.java:117-146`). On the bridge, cancel
completes only the caller-visible future (`SmithyBridgeClient.runAsync`): the envelope keeps
executing, **including remaining retries and backoff sleeps against a possibly-throttling
service**; a cancelled `GetObject(toFile)` keeps streaming the whole object to disk with no
`exceptionOccurred`; a cancelled multi-GB upload keeps transmitting. Cancel-heavy patterns
(hedged requests, user-abandoned requests) leak transport concurrency — Netty `maxConcurrency`
defaults to 50 — until responses complete: a burst of cancels saturates the client and presents
as server-side latency. Mostly wirable in the bridge (the per-attempt abort action exists for
timeouts), but interruption cannot reach non-interruptible customer code, and the exchange abort
contract has to be bolted around smithy's `ClientTransport.send`, which returns no handle —
the real fix is upstream.

**[5.2][High] `apiCallTimeout` ends at response headers for streaming operations.**
v2's whole-call timer covers through the `AsyncResponseTransformer`'s completion
(`MakeAsyncHttpRequestStage.java:109-174`); the bridge's `V2Timeout` wraps `Client.call`, which
for streaming returns before the caller drains the body (`V2AsyncStreamingInvoker`). A
`getObject(..., toBytes())` with `apiCallTimeout(30s)` against a mid-body stall **hangs
indefinitely** where stock raises `ApiCallTimeoutException` at 30 s. The headline timeout
customers configure no longer means what it means.

**[5.3][Medium] The bridge's timer is a static, unmanaged, shared daemon thread** that ignores
`overrideConfiguration().scheduledExecutorService(...)` (`V2Timeout.java:54-58`; no reference to
`SCHEDULED_EXECUTOR_SERVICE` in the bridge): thread-audited environments get a thread they
cannot supply or size; app-server redeploys pin the classloader; one thread serializes every
client's timeout aborts process-wide; and `timer.cancel(false)` does not stop an in-flight
`fire()`, which can leave a stray interrupt on the caller's platform thread for the next
blocking call to eat. v2 timeouts also never interrupt customer code; the bridge's do, and a
timeout inside non-interruptible code fires late or never where stock fires on time.

---

## 6. Async threading model

**[6.1][High, fundamental] All pre-transport work — including customer code — moves to a fresh
envelope thread per call.** `beforeExecution`/`modifyRequest` interceptors, credential
resolution, marshalling run on `sdk-smithy-bridge-call-N`, not the caller's thread (ledger
16.1). Everything thread-bound breaks silently: MDC logging context, OpenTelemetry/X-Ray span
parents captured by thread, Spring `SecurityContextHolder`, request-scoped tenant IDs — all read
empty. `InheritableThreadLocal` copies as a creation-time snapshot with different semantics.
Thread names in logs change; exceptions from interceptors/credentials/serialization carry
envelope-only stacks with **no caller frames**, degrading APM attribution and debuggability
fleet-wide. Not fixable generically: arbitrary ThreadLocal propagation has no hook, and running
pre-transport work on the caller is impossible because `Client.call` is blocking end to end.

**[6.2][Medium] Customer code on virtual threads: pinning and cache churn.**
On JDK 21 (the stated baseline), a customer interceptor or credential provider doing blocking
work under `synchronized` pins a carrier; at core-count concurrency that stalls every envelope
*and the application's own virtual threads* — an SDK upgrade now changes process-wide
scheduling (`-Djdk.virtualThreadScheduler.*` becomes relevant tuning). The branch's JFR
"0 of 458 pinned" covered bridge code only; customer code is unbounded. Separately, a fresh
thread per call defeats every per-thread cache in customer code (crypto contexts, formatters,
buffer recyclers) — per-call allocation invisible to the bridge's own benchmarks, which contain
no customer code.

**[6.3][Medium] `AsyncResponseTransformer.prepare()` once per call, with a now-proven
consequence**: stock async **does** retry mid-body failures — `prepare()` is called per attempt
(`MakeAsyncHttpRequestStage.java:133`) and any exceptional completion routes through
`AsyncRetryableStage.java:120-124` — so a connection reset halfway through a 5 GB
`getObject(toFile)` is retried on stock and **fails the call on the bridge** (the ledger left
this "not measured"; the source settles it). Sync is the same shape, plus a cosmetic leak:
`RetryableException` from a transformer reaches the caller, a type stock never leaks. The
customer's `maxAttempts` silently stops applying to an entire failure class. Fundamental without
an upstream hook (ledger 13.4's analysis stands).

---

## 7. Streaming and the S3 ecosystem — new High findings beyond the ledger

**[7.1][High] A 200 response with an `<Error>` body is treated as success. Silent data loss.**
Stock v2 wires every non-streaming S3 operation through
`AwsS3ProtocolFactory.createErrorCouldBeInBodyResponseHandler` →
`DecorateErrorFromResponseBodyUnmarshaller.java:56-84`, which inspects a 2xx body's root element
and converts `<Error>` into an `S3Exception` carrying the real code — and **retries** it
(`AwsErrorCode.java:41,67`: `InternalError` retryable, `SlowDown` throttling). S3 is documented
to return exactly this for `CompleteMultipartUpload` and `CopyObject`. The bridge has no
equivalent anywhere: `V2ErrorEnricher.java:197-198` explicitly guards `statusCode() >= 400`
(its comment: building a service exception for a 2xx "would report `statusCode() == 200`, which
no v2 caller expects"), and the rest-xml deserializer parses `<Error>` as the response shape —
unknown elements are skipped, yielding a **successful `CompleteMultipartUploadResponse` with
null `eTag`/`location`/`key`** while the object was never assembled. The multipart helper
(`GenericMultipartHelper.java:85-93`) does not validate the response, so Transfer Manager
reports a completed upload, the parts remain as an orphaned billed multipart upload, and nothing
anywhere errors. This is the worst failure class in the rubric — silent, undetectable,
data-integrity — on one of S3's most used composite operations. Fixable in the bridge (peek the
buffered 2xx body), but it is the sharpest evidence of the audit problem: the ledger
byte-diffed S3 for a month, called itself exhaustive, and missed a High data-loss defect that
stock handles in a protocol-factory default the bridge path simply never runs.

**[7.2][High] `S3CrtAsyncClient` — the default Transfer Manager engine — silently rides the
bridged pipeline and loses the attributes the CRT client requires.**
`S3AsyncClient.crtBuilder()` is not a separate stock pipeline: `DefaultS3CrtAsyncClient.java:164`
builds an inner `S3AsyncClient.builder()` — bridged, since S3 sets
`generateSmithyJavaSerde: true` — with a `NoOpSigner` and the CRT meta-request HTTP client. That
client is driven by `SDK_HTTP_EXECUTION_ATTRIBUTES` (operation name, signing region, checksum
config, file paths, CRT credentials adapter, progress listener, pause observable —
`S3CrtAsyncHttpClient.java:145-198`), which stock passes via `MakeAsyncHttpRequestStage` and the
bridge **never forwards** — `V2AsyncTransportBridge.java:119` builds `AsyncExecuteRequest` with
request/publisher/handler only; the bridge contains zero references to
`SdkHttpExecutionAttributes` (and never sets `fullDuplex`). CRT-based S3 — including
`S3TransferManager` with its default CRT engine, and its progress/pause/resume feature set
(`CrtS3TransferManager.java:74-156`) — is broken or stranded. Neither the ledger nor the design
doc mentions `S3CrtAsyncClient` at all; the prototype's CRT coverage tested the generic
`AwsCrtAsyncHttpClient` only.

**[7.3][Medium] S3 presigned URLs and client requests are now built by different serializers.**
`DefaultS3Presigner.java:124-128` uses the stock `*RequestMarshaller`s; client calls use the
smithy serializer. Any divergence in query/path encoding, empty-payload rules, or
customization-injected members (the `preClientExecutionRequestCustomizer` class of ledger 12.6)
makes a presigned request behave differently from the same request object sent through the
client — silent, and nothing on the branch compares the two paths.

**[7.4][Medium] Remaining ledgered streaming gaps that resist bridging**: the response
transformer runs outside the retry loop (13.4/16.3, now sharpened by §6.3);
`abort()` maps to `close()` — early-closing a huge download *drains* the connection instead of
aborting it, a tail-latency and bandwidth change (13.4); `requiresLength` operations send
chunked and fail server-side instead of client-side (15.1); one-shot async-body detection is a
class-name heuristic (16.4).

---

## 8. Event streams and HTTP/2 — a fork in the design, not a gap

16 of 425 services model event streams, and they are flagship APIs: Bedrock Runtime
(`ConverseStream`, `InvokeModelWithResponseStream`, bidirectional `InvokeModelWithBidirectionalStream`),
TranscribeStreaming (4 bidi H2 operations), Kinesis `SubscribeToShard` (H2), Lex V2, QBusiness
`Chat`, CloudWatch Logs Live Tail, Lambda `InvokeWithResponseStream`, S3 `SelectObjectContent`,
and more.

**[8.1][High] Every event-stream operation is excluded by construction
(`ClientClassUtils.usesSmithyPipeline`), permanently splitting each client into two pipelines.**
For Bedrock Runtime this inverts the value proposition: the operations carrying most traffic and
latency sensitivity get zero benefit, while the client pays the dual-pipeline cold start and
carries every ledger risk on the non-streaming neighbors. One client; two interceptor hook sets,
two metric behaviors, two retry mechanisms, two timeout implementations, two threading models —
selected per operation, invisibly.

**[8.2][High] The two SDKs put the stream in incompatible places. Fundamental.**
smithy-java 1.6.1 models the stream as a typed member of the POJO, driven via
`inputEventBuilderSupplier()` with a *blocking* reader/writer surface; v2 strips the member and
passes a reactive `Publisher<Event>` beside the request, with a visitor-driven, 420-line
ordering-sensitive response state machine. The out-of-band `RequestOverrideConfig` trick that
rescued blob streaming cannot carry a typed `EventStream<T>` member the protocol resolves on the
generated operation. The exits: regenerate POJOs with stream members (**public API break**), or
a reactive↔blocking adapter costing two parked threads per direction per stream and a per-event
hop — a performance **regression** on long-lived AI streaming workloads, which under this
review's criterion invalidates the design on its own.

**[8.3][High] AWS rolling frame signatures cannot be seeded from the bridge's signing scheme.**
Each event frame's signature chains off the previous, seeded by the HTTP request's SigV4
signature — smithy-java seeds its event signer from `SignResult.signature()`
(`ClientPipeline.java:196-199`), and **every `SignResult` the bridge constructs is built without
a signature** (`V2SignerBridge.java:112,326-335`); v2's `SignedRequest` does not expose it, and
hybrid signing (ledger 17.12) makes the seed depend on which signer signed the attempt. The
precedent that this class of defect ships undetected is on this very branch: 17.13's re-signing
bug was invisible for the prototype's whole life "because a mock server does not check
signatures." Per-frame rolling signatures on long-lived connections are the same blind spot,
magnified.

**[8.4][High] HTTP/2 has never been carried.** The bridge hardcodes `HTTP_1_1`, and
unconditionally adds `Transfer-Encoding: chunked` to unknown-length bodies
(`V2AsyncTransportBridge.addFraming`) — a connection-specific header forbidden on H2 — where v2
keys the decision on model traits *and* `useHttp2` (`AbstractStreamingRequestMarshaller.java:66-76`).
Any customer setting `NettyNioAsyncHttpClient.builder().protocol(HTTP2)` — supported on every
async client — is on an untraveled path, and H2 stream-level failures (GOAWAY, RST_STREAM; 2,037
LOC of v2 handling) have never met the deferred-transport-failure retry workaround (§4.1).

---

## 9. Metrics and telemetry — the cleanest proof of the thesis

**[9.1][High] Total metric blackout, with the publisher still "working".**
Every duration, retry, error-type and concurrency metric flatlines at upgrade for every customer
with a `MetricPublisher`: CloudWatch alarms on `RetryCount`/`ApiCallDuration`/`AvailableConcurrency`
go no-data, pages nobody, and the per-call record still arrives — containing only
`ServiceId`+`OperationName` (the generated method's two) and **no measurements**. An empty datum
is harder to notice than an absent one, and the publisher's cost is still paid. 14 of ~25 datums
are recorded inside stages, generated interceptors, and auth strategies the bridge deletes or
never runs.

**[9.2][High] "Run v2's own code" demonstrably fails here.** The customer's own unmodified
Apache/Netty/CRT HTTP client records `AVAILABLE_CONCURRENCY`, `CONCURRENCY_ACQUIRE_DURATION`
etc. — into a **no-op**: `ApacheHttpClient.java:232` falls back to `NoOpMetricCollector` when
the request carries no collector, and the bridge's transports never set one (zero
`MetricCollector` references in the bridge module). The telemetry lives in the *pipeline
structure* (the ApiCall → per-attempt collector tree), not in any component the bridge can
adopt.

**[9.3][High] The `User-Agent` changes on every request for every caller.** The `m/` business-
metrics section (57 feature IDs: credential provenance, retry mode, checksum usage, account-ID
mode, S3 Express, waiters/paginators, DDB mapper, TM) is stamped by stages and generated
interceptors that are structurally dead on the bridge, and smithy's UA replaces v2's
spec-governed format. Every CloudTrail `userAgent` field, S3/ALB access log, SIEM detection rule
and attribution pipeline keyed on `aws-sdk-java/2...` changes fleet-wide at upgrade; AWS loses
the Java-fleet telemetry service teams use to scope deprecations, and support loses
`amz-sdk-invocation-id`/`amz-sdk-request` correlation (ledger 12.10) at the same time.

**[9.4][High] Exact parity is impossible in places and margin-erasing everywhere else.**
Several definitions are tied to pipeline shapes the bridge changed:
`CREDENTIALS_FETCH_DURATION`/`ENDPOINT_RESOLVE_DURATION` are per-execution concepts on a
per-attempt pipeline (§3.1); streaming `TIME_TO_LAST_BYTE`/`READ_THROUGHPUT` attach to an
attempt that no longer exists when the transformer runs (§6.3). The rest require wrapping every
bridge component with per-attempt collectors and ~10-14 clock reads — the same cost class as
the interceptor bridge (measured 5.9-8.4% CPU), and *unfilterable*: a client with a publisher
needs it on every call. The benchmark arms all ran publisher-free; no published number
describes a metrics-on bridge. smithy's own OTel plugin is a different vocabulary and
cardinality — adopting it is a breaking replacement of `@SdkPublicApi` types, not a bridge.

---

## 10. Generalization: DynamoDB + S3 are the easy two

**[10.1][High] Protocol coverage.** Census of 425 services: rest-json 253, awsJson 140, query 15
(**STS, IAM, SNS, RDS, CloudFormation**, ELB, SES…), rpc-v2-cbor 9, rest-xml 4, ec2 1 (EC2, 769
operations). smithy-java 1.6.1 has **no ec2Query client protocol at all**, and nothing on this
branch has ever exercised its rest-json, awsQuery, or cbor protocols — nor `awsQueryCompatible`
error-code remapping (SQS, CloudWatch). The calibration for what "exists upstream" is worth:
byte-diffing **six operations of one service** on rest-xml found **five silent wrong-bytes
bugs**, including total silent loss of `x-amz-meta-*` user metadata (ledger 12.1-12.5). That
discovery process — build a golden harness, diff bytes, fix schema translation — recurs per
protocol × per service, with silent data loss as the established worst case, and §7.1 proves
the process misses things even where it ran.

**[10.2][High] The customization long tail makes codegen naming load-bearing on the wire.**
Two proven classes — `X-Amz-Target` from the generated shape name (wrong name = blanket 400,
ledger 11.1) and a `renameShapes` member rename unbinding a URI label (every CopyObject threw,
12.4) — generalize to every renaming/injecting customization: 26 services use `shapeModifiers`,
13 add their own interceptors (each needing §2's audit), 8 use `renameShapes`, plus
`preClientExecutionRequestCustomizer` (the 12.6 class), with **no build-time check** that a
generated name matches the wire (1.7). Each of ~384 customization.configs is a per-service
audit whose failure default is silent.

**[10.3][Medium] Even a DynamoDB+S3-only rollout doesn't contain the risk**: assumed-role/SSO
chains call STS (query protocol, unbridged) from inside a bridged client's **per-attempt**
credential resolution (§3.1) — a call-pattern change against the service AWS most needs stable
during incident recovery.

---

## 11. The performance case does not survive its own fidelity bill

The project's sole motivation, measured honestly, with the trajectory already on the branch:

| State | sync small-op app CPU vs stock | async small-op |
|---|---|---|
| before §17 fidelity work | −35.9% / −36.6% | −11.5% / −13.6% |
| after §17 (v2 signer, response hooks, overrides, timeouts) | −18.5% / −19.8% | **+1.5% / −0.3%** |
| after hybrid signing recovered part | −31.0% / −30.6% | −4.7% / −9.1% |

(`RESULTS.md`, *Fidelity cost*.) One tranche of fidelity — S3 signing behavior — consumed the
entire async margin until a hybrid scheme (which itself introduced §2.6's mutation-dropping and
carries 8.3's seed problem) bought part of it back. Now price what is still owed:

- **Interceptor fidelity**: the bridge is measured at 5.9-8.4% CPU when installed; the headline
  numbers exist because a filter keeps it off default clients (§2.5). Clients with real
  interceptors — the enterprise/APM population that most needs compatibility — pay it, and full
  18-hook fidelity (§2.1, §2.3) adds body materialization and error rewrap on top. **No
  published number describes a bridged client with interceptor fidelity on.**
- **Metrics parity**: a second tax of the same class, unfilterable for publisher users (§9.4),
  and partially impossible regardless of spend.
- **Pipeline-order fixes**: per-call memoization state for identity/endpoint (§3.1, §3.5),
  cancellation wiring, timeout-span extension — individually small, collectively the same
  per-call object construction the bridge's win comes from not doing. The retry path already
  shows the decay: 0.86× marginal per attempt vs 0.60× per call.
- **What cannot be bought back at any price**: the remaining 22 µs between "bridges stripped"
  and native smithy is the v2 model/API veneer itself (`RESULTS.md`, *Decomposition*) —
  removable only by breaking the public API, which is out of scope by definition.
- **Cold start is already a regression** (+10% to first response, 0/10 wins) for the
  Lambda/CLI segment, and it worsens with every added bridge component.
- Items the current margin silently includes by *not doing them*: response metadata on success
  (currently an **NPE** where v2 returns a value — ledger 1.4), gzip negotiation, business
  metrics, invocation-id headers, CRC32 validation on the measured path (`RESULTS.md` caveat 4).

The structure of the argument matters more than any one number: **every behavioral fix on this
branch has worked by running v2's code inside smithy's pipeline, and every such fix moves the
bridge's cost back toward v2's.** The limit of that process is v2's performance with smithy's
risks. The savings that remain at any point in time are, to first order, an inventory of v2
behaviors not yet restored.

## 12. Dependency and maintainability

**[12.1][High] The runtime being wrapped is sealed against its one indispensable customer.**
`ClientPipeline` package-private/final, `AwsJsonProtocol` sealed with no injection points
(ledger 1.1), protocols final. The prototype needed **four load-bearing workarounds against a
single minor release** of smithy-java: the fake-599 transport-failure deferral (3.6), interceptor
ordering as a correctness invariant (3.7), a reimplemented response `DataStream` (15.6), and a
rebuilt override-config composition (16.8) — each the kind of internal coupling that breaks on
any upstream refactor.

**[12.2][High] Two-master releases.** v2 ships daily against services launching weekly; every
pipeline-touching feature (new checksum algorithm, auth scheme, endpoint behavior) must now
clear two repos, two review bars, two release trains — or accrue more `V2*` workaround classes,
which is how the performance tax re-accumulates. Upstream behaviors v2 cannot patch become
customer-visible v2 behavior: Jackson 3's exception taxonomy (§4.4), codec cosmetics, and
whatever the next minor changes.

---

## 13. Consolidated list of fundamental incompatibilities

These are the ones that survive any amount of bridge engineering, with the breaking change each
implies:

| # | Incompatibility | Breaks |
|---|---|---|
| 1 | JDK 21 baseline (§1) | the Java 8/11/17 install base, with no opt-out |
| 2 | Endpoint resolved after the only request-modify hook (§2.2) | host-redirecting and presigning interceptors (ML Predict, EC2/RDS presign), silently |
| 3 | Request-modify hooks per attempt + retries re-signed from attempt 1's request (§2.6) | any non-idempotent or retry-varying interceptor; conflicts with restoring `amz-sdk-request` |
| 4 | Identity resolved per attempt (§3.1) | credential call patterns during outages; one-identity-per-call invariant; failure timing |
| 5 | Retry loop inside `deserialize` (§4.1) | transport-failure retries, patched only by a fake-response hack upstream disowns |
| 6 | Attempt/call "span" ends at headers; transformer outside the retry loop (§3.2, §5.2, §6.3) | attempt timeouts, apiCallTimeout on streaming, mid-body retry — all silent |
| 7 | Single-inheritance exceptions (§2.1, §2.4) | every error-path hook sees shims; `instanceof AwsServiceException` monitoring |
| 8 | Caller-thread execution of pre-transport work (§6.1) | MDC/OTel/X-Ray/security contexts, stack traces, thread identity — unfixable generically |
| 9 | Event-stream placement + rolling signature seed + H2 (§8) | 16 services' flagship APIs; permanent dual pipelines |
| 10 | Telemetry lives in pipeline structure, not components (§9) | exact metric parity impossible; UA changes on every request |
| 11 | Sealed upstream + two-master cadence (§12) | v2's ability to ship fixes and features at its own pace |
| 12 | The fidelity-performance exchange rate (§11) | the motivation itself: each restored behavior re-buys v2's cost |

## 14. A note on what this review says about auditability

The strongest single argument may be epistemic. The ledger is the most rigorous compatibility
audit this prototype could realistically get — months of byte-diffing, fault sweeps, paired
benchmarks, on **two** services — and it describes itself as exhaustive. This review still found,
in those same two services: silent 200-with-error data loss on `CompleteMultipartUpload` (§7.1),
a broken default Transfer Manager engine (§7.2), silently discarded retry-time request mutations
documented only in a javadoc (§2.6), an unbounded mid-body stall under the headline timeout
(§5.2), a cancelled call that keeps retrying against a throttling service (§5.1), and a
monitoring-facing exception-type break on every failure (§2.4). Each is High. If that is the
discovery rate *after* the exhaustive audit of the two easiest services, the audit cost of the
remaining ~423 services and five protocols is not a line item — it is the program. A rewrite of
the request pipeline behind an unchanged API has to be held to the standard the SRA/endpoints
reviews held far smaller changes to, and at that standard this design cannot get there from
here.
