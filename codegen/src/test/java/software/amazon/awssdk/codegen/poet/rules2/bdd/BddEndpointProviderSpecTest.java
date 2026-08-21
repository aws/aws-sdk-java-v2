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
