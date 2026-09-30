# V2-on-smithy-java: compatibility issue ledger

Branch: `smithy-java-bridge-alexwoo-full`. Scope: **DynamoDB (awsJson) and S3 (rest-xml), sync client**,
covering non-streaming operations (sections 1-12), sync streaming (13) and multipart through a sync-backed
async façade (14). smithy-java 1.6.1 / smithy 1.73.0 / AWS SDK for Java v2 2.46.11-SNAPSHOT.

S3 findings (section 12) come from `test/wire-diff`, which captures the request at the
`SdkHttpClient` boundary — after marshalling, endpoint resolution and signing — and diffs it against a
golden capture from a stock build. Anything in section 12 quoted as a wire snippet was observed, not
reasoned about. Sections 13 and 14 come from the behavioral tests in the same module
(`S3StreamingTest`, `S3MultipartTest`), which assert on bytes moved and errors raised rather than on
request text.

This is an **exhaustive inventory of behavioral differences**, not a fix list. The goal of the
prototype is to learn where a smithy-java-based pipeline cannot reproduce v2 semantics, and what
it costs to make it. Nothing here is expected to be fixed on this branch unless it blocks a
working request/response.

Status legend:

| Status | Meaning |
|---|---|
| `BRIDGED` | v2 component is adapted and driven by the smithy pipeline; behavior intended to match |
| `TRANSLATED` | v2 configuration is read once at client construction and mapped to a smithy-native equivalent |
| `DEGRADED` | works, but observably differs from v2 |
| `MISSING` | v2 feature has no effect at all on this path |
| `BLOCKED` | cannot be represented in smithy-java without an upstream change |
| `FRAGILE` | behavior currently matches, but rests on a coincidence that nothing enforces — a name, a class hierarchy — so it can regress silently |

Each entry records what a customer would observe, not just the internal difference.

---

## 1. Errors and exceptions

### 1.1 `BLOCKED` — v2 modeled exceptions cannot be smithy `ModeledException`s

`software.amazon.smithy.java.core.error.ModeledException` is an **abstract class** extending
`CallException`. Every v2 modeled error already extends `DynamoDbException -> AwsServiceException
-> SdkServiceException -> SdkException -> RuntimeException`. Java has no multiple inheritance, so a
generated v2 exception can never be a `ModeledException`.

This matters because `HttpErrorDeserializer.ErrorPayloadParser.parsePayload` resolves the error
builder with:

```java
typeRegistry.createBuilder(id, ModeledException.class)
```

`TypeRegistry.createBuilder(id, type)` throws `SerializationException` when the registered class is
not assignable to `type`. Registering the v2 exception directly is therefore not viable.

**Consequence before mitigation:** modeled errors never deserialize. Every service error surfaces
as a bare `CallException` with no error code, no modeled members, and — because
`ApplyModelRetryInfoPlugin` keys off `error instanceof ModeledException` — no retry classification.
Throttling errors are not retried.

**Mitigation on this branch:** a single hand-written shim,
`software.amazon.awssdk.bridge.smithyjava.error.V2ModeledError extends ModeledException`, wraps the
built v2 exception. Its `ShapeBuilder` delegates deserialization to the v2 exception's generated
builder. `ApiOperationSpec` registers the shim per error shape instead of the v2 exception. The
bridge unwraps `V2ModeledError` at the client boundary so callers still catch e.g.
`ProvisionedThroughputExceededException`.

**Residual difference:** the exception a *smithy `ClientInterceptor`* observes inside the pipeline is
`V2ModeledError`, not the v2 exception. Anything inspecting error types inside the pipeline (including
a customer-supplied smithy retry strategy) sees the shim.

**Upstream change that would remove this:** make `HttpErrorDeserializer` resolve builders as
`SerializableStruct` and let the protocol decide how to throw, or accept a
`KnownErrorFactory`/`ErrorPayloadParser` on `AwsJson1Protocol`. Today `AwsJsonProtocol` is
`sealed`/package-private and builds its `HttpErrorDeserializer` internally, so there is no injection
point.

### 1.2 `PARTLY RESOLVED` (§17) — generated smithy `Schema`s carry no traits

> **Update, §17.** The operation-level traits that decide wire behavior are now carried — as v2's own
> metadata on the generated operation (`V2OperationMetadata`: `httpChecksum`, `httpChecksumRequired`,
> `requestCompression`), plus `@endpoint` host prefixes and `@idempotencyToken` defaults in codegen — and
> are honored by v2's own code (signer, checksum rules, interceptors). What stays as described below is
> smithy-java's *internal* use of traits (`@retryable`/`@readonly`/`@error` for its own retry and fault
> views), which the bridge does not rely on. Measured against stock: 16/16 checksum goldens, 12/12 response
> validation cases, host prefix and token behavior identical.


`SdkSchemaFactory.structure(...)` and `Schema.createOperation(ShapeId)` produce schemas with **no
traits**. Everything in smithy-java that reads traits off a schema is therefore inert:

| Trait consumer | Trait | Effect of absence |
|---|---|---|
| `ApplyModelRetryInfoPlugin` | `@retryable`, `@readonly`, `@idempotent` | no model-driven retry classification |
| `ModeledException.getHttpStatusCode` | `@httpError`, `@error` | status code always defaults to 500 |
| `CallException.getFault` | `@error` | fault always `OTHER`, never `CLIENT`/`SERVER` |
| `InjectIdempotencyTokenPlugin` | `@idempotencyToken` | tokens not auto-generated |
| `HostLabelEndpointResolver` | `@hostLabel`, `@endpoint` | host prefixes not applied |

On this branch retry classification is instead recomputed from the **v2** exception by
`V2RetryClassification` (see 3.2), so the missing traits do not break retries — but they do mean the
smithy-native retry arm of the benchmark classifies nothing as retryable.

The two status/fault rows are about smithy's *internal* view of the error, not the caller's: the status
code and fault a v2 caller reads come from the HTTP response via `V2ErrorEnricher` (1.4) and are
correct. `V2UnmodeledError` derives its fault with `ErrorFault.ofHttpStatusCode` for the same reason —
what smithy inferred without a parsed payload is not worth propagating.

### 1.3 `RESOLVED` (§17.7) — unmodeled errors keep their metadata but lose their concrete type

> **Update.** The case that lost its type in practice was an error the *service* models but the
> *operation* does not declare (`ConditionalCheckFailedException` from a `GetItem`). Stock v2's generated
> error mapping for every operation lists every service error; the bridge's per-operation registry now does
> too. Fault sweep `20260930-*`: the `conditional-check-failed` rows are gone.


When the wire error code matches no shape in the operation's `TypeRegistry`, smithy throws a bare
`CallException` whose only content is a synthesized message (`"Server HTTP/1.1 503 response from
operation ...GetItemOperation@476a736d."`). This is the common case, not the exceptional one: a 503
from a load balancer carries no AWS error payload, an empty 5xx has no body to parse, and an error the
*service* models but the *operation* does not declare is absent from a per-operation registry.

**Measured, before mitigation** (`pipeline_benchmark2/errors/20260905-0138`): the bare `CallException`
path was catastrophic, not merely lossy. On five of nine injected faults the bridge returned a
`DynamoDbException` with `statusCode() == 0`, a null request ID, a null error code, no
`awsErrorDetails()`, and **one attempt where v2 made three** — a 503 was handed straight back to the
caller. See §3.2 for why losing the metadata also loses the retry.

**Mitigation on this branch:** `V2ErrorEnricher` no longer early-outs on non-`V2ModeledError`
failures. When the attempt has an HTTP response with a status `>= 400`, it builds the service's base
exception from that response — status code, request ID, `x-amz-id-2`, `awsErrorDetails` with the error
code smithy put on `CallContext.RESPONSE_ERROR_CODE`, and `sdkHttpResponse` — wraps it in
`V2UnmodeledError`, and *throws that in place of* the bare `CallException`. `ClientPipeline` treats an
error thrown from `modifyBeforeAttemptCompletion` as the attempt's error (it routes it through
`swapError`), so the retry strategy sees the classification stamped on the new exception. The client
boundary then unwraps it exactly as it does a modeled error.

The `>= 400` guard matters: a 2xx whose body fails to deserialize is a client-side problem, and
building a service exception for it would report `statusCode() == 200`, which no v2 caller expects.
Those keep going through `SmithyBridgeClient.translate`, which returns the first `SdkException` on the
cause chain unchanged (so transport, credential, and endpoint failures the bridge itself raised keep
their real v2 type) and otherwise maps a smithy `TransportException` to `SdkClientException`.

**Residual difference — the exception class.** smithy's error registry is **per operation**
(`ApiOperation.errorRegistry()`), built from the errors that operation declares. v2's is effectively
**per service**: `AwsJsonProtocolErrorUnmarshaller` is handed the whole service's exception list, so it
can name an error the operation never declared. Injecting `ConditionalCheckFailedException` — modeled
by DynamoDB, not declared on `GetItem` — shows the gap exactly: v2 throws
`ConditionalCheckFailedException` with `errorCode=ConditionalCheckFailedException`; the bridge throws
`DynamoDbException` with the same status code, request ID, service name and error code, but not the
same class. A caller with `catch (ConditionalCheckFailedException e)` around a `GetItem` misses.

