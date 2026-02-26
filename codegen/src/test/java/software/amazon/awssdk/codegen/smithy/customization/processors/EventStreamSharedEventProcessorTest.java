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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Tests for {@link EventStreamSharedEventProcessor}.
 *
 * <p><b>Property 20: Event Stream Shared Event Duplication</b> — verify duplicated event shapes exist with new names.
 * <p><b>Validates: Requirements 13.1, 13.2, 13.3, 30.6</b>
 */
class EventStreamSharedEventProcessorTest {

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
    // Helper: build a model with the service, an operation whose output
    // references the event stream union, and the event shapes.
    // -----------------------------------------------------------------------

    private Model buildModelWithEventStream(UnionShape eventStreamUnion,
                                            StructureShape... eventShapes) {
        StructureShape inputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperationInput"))
            .build();
        StructureShape outputShape = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#TestOperationOutput"))
            .addMember("events", eventStreamUnion.getId())
            .build();
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
                                     .addShape(outputShape)
                                     .addShape(eventStreamUnion);
        for (StructureShape eventShape : eventShapes) {
            builder.addShape(eventShape);
        }
        return builder.build();
    }

    // -----------------------------------------------------------------------
    // Property 20: Event Stream Shared Event Duplication — single rename
    // Validates: Requirement 13.1
    // -----------------------------------------------------------------------

    /**
     * Property 20: Event Stream Shared Event Duplication.
     * When a duplication config specifies an event stream and an event rename,
     * the shared event shape is duplicated with the new name and the union
     * member is updated to target the new shape.
     * Validates: Requirement 13.1
     */
    @Test
    void preprocess_newConfig_duplicatesSharedEventWithNewName() {
        StructureShape sharedEvent = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SharedEvent"))
            .addMember("data", ShapeId.from("smithy.api#String"))
            .build();

        UnionShape eventStream = UnionShape.builder()
            .id(ShapeId.from(NAMESPACE + "#MyEventStream"))
            .addMember("SharedEvent", sharedEvent.getId())
            .addTrait(new StreamingTrait())
            .build();

        Model model = buildModelWithEventStream(eventStream, sharedEvent);

        // Config: rename SharedEvent → DuplicatedEvent in MyEventStream
        Map<String, String> eventRenames = Collections.singletonMap("SharedEvent", "DuplicatedEvent");
        Map<String, Map<String, String>> config = Collections.singletonMap(
            NAMESPACE + "#MyEventStream", eventRenames);

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Duplicated shape should exist with the new name
        ShapeId duplicatedId = ShapeId.from(NAMESPACE + "#DuplicatedEvent");
        assertThat(result.getShape(duplicatedId)).isPresent();

        // Duplicated shape should have the same members as the original
        StructureShape duplicated = result.expectShape(duplicatedId, StructureShape.class);
        assertThat(duplicated.getMember("data")).isPresent();
        assertThat(duplicated.getMember("data").get().getTarget())
            .isEqualTo(ShapeId.from("smithy.api#String"));

        // Union member should now target the duplicated shape
        UnionShape resultUnion = result.expectShape(
            ShapeId.from(NAMESPACE + "#MyEventStream"), UnionShape.class);
        assertThat(resultUnion.getMember("SharedEvent")).isPresent();
        assertThat(resultUnion.getMember("SharedEvent").get().getTarget()).isEqualTo(duplicatedId);
    }

    // -----------------------------------------------------------------------
    // Property 20: Multiple event renames in a single event stream
    // Validates: Requirement 13.1
    // -----------------------------------------------------------------------

    /**
     * Property 20: Multiple event renames in a single event stream.
     * When multiple events are renamed in the same event stream, each shared
     * event shape is duplicated with its respective new name.
     * Validates: Requirement 13.1
     */
    @Test
    void preprocess_newConfig_multipleEventRenamesInSingleEventStream() {
        StructureShape eventA = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#EventA"))
            .addMember("fieldA", ShapeId.from("smithy.api#String"))
            .build();
        StructureShape eventB = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#EventB"))
            .addMember("fieldB", ShapeId.from("smithy.api#Integer"))
            .build();

        UnionShape eventStream = UnionShape.builder()
            .id(ShapeId.from(NAMESPACE + "#MyEventStream"))
            .addMember("EventA", eventA.getId())
            .addMember("EventB", eventB.getId())
            .addTrait(new StreamingTrait())
            .build();

        Model model = buildModelWithEventStream(eventStream, eventA, eventB);

        Map<String, String> eventRenames = new HashMap<>();
        eventRenames.put("EventA", "CopiedEventA");
        eventRenames.put("EventB", "CopiedEventB");
        Map<String, Map<String, String>> config = Collections.singletonMap(
            NAMESPACE + "#MyEventStream", eventRenames);

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(null, config);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Both duplicated shapes should exist
        ShapeId copiedAId = ShapeId.from(NAMESPACE + "#CopiedEventA");
        ShapeId copiedBId = ShapeId.from(NAMESPACE + "#CopiedEventB");
        assertThat(result.getShape(copiedAId)).isPresent();
        assertThat(result.getShape(copiedBId)).isPresent();

        // Verify duplicated shapes have correct members
        StructureShape copiedA = result.expectShape(copiedAId, StructureShape.class);
        assertThat(copiedA.getMember("fieldA")).isPresent();

        StructureShape copiedB = result.expectShape(copiedBId, StructureShape.class);
        assertThat(copiedB.getMember("fieldB")).isPresent();

        // Union members should target the duplicated shapes
        UnionShape resultUnion = result.expectShape(
            ShapeId.from(NAMESPACE + "#MyEventStream"), UnionShape.class);
        assertThat(resultUnion.getMember("EventA").get().getTarget()).isEqualTo(copiedAId);
        assertThat(resultUnion.getMember("EventB").get().getTarget()).isEqualTo(copiedBId);
    }

