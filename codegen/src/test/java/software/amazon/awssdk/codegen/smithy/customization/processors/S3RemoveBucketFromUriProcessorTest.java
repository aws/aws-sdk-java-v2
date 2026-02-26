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
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;

/**
 * Tests for {@link S3RemoveBucketFromUriProcessor}.
 *
 * <p><b>Property 18: S3 Bucket URI Removal</b> — verify {@code /{Bucket}} removed from all
 * operation URIs, empty URI becomes {@code "/"}, non-S3 model unchanged.
 * <p><b>Validates: Requirements 17.1, 17.2, 17.3</b>
 */
class S3RemoveBucketFromUriProcessorTest {

    private static final String NAMESPACE = "com.example.testservice";
    private static final ShapeId SERVICE_ID = ShapeId.from(NAMESPACE + "#TestService");

    private S3RemoveBucketFromUriProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new S3RemoveBucketFromUriProcessor();
    }

    /**
     * Non-S3 service (e.g., "DynamoDB") returns model unchanged (isSameAs).
     * Validates: Requirement 17.3
     */
    @Test
    void preprocess_when_nonS3Service_returnsModelUnchanged() {
        ServiceShape service = serviceWithSdkId("DynamoDB");
        ShapeId opId = ShapeId.from(NAMESPACE + "#GetItem");
        OperationShape operation = operationWithHttpUri(opId, "/{Bucket}/key");
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * S3 service with operation URI containing {@code /{Bucket}/key} → {@code /{Bucket}}
     * removed, URI becomes {@code /key}.
     * Validates: Requirement 17.1
     */
    @Test
    void preprocess_when_s3OperationWithBucketInUri_removesBucketSegment() {
        ServiceShape service = serviceWithSdkId("S3");
        ShapeId opId = ShapeId.from(NAMESPACE + "#GetObject");
        OperationShape operation = operationWithHttpUri(opId, "/{Bucket}/key");
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        OperationShape resultOp = result.expectShape(opId, OperationShape.class);
        String resultUri = resultOp.expectTrait(HttpTrait.class).getUri().toString();
        assertThat(resultUri).isEqualTo("/key");
    }

    /**
     * S3 service with operation URI that is just {@code /{Bucket}} → URI becomes {@code "/"}.
     * Validates: Requirement 17.2
     */
    @Test
    void preprocess_when_s3OperationWithOnlyBucket_uriBecomesSlash() {
        ServiceShape service = serviceWithSdkId("S3");
        ShapeId opId = ShapeId.from(NAMESPACE + "#ListBuckets");
        OperationShape operation = operationWithHttpUri(opId, "/{Bucket}");
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        OperationShape resultOp = result.expectShape(opId, OperationShape.class);
        String resultUri = resultOp.expectTrait(HttpTrait.class).getUri().toString();
        assertThat(resultUri).isEqualTo("/");
    }

    /**
     * S3 service with operation that has no {@code @http} trait → model unchanged for that operation.
     * Validates: Requirement 17.1
     */
    @Test
    void preprocess_when_s3OperationWithNoHttpTrait_operationUnchanged() {
        ServiceShape service = serviceWithSdkId("S3");
        ShapeId opId = ShapeId.from(NAMESPACE + "#NoHttpOp");
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
     * S3 service with operation URI not containing {@code /{Bucket}} → that operation's URI unchanged.
     * Validates: Requirement 17.1
     */
    @Test
    void preprocess_when_s3OperationWithoutBucketInUri_uriUnchanged() {
        ServiceShape service = serviceWithSdkId("S3");
        ShapeId opId = ShapeId.from(NAMESPACE + "#ListAllBuckets");
        OperationShape operation = operationWithHttpUri(opId, "/");
        Model model = Model.builder()
                           .addShape(service.toBuilder().addOperation(opId).build())
                           .addShape(operation)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        assertThat(result).isSameAs(model);
    }

    /**
     * S3 service with multiple operations, some with {@code /{Bucket}} and some without →
     * only affected ones modified.
     * Validates: Requirements 17.1, 17.2
     */
    @Test
    void preprocess_when_s3MultipleOperations_onlyAffectedOnesModified() {
        ServiceShape service = serviceWithSdkId("S3");
        ShapeId opWithBucket = ShapeId.from(NAMESPACE + "#GetObject");
        ShapeId opWithoutBucket = ShapeId.from(NAMESPACE + "#ListAllBuckets");
        ShapeId opBucketOnly = ShapeId.from(NAMESPACE + "#HeadBucket");

        OperationShape getObject = operationWithHttpUri(opWithBucket, "/{Bucket}/{Key}");
        OperationShape listBuckets = operationWithHttpUri(opWithoutBucket, "/");
        OperationShape headBucket = operationWithHttpUri(opBucketOnly, "/{Bucket}");

        Model model = Model.builder()
                           .addShape(service.toBuilder()
                                            .addOperation(opWithBucket)
                                            .addOperation(opWithoutBucket)
                                            .addOperation(opBucketOnly)
                                            .build())
                           .addShape(getObject)
                           .addShape(listBuckets)
                           .addShape(headBucket)
                           .build();
        ServiceShape resolvedService = model.expectShape(SERVICE_ID, ServiceShape.class);

        Model result = processor.preprocess(model, resolvedService);

        // GetObject: /{Bucket}/{Key} → /{Key}
        String getObjectUri = result.expectShape(opWithBucket, OperationShape.class)
                                    .expectTrait(HttpTrait.class).getUri().toString();
        assertThat(getObjectUri).isEqualTo("/{Key}");

        // ListAllBuckets: / → / (unchanged)
        String listBucketsUri = result.expectShape(opWithoutBucket, OperationShape.class)
                                      .expectTrait(HttpTrait.class).getUri().toString();
        assertThat(listBucketsUri).isEqualTo("/");

        // HeadBucket: /{Bucket} → /
        String headBucketUri = result.expectShape(opBucketOnly, OperationShape.class)
                                     .expectTrait(HttpTrait.class).getUri().toString();
        assertThat(headBucketUri).isEqualTo("/");
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

    private static OperationShape operationWithHttpUri(ShapeId opId, String uri) {
        return OperationShape.builder()
                             .id(opId)
                             .addTrait(HttpTrait.builder()
                                               .method("GET")
                                               .uri(UriPattern.parse(uri))
                                               .code(200)
                                               .build())
                             .build();
    }
}
