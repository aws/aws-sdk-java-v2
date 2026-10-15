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
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.smithy.model.Model;

/**
 * Covers what the builder takes from the service model instead of C2J sidecar files: the endpoint models, and the
 * paginators with the {@code isPaginated} flag that must agree with them. A service without the endpoint traits
 * lands on the same defaults the C2J path uses when the sidecar files are absent.
 */
class SmithyIntermediateModelBuilderTest {

    private static final ClassLoader LOADER = SmithyIntermediateModelBuilderTest.class.getClassLoader();

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

    private static IntermediateModel buildPaginated(CustomizationConfig config) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "use software.amazon.awssdk.codegen#paginatorOverride\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + "@restJson1\n"
            + "service DemoService { version: \"2024-01-01\", operations: [ListThings, ListOld, ListExpr, Get] }\n\n"
            + "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\", items: \"things\")\n"
            + "@readonly @http(method: \"GET\", uri: \"/things\")\n"
            + "operation ListThings { input: ListRequest, output: ListResponse }\n"
            + "@paginated(inputToken: \"nextToken\", outputToken: \"nextToken\", items: \"things\")\n"
            + "@readonly @http(method: \"GET\", uri: \"/old\")\n"
            + "operation ListOld { input: ListRequest, output: ListResponse }\n"
            + "@paginatorOverride(inputToken: [\"nextToken\"], outputToken: [\"nextToken || things[-1]\"])\n"
            + "@readonly @http(method: \"GET\", uri: \"/expr\")\n"
            + "operation ListExpr { input: ListRequest, output: ListResponse }\n"
            + "@readonly @http(method: \"GET\", uri: \"/get\")\n"
            + "operation Get { input: ListRequest, output: ListResponse }\n"
            + "structure ListRequest { @httpQuery(\"nextToken\") nextToken: String }\n"
            + "structure ListResponse { nextToken: String, things: Things }\n"
            + "list Things { member: String }\n";
        Model smithyModel = Model.assembler(LOADER)
                                 .discoverModels(LOADER)
                                 .addUnparsedModel("test.smithy", src)
                                 .assemble()
                                 .unwrap();
        return new SmithyIntermediateModelBuilder(
            SmithyModels.builder().model(smithyModel).customizationConfig(config).build()).build();
    }

    @Test
    void paginatedOperations_areMarkedPaginatedAndHaveDefinitions() {
        IntermediateModel model = buildPaginated(CustomizationConfig.create());

        assertThat(model.getPaginators()).containsOnlyKeys("ListThings", "ListOld", "ListExpr");
        assertThat(model.getPaginators().get("ListThings").getResultKey()).containsExactly("things");
        assertThat(model.getOperation("ListThings").isPaginated()).isTrue();
        assertThat(model.getOperation("ListOld").isPaginated()).isTrue();
        assertThat(model.getOperation("Get").isPaginated()).isFalse();
    }

    /**
     * C2J keeps such a definition in the map but generates nothing for it, so {@code isPaginated} must be false too:
     * the client interface would otherwise reference a paginator class that does not exist.
     */
    @Test
    void definitionCodegenCannotGenerate_isNotMarkedPaginated() {
        IntermediateModel model = buildPaginated(CustomizationConfig.create());

        assertThat(model.getPaginators().get("ListExpr").hasAllRequiredFields()).isFalse();
        assertThat(model.getOperation("ListExpr").isPaginated()).isFalse();
    }

    @Test
    void deprecatedOperation_losesItsPaginator() {
        CustomizationConfig config = CustomizationConfig.create();
        config.setDeprecatedOperations(Collections.singletonList("ListOld"));

        IntermediateModel model = buildPaginated(config);

        assertThat(model.getOperations()).doesNotContainKey("ListOld");
        assertThat(model.getPaginators()).containsOnlyKeys("ListThings", "ListExpr");
    }

    @Test
    void stringWithEnumTrait_isTranslatedAsEnum() {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + "@restJson1\n"
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { color: Color }\n"
            + "structure OpResponse {}\n"
            + "@enum([{ value: \"red\" }, { value: \"dark-blue\" }])\n"
            + "string Color\n";
        Model smithyModel = Model.assembler()
                                 .discoverModels(Model.class.getClassLoader())
                                 .addUnparsedModel("test.smithy", src)
                                 .assemble()
                                 .unwrap();

        IntermediateModel model = new SmithyIntermediateModelBuilder(
            SmithyModels.builder()
                        .model(smithyModel)
                        .customizationConfig(CustomizationConfig.create())
                        .build()).build();

        assertThat(model.getShapes().get("Color").getType()).isEqualTo(ShapeType.Enum.getValue());
        assertThat(model.getShapes().get("Color").getEnums())
            .extracting(EnumModel::getValue)
            .containsExactly("red", "dark-blue");
        assertThat(model.getShapes().get("OpRequest").findMemberModelByC2jName("color").getEnumType())
            .isEqualTo("Color");
    }

    @Test
    void enumValueSmithyCannotName_isTranslatedAsEnum() {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "use aws.protocols#restJson1\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + "@restJson1\n"
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest { color: Color }\n"
            + "structure OpResponse {}\n"
            // Smithy cannot synthesize a member name for "1.0" because it starts with a digit.
            + "@enum([{ value: \"1.0\" }, { value: \"red\" }])\n"
            + "string Color\n";
        Model smithyModel = Model.assembler()
                                 .discoverModels(Model.class.getClassLoader())
                                 .addUnparsedModel("test.smithy", src)
                                 .assemble()
                                 .unwrap();

        IntermediateModel model = new SmithyIntermediateModelBuilder(
            SmithyModels.builder()
                        .model(smithyModel)
                        .customizationConfig(CustomizationConfig.create())
                        .build()).build();

        assertThat(model.getShapes().get("Color").getType()).isEqualTo(ShapeType.Enum.getValue());
        assertThat(model.getShapes().get("Color").getEnums())
            .extracting(EnumModel::getName, EnumModel::getValue)
            .containsExactly(tuple("VALUE_1_0", "1.0"), tuple("RED", "red"));
        assertThat(model.getShapes().get("OpRequest").findMemberModelByC2jName("color").getEnumType())
            .isEqualTo("Color");
    }
}
