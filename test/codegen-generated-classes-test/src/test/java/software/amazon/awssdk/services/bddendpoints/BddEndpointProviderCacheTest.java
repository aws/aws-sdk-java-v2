/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.bddendpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bddendpoints.endpoints.BddEndpointsEndpointParams;
import software.amazon.awssdk.services.bddendpoints.endpoints.BddEndpointsEndpointProvider;

/**
 * Behavioural tests for the single-entry result cache generated into the BDD endpoint provider.
 *
 * <p>The suite is organised around the one invariant that matters: a cache hit must be indistinguishable from a fresh
 * resolution. That splits into two obligations.
 *
 * <ol>
 *   <li><b>No stale hit.</b> Changing any single parameter must not return the endpoint resolved for the previous value.
 *       Every parameter the model declares gets its own test, because a parameter accidentally left out of the
 *       generated key check is the one defect here that produces a wrong endpoint rather than a slow one.</li>
 *   <li><b>Hits where they are due.</b> Equal params must actually reuse the cached instance, otherwise the change is
 *       cost without benefit. Asserted with {@code isSameAs}, which is the only externally visible evidence that the
 *       cache was consulted.</li>
 * </ol>
 *
 * <p>The model declares parameters its BDD graph never reads, so that every classification tier is represented. Those
 * parameters cannot change the resolved URL, which makes them the more interesting cases to test: a stale hit is
 * detectable only by instance identity, not by comparing hosts.
 */
class BddEndpointProviderCacheTest {
    private static final Region REGION = Region.US_EAST_1;
    private static final Region OTHER_REGION = Region.US_WEST_2;

    /**
     * Returns params that resolve successfully, with every optional parameter left unset.
     */
    private static BddEndpointsEndpointParams.Builder baseBuilder() {
        return BddEndpointsEndpointParams.builder()
                                         .region(REGION)
                                         .useDualStack(false)
                                         .useFips(false);
    }

    private static BddEndpointsEndpointParams params(Consumer<BddEndpointsEndpointParams.Builder> customizer) {
        BddEndpointsEndpointParams.Builder builder = baseBuilder();
        customizer.accept(builder);
        return builder.build();
    }

    private static BddEndpointsEndpointProvider provider() {
        return BddEndpointsEndpointProvider.defaultProvider();
    }

    private static Endpoint resolve(BddEndpointsEndpointProvider provider, BddEndpointsEndpointParams params) {
        return provider.resolveEndpoint(params).join();
    }

    /**
     * Resolves {@code first}, then {@code second}, and asserts the second call did not reuse the first result.
     *
     * <p>Instance identity rather than URL comparison, so this works for the parameters that do not influence the
     * resolved URL. Those are exactly the parameters where a missing key check would go unnoticed.
     */
    private static void assertInvalidates(BddEndpointsEndpointParams first, BddEndpointsEndpointParams second) {
        BddEndpointsEndpointProvider provider = provider();
        Endpoint firstEndpoint = resolve(provider, first);
        Endpoint secondEndpoint = resolve(provider, second);
        assertThat(secondEndpoint).isNotSameAs(firstEndpoint);
    }

    private static void assertHits(BddEndpointsEndpointParams first, BddEndpointsEndpointParams second) {
        BddEndpointsEndpointProvider provider = provider();
        Endpoint firstEndpoint = resolve(provider, first);
        Endpoint secondEndpoint = resolve(provider, second);
        assertThat(secondEndpoint).isSameAs(firstEndpoint);
    }

    // ---- hits ----

    @Test
    void sameParamsInstance_reusesCachedEndpoint() {
        BddEndpointsEndpointParams p = params(b -> {
        });
        assertHits(p, p);
    }

    @Test
    void distinctButEqualParams_reusesCachedEndpoint() {
        assertHits(params(b -> {
        }), params(b -> {
        }));
    }

    @Test
    void cacheIsPerProviderInstance() {
        BddEndpointsEndpointParams p = params(b -> {
        });
        Endpoint fromFirstProvider = resolve(provider(), p);
        Endpoint fromSecondProvider = resolve(provider(), p);
        assertThat(fromSecondProvider).isNotSameAs(fromFirstProvider);
        assertThat(fromSecondProvider.endpointUrl().host()).isEqualTo(fromFirstProvider.endpointUrl().host());
    }

