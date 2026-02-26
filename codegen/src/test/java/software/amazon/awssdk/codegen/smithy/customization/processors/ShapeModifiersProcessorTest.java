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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.ModifyModelShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.ShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.SmithyMemberDefinition;
import software.amazon.awssdk.codegen.model.config.customization.SmithyModifyShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.SmithyShapeModifier;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;

/**
 * Tests for {@link ShapeModifiersProcessor}.
 *
 * <p><b>Property 8: Shape Modifier Exclude Completeness</b> — verify excluded members removed, others preserved.
 * <p><b>Property 9: Shape Modifier Inject Completeness</b> — verify injected members added with correct name and target.
 * <p><b>Property 10: Shape Modifier Modify Correctness</b> — verify emitPropertyName renames member, emitAsType
 * retargets, deprecated applies trait.
 * <p><b>Property 11: Shape Modifier Wildcard Application</b> — verify {@code "*"} applies to all structure shapes.
 * <p><b>Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 30.1</b>
 */
class ShapeModifiersProcessorTest {

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
    // so that structure shapes are in the service closure (reachable by Walker).
    // -----------------------------------------------------------------------

    private Model buildModelWithOperation(StructureShape inputShape, StructureShape... extraShapes) {
        OperationShape operation = OperationShape.builder()
                                                 .id(ShapeId.from(NAMESPACE + "#TestOperation"))
                                                 .input(inputShape.getId())
                                                 .build();
        ServiceShape svc = service.toBuilder()
                                  .addOperation(operation.getId())
                                  .build();
        Model.Builder builder = Model.builder()
                                     .addShape(svc)
                                     .addShape(operation)
                                     .addShape(inputShape);
        for (StructureShape extra : extraShapes) {
            builder.addShape(extra);
        }
        return builder.build();
    }

    // -----------------------------------------------------------------------
    // Property 8: Shape Modifier Exclude Completeness
    // Validates: Requirement 8.1
    // -----------------------------------------------------------------------

