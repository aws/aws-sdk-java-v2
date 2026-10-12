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

package software.amazon.awssdk.codegen.smithy.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.REST_JSON_PREFIX;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.model;

import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.SmithyBuildResult;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;

/**
 * Runs the codegen transforms through real smithy-build, mixed with Smithy built-ins, and checks what reaches the
 * intermediate model.
 */
class SmithyBuildTransformsTest {

    private static final ClassLoader LOADER = SmithyBuildTransformsTest.class.getClassLoader();

    private static final Model MODEL = model(
        REST_JSON_PREFIX
        + "service DemoService { version: \"2024-01-01\", operations: [Upload] }\n"
        + "@http(method: \"POST\", uri: \"/upload\")\n"
        + "operation Upload { input: UploadRequest, output: UploadResponse }\n"
        + "structure UploadRequest {\n"
        + "  @httpQuery(\"return\") return: String\n"
        + "  Amount: String\n"
        + "  Filter: Filter\n"
        + "  Legacy: String\n"
        + "}\n"
        + "structure UploadResponse { Object: Object }\n"
        + "structure Filter { Prefix: String, Tag: String }\n"
        + "structure Object { Key: String }\n");

    private static final String CONFIG =
        "{\"version\": \"1.0\", \"projections\": {\"sdk\": {\"transforms\": ["
        + "{\"name\": \"renameShapes\", \"args\": {\"renamed\": {\"demo#Object\": \"demo#S3Object\"}}},"
        + "{\"name\": \"excludeShapesBySelector\", \"args\": {\"selector\": \"[id = 'demo#UploadRequest$Legacy']\"}},"
        + "{\"name\": \"changeTypes\", \"args\": {\"shapeTypes\": {\"demo#Filter\": \"union\"}}},"
        + "{\"name\": \"renameMembers\", \"args\": {\"renamed\": {"
        + "  \"demo#UploadRequest$return\": {\"name\": \"returnValues\"}}}},"
        + "{\"name\": \"addShapes\", \"args\": {\"shapes\": {\"demo#SdkPartType\": {\"type\": \"enum\", \"members\": {"
        + "  \"DEFAULT\": {\"target\": \"smithy.api#Unit\", \"traits\": {\"smithy.api#enumValue\": \"DEFAULT\"}},"
        + "  \"LAST\": {\"target\": \"smithy.api#Unit\", \"traits\": {\"smithy.api#enumValue\": \"LAST\"}}}}}}},"
        + "{\"name\": \"injectMembers\", \"args\": {\"members\": {"
        + "  \"demo#UploadRequest$ContentLength\": {\"target\": \"smithy.api#Long\","
        + "    \"traits\": {\"smithy.api#httpHeader\": \"Content-Length\","
        + "               \"smithy.api#suppress\": [\"HttpHeaderTrait\"]}},"
        + "  \"demo#UploadRequest$SdkPartType\": {\"target\": \"demo#SdkPartType\"}}}},"
        + "{\"name\": \"retargetMembers\", \"args\": {\"targets\": {\"demo#UploadRequest$Amount\": \"smithy.api#BigDecimal\"}}}"
        + "]}}}";

    @Test
    void codegenTransforms_areDiscoveredByName() {
        for (String name : Arrays.asList("renameMembers", "injectMembers", "retargetMembers", "addShapes")) {
            assertThat(ProjectionTransformer.createServiceFactory(LOADER).apply(name))
                .as("transform %s resolves through META-INF/services", name)
                .isPresent();
        }
    }

    @Test
    void transformedModel_reachesTheIntermediateModel(@TempDir Path output) {
        SmithyBuildResult result = SmithyBuild.create(LOADER)
                                              .config(SmithyBuildConfig.fromNode(Node.parse(CONFIG)))
                                              .model(MODEL)
                                              .outputDirectory(output)
                                              .build();
        assertThat(result.anyBroken())
            .as("projection events: %s", result.getProjectionResult("sdk").get().getEvents())
            .isFalse();
        Model projected = result.getProjectionResult("sdk").get().getModel();

        IntermediateModel model = new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                                                 .model(projected)
                                                                                 .customizationConfig(
                                                                                     CustomizationConfig.create())
                                                                                 .build())
            .build();
        assertThat(model.getMetadata().getProtocol()).isEqualTo(Protocol.REST_JSON);

        ShapeModel request = Utils.findShapeModelByC2jName(model, "UploadRequest");
        assertThat(request.getMembers()).extracting(MemberModel::getC2jName)
                                        .containsExactly("Amount", "Filter", "returnValues", "ContentLength",
                                                         "SdkPartType");

        MemberModel returnValues = request.findMemberModelByC2jName("returnValues");
        assertThat(returnValues.getFluentGetterMethodName()).isEqualTo("returnValues");
        assertThat(returnValues.getHttp().getMarshallLocationName()).isEqualTo("return");

        MemberModel contentLength = request.findMemberModelByC2jName("ContentLength");
        assertThat(contentLength.getHttp().getMarshallLocationName()).isEqualTo("Content-Length");
        assertThat(contentLength.getVariable().getVariableType()).isEqualTo("Long");

        assertThat(request.findMemberModelByC2jName("Amount").getVariable().getVariableType())
            .isEqualTo("java.math.BigDecimal");
        assertThat(Utils.findShapeModelByC2jName(model, "SdkPartType").getEnums())
            .extracting(e -> e.getValue())
            .containsExactly("DEFAULT", "LAST");
        assertThat(Utils.findShapeModelByC2jName(model, "Filter").isUnion()).isTrue();
        assertThat(Utils.findShapeModelByC2jNameIfExists(model, "S3Object")).isNotNull();
        assertThat(Utils.findShapeModelByC2jNameIfExists(model, "Object")).isNull();
    }
}