    /**
     * The cached endpoint must be the one the params call for, not merely some previously resolved endpoint. Alternating
     * between two parameter sets in a loop would pass even if the cache returned the wrong entry, so each round asserts
     * the host as well.
     */
    @Test
    void alternatingParams_eachResolutionMatchesItsOwnParams() {
        BddEndpointsEndpointProvider provider = provider();
        BddEndpointsEndpointParams plain = params(b -> {
        });
        BddEndpointsEndpointParams fips = params(b -> b.useFips(true));

        String plainHost = resolve(provider, plain).endpointUrl().host();
        String fipsHost = resolve(provider, fips).endpointUrl().host();
        assertThat(plainHost).doesNotContain("fips");
        assertThat(fipsHost).contains("fips");

        for (int i = 0; i < 4; i++) {
            assertThat(resolve(provider, plain).endpointUrl().host()).isEqualTo(plainHost);
            assertThat(resolve(provider, fips).endpointUrl().host()).isEqualTo(fipsHost);
        }
    }

    // ---- no stale hit, one test per parameter ----

    @Test
    void booleanTier_useFipsChange_invalidates() {
        assertInvalidates(params(b -> {
        }), params(b -> b.useFips(true)));
    }

    @Test
    void booleanTier_useDualStackChange_invalidates() {
        assertInvalidates(params(b -> {
        }), params(b -> b.useDualStack(true)));
    }

    @Test
    void clientStaticRefTier_regionChange_invalidates() {
        assertInvalidates(params(b -> {
        }), params(b -> b.region(OTHER_REGION)));
    }

    @Test
    void clientStaticRefTier_clientStringParamChange_invalidates() {
        assertInvalidates(params(b -> b.clientStringParam("first")),
                          params(b -> b.clientStringParam("second")));
    }

    @Test
    void operationStaticTier_staticStringParamChange_invalidates() {
        assertInvalidates(params(b -> b.staticStringParam("first")),
                          params(b -> b.staticStringParam("second")));
    }

    @Test
    void semiStableTier_endpointOverrideChange_invalidates() {
        assertInvalidates(params(b -> b.endpoint("https://first.example.com")),
                          params(b -> b.endpoint("https://second.example.com")));
    }

    @Test
    void semiStableTier_accountIdEndpointModeChange_invalidates() {
        assertInvalidates(params(b -> b.accountIdEndpointMode("preferred")),
                          params(b -> b.accountIdEndpointMode("disabled")));
    }

    @Test
    void identityDerivedTier_accountIdChange_invalidates() {
        assertInvalidates(params(b -> b.accountId("111111111111")),
                          params(b -> b.accountId("222222222222")));
    }

    @Test
    void requestDynamicTier_requestStringParamChange_invalidates() {
        assertInvalidates(params(b -> b.requestStringParam("first")),
                          params(b -> b.requestStringParam("second")));
    }

    // ---- lists read as a whole: wholeArnList, reached via isSet, so every element is part of the key ----

    @Test
    void wholeList_elementChange_invalidates() {
        assertInvalidates(params(b -> b.wholeArnList(Arrays.asList("a", "b"))),
                          params(b -> b.wholeArnList(Arrays.asList("a", "c"))));
    }

    @Test
    void wholeList_lengthChange_invalidates() {
        assertInvalidates(params(b -> b.wholeArnList(Arrays.asList("a", "b"))),
                          params(b -> b.wholeArnList(Collections.singletonList("a"))));
    }

    @Test
    void wholeList_orderChange_invalidates() {
        assertInvalidates(params(b -> b.wholeArnList(Arrays.asList("a", "b"))),
                          params(b -> b.wholeArnList(Arrays.asList("b", "a"))));
    }

    // ---- lists read only at index 0: resourceArnList, reached via getAttr(list, "[0]") ----
    //
    // The BDD's only read of this list is its first element, so nothing past element 0 can reach the endpoint. The key
    // therefore compares element 0 alone, which turns changes the endpoint cannot see into hits rather than misses.
    // This is the DynamoDB shape, where comparing the whole list costs more than half a regional resolution.

    @Test
    void firstElementList_firstElementChange_invalidates() {
        assertInvalidates(params(b -> b.resourceArnList(Arrays.asList("a", "b"))),
                          params(b -> b.resourceArnList(Arrays.asList("z", "b"))));
    }

    @Test
    void firstElementList_laterElementChange_isAHit() {
        assertHits(params(b -> b.resourceArnList(Arrays.asList("a", "b"))),
                   params(b -> b.resourceArnList(Arrays.asList("a", "c"))));
    }