    // -----------------------------------------------------------------------
    // Old-to-new conversion resolves simple names to ShapeIds
    // Validates: Requirements 13.2, 30.6
    // -----------------------------------------------------------------------

    /**
     * Old config conversion: simple event stream name keys are resolved to full ShapeIds.
     * Validates: Requirements 13.2, 30.6
     */
    @Test
    void preprocess_oldConfig_resolvesSimpleEventStreamNameToFullShapeId() {
        StructureShape sharedEvent = StructureShape.builder()
            .id(ShapeId.from(NAMESPACE + "#SharedEvent"))
            .addMember("data", ShapeId.from("smithy.api#String"))
            .build();

        UnionShape eventStream = UnionShape.builder()
            .id(ShapeId.from(NAMESPACE + "#MyEventStream"))
            .addMember("SharedEvent", sharedEvent.getId())
            .addTrait(new StreamingTrait())
            .build();

        Model model = buildModelWithEventStream(eventStream, sharedEvent);

        // Old config uses simple name "MyEventStream" as key
        Map<String, String> eventRenames = Collections.singletonMap("SharedEvent", "DuplicatedEvent");
        Map<String, Map<String, String>> oldConfig = Collections.singletonMap(
            "MyEventStream", eventRenames);

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(oldConfig, null);

        Model result = processor.preprocess(model, model.expectShape(SERVICE_ID, ServiceShape.class));

        // Duplicated shape should exist — proves the simple name was resolved to full ShapeId
        ShapeId duplicatedId = ShapeId.from(NAMESPACE + "#DuplicatedEvent");
        assertThat(result.getShape(duplicatedId)).isPresent();

        // Union member should target the duplicated shape
        UnionShape resultUnion = result.expectShape(
            ShapeId.from(NAMESPACE + "#MyEventStream"), UnionShape.class);
        assertThat(resultUnion.getMember("SharedEvent").get().getTarget()).isEqualTo(duplicatedId);
    }

    // -----------------------------------------------------------------------
    // Null/empty config returns model unchanged
    // Validates: Requirement 13.3
    // -----------------------------------------------------------------------

    /**
     * Both configs null → returns model unchanged.
     * Validates: Requirement 13.3
     */
    @Test
    void preprocess_when_bothConfigsNull_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(null, null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty new config map → returns model unchanged.
     * Validates: Requirement 13.3
     */
    @Test
    void preprocess_when_emptyNewConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(null, Collections.emptyMap());

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Empty old config map → returns model unchanged.
     * Validates: Requirement 13.3
     */
    @Test
    void preprocess_when_emptyOldConfig_returnsModelUnchanged() {
        Model model = Model.builder().addShape(service).build();
        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(Collections.emptyMap(), null);

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    // -----------------------------------------------------------------------
    // Dual-config mutual exclusion (inherited from AbstractDualConfigProcessor)
    // Validates: Requirement 13.4
    // -----------------------------------------------------------------------

    /**
     * Both old and new config set → throws IllegalStateException.
     * Validates: Requirement 13.4
     */
    @Test
    void preprocess_when_bothConfigsSet_throwsIllegalStateException() {
        Map<String, Map<String, String>> oldConfig = new HashMap<>();
        oldConfig.put("SomeStream", Collections.singletonMap("EventA", "CopiedEventA"));

        Map<String, Map<String, String>> newConfig = new HashMap<>();
        newConfig.put("com.example#SomeStream", Collections.singletonMap("EventA", "CopiedEventA"));

        EventStreamSharedEventProcessor processor =
            new EventStreamSharedEventProcessor(oldConfig, newConfig);

        Model model = Model.builder().addShape(service).build();

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("duplicateAndRenameSharedEvents")
            .hasMessageContaining("smithyDuplicateAndRenameSharedEvents");
    }
}
