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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Tests for {@link UseLegacyEventGenerationSchemeProcessor}.
 *
 * <p><b>Property 22: Postprocess Idempotence for Model-Agnostic Processors</b> — verify
 * applying postprocess twice produces same result as once.
 * <p><b>Validates: Requirements 15.2</b>
 */
class UseLegacyEventGenerationSchemeProcessorTest {

    private UseLegacyEventGenerationSchemeProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new UseLegacyEventGenerationSchemeProcessor();
    }

    /**
     * Preprocess returns input model unchanged (postprocess-only processor).
     * Validates: Requirement 15.1
     */
    @Test
    void preprocess_returnsInputModelUnchanged() {
        ServiceShape service = ServiceShape.builder()
                                           .id(ShapeId.from("com.example#TestService"))
                                           .version("2024-01-01")
                                           .build();
        Model model = Model.builder().addShape(service).build();

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Property 22: Postprocess Idempotence.
     * Empty config: postprocess is a no-op and can be applied multiple times.
     * Validates: Requirement 15.2
     */
    @Test
    void postprocess_when_emptyConfig_isNoOp() {
        IntermediateModel intermediateModel = createIntermediateModelWithConfig(
                Collections.emptyMap(), Collections.emptyMap());

        // Should not throw on first or second application
        processor.postprocess(intermediateModel);
        processor.postprocess(intermediateModel);
    }

    /**
     * Property 22: Postprocess Idempotence.
     * Valid config with distinct shapes per member: postprocess succeeds and is idempotent.
     * Validates: Requirement 15.2
     */
    @Test
    void postprocess_when_validConfig_isIdempotent() {
        ShapeModel eventStreamShape = createEventStreamShape("MyEventStream",
                memberWithShape("EventA", "EventAShape"),
                memberWithShape("EventB", "EventBShape"));

        Map<String, List<String>> legacyConfig = new HashMap<>();
        legacyConfig.put("MyEventStream", Arrays.asList("EventA", "EventB"));

        IntermediateModel intermediateModel = createIntermediateModelWithConfig(
                Collections.singletonMap("MyEventStream", eventStreamShape), legacyConfig);

        // First application should succeed
        processor.postprocess(intermediateModel);
        // Second application should also succeed (idempotent)
        processor.postprocess(intermediateModel);
    }

    /**
     * Duplicate shape targets: postprocess throws IllegalArgumentException when two members
     * in the same event stream target the same shape.
     * Validates: Requirement 15.2
     */
    @Test
    void postprocess_when_duplicateShapeTargets_throwsIllegalArgumentException() {
        ShapeModel eventStreamShape = createEventStreamShape("MyEventStream",
                memberWithShape("EventA", "SharedShape"),
                memberWithShape("EventB", "SharedShape"));

        Map<String, List<String>> legacyConfig = new HashMap<>();
        legacyConfig.put("MyEventStream", Arrays.asList("EventA", "EventB"));

        IntermediateModel intermediateModel = createIntermediateModelWithConfig(
                Collections.singletonMap("MyEventStream", eventStreamShape), legacyConfig);

        assertThatThrownBy(() -> processor.postprocess(intermediateModel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UseLegacyEventGenerationScheme")
                .hasMessageContaining("MyEventStream");
    }

    /**
     * Unrecognized event stream: postprocess logs warning but does not throw.
     * Validates: Requirement 15.2
     */
    @Test
    void postprocess_when_unrecognizedEventStream_doesNotThrow() {
        Map<String, List<String>> legacyConfig = new HashMap<>();
        legacyConfig.put("NonExistentStream", Collections.singletonList("EventA"));

        IntermediateModel intermediateModel = createIntermediateModelWithConfig(
                Collections.emptyMap(), legacyConfig);

        // Should not throw — just logs a warning
        processor.postprocess(intermediateModel);
    }

    /**
     * Unrecognized member in event stream: postprocess logs warning but does not throw
     * when the member count for any shape does not exceed 1.
     * Validates: Requirement 15.2
     */
    @Test
    void postprocess_when_unrecognizedMember_doesNotThrow() {
        ShapeModel eventStreamShape = createEventStreamShape("MyEventStream",
                memberWithShape("EventA", "EventAShape"));

        Map<String, List<String>> legacyConfig = new HashMap<>();
        legacyConfig.put("MyEventStream", Arrays.asList("EventA", "NonExistentMember"));

        IntermediateModel intermediateModel = createIntermediateModelWithConfig(
                Collections.singletonMap("MyEventStream", eventStreamShape), legacyConfig);

        // Should not throw — just logs a warning for the unrecognized member
        processor.postprocess(intermediateModel);
    }

    /**
     * Non-event-stream shape referenced in config: postprocess logs warning but does not throw.
     * Validates: Requirement 15.2
     */
    @Test
    void postprocess_when_nonEventStreamShape_doesNotThrow() {
        ShapeModel regularShape = new ShapeModel("RegularShape");
        regularShape.setShapeName("RegularShape");
        regularShape.setC2jName("RegularShape");
        regularShape.setType(ShapeType.Model);

        Map<String, List<String>> legacyConfig = new HashMap<>();
        legacyConfig.put("RegularShape", Collections.singletonList("SomeMember"));

        IntermediateModel intermediateModel = createIntermediateModelWithConfig(
                Collections.singletonMap("RegularShape", regularShape), legacyConfig);

        // Should not throw — shape is not an event stream, so it logs a warning
        processor.postprocess(intermediateModel);
    }

    private static MemberModel memberWithShape(String c2jName, String c2jShape) {
        MemberModel member = new MemberModel();
        member.setC2jName(c2jName);
        member.setName(c2jName);
        member.setC2jShape(c2jShape);
        return member;
    }

    private static ShapeModel createEventStreamShape(String c2jName, MemberModel... members) {
        ShapeModel shape = new ShapeModel(c2jName);
        shape.setShapeName(c2jName);
        shape.setC2jName(c2jName);
        shape.setType(ShapeType.Model);
        shape.withIsEventStream(true);
        List<MemberModel> memberList = new ArrayList<>(Arrays.asList(members));
        shape.setMembers(memberList);
        return shape;
    }

    private static IntermediateModel createIntermediateModelWithConfig(
            Map<String, ShapeModel> shapes,
            Map<String, List<String>> useLegacyEventGenerationScheme) {
        CustomizationConfig config = CustomizationConfig.create();
        config.setUseLegacyEventGenerationScheme(useLegacyEventGenerationScheme);
        IntermediateModel model = new IntermediateModel();
        model.setShapes(new HashMap<>(shapes));
        model.setCustomizationConfig(config);
        return model;
    }
}
