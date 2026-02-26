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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.OperationModifier;
import software.amazon.awssdk.codegen.model.config.customization.SmithyOperationModifier;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Tests for {@link OperationModifiersProcessor}.
 *
 * <p><b>Property 15: Operation Exclusion</b> — verify excluded operation shape removed from model.
 * <p><b>Validates: Requirement 11.1</b>
 *
 * <p><b>Property 16: Operation Result Wrapping</b> — verify wrapper structure created with correct member and
 * target, operation output updated.
 * <p><b>Validates: Requirement 11.2</b>
 */
class OperationModifiersProcessorTest {

    private static final String NAMESPACE = "com.example.testservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestService");

    private ServiceShape service;

    @BeforeEach
    void setUp() {
        service = ServiceShape.builder()
                              .id(SERVICE_ID)
                              .version("2024-01-01")
                              .build();
    }

    // -----------------------------------------------------------------------
    // Helper: build a model with the service and given operations/shapes
    // so that operation shapes are in the service closure.
    // -----------------------------------------------------------------------

    private Model buildModel(OperationShape operation, StructureShape inputShape,
                             StructureShape outputShape, Object... extraShapes) {
        ServiceShape svc = service.toBuilder()
                                  .addOperation(operation.getId())
                                  .build();
        Model.Builder builder = Model.builder()
                                     .addShape(svc)
                                     .addShape(operation)
                                     .addShape(inputShape)
                                     .addShape(outputShape);
        for (Object extra : extraShapes) {
            builder.addShape((software.amazon.smithy.model.shapes.Shape) extra);
        }
        return builder.build();
    }

    // -----------------------------------------------------------------------
    // Property 15: Operation Exclusion
    // Validates: Requirement 11.1
    // -----------------------------------------------------------------------

    /**
     * Property 15: Operation Exclusion.
     * When exclude is true for an operation, the operation shape is removed
     * from the model.
     * Validates: Requirement 11.1
     */
    @Test
    void preprocess_excludeOperation_removesOperationShapeFromModel() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperationInput"))
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperationOutput"))
            .build();
        OperationShape operation = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperation"))
            .input(inputShape.getId())
            .output(outputShape.getId())
            .build();

        Model model = buildModel(operation, inputShape, outputShape);

        SmithyOperationModifier modifier = new SmithyOperationModifier();
        modifier.setExclude(true);

        Map<String, SmithyOperationModifier> config = Collections.singletonMap("TestOperation", modifier);
        OperationModifiersProcessor processor = new OperationModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Operation shape should be removed
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#TestOperation"))).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Property 16: Operation Result Wrapping
    // Validates: Requirement 11.2
    // -----------------------------------------------------------------------

    /**
     * Property 16: Operation Result Wrapping.
     * When useWrappingResult is true, a wrapper structure is created with
     * the correct member and target, and the operation output is updated.
     * Validates: Requirement 11.2
     */
    @Test
    void preprocess_useWrappingResult_createsWrapperAndUpdatesOperationOutput() {
        StructureShape wrappedResultShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#Reservation"))
            .addMember("instanceId", ShapeId.from("smithy.api#String"))
            .build();
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#RunInstancesInput"))
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#RunInstancesOutput"))
            .build();
        OperationShape operation = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#RunInstances"))
            .input(inputShape.getId())
            .output(outputShape.getId())
            .build();

        Model model = buildModel(operation, inputShape, outputShape, wrappedResultShape);

        SmithyOperationModifier modifier = new SmithyOperationModifier();
        modifier.setUseWrappingResult(true);
        modifier.setWrappedResultShape(NAMESPACE + "#Reservation");
        modifier.setWrappedResultMember("reservation");

        Map<String, SmithyOperationModifier> config = Collections.singletonMap("RunInstances", modifier);
        OperationModifiersProcessor processor = new OperationModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Wrapper structure should exist
        ShapeId wrapperId = ShapeId.from(NAMESPACE + "#RunInstancesResponse");
        assertThat(result.getShape(wrapperId)).isPresent();
        StructureShape wrapper = result.expectShape(wrapperId, StructureShape.class);

        // Wrapper should have a member named "reservation" targeting Reservation
        assertThat(wrapper.getMember("reservation")).isPresent();
        assertThat(wrapper.getMember("reservation").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#Reservation"));

        // Operation output should point to the wrapper
        OperationShape updatedOp = result.expectShape(
            ShapeId.from(NAMESPACE + "#RunInstances"), OperationShape.class);
        assertThat(updatedOp.getOutputShape()).isEqualTo(wrapperId);
    }

    // -----------------------------------------------------------------------
    // Missing operation throws IllegalStateException
    // Validates: Requirement 11.3
    // -----------------------------------------------------------------------

    /**
     * Referencing a non-existent operation throws IllegalStateException.
     * Validates: Requirement 11.3
     */
    @Test
    void preprocess_missingOperation_throwsIllegalStateException() {
        Model model = Model.builder().addShape(service).build();

        SmithyOperationModifier modifier = new SmithyOperationModifier();
        modifier.setExclude(true);

        Map<String, SmithyOperationModifier> config = Collections.singletonMap("NonExistentOperation", modifier);
        OperationModifiersProcessor processor = new OperationModifiersProcessor(null, config);

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NonExistentOperation")
            .hasMessageContaining(NAMESPACE);
    }

    // -----------------------------------------------------------------------
    // Null/empty config returns model unchanged
    // Validates: Requirement 11.4
    // -----------------------------------------------------------------------

    /**
     * Both configs null → returns model unchanged.
     * Validates: Requirement 11.4
     */
    @Test
    void preprocess_when_bothConfigsNull_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        OperationModifiersProcessor processor = new OperationModifiersProcessor(null, null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty new config map → returns model unchanged.
     * Validates: Requirement 11.4
     */
    @Test
    void preprocess_when_emptyNewConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        OperationModifiersProcessor processor =
            new OperationModifiersProcessor(null, Collections.emptyMap());

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty old config map → returns model unchanged.
     * Validates: Requirement 11.4
     */
    @Test
    void preprocess_when_emptyOldConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        OperationModifiersProcessor processor =
            new OperationModifiersProcessor(Collections.emptyMap(), null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    // -----------------------------------------------------------------------
    // Old-to-new conversion resolves wrappedResultShape
    // Validates: Requirements 11.6, 30.4
    // -----------------------------------------------------------------------

    /**
     * Old config conversion: wrappedResultShape simple name is resolved to full ShapeId.
     * Validates: Requirements 11.6, 30.4
     */
    @Test
    void preprocess_oldConfig_resolvesWrappedResultShapeSimpleNameToFullShapeId() {
        StructureShape wrappedResultShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#Reservation"))
            .addMember("instanceId", ShapeId.from("smithy.api#String"))
            .build();
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#RunInstancesInput"))
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#RunInstancesOutput"))
            .build();
        OperationShape operation = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#RunInstances"))
            .input(inputShape.getId())
            .output(outputShape.getId())
            .build();

        Model model = buildModel(operation, inputShape, outputShape, wrappedResultShape);

        // Old config uses simple name for wrappedResultShape
        OperationModifier oldModifier = new OperationModifier();
        oldModifier.setUseWrappingResult(true);
        oldModifier.setWrappedResultShape("Reservation");
        oldModifier.setWrappedResultMember("reservation");

        Map<String, OperationModifier> oldConfig = Collections.singletonMap("RunInstances", oldModifier);
        OperationModifiersProcessor processor = new OperationModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Wrapper should exist and member should target the resolved full ShapeId
        ShapeId wrapperId = ShapeId.from(NAMESPACE + "#RunInstancesResponse");
        assertThat(result.getShape(wrapperId)).isPresent();
        StructureShape wrapper = result.expectShape(wrapperId, StructureShape.class);
        assertThat(wrapper.getMember("reservation").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#Reservation"));

        // Operation output should point to the wrapper
        OperationShape updatedOp = result.expectShape(
            ShapeId.from(NAMESPACE + "#RunInstances"), OperationShape.class);
        assertThat(updatedOp.getOutputShape()).isEqualTo(wrapperId);
    }

    /**
     * Old config conversion: exclude flag is preserved during conversion.
     * Validates: Requirements 11.6, 30.4
     */
    @Test
    void preprocess_oldConfig_excludeFlagPreservedDuringConversion() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperationInput"))
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperationOutput"))
            .build();
        OperationShape operation = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperation"))
            .input(inputShape.getId())
            .output(outputShape.getId())
            .build();

        Model model = buildModel(operation, inputShape, outputShape);

        OperationModifier oldModifier = new OperationModifier();
        oldModifier.setExclude(true);

        Map<String, OperationModifier> oldConfig = Collections.singletonMap("TestOperation", oldModifier);
        OperationModifiersProcessor processor = new OperationModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Operation should be removed
        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#TestOperation"))).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Dual-config mutual exclusion (inherited from AbstractDualConfigProcessor)
    // -----------------------------------------------------------------------

    /**
     * Both old and new config set → throws IllegalStateException.
     */
    @Test
    void preprocess_when_bothConfigsSet_throwsIllegalStateException() {
        Map<String, OperationModifier> oldConfig = new HashMap<>();
        oldConfig.put("SomeOperation", new OperationModifier());

        Map<String, SmithyOperationModifier> newConfig = new HashMap<>();
        newConfig.put("SomeOperation", new SmithyOperationModifier());

        OperationModifiersProcessor processor = new OperationModifiersProcessor(oldConfig, newConfig);

        Model model = Model.builder().addShape(service).build();

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("operationModifiers")
            .hasMessageContaining("smithyOperationModifiers");
    }
}
