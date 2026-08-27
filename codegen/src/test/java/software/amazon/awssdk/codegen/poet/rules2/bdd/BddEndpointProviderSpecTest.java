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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;

public class BddEndpointProviderSpecTest {

    @Test
    void endpointProviderClass_simpleBdd_generatesExpectedCode() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithSimpleBddEndpoints());
        MatcherAssert.assertThat(spec, generatesTo("endpoint-provider-bdd-class.java"));
    }

    /**
     * The S3 BDD is the only model that exercises all four peephole rewrites, the dynamic auth scheme
     * name, and the {@code ite} assign conditions. The simple-BDD fixture above contains none of
     * those shapes, so this golden file is what makes the pass's effect on generated code reviewable
     * and pins it against unintended change.
     */
    @Test
    void endpointProviderClass_s3Bdd_generatesExpectedCode() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());
        MatcherAssert.assertThat(spec, generatesTo("endpoint-provider-bdd-s3-class.java"));
    }

    /**
     * Correctness invariants that must hold no matter what the golden file above happens to contain.
     *
     * <p>These are deliberately not left to the fixture. Regenerating a golden file after changing an
     * emitter is a one-command operation, and doing it without reading the diff is how a defect gets
     * blessed into a fixture - this repo has an instance of exactly that. Every assertion here is
     * phrased negatively and carries the reason, so restoring the faster-but-wrong form means
     * consciously deleting a stated invariant rather than accepting a regenerated file.
     */
    @Test
    void s3Bdd_neverEmitsSpecViolatingForms() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());
        String generated = spec.poetSpec().toString();

        // Inlined String comparisons skip substring's rejection of non-ASCII input, which would route
        // a bucket like "mybuck\u00e9t--x-s3" to S3 Express with sigv4-s3express instead of to a
        // regular S3 endpoint with SigV4. RulesFunctions.substringEquals exists to preserve that.
        assertThat(generated)
            .as("substring comparisons must go through substringEquals, which checks for non-ASCII input")
            .doesNotContain(".startsWith(")
            .doesNotContain(".endsWith(")
            .doesNotContain(".regionMatches(");

        // The spec's stringEquals returns false for a null operand; .equals throws. Only a comparison
        // with a string constant on the receiver is safe to inline.
        assertThat(generated)
            .as("stringEquals with two nullable operands must stay null-safe")
            .doesNotContain("region.equals(bucketArn.region())")
            .doesNotContain("bucketArn.partition().equals(")
            .doesNotContain("bucketPartition.name().equals(");

        // A ternary would emit the coalesce subject twice, running any non-trivial operand twice.
        assertThat(generated)
            .as("boolean coalesce must evaluate its subject once")
            .doesNotContain("!= null ?");

        // Each rewrite must actually fire; falling back to the generic dispatch is a silent
        // de-optimization that the fixture alone would absorb without comment.
        assertThat(generated)
            .as("no rewritten shape may fall back to a RulesFunctions dispatch")
            .doesNotContain("RulesFunctions.ite(")
            .doesNotContain("RulesFunctions.coalesce(")
            .doesNotContain("RulesFunctions.isValidHostLabel(");
    }

    /**
     * The three {@code ite} nodes in the S3 BDD all have string-literal branches, so their assign
     * conditions are provably non-null and keep the elided null check. Paired with
     * {@code ConditionFnCodeGeneratorVisitorTest}, which pins the nullable-branch case that must emit
     * the check. Stated here as an invariant because the two halves only make sense together.
     */
    @Test
    void literalBranchIteAssigns_keepElidedNullCheck() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());
        String generated = spec.poetSpec().toString();

        assertThat(generated).contains("_s3e_ds = (params.useDualStack() ? \".dualstack\" : \"\");\n      return true;");
        assertThat(generated).contains("_s3e_fips = (params.useFips() ? \"-fips\" : \"\");\n      return true;");
    }

    /**
     * The S3 BDD merges results that differ only in their auth scheme name, so the name is resolved at runtime via
     * {@code DynamicEndpointAuthSchemeFactory.create(name)}. The sibling properties must still be emitted, otherwise
     * the signing configuration would be silently dropped.
     */
    @Test
    void dynamicAuthSchemeName_delegatesToFactoryAndKeepsProperties() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());

        String generated = spec.poetSpec().toString();

        assertThat(generated).contains("DynamicEndpointAuthSchemeFactory.builder()");
        assertThat(generated).contains(".create(_s3e_auth)");
        assertThat(generated).contains(".signingName(\"s3express\")");
        assertThat(generated).contains(".disableDoubleEncoding(true)");
        assertThat(generated).doesNotContain("DynamicEndpointAuthSchemeFactory.builder().build()");
    }

    /**
     * Verifies that complement edges (negative node references) generate nodeN methods with
     * swapped branches. A complement edge to node N means: evaluate the same condition, but
     * follow lowRef when true and highRef when false (the inverse of nodeP).
     */
    @Test
    void complementEdge_generatesNodeNWithSwappedBranches() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithComplementBddEndpoints());

        String generated = spec.poetSpec().toString();

        // nodeP1 should exist with normal branch order
        assertThat(generated).contains("nodeP1()");
        // nodeN1 should exist (complement variant with swapped branches)
        assertThat(generated).contains("nodeN1()");
        // nodeP2's false branch should reference nodeN1 (the complement edge)
        assertThat(generated).contains("Endpoint nodeN1()");
    }

    /**
     * The one invariant the result cache depends on: every parameter the BDD declares must appear in the generated key.
     * A parameter left out is not a slow cache, it is a cache that returns an endpoint resolved for a different value of
     * that parameter, and nothing else in the test suite would catch it.
     *
     * <p>Asserted against the generated source rather than against an intermediate model, so it holds regardless of how
     * the comparison is built.
     */
    @Test
    void cacheKeyComparesEveryDeclaredParameter() {
        assertCacheKeyIsComplete(ClientTestModels.queryServiceModelsWithSimpleBddEndpoints());
        assertCacheKeyIsComplete(ClientTestModels.queryServiceModelsWithBddEndpoints());
        assertCacheKeyIsComplete(ClientTestModels.queryServiceModelsWithComplementBddEndpoints());
    }

    private static void assertCacheKeyIsComplete(IntermediateModel model) {
        EndpointRulesSpecUtils utils = new EndpointRulesSpecUtils(model);
        List<String> expected = model.getEndpointBddModel().getParameters().keySet().stream()
                                     .map(utils::paramMethodName)
                                     .collect(Collectors.toList());

        assertThat(cacheKeyGetterOrder(new BddEndpointProviderSpec(model)))
            .as("every parameter the BDD declares must be part of the cache key")
            .containsExactlyInAnyOrderElementsOf(expected);
    }

    /**
     * The generated key compares booleans first, then the strings whose reference the SDK keeps stable, then everything
     * else. Ordering cannot change the result - the chain compares every parameter before returning true - it only
     * decides how quickly a mismatch is found, so this is a performance property rather than a correctness one. It is
     * pinned because the ordering is the entire reason the grouping exists; if it silently degraded to declaration
     * order the code would still be correct and the benefit would be gone.
     */
    @Test
    void cacheKeyOrdersBooleansThenStableStringsThenTheRest() {
        List<String> order = cacheKeyGetterOrder(
            new BddEndpointProviderSpec(ClientTestModels.queryServiceModelsWithSimpleBddEndpoints()));

        assertThat(order).containsExactly("useDualStack", "useFips",              // booleans
                                          "region", "stringContextParam",        // reference-stable strings
                                          "endpoint", "staticStringParam",       // everything else, declaration order
                                          "operationContextParam", "arnList");
    }

    /**
     * A list parameter needs a bounded comparison, so it routes through the emitted helper rather than
     * {@code Objects.equals}, whose {@code List.equals} would walk every element however long the list is.
     */
    @Test
    void listParametersUseTheBoundedHelper() {
        String generated = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithSimpleBddEndpoints()).poetSpec().toString();

        assertThat(generated).contains("cacheListsMatch(a.arnList(), b.arnList())");
        assertThat(generated).contains("if (size > 8) return false");
    }

    /**
     * The helper is only useful when the model has a list parameter, and the S3 BDD has none.
     */
    @Test
    void listHelperIsOmittedWhenNoListParameterExists() {
        String generated = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints()).poetSpec().toString();

        assertThat(generated).doesNotContain("cacheListsMatch");
    }

    /**
     * Returns the parameter getters referenced by the generated {@code cacheParamsMatch}, in the order they are
     * compared.
     */
    private static List<String> cacheKeyGetterOrder(BddEndpointProviderSpec spec) {
        String generated = spec.poetSpec().toString();
        int start = generated.indexOf("boolean cacheParamsMatch(");
        assertThat(start).as("generated provider must contain cacheParamsMatch").isNotNegative();
        int end = generated.indexOf(";", start);
        String body = generated.substring(start, end);

        List<String> getters = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\ba\\.(\\w+)\\(\\)").matcher(body);
        while (matcher.find()) {
            getters.add(matcher.group(1));
        }
        return getters;
    }

    /**
     * {@code DynamicEndpointAuthSchemeFactory} is S3-specific, so a dynamically resolved auth scheme name in any other
     * service must fail codegen rather than emitting code that cannot compile.
     */
    @Test
    void dynamicAuthSchemeName_withoutS3ExpressCustomization_failsCodegen() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints(false));

        assertThatThrownBy(spec::poetSpec)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("useS3ExpressSessionAuth")
            .hasMessageContaining("resolved at runtime");
    }
}
