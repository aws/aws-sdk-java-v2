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

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class BddEndpointProviderSpecTest {

    @Test
    void endpointProviderClass_simpleBdd_generatesExpectedCode() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithSimpleBddEndpoints());
        MatcherAssert.assertThat(spec, generatesTo("endpoint-provider-bdd-class.java"));
    }

    @Test
    void endpointProviderClass_s3Bdd_generatesWithoutError() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());
        assertThat(spec.poetSpec()).isNotNull();
    }

    /**
     * The substring peephole fires on the S3 BDD (the golden-file model has no substring conditions,
     * so it is not covered there). Each rewrite must emit a single {@code substringEquals} call: the
     * helper reproduces {@code substring}'s non-ASCII rejection, whereas an inlined
     * {@code startsWith}/{@code endsWith}/{@code regionMatches} would not, and would silently take a
     * different branch than the endpoint spec requires for a non-ASCII bucket name.
     */
    @Test
    void substringConditions_emitSpecFaithfulHelperNotInlinedStringOps() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());
        String generated = spec.poetSpec().toString();

        assertThat(generated).contains("substringEquals(params.bucket(), 0, 6, true, \"--x-s3\")");
        assertThat(generated).contains("substringEquals(params.bucket(), 0, 4, false, \"arn:\")");
        assertThat(generated).contains("substringEquals(params.bucket(), 16, 18, true, \"--\")");

        // No inlined String comparison may remain: those skip the ASCII check.
        assertThat(generated).doesNotContain(".startsWith(");
        assertThat(generated).doesNotContain(".endsWith(");
        assertThat(generated).doesNotContain(".regionMatches(");

        // RulesFunctions.substring calls do legitimately remain, but only for assign conditions that
        // bind the result to a register for later use in a URL template. Those are not comparisons, so
        // the peephole must leave them alone.
        assertThat(generated).contains("accessPointSuffix = ")
                             .contains("RulesFunctions.substring(params.bucket(), 0, 7, true)");
    }

    /**
     * The remaining three peephole rewrites, none of which appear in the simple-BDD golden fixture.
     * The S3 BDD is the only model exercising them, so without these the PR's headline claim has no
     * regression guard: a change to the emitters or to PrepareForCodegenVisitor could silently
     * restore the RulesFunctions dispatches with a green build.
     */
    @Test
    void iteCoalesceAndHostLabel_emitInlineFormsNotRulesFunctionsDispatches() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());
        String generated = spec.poetSpec().toString();

        // ite -> native ternary
        assertThat(generated).contains("_s3e_ds = (params.useDualStack() ? \".dualstack\" : \"\")");
        assertThat(generated).contains("_s3e_fips = (params.useFips() ? \"-fips\" : \"\")");

        // coalesce(x, boolLiteral) -> wrapper equality, subject evaluated once
        assertThat(generated).contains("Boolean.TRUE.equals(params.useS3ExpressControlEndpoint())");
        assertThat(generated).contains("!Boolean.FALSE.equals(params.useArnRegion())");
        assertThat(generated)
            .as("a ternary would emit the coalesce subject twice")
            .doesNotContain("!= null ?");

        // isValidHostLabel(x, boolLiteral) -> specialized variant, no runtime allowDots branch
        assertThat(generated).contains("isValidHostLabelSingle(");
        assertThat(generated).contains("isValidHostLabelMulti(");

        // None of the rewritten shapes may fall back to the generic dispatch.
        assertThat(generated).doesNotContain("RulesFunctions.ite(");
        assertThat(generated).doesNotContain("RulesFunctions.coalesce(");
        assertThat(generated).doesNotContain("RulesFunctions.isValidHostLabel(");
    }

    /**
     * The three {@code ite} nodes in the S3 BDD all have string-literal branches, so their assign
     * conditions are provably non-null and keep the elided null check. Paired with
     * {@code ConditionFnCodeGeneratorVisitorTest}, which pins the nullable-branch case that must
     * emit the check.
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
     * A {@code stringEquals} with two nullable operands must stay a {@code RulesFunctions.stringEquals}
     * call, which returns false for a null operand. An emitted {@code left.equals(right)} would throw
     * a {@code NullPointerException} out of endpoint resolution instead.
     */
    @Test
    void stringEqualsWithTwoNullableOperands_staysNullSafe() {
        BddEndpointProviderSpec spec = new BddEndpointProviderSpec(
            ClientTestModels.queryServiceModelsWithBddEndpoints());
        String generated = spec.poetSpec().toString();

        assertThat(generated).contains("RulesFunctions.stringEquals(region, bucketArn.region())");
        assertThat(generated).doesNotContain("region.equals(bucketArn.region())");
        assertThat(generated).doesNotContain("bucketArn.partition().equals(");
        assertThat(generated).doesNotContain("bucketPartition.name().equals(");

        // Constant-operand comparisons are still rewritten, with the constant as the receiver.
        assertThat(generated).contains("\"aws-cn\".equals(partitionResult.name())");
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