    /**
     * Property 8: Shape Modifier Exclude Completeness.
     * Excluded members are removed, others preserved.
     * Validates: Requirement 8.1
     */
    @Test
    void preprocess_excludeMembers_removesSpecifiedMembersAndPreservesOthers() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("field1", ShapeId.from("smithy.api#String"))
            .addMember("field2", ShapeId.from("smithy.api#Integer"))
            .addMember("field3", ShapeId.from("smithy.api#Boolean"))
            .build();
        Model model = buildModelWithOperation(inputShape);

        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setExclude(Collections.singletonList("field2"));

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("TestInput", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        StructureShape resultShape = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultShape.getMember("field1")).isPresent();
        assertThat(resultShape.getMember("field2")).isNotPresent();
        assertThat(resultShape.getMember("field3")).isPresent();
    }

    // -----------------------------------------------------------------------
    // Property 9: Shape Modifier Inject Completeness
    // Validates: Requirement 8.2
    // -----------------------------------------------------------------------

    /**
     * Property 9: Shape Modifier Inject Completeness.
     * Injected members are added with correct name and target.
     * Validates: Requirement 8.2
     */
    @Test
    void preprocess_injectMembers_addsNewMembersWithCorrectTarget() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("existingField", ShapeId.from("smithy.api#String"))
            .build();
        Model model = buildModelWithOperation(inputShape);

        SmithyMemberDefinition injectedMember = new SmithyMemberDefinition();
        injectedMember.setName("newField");
        injectedMember.setTarget("smithy.api#Integer");

        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setInject(Collections.singletonList(injectedMember));

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("TestInput", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        StructureShape resultShape = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultShape.getMember("existingField")).isPresent();
        assertThat(resultShape.getMember("newField")).isPresent();
        assertThat(resultShape.getMember("newField").get().getTarget())
            .isEqualTo(ShapeId.from("smithy.api#Integer"));
    }

    // -----------------------------------------------------------------------
    // Property 10: Shape Modifier Modify Correctness
    // Validates: Requirements 8.3, 8.4, 8.5
    // -----------------------------------------------------------------------

    /**
     * Property 10: emitPropertyName renames member.
     * Validates: Requirement 8.3
     */
    @Test
    void preprocess_modifyEmitPropertyName_renamesMember() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("OldName", ShapeId.from("smithy.api#String"))
            .build();
        Model model = buildModelWithOperation(inputShape);

        SmithyModifyShapeModifier modifyModel = new SmithyModifyShapeModifier();
        modifyModel.setEmitPropertyName("NewName");

        Map<String, SmithyModifyShapeModifier> modMap = Collections.singletonMap("OldName", modifyModel);
        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setModify(Collections.singletonList(modMap));

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("TestInput", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        StructureShape resultShape = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultShape.getMember("OldName")).isNotPresent();
        assertThat(resultShape.getMember("NewName")).isPresent();
        assertThat(resultShape.getMember("NewName").get().getTarget())
            .isEqualTo(ShapeId.from("smithy.api#String"));
    }

    /**
     * Property 10: emitAsType retargets member to synthetic shape.
     * Validates: Requirement 8.4
     */
    @Test
    void preprocess_modifyEmitAsType_retargetsMemberToSyntheticShape() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("amount", ShapeId.from("smithy.api#String"))
            .build();
        Model model = buildModelWithOperation(inputShape);

        SmithyModifyShapeModifier modifyModel = new SmithyModifyShapeModifier();
        modifyModel.setEmitAsType("bigDecimal");

        Map<String, SmithyModifyShapeModifier> modMap = Collections.singletonMap("amount", modifyModel);
        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setModify(Collections.singletonList(modMap));

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("TestInput", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        StructureShape resultShape = result.expectShape(inputShape.getId(), StructureShape.class);
        MemberShape resultMember = resultShape.getMember("amount").get();
        ShapeId expectedTarget = ShapeId.from(NAMESPACE + "#SDK_bigDecimal");
        assertThat(resultMember.getTarget()).isEqualTo(expectedTarget);
        assertThat(result.getShape(expectedTarget)).isPresent();
    }

    /**
     * Property 10: deprecated applies @deprecated trait to member.
     * Validates: Requirement 8.5
     */
    @Test
    void preprocess_modifyDeprecated_appliesDeprecatedTrait() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("oldField", ShapeId.from("smithy.api#String"))
            .build();
        Model model = buildModelWithOperation(inputShape);

        SmithyModifyShapeModifier modifyModel = new SmithyModifyShapeModifier();
        modifyModel.setDeprecated(true);
        modifyModel.setDeprecatedMessage("Use newField instead");

        Map<String, SmithyModifyShapeModifier> modMap = Collections.singletonMap("oldField", modifyModel);
        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setModify(Collections.singletonList(modMap));

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("TestInput", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        StructureShape resultShape = result.expectShape(inputShape.getId(), StructureShape.class);
        MemberShape resultMember = resultShape.getMember("oldField").get();
        assertThat(resultMember.hasTrait(DeprecatedTrait.class)).isTrue();
        assertThat(resultMember.expectTrait(DeprecatedTrait.class).getMessage())
            .hasValue("Use newField instead");
    }

    // -----------------------------------------------------------------------
    // Property 11: Shape Modifier Wildcard Application
    // Validates: Requirement 8.6
    // -----------------------------------------------------------------------

    /**
     * Property 11: Shape Modifier Wildcard Application.
     * Wildcard {@code "*"} applies modifications to all structure shapes in the service closure.
     * Validates: Requirement 8.6
     */
    @Test
    void preprocess_wildcardKey_appliesModificationToAllStructureShapes() {
        StructureShape shape1 = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#Shape1"))
            .addMember("removeMe", ShapeId.from("smithy.api#String"))
            .addMember("keepMe", ShapeId.from("smithy.api#Integer"))
            .build();
        StructureShape shape2 = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#Shape2"))
            .addMember("removeMe", ShapeId.from("smithy.api#String"))
            .addMember("keepMe", ShapeId.from("smithy.api#Boolean"))
            .build();

        // Wire both shapes into the service closure via two operations
        OperationShape op1 = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#Op1"))
            .input(shape1.getId())
            .build();
        OperationShape op2 = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#Op2"))
            .input(shape2.getId())
            .build();
        ServiceShape svc = service.toBuilder()
            .addOperation(op1.getId())
            .addOperation(op2.getId())
            .build();
        Model model = Model.builder()
            .addShape(svc)
            .addShape(op1)
            .addShape(op2)
            .addShape(shape1)
            .addShape(shape2)
            .build();

        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setExclude(Collections.singletonList("removeMe"));

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("*", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        Model result = processor.preprocess(model, svc);

        StructureShape resultShape1 = result.expectShape(shape1.getId(), StructureShape.class);
        assertThat(resultShape1.getMember("removeMe")).isNotPresent();
        assertThat(resultShape1.getMember("keepMe")).isPresent();

        StructureShape resultShape2 = result.expectShape(shape2.getId(), StructureShape.class);
        assertThat(resultShape2.getMember("removeMe")).isNotPresent();
        assertThat(resultShape2.getMember("keepMe")).isPresent();
    }

    // -----------------------------------------------------------------------
    // excludeShape removes entire shape
    // Validates: Requirement 8.7
    // -----------------------------------------------------------------------

    /**
     * excludeShape=true removes the entire shape from the model.
     * Validates: Requirement 8.7
     */
    @Test
    void preprocess_excludeShape_removesEntireShapeFromModel() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#ToBeRemoved"))
            .addMember("field1", ShapeId.from("smithy.api#String"))
            .build();
        Model model = buildModelWithOperation(inputShape);

        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setExcludeShape(true);

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("ToBeRemoved", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        assertThat(result.getShape(ShapeId.from(NAMESPACE + "#ToBeRemoved"))).isNotPresent();
    }

    // -----------------------------------------------------------------------
    // Missing shape throws IllegalStateException
    // Validates: Requirement 8.8
    // -----------------------------------------------------------------------

    /**
     * Referencing a non-existent shape (non-wildcard) throws IllegalStateException.
     * Validates: Requirement 8.8
     */
    @Test
    void preprocess_missingShape_throwsIllegalStateException() {
        Model model = Model.builder().addShape(service).build();

        SmithyShapeModifier modifier = new SmithyShapeModifier();
        modifier.setExclude(Collections.singletonList("someField"));

        Map<String, SmithyShapeModifier> config = Collections.singletonMap("NonExistentShape", modifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, config);

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NonExistentShape")
            .hasMessageContaining(NAMESPACE);
    }

    // -----------------------------------------------------------------------
    // Null/empty config returns model unchanged
    // Validates: Requirement 8.9
    // -----------------------------------------------------------------------

    /**
     * Both configs null → returns model unchanged.
     * Validates: Requirement 8.9
     */
    @Test
    void preprocess_when_bothConfigsNull_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(null, null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty new config map → returns model unchanged.
     * Validates: Requirement 8.9
     */
    @Test
    void preprocess_when_emptyNewConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        ShapeModifiersProcessor processor =
            new ShapeModifiersProcessor(null, Collections.emptyMap());

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty old config map → returns model unchanged.
     * Validates: Requirement 8.9
     */
    @Test
    void preprocess_when_emptyOldConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        ShapeModifiersProcessor processor =
            new ShapeModifiersProcessor(Collections.emptyMap(), null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    // -----------------------------------------------------------------------
    // Old-to-new conversion drops C2J-only fields
    // Validates: Requirement 30.1
    // -----------------------------------------------------------------------

    /**
     * Old config conversion: ModifyModelShapeModifier with C2J-only fields
     * (marshallLocationName, unmarshallLocationName, ignoreDataTypeConversionFailures)
     * are dropped during conversion. Smithy-applicable fields are preserved.
     * Validates: Requirement 30.1
     */
    @Test
    void preprocess_oldConfig_dropsC2jOnlyFieldsDuringConversion() {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("myField", ShapeId.from("smithy.api#String"))
            .build();
        Model model = buildModelWithOperation(inputShape);

        // Build old C2J config with C2J-only fields set
        ModifyModelShapeModifier oldModify = new ModifyModelShapeModifier();
        oldModify.setDeprecated(true);
        oldModify.setDeprecatedMessage("old field");
        oldModify.setMarshallLocationName("ShouldBeDropped");
        oldModify.setUnmarshallLocationName("AlsoDropped");
        oldModify.setIgnoreDataTypeConversionFailures(true);

        Map<String, ModifyModelShapeModifier> modMap = Collections.singletonMap("myField", oldModify);
        ShapeModifier oldShapeModifier = new ShapeModifier();
        oldShapeModifier.setModify(Collections.singletonList(modMap));

        Map<String, ShapeModifier> oldConfig = Collections.singletonMap("TestInput", oldShapeModifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(oldConfig, null);

        // The conversion should succeed and apply the deprecated trait
        // (C2J-only fields are silently dropped, not causing errors)
        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        StructureShape resultShape = result.expectShape(inputShape.getId(), StructureShape.class);
        MemberShape resultMember = resultShape.getMember("myField").get();
        assertThat(resultMember.hasTrait(DeprecatedTrait.class)).isTrue();
        assertThat(resultMember.expectTrait(DeprecatedTrait.class).getMessage())
            .hasValue("old field");
    }

    /**
     * Old config conversion: C2J Member inject is converted to SmithyMemberDefinition.
     * Validates: Requirement 30.1
     */
    @Test
    void preprocess_oldConfig_convertsInjectMemberToSmithyMemberDefinition() {
        StringShape targetShape = StringShape.builder()
            .id(ShapeId.from(NAMESPACE + "#MyString"))
            .build();
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestInput"))
            .addMember("existingField", ShapeId.from("smithy.api#String"))
            .build();

        OperationShape operation = OperationShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperation"))
            .input(inputShape.getId())
            .build();
        ServiceShape svc = service.toBuilder()
            .addOperation(operation.getId())
            .build();
        Model model = Model.builder()
            .addShape(svc)
            .addShape(operation)
            .addShape(inputShape)
            .addShape(targetShape)
            .build();

        // Build old C2J inject config
        Member c2jMember = new Member();
        c2jMember.setShape("MyString");

        Map<String, Member> injectMap = Collections.singletonMap("injectedField", c2jMember);
        ShapeModifier oldShapeModifier = new ShapeModifier();
        oldShapeModifier.setInject(Collections.singletonList(injectMap));

        Map<String, ShapeModifier> oldConfig = Collections.singletonMap("TestInput", oldShapeModifier);
        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(oldConfig, null);

        Model result = processor.preprocess(model, svc);

        StructureShape resultShape = result.expectShape(inputShape.getId(), StructureShape.class);
        assertThat(resultShape.getMember("injectedField")).isPresent();
        assertThat(resultShape.getMember("injectedField").get().getTarget())
            .isEqualTo(ShapeId.from(NAMESPACE + "#MyString"));
    }

    // -----------------------------------------------------------------------
    // Dual-config mutual exclusion (inherited from AbstractDualConfigProcessor)
    // -----------------------------------------------------------------------

    /**
     * Both old and new config set → throws IllegalStateException.
     */
    @Test
    void preprocess_when_bothConfigsSet_throwsIllegalStateException() {
        Map<String, ShapeModifier> oldConfig = new HashMap<>();
        oldConfig.put("SomeShape", new ShapeModifier());

        Map<String, SmithyShapeModifier> newConfig = new HashMap<>();
        newConfig.put("SomeShape", new SmithyShapeModifier());

        ShapeModifiersProcessor processor = new ShapeModifiersProcessor(oldConfig, newConfig);

        Model model = Model.builder().addShape(service).build();

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("shapeModifiers")
            .hasMessageContaining("smithyShapeModifiers");
    }
}
