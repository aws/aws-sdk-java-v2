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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.ShapeSubstitution;
import software.amazon.awssdk.codegen.model.config.customization.SmithyShapeSubstitution;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Tests for {@link ShapeSubstitutionsProcessor}.
 *
 * <p><b>Property 12: Shape Substitution Retargeting</b> — verify all members targeting original shape now target
 * substitute.
 * <p><b>Validates: Requirements 9.1, 9.2, 9.3, 9.4, 30.2</b>
 */
class ShapeSubstitutionsProcessorTest {

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
    // Helper: build a model with the service, an operation, and given shapes
    // so that structure shapes are in the service closure.
    // -----------------------------------------------------------------------

    private Model buildModelWithOperation(StructureShape inputShape, StructureShape outputShape,
                                          Object... extraShapes) {
        OperationShape operation = OperationShape.builder()
                                                 .id(ShapeId.from(NAMESPACE + "#TestOperation"))
                                                 .input(inputShape.getId())
                                                 .output(outputShape.getId())
                                                 .build();
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
    // Property 12: Shape Substitution Retargeting — emitAsShape
    // Validates: Requirements 9.1, 9.2
    // -----------------------------------------------------------------------

    /**
     * Property 12: Shape Substitution Retargeting.
     * When emitAsShape is specified, all members targeting the original shape
     * are retargeted to the substitute shape.
     * Validates: Requirement 9.1
     */
    @Test
    void preprocess_emitAsShape_retargetsAllMembersToSubstituteShape() {
        // Original shape that will be substituted
        StructureShape originalShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShape"))
            .addMember("value", ShapeId.from("smithy.api#String"))
            .build();

        // Substitute shape
        StringShape substituteShape = StringShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SubstituteShape"))
            .build();

        // Two structures that reference the original shape
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("field1", originalShape.getId())
            .addMember("field2", ShapeId.from("smithy.api#Integer"))
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOutput"))
            .addMember("result", originalShape.getId())
            .build();

        Model model = buildModelWithOperation(inputShape, outputShape, originalShape, substituteShape);

        SmithyShapeSubstitution substitution = new SmithyShapeSubstitution();
        substitution.setEmitAsShape(NAMESPACE + "#SubstituteShape");

        Map<String, SmithyShapeSubstitution> config = Collections.singletonMap("OriginalShape", substitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // All members that targeted OriginalShape should now target SubstituteShape
        StructureShape resultInput = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultInput.getMember("field1").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#SubstituteShape"));
        // Non-substituted member should be unchanged
        assertThat(resultInput.getMember("field2").get().getTarget())
            .isEqualTo(ShapeId.from("smithy.api#Integer"));

        StructureShape resultOutput = result.expectShape(outputShape.getId(), StructureShape.class);
        assertThat(resultOutput.getMember("result").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#SubstituteShape"));
    }

    /**
     * Property 12: Shape Substitution Retargeting with emitFromMember.
     * When emitFromMember is specified, the member retargeting still occurs
     * and the substitution is tracked for postprocess.
     * Validates: Requirement 9.2
     */
    @Test
    void preprocess_emitAsShapeWithEmitFromMember_retargetsMembersAndTracksForPostprocess() {
        StructureShape originalShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShape"))
            .addMember("dataField", ShapeId.from("smithy.api#String"))
            .build();

        StringShape substituteShape = StringShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SubstituteShape"))
            .build();

        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("ref", originalShape.getId())
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOutput"))
            .build();

        Model model = buildModelWithOperation(inputShape, outputShape, originalShape, substituteShape);

        SmithyShapeSubstitution substitution = new SmithyShapeSubstitution();
        substitution.setEmitAsShape(NAMESPACE + "#SubstituteShape");
        substitution.setEmitFromMember("dataField");

        Map<String, SmithyShapeSubstitution> config = Collections.singletonMap("OriginalShape", substitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Member should be retargeted
        StructureShape resultInput = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultInput.getMember("ref").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#SubstituteShape"));
    }

    /**
     * Property 12: Shape Substitution Retargeting with emitAsType.
     * When emitAsType is specified, a synthetic shape is created and members
     * are retargeted to it.
     * Validates: Requirement 9.1
     */
    @Test
    void preprocess_emitAsType_createsSyntheticShapeAndRetargetsMembers() {
        StructureShape originalShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShape"))
            .addMember("value", ShapeId.from("smithy.api#String"))
            .build();

        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("field1", originalShape.getId())
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOutput"))
            .build();

        Model model = buildModelWithOperation(inputShape, outputShape, originalShape);

        SmithyShapeSubstitution substitution = new SmithyShapeSubstitution();
        substitution.setEmitAsType("string");

        Map<String, SmithyShapeSubstitution> config = Collections.singletonMap("OriginalShape", substitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Member should be retargeted to the synthetic shape
        ShapeId syntheticId = ShapeId.from(NAMESPACE + "#SdkCustomization_string");
        StructureShape resultInput = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultInput.getMember("field1").get().getTarget()).isEqualTo(syntheticId);

        // Synthetic shape should exist in the model
        assertThat(result.getShape(syntheticId)).isPresent();
        assertThat(result.expectShape(syntheticId).isStringShape()).isTrue();
    }

    /**
     * Property 12: Shape Substitution Retargeting for list members.
     * When a list shape's element targets the original shape, the list element
     * is retargeted to the substitute shape.
     * Validates: Requirement 9.1
     */
    @Test
    void preprocess_emitAsShape_retargetsListMemberElements() {
        StructureShape originalShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShape"))
            .addMember("value", ShapeId.from("smithy.api#String"))
            .build();

        StringShape substituteShape = StringShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SubstituteShape"))
            .build();

        ListShape listShape = ListShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShapeList"))
            .member(MemberShape.builder()
                .id(ShapeId.from(NAMESPACE + "#OriginalShapeList$member"))
                .target(originalShape.getId())
                .build())
            .build();

        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("items", listShape.getId())
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOutput"))
            .build();

        Model model = buildModelWithOperation(inputShape, outputShape, originalShape, substituteShape, listShape);

        SmithyShapeSubstitution substitution = new SmithyShapeSubstitution();
        substitution.setEmitAsShape(NAMESPACE + "#SubstituteShape");

        Map<String, SmithyShapeSubstitution> config = Collections.singletonMap("OriginalShape", substitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // The list's member should now target SubstituteShape
        ListShape resultList = result.expectShape(listShape.getId(), ListShape.class);
        assertThat(resultList.getMember().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#SubstituteShape"));
    }

    // -----------------------------------------------------------------------
    // Missing shape throws IllegalStateException
    // Validates: Requirement 9.3
    // -----------------------------------------------------------------------

    /**
     * Referencing a non-existent shape throws IllegalStateException.
     * Validates: Requirement 9.3
     */
    @Test
    void preprocess_missingOriginalShape_throwsIllegalStateException() {
        Model model = Model.builder().addShape(service).build();

        SmithyShapeSubstitution substitution = new SmithyShapeSubstitution();
        substitution.setEmitAsShape(NAMESPACE + "#SomeSubstitute");

        Map<String, SmithyShapeSubstitution> config = Collections.singletonMap("NonExistentShape", substitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(null, config);

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NonExistentShape")
            .hasMessageContaining(NAMESPACE);
    }

    /**
     * Referencing a non-existent emitAsShape throws IllegalStateException.
     * Validates: Requirement 9.3
     */
    @Test
    void preprocess_missingEmitAsShape_throwsIllegalStateException() {
        StructureShape originalShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShape"))
            .addMember("value", ShapeId.from("smithy.api#String"))
            .build();
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("field1", originalShape.getId())
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOutput"))
            .build();

        Model model = buildModelWithOperation(inputShape, outputShape, originalShape);

        SmithyShapeSubstitution substitution = new SmithyShapeSubstitution();
        substitution.setEmitAsShape(NAMESPACE + "#DoesNotExist");

        Map<String, SmithyShapeSubstitution> config = Collections.singletonMap("OriginalShape", substitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(null, config);

        assertThatThrownBy(() -> processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DoesNotExist");
    }

    // -----------------------------------------------------------------------
    // Null/empty config returns model unchanged
    // Validates: Requirement 9.4
    // -----------------------------------------------------------------------

    /**
     * Both configs null → returns model unchanged.
     * Validates: Requirement 9.4
     */
    @Test
    void preprocess_when_bothConfigsNull_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(null, null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty new config map → returns model unchanged.
     * Validates: Requirement 9.4
     */
    @Test
    void preprocess_when_emptyNewConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        ShapeSubstitutionsProcessor processor =
            new ShapeSubstitutionsProcessor(null, Collections.emptyMap());

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty old config map → returns model unchanged.
     * Validates: Requirement 9.4
     */
    @Test
    void preprocess_when_emptyOldConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        ShapeSubstitutionsProcessor processor =
            new ShapeSubstitutionsProcessor(Collections.emptyMap(), null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    // -----------------------------------------------------------------------
    // Old-to-new conversion resolves simple names to ShapeIds
    // Validates: Requirement 30.2
    // -----------------------------------------------------------------------

    /**
     * Old config conversion: emitAsShape simple name is resolved to full ShapeId.
     * Validates: Requirement 30.2
     */
    @Test
    void preprocess_oldConfig_resolvesEmitAsShapeSimpleNameToFullShapeId() {
        StructureShape originalShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShape"))
            .addMember("value", ShapeId.from("smithy.api#String"))
            .build();

        StringShape substituteShape = StringShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SubstituteShape"))
            .build();

        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("field1", originalShape.getId())
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOutput"))
            .build();

        Model model = buildModelWithOperation(inputShape, outputShape, originalShape, substituteShape);

        // Old config uses simple name for emitAsShape
        ShapeSubstitution oldSubstitution = new ShapeSubstitution();
        oldSubstitution.setEmitAsShape("SubstituteShape");

        Map<String, ShapeSubstitution> oldConfig = Collections.singletonMap("OriginalShape", oldSubstitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(oldConfig, null);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // The member should be retargeted to the full ShapeId of SubstituteShape
        StructureShape resultInput = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultInput.getMember("field1").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#SubstituteShape"));
    }

    /**
     * Old config conversion: skipMarshallPathForShapes simple names are resolved to full ShapeIds.
     * Validates: Requirement 30.2
     */
    @Test
    void preprocess_oldConfig_resolvesSkipMarshallPathForShapesSimpleNamesToFullShapeIds() {
        StructureShape originalShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#OriginalShape"))
            .addMember("value", ShapeId.from("smithy.api#String"))
            .build();

        StringShape substituteShape = StringShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SubstituteShape"))
            .build();

        StructureShape skipShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SkipThisShape"))
            .addMember("field1", originalShape.getId())
            .build();

        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("field1", originalShape.getId())
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOutput"))
            .build();

        OperationShape op1 = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperation"))
            .input(inputShape.getId())
            .output(outputShape.getId())
            .build();
        OperationShape op2 = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperation2"))
            .input(skipShape.getId())
            .build();
        ServiceShape svc = service.toBuilder()
            .addOperation(op1.getId())
            .addOperation(op2.getId())
            .build();
        Model model = Model.builder()
            .addShape(svc)
            .addShape(op1)
            .addShape(op2)
            .addShape(inputShape)
            .addShape(outputShape)
            .addShape(skipShape)
            .addShape(originalShape)
            .addShape(substituteShape)
            .build();

        // Old config uses simple names for both emitAsShape and skipMarshallPathForShapes
        ShapeSubstitution oldSubstitution = new ShapeSubstitution();
        oldSubstitution.setEmitAsShape("SubstituteShape");
        oldSubstitution.setEmitFromMember("value");
        oldSubstitution.setSkipMarshallPathForShapes(Arrays.asList("SkipThisShape"));

        Map<String, ShapeSubstitution> oldConfig = Collections.singletonMap("OriginalShape", oldSubstitution);
        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(oldConfig, null);

        // Should not throw — simple names should be resolved to full ShapeIds during conversion
        Model result = processor.preprocess(model, svc);

        // Both members should be retargeted
        StructureShape resultInput = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultInput.getMember("field1").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#SubstituteShape"));

        StructureShape resultSkip = result.expectShape(skipShape.getId(), StructureShape.class);
        assertThat(resultSkip.getMember("field1").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#SubstituteShape"));
    }

    // -----------------------------------------------------------------------
    // Dual-config mutual exclusion (inherited from AbstractDualConfigProcessor)
    // -----------------------------------------------------------------------

    /**
     * Both old and new config set → throws IllegalStateException.
     */
    @Test
    void preprocess_when_bothConfigsSet_throwsIllegalStateException() {
        Map<String, ShapeSubstitution> oldConfig = new HashMap<>();
        oldConfig.put("SomeShape", new ShapeSubstitution());

        Map<String, SmithyShapeSubstitution> newConfig = new HashMap<>();
        newConfig.put("SomeShape", new SmithyShapeSubstitution());

        ShapeSubstitutionsProcessor processor = new ShapeSubstitutionsProcessor(oldConfig, newConfig);

        Model model = Model.builder().addShape(service).build();

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("shapeSubstitutions")
            .hasMessageContaining("smithyShapeSubstitutions");
    }
}
