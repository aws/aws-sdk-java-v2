# Java v2 API Surface Area Review - Endpoints BDD

## Context

Endpoint resolution today is generated as a compiled decision tree from `endpoint-rule-set.json`. This effort adds a second codegen path that consumes a **BDD** (binary decision diagram) model, `endpoint-bdd-1.json`, and emits a flat graph of node methods plus a single-entry result cache. 

**The BDD path is dormant on release.** Codegen selects it only when a service package contains `endpoint-bdd-1.json`, and no service ships one (verified: 0 committed `endpoint-bdd*.json` under `services/`).

**References:**

- [Endpoints BDD SEP](https://code.amazon.com/packages/AwsDrSeps/blobs/main/--/seps/accepted/shared/endpoints/endpoint-bdd/endpoint-bdd.md)
-  [Smithy Endpoints BDD Spec](https://smithy.io/2.0/additional-specs/rules-engine/specification.html#smithy-rules-endpointbdd-trait)
- [Original Java 2.x Endpoints BDD Design Doc (Quip)](https://quip-amazon.com/lEMXAMZem3aI/Java-2x-Endpoints-BDD)
- PRs:

  - [<underline>[Endpoints BDD 1/5] BDD endpoint model and infrastructure</underline>](https://github.com/aws/aws-sdk-java-v2/pull/7302)
  - [<underline>[Endpoints BDD 2/5] Codegen baseline BDD Resolver</underline>](https://github.com/aws/aws-sdk-java-v2/pull/7303)
  - [<underline>[Endpoints BDD 3/5] Peephole optimizations for BDD endpoint codegen</underline>](https://github.com/aws/aws-sdk-java-v2/pull/7304)
  - [[Endpoints BDD 4/5] Avoid eager map allocation when building an Endpoint](https://github.com/aws/aws-sdk-java-v2/pull/7317)
  - [[Endpoints BDD 5/5] Add a result cache to the BDD endpoint provider](https://github.com/aws/aws-sdk-java-v2/pull/7322)
- Feature Branch:  [<underline>feature/master/endpoints-bdd</underline>](https://github.com/aws/aws-sdk-java-v2/tree/feature/master/endpoints-bdd) 

## Versions and Release Plan

- Minor version bump? **yes** - new `aws-core` compiles against a new `sdk-core` API, and new service modules against a new `aws-core` API.
- Release staging? **yes** - 2 fast runs (DynamoDB, S3). Rationale and coverage limits in [Testing, Validation and Release Plan](#testing-validation-and-release-plan).

## Customer Experience

BDD is invisible to customers: a BDD-backed service exposes the same `<X>EndpointProvider` interface, the same `Default<X>EndpointProvider` class name, and the same single `resolveEndpoint` method. No customer code changes when a service adopts a BDD model. The only customer-facing surface in this stack is the two items below.

New public accessor for the endpoint-rules wire form of `AccountIdEndpointMode`:

```java
// Was only obtainable as mode.name().toLowerCase(), which is locale-dependent.
AccountIdEndpointMode mode = AccountIdEndpointMode.REQUIRED;
String wireValue = mode.endpointModeValue();   // "required" - always, on any default locale
assert AccountIdEndpointMode.fromValue(wireValue) == mode;
```

`Endpoint.headers()` is now unmodifiable. Code that mutated the resolved map must copy it instead:

```java
Endpoint resolved = S3EndpointProvider.defaultProvider().resolveEndpoint(params).join();

// Before: silently mutated the resolved Endpoint. Now: UnsupportedOperationException.
resolved.headers().put("x-custom", Arrays.asList("v"));

// Supported form - build a new Endpoint.
Endpoint withHeader = resolved.toBuilder().putHeader("x-custom", "v").build();
```

## Public API Changes

- New method on `AccountIdEndpointMode` (`@SdkPublicApi`, `aws-core`)

```java
public String endpointModeValue();   // canonical lowercase wire form, interned literal
```

- `Endpoint` (`@SdkPublicApi`, `endpoints-spi`) - no signature change; `headers()` and the backing attributes map are now unmodifiable, and both are allocated lazily. See [Changed Behaviors](#changed-behaviors).

## Protected API Changes

- New default method on `ClientEndpointProvider` (`@SdkProtectedApi`, `sdk-core`)

```java
default String sanitizedEndpointString();   // endpoint minus query and user-info; null if not overridden
```

- `AwsEndpointProviderUtils.endpointBuiltIn` (`@SdkProtectedApi`, `aws-core`) now delegates to the above instead of re-assembling the URI inline. Behaviour preserved: `endpointIsOverridden(attrs)` reads `CLIENT_ENDPOINT_PROVIDER.isEndpointOverridden()`, the same predicate the default method guards on, so the two cannot disagree.
- New constructor and `toString()` on `EndpointProviderTestCase` (`test/ruleset-testing-core`) so generated endpoint conformance tests are named by their model documentation. Test-support artifact, no `@Sdk*Api` annotation.

## New Behaviors

1. **BDD codegen path** - `endpoint-bdd-1.json` in a service package selects `BddEndpointProviderSpec` over the compiled-rules provider. Dormant on release; adoption is per-service and additive.
2. **Single-entry endpoint result cache** in BDD-generated providers. The key compares only the parameters the BDD actually reads, which is what makes it worthwhile: S3 declares `Key`/`Prefix`/`CopySource` and reads none of them, so the cache still hits across object requests. Lists reached only via `[0]` compare element 0 alone (DynamoDB); lists read whole compare all elements up to a bound of 4, above which the check reports a miss rather than walking. Errors are never cached.
3. **Rules runtime additions** in `RulesFunctions`: `coalesce`, `split`, `ite`, `substringEquals` (peephole replacement for `stringEquals(coalesce(substring(..), ""), literal)`), `isValidHostLabelSingle`/`Multi`; `listAccess` accepts negative indices; the tokenizer and parser accept negative list indices and non-`ref` sources for `getAttr`. All additive and unreachable by any current ruleset.

## Changed Behaviors

1. **`Endpoint.headers()` is unmodifiable** (was the builder's live `HashMap`). Mutating it now throws `UnsupportedOperationException`. No opt-out. Never a documented pattern and the SDK itself always uses `toBuilder()`, but it is a real break for anyone who did.
2. **DynamoDB's `AWS::Auth::AccountIdEndpointMode` wire value** is now a fixed literal rather than `name().toLowerCase()`. Identical output on every locale except Turkish, where the old code produced `dısabled` and `requıred` (dotless i). Those did not match the rules engine's string comparisons, so `DISABLED` silently behaved like `PREFERRED` (account-based endpoint used despite being disabled) and `REQUIRED` failed to error. This is a **fix**, and DynamoDB is the only service in the SDK whose ruleset uses this built-in.
3. **`awsPartition` returns a shared `RulePartition`** per partition, built once at partition load, instead of a fresh instance per resolution. `RulePartition` is immutable (all fields final, private constructor). Region-specific overrides are still discarded, as before.
4. **`loadPartitionData` fails fast** with `SdkClientException` if `partitions.json` lacks the `aws` partition. Previously `awsPartition` would NPE on an unmatched region while known regions kept working.
5. **`SDK::Endpoint` built-in is precomputed** in the `StaticClientEndpointProvider` constructor, so a malformed override URI surfaces at client construction rather than first request. Same exception type (`invokeSafely`).

## Notable Changes

- **`IntermediateModel`'s public constructor gained a parameter** (11 -> 12 args), a source and binary break in the `codegen` artifact. Build-time tool, no `@Sdk*Api` contract; called out for anyone generating clients out-of-tree. Note: The large constructor is an existing pattern and isn't refactored to a build here and this is likely better accomplished during Phase 3 of smithy migration!.
- **Release infrastructure does not yet copy** `endpoint-bdd-1.json` into service packages. This ensures we do not start immediately using BDD endpoints for all services since BDD endpoints are published by Trebuchet for every service as of Q1 2026.
- **Generated code size is comparable**, not larger: the S3 BDD provider golden is 4,139 lines against 3,942 for today's compiled-rules provider.
- Codegen for array-valued `staticContextParams` now hoists a shared `static final List<String>`. Zero services have array-valued static context params today, so this is currently exercised only by the codegen fixture.

## Footgun Assessment

**Verdict:** low footgun risk - the one new public method is a pure accessor with no parameters, no resources, and no configuration; the `Endpoint` change removes a mutation path rather than adding one.

**Misconfiguration failure modes:** `endpointModeValue()` takes no arguments and cannot be misconfigured. The realistic mistake is the inverse - a customer *keeps* using `mode.name().toLowerCase()` when hand-building endpoint params, and reintroduces the locale bug in their own code. The `Endpoint` change has no configuration surface; its failure mode is an upgrade-time `UnsupportedOperationException`, covered under Changed Behaviors, not a misuse.

**Guardrails and mitigations:** `fromValue(endpointModeValue())` round-trips for all three modes, so the pair is self-consistent. The javadoc states why it exists over `name().toLowerCase()` (stable reference, locale-independent), which is the discoverability guardrail that steers customers off the buggy idiom. No validation is applicable. No `@SdkAdvancedApi` marker warranted.

## Backwards Compatibility

**Opt-in or opt-out?** Mixed. The BDD path is opt-in per service (requires a model file), but no opt-out is available for customers to configure.

**Performance impact?** Improvement. See [Performance](#performance).

**Version compatibility:**

| Combination | Result |
| --- | --- |
| New `endpoints-spi` + old everything | ✅ None - `Endpoint` change is additive plus stricter mutability |
| New `sdk-core` + old `aws-core` | ✅ None - old `aws-core` sanitizes inline and never calls the new method |
| New `aws-core` + old `sdk-core` | ❌ Runtime failure - `NoSuchMethodError` on `ClientEndpointProvider.sanitizedEndpointString()`, on every request that resolves against an overridden client endpoint |
| New service module + old `aws-core` | ❌ Runtime failure - `NoSuchMethodError` on `AccountIdEndpointMode.endpointModeValue()`; DynamoDB only, when the accountId built-in is read |
| Old service module + new core | ✅ None - old generated code calls only pre-existing APIs |
| New service module + new core | ✅ Works |

- **Internal customers:** compile failures at build time when merging from live.
- **External customers:** runtime `NoSuchMethodError` if they pin core and service modules to different minors.

**Minor version bump reasoning:** Yes. Two cross-module compile-time dependencies on new APIs (`aws-core` -> `sdk-core`, service -> `aws-core`), and the change touches a runtime component present in 346 service modules.

## Release Risk

**Overall verdict:** medium release risk - correctness-critical code in the request path of 346 service modules is reworked behind an unchanged API, and the highest-impact failure modes (wrong partition data, wrong host) would be silent.  However, endpoint rules/BDD are currently guaranteed (by upstream build logic) to have 100% leaf node coverage by endpoint tests, which are unchanged in this change which increases confidence in the correctness of our BDD implementation.

### Changes and Risks

#### 1. `RulesFunctions` runtime rework (ships to 346 service modules)

- **[1.1][medium]** `awsPartition` hands out one shared `RulePartition` per partition instead of a fresh instance. Verified immutable today, but if any current or future code mutates it, every subsequent resolution in that module sees the corruption - wrong `dnsSuffix`/`dualStackDnsSuffix` means a request sent to the wrong host, with no error. Broadest blast radius in the change and the least detectable.
- **[1.2][medium]** `substring` was restructured (ASCII scan extracted to `isAsciiOnly`). This is the function behind S3/S3 Control/S3 Outposts ARN parsing and S3 Express `--x-s3` bucket detection. A regression routes an Express or access-point request as a general-purpose bucket, or vice versa - usually loud (403/404) but capable of reaching a valid-but-wrong endpoint.
- **[1.3][low]** `isValidHostLabel` now delegates to two specialized helpers. Pure refactor; a differential test pinning all three against each other landed in PR3.
- **[1.4][low]** `loadPartitionData` now throws when the `aws` partition is absent, where previously known regions still resolved. Reachable only with a corrupt `partitions.json` shipped in `regions`, and fails loudly with a message that names the cause.  This is justifiable within the spec which requires any unmatched region to fallback to `aws`. 

#### 2. `Endpoint` immutability and lazy allocation

- **[2.1][medium]** `headers()` became unmodifiable with no opt-out. Any customer `EndpointProvider` or interceptor that mutated the resolved map breaks on upgrade. Fails loudly and is trivially diagnosable and is in line with other immutability contracts in the SDK.
- **[2.2][low]** The attributes map is now `emptyMap`/`singletonMap`/`unmodifiableMap` depending on arity. `Endpoint.equals`/`hashCode` are content-based via `AbstractMap`, so old-built and new-built endpoints still compare equal - confirmed by the arity round-trip tests added in PR4.

#### 3. Precomputed `SDK::Endpoint` built-in

- **[3.1][low]** Sanitization moved from per-request to the `StaticClientEndpointProvider` constructor, so failure timing moves earlier. Same exception type; earlier is better.
- **[3.2][low]** The override is `final`, so an `@SdkInternalApi` subclass can no longer customize it. No supported consumer affected.

#### 4. `AccountIdEndpointMode` wire value (DynamoDB only)

- **[4.1][medium]** Byte-identical for essentially all customers; on a Turkish-locale JVM it changes DynamoDB routing because it fixes the dotless-i bug. A customer who unknowingly depended on `DISABLED` behaving like `PREFERRED` sees their endpoints change. Silent, narrow population, and the new behaviour is the correct one.
- **[4.2][low]** `fromValue` now compares against the literal rather than `name()`; `equalsIgnoreCase` over identical ASCII letters, so unchanged.

#### 5. Dormant BDD codegen path and result cache

- **[5.1][low]** Dead code in the shipped artifacts. The risk is release-infrastructure: without model-file plumbing, adding `endpoint-bdd-1.json` silently falls back to the compiled-rules provider. Consequence is "optimization not applied", not incorrect behaviour.
- **[5.2][medium]** *Fires only once models ship.* The cache returns a shared `Endpoint` instance across requests and threads. Correctness depends on `Endpoint` being effectively immutable - true for `headers()` and the attributes map after PR4, but **not for attribute values**, e.g. the `List<EndpointAuthScheme>` behind `AUTH_SCHEMES`. Verified read-only today (`DefaultS3Presigner`, generated `authSchemeWithEndpointSignerProperties`, and `addHostPrefix` which copies via `toBuilder()`), but a future mutation would leak signing properties across requests.

## Testing, Validation and Release Plan

**Existing test coverage:**

| Risk | Unit/Integration coverage | Notes |
| --- | --- | --- |
| [1.1] | partial | Per-service `endpoint-tests.json` conformance tests assert resolved values; nothing asserts the shared `RulePartition` is immutable or identical per partition |
| [1.2] | covered | `RulesFunctionsSubstringEqualsTest` (PR3) differentially compares `substringEquals` against the spec composition over a non-ASCII/surrogate matrix, plus per-service conformance tests |
| [1.3] | covered | `isValidHostLabel_agreesWithSpecializedVariants` |
| [1.4] | not covered | No test for the missing-`aws`-partition path |
| [1.5] | covered | `TokenizerTest` additions, plus the 34-model regeneration diff above |
| [2.1] [2.2] | covered | `EndpointTest.headers_isUnmodifiable`, attribute-arity and `toBuilder` round-trip tests (PR4) |
| [3.1] [3.2] | **not covered** | `StaticClientEndpointProvider`'s javadoc cites `ClientEndpointProviderTest.sanitizedEndpointString_cachedFormMatchesRecomputedForm` as the test that holds the cached and recomputed forms together. **That file is untracked and exists on no branch.** |
| [4.1] [4.2] | **not covered** | `AccountIdEndpointModeTest` is likewise untracked. Only indirect coverage via codegen golden files. |
| [5.1] | covered | `BddEndpointProviderSpecTest`, `BddPeepholeVisitorTest`, `ConditionFnCodeGeneratorVisitorTest`, golden files for the default-regional and S3 shapes |
| [5.2] | partial | `BddEndpointProviderCacheTest` is thorough on key correctness, per-parameter invalidation, error non-caching and 16-thread concurrency, but asserts instance identity, not immutability of attribute values |
| [5.3] | not covered | Accepted |

**Proposed validation per remaining risk:**

- **[1.1]** Add a unit test asserting `awsPartition` returns the same instance for the same region and that `RulePartition` exposes no mutator. Cheap, permanent, and directly targets the least-detectable risk.
- **[1.4]** Add a `loadPartitionData` test with an `aws`-less partitions document asserting the `SdkClientException` message. Cheap.
- **[1.6]** Add `split` null/limit tests, or null-guard `split` to return `null` like its neighbours. Decide which before merge - the current asymmetry is a trap for the first ruleset that uses it.
- **[3.1] [3.2] [4.1] [4.2]** **Commit the two missing test files before merge.** These are the only two risks in the table with no committed coverage at all, and one of them is referenced by javadoc as though it exists. Highest priority item in this review.
- **[5.2]** Add an assertion that a cached `Endpoint`'s `AUTH_SCHEMES` list is unmodifiable, or have the generated BDD result code wrap it. Do this before batch 1, not before merge - it cannot fire while the path is dormant.
- **[5.1]** Release-infrastructure work item for `endpoint-bdd-1.json` propagation, plus a codegen warning (or build failure) when a BDD model is present but unused, so the fallback is never silent.
- **[1.2] [1.3] [1.5] [2.1] [2.2]** Covered; additionally exercised by Release Staging below.
- **[5.3]** Accepted - bounded to one params object per client.

**Release Staging:** **yes** - 2 runs (DynamoDB, S3), fast setting, as proposed.

This is the right call, though not for the reason the plan implies. The change is *not* net-new from a release perspective: `RulesFunctions` is existing request-path behaviour reworked in 346 modules, and `Endpoint.headers()` is a public behaviour change with no opt-out. Both fall squarely in "changes to existing SDK behavior" and "large refactoring". The two-service choice is well targeted: DynamoDB is the only consumer of the `AccountIdEndpointMode` line and the only ruleset reading `listAccess`, and S3 exercises `substring` and `isValidHostLabel` more than any other ruleset. Fast setting is proportionate given nothing customer-visible is enabled.

- **Risks covered by Release Staging:** [1.1], [1.2], [1.3], [1.5], [2.1], [2.2], [3.1], [4.1] - internal pipelines resolve endpoints on every request, so a partition, substring or host-label regression surfaces as a routing or signing failure in a small cohort.
- **Risks NOT covered:** [1.4] and [1.6] (unreachable code paths), [5.1] (release tooling, not runtime), [5.2] and [5.3] (dormant until models ship), and any Turkish-locale variant of [4.1] - internal pipelines will not be running a `tr` default locale.

> **Reviewer action:** Please confirm (a) that the two untracked test files are committed before merge, (b) the `split` null-handling decision, and (c) the batch composition below. The value-vs-risk-vs-cost tradeoff is yours.

## Look Out For

1. **`UnsupportedOperationException` from `Endpoint.headers()`** - customers or third-party endpoint providers that mutated the resolved header map. Direct them to `toBuilder().putHeader(..)`.
2. **`NoSuchMethodError` on `endpointModeValue()` or `sanitizedEndpointString()`** - a mixed-version dependency tree. Point at the BOM.
3. **DynamoDB endpoints changing on a Turkish-locale JVM** - expected, and the new behaviour is correct.
4. **"I added a BDD model and nothing happened"** - the silent fallback in [5.1], until the release tooling and the codegen warning land.

## Performance

Endpoint resolution performance measured on the cross-SDK standards (and dynamodb) on m7i.xlarge, all values are microseconds.  "Aggregate" test cases run all of the individual test cases, shuffled per iteration.  These results do NOT include caching, for caching results, see next section.

| **service** | **case** | **rules (ns/op)** | **BDD (ns/op)** |
| --- | --- | --- | --- |
| Connect | aggregate | 267 | 230 |
| Connect | case0_usEast1 | 33 | 27 |
| Connect | case1_customEndpoint | 59 | 56 |
| Connect | case2_usEast1Fips | 34 | 27 |
| Connect | case3_usWest2FipsDualStack | 33 | 27 |
| Connect | case4_euCentral1DualStack | 34 | 27 |
| Connect | case5_cnNorth1DualStack | 33 | 27 |
| DynamoDb | aggregate | 274 | 274 |
| DynamoDb | case0_regional | 34 | 28 |
| DynamoDb | case1_fipsDualStack | 35 | 28 |
| DynamoDb | case2_accountIdPreferred | 51 | 60 |
| DynamoDb | case3_accountIdChinaFallback | 36 | 28 |
| DynamoDb | case4_customEndpoint | 73 | 70 |
| S3 | aggregate | 1579 | 1486 |
| S3 | case0_virtualAddressing | 83 | 82 |
| S3 | case1_pathStyle | 118 | 84 |
| S3 | case2_s3ExpressDataPlane | 116 | 219 |
| S3 | case3_accessPointArn | 405 | 266 |
| S3 | case4_outposts | 490 | 346 |

**Single Entry Cache:**

(Values for resolution avoided on a hit are based on above endpoint BDD benchmarks).

| **Service** | **Params declared** | **In cache key** | **Cache hit** | **Cache miss** | **Resolution avoided on a hit** | **Break-even hit rate** |
| --- | --- | --- | --- | --- | --- | --- |
| Standard regional (Connect) | 4 | 4 | 1.1 ns | 1.7 ns | 27–56 ns (96%) | 6% |
| DynamoDB | 9 | 9 | 4.6 ns | 3.4 ns | 28–70 ns (84%) | 13% |
| DynamoDB (batch, ARN list set) | 9 | 9 | 5.4 ns | 4.3 ns | 28–70 ns (81%) | 16% |
| S3 | 17 | 14 | 4.9 ns | 4.5 ns | 82–346 ns (94%) | 6% |

## Appendix

### Recommended external rollout batches

The batches are the per-service BDD model adoption, since the merge itself changes no endpoint provider. Order by how far a service's ruleset departs from the shape the golden tests pin, and put traffic-weight last:

**Batch 1 - plain default-regional services (~5-10).** CloudWatch, SQS, SNS, KMS, Kinesis, Lambda, SSO, plus two or three more of the same shape. This is exactly the `endpoint-bdd-default-regional.json` golden the codegen tests pin, and it is the shape of the overwhelming majority of the 433 services, so it buys the most confidence per unit of risk. It also proves the release-infrastructure model-file plumbing on real service packages, and makes the cache live for the first time in a place where a stale hit is easy to spot (region/FIPS/dual-stack only).

**Batch 2 - structured rulesets, still not S3.** DynamoDB first: it is the only accountId-endpoint-mode service, the only `resourceArnList[0]` consumer, and therefore the only exercise of the first-element cache-key optimization - which is the part of the design most likely to be wrong. Add EventBridge, Route 53, CloudFront KeyValueStore, and one endpoint-discovery service (Timestream) to cover ARN parsing, host-label validation and the discovery interaction.

**Batch 3 - S3 and friends.** S3, S3 Control, S3 Outposts. Deliberately last: the largest ruleset in the SDK (a 4,139-line BDD provider), every `substringEquals` peephole rewrite, MRAP/Express/access-point/ARN routing, and a cache key that intentionally excludes `Key`/`Prefix`/`CopySource` - the least obvious decision in the design. S3 also has the widest set of high-level consumers (Transfer Manager, presigner, CRT, external Hadoop/S3A) whose interactions are hardest to cover with tests.

Do **not** lead with S3. Leading with it would put the least-pinned ruleset, the largest generated artifact and the highest traffic in the same first step, with no earlier batch to attribute a regression against.

### Open items for the review meeting

1. **`RulesFunctions.coalesce` has two overloads with different semantics.** `<T> T coalesce(T...)` returns the first non-`null`; `String coalesce(String...)` returns the first non-`null`-**and-non-empty**. Java resolves `String` arguments to the second, so the generic one is unreachable for the common case and the two disagree on `coalesce("", "x")`. Which one matches the endpoint rules spec? The generic version's comment ("All preceding arguments empty") describes the String version's behaviour, which suggests one of them was not intended.
2. Should the codegen fail, rather than silently fall back, when a service package has an `endpoint-bdd-1.json` that the build does not pick up?
3. Confirm `IntermediateModel`'s constructor break in the published `codegen` artifact is acceptable, or add an overload preserving the 11-arg form.

### Method

Generated-code impact was measured, not inferred: `codegen` and `codegen-maven-plugin` were built at merge-base `858c1dcda8e` and at PR5 HEAD `5f4c7dfd435`, `generate-sources` run over 34 service models (S3, S3 Control, S3 Outposts, DynamoDB, EC2, IAM, EventBridge, CloudFront, CloudFront KeyValueStore, API Gateway, Lambda, SQS, SNS, Kinesis, Firehose, Route 53, Timestream Query, SageMaker, CodeCatalyst, Transcribe Streaming, MediaStore Data, STS, SSO, CloudWatch, KMS, Cognito, Connect, Pinpoint, ACM and their sub-models), and the 663 resulting files diffed. The local, uncommitted `services/dynamodb/.../endpoint-bdd-1.json` was set aside for the comparison so both sides took the non-BDD path.