Widening the registry to the whole service would close it, at the cost of one `TypeRegistry` per
service rather than per operation (DynamoDB: ~30 error shapes against `GetItem`'s 6) plus the
construction cost on every client. Not attempted here.

### 1.4 `PARTLY RESOLVED` (§17.7) — `requestId` / `extendedRequestId` and HTTP metadata

> **Update.** `awsErrorDetails().rawResponse()` is now populated for error responses (the enricher keeps
> the bytes before smithy's deserializer consumes them); the fault sweep's ten `rawResponse` rows are gone.
> Response-side metadata on *successful* responses is unchanged from what is described below.


v2 populates `SdkResponse.responseMetadata()`, `SdkResponse.sdkHttpResponse()` and
`SdkServiceException.requestId()`/`statusCode()`/`awsErrorDetails()` from the HTTP response during
unmarshalling. On this path it splits three ways:

- **Modeled errors: restored.** `V2ErrorEnricher` runs at smithy's `modifyBeforeAttemptCompletion`
  and calls `V2ModeledError.enrich(httpResponse, RESPONSE_ERROR_CODE, serviceName)`, which sets
  `requestId()`, `statusCode()` and `awsErrorDetails()` on the wrapped v2 exception before it is
  unwrapped. It exists for retry classification (3.2), which needs the AWS error code per attempt;
  the caller-visible fields are a deliberate side effect of the same pass.
- **Unmodeled errors: restored.** Same pass, via `V2UnmodeledError`; see 1.3. `extendedRequestId`
  (`x-amz-id-2`) is read on both error paths.
- **Successful responses: missing, and not null-safe.** Nothing in the deserialization bridge sets
  `responseMetadata` or `sdkHttpResponse`, and the generated `DynamoDbResponse` simply returns the
  unset field. `response.responseMetadata().requestId()` and
  `response.sdkHttpResponse().statusCode()` therefore throw `NullPointerException` rather than
  returning `null`/`UNKNOWN`. v2 always populates both.

### 1.5 `BRIDGED` — retry-after honoring from response headers

Originally recorded as `MISSING`, and the sweep showed it mattered: v2 spent 2 010 ms on a
`503` + `Retry-After: 1` where it spent 54 ms on the same 503 without the header, so v2 does honor it
and the bridge visibly did not.

The plumbing turned out to be present on both sides. `ClientPipeline` reads `RetryInfo.retryAfter()`
off the attempt's error and passes it to `RefreshRetryTokenRequest` as a suggested delay; nothing on
this path was populating it. `V2ErrorEnricher` now does, for both error paths, parsing `Retry-After` as
whole seconds.

**Residual differences:** only whole seconds are read and the HTTP-date form of `Retry-After` is
ignored — deliberately, because `RetryableStage.retryAfter` in v2 does the same and the point is for
both pipelines to back off by the same amount. `x-amz-retry-after` (milliseconds) is *not* read; v2
reads it only under `NEW_RETRIES_2026_ENABLED`, which this branch does not implement.

### 1.6 `DEGRADED` — the message on an unmodeled error omits the payload's `message`

`CallContext` exposes the normalized error *code* (`RESPONSE_ERROR_CODE`) but not the error *message*,
and by `modifyBeforeAttemptCompletion` the response body has been consumed. So `V2ErrorEnricher` cannot
recover a `message` member from an error whose shape it could not resolve, and reproduces v2's two
*fallback* messages instead: `"Service returned error code <code>"` when a code is available, else
`"Service returned HTTP status code <n>"` (mirroring
`AwsJsonProtocolErrorUnmarshaller.errorMessageForException`).

That is exact whenever v2 also had no message — a body-less 503, an empty 5xx. It differs when the
payload did carry one: for an unmodeled-on-this-operation error, v2 reports `injected fault (Service:
DynamoDb; Status Code: 400; Request ID: ...)` and the bridge reports `Service returned error code
ConditionalCheckFailedException (Service: DynamoDb; ...)`.

Separately, and on **every** error including modeled ones, v2 appends `(SDK Attempt Count: N)` to the
message and this path does not: v2 stamps it from `SdkStandardLogger`/`AwsServiceException`'s
`numAttempts`, which the bridge never sets because the attempt count lives in smithy's
`CallContext.RETRY_ATTEMPT` rather than on the v2 exception. Message text only — no field a caller
reads differs.

### 1.7 `FRAGILE` — the error registry is keyed on shape ID, so any codegen rename silently disables it

Found by the error sweep, not by inspection: `internal-error` (500, `InternalServerError`, **declared
on `GetItem`**) was falling through to the unmodeled path while `throughput-exceeded` resolved
correctly. The cause was `AwsServiceModel.smithyShapeId()` building the id from
`shapeModel.getShapeName()` — the generated **Java class name** — where v2's naming strategy has
already appended `Exception` to error shapes. So `InternalServerError` on the wire was looked up
against a registry keyed `com.amazonaws.dynamodb#InternalServerErrorException` and missed. Fixed by
using `shapeModel.getC2jName()`, the name as the model spells it.

Worth recording as a hazard rather than just a fixed bug, because the failure mode is silent and total:
a mismatched id produces no error, no log line, and a plausible-looking `DynamoDbException` — it just
quietly costs the concrete exception type, the error code, and (before 1.3's mitigation) the retry.
Every place v2 codegen renames a shape is a place this can recur, and there is no build-time check that
a registered id matches what the service actually sends.

---

## 2. Interceptors and plugins

### 2.1 `BRIDGED` (lazily) — `ExecutionInterceptor`, now 13 of 18 hooks

> **Update, §17.4.** The response side is bridged: `afterTransmission`, `modifyHttpResponse`,
> `modifyHttpResponseContent` (sync) / `modifyAsyncHttpResponseContent` (async), `beforeUnmarshalling`,
> `afterUnmarshalling`, `modifyResponse`. This is what activates v2's own `HttpChecksumValidationInterceptor`
> (on every v2 client) and S3's response interceptors (trailing-MD5 stripping, URL-decoding of listings).
> Still unbridged: `beforeMarshalling`, `afterMarshalling`, `modifyAsyncHttpContent`, `beforeTransmission`,
> and `modifyException` — the last is the one with a known consumer (S3's `ExceptionTranslationInterceptor`).
> The interceptors also now see the client's checksum modes and a request's own execution attributes.


v2's generated builder (`BaseClientBuilderClass`) installs interceptors from five sources: three
service built-ins (auth-scheme resolution, endpoint resolution, endpoint request-modifier), the
customization config's list, classpath discovery via
`ClasspathInterceptorChainFactory` (`software/amazon/awssdk/services/dynamodb/execution.interceptors`),
`overrideConfiguration().addExecutionInterceptor(...)`, and — for every AWS client —
`AwsDefaultClientBuilder.awsInterceptors()`.

On this branch the **three service built-ins are dropped**, because smithy owns auth-scheme resolution
and endpoint resolution natively (see 4.1, 5.1). Everything else is wrapped by `V2InterceptorBridge`
**only when the resolved list is non-empty**.

That lazy install was expected to keep the default path free, and at first it did not:
`awsInterceptors()` unconditionally adds `HelpfulUnknownHostExceptionInterceptor`,
`EventStreamInitialRequestInterceptor` and `TraceIdExecutionInterceptor` to every AWS client, so the
bridge was installed on **every** client and a plain `DynamoDbClient.create()` paid per-attempt v2
`Context`/`ExecutionAttributes` construction at all four bridged hook points to run three interceptors
that could not do anything. Measured cost of that: 5.9% of app CPU on `small-get` and 8.4% on
`small-put` (4/4 paired wins, `pipeline_benchmark2/components/20260904-2313`) — the largest
single-component cost of the four bridges.

`V2ConfigTranslator.inert()` now drops an interceptor that provably cannot act, in two layers:

1. **General:** an interceptor overriding none of the six hooks the bridge invokes is dropped. It was
   already silently ineffective (see the 12-hook list below); this stops charging for it.
   `HelpfulUnknownHostExceptionInterceptor` falls out here, since it implements only `modifyException`.
2. **Named, scope-specific:** `EventStreamInitialRequestInterceptor` gates both its hooks on
   `HAS_INITIAL_REQUEST_EVENT`, which only an async event-stream operation sets — inert for sync clients,
   and **this drop must be revisited if async comes into scope** (10.2). `TraceIdExecutionInterceptor`
   gates all four of its hooks on the `AWS_LAMBDA_FUNCTION_NAME` environment variable, which cannot
   change while the JVM runs, so it is dropped only when that variable is unset.

The compatibility consequence is narrow but real: **the inert-ness is judged once, at client
construction, from the class and the environment** rather than per call. If a future v2 release gives
one of those two named classes an effect outside the gate the filter assumes, a bridged client silently
loses it. `awssdk.bridge.keepInertInterceptors=true` restores the unfiltered behavior, and the
`v2-sync-inert-interceptors` benchmark arm uses it to measure what the filter recovers. Nothing else
changes: with the filter on, a default DynamoDB client installs no interceptor bridge at all —
`V2InterceptorBridge` is not even class-loaded — which is what the lazy install was supposed to achieve.

Measured recovery (`pipeline_benchmark2/inertfilter/20260904-2355`): restoring the old behavior costs
7.4% of app CPU on `small-get` and 8.4% on `small-put` (0/4 paired wins), and stripping the interceptor
bridge now buys +0.7%/+0.0% — nothing, because nothing is left to strip. A customer interceptor
overriding a bridged hook still runs, verified directly.

Six of v2's eighteen hooks are invoked, mapped onto four smithy hooks:

| smithy `ClientInterceptor` | v2 `ExecutionInterceptor` |
|---|---|
| `readBeforeExecution` | `beforeExecution` |
| `modifyBeforeSerialization` | `modifyRequest` |
| `modifyBeforeSigning` | `modifyHttpRequest`, `modifyHttpContent` |
| `readAfterExecution` | `afterExecution` *or* `onExecutionFailure` |

`modifyHttpRequest` maps to `modifyBeforeSigning` so that a header an interceptor adds still ends up
inside the signature, as in v2.

The other **12 hooks are never invoked**: `beforeMarshalling`, `afterMarshalling`,
`modifyAsyncHttpContent`, `beforeTransmission`, `afterTransmission`, `modifyHttpResponse`,
`modifyHttpResponseContent`, `modifyAsyncHttpResponseContent`, `beforeUnmarshalling`,
`afterUnmarshalling`, `modifyResponse`, `modifyException`. Notably, an interceptor that rewrites the
response (`modifyResponse`) or the exception (`modifyException`) is silently inert.

Residual differences in the hooks that *are* bridged:

- **`modifyHttpRequest` does not see the resolved endpoint.** smithy resolves the endpoint *after*
  `modifyBeforeSigning` (`ClientPipeline.doSendOrRetry` calls the hook, then `afterIdentity` does
  `resolveEndpoint` + `ClientProtocol.setServiceEndpoint`), so the request smithy hands the bridge
  carries its unresolved placeholder URI: no scheme, no host, path `/`. A v2
  `SdkHttpFullRequest` cannot even be *built* without a protocol, so the bridge substitutes the
  **client endpoint** (`CLIENT_ENDPOINT_PROVIDER.clientEndpoint()`, falling back to
  `https://unresolved.invalid`). That equals the resolved endpoint for a client with an
  `endpointOverride`, and differs from it whenever the rules engine rewrites the host — account-ID
  endpoints, FIPS, dual-stack, host prefixes. An interceptor that inspects the host, or that keys off
  it (region sniffing, per-endpoint routing decisions, logging), sees the wrong value.
- **Host/scheme/port changes made by an interceptor are silently discarded.** `setServiceEndpoint`
  runs after the hook and overwrites scheme, host and port from the resolved endpoint, so only the
  path, query and headers written back survive. In v2, `modifyHttpRequest` *can* redirect a request to
  a different host; here it cannot, and fails silently rather than erroring.
- The path written back has to be de-prefixed by the bridge, because `setServiceEndpoint`
  concatenates the endpoint's path in front of it; an interceptor that rewrites the path to something
  not starting with the client endpoint's path prefix produces a different final path than in v2.
- **A `RequestBody` returned by `modifyHttpContent` is dropped.** smithy owns serialization and the
  body is already a `DataStream` by this point. The hook runs (v2's chain calls both together via
  `modifyHttpRequestAndHttpContent`) and its request modifications are kept, but body replacement —
  the whole point of the hook — has no effect.
- `ExecutionAttributes` passed to bridged hooks is synthesized from client config and holds four
  attributes only: `AWS_REGION`, `ENDPOINT_PREFIX`, `SERVICE_NAME`, `CLIENT_TYPE`, plus
  `OPERATION_NAME` per call. Everything else v2 populates is absent — metric collectors, checksum
  specs (`RESOLVED_CHECKSUM_SPECS`), the auth-scheme attributes (`SELECTED_AUTH_SCHEME`,
  `SIGNING_REGION`, `SIGNER`), `CLIENT_ENDPOINT`, `RESOLVED_ENDPOINT`, `API_CALL_ATTEMPT_*`. An
  interceptor reading any of them sees `null`, and one *writing* an attribute for a later built-in to
  read is writing into a void.
- Attributes are not shared with anything else: v2's built-in interceptors are not in the chain
  (below), so cross-interceptor attribute conventions between customer and SDK code are broken.
- **`modifyHttpRequest` is invoked once per attempt instead of once per execution.** v2 calls it from
  `BaseClientHandler.finalizeSdkHttpFullRequest`, during marshalling and outside the retry loop;
  smithy's `modifyBeforeSigning` is inside it. An interceptor that appends rather than sets — a
  counter, a sequence number, a header it adds unconditionally — compounds across retries here where
  it did not in v2.
- `dynamodb-enhanced`'s `ApplyUserAgentInterceptor` is discovered and bridged, so the
  enhanced-client user-agent suffix is preserved.

### 2.2 `PARTLY RESOLVED` (§17.8) — `SdkPlugin`

> **Update.** Request-level plugins now run, through the generated client's own
> `updateSdkClientConfiguration` (so they see exactly what they see on stock), and the parts of the result
> smithy-java reads per call — endpoint resolution (region, endpoint provider, builtins) and identity — are
> rebuilt for that call. Measured: a plugin that changes the region signs for the new region, sync and async,
> as on stock. Not rebuilt per call: the transport and the retry strategy, which smithy fixes at client
> construction; a plugin that changes either is honored on stock and ignored on the bridge.


`overrideConfiguration()`/`addPlugin(SdkPlugin)` mutates `SdkServiceClientConfiguration.Builder` at
client build time. Client-level plugins still run (they operate on v2 config before the bridge reads
it), but **request-level** plugins do not, because the bridge never rebuilds its `ClientConfig`
per request.

### 2.3 `RESOLVED` (§17.8) — request-level `overrideConfiguration()`

> **Update.** Every field is honored, and `RequestOverridesTest` holds each one to stock's observed
> behavior on the sync and async clients (24/24 lines identical): headers, raw query parameters, API names,
> credentials, endpoint provider, auth-scheme provider, execution attributes, legacy signer, plugins, metric
> publishers, `apiCallTimeout`, `apiCallAttemptTimeout`. `compressionConfiguration` is carried but has no
> operation to act on in these two services (6.2). The text below describes the state before.


`AwsRequestOverrideConfiguration` on an individual request (credentials, interceptors, headers,
query params, API call timeouts, metric publishers, signer, plugins,
`putExecutionAttribute`) is entirely ignored. smithy has an equivalent concept
(`RequestOverrideConfig` on `Client.call`), so this is bridgeable in principle; it is simply not
wired here — `SmithyBridgeClient.invoke` passes `null` for it.

Concretely: codegen still emits `updateSdkClientConfiguration(SdkRequest, SdkClientConfiguration)`,
the method that folds request-level overrides into a per-request config, but on the smithy path
**nothing calls it** (one definition, zero call sites in `DefaultDynamoDbClient`; the async client
calls it per operation). The one piece of request override configuration that does survive is metric
publishers, because the generated method resolves them before delegating — see 7.1.

### 2.4 `RESOLVED` (§17.8) — `executionAttributes()` on requests

> **Update.** A request's execution attributes reach the interceptor chain and the endpoint/signing
> attributes, with request precedence, as on stock.


No `SdkRequest`-level execution attribute reaches the pipeline.

---

## 3. Retries

### 3.1 `BRIDGED` — retry strategy

`software.amazon.smithy.java:aws-sdkv2-retries`'s `SdkRetryStrategy.of(v2Strategy)` wraps v2's
resolved `RetryStrategy`. This preserves, exactly: `maxAttempts`, backoff strategy, the retry token
bucket, `RetryMode` (`LEGACY`/`STANDARD`/`ADAPTIVE`/`ADAPTIVE_V2`), the `new-retries-2026`
resolution, and DynamoDB's `DynamoDbRetryPolicy` overrides (8 retries pre-2026 / 4 attempts post,
25 ms base delay, exponential/full-jitter backoff).

The backoff half of that is **measured, not just asserted**. `javap` on the published jar shows
`refreshRetryToken` to be pure delegation — it rebuilds a v2 `RefreshRetryTokenRequest` from
(token, failure, suggestedDelay), calls the wrapped v2 strategy, and returns its `delay()` verbatim;
`acquireInitialToken` and `maxAttempts` likewise. So delay *computation* is v2's in both arms and
cannot differ by construction. Empirically, with an unjittered 200 ms backoff pinned on both the
throttling and non-throttling paths, every 3-attempt case takes **203.0 ms per extra attempt in both
arms** (`pipeline_benchmark2/errors/20260905-1527`). A dropped, halved or defaulted backoff would be
a flat offset and is excluded. Scope: the probe pins `RetryMode.STANDARD`, so the measurement covers
the standard strategy. The `DynamoDbRetryPolicy` and `RetryMode` claims above still rest on the
delegation argument alone, and the deprecated `retryPolicy(...)` path is a separate defect (3.4).

Note: `aws-sdkv2-retries` declares `software.amazon.awssdk:retries-spi:2.52.0`. The bridge pom
excludes that transitive dependency so the in-tree `${awsjavasdk.version}` is used instead.

Cost on a call that succeeds: **nothing measurable.** Swapping the wrapper for smithy's own
`StandardRetryStrategy` (`awssdk.bridge.stripRetries`) moves app CPU by −0.4% / −0.3% on small
operations — inside the ±3% noise floor, and only 3/4 and 2/4 paired wins
(`pipeline_benchmark2/components/20260904-2313`). Same for the error enricher (1.3, 3.2): +0.1% /
−0.4%, i.e. unmeasurable, which is expected since it only does work on the error path.

Cost on a call that *does* retry, with the backoff sleep removed so wall time is the client's own
work (`pipeline_benchmark2/errors/20260905-1532`, 60 reps): the marginal cost of one extra attempt is
**675 µs baseline vs 578 µs bridge**, against a warm 1-attempt call of 1,110 vs 669 µs. The bridge is
cheaper on both, but note the ratios — **0.60x for the one-time work and only 0.86x per attempt**, so
roughly a fifth of the bridge's per-call advantage recurs on a retry and its relative advantage
decays with attempt count. The likely cause is 3.3, not this section.

### 3.2 `DEGRADED` — retry *classification* is recomputed, not reused

`SdkRetryStrategy.of()` deliberately **replaces** the delegate's `retryOnException` and
`treatAsThrottling` predicates with ones that read smithy's `RetryInfo`. v2's own classification
(`RetryOnExceptionsCondition`, `RetryOnStatusCodeCondition`, `RetryOnClockSkewCondition`,
`AwsRetryStrategy`'s error-code lists) is therefore **not consulted**.

This is a sharper constraint than "the predicates are replaced": `SdkRetryStrategy`'s substitute
predicate is `getInfo(t) != null && getInfo(t).isRetrySafe() == RetrySafety.YES`. Not-`YES` is not
retried, and `RetrySafety.MAYBE` — the default on a bare `CallException` — is not `YES`. **Any error
nothing explicitly classifies is silently non-retryable.**

On this branch `V2RetryClassification` repopulates `RetryInfo` from a v2 exception using v2's own
predicates (`isThrottlingException()`, `isClockSkewException()`, retryable status codes,
`RetryableException`, `IOException`), which matches v2's *default* classification. It is applied on
three paths, all of which had to exist before the behavior matched:

- `V2ModeledError`, at construction and again after `enrich` (the AWS error code is what makes a
  throttling error retryable, and that is only known once the response is in hand).
- `V2UnmodeledError`, whose v2 exception carries the real status code — this is what makes a 503 or an
  empty 500 retryable (1.3).
- Anything else, classified in place by `V2ErrorEnricher`: `RetrySafety.YES` if the cause chain reaches
  an `IOException`/`UncheckedIOException`, or — when a response was received — a smithy
  `SerializationException`. Not forced to `NO` otherwise, since a bridge failure of unknown origin is
  not evidence that retrying is unsafe. The second clause exists because of 3.5.

**Measured** (`pipeline_benchmark2/errors/20260905-0138`, before the second and third paths existed):
of nine injected faults, the bridge retried **two**. `internal-error` (500), `unavailable` (503),
`unavailable-retry-after`, `empty-500`, and `malformed-body` all returned on the first attempt where v2
made three — and in the transient variants, where the third attempt would have succeeded, the bridge
threw and v2 returned the item. That is the most severe finding in this ledger: a working-looking client
that does not retry server errors.

**Re-measured** with all three paths in place (`pipeline_benchmark2/errors/20260905-0159`): the whole
diff fell from 87 behavioral differences to 19, and eight of the nine faults now retry exactly as v2
does — same attempt count on every persistent case, and every transient case recovers on the third
attempt in both arms. `malformed-body` was the one holdout, for the reason in 3.5.

**Re-measured again** once 3.5 was fixed (`pipeline_benchmark2/errors/20260905-0223`): **attempt counts
identical on all 21 cases** then in the catalogue, with zero variation across reps. What remained was 14
behavioral differences plus 12 message-wording ones, all metadata or text — `rawResponse` never populated,
the `(SDK Attempt Count: N)` message suffix absent, the 1.3 type mismatch, and the cause type in 3.5 — none
of which changes whether or how often a call is retried. (Counts throughout this ledger are
`error_behavior_diff.py`'s, which reports message text separately from behavior; the sum of the two is the
"26 field differences" figure an earlier revision quoted here.)

Adding a fault the catalogue had been missing then found the one case that still differs, and it is not a
classification problem at all: a connection reset before response headers is retried three times by v2 and
once by the bridge, because smithy-java's retry loop is unreachable from the transport path. See 3.6. Every
*response* the service sends, well-formed or not, is now retried as v2 retries it; nothing that fails
before a response is retried at all.

What still differs:

- A customer-supplied `RetryStrategy` whose `retryOnException` adds custom conditions has those
  conditions silently discarded.
- `RetryPolicy` (the deprecated API) adapted via `RetryPolicyAdapter` — its `RetryCondition`s are
  likewise discarded. This includes `additionalRetryConditionsAllowed(false)`, which
  `DynamoDbRetryPolicy` sets on the `ADAPTIVE` path.
- Transport-level exceptions from `V2TransportBridge` are remapped by
  `ClientTransport.remapExceptions`, so what reaches the strategy is a smithy `TransportException`
  subtype, not the original v2 `SdkClientException`/`IOException`. This is worse than a lost type: the
  remapped types cannot be classified at all — see 3.6.

### 3.3 `DEGRADED` — endpoint and identity are re-resolved on every attempt

smithy's `ClientPipeline.doSendOrRetry` resolves the auth scheme, identity, and endpoint **inside**
the retry loop. v2 resolves the endpoint once per execution (in `modifyRequest`) and the identity
once per execution (in the auth-scheme interceptor). Retried calls therefore do extra work here, and
a resolver with side effects observes more invocations than under v2.

There is a cost signature consistent with this. On calls with the backoff removed
(`pipeline_benchmark2/errors/20260905-1532`) the bridge is 0.60x baseline on the once-per-execution
work but only 0.86x on each additional attempt — i.e. the advantage erodes in proportion to attempts,
which is the shape of once-per-execution work being paid per attempt. **This is a hypothesis the run
is consistent with, not one it isolates**; the run measures the aggregate. Memoizing the endpoint
bridge is the test, and 1532 is its before-measurement. A configuration allowing more than 3 attempts
would sharpen it further, since the gap should widen linearly.

### 3.4 `MISSING` — `RetryPolicy`-only configuration path

A client configured with the deprecated `overrideConfiguration().retryPolicy(...)` gets **none of
it**. v2 resolves `RETRY_STRATEGY` to a `RetryPolicyAdapter` wrapping the policy, and that adapter
cannot be bridged: `SdkRetryStrategy.of` rewires the strategy's predicates through its builder, and
`RetryPolicyAdapter.Builder` throws `UnsupportedOperationException` from exactly those setters. It
also needs a per-request `RetryPolicyContext` that smithy's pipeline never constructs.

`V2ConfigTranslator.resolveRetryStrategy` therefore returns `null` for a `RetryPolicyAdapter`, which
leaves smithy-java on its **own default `StandardRetryStrategy`**: 3 attempts (v2's `RetryPolicy`
default is 4), smithy's own backoff, and no `maxBackoffTime`/token-bucket setting the customer chose.
This is silent — no warning, no exception. `DynamoDbRetryPolicy`'s overrides (8 retries pre-2026,
25 ms base delay) are lost with it, so a `retryPolicy(...)`-configured DynamoDB client retries
*fewer* times and *faster* than v2.

`retryStrategy(...)` (the current API) is unaffected; see 3.1.

### 3.5 `FRAGILE` — v2 classifies a torn response body by its Jackson exception, which changed type in Jackson 3

v2's retryable set is `RetryableException`, `IOException`, `UncheckedIOException`,
`ApiCallAttemptTimeoutException`, matched against the exception **or any cause of it**
(`SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS`, applied via `retryOnExceptionOrCauseInstanceOf`). It
has no rule about malformed responses as such. A truncated response body is retried only as a
side effect of Jackson 2's hierarchy: `JsonEOFException` extends `JsonProcessingException` extends
`IOException`, so the parse failure lands in the retryable set on its own.

smithy-java parses with Jackson 3 (`tools.jackson`), where `JacksonException` extends
`RuntimeException`, and wraps the result in a `SerializationException` — also a `RuntimeException`.
So the two pipelines' cause chains for the *same* torn body are:

| | v2 | bridge |
|---|---|---|
| top | `SdkClientException` | `SdkClientException` |
| | `UncheckedIOException` | `TransportException` |
| | `JsonEOFException` (**an `IOException`**) | `SerializationException` |
| | | `SerializationException` |
| | | `UnexpectedEndOfInputException` (**a `RuntimeException`**) |

The chain walk that reproduces v2 for transport errors therefore found nothing to match, and
`SdkRetryStrategy`'s `YES`-only predicate (3.2) turned "not classified" into "not retried".
**Measured** (`pipeline_benchmark2/errors/20260905-0159`): 1 attempt against v2's 3, and in the
transient variant v2 returned the item while the bridge threw.

`V2ErrorEnricher.retriedByV2` now also treats a `SerializationException` as retry-safe **when a response
was received**, which reproduces v2's effective behavior (every unparseable response is retried) without
also retrying a request-serialization failure, which in v2 happens before the attempt and is never
retried. Two further changes were needed to make that reachable at all: the enricher used to return
early unless the error was a `CallException`, and a `SerializationException` is not one — it is a plain
`RuntimeException` carrying no `RetryInfo` — so the classification could not be stamped on it either.
That second half is the same defect as 3.6 and uses the same `V2RetryableError` substitution.

**Re-measured** (`pipeline_benchmark2/errors/20260905-0223`): 3 attempts against v2's 3, and the
transient variant recovers in both arms. What still differs is only the cause type — v2's
`UncheckedIOException` wrapping `JsonEOFException` against the bridge's `SerializationException` — and
the parser's message text. Both arms throw `SdkClientException`.

Filed as `FRAGILE` rather than `DEGRADED` because the mapping is a coincidence on both sides: v2's
behavior here is emergent from a third-party class hierarchy rather than intended, and the bridge's
replacement is pinned to smithy's own exception type. If either codec's exception hierarchy changes,
this silently diverges again, in whichever direction. The general shape — v2 classifying retries off
concrete exception *types* from a JSON library that smithy-java does not use — applies to anything
else that reaches v2's retryable set by inheritance rather than by declaration.

### 3.6 `FIXED in the bridge`, `UPSTREAM` (§17.9) — transport failures are never retried

> **Update.** No longer blocked. A bridged transport now *defers* its failure into the retry loop instead
> of throwing past it: it records the failure against the attempt and returns a marked stand-in response,
> which deserialization turns into an attempt error, and `V2ErrorEnricher` swaps the real failure back in and
> classifies it as v2 would (`V2DeferredTransportFailure`). Measured by the fault sweep: `connection-reset` now makes
> as many attempts as stock and differs only in the name of the cause, and an attempt timeout is retried four
> times, as on stock. The analysis below of *why* smithy-java cannot retry these stands, and the fix still
> belongs upstream — this is the argument that it is a one-throw change.


`CallException` holds retry safety in a mutable field with a setter, which is the mechanism the whole
retry bridge depends on: `V2RetryClassification` and `V2ErrorEnricher` stamp v2's verdict onto the
error, and `ClientPipeline` reads it back through `RetryInfo`. `TransportException` **overrides the
getter with a constant**, discarding that field:

```java
// TransportException (smithy-java 1.6.1); ConnectTimeoutException repeats it verbatim
public RetrySafety isRetrySafe() {
    return RetrySafety.NO;
}
```

`V2TransportBridge` funnels every `IOException` from the v2 HTTP client through
`ClientTransport.remapExceptions`, and **every one of that method's six targets descends from
`TransportException`**:

| `IOException` from the v2 HTTP client | smithy type | `isRetrySafe()` |
|---|---|---|
| `ConnectException` | `ConnectTimeoutException` | `NO` (own override) |
| `SocketTimeoutException` | `TransportSocketTimeout` → `TransportSocketException` | `NO` (inherited) |
| `SocketException` | `TransportSocketException` | `NO` (inherited) |
| `SSLException` | `TlsException` → `TransportProtocolException` | `NO` (inherited) |
| `ProtocolException` | `TransportProtocolException` | `NO` (inherited) |
| anything else | `TransportException` | `NO` (own override) |

So the setter writes a field nothing reads, and combined with `SdkRetryStrategy`'s `YES`-only predicate
(3.2) the result is categorical: **no error of any of those types is retryable in a bridged client**,
while stock v2 retries every one of them — `SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS` contains
`IOException`, matched against the whole cause chain.

**How much this covers is narrower than it looks, and measurement is what established that.**
`V2TransportBridge.send` wraps only `prepareRequest(...).call()` in its `catch (IOException)`, and that
returns as soon as the response *headers* arrive: bodies are streamed on as a `DataStream`, drained later
by the codec. So the remapping — and this defect — is reachable only for failures during **connect, TLS
handshake, request send, or response-header read**. Anything that fails once a response has begun
arriving, including a torn response body, never becomes a `TransportException` at all; it surfaces as
whatever the stream or codec threw, and is handled by 3.5's rule instead. See
`errors/20260905-0230`, where a truncated body produced an identical `UncheckedIOException` cause and an
identical 3 attempts in both arms.

That narrower surface is not a reprieve. A connection reset before headers is the most common transport
failure in production — an idle pooled connection reaped by a load balancer is exactly this — and connect
timeouts are the next.

**The hardcoded getter is the symptom. The cause is that the retry loop is inside `deserialize`.**
Trying to fix this from the bridge is what established that, and the attempt is worth recording because
the obvious mitigation looks correct and is inert.

Everywhere else in section 3, a failure that smithy will not classify can be replaced by one that will:
`V2ErrorEnricher` throws a `V2RetryableError` — a `CallException` that returns `YES` — carrying the
original, which the client boundary unwraps once retries are over. It detects the need by *writing then
reading back* `isRetrySafe` rather than by naming the offending classes, so a new upstream type with the
same override is handled without a code change. That is what fixes 3.5.

It cannot work here, for two reasons, and the second is fatal:

1. The enricher never runs. It hooks `modifyBeforeAttemptCompletion`, and `ClientPipeline` invokes that
   in exactly one place — inside `deserialize`. `afterIdentity` wraps both calls in one try:

   ```java
   try {
       ResponseT response = transport.send(context, request);
       return deserialize(call, request, response, interceptor);
   } catch (Exception e) {
       throw ClientTransport.remapExceptions(e);   // a send failure skips deserialize entirely
   }
   ```

2. **Neither does the retry loop.** `refreshRetryToken` and the recursive `retry(...)` call live inside
   `deserialize` too, in the `catch (RuntimeException)` around `deserializeResponse`. `doSendOrRetry`, in
   spite of its name, contains no exception handler at all, and neither does anything above it. So the
   only code path that can produce a second attempt is one that got far enough to have a response to
   deserialize.

Which means **smithy-java 1.6.1 does not retry transport failures at all** — not because they are
classified `NO`, but because nothing on that path ever consults the classification. The hardcoded getter
is consistent with the design rather than an oversight in it. A native smithy-java client has the same
behavior; this is not a bridging artifact.

The bridge has no fix. `V2TransportBridge.send` throwing a `V2RetryableError` instead of the remapped
exception was implemented and measured: **no change, 1 attempt**, because there is no handler between
that throw and the caller. It was reverted, and the catch clause now carries a comment saying so, since
it is exactly the change a later reader would try. The remaining options are all above the pipeline —
wrapping `Client.call` in a retry loop of the bridge's own, which would duplicate the strategy, the token
bucket, and the attempt accounting — and none belong in a compatibility layer.

`BLOCKED`, and the most consequential entry in this ledger for a real deployment.

`BLOCKED` because the underlying defect is upstream: a transport error that cannot be marked retryable
is not a property a bridge should have to work around, and the fix belongs in smithy-java — either drop
the overrides or make the constructors take a `RetrySafety`. Until then any smithy-java client, bridged
or native, has the same behavior.

**Measured** (`pipeline_benchmark2/errors/20260905-0239`) with the `connection-reset` fault, added to
`Faults` for this: the server closes the socket with no response at all, which per the paragraph above is
the only way to make the failure land inside the transport. Attempt counts come from the server's own
request counter, since a transport failure yields an exception with no attempt number on it.

| | v2 | bridge (before the fix) |
|---|---|---|
| attempts, persistent | 3 | **1** |
| attempts, transient (3rd would succeed) | 3 | **1** |
| outcome, transient | **ok**, item returned | **throw** |
| wall, persistent | 131 ms | 2 ms |

The transient row is the whole finding: v2 recovers and returns the item where the bridge throws. The 2 ms
wall is the same fact as a timing — no backoff, because no second attempt.

---


### 3.7 `FIXED in the bridge`, `UPSTREAM` — one interceptor that does not override the error-path hook disables every interceptor after it

Found when a harmless new interceptor silently stopped every 5xx from being retried. smithy-java 1.6.1's
`ClientInterceptorChain.modifyBeforeAttemptCompletion` loops over the interceptors with no try/catch, and
`ClientInterceptor`'s default implementation is `hook.forward(error)`, which **rethrows**. So on the error
path the first interceptor that does not override the hook ends the loop, and no interceptor after it sees
the error. Placing `V2Crc32Validation` (which overrides only `modifyBeforeDeserialization`) ahead of
`V2ErrorEnricher` meant the enricher never ran: no v2 classification, no retry, the fallback exception type.
Nothing failed loudly; `DynamoDbBehaviorTest` caught it because it is the one test whose transport fails an
attempt.

Worked around by ordering — the enricher is the first client interceptor, with the reason at the
installation site — and pinned by `DynamoDbBehaviorTest`. The same shape applies to `modifyBeforeCompletion`.
Upstream fix: the chain should catch and carry the error forward, or the default should return the error
rather than throw it; as it stands, adding an interceptor to a smithy-java client can change its retry
behavior, which no interceptor author would expect.

## 4. Authentication and credentials

### 4.1 `TRANSLATED` — auth scheme resolution

v2 resolves auth schemes with a generated `ExecutionInterceptor` driven by an
`AuthSchemeProvider` and the model's `@auth` traits, and can select sigv4a, bearer, or `noAuth`
per operation. The bridge installs smithy's `SigV4AuthScheme` and lets
`AuthSchemeResolver.DEFAULT` pick from `ApiOperation.effectiveAuthSchemes()`.

`ApiOperationSpec` currently hardcodes `SCHEMES = List.of(ShapeId.from("aws.auth#sigv4"))` for every
operation. Consequences:

- A customer-supplied `authSchemeProvider(...)` has no effect.
- `@optionalAuth` / `@auth([])` operations are still signed.
- sigv4a is not available; a service or endpoint requiring it would fail. (Not reachable for
  DynamoDB.)
- Endpoint-driven auth-scheme overrides *are* honored, because smithy's
  `applyEndpointAuthSchemeOverrides` reads `Endpoint.authSchemes()` — but the bridged endpoint
  resolver does not translate v2's `AwsEndpointAttribute.AUTH_SCHEMES` into smithy
  `EndpointAuthScheme`s, so in practice they are dropped.

### 4.2 `BRIDGED` — credentials

`V2IdentityResolver` adapts v2's `IdentityProvider<? extends AwsCredentialsIdentity>` to smithy's
`IdentityResolver<AwsCredentialsIdentity>`. This is deliberately bridged rather than translated:
smithy-java's `aws-credential-chain` ships only environment-variable and system-property resolvers,
whereas v2's `DefaultCredentialsProvider` chain covers profile files, SSO, container credentials,
IMDS, process credentials, web identity, and STS. Preserving that chain is the whole point.

Residual: v2's `resolveIdentity(ResolveIdentityRequest)` properties are not forwarded; the bridge
calls the no-arg overload. Credential-scoped properties from an auth scheme (e.g. S3 Express)
therefore do not reach the provider.

### 4.3 `BRIDGED` (§17.2) — SigV4 signing

> **Update.** Signing is now v2's own `AwsV4HttpSigner`, driven with v2's signer properties from v2's own
> auth-scheme provider plus the endpoint's auth-scheme overrides, as the stock interceptors compose them
> (`V2SigningAuthScheme`). This answers open question 1 (signature equivalence) by construction, and it is
> what closes 12.8, 12.9, 13.2 and 13.3, all of which were behaviors of v2's signer that smithy-java's does
> not have. SigV4a and S3 Express session auth are offered by v2's provider but not implemented by the bridge;
> it signs with the SigV4 option. `awssdk.bridge.stripSigner` restores smithy-java's signer.


smithy's `SigV4Signer` replaces v2's `AwsV4HttpSigner`. Region and signing name come from
`SdkClientConfiguration` (`AwsClientOption.SIGNING_REGION`, `SERVICE_SIGNING_NAME`) into
`SigV4Settings.REGION` / `SIGNING_NAME`. This is the single largest allocation win measured on the
prototype.

Differences:

- `SdkAdvancedClientOption.SIGNER` and any customer-supplied `HttpSigner`/`AuthScheme` are ignored.
- v2's signer properties (`AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME`,
  `DOUBLE_URL_ENCODE`, `NORMALIZE_PATH`, `PAYLOAD_SIGNING_ENABLED`, `CHUNK_ENCODING_ENABLED`,
  `EXPIRATION_DURATION`) are not translated. Defaults happen to match for DynamoDB.
- The set of headers excluded from the canonical request is smithy's, not v2's. Any divergence shows
  up as a signature mismatch only if a customer adds one of the differing headers.

### 4.4 `MISSING` — anonymous credentials

`AnonymousCredentialsProvider` short-circuits signing in v2. The bridge always installs
`SigV4AuthScheme`, and `IdentityResolver` would return an `AnonymousIdentity` that
`SigV4Signer` cannot use.

---

## 5. Endpoints

### 5.1 `BRIDGED` — endpoint resolution

`V2EndpointResolverBridge` implements smithy's `EndpointResolver` by calling the **generated v2
rules engine**: `DynamoDbResolveEndpointInterceptor.ruleParams(request, executionAttributes)`
followed by `DynamoDbEndpointProvider.resolveEndpoint(params).join()`. This preserves
`endpointOverride`, FIPS, dual-stack, account-ID-based routing, `ResourceArn`-driven routing,
client context params, static/operation context params, and any customer-supplied
`EndpointProvider`.

The account-ID builtin works because smithy resolves the identity *before* the endpoint
(`ClientPipeline.afterIdentity`), so the bridge reads the resolved `AwsCredentialsIdentity` out of
`CallContext.IDENTITY` and synthesizes the `SelectedAuthScheme` that `ruleParams` expects.

Residual differences:

- The `ExecutionAttributes` handed to `ruleParams` is synthesized from client config and contains
  only what the rules engine needs. An `EndpointProvider` reading other attributes sees `null`.
- `AwsEndpointAttribute.AUTH_SCHEMES` returned by the rules engine is not translated (see 4.1).
- Endpoint headers (`Endpoint.headers()`, applied by v2's `modifyHttpRequest`) are dropped.
- Business metrics recorded during endpoint resolution go into a throwaway collection.
- `ENDPOINT_RESOLVE_DURATION` is not reported (no metrics — see 7).
- **Cost, not just behavior:** every resolution allocates an `ExecutionAttributes` copy (a `HashMap`),
  a `BusinessMetricCollection`, a v2 `AwsCredentialsIdentity` converted from smithy's, a
  `SelectedAuthScheme` holding an already-completed `CompletableFuture`, the generated rule-params
  builder and params object, the `CompletableFuture` that `resolveEndpoint` returns and that is
  immediately `join()`ed, and a v2 `Endpoint`. A single-entry memo keyed on the resolved URI keeps the
  smithy `Endpoint` itself from being rebuilt, which is the only part of this that steady state avoids.
  All of it is on the hot path and runs **once per attempt** rather than once per execution (3.3). The
  `awssdk.bridge.stripEndpoints` knob (10.6) exists to measure exactly this, and it comes to **4.0% of
  app CPU on `small-get`, 5.1% on `small-put`** (4/4 paired wins,
  `pipeline_benchmark2/components/20260904-2313`) — the second-largest of the four bridges, behind the
  interceptor bridge (2.1).

### 5.2 `MISSING` — endpoint discovery

DynamoDB's `customization.config` sets `enableEndpointDiscoveryMethodRequired: true`. The
`EndpointDiscoveryRefreshCache`, the `DescribeEndpoints` cache-fill call, and
`AwsEndpointProviderUtils.endpointIsDiscovered` short-circuit are all bypassed. A client built with
`endpointDiscoveryEnabled(true)` silently uses the regional endpoint.

The generated client still *creates* the `EndpointDiscoveryRefreshCache` in its constructor when
discovery is enabled (including via the `AWS_ENABLE_ENDPOINT_DISCOVERY` env var / profile property,
which the builder still resolves), but no operation ever consults it: the constructor holds the only
reference to the field. The knob is fully wired and completely inert.

### 5.3 `DEGRADED` — `smithy.rules#endpointBdd` ignored

The prototype codegen ignores the `endpointBdd` trait and relies on the C2J
`endpoint-rule-set.json` sidecar. Any service whose authoritative rules are BDD-only would resolve
differently. Not an issue for DynamoDB, where both forms exist and agree.

### 5.4 `RESOLVED` (§17.6) — host prefixes / `@endpoint` trait

> **Update.** The endpoint bridge applies the operation's host prefix after resolution, as the stock
> resolver interceptor does, honoring `DISABLE_HOST_PREFIX_INJECTION` (the generated `hostPrefix` is exposed
> for bridged services). `write-get-object-response` matches stock byte for byte: the request goes to
> `route-1.s3.us-east-1.amazonaws.com`.


Neither smithy's `HostLabelEndpointResolver` (no traits on generated schemas, see 1.2) nor the
bridged v2 path (`AwsEndpointProviderUtils.addHostPrefix` lives in the interceptor's
`modifyRequest`, not in `ruleParams`) applies host prefixes. No DynamoDB operation uses one.

---

## 6. Checksums, compression, and content encoding

### 6.1 `RESOLVED` (§17.3, §17.5) — CRC32 response validation

> **Update.** Both forms. DynamoDB's `x-amz-crc32` is validated with v2's own `Crc32Validation`
> (`V2Crc32Validation`), on sync *and* async clients, unretried on mismatch — all three as measured on stock,
> which contradicted the source reading that async does not validate. S3's flexible response checksums are
> validated by v2's own `HttpChecksumValidationInterceptor`, now reachable through the bridged response hooks;
> `S3ResponseChecksumTest` holds 12 cases to stock (one named exception-type difference, 13.4).


DynamoDB sets `calculateCrc32FromCompressedData: true`. v2 validates the `x-amz-crc32` response
header against the received body and throws on mismatch. smithy-java's `HttpChecksumPlugin` covers
request checksums for `@httpChecksum`-modeled operations; nothing validates DynamoDB's legacy CRC32
response header. **Corrupted responses are accepted silently.**

### 6.2 `MISSING`, metadata carried — request compression

> **Update.** The `requestCompression` metadata now reaches the call's attributes (`V2OperationMetadata`),
> but v2 compresses in a pipeline *stage* (`CompressRequestStage`), not an interceptor or the signer, so nothing
> on the bridge acts on it. No operation in DynamoDB or S3 is affected; a service that uses it would need the
> stage's logic ported, which is small.


`RequestCompressionTrait` / `@requestCompression` is not applied. smithy has
`RequestCompressionPlugin` but it is trait-driven, and generated schemas have no traits (1.2).
No DynamoDB operation is affected.

### 6.3 `MISSING` — `gzip` response decoding negotiation

v2's DynamoDB client sends `Accept-Encoding: gzip` and decodes. The smithy path does not.

---

## 7. Metrics

### 7.1 `MISSING` — all of v2's metrics

`MetricPublisher`, `CoreMetric` (`API_CALL_DURATION`, `SERVICE_CALL_DURATION`, `RETRY_COUNT`,
`ENDPOINT_RESOLVE_DURATION`, `MARSHALLING_DURATION`, `SIGNING_DURATION`, `CREDENTIALS_FETCH_DURATION`,
`AVAILABLE_CONCURRENCY`, …) and `HttpMetric` are not collected or published.

Worse than silence: the generated operation method still resolves the configured publishers, creates
an `"ApiCall"` `MetricCollector`, reports `SERVICE_ID` and `OPERATION_NAME` into it, and publishes it
in a `finally` block. So a customer with a CloudWatch publisher configured gets a metric record per
call containing **only those two dimensions and no measurements** — an empty datum rather than no
datum, which is harder to notice than an outright gap. It also means the publisher cost is still paid
on the hot path.

smithy-java has its own metrics story (`client-metrics-otel`); nothing translates between the two
vocabularies.

### 7.2 `MISSING` — business metrics / user-agent feature IDs

v2 stamps feature IDs into the `User-Agent` (`m/...`) from `SdkInternalExecutionAttribute.BUSINESS_METRICS`.
smithy has its own `CallContext.FEATURE_IDS`, but the two vocabularies are unrelated and nothing
translates between them. The emitted user agent will not match v2's, which affects AWS-side
telemetry and any customer parsing it.

---

## 8. Timeouts

### 8.1 `RESOLVED` (§17.8) — `apiCallTimeout`

> **Update.** Implemented in the bridge (`V2Timeout`), client- and request-level, the way v2's timeout
> stage does it: a timer that aborts the in-flight request and interrupts the wait, then v2's
> `ApiCallTimeoutException`. Matches stock on sync and async against a stalled server.


v2 enforces a whole-call deadline with a scheduled interrupt (`ApiCallTimeoutTracker`). smithy-java
has no equivalent. A hung call is bounded only by the HTTP client's socket timeout.

### 8.2 `RESOLVED` (§17.8) — `apiCallAttemptTimeout`

> **Update.** As 8.1, per attempt, in the transport bridges; the aborted attempt is retried, because transport
> failures now reach the retry loop (3.6). Stock and bridge both make four attempts and surface
> `ApiCallAttemptTimeoutException`.


Same, per attempt.

Both are configured on `overrideConfiguration()` and are commonly used in production. This is the
most likely of the `MISSING` items to be a launch blocker.

---

## 9. Configuration surface not translated

Read once from `SdkClientConfiguration` and honored: region, signing name, endpoint override,
`SYNC_HTTP_CLIENT`, credentials provider, retry strategy, execution interceptors, endpoint provider.

Silently ignored:

| v2 configuration | Notes |
|---|---|
| `SdkAdvancedClientOption.SIGNER` | **honored** now: a client-level legacy signer replaces SigV4 (§17.8) |
| `SdkAdvancedClientOption.USER_AGENT_PREFIX` / `USER_AGENT_SUFFIX` | **honored** now, around smithy's own agent string (§17.8) |
| `SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION` | **honored** now (5.4) |
| `overrideConfiguration().headers(...)` / `putHeader(...)` | **honored** now, merged as stock merges them (§17.8) |
| `overrideConfiguration().compressionConfiguration(...)` | see 6.2 |
| `overrideConfiguration().scheduledExecutorService(...)` | only used by timeouts (8) |
| `overrideConfiguration().defaultProfileFile/Name` | used indirectly via `RetryMode` resolution only |
| `dualstackEnabled` / `fipsEnabled` | **honored**, via the bridged rules engine (5.1) |
| `accountIdEndpointMode` | **honored**, via the bridged rules engine (5.1) |
| `responseChecksumValidation` / `requestChecksumCalculation` | **honored** now, by v2's signer and interceptors (6.1, 12.9) |
| `SdkClientOption.API_CALL_ATTEMPT_TIMEOUT`, `API_CALL_TIMEOUT` | **honored** now (8) |
| `CRC32_FROM_COMPRESSED_DATA_ENABLED` | **honored** (6.1) |
| `overrideConfiguration().retryPolicy(...)` | **silently downgraded** to smithy's default strategy — see 3.4 |
| `overrideConfiguration().metricPublishers(...)` | publisher runs, but the record is empty — see 7.1 |
| `endpointDiscoveryEnabled` | resolved and stored, never consulted — see 5.2 |
| request-level `overrideConfiguration()` | **honored**, every field — see 2.3 |

---

## 10. Structural / build-level

### 10.1 `BLOCKED` — Java baseline

smithy-java 1.6.1 requires **JDK 21** (sealed interfaces, records, pattern matching in the runtime
it ships). The AWS SDK for Java v2 supports **Java 8**. Adopting smithy-java as v2's internals would
be a breaking platform change for the entire SDK, independent of any behavioral issue in this
ledger. This branch builds at `jre.version=21`.

### 10.2 in scope since §16 — async clients

> **Update.** Built; see §15-16.


`SdkAsyncHttpClient` / `CompletableFuture` operations, and therefore
`DynamoDbAsyncClient`, are untouched. smithy-java's async client path exists but the transport
bridge here is sync-only.

### 10.3 partly fixed — streaming and event streams

Sync streaming input and output are now on the smithy path; see section 13. `usesSmithyPipeline` still
excludes **event streams** in both directions, which remain on the v2 pipeline: they need a duplex
frame codec and `@streaming` union handling, neither of which the bridge attempts.

Async streaming is out of scope with the rest of async (10.2).

### 10.4 prototype-only codegen shortcuts

Carried over from the earlier branches, not compatibility issues per se but they limit what can be
concluded: `ALLOW_UNKNOWN_TRAITS`, `-Dawssdk.codegen.skipValidation=true`, C2J sidecars for endpoint
rules / paginators / waiters.

### 10.5 `MISSING` — paginators, waiters, utilities

`*Paginator` types, waiters, and `DynamoDbClient.serviceClientConfiguration()` reflect the v2
configuration objects, not the smithy `ClientConfig`. Paginators call the same operation methods so
they work, but nothing reconciles the two configuration views.

### 10.6 measurement knobs that change behavior

`V2ConfigTranslator` reads six system properties at client construction. Five replace one bridge with
smithy-java's own component so that "what does bridging cost?" can be answered from one binary and one
code path; the sixth restores a behavior the bridge now optimizes away, for the same reason.

| Property | Effect |
|---|---|
| `awssdk.bridge.stripAll` | implies all of the strip properties below |
| `awssdk.bridge.stripEndpoints` | fixed client endpoint instead of the bridged rules engine (5.1) |
| `awssdk.bridge.stripRetries` | smithy's `StandardRetryStrategy` instead of the configured v2 strategy (3.1) |
| `awssdk.bridge.stripInterceptors` | v2 interceptors not run at all (2.1) |
| `awssdk.bridge.stripErrorEnricher` | no AWS error code / request ID / status code on exceptions, and therefore no v2-equivalent retry classification (1.4, 3.2) |
| `awssdk.bridge.keepInertInterceptors` | bridge interceptors that provably cannot act, instead of dropping them (2.1) |

These are **not** a supported feature and they are listed here because they are behavior-changing: a
stripped client is a different SDK, not a faster one. They are read once per client construction,
never on the call path. The benchmark's `v2-sync-stripped` arm sets `stripAll`, and
`v2-sync-inert-interceptors` sets `keepInertInterceptors`.

Also worth recording: the generated v2 response/error handlers
(`createErrorResponseHandler`, the protocol factory's `createResponseHandler`) are still emitted but
unused for smithy-path operations — dead code that makes the generated client look more v2-like than
it is.

### 10.7 `DEGRADED` — client construction loads both pipelines, including `smithy-model`

Measured cold start (`pipeline_benchmark2/coldstart/20260904-2341`): building a `DynamoDbClient` takes
**507 ms on stock v2 and 603 ms here (+22%)**, which is the whole of the bridged client's +10% time to
first response — its first *call* is 20% faster than v2's. For a Lambda or a CLI making a handful of
calls, this is the tradeoff that matters, and it goes the wrong way.

`-verbose:class` on one JVM of each: 4 534 classes loaded versus 4 354, i.e. 534 loaded that stock v2
does not load and 385 no longer loaded. The largest bridge-only groups:

| classes | package | what it is |
|---:|---|---|
| 100 | `smithy.java.internal` | smithy runtime internals |
| 80 | `smithy.java.client` | the client pipeline |
| **114** | **`smithy.model.shapes` + `smithy.model.traits`** | **the Smithy *model* library** |
| 56 | `smithy.java.core` | schemas, serde |
| 46 + 26 | `smithy.java.json`, `awssdk.bridge.smithyjava` | protocol, the bridges themselves |

The 114 `smithy.model` classes are the interesting ones: the generated `SCHEMAS` constants build
`ShapeId`s and trait objects in static initializers, so a *runtime* client drags in the Smithy
model/traits library — machinery meant for build-time model processing. That is a plausible target for
the construction cost, and it is a dependency-surface question as much as a performance one.

Not attributed further here: whether the +95 ms is dominated by class loading, static initialization,
or the bridge objects. A JFR class-load recording on a single JVM would settle it.

---

## 11. Protocol layer

### 11.1 `DEGRADED` — the awsJson target header comes from the *generated service shape name*

`AwsJsonProtocol.createRequest` builds `X-Amz-Target` as
`serviceShapeId.getName() + "." + operationShapeId.getName()`. There is no `@awsJson1_0` target-prefix
trait involved and no way to override it: the header is a pure function of the two generated shape
names.

That makes a codegen naming choice load-bearing on the wire. The first version of `ApiServiceSpec`
used the SDK's Java-friendly service name and produced `X-Amz-Target: DynamoDb.GetItem`, which
DynamoDB answers with **HTTP 400 for every operation**. The fix was to carry the C2J
`metadata.targetPrefix` through `Metadata`/`AddMetadata` and name the service shape after it, so the
generated shape is `com.amazonaws.dynamodb#DynamoDB_20120810`.

Consequences to keep in mind:

- The service shape *name* cannot be chosen for readability; for any awsJson service it must equal
  `targetPrefix`, which for most services does not look like a shape name at all.
- Operation shape names must equal the C2J operation names exactly. Any codegen renaming
  (`customization.config` `renameShapes`, `verbNameOverrides`, or the operation-name sanitizing v2 does
  elsewhere) would silently change the target header.
- The failure mode is a blanket 400 with no hint about the cause, and it is invisible to any test that
  does not talk to a server that validates the target — the mock server in
  `test/standalone-e2e-benchmarks` catches it only because it routes on `X-Amz-Target` and 400s unknown
  targets.
- Services with no `targetPrefix` (non-awsJson protocols) fall back to the service name; harmless
  today because the header is unused there, but it is a latent trap if another protocol is added.

---
---

## 12. rest-xml and S3

Everything in this section was found by `test/wire-diff` on the six non-streaming S3 cases in
`S3Cases`. Entries marked **fixed** were repaired in `SdkSchemaFactory` during this pass and are
recorded anyway, because each was a *silent* wrong-bytes bug and the class of mistake will recur for
the next protocol.

### 12.1 fixed (was `BLOCKED`) — flattened lists gained a `<member>` wrapper, in both directions

v2 records flattening on the container (`ListTrait.isFlattened()`, `MapTrait.isFlattened()`); smithy
records it on the member (`@xmlFlattened`). `SdkSchemaFactory` never translated it, so every flattened
XML list was wrong on the wire *and* unreadable off it:

```
stock v2:  <CompleteMultipartUpload><Part>..</Part><Part>..</Part></CompleteMultipartUpload>
bridge:    <CompleteMultipartUpload><Part><member>..</member><member>..</member></Part></...>
```

The read side failed loudly — `ListObjectsV2` died with
`Expected list item 'member' but found 'Key'` while parsing `<Contents>` — but the write side did not.
S3 would have answered `MalformedXML` in production and the harness is what turned that into a
one-line diff.

### 12.2 fixed (was `BLOCKED`) — list and map element names fell back to smithy's defaults

v2 puts the element's XML name on the container too (`ListTrait.memberLocationName`,
`MapTrait.keyLocationName`/`valueLocationName`). Untranslated, a non-flattened list serialized its
items as `<member>`:

```
stock v2:  <TagSet><Tag><Key>env</Key>..</Tag></TagSet>
bridge:    <TagSet><member><Key>env</Key>..</member></TagSet>
```

Fixed by emitting `@xmlName` on the list's `member` / the map's `key` and `value`. Note this is a
*second*, independent gap from 12.1 — `Tagging.TagSet` is not flattened but does rename its item, and
`CompletedMultipartUpload.Parts` is flattened; a fix for either one alone leaves the other broken.

### 12.3 fixed (was `BLOCKED`) — synthetic nested `ShapeId`s reached the wire as XML element names

`SdkSchemaFactory` minted nested structure shape ids as `<memberName> + "Struct"`, on the documented
assumption that "synthetic ids never affect wire output" — element names come from
`putMember(memberName, ...)`. That holds for JSON. It does not hold for XML: the root element of an
`@httpPayload` structure is named from the **target shape's id**, so `PutObjectTagging` went out as

```
stock v2:  <Tagging><TagSet>..</TagSet></Tagging>
bridge:    <TaggingStruct><TagSet>..</TagSet></TaggingStruct>
```

Fixed by reading the nested shape's real id off its generated `$SCHEMA` (via `ShapeBuilder.schema()`),
falling back to the synthetic id when that returns null — which is what a self-referential shape
reports while its own static initializer is still running, i.e. the DynamoDB `AttributeValue` case.

The same change removes a latent collision: the synthetic id keyed only on the member name, so two
distinct shapes reached through same-named members in different sub-trees shared one `inProgress`
entry and the second silently inherited the first's schema.

### 12.4 fixed (was `BLOCKED`) — a codegen member rename unbound its URI label

`@httpLabel` carries no name; smithy matches a label to a member **by member name**. v2's member name
is the Java-facing one and `customization.config` is free to change it. S3 renames CopyObject's
`Bucket`/`Key` to `DestinationBucket`/`DestinationKey`, so `/{Key+}` had nothing to bind to and every
CopyObject threw before a single byte was sent:

```
java.lang.IllegalStateException: URI label `Key` not set for com.amazonaws.s3#CopyObjectRequest
```

Fixed by registering PATH/GREEDY_PATH members under their `locationName` instead of their member name.
Safe only because a label member is never also a body member; header and query members keep the v2
member name because their wire name travels in `@httpHeader`/`@httpQuery`.

This is the rest-xml twin of 11.1: another place where a codegen naming choice is silently load-bearing
on the wire, with no test in the SDK that would catch it.

### 12.5 fixed (was `MISSING`) — header maps were dropped entirely

v2 spells a header map as a HEADER-located **map** whose `locationName` is the shared prefix
(`x-amz-meta-`). `SdkSchemaFactory` gave it a plain `@httpHeader`, and smithy wrote **nothing** — every
user-metadata entry on CopyObject/PutObject/CreateMultipartUpload vanished with no error on either
side. Fixed by emitting `@httpPrefixHeaders` when a HEADER-located member is a map.

Worth noting as the worst failure shape in this whole ledger: silent data loss on a documented,
commonly-used feature, invisible to any test that does not inspect the request.

### 12.6 `RESOLVED` (§17.6) — customization-injected members become body members

> **Update.** The diagnosis below was wrong about the mechanism: stock's marshaller *also* reports
> `hasPayloadMembers(true)` for `CopyObject`. The real difference is two rules in v2's
> `XmlProtocolMarshaller` — an empty payload produces no body, and a modeled `Content-Type` suppresses
> `application/xml` — which the bridge now applies after serialization (`V2RestXmlBodyRules`). `copy-object`
> matches stock in both byte diffs; the ledgered skip is gone.


CopyObject's `SourceBucket`/`SourceKey`/`SourceVersionId` are injected by `customization.config` and
consumed by a `preClientExecutionRequestCustomizer` before marshalling; they never go on the wire.
Codegen still gives them `MarshallLocation.PAYLOAD`, so the bridge believes CopyObject has body members
and emits an empty document plus a content type:

```
stock v2:  (0 bytes, no Content-Type)
bridge:    <CopyObjectRequest></CopyObjectRequest>  +  content-type: application/xml
```

The asymmetry is structural, not a missing mapping. v2 does not decide "has a body" from `SDK_FIELDS`
at all — it reads `hasPayloadMembers` off the operation's `ShapeMarshaller`, which C2J computed from
the real model. The bridge infers it from `SDK_FIELDS`, where a synthetic member is indistinguishable
from a modeled one. Fixing it means carrying `hasPayloadMembers` into the operation schema, or marking
injected members in codegen.

Affects any operation with customization-injected members: CopyObject, UploadPartCopy, and
`UploadPartRequest.SdkPartType` (whose own documentation says "will not be included in the request
payload").

### 12.7 `RESOLVED` (§17.6) — duplicate `Content-Type`

> **Update.** The diagnosis below was wrong about the mechanism: stock's marshaller *also* reports
> `hasPayloadMembers(true)` for `CopyObject`. The real difference is two rules in v2's
> `XmlProtocolMarshaller` — an empty payload produces no body, and a modeled `Content-Type` suppresses
> `application/xml` — which the bridge now applies after serialization (`V2RestXmlBodyRules`). `copy-object`
> matches stock in both byte diffs; the ledgered skip is gone.


A consequence of 12.6, but it deserves its own line because it is the part S3 would reject rather than
ignore. CopyObject models a `ContentType` header member, and `RestXmlClientProtocol` adds its own
`application/xml` for the (spurious) body, so two `Content-Type` headers go out:

```
content-type: application/xml
content-type: text/plain
```

Even without 12.6 this needs an answer: for any operation that both has an XML body and models
`Content-Type` as a header, v2 lets the modeled value win and smithy-java appends.

### 12.8 `RESOLVED` (§17.2) — `x-amz-content-sha256` is a real body hash where v2 sends `UNSIGNED-PAYLOAD`

> **Update.** v2's own signer decides the payload hash now; the checksum goldens compare it verbatim.


Over HTTPS, v2's S3 signer sends `UNSIGNED-PAYLOAD` and never hashes the body. smithy-java's SigV4
signer computes the actual SHA-256 every time:

```
stock v2:  x-amz-content-sha256: UNSIGNED-PAYLOAD
bridge:    x-amz-content-sha256: aea5b2b0b6c6ca0468d92393adf0455d6e6343683dddedd81ab2c3837ea1d186
```

Both are accepted, so this is invisible functionally, but the cost is not: the bridge reads the body to
hash it where v2 does not.

**Now scoped to non-streaming operations only.** Streaming operations send `UNSIGNED-PAYLOAD`, because
otherwise they could not stream at all; 13.1 has the mechanism. So this entry is a per-request CPU and
allocation cost on small request bodies, not the heap ceiling it was originally feared to be. Making
non-streaming requests match v2 as well would mean setting the same header for every S3 operation,
which is a one-line change to the config translator and has not been made because nothing here needs
it.

### 12.9 `RESOLVED` (§17.3) — request checksums (`@httpChecksum`)

> **Update.** Computed by v2's own signer from v2's own checksum decision, 16/16 against stock checksum
> goldens (header, trailer, caller-chosen algorithm, precomputed value, sync and async). The CRC over an XML
> body differs from stock's only because the body does (12.13), and the test checks it is correct for the
> bytes sent.


v2 computes a request checksum for operations that model one, and for those where S3 requires one it is
not optional. `PutObjectTagging`:

```
stock v2:  x-amz-checksum-crc32: 4afhuQ==
           x-amz-sdk-checksum-algorithm: CRC32
bridge:    (absent)
```

S3 answers `MissingContentMD5`/`InvalidRequest` for `PutObjectTagging`, `PutBucketPolicy`,
`DeleteObjects` and the rest of the "checksum required" set, so this is a **functional break**, not a
degradation, for every one of those operations. Trailing checksums (`aws-chunked` + a trailer) are a
separate and larger problem on the streaming path.

Related: 6.1 already records that CRC32 *response* validation is missing.

### 12.10 `MISSING` — `amz-sdk-invocation-id` and `amz-sdk-request`

Absent from every bridged request. v2 sends both on every attempt:

```
stock v2:  amz-sdk-invocation-id: <uuid>
           amz-sdk-request: attempt=1; max=4
bridge:    (absent)
```

`amz-sdk-invocation-id` correlates the attempts of one API call in service-side logs;
`amz-sdk-request` is what AWS's adaptive-retry and client-health telemetry reads. Nothing breaks for
the caller, but a customer who bridges loses the ability to have AWS support correlate a retry storm,
and AWS loses the signal it uses to detect misbehaving clients. Cheap to add in the bridge.

### 12.11 `DEGRADED` — `host` and `Content-Length` are not in the request header map

The bridge's request reaches the v2 `SdkHttpClient` without a `host` or `Content-Length` header, where
stock v2 sets both explicitly. `host` *is* covered by the signature, so signing is not the issue —
smithy signs from the URI. In practice the v2 HTTP clients add both, so requests still go out
correctly, and the harness sees the difference only because it captures before the transport.

It is still a real difference for anything that reads headers off `SdkHttpRequest`: a v2
`ExecutionInterceptor` doing `request.firstMatchingHeader("Content-Length")`, an
`SdkHttpClient` implementation that trusts the header rather than the stream, or a customer's
`RequestBody`-size assertion.

### 12.12 `DEGRADED` — an empty URI pattern adds a trailing slash

Operations whose C2J `requestUri` has no path (`?list-type=2`, which becomes the smithy pattern
`/?list-type=2`) get the pattern's `/` appended to the endpoint path:

```
stock v2:  GET /wirediff-bucket
bridge:    GET /wirediff-bucket/
```

Benign for S3 — both address the bucket, and each arm signs what it sends — but it is a wire
difference on every bucket-level operation, and for a service that routes on exact path it would not
be benign. The joining happens inside smithy-java's endpoint/`UriPattern` composition, so the bridge
cannot fix it without either post-processing the URI or teaching codegen to emit a pattern that
composes cleanly.

### 12.13 `DEGRADED` — XML prolog, root namespace, and quote escaping

Three cosmetic codec differences, all accepted by S3, listed so they are not re-investigated:

| | stock v2 | bridge |
|---|---|---|
| prolog | `<?xml version="1.0" encoding="UTF-8"?>` | absent |
| root namespace | `xmlns="http://s3.amazonaws.com/doc/2006-03-01/"` | absent |
| `"` in element text | `&quot;` | `"` |

The namespace is the only one with a path to a fix in the bridge: C2J carries it as
`ShapeMarshaller.getXmlNameSpaceUri()` and smithy has `@xmlNamespace`, but the trait belongs on the
*payload* shape (`Tagging`), not the request shape, and `SdkSchemaFactory` has no operation context
when it builds a nested shape. The prolog is an `xml-codec` behavior with no setting. The escaping is
correct XML either way — `&quot;` is only required inside attribute values.

### 12.14 what rest-xml got right

Recorded because it bounds the problem: after 12.1-12.5, these were byte-identical to stock v2 with no
bridge-specific work.

- Greedy path labels, including percent-encoding: `nested/path/with spaces/object.txt` →
  `/nested/path/with%20spaces/object.txt`, slashes preserved, space encoded.
- Literal query parameters baked into the URI pattern (`?list-type=2`), merged with bound `@httpQuery`
  members and not duplicated.
- Valueless query parameters (`?tagging`, not `?tagging=`).
- Modeled non-200 success codes (204 on DeleteObject) via `HttpTrait.code`.
- Header-only operations (HeadObject) including conditional headers.
- `@httpPrefixHeaders` values and ordering, once the trait was emitted.
- Nested XML structures and lists, once 12.1-12.3 were fixed: `Tagging`, `CompletedMultipartUpload`,
  and the `ListObjectsV2` response all round-trip.

## 13. Sync streaming (S3 GetObject / PutObject / UploadPart)

Scope: `S3Client.putObject(request, RequestBody)`, `getObject(request, ResponseTransformer)`, and
`uploadPart`, on the smithy pipeline. Verified by `test/wire-diff` — `S3WireDiffTest` for the request
bytes against stock v2 and `S3StreamingTest` for behavior a wire diff cannot see. `put-object` and
`upload-part` are **byte-identical** to stock v2 modulo the section 12 normalizations; `get-object`
differs only by 13.6.

### 13.1 fixed (was the gating question) — a streaming body cannot live in the shape, so it travels beside it

This closes open question 7, and the answer arrived with a complication that mattered more than the
question.

**The signer.** `SigV4Signer` resolves the payload hash in three steps: `SigV4Settings.PAYLOAD_HASH_OVERRIDE`
from the context, then an `x-amz-content-sha256` header that is already on the request, then hashing the
body. Step three is unusable for streaming at any size — an unknown-length body throws
`UnsupportedOperationException("Cannot SigV4-sign a body whose length is unknown without
UNSIGNED-PAYLOAD or chunked signing, neither of which is implemented yet.")`, and a known-length body
calls `DataStream.asByteBuffer()`, pulling the whole object into the heap. Either of the first two steps
avoids it, so streaming is possible; `V2StreamingBridge` uses the header, which needs no context
plumbing and is what v2's S3 signer sends anyway.

**The complication.** v2's generator *removes* a streaming member from the shape: `PutObjectRequest` has
no `Body` field, `GetObjectResponse` has no `Body` field, because v2 passes the body beside the request
and the response body to a `ResponseTransformer`. So `SdkSchemaFactory` has no `SdkField` to translate
into an `@httpPayload @streaming` member, and synthesizing one would not help — there is no value on the
POJO for it to serialize. The body has to travel out of band, exactly as it does in v2.

It does so in a per-call `RequestOverrideConfig`: the body (or a holder for the response body) goes in
the call context, and the interceptor that applies it is added to that call only. A non-streaming
operation never walks past it, so the DynamoDB numbers in the benchmark section are unaffected.

`V2DataStreams` adapts both directions without copying: v2 `RequestBody` → a `DataStream` over its
`ContentStreamProvider` (reporting `isReplayable() == true`, which is what lets a retry re-send it), and
`DataStream` → `AbortableInputStream` for the response.

Confirmed by `S3StreamingTest.streamsBodiesLargerThanTheHeap`, which moves 1 GiB in each direction under
a 256 MiB heap and compares position-dependent CRCs, so a pipeline that buffered, truncated, or
reordered would fail rather than pass on a large host.

### 13.2 `RESOLVED` (§17.2) — chunked (`aws-chunked`) signing; the bridge refuses to stream over plain HTTP

> **Update.** v2's signer chunk-signs over HTTP, matching stock's `put-object-http` golden; the refusal is gone.


Over HTTPS, sending `UNSIGNED-PAYLOAD` is what v2 does. Over plain HTTP v2 does **not**: it switches to
chunked signing so the body stays authenticated on an unencrypted connection. `AwsChunkedDataStream`
exists in `aws-sigv4-1.6.1`, but nothing wires it into the signer, so the bridge has no equivalent.

The bridge therefore refuses, with a message naming the reason, rather than silently downgrading the
request's integrity protection to "signed headers only". Asserted by
`S3StreamingTest.refusesToStreamOverPlainHttp`. A caller who uses an `http://` endpoint override — local
testing against MinIO, or a proxy — works on v2 and fails here.

The check runs at `readAfterSigning`, not with the rest of the streaming setup, because the endpoint is
not on the request any earlier: `ClientPipeline` calls `modifyBeforeSigning` and only then
`setServiceEndpoint`, so `uri().getScheme()` is null in every hook before signing. This is the same
ordering problem as 2.1 and open question 4, reached from a different direction. Nothing is transmitted
either way, so the caller sees the same refusal; the only cost is one wasted signature.

### 13.3 `RESOLVED` (§17.3) — trailing request checksums, and what that costs the wire diff

> **Update.** Trailers match stock byte for byte (`S3ChecksumWireDiffTest`). The binding diffs still run with
> checksums off on purpose; the checksum goldens are the comparison with them on.


v2 defaults `requestChecksumCalculation` to `WHEN_SUPPORTED`, and for a streaming `PutObject` that is not
a header: it adds `x-amz-checksum-crc32`, `content-encoding: aws-chunked` and
`x-amz-decoded-content-length`, and **rewrites the body into chunk framing with the checksum in a
trailer**. The bridge computes no checksums at all (12.9), so it sends the raw bytes.

This is 12.9 again, but the streaming form is worse in two ways. Functionally, the framing is part of the
protocol: a service that expects `aws-chunked` gets a body that is not. And methodologically, it makes a
naive golden diff useless — comparing chunk framing against raw bytes reports one enormous difference
that says nothing about the HTTP binding.

So `S3Cases.client` sets `requestChecksumCalculation(WHEN_REQUIRED)` on **both** arms. That is a
precondition of the harness, not a fix: with v2's default, `put-object` and `upload-part` do not match
and cannot be made to match without implementing trailing checksums. Anyone reading a green
`S3WireDiffTest` should read it as "the binding matches, with checksums off".

### 13.4 `DEGRADED` — the response transformer runs outside the retry loop

In v2, `ResponseTransformer.transform` runs inside the retry loop, so a `RetryableException` thrown from
it — a truncated body, a checksum mismatch the caller detects — retries the whole call. Here the
transformer runs after `Client#call` returns, because the response body is handed out through the call
context and there is no smithy hook that owns "the caller has finished reading the body". Retries are
over by then, and the exception propagates.

`V2StreamingInvoker.transform` otherwise mirrors `BaseSyncClientHandler.transformResponse`: interrupt
checks either side, `RetryableException`/`AbortedException` rethrown as-is, `InterruptedException`
converted to `AbortedException` with the interrupt flag restored, everything else wrapped in
`NonRetryableException`. It also closes the body unless the transformer asked for the connection to be
left open (`toInputStream()`), and closes it regardless if the transformer threw — a transformer that
failed partway is not going to close what it never finished reading.

Related and smaller: `DataStream` has no abort concept, so `AbortableInputStream.abort()` maps to
`close()`. For the case that matters the stream underneath is v2's own abortable stream, handed through
by the transport bridge, so the connection is released; for any other `DataStream` implementation the
abort is a close.

### 13.5 fixed (was `DEGRADED`) — bridged v2 interceptors could not see the streaming body

The first wire diff of `put-object` showed the bridge sending `expect: 100-continue` where stock v2 did
not, and the cause was two independent omissions, both of which are now fixed:

1. **The body was attached too late.** `V2InterceptorBridge` runs v2's `modifyHttpRequest` at
   `modifyBeforeSigning`, and the streaming bridge originally attached the body at the same hook, so
   whether a v2 interceptor saw a `Content-Length` came down to interceptor ordering. S3's
   `StreamingRequestInterceptor` decides whether to send `Expect: 100-continue` from exactly that header,
   and its no-header fallback is `.orElse(true)`. The body is now attached at `modifyBeforeRetryLoop`,
   which is before every `modifyBeforeSigning`, so the ordering question does not arise.
2. **`SERVICE_CONFIG` was absent from the bridged execution attributes.** Omitting it does not disable
   the interceptors that read it — it sends them down their no-configuration path. Here that meant an
   `expectContinueThresholdInBytes` of 0 instead of the 1 MiB default, so every `PutObject` of any size
   got the header. `V2InterceptorBridge` now translates `SdkClientOption.SERVICE_CONFIGURATION` into
   `SdkExecutionAttribute.SERVICE_CONFIG`.

The general lesson is worth more than the header: **a v2 interceptor that reads client configuration or
request content does not fail when the bridge withholds it, it takes a different branch.** Section 2.1
lists what is still absent from the attribute map, and each entry there should be read as "some
interceptor silently behaves differently", not "some interceptor is unavailable".

### 13.6 `RESOLVED` (§17.4) — `GetObject` trailing MD5 validation (`x-amz-te: append-md5`), deliberately not enabled

> **Update.** Enabled, now that the response half is bridged: the request header matches stock, the trailer is
> stripped and checked (`ResponseChecksumProbe`'s `append-md5` cases match stock), and `WireFormat` no longer
> normalizes `x-amz-te`.


Stock v2 sends `x-amz-te: append-md5` on `GetObject` and validates the trailing MD5 S3 appends;
`EnableTrailingChecksumInterceptor.modifyResponse` also subtracts the 16 trailer bytes from the
response's `contentLength`. The bridge sends nothing, because `RESPONSE_CHECKSUM_VALIDATION` is not in
the bridged execution attributes and the interceptor's predicate requires it to be `WHEN_SUPPORTED`.

Unlike 13.5, translating that attribute would be **actively harmful**: `modifyResponse` is not one of the
four bridged hooks (2.1), so the request would ask for a trailer that nothing strips or verifies. The
caller would get 16 extra bytes appended to every object and a `contentLength` that disagrees with what
they read. Silent body corruption is worse than a missing integrity check, so the header stays off until
the response half is bridged.

This is the only remaining difference in the `get-object` wire diff. `WireFormat` drops the header, with
this entry named, so the rest of that request — path, range, signing coverage — is still compared.

Related: 6.1 (CRC32 response validation) is the modern form of the same gap.

### 13.7 `DEGRADED` — v2 interceptors never see the `RequestBody`

`V2InterceptorBridge` builds an `SdkHttpRequest` for the v2 chain, not an `SdkHttpFullRequest` with a
content stream, and `InterceptorContext.requestBody()` is empty. So `modifyHttpContent` cannot replace a
streaming body, and any interceptor that inspects the body — rather than its length — sees nothing. v2
uses this: `LegacyMd5Plugin` and the checksum interceptors read the body to compute over it.

Fixing it means threading the `RequestBody` from generated client code into the interceptor bridge, which
is a wider change than the context key the streaming bridge uses today.

### 13.8 what streaming got right

- **No buffering anywhere in either direction**, under a heap 4x smaller than the payload, with CRCs
  proving the bytes were not merely counted.
- `put-object` and `upload-part` are byte-identical to stock v2 (checksums off, per 13.3), including the
  `Content-Type` v2 derives from the `RequestBody`, `x-amz-meta-*` user metadata, and the bound query
  parameters on `UploadPart`.
- `x-amz-content-sha256: UNSIGNED-PAYLOAD` is inside `SignedHeaders`, so S3 will accept it.
- Header-bound response members survive the interceptor taking the body away: `GetObjectResponse`
  still carries `contentLength`, `contentType`, and the rest. (`responseMetadata()`/`sdkHttpResponse()`
  do not, but that is 1.4 and applies to every successful response, streaming or not.)
- Modeled errors still work on streaming operations: the interceptor leaves a non-2xx response's body
  alone, so `V2ErrorEnricher` can still build the exception from the XML.

### 13.9 what streaming costs

Measured, paired against published v2 2.46.10 — see `pipeline_benchmark2/RESULTS.md`, *Streaming*. Short
version: the bridge saves a **fixed ~50 µs per call** on `GetObject`, the same amount it saves on a
DynamoDB `GetItem`, so the win is −33% on an 8 KiB object and indistinguishable from parity on an 8 MiB
one. `PutObject` additionally gains about **0.04 µs per KiB uploaded** (unexplained; a hypothesis about
one fewer buffer touch is recorded there, not a conclusion).

Two consequences of the gaps above show up as *measurement* constraints, which is worth recording here
because they will constrain anyone else measuring this:

- 13.2 makes an HTTP `PutObject` incomparable between the arms at all (stock v2 chunk-signs, the bridge
  refuses), so streaming can only be benchmarked over HTTPS.
- 13.3 and 6.1 make the checksum paths incomparable, so the benchmark runs with both checksum knobs at
  `WHEN_REQUIRED`. The numbers therefore describe the streaming pipeline with checksums off, not a
  default `S3Client`.

## 14. Multipart (`MultipartS3AsyncClient`)

Scope: v2's own multipart client — unmodified — driving a bridged `S3Client` through a sync-backed
`S3AsyncClient` façade. Verified by `test/wire-diff/S3MultipartTest`: a 20 MiB upload at 5 MiB parts, a
4-part download, one injected part failure, a blocking-stream download on a one-thread pool, and a
non-multipart control, with a guard test asserting the client under test really is the bridged one (every
other test here would pass against stock v2, since that is the point).

> **Status after §16.** The bridged async client now exists, and multipart runs on it through the public
> builder, so most of this section is history. 14.1, 14.2 and 14.4 are **resolved**; 14.3 is
> **corrected** — stock v2 has the same behavior, so it was never a bridge degradation, and the bridge
> now reports it better; 14.5 is **still open**. The entries are kept as written because they are the
> record of what a sync-backed façade costs, which is the alternative anyone will propose next.
> `S3MultipartTest` and `SyncBackedS3AsyncClient` stay for the same reason.

**The headline is not about multipart.** Multipart splitting, part numbering, reassembly, cleanup and the
`CompleteMultipartUpload` document all work on the bridged pipeline without a single change to v2's
multipart code — the parts reassemble byte-identically to the source, and a failure still aborts the
upload. What does not work is the *async surface* multipart is reached through. Everything below is a
consequence of that one gap, and would be a consequence for `S3TransferManager` too, which is also built
on `S3AsyncClient`.

### 14.1 `RESOLVED` (§16) — the bridge generates no async client, so multipart is unreachable through its public API

Only `SyncClientClass.java` is gated on `generateSmithyJavaSerde`. The generated `S3AsyncClient` is stock
v2 and touches no smithy-java, so `S3AsyncClient.builder().multipartEnabled(true)` — the documented way
to get multipart — silently produces a fully stock pipeline. There is no configuration that yields
multipart *on the bridge*.

`test/wire-diff/SyncBackedS3AsyncClient` exists to make the phase measurable at all: it implements the
ten `S3AsyncClient` operations multipart actually calls (`createMultipartUpload`,
`completeMultipartUpload`, `abortMultipartUpload`, `uploadPart`, `uploadPartCopy`, `putObject`,
`getObject`, `headObject`, `copyObject`, `listParts` — which also serves `listPartsPaginator` via its
interface default) by submitting each to a thread pool and calling the blocking client there. The test
reaches `MultipartS3AsyncClient.create(...)` directly for the same reason.

It is a measurement device, not a proposal. A real async bridge means either generating an async client
over smithy-java's own async client (the right answer, and a phase of its own) or shipping a façade with
14.2-14.5 attached.

### 14.2 `RESOLVED` (§16) — one thread is held per in-flight part, so the pool is the real concurrency limit

A pool thread is occupied for the whole of each call, including the entire transfer of a part's body; a
real async client holds no thread while bytes are in flight. So `MultipartConfiguration`'s concurrency is
not what determines parts in flight — the pool size is. Ask for 16 concurrent parts on a pool of 8 and 8
run, with no error and no warning: the upload **silently serializes** to the pool width.
`S3MultipartTest.oneThreadSerializesTheParts` pins this by uploading a 4-part object on a
single-threaded pool and asserting the parts never overlap.

The corollary is worse than the throughput cost. A body-producing publisher scheduled on the same
executor deadlocks against the thread blocked reading it, and nothing detects that — it presents as a
hang, not an error.

This is a property of *any* sync-backed async façade, generated or hand-written. It is the reason a
sync-backed async client cannot be the answer for multipart, independently of anything smithy-java does.

### 14.3 `CORRECTED` (§16.4) — a retryable failure on one part fails the whole upload

> **Correction.** The comparison below — "where stock v2 retries that part" — is wrong. Stock v2 does not
> retry it either: `AsyncRequestBody.split` builds its parts as `NonRetryableSubAsyncRequestBody`
> (`AsyncRequestBody.java:561,590`), which refuses a second subscriber, and the "Multiple subscribers
> detected" message quoted below is v2's splitter refusing *v2's own* retry. Only
> `BufferedSplittableAsyncRequestBody` produces retryable parts, and nothing in S3's multipart helpers
> uses it unless the caller does. So this is stock behavior that the façade reproduced faithfully. What
> the bridged async client does instead is in 16.4: same outcome, better diagnosis, and a retry that
> works when the caller opts into retryable parts.

Confirmed, not inferred: `S3MultipartTest.aRetryableFailureOnOnePartIsNotRetried` injects a single 500 on
part 2 of 4 and the upload fails with v2's own

> `NonRetryableException: Multiple subscribers detected. This could happen due to a retry attempt. The
> AsyncRequestBody implementation provided does not support splitting to retryable/resubscribable
> AsyncRequestBody.`

The façade turns each `AsyncRequestBody` into a `RequestBody` whose content provider subscribes to the
publisher on every attempt, because that is what makes a retry resend the bytes. The bodies the multipart
splitter produces permit exactly one subscriber, so attempt two is rejected by v2's splitter before the
bridged pipeline is involved at all. Non-multipart bodies (byte arrays, files) resubscribe and retry
normally, which is why 13.1's replayability claim still holds — this is specific to split bodies.

The blast radius is the part that matters: a transient 500 or throttle on **one part** of a large upload
fails the **entire** upload, where stock v2 retries that part. On a 100-part upload with a 1% per-part
error rate that is a ~63% failure rate.

Cleanup is intact — the upload is aborted, so this does not leak incomplete multipart uploads. (The test
checks that while the client is still open: the abort is fire-and-forget after the caller's future has
already failed, so closing first cancels it and makes a working abort look like a leak.)

### 14.4 `RESOLVED` (§16) — a deferred-consumption transformer works, but pins a thread until the caller lets go

This was written down as a deadlock and is not one; the test says otherwise, so the entry says otherwise.
`AsyncResponseTransformer.toBlockingInputStream()` returns a usable stream and delivers the whole body
(`S3MultipartTest.aBlockingInputStreamWorksButPinsAPoolThread`, on a *single*-threaded pool).

It works for the same reason the façade is one thread per call: the publisher reads on whichever thread
calls `request(n)`, and for this transformer that is the **caller's** thread pulling bytes out of the
stream. The pool thread parks inside the blocking `ResponseTransformer` — it has to, since returning
closes the body and would truncate anything not yet pulled — and is released when the stream ends or is
closed early.

So the real cost is a held thread rather than a hang: a caller that reads slowly or forgets to close
occupies a pool slot for that whole time, and on a small pool that is the entire client. Same shape as
14.2 and the same root cause. Early close does release the thread, which is the case that would otherwise
strand one permanently — the test checks it by issuing a further call on the same one-thread pool after
abandoning a half-read stream.

`toBytes`, `toFile` and the split transformers multipart itself uses consume eagerly and release
immediately; `S3MultipartTest.multipartDownloadReassemblesEveryPart` drives 4 split transformers and
reassembles the object exactly.

One implementation note that is a genuine trap rather than a caveat: reading inside `request(n)` means
reactive-streams reentrancy is unavoidable, because transformers call `request(1)` from inside `onNext`.
A naive publisher recurses once per chunk and overflows the stack on any real object. A work-in-progress
counter is required, not an optimization.

### 14.5 `MISSING` — no cancellation (still open on the async client; see 15.4)

Cancelling a returned future does not abort the underlying HTTP request; the pool thread runs the call to
completion. For multipart that means cancelling a large upload releases the caller but not the bytes.

### 14.6 not measured

No multipart benchmark. The comparison that would matter is against stock v2's async multipart, and it
would be measuring 14.2 — the thread-per-part cost — rather than anything about smithy-java. Worth doing
only after there is a real async client to compare, or explicitly as the price of the façade.

Also unexercised: multipart *copy* (`uploadPartCopy` is implemented on the façade and unused by any
test), multipart with checksums enabled (`create(..., checksumEnabled = true)`, which runs into 12.9 and
13.3), and `UnknownContentLength` uploads, which take a different helper — the façade rejects an
`AsyncRequestBody` of unknown length outright, since measuring one means buffering it.

## 15. Async transport (`V2AsyncTransportBridge`)

Scope: v2's two async HTTP clients — `NettyNioAsyncHttpClient` and `AwsCrtAsyncHttpClient`, unmodified —
driving smithy-java's synchronous `ClientTransport` contract. Verified by
`test/wire-diff/V2AsyncTransportBridgeTest`: 8 cases against a real loopback server, every one run over
both transports (16 tests).

This is the first half of a real async client, and it is the half that decides whether the rest is worth
building. The design is a blocking `send` that returns on response **headers**, handing back a
`DataStream` that wraps the transport's own body publisher via `FlowAdapters`. Both halves of that are
measured below: the call returns in well under the server's 800 ms mid-body stall, and 200 concurrent
calls complete on a virtual-thread executor against a server stalling every response — so there is no
thread per in-flight request anywhere, which is the one thing `SyncBackedS3AsyncClient` (14.2) could not
say. Bodies are not buffered and not copied; `DataStream` already *is* a `Flow.Publisher<ByteBuffer>`, so
the adaptation is interface-only in both directions.

### 15.1 `FIXED in the bridge` — neither async transport frames an unknown-length request body, so it is silently lost

Found by test, on both transports, before any of this was written down. A request body of unknown length
(`DataStream` with `contentLength() == -1`: an `InputStream` with no length, or v2's
`AsyncRequestBody.fromPublisher` with no content length) reached the server as **zero bytes, with a 200
response and no error on either side**.

The cause is a gap between two layers that each assume the other does it. In stock v2 the framing decision
belongs to the marshaller: `AbstractStreamingRequestMarshaller#addHeaders` (`core/sdk-core/.../transform/
AbstractStreamingRequestMarshaller.java:61-76`) writes `Content-Length` from the body's length and falls
back to `Transfer-Encoding: chunked`. Neither transport will do it for you:

- **Netty** never reads `SdkHttpContentPublisher.contentLength()` at all — `RequestAdapter#adapt`
  (`http-clients/netty-nio-client/.../internal/RequestAdapter.java:48-58,80-95`) copies the request's
  headers verbatim and adds only `Host` and the H2 `:scheme`. It then *does* subscribe and write the body
  (`NettyRequestExecutor.java:243-248`), unframed, which the peer reads as no body. That is a protocol
  violation, not an empty request.
- **CRT** derives `Content-Length` from the publisher when the caller left it unset
  (`http-clients/aws-crt-client/.../internal/request/CrtRequestAdapter.java:111-115`), but collapses an
  absent length to 0 (`CrtRequestBodyAdapter#getLength:46-49`) and then never subscribes.

smithy-java's serializer writes `Content-Length` when it knows the length — which is why a known-length
body worked from the start — and has no chunked fallback. `V2AsyncTransportBridge#addFraming` supplies it:
`Transfer-Encoding: chunked` when the publisher reports no length and no framing header is already
present. Asserted in both directions (`unknownLengthRequestBodyIsChunked`,
`bodylessRequestGetsNoFramingHeaders`), because the wrong fix here adds `chunked` to a GET.

Two parts of v2's version are model-driven and are **not** reproduced:

- The `requiresLength` trait. v2 fails fast with `SdkClientException`, "This API requires Content-Length
  header to be set", for operations that cannot be chunked (S3 `UploadPart` among them). The bridge sends
  chunked instead and lets the service reject it, so the error a caller sees is a 4xx from S3 rather than
  a client-side message naming the fix.
- The HTTP/2 case. v2 omits the header when `useHttp2`; the bridge does not know the negotiated version at
  this point, and `HttpVersion` in the smithy request is `HTTP_1_1` by construction.

Both are consequences of the same thing: these are traits on the operation, and the transport is the wrong
layer to learn them. A real fix puts framing in whatever generates the smithy request, which is where
stock v2 puts it.

### 15.2 `FIXED in the bridge` (3.6) — same retry blind spot as the sync transport, for the same reason

> **Update.** Fixed with 3.6, by the same deferral, in both transport bridges.


`send` returns on headers, so a failure while reading the body surfaces later, inside the pipeline's
`deserialize`, and is retried. Everything this class sees — connect, TLS handshake, request send,
response-header read — is not, because `ClientPipeline#afterIdentity` wraps the send and the deserialize
in one try and the send's failure never reaches the code that would retry it. Identical to 3.6; the async
path inherits it unchanged, and it is not fixable from the transport.

Verified only that the exception *contract* holds: a refused connection arrives as a
`software.amazon.smithy.java.*` type on both transports, not a raw `IOException` and not
`CrtRuntimeException` (`connectFailureIsRemappedToSmithysExceptionContract`).

### 15.3 `DIFFERENT` — a queued call costs a parked virtual thread rather than a future

> **Revised.** This entry originally said virtual threads *remove* the transport's concurrency bound and
> that the bridge would need its own semaphore. That was wrong, and no semaphore was added. Every
> envelope still calls the v2 transport's `execute`, so the transport's bounds apply unchanged beneath
> the bridge: Netty's `maxConcurrency` (default 50) limits what is on the wire, and
> `maxPendingConnectionAcquires` (default 10,000, `SdkHttpConfigurationOption:152`) limits what may
> queue for a connection, after which Netty fails the acquire exactly as it does for a stock client.
> The 200-call test is the first bound working; nothing in the bridge sidesteps the second.

What does differ is the cost of a queued call. In stock v2, a call waiting for a connection is a pending
future and a handful of pipeline objects. On the bridge it is those plus a parked virtual thread — a
continuation of perhaps a kilobyte, holding the envelope's stack. 10,000 queued calls is ~10 MB more
heap, not 10,000 platform threads. Measurable and bounded, and the same bound applies to both.

### 15.4 `PARTLY RESOLVED` (§17.8) — cancellation, still

> **Update.** Timeouts now abort the in-flight exchange (the transport registers an abort action per
> attempt). A caller cancelling the returned future still does not; that needs the same hook wired to the
> future, which is small but not done.


Unchanged from 14.5 and 8.1. The bridge's `send` parks on a `CompletableFuture`; interrupting the parked
thread does not abort the v2 exchange, and smithy-java's transport contract has no place to return the
handle that would. `apiCallTimeout` needs the same missing piece.

### 15.5 not exercised at this layer

Originally this entry said the async *client* did not exist yet; it does now (§16). What is still
unexercised at the transport layer: HTTP/2 (Netty's H2 mode, which 15.1 notes changes the framing rules),
`Expect: 100-continue` (Netty defers the body subscribe until the 100 arrives —
`nrs/HttpStreamsClientHandler.java:134-142` — and the bridge has never driven that path), and a response
whose declared `Content-Length` disagrees with the bytes delivered. TLS, listed here originally, is now
covered: every S3 case in `BridgedAsyncClientsTest` runs over HTTPS, on both transports.

### 15.6 `FIXED in the bridge`, `UPSTREAM` — smithy-java's publisher-backed `DataStream` cannot be read with `asByteBuffer()`

A smithy-java 1.6.1 bug, found because the async transport is the first thing to hand the pipeline a
publisher-backed response body. `PublisherDataStream.asByteBuffer()` sets `consumed = true` (line 60) and
then subscribes through its own public `subscribe()` (line 62), which checks `consumed` and throws
`IllegalStateException: DataStream is not replayable and has already been consumed` (line 202).
`asInputStream()` does not have the bug, because it subscribes through the private `innerSubscribe`.

So a one-shot `DataStream.ofPublisher(...)` body fails on its **first** `asByteBuffer()`, and that is how
the codecs read every non-streaming response: `HttpBindingDeserializer.bodyAsByteBuffer`. Every XML
response on the async path failed — `ListObjectsV2`, `CompleteMultipartUpload`, `CopyObject` — while
bodyless and streaming responses passed, which made it look like a bridge serialization bug until the
stack trace was read. smithy-java's own transport never hits it: it hands the pipeline
`InputStream`-backed bodies.

Fixed on the bridge side by not using `ofPublisher` for response bodies. `ResponseBodyDataStream` is the
same one-shot semantics, reimplemented over v2's own `InputStreamSubscriber` (bounded buffer, streams
rather than buffers). Pinned by `V2AsyncTransportBridgeTest.responseBodyReadsAsAByteBufferExactlyOnce`,
which also asserts the second read still fails. The upstream fix is one line — `innerSubscribe` in
`asByteBuffer` — and worth filing, since any smithy-java user with a publisher-backed transport hits it.


## 16. Async client (generated `Default*AsyncClient` on smithy-java)

Scope: `AsyncClientClass` now generates the async client onto the bridge behind the same
`generateSmithyJavaSerde` gate as the sync one. For S3 that is 105 of 106 operations; the one left on the
stock pipeline is `SelectObjectContent`, an event stream (16.6). DynamoDB is all of them. The async client
shares the sync client's whole pipeline; what makes it async is two pieces, and neither is in smithy-java:

- **The envelope.** `SmithyBridgeClient#runAsync` runs the blocking `Client#call` on its own virtual thread
  and completes a v2-shaped future. smithy-java 1.6.1 has no async API at all (`Client.call → O`,
  `ClientPipeline.send → O`, `ClientTransport.send → ResponseT`), so something has to park, and a virtual
  thread parked on response headers is the cheapest thing that can.
- **The bodies.** `V2DataStreams.toDataStream(AsyncRequestBody)` and `toSdkPublisher(DataStream)` adapt
  v2's reactive bodies to smithy's `Flow.Publisher`-based `DataStream` with `FlowAdapters`, and
  `V2AsyncStreamingInvoker` drives `AsyncResponseTransformer`. With `V2AsyncTransportBridge` underneath,
  bytes move on Netty's or CRT's event loops in both directions, and nothing buffers.

Verified by:

| Suite | What | Result |
|---|---|---|
| `S3AsyncWireDiffTest` | the 9 `S3Cases` operations through `S3AsyncClient`, byte-diffed against goldens captured from **stock published 2.46.10 async** (`golden/async/`) | 8 identical; `copy-object` skipped for 12.6/12.7, the same ledgered reason as its sync twin |
| `BridgedAsyncClientsTest` | public builders only, over a real socket, **Netty and CRT** each: put/get round trips, `toBlockingInputStream`, modeled errors, completion thread, 20 MiB multipart upload and download via `multipartEnabled(true)`, part-failure handling both ways, DynamoDB `GetItem` + `ConditionalCheckFailedException` | 19/19 |
| `V2AsyncTransportBridgeTest` | the transport alone (§15), both transports | 18/18 |
| JFR, `jdk.VirtualThreadPinned` at a **0 ms** threshold, over the two suites above | whether any envelope pins its carrier | 458 virtual threads started, **0 pinned** |

The stock async goldens were compared with the stock *sync* goldens when they were captured: identical once
normalized, and unabridged they differ only in stock async sending `content-length: 0` on the two bodyless
requests. So the async diff is a bridge-versus-stock comparison, not a comparison of v2's two pipelines.

### 16.1 `DIFFERENT` — pre-transport work runs on the envelope thread, not the caller's

Stock v2 async does a surprising amount on the **caller's** thread before `execute` returns: interceptors'
`beforeExecution` and `modifyRequest` (`BaseAsyncClientHandler:74`), marshalling, endpoint resolution, and
credential resolution — `AwsCredentialsProvider#resolveIdentity` defaults to
`completedFuture(resolveCredentials())`, so a slow credentials provider blocks the caller. On the bridge
all of it runs inside `Client#call`, on the envelope's virtual thread, and the caller returns immediately.

Mostly an improvement: a caller on an event loop can no longer be blocked by an IMDS round trip. The
compatibility cost is anything **thread-bound**. An `ExecutionInterceptor` that reads a `ThreadLocal` in
`beforeExecution` — an MDC logging context, an OpenTelemetry or X-Ray context captured by thread, a
request-scoped security context — sees the envelope thread's (empty) value rather than the caller's.
Stock v2 makes no documented promise here, but the behavior is long-standing and tracing integrations
rely on it. A fix is to capture a context snapshot on the caller's thread and restore it on the envelope;
that needs a hook the bridge does not have (a v2 SPI for "context to propagate", or smithy-java's
`Context` carrying it), so it is not attempted.

### 16.2 `SAME` — future shape and completion thread

Matched deliberately, and both asserted over both transports:

- Failures are a `CompletionException` whose cause is the v2 exception, as
  `AsyncExecutionFailureExceptionReportingStage:51` produces — so `join()` throws it as is, `get()` wraps
  it in `ExecutionException`, and `whenComplete` sees `t.getCause()` as the service exception.
- Futures complete on `FUTURE_COMPLETION_EXECUTOR` (default threads `sdk-async-response-*`), so a
  caller's executor, and any context propagation it does, is honored. The hop protects less than in v2 —
  the envelope thread is not an I/O thread — but skipping it would silently ignore the configuration. A
  rejected completion task completes inline rather than never.

Cancellation forwarding is present in the same shape as stock (`CompletableFutureUtils.forwardExceptionTo`
from the returned future to the call's), but see 16.5 for what it does not reach.

### 16.3 `DIFFERENT` — `AsyncResponseTransformer.prepare` is called once per call, not once per attempt

The async twin of 13.4. v2 calls `prepare()` inside its retry loop, so a transformer is re-prepared for
each attempt, and a failure the transformer signals can be retried. The bridge calls `prepare()`,
`onResponse()` and `onStream()` after `Client#call` returns, because smithy-java's retry loop is not
reachable from outside it. In practice a retry the bridge performs happens before the transformer sees
anything — a 5xx is retried inside the pipeline and the transformer never learns of it — and a failure
*during* the body stream goes to the transformer's `exceptionOccurred` and is not retried. Whether stock
v2 retries a mid-stream failure depends on its retry stage and the transformer, and was not measured
here. Order of the callbacks, and `exceptionOccurred` on failure, match v2.

The end-of-stream wrapper v2's generated async code puts around the transformer
(`AsyncResponseTransformerUtils.wrapWithEndOfStreamFuture`) is omitted: it feeds v2's own pipeline
metrics, which do not exist here. API-call metrics are published when the future completes, as in the
non-streaming case.

### 16.4 `IMPROVED` — a one-shot request body is never re-sent, and the caller sees the real failure

The principled half of 14.3. smithy-java already refuses to retry a call whose body cannot be re-read —
`ClientCall.isRetryDisallowed` checks `DataStream.isReplayable()` — but it only looks at the operation's
**modeled** input stream member. The bridge passes bodies out of band, in the context
(`V2StreamingBridge.REQUEST_BODY`), because v2's shapes have no body member; so the check never sees them
and a one-shot body is retried anyway. That is what produced "Multiple subscribers detected" under the
façade.

`V2ErrorEnricher` now closes the gap. After classifying an attempt's failure as usual, if the out-of-band
body is not replayable it substitutes a `V2NonReplayableError` (retry safety `NO`, a constant), and the
client boundary unwraps it to the original failure. Replayability is decided in
`V2DataStreams.toDataStream(AsyncRequestBody)`, because v2 does not record it — v2 simply resubscribes and
lets a one-shot body fail. The rule is conservative in one direction only: a body is one-shot when it is
*known* to be (`NonRetryableSubAsyncRequestBody`, or a `Stream`-typed body — the blocking input/output
stream bodies and `fromInputStream`); bytes, files, and caller publishers are assumed replayable, which is
what v2 assumes of every body.

The bridge column is measured (`BridgedAsyncClientsTest`, both transports, same fake S3). The stock column
is **not** measured on the async client: it is read from source (`AsyncRequestBody.java:561,590` build
one-shot parts; `NonRetryableSubAsyncRequestBody` refuses the second subscriber) and matches the message
the façade observed in 14.3, which came from the same v2 splitter.

| Part body | Stock v2 (from source) | Bridge (measured) |
|---|---|---|
| `fromBytes`, split by multipart (one-shot parts) | retries, the part refuses the second subscriber, upload fails with `NonRetryableException: Multiple subscribers detected`, aborted | does not retry, the part is sent **once**, upload fails with the real `S3Exception` (500, `InternalError`), aborted |
| `BufferedSplittableAsyncRequestBody` (retryable parts) | retries the part, upload succeeds | retries the part — sent twice — and the upload succeeds |

Same outcome where stock fails, a better error, and no wasted second attempt. The heuristic is the
fragile part: an `AsyncRequestBody` implementation that is one-shot but reports a non-`Stream` body type
is still retried, and fails as it would on stock.

### 16.5 `MISSING` — cancellation stops at the future

Cancelling the future a caller holds completes the call's future exceptionally (the stock forwarding),
but the envelope's virtual thread keeps running `Client#call`, and the v2 exchange underneath is not
aborted — smithy-java's transport contract has nowhere to return the handle that would. Same root as
15.4, 14.5 and 8.1 (`apiCallTimeout`). A cancelled large upload stops being awaited but keeps sending.

### 16.6 `SAME AS STOCK` — event-stream operations stay on the stock pipeline, inside the same client

`ClientClassUtils.usesSmithyPipeline` excludes event-stream operations, and the async client — unlike the
sync one, which filters them out entirely — still generates them, on the stock body. So `S3AsyncClient`
holds two pipelines: `SelectObjectContent` runs through v2's `clientHandler` and everything else through
smithy-java. Both are built from the same `SdkClientConfiguration`, so configuration is consistent, but
their behavior differences (everything in this ledger) apply to one operation and not its neighbors. A
customer who measures or debugs `SelectObjectContent` is measuring stock v2.

### 16.7 measured for DynamoDB; cheaper than stock async, by less than sync

Paired stock-versus-bridge, CRT and Netty, concurrency 1 and 16 (`pipeline_benchmark2/RESULTS.md`, "Async").
At concurrency 1 the bridged async client is 4-42% cheaper in app CPU and 10-41% lower in latency, 4/4 in
every cell — about half of the sync client's margin on small operations. At concurrency 16, one loss:
Netty `batch-get`, +4.9% CPU. Latency at concurrency 16 is not comparable, because stock v2's
caller-thread marshalling (16.1) keeps a single-submitter harness from reaching the target concurrency.
Not measured: async S3 streaming, async multipart throughput, and the queued-call memory 15.3 estimates.

### 16.8 `FIXED in the bridge`, `UPSTREAM` — `RequestOverrideConfig.toBuilder()` drops the per-call context

smithy-java 1.6.1's `RequestOverrideConfig.toBuilder()` copies interceptors, resolvers, auth schemes and the
retry strategy, but not the context — and `context()` is package-private, so a caller cannot copy it either.
Found when request-level overrides were layered onto a streaming call's per-call config: the body and the
response sink lived in the context, so multipart parts went out empty and downloads came back empty, on the
façade and on the async client alike. Fixed by composing every per-call contributor into one builder that is
built once (`V2RequestOverride.apply` takes contributions, not a built config). Upstream: copy the context in
`toBuilder()`.

## 17. Schema traits and request overrides: what was addressable

Scope: the ledger entries caused by metadata the smithy path dropped — operation traits, checksums, host
prefixes, idempotency tokens, error mapping — and by request-level `overrideConfiguration()`. The question
this phase set out to answer is which of those are real limits of a smithy-java pipeline and which were
simply not wired. Answer first: **every one of them was addressable**, and in almost every case by running
v2's *own* code at the point smithy-java would run its equivalent. The residue is in 17.11.

Verification is differential throughout: each behavior is recorded from **published 2.46.10** and the bridge
is held to it, sync and async.

| Suite | What | Result |
|---|---|---|
| `S3ChecksumWireDiffTest` | 8 checksum cases × sync/async, v2 default `WHEN_SUPPORTED`, checksum headers, `x-amz-content-sha256` and `aws-chunked` bodies compared verbatim | 16/16 |
| `S3ResponseChecksumTest` | `ResponseChecksumProbe`: checksum mode on/off × correct/corrupt/absent, trailing MD5 correct/corrupt, sync/async | 16/16 lines, one named type difference (13.4) |
| `DynamoDbBehaviorTest` | `DynamoDbBehaviorProbe`: `x-amz-crc32` correct/corrupt, idempotency token present and stable across a retry, sync/async | 6/6 |
| `RequestOverridesTest` | `RequestOverrideProbe`: every request-level field and the client-level headers, signer and user-agent options, sync/async, timeouts against a real stalled server | 32/32 |
| `S3WireDiffTest`, `S3AsyncWireDiffTest` | the binding diffs, now with `write-get-object-response` (host prefix) and without any ledgered skip | 10/10, 11/11 |
| fault sweep (`error-behavior.sh`, all faults, persistent + transient) | exception type, attempts, retryability, request ID, raw response against stock | **1 behavioral difference** (was 19 at the start of this phase): `malformed-body`'s cause type, 3.5 |

The whole `test/wire-diff` module is 91/91; codegen 577/577.

### 17.1 The approach: carry v2's metadata, run v2's code

smithy-java 1.6.1 has trait consumers for some of this and not for the parts that matter: its only checksum
support is `Content-MD5` for `@httpChecksumRequired`; it has no chunked signing, no flexible checksums, no
response validation, no host-prefix support in the configuration the bridge uses, no timeouts. Putting smithy
traits on the schemas would have switched on the one behavior (MD5) that current v2 does *not* use. So the
operation-level traits go onto the generated `ApiOperation` as v2's own execution attributes
(`V2OperationMetadata`, emitted by the same generators as the stock client method), and the behavior comes
from v2's signer, v2's checksum rules and v2's interceptors, driven from smithy-java's pipeline.

### 17.2 v2's signer is bridged (4.3, 12.8, 13.2) — used only where required since 17.12

`V2SigningAuthScheme`/`V2SignerBridge`: smithy's `aws.auth#sigv4` scheme, signing with `AwsV4HttpSigner` —
`sign` for a content-provider body, `signAsync` for an `AsyncRequestBody` — with the signer properties v2's
own auth-scheme provider resolves (generated `authSchemeParams`, exposed for bridged services) and the
endpoint's `authSchemes` applied over them, as the stock interceptors compose them. The endpoint bridge
assembles the inputs, because it runs before signing and has the input in hand, and they ride to the signer
on the resolved `Endpoint`'s properties, because the resolver's context is read-only.

### 17.3 Request checksums (12.9, 13.3)

The checksum decision is v2's `HttpChecksumUtils.isHttpChecksumCalculationNeeded` and
`HttpChecksumResolver`, run against the attempt's attributes; the default algorithm and header are
`HttpChecksumStage#sraChecksum`'s lines; the computing is v2's signer. 16/16 against stock.

### 17.4 Response hooks (2.1, 6.1 S3, 13.6)

The interceptor bridge now drives v2's response-side hooks, handing the body as an `InputStream` or a
publisher by client type, as v2 does. That alone switches on v2's `HttpChecksumValidationInterceptor` and
S3's response interceptors. To keep a default DynamoDB client from installing the interceptor bridge for the
validator — the cost the inert filter was built to avoid — codegen marks it replaced for services with no
response-validating operation.

### 17.5 `x-amz-crc32` (6.1 DynamoDB)

v2's `Crc32Validation`, from a response hook, on both client types. The source said async does not validate;
stock does, so the bridge does.

### 17.6 Codec and binding rules (12.6, 12.7, 5.4, idempotency tokens)

`V2RestXmlBodyRules` applies v2's two rest-xml body rules after serialization. The endpoint bridge applies
`@endpoint` host prefixes. Generated `serializeMembers` fills an unset `@idempotencyToken` from v2's own
generator — a gap no earlier entry had recorded: stock sends a UUID, the bridge sent nothing, so a retried
`TransactWriteItems` was not idempotent.

### 17.7 Errors (1.3, 1.4)

Service-wide error registration per operation, as stock's generated mapping does; the raw error body kept for
`awsErrorDetails().rawResponse()`; a transport failure's real cause, not smithy's wrapper.

### 17.8 Request-level overrides and timeouts (2.2, 2.3, 2.4, 8.1, 8.2)

`V2RequestOverride` translates each field to the component that owns it; `V2Timeout` implements both
timeouts with a timer that aborts the attempt through an action each transport registers. The client-level
counterparts go through the same components — `overrideConfiguration().headers()`, the legacy `SIGNER`
advanced option, `USER_AGENT_PREFIX`/`SUFFIX`. 32/32 lines against stock, sync and async.

Two things stock does that the source did not make obvious, both found by the probe and both now matched: a
header set at client *and* request level is sent with **both** values, the client's first
(`MergeCustomHeadersStage` appends, except for single-valued headers) — so the client's is the one
`firstMatchingHeader` returns; and the user-agent prefix, API names and suffix wrap the *final* agent string,
which smithy-java only sets after signing, so the bridge applies them just before transmission.

### 17.9 Transport failures reach the retry loop (3.6)

`V2DeferredTransportFailure`: a transport defers its failure into the attempt as a marked stand-in response, and the
enricher restores and classifies it. This retires the one `BLOCKED` finding that would have stopped adoption
on its own — as a bridge workaround; the upstream change is small and still the right fix.

### 17.10 Three more smithy-java defects, found by this work

All worked around in the bridge, all worth filing upstream: 15.6 (`PublisherDataStream.asByteBuffer()`
always fails), 3.7 (an interceptor that does not override `modifyBeforeAttemptCompletion` disables every
interceptor after it on the error path), 16.8 (`RequestOverrideConfig.toBuilder()` drops the context).

### 17.12 Hybrid signing: v2's signer only when it is required

v2's signer is required for what it does beyond SigV4 over the request as it stands — a flexible checksum, a
streaming body (trailer or chunk signing), non-default signer properties (S3's `DOUBLE_URL_ENCODE=false`,
`NORMALIZE_PATH=false`, unsigned payloads, chunk encoding), and a legacy `Signer` override. A call that needs
none of those is signed by smithy-java's own `SigV4Signer` (`V2SignerBridge#smithyCanSign`), which recovers
the per-call cost §17.2 added. In practice: every DynamoDB call takes smithy's signer, and no S3 call does.
Anything unrecognized — a signer property the check does not know — goes to v2's signer.

**Equivalence is verified, not assumed.** `-Dawssdk.bridge.verifySigners=true` signs every eligible call both
ways at one fixed instant and fails it unless the signatures are byte-identical over the same headers; the
whole `test/wire-diff` suite passes in that mode (93/93), including retries. Two differences had to be dealt
with first:

- `DIFFERENT` — **`Content-Length` is not in `SignedHeaders`** for a smithy-signed call. smithy-java's
  signer excludes it by a fixed list; v2 signs it. Both are valid SigV4, and this is the one wire difference
  from stock that hybrid signing introduces: verification therefore compares v2's signature over the request
  *without* `Content-Length` against smithy's, and requires them to be identical.
- matched — v2 always sends and signs `x-amz-content-sha256`; smithy's signer only hashes internally. The
  bridge sets the header from the body before smithy signs, which smithy then uses rather than hashing twice.

### 17.13 `FIXED` — v2's signer re-signed the previous attempt's signed request on a retry

A defect introduced by §17.2 and found by signer verification. On a retry smithy-java hands the signer the
previous attempt's *signed* request. smithy's own signer copes (it never signs `Authorization`), but v2's
signs every header it is given: every retried call signed by v2's signer put the stale `Authorization` into
`SignedHeaders` and carried a signature a real service rejects, and a retried `aws-chunked` trailer body
would have been framed twice. No test caught it because a mock server does not check signatures. Fixed by
signing each retry from the call's unsigned request; `RetrySigningTest` pins it for both signing paths.

### 17.11 What is left, and whether it is a real limit

| Remaining | Real limit of the pipeline? |
|---|---|
| 1.1 v2 exceptions cannot be smithy `ModeledException`s | **Yes, structurally** (single inheritance). Worked around with a shim; invisible to callers. |
| 3.5 / `malformed-body` cause type (Jackson 3 vs 2) | **Yes, as long as the codec is Jackson 3.** Type and retryability match; only the cause's class differs. |
| 13.4 / 16.3 response transformer outside the retry loop | **Yes, without an upstream hook.** Visible as one exception type (a subclass of stock's) and as no retry of a mid-stream failure. |
| 16.1 thread-bound context on the async envelope | **No** — needs a context-propagation hook, not a smithy change. |
| 12.13 XML prolog / root namespace / `&quot;` | **Mostly no.** Cosmetic; the namespace is addressable in codegen, the prolog is a codec setting smithy lacks. |
| 2.1 `modifyException`, `beforeMarshalling` and two other hooks | **No** — same pattern as the hooks bridged here. |
| 2.2 plugins that change the transport or retry strategy per request | **Partly.** `RequestOverrideConfig` can carry a retry strategy; the transport is fixed at construction. |
| SigV4a, S3 Express session auth | **No** — v2's signers exist; not wired. |
| 6.2 request compression | **No** — v2's stage logic is small; no affected operation here. |
| 7.x metrics and user agent | **No**, but large: v2's collectors would need driving from smithy hooks. |
| 15.4 / 16.5 caller cancellation | **No** — the abort hook exists now; cancellation needs wiring to it. |

The performance cost of this phase is in `pipeline_benchmark2/RESULTS.md` ("Fidelity cost").

## Open questions

1. ~~Does `SigV4Signer`'s canonical-header exclusion list match v2's `AwsV4HttpSigner` exactly? A signature-comparison test against a fixed clock and fixed credentials would settle it.~~ **Answered (§17).** The bridge signs with v2's own `AwsV4HttpSigner` (4.3), so the canonical request is v2's by construction; the checksum goldens compare the signed-headers list verbatim.
2. Is `ClientPipeline`'s per-attempt endpoint/identity re-resolution a measurable cost at
   concurrency 1, or is it noise? (Benchmark: retry-free path, so probably invisible — but it shows
   up under throttling.)
3. ~~`apiCallTimeout` (8.1) has no smithy concept at all. Would adding one upstream be accepted, or does the bridge have to own it?~~ **Answered (§17).** The bridge owns it (`V2Timeout`), with a per-attempt abort registered by each transport; it matches stock on sync and async (8.1, 8.2).
4. Can `ClientPipeline` resolve the endpoint *before* `modifyBeforeSigning` (2.1)? Every alternative
   the bridge has is worse: showing interceptors a URI with no host, showing them the client endpoint
   and lying, or adding a second smithy hook after `setServiceEndpoint` that v2's chain has no
   position for. This is the one ordering difference that cannot be papered over in the bridge, and it
   silently breaks a documented v2 capability (redirecting a request in `modifyHttpRequest`).
5. Is the `X-Amz-Target`-from-shape-name coupling (11.1) something smithy-java would take a trait or
   protocol setting for? Today it makes generated shape names part of the wire contract for every
   awsJson service.
6. Do the `v2-sync-strip-*` arms resolve individually above the noise floor, or is the bridging tax
   only measurable in aggregate (`v2-sync-stripped`)? If only in aggregate, per-component attribution
   needs allocation profiling rather than timing.
7. ~~Can smithy-java's SigV4 signer be told to send `UNSIGNED-PAYLOAD` (12.8)?~~ **Answered: yes, two
   ways** — a `PAYLOAD_HASH_OVERRIDE` in the context or an `x-amz-content-sha256` header already on the
   request; only the third fallback hashes the body. 13.1 has the resolution order and why the header
   was chosen. Streaming is not heap-bounded.
8. Where should "does this operation have a body?" come from (12.6)? v2 reads `hasPayloadMembers` off
   the operation's `ShapeMarshaller`; the bridge infers it from `SDK_FIELDS`, where a
   customization-injected member is indistinguishable from a modeled one. Carrying the flag into the
   operation schema is the smaller change; marking injected members in codegen is the more correct one.
9. ~~Does anything besides `@httpChecksum` (12.9) make a bridged S3 operation outright fail? The wire    diff covers six operations; the checksum-required set alone is larger than that, and 200-with-error body, `modifyException`, and virtual-host addressing are all still unexercised.~~ **Answered (§17).** The `@httpChecksum` breakage is fixed (12.9). What else was found and fixed: empty-payload rules (12.6/12.7), host prefixes (5.4), trailing MD5 (13.6). Still unexercised: 200-with-error-body, virtual-host addressing, S3 Express.
10. ~~What does a bridged **async** client cost (14.1)?~~ **Answered (§16).** The premise was wrong —
    smithy-java 1.6.1 is synchronous top to bottom, so the async client is the *thicker* bridge: a virtual
    thread per call over the sync pipeline, with bodies adapted by `FlowAdapters`. It is cheaper than stock
    v2 async all the same, by 4-42% in app CPU at concurrency 1, and about half the sync margin on small
    operations (16.7). Multipart runs on it unmodified through the public builder, so the §14 façade
    findings no longer describe the bridge.