    @Test
    void firstElementList_lengthChangeKeepingFirstElement_isAHit() {
        assertHits(params(b -> b.resourceArnList(Arrays.asList("a", "b", "c"))),
                   params(b -> b.resourceArnList(Collections.singletonList("a")))); 
    }

    @Test
    void firstElementList_orderChangeMovingFirstElement_invalidates() {
        assertInvalidates(params(b -> b.resourceArnList(Arrays.asList("a", "b"))),
                          params(b -> b.resourceArnList(Arrays.asList("b", "a"))));
    }

    /**
     * The rules engine's {@code listAccess} returns null for a null list and for an index past the end, so an absent
     * list and an empty one take the same branch and must be treated as the same key.
     */
    @Test
    void firstElementList_emptyAndUnset_areTheSameKey() {
        assertHits(params(b -> b.resourceArnList(Collections.emptyList())), params(b -> {
        }));
    }

    /**
     * No size cap applies when only the first element is compared, so a list far past the cap still hits. That is the
     * point: the comparison is O(1) rather than bounded-but-linear.
     */
    @Test
    void firstElementList_farPastTheSizeCap_stillHits() {
        List<String> long1 = new ArrayList<>(listOfSize(500));
        List<String> long2 = new ArrayList<>(listOfSize(500));
        long2.set(499, "different-tail");
        assertHits(params(b -> b.resourceArnList(long1)), params(b -> b.resourceArnList(long2)));
    }

    // ---- parameters the BDD never reads are not part of the key ----

    /**
     * {@code unusedStringParam} is declared by the model and read by no condition and no result, so it cannot change the
     * resolved endpoint and must not evict the cached one.
     *
     * <p>This is the behaviour that makes the cache worth having for S3, whose rule set declares {@code Key},
     * {@code Prefix} and {@code CopySource} and reads none of them. {@code Key} changes on essentially every object
     * request, so treating it as part of the key would mean the cache almost never hits.
     */
    @Test
    void parameterTheBddNeverReads_doesNotInvalidate() {
        assertHits(params(b -> b.unusedStringParam("first")),
                   params(b -> b.unusedStringParam("second")));
    }

    @Test
    void parameterTheBddNeverReads_settingItDoesNotInvalidate() {
        assertHits(params(b -> {
        }), params(b -> b.unusedStringParam("now-set")));
    }

    // ---- transitions to and from unset ----

    @Test
    void settingAPreviouslyUnsetParam_invalidates() {
        assertInvalidates(params(b -> {
        }), params(b -> b.requestStringParam("now-set")));
    }

    @Test
    void clearingAPreviouslySetParam_invalidates() {
        assertInvalidates(params(b -> b.requestStringParam("was-set")), params(b -> {
        }));
    }

    @Test
    void settingAPreviouslyUnsetList_invalidates() {
        assertInvalidates(params(b -> {
        }), params(b -> b.wholeArnList(Collections.singletonList("a"))));
    }

    @Test
    void clearingAPreviouslySetList_invalidates() {
        assertInvalidates(params(b -> b.wholeArnList(Collections.singletonList("a"))), params(b -> {
        }));
    }

    @Test
    void emptyListAndUnsetList_areDistinguished() {
        assertInvalidates(params(b -> b.wholeArnList(Collections.emptyList())), params(b -> {
        }));
    }

    // ---- equals fallback ----

    /**
     * The tiers with an {@code equals} fallback must hit on an equal value arriving as a fresh reference. Without the
     * fallback, a request-derived string would miss on every call and the cache would never pay off for the services
     * that need it most.
     */
    @Test
    void equalsFallbackTiers_equalValueDifferentReference_hits() {
        String value = "shared-value";
        String copy = new String(value);
        assertThat(value).isNotSameAs(copy);

        assertHits(params(b -> b.requestStringParam(value)), params(b -> b.requestStringParam(copy)));
        assertHits(params(b -> b.accountId(value)), params(b -> b.accountId(copy)));

        String url = "https://override.example.com";
        assertHits(params(b -> b.endpoint(url)), params(b -> b.endpoint(new String(url))));
    }

    @Test
    void requestList_equalContentsDifferentListInstance_hits() {
        assertHits(params(b -> b.wholeArnList(new ArrayList<>(Arrays.asList("a", "b")))),
                   params(b -> b.wholeArnList(new ArrayList<>(Arrays.asList("a", "b")))));
    }

    /**
     * Element comparison also falls back to {@code equals}, so equal strings held by different references still hit.
     */
    @Test
    void requestList_equalElementsDifferentReferences_hits() {
        assertHits(params(b -> b.wholeArnList(Collections.singletonList("element"))),
                   params(b -> b.wholeArnList(Collections.singletonList(new String("element")))));
    }

