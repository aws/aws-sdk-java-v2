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
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpPayloadTrait;

/**
 * Tests for {@link ExplicitStringPayloadQueryProtocolProcessor}.
 *
 * <p><b>Validates: Requirement 19.1</b> — relevant trait combination (httpPayload on string + query protocol) is handled.
 * <p><b>Validates: Requirement 19.2</b> — no relevant traits returns model unchanged.
 */
class ExplicitStringPayloadQueryProtocolProcessorTest {

    private static final String NAMESPACE = "com.example.testservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestService");
    private static final StringShape PRELUDE_STRING = StringShape.builder()
                                                                 .id(ShapeId.from("smithy.api#String"))
                                                                 .build();

    private ExplicitStringPayloadQueryProtocolProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ExplicitStringPayloadQueryProtocolProcessor();
    }

    /**
     * Non-query service (restJson1) returns model unchanged.
     * Validates: Requirement 19.2
     */
    @Test
    void preprocess_when_nonQueryService_returnsModelUnchanged() {
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addTrait(RestJson1Trait.builder().build())
                                           .build();
        Model model = Model.builder().addShape(service).build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * awsQuery service with operation having {@code @httpPayload} on string member in input
     * → throws RuntimeException.
     * Validates: Requirement 19.1
     */
    @Test
    void preprocess_when_awsQueryWithStringPayloadInInput_throwsRuntimeException() {
        ShapeId opId = ShapeId.from(NAMESPACE + "#TestOperation");
        ShapeId inputId = ShapeId.from(NAMESPACE + "#TestOperationInput");

        MemberShape payloadMember = MemberShape.builder()
                                               .id(inputId.withMember("body"))
                                               .target(ShapeId.from("smithy.api#String"))
                                               .addTrait(new HttpPayloadTrait())
                                               .build();
        StructureShape inputStruct = StructureShape.builder()
                                                   .id(inputId)
                                                   .addMember(payloadMember)
                                                   .build();
        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .input(inputId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addTrait(new AwsQueryTrait())
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation, inputStruct, PRELUDE_STRING)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        assertThatThrownBy(() -> processor.preprocess(model, resolvedService))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("TestOperation");
    }

    /**
     * ec2Query service with operation having {@code @httpPayload} on string member in output
     * → throws RuntimeException.
     * Validates: Requirement 19.1
     */
    @Test
    void preprocess_when_ec2QueryWithStringPayloadInOutput_throwsRuntimeException() {
        ShapeId opId = ShapeId.from(NAMESPACE + "#TestOperation");
        ShapeId outputId = ShapeId.from(NAMESPACE + "#TestOperationOutput");

        MemberShape payloadMember = MemberShape.builder()
                                               .id(outputId.withMember("body"))
                                               .target(ShapeId.from("smithy.api#String"))
                                               .addTrait(new HttpPayloadTrait())
                                               .build();
        StructureShape outputStruct = StructureShape.builder()
                                                    .id(outputId)
                                                    .addMember(payloadMember)
                                                    .build();
        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .output(outputId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addTrait(new Ec2QueryTrait())
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation, outputStruct, PRELUDE_STRING)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        assertThatThrownBy(() -> processor.preprocess(model, resolvedService))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("TestOperation");
    }

    /**
     * awsQuery service with operation having {@code @httpPayload} on non-string member (blob)
     * → returns model unchanged.
     * Validates: Requirement 19.2
     */
    @Test
    void preprocess_when_awsQueryWithBlobPayload_returnsModelUnchanged() {
        ShapeId opId = ShapeId.from(NAMESPACE + "#TestOperation");
        ShapeId inputId = ShapeId.from(NAMESPACE + "#TestOperationInput");
        ShapeId blobId = ShapeId.from(NAMESPACE + "#MyBlob");

        BlobShape blobShape = BlobShape.builder().id(blobId).build();
        MemberShape payloadMember = MemberShape.builder()
                                               .id(inputId.withMember("body"))
                                               .target(blobId)
                                               .addTrait(new HttpPayloadTrait())
                                               .build();
        StructureShape inputStruct = StructureShape.builder()
                                                   .id(inputId)
                                                   .addMember(payloadMember)
                                                   .build();
        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .input(inputId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addTrait(new AwsQueryTrait())
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation, inputStruct, blobShape)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * awsQuery service with operation having no {@code @httpPayload} members
     * → returns model unchanged.
     * Validates: Requirement 19.2
     */
    @Test
    void preprocess_when_awsQueryWithNoPayloadMembers_returnsModelUnchanged() {
        ShapeId opId = ShapeId.from(NAMESPACE + "#TestOperation");
        ShapeId inputId = ShapeId.from(NAMESPACE + "#TestOperationInput");

        MemberShape regularMember = MemberShape.builder()
                                               .id(inputId.withMember("name"))
                                               .target(ShapeId.from("smithy.api#String"))
                                               .build();
        StructureShape inputStruct = StructureShape.builder()
                                                   .id(inputId)
                                                   .addMember(regularMember)
                                                   .build();
        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .input(inputId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addTrait(new AwsQueryTrait())
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation, inputStruct)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * awsQuery service with operation having no input/output
     * → returns model unchanged.
     * Validates: Requirement 19.2
     */
    @Test
    void preprocess_when_awsQueryWithNoInputOutput_returnsModelUnchanged() {
        ShapeId opId = ShapeId.from(NAMESPACE + "#TestOperation");

        OperationShape operation = OperationShape.builder()
                                                 .id(opId)
                                                 .build();
        ServiceShape service = ServiceShape.builder()
                                           .id(SERVICE_ID)
                                           .version("2024-01-01")
                                           .addTrait(new AwsQueryTrait())
                                           .addOperation(opId)
                                           .build();
        Model model = Model.builder()
                           .addShapes(service, operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * Postprocess is a no-op.
     */
    @Test
    void postprocess_isNoOp() {
        processor.postprocess(null);
    }
}
