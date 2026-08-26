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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

/**
 * Classification tests for the BDD endpoint provider result cache.
 *
 * <p>Classification is not observable from runtime behaviour: identity-only and identity-then-equals both invalidate
 * when a value changes, and both hit when the reference is the same. The difference is only in how much work a hit
 * costs. So the tier a parameter lands in has to be asserted here, or it is not covered at all.
 */
public class EndpointProviderCacheIndexTest {

    private static Map<String, EndpointCacheKeyClassification> classify(IntermediateModel model) {
        return EndpointProviderCacheIndex.of(model).classifiedParameters();
    }

    /**
     * Every parameter the BDD declares must be classified. A parameter missing from this map is a parameter missing
     * from the generated cache key, which is the one defect here that yields a wrong endpoint rather than a slow one.
     */
    @Test
    void everyBddParameterIsClassified() {
        assertThat(classify(ClientTestModels.queryServiceModelsWithSimpleBddEndpoints()))
            .containsOnlyKeys("Region", "UseDualStack", "UseFIPS", "Endpoint",
                              "stringContextParam", "staticStringParam", "operationContextParam", "arnList");
    }

    /**
     * Pins the tier of each parameter shape. In particular {@code staticStringParam} must be {@code OPERATION_STATIC}:
     * a parameter whose only binding site is a {@code staticContextParams} literal is fixed per operation, so it needs
     * no {@code equals} fallback. Getting this wrong is invisible at runtime, so it is asserted rather than inferred.
     */
    @Test
    void parametersAreClassifiedByTheirBindingSite() {
        assertThat(classify(ClientTestModels.queryServiceModelsWithSimpleBddEndpoints()))
            .contains(entry("UseDualStack", EndpointCacheKeyClassification.BOOLEAN),
                      entry("UseFIPS", EndpointCacheKeyClassification.BOOLEAN),
                      entry("Region", EndpointCacheKeyClassification.CLIENT_STATIC_REF),
                      entry("stringContextParam", EndpointCacheKeyClassification.CLIENT_STATIC_REF),
                      entry("staticStringParam", EndpointCacheKeyClassification.OPERATION_STATIC),
                      entry("Endpoint", EndpointCacheKeyClassification.SEMI_STABLE),
                      entry("operationContextParam", EndpointCacheKeyClassification.REQUEST_DYNAMIC),
                      entry("arnList", EndpointCacheKeyClassification.REQUEST_LIST));
    }

    /**
     * The generated comparison order is this map's iteration order, so it has to be deterministic and cheapest-first.
     * Ordering the checks the other way round would still be correct but would pay for the expensive comparisons before
     * the cheap ones had a chance to exit.
     */
    @Test
    void parametersAreOrderedCheapestComparisonFirst() {
        Map<String, EndpointCacheKeyClassification> classified =
            classify(ClientTestModels.queryServiceModelsWithSimpleBddEndpoints());

        assertThat(new ArrayList<>(classified.values()))
            .isSortedAccordingTo((a, b) -> Integer.compare(a.ordinal(), b.ordinal()));
        assertThat(classified.keySet())
            .containsExactly("UseDualStack", "UseFIPS",           // BOOLEAN
                             "Region", "stringContextParam",      // CLIENT_STATIC_REF
                             "staticStringParam",                 // OPERATION_STATIC
                             "Endpoint",                          // SEMI_STABLE
                             "operationContextParam",             // REQUEST_DYNAMIC
                             "arnList");                          // REQUEST_LIST
    }

    /**
     * A parameter no binding site names could come from anywhere, so it gets the conservative string classification
     * rather than being assumed stable. The S3 BDD is paired with the query service model, which binds none of S3's
     * request parameters, so this is the shape that model produces.
     */
    @Test
    void unboundStringParameterFallsBackToRequestDynamic() {
        assertThat(classify(ClientTestModels.queryServiceModelsWithBddEndpoints()))
            .contains(entry("Bucket", EndpointCacheKeyClassification.REQUEST_DYNAMIC),
                      entry("Key", EndpointCacheKeyClassification.REQUEST_DYNAMIC),
                      entry("CopySource", EndpointCacheKeyClassification.REQUEST_DYNAMIC),
                      entry("Prefix", EndpointCacheKeyClassification.REQUEST_DYNAMIC));
    }

    /**
     * Built-ins are classified from the built-in rather than from a name collision with a client context param, and the
     * two whose reference stability rests on an SDK implementation detail keep their {@code equals} fallback.
     */
    @Test
    void builtInsAreClassifiedFromTheBuiltIn() {
        Map<String, EndpointCacheKeyClassification> classified =
            classify(ClientTestModels.queryServiceModelsWithBddEndpoints());

        assertThat(classified)
            .contains(entry("Region", EndpointCacheKeyClassification.CLIENT_STATIC_REF),
                      entry("Endpoint", EndpointCacheKeyClassification.SEMI_STABLE),
                      entry("UseFIPS", EndpointCacheKeyClassification.BOOLEAN),
                      entry("UseDualStack", EndpointCacheKeyClassification.BOOLEAN),
                      entry("Accelerate", EndpointCacheKeyClassification.BOOLEAN),
                      entry("UseArnRegion", EndpointCacheKeyClassification.BOOLEAN));
    }

    /**
     * Classification is read off the BDD, not the rule set, because the BDD is what the generated provider evaluates.
     * The complement model pairs a two-parameter BDD with the eight-parameter default-regional rule set, so reading the
     * rule set here would silently add six parameters the provider cannot use — and, in the opposite pairing, silently
     * drop parameters it does use.
     */
    @Test
    void classificationComesFromTheBddNotTheRuleSet() {
        IntermediateModel model = ClientTestModels.queryServiceModelsWithComplementBddEndpoints();

        assertThat(model.getEndpointRuleSetModel().getParameters()).hasSize(8);
        assertThat(model.getEndpointBddModel().getParameters()).containsOnlyKeys("Endpoint", "Region");
        assertThat(classify(model)).containsOnlyKeys("Endpoint", "Region");
    }
}
