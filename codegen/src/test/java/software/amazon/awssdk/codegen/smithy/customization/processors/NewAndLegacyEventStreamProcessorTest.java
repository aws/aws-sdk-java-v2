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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Tests for {@link NewAndLegacyEventStreamProcessor}.
 *
 * <p><b>Validates: Requirements 16.1, 16.2, 31.5</b>
 */
class NewAndLegacyEventStreamProcessorTest {

    private static final String NAMESPACE = "com.example.testservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestService");

    private NewAndLegacyEventStreamProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new NewAndLegacyEventStreamProcessor();
    }

    /**
     * Service not in approved map (e.g., "DynamoDB") returns model unchanged.
     * Validates: Requirement 16.2
     */
    @Test
    void preprocess_when_serviceNotInApprovedMap_returnsModelUnchanged() {
        ServiceShape service = serviceWithSdkId("DynamoDB");
        Model model = Model.builder().addShape(service).build();

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Service without ServiceTrait returns model unchanged.
     * Validates: Requirement 16.2
     */
    @Test
    void preprocess_when_serviceHasNoServiceTrait_returnsModelUnchanged() {
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .build();
        Model model = Model.builder().addShape(service).build();

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Kinesis service with only approved event stream ("SubscribeToShardEventStream")
     * returns model unchanged.
     * Validates: Requirement 16.2
     */
    @Test
    void preprocess_when_kinesis_approvedEventStream_returnsModelUnchanged() {
        ServiceShape service = serviceWithSdkId("Kinesis");
        UnionShape approvedStream = streamingUnion("SubscribeToShardEventStream");
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(approvedStream)
                           .addShape(stringShape("EventPayload"))
                           .build();

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Kinesis service with unapproved event stream throws RuntimeException.
     * Validates: Requirements 16.1, 31.5
     */
    @Test
    void preprocess_when_kinesis_unapprovedEventStream_throwsRuntimeException() {
        ServiceShape service = serviceWithSdkId("Kinesis");
        UnionShape unapprovedStream = streamingUnion("SomeNewEventStream");
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(unapprovedStream)
                           .addShape(stringShape("EventPayload"))
                           .build();

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Kinesis")
            .hasMessageContaining("SomeNewEventStream");
    }

    /**
     * Transcribe Streaming with all approved streams returns model unchanged.
     * Validates: Requirement 16.2
     */
    @Test
    void preprocess_when_transcribeStreaming_allApprovedStreams_returnsModelUnchanged() {
        ServiceShape service = serviceWithSdkId("Transcribe Streaming");
        StringShape payload = stringShape("EventPayload");
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(streamingUnion("AudioStream"))
                           .addShape(streamingUnion("TranscriptResultStream"))
                           .addShape(streamingUnion("MedicalTranscriptResultStream"))
                           .addShape(streamingUnion("CallAnalyticsTranscriptResultStream"))
                           .addShape(streamingUnion("MedicalScribeInputStream"))
                           .addShape(streamingUnion("MedicalScribeResultStream"))
                           .addShape(payload)
                           .build();

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Transcribe Streaming with unapproved stream throws RuntimeException.
     * Validates: Requirements 16.1, 31.5
     */
    @Test
    void preprocess_when_transcribeStreaming_unapprovedStream_throwsRuntimeException() {
        ServiceShape service = serviceWithSdkId("Transcribe Streaming");
        UnionShape unapprovedStream = streamingUnion("BrandNewStream");
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(unapprovedStream)
                           .addShape(stringShape("EventPayload"))
                           .build();

        assertThatThrownBy(() -> processor.preprocess(model, service))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Transcribe Streaming")
            .hasMessageContaining("BrandNewStream");
    }

    /**
     * Service with union shape but no @streaming trait is not treated as event stream.
     * Validates: Requirement 16.2
     */
    @Test
    void preprocess_when_kinesis_unionWithoutStreamingTrait_returnsModelUnchanged() {
        ServiceShape service = serviceWithSdkId("Kinesis");
        // Union without @streaming trait — should not be flagged
        UnionShape nonStreamingUnion = UnionShape.builder()
                                                  .id(ShapeId.from(NAMESPACE + "#SomeUnion"))
                                                  .addMember("EventA", ShapeId.from("smithy.api#String"))
                                                  .build();
        Model model = Model.builder()
                           .addShape(service)
                           .addShape(nonStreamingUnion)
                           .build();

        Model result = processor.preprocess(model, service);

        assertThat(result).isSameAs(model);
    }

    /**
     * Postprocess is a no-op (can call with null).
     */
    @Test
    void postprocess_isNoOp() {
        processor.postprocess(null);
    }

    // --- Helper methods ---

    private static ServiceShape serviceWithSdkId(String sdkId) {
        String normalized = sdkId.toLowerCase().replace(" ", "");
        return ServiceShape.builder()
                           .id(SERVICE_ID)
                           .version("2024-01-01")
                           .addTrait(ServiceTrait.builder()
                                                 .sdkId(sdkId)
                                                 .arnNamespace(normalized)
                                                 .cloudFormationName(sdkId.replace(" ", ""))
                                                 .cloudTrailEventSource(normalized + ".amazonaws.com")
                                                 .build())
                           .build();
    }

    private static UnionShape streamingUnion(String name) {
        return UnionShape.builder()
                         .id(ShapeId.from(NAMESPACE + "#" + name))
                         .addMember("EventA", ShapeId.from("smithy.api#String"))
                         .addTrait(new StreamingTrait())
                         .build();
    }

    private static StringShape stringShape(String name) {
        return StringShape.builder()
                          .id(ShapeId.from(NAMESPACE + "#" + name))
                          .build();
    }
}
