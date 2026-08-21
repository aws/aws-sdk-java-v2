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
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.smithy.model.Model;

/**
 * Covers the endpoint models reaching the intermediate model from the service's traits. The Smithy
 * path takes no sidecar input, so a service without the traits lands on the same defaults the C2J
 * path uses when the sidecar files are absent.
 */
class SmithyIntermediateModelBuilderTest {

    /**
     * {@code clientContextParams} binds the rule-set parameter to the service model, which the
     * rules-engine validator requires for any parameter that is not a registered built-in.
     */
    private static final String RULE_SET =
        "@smithy.rules#clientContextParams(\n"
        + "  Region: { type: \"string\", documentation: \"The region\" }\n"
        + ")\n"
        + "@smithy.rules#endpointRuleSet({\n"
        + "  version: \"1.0\"\n"
        + "  parameters: {\n"
        + "    Region: { required: false, type: \"string\" }\n"
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
        + "      documentation: \"from the trait\"\n"
        + "      params: { Region: \"us-east-1\" }\n"
        + "      expect: { endpoint: { url: \"https://example.amazonaws.com\" } }\n"
        + "    }\n"
        + "  ]\n"
        + "})\n";

    private static IntermediateModel build(String serviceTraits) {
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
        return new SmithyIntermediateModelBuilder(
            SmithyModels.builder()
                        .model(model)
                        .customizationConfig(CustomizationConfig.create())
                        .build()).build();
    }

    @Test
    void endpointTraitsPresent_reachTheIntermediateModel() {
        IntermediateModel model = build(RULE_SET + TESTS);

        assertThat(model.getEndpointRuleSetModel().getVersion()).isEqualTo("1.0");
        assertThat(model.getEndpointRuleSetModel().getParameters()).containsOnlyKeys("Region");
        assertThat(model.getEndpointTestSuiteModel().getTestCases()).hasSize(1);
    }

    /**
     * Neither getter returns null. {@code IntermediateModel} substitutes an empty test suite, and
     * {@code EndpointRuleSetModel.defaultRules(endpointPrefix)} for the rule-set, so a service
     * without the traits degrades to the generic rules exactly as a C2J service with no sidecar does.
     */
    @Test
    void endpointTraitsAbsent_fallBackToTheDefaultRules() {
        IntermediateModel model = build("");

        assertThat(model.getEndpointRuleSetModel())
            .usingRecursiveComparison()
            .isEqualTo(EndpointRuleSetModel.defaultRules(model.getMetadata().getEndpointPrefix()));
        assertThat(model.getEndpointTestSuiteModel().getTestCases()).isEmpty();
    }
}
