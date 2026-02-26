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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.TitleTrait;

/**
 * Integration tests for the processor pipeline in {@link SmithyIntermediateModelBuilder#build()}.
 *
 * <p>These tests verify that the processor chain (preprocess and postprocess) is correctly
 * invoked during the build, using real customization configs that trigger observable effects
 * in the IntermediateModel.
 *
 * <p><b>Property 1: Service Shape Preservation</b>
 * <p><b>Validates: Requirements 6.2, 6.3, 6.4, 6.5, 6.6</b>
 */
class SmithyIntermediateModelBuilderProcessorIntegrationTest {

    private static final String NAMESPACE = "com.example.test";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestSvc");
    private static final ShapeId OPERATION_ID = ShapeId.from(NAMESPACE + "#GetItem");
    private static final ShapeId INPUT_ID = ShapeId.from(NAMESPACE + "#GetItemRequest");
    private static final ShapeId OUTPUT_ID = ShapeId.from(NAMESPACE + "#GetItemResponse");
    private static final ShapeId ERROR_ID = ShapeId.from(NAMESPACE + "#ItemNotFoundException");
    private static final ShapeId ITEM_DATA_ID = ShapeId.from(NAMESPACE + "#ItemData");

    /**
     * Test 1: Empty config does not break the pipeline and service metadata is preserved.
     *
     * <p><b>Property 1: Service Shape Preservation</b>
     * <p><b>Validates: Requirements 6.3, 6.4</b>
     */
    @Test
    void build_withEmptyConfig_serviceShapePreserved() {
        Model model = buildMinimalModel();
        CustomizationConfig config = CustomizationConfig.create();

        SmithyModelWithCustomizations modelWithCustomizations = SmithyModelWithCustomizations.builder()
            .smithyModel(model)
            .customizationConfig(config)
            .build();
        IntermediateModel result = new SmithyIntermediateModelBuilder(modelWithCustomizations).build();

        assertThat(result).isNotNull();
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata().getServiceName()).isEqualTo("TestSvc");
    }

    /**
     * Test 2: RenameShapes config causes the renamed shape to appear in the IntermediateModel.
     * This proves preprocess runs before IntermediateModel construction.
     *
     * <p>We rename a model shape (ItemData → ItemInfo) rather than an input shape, because
     * input shape keys in the IM are derived from the operation name, not the Smithy shape name.
     * Model shapes use {@code namingStrategy.getShapeClassName(shapeId.getName())} as the key,
     * so a Smithy rename is directly observable in the IM.
     *
     * <p><b>Validates: Requirement 6.2</b>
     */
    @Test
    void build_withRenameShapes_renamedShapeAppearsInIntermediateModel() {
        Model model = buildMinimalModel();
        CustomizationConfig config = CustomizationConfig.create();
        Map<String, String> renameShapes = new HashMap<>();
        renameShapes.put("ItemData", "ItemInfo");
        config.setRenameShapes(renameShapes);

        SmithyModelWithCustomizations modelWithCustomizations = SmithyModelWithCustomizations.builder()
            .smithyModel(model)
            .customizationConfig(config)
            .build();
        IntermediateModel result = new SmithyIntermediateModelBuilder(modelWithCustomizations).build();

        assertThat(result.getShapes()).containsKey("ItemInfo");
        assertThat(result.getShapes()).doesNotContainKey("ItemData");
    }

    /**
     * Test 3: The RemoveExceptionMessageProcessor removes the message member from
     * exception shapes during postprocess.
     *
     * <p><b>Validates: Requirement 6.6</b>
     */
    @Test
    void build_withExceptionShape_messageFieldRemovedByPostprocess() {
        Model model = buildMinimalModel();
        CustomizationConfig config = CustomizationConfig.create();

        SmithyModelWithCustomizations modelWithCustomizations = SmithyModelWithCustomizations.builder()
            .smithyModel(model)
            .customizationConfig(config)
            .build();
        IntermediateModel result = new SmithyIntermediateModelBuilder(modelWithCustomizations).build();

        ShapeModel exceptionShape = result.getShapes().get("ItemNotFoundException");
        assertThat(exceptionShape)
            .as("Exception shape should exist in IntermediateModel")
            .isNotNull();
        if (exceptionShape.getMembers() != null) {
            boolean hasMessage = exceptionShape.getMembers().stream()
                .anyMatch(m -> "message".equalsIgnoreCase(m.getC2jName()));
            assertThat(hasMessage)
                .as("message member should be removed from exception shape by postprocess")
                .isFalse();
        }
    }

    /**
     * Test 4: After build with renameShapes, the builder's fields reflect the preprocessed model.
     * The smithyModel contains the renamed shape, the service is re-resolved, and the
     * serviceIndex is rebuilt.
     *
     * <p><b>Validates: Requirements 6.3, 6.4, 6.5</b>
     */
    @Test
    void build_withRenameShapes_fieldsReassignedAfterPreprocessing() {
        Model model = buildMinimalModel();
        CustomizationConfig config = CustomizationConfig.create();
        Map<String, String> renameShapes = new HashMap<>();
        renameShapes.put("ItemData", "ItemInfo");
        config.setRenameShapes(renameShapes);

        SmithyModelWithCustomizations modelWithCustomizations = SmithyModelWithCustomizations.builder()
            .smithyModel(model)
            .customizationConfig(config)
            .build();
        SmithyIntermediateModelBuilder builder = new SmithyIntermediateModelBuilder(modelWithCustomizations);
        builder.build();

        // After build, the smithyModel should contain the renamed shape
        Model postBuildModel = builder.getSmithyModel();
        ShapeId renamedId = ShapeId.from(NAMESPACE + "#ItemInfo");
        assertThat(postBuildModel.getShape(renamedId)).isPresent();

        // The original shape should NOT exist
        ShapeId originalId = ShapeId.from(NAMESPACE + "#ItemData");
        assertThat(postBuildModel.getShape(originalId)).isNotPresent();

        // The service shape should still be present (re-resolved)
        assertThat(builder.getService()).isNotNull();
        assertThat(builder.getService().getId()).isEqualTo(SERVICE_ID);

        // The serviceIndex should be non-null (rebuilt from preprocessed model)
        assertThat(builder.getServiceIndex()).isNotNull();
    }

    // --- Helper methods ---

    /**
     * Builds a minimal Smithy model with one service, one operation, input/output/error shapes,
     * and a model shape (ItemData) referenced by the output. This provides enough structure for
     * the SmithyIntermediateModelBuilder to produce a valid IntermediateModel.
     */
    private static Model buildMinimalModel() {
        StructureShape itemData = StructureShape.builder()
            .id(ITEM_DATA_ID)
            .addMember("value", ShapeId.from("smithy.api#String"))
            .build();

        StructureShape input = StructureShape.builder()
            .id(INPUT_ID)
            .addMember("itemId", ShapeId.from("smithy.api#String"))
            .build();

        StructureShape output = StructureShape.builder()
            .id(OUTPUT_ID)
            .addMember("item", ITEM_DATA_ID)
            .build();

        StructureShape error = StructureShape.builder()
            .id(ERROR_ID)
            .addTrait(new ErrorTrait("client"))
            .addMember("message", ShapeId.from("smithy.api#String"))
            .build();

        OperationShape operation = OperationShape.builder()
            .id(OPERATION_ID)
            .input(INPUT_ID)
            .output(OUTPUT_ID)
            .addError(ERROR_ID)
            .addTrait(HttpTrait.builder()
                .uri(UriPattern.parse("/getItem"))
                .method("POST")
                .code(200)
                .build())
            .build();

        ServiceShape service = ServiceShape.builder()
            .id(SERVICE_ID)
            .version("2024-01-01")
            .addOperation(OPERATION_ID)
            .addTrait(ServiceTrait.builder()
                .sdkId("TestSvc")
                .arnNamespace("testsvc")
                .cloudFormationName("TestSvc")
                .cloudTrailEventSource("testsvc.amazonaws.com")
                .build())
            .addTrait(SigV4Trait.builder().name("testsvc").build())
            .addTrait(RestJson1Trait.builder().build())
            .addTrait(new TitleTrait("Test Svc"))
            .build();

        return Model.assembler()
            .addShape(service)
            .addShape(operation)
            .addShape(input)
            .addShape(output)
            .addShape(error)
            .addShape(itemData)
            .discoverModels(SmithyIntermediateModelBuilderProcessorIntegrationTest.class.getClassLoader())
            .disableValidation()
            .assemble()
            .unwrap();
    }
}