    // ---- list size cap ----

    /**
     * At the cap the element walk still runs, so equal lists hit.
     */
    @Test
    void requestList_atSizeCap_stillHits() {
        assertHits(params(b -> b.wholeArnList(listOfSize(8))), params(b -> b.wholeArnList(listOfSize(8))));
    }

    /**
     * Past the cap the check bails out and reports a miss without walking the elements, which keeps the key check
     * bounded. Equal lists therefore stop hitting; that is a deliberate cost ceiling, not a defect, and it is pinned
     * here so that changing the cap is a conscious decision.
     */
    @Test
    void requestList_pastSizeCap_alwaysMisses() {
        assertInvalidates(params(b -> b.wholeArnList(listOfSize(9))),
                          params(b -> b.wholeArnList(listOfSize(9))));
    }

    /**
     * Even a miss must still resolve correctly, so an oversized list is not a functional break.
     */
    @Test
    void requestList_pastSizeCap_stillResolvesCorrectly() {
        Endpoint endpoint = resolve(provider(), params(b -> b.wholeArnList(listOfSize(50))));
        assertThat(endpoint.endpointUrl().host()).isEqualTo("connect.us-east-1.amazonaws.com");
    }

    private static List<String> listOfSize(int size) {
        return IntStream.range(0, size).mapToObj(i -> "element-" + i).collect(Collectors.toList());
    }

    // ---- failures are never cached ----

    /**
     * A rule error must not be stored, and must not evict a good entry. Replaying a cached failure would turn one bad
     * call into a permanently broken client.
     */
    @Test
    void ruleError_isNotCached_andLeavesEarlierEntryIntact() {
        BddEndpointsEndpointProvider provider = provider();
        BddEndpointsEndpointParams good = params(b -> {
        });
        Endpoint first = resolve(provider, good);

        // FIPS combined with an endpoint override is an error in this rule set.
        BddEndpointsEndpointParams bad = params(b -> b.useFips(true).endpoint("https://override.example.com"));
        assertThatThrownBy(() -> resolve(provider, bad)).isInstanceOf(CompletionException.class);

        assertThat(resolve(provider, good)).isSameAs(first);
        // Still an error the second time, rather than a replayed success.
        assertThatThrownBy(() -> resolve(provider, bad)).isInstanceOf(CompletionException.class);
    }

    @Test
    void missingRegion_isNotCached() {
        BddEndpointsEndpointProvider provider = provider();
        BddEndpointsEndpointParams noRegion = BddEndpointsEndpointParams.builder()
                                                                        .useDualStack(false)
                                                                        .useFips(false)
                                                                        .build();
        assertThatThrownBy(() -> resolve(provider, noRegion)).isInstanceOf(CompletionException.class);
        assertThatThrownBy(() -> resolve(provider, noRegion)).isInstanceOf(CompletionException.class);

        assertThat(resolve(provider, params(b -> {
        })).endpointUrl().host()).isEqualTo("connect.us-east-1.amazonaws.com");
    }

    // ---- concurrency ----

    /**
     * Concurrent resolution of two distinct parameter sets against one provider. The cache field is written without any
     * lock, so threads race to overwrite it; every thread must still receive the endpoint its own params call for. A
     * torn or misattributed entry shows up here as a host mismatch.
     */
    @Test
    void concurrentResolution_neverReturnsAnotherThreadsEndpoint() throws Exception {
        BddEndpointsEndpointProvider provider = provider();
        BddEndpointsEndpointParams plain = params(b -> {
        });
        BddEndpointsEndpointParams dualStack = params(b -> b.useDualStack(true));
        String plainHost = resolve(provider(), plain).endpointUrl().host();
        String dualStackHost = resolve(provider(), dualStack).endpointUrl().host();
        assertThat(plainHost).isNotEqualTo(dualStackHost);

        int threads = 16;
        int iterations = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                boolean useDualStack = t % 2 == 0;
                BddEndpointsEndpointParams params = useDualStack ? dualStack : plain;
                String expectedHost = useDualStack ? dualStackHost : plainHost;
                tasks.add(() -> {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        assertThat(resolve(provider, params).endpointUrl().host()).isEqualTo(expectedHost);
                    }
                    return null;
                });
            }
            List<Future<Void>> futures = tasks.stream().map(executor::submit).collect(Collectors.toList());
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
