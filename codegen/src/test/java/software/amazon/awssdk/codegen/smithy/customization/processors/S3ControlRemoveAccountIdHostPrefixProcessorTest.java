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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;

/**
 * Tests for {@link S3ControlRemoveAccountIdHostPrefixProcessor}.
 *
 * <p><b>Validates: Requirement 18.1</b> — AccountId host prefix removed for S3 Control operations.
 * <p><b>Validates: Requirement 18.2</b> — Non-S3-Control service returns model unchanged.
 */
class S3ControlRemoveAccountIdHostPrefixProcessorTest {

    private static final String NAMESPACE = "com.example.testservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestService");

    private S3ControlRemoveAccountIdHostPrefixProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new S3ControlRemoveAccountIdHostPrefixProcessor();
    }

    /**
     * Non-S3-Control service (e.g., "S3") returns model unchanged (isSameAs).
     * Validates: Requirement 18.2
     */
    @Test
    void preprocess_when_nonS3ControlService_returnsModelUnchanged() {
        ServiceShape service = serviceWithSdkId("S3");
        ShapeId opId = ShapeId.from(NAMESPACE + "#SomeOperation");
        OperationShape operation = operationWithEndpoint(opId, "{AccountId}.");
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * S3 Control service with operation having {@code {AccountId}.} host prefix →
     * endpoint trait removed entirely.
     * Validates: Requirement 18.1
     */
    @Test
    void preprocess_when_s3ControlOperationWithAccountIdPrefix_removesEndpointTrait() {
        ServiceShape service = serviceWithSdkId("S3 Control");
        ShapeId opId = ShapeId.from(NAMESPACE + "#GetAccessPoint");
        OperationShape operation = operationWithEndpoint(opId, "{AccountId}.");
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        OperationShape resultOp = result.expectShape(opId, OperationShape.class);
        assertThat(resultOp.getTrait(EndpointTrait.class)).isNotPresent();
    }

    /**
     * S3 Control service with operation having a different host prefix (e.g., {@code {BucketName}.})
     * → endpoint trait preserved.
     * Validates: Requirement 18.1
     */
    @Test
    void preprocess_when_s3ControlOperationWithDifferentPrefix_preservesEndpointTrait() {
        ServiceShape service = serviceWithSdkId("S3 Control");
        ShapeId opId = ShapeId.from(NAMESPACE + "#GetBucket");
        OperationShape operation = operationWithEndpoint(opId, "{BucketName}.");
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        OperationShape resultOp = result.expectShape(opId, OperationShape.class);
        assertThat(resultOp.getTrait(EndpointTrait.class)).isPresent();
        assertThat(resultOp.getTrait(EndpointTrait.class).get().getHostPrefix().toString())
            .isEqualTo("{BucketName}.");
    }

    /**
     * S3 Control service with operation having no {@code @endpoint} trait → operation unchanged.
     * Validates: Requirement 18.1
     */
    @Test
    void preprocess_when_s3ControlOperationWithNoEndpointTrait_operationUnchanged() {
        ServiceShape service = serviceWithSdkId("S3 Control");
        ShapeId opId = ShapeId.from(NAMESPACE + "#ListAccessPoints");
        OperationShape operation = OperationShape.builder().id(opId).build();
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * S3 Control service with multiple operations — some with {@code {AccountId}.} and some without →
     * only affected ones have endpoint trait removed.
     * Validates: Requirement 18.1
     */
    @Test
    void preprocess_when_s3ControlMultipleOperations_onlyAffectedOnesModified() {
        ServiceShape service = serviceWithSdkId("S3 Control");
        ShapeId opWithAccountId = ShapeId.from(NAMESPACE + "#GetAccessPoint");
        ShapeId opWithDifferentPrefix = ShapeId.from(NAMESPACE + "#GetBucket");
        ShapeId opWithNoEndpoint = ShapeId.from(NAMESPACE + "#ListAccessPoints");

        OperationShape getAccessPoint = operationWithEndpoint(opWithAccountId, "{AccountId}.");
        OperationShape getBucket = operationWithEndpoint(opWithDifferentPrefix, "{BucketName}.");
        OperationShape listAccessPoints = OperationShape.builder().id(opWithNoEndpoint).build();

        Model model = Model.builder()
                           .addShape(service.toBuilder()
                                            .addOperation(opWithAccountId)
                                            .addOperation(opWithDifferentPrefix)
                                            .addOperation(opWithNoEndpoint)
                                            .build())
                           .addShape(getAccessPoint)
                           .addShape(getBucket)
                           .addShape(listAccessPoints)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        // GetAccessPoint: {AccountId}. prefix → endpoint trait removed
        OperationShape resultGetAccessPoint = result.expectShape(opWithAccountId, OperationShape.class);
        assertThat(resultGetAccessPoint.getTrait(EndpointTrait.class)).isNotPresent();

        // GetBucket: {BucketName}. prefix → endpoint trait preserved
        OperationShape resultGetBucket = result.expectShape(opWithDifferentPrefix, OperationShape.class);
        assertThat(resultGetBucket.getTrait(EndpointTrait.class)).isPresent();
        assertThat(resultGetBucket.getTrait(EndpointTrait.class).get().getHostPrefix().toString())
            .isEqualTo("{BucketName}.");

        // ListAccessPoints: no endpoint trait → unchanged
        OperationShape resultListAccessPoints = result.expectShape(opWithNoEndpoint, OperationShape.class);
        assertThat(resultListAccessPoints.getTrait(EndpointTrait.class)).isNotPresent();
    }

    /**
     * Postprocess is a no-op.
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

    private static OperationShape operationWithEndpoint(ShapeId opId, String hostPrefix) {
        return OperationShape.builder()
                             .id(opId)
                             .addTrait(EndpointTrait.builder()
                                                   .hostPrefix(hostPrefix)
                                                   .build())
                             .build();
    }
}
