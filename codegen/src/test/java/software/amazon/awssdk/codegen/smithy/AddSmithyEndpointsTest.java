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

package software.amazon.awssdk.codegen.smithy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Unit tests for {@link AddSmithyEndpoints}, covering trait present, trait absent, and the
 * lower-case parameter {@code type} the traits use where the sidecar files capitalise it.
 */
class AddSmithyEndpointsTest {

    /**
     * Rule-set parameters must be bound in the service model, and AWS built-ins like
     * {@code AWS::Region} are registered by smithy-aws-endpoints, which codegen does not depend on.
     * Binding through {@code clientContextParams} keeps the model self-contained.
     */
    private static final String RULE_SET =
        "@smithy.rules#clientContextParams(\n"
        + "  Region: { type: \"string\", documentation: \"The region\" }\n"
        + "  UseFIPS: { type: \"boolean\", documentation: \"Use FIPS endpoints\" }\n"
        + ")\n"
        + "@smithy.rules#endpointRuleSet({\n"
        + "  version: \"1.0\"\n"
        + "  parameters: {\n"
        + "    Region: { required: false, documentation: \"The region\", type: \"string\" }\n"
        + "    UseFIPS: { required: true, default: false, type: \"boolean\" }\n"
        + "  }\n"
        + "  rules: [\n"
        + "    { conditions: [], endpoint: { url: \"https://example.amazonaws.com\" }, type: \"endpoint\" }\n"
        + "  ]\n"
        + "})\n";

    private static final String TESTS =
        "@smithy.rules#endpointTests({\n"
        + "  version: \"1.0\"\n"
        + "  testCases: [\n"
        + "    {\n"
        + "      documentation: \"basic\"\n"
        + "      params: { Region: \"us-east-1\", UseFIPS: false }\n"
        + "      expect: { endpoint: { url: \"https://example.amazonaws.com\" } }\n"
        + "    }\n"
        + "  ]\n"
        + "})\n";

    private static ServiceShape serviceOf(String serviceTraits) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + "@restJson1\n"
            + serviceTraits
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse {}\n";
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("test.smithy", src)
                           .assemble()
                           .unwrap();
        return model.getServiceShapes().iterator().next();
    }

    @Test
    void ruleSetTraitPresent_isTranslated() {
        EndpointRuleSetModel ruleSet = AddSmithyEndpoints.endpointRuleSet(serviceOf(RULE_SET));

        assertThat(ruleSet).isNotNull();
        assertThat(ruleSet.getVersion()).isEqualTo("1.0");
        assertThat(ruleSet.getParameters()).containsOnlyKeys("Region", "UseFIPS");
        assertThat(ruleSet.getRules()).hasSize(1);
    }

    @Test
    void ruleSetTraitAbsent_isNull() {
        assertThat(AddSmithyEndpoints.endpointRuleSet(serviceOf(""))).isNull();
    }

    @Test
    void endpointTestsTraitPresent_isTranslated() {
        EndpointTestSuiteModel tests = AddSmithyEndpoints.endpointTests(serviceOf(RULE_SET + TESTS));

        assertThat(tests).isNotNull();
        assertThat(tests.getTestCases()).hasSize(1);
    }

    @Test
    void endpointTestsTraitAbsent_isNull() {
        assertThat(AddSmithyEndpoints.endpointTests(serviceOf(""))).isNull();
    }

    /**
     * The traits write {@code "string"} where the sidecar files write {@code "String"}. The value is
     * carried through verbatim; every consumer lower-cases before switching on it.
     */
    @Test
    void parameterType_keepsTheTraitsLowerCaseForm() {
        EndpointRuleSetModel ruleSet = AddSmithyEndpoints.endpointRuleSet(serviceOf(RULE_SET));

        assertThat(ruleSet.getParameters().get("Region").getType()).isEqualTo("string");
        assertThat(ruleSet.getParameters().get("UseFIPS").getType()).isEqualTo("boolean");
    }
}
