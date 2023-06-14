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

package software.amazon.awssdk.services.s3.internal.crossregion;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClientTest.customHttpResponse;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClientTest.successHttpResponse;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectBaseTest.CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectBaseTest.X_AMZ_BUCKET_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectBaseTest.X_AMZ_BUCKET_REGION;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

class S3CrossRegionSyncClientTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String TOKEN = "token";

    private final MockSyncHttpClient mockSyncHttpClient = new MockSyncHttpClient();
    private CaptureInterceptor captureInterceptor;
    private S3Client defaultS3Client;

    @BeforeEach
    void before() {
        captureInterceptor = new CaptureInterceptor();
        defaultS3Client = clientBuilder().build();
    }

    private static Stream<Arguments> stubResponses() {
        Consumer<MockSyncHttpClient> redirectStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION), successHttpResponse());

        Consumer<MockSyncHttpClient> successStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());

        return Stream.of(
            Arguments.of(redirectStubConsumer, BucketEndpointProvider.class),
            Arguments.of(successStubConsumer, DefaultS3EndpointProvider.class)
        );
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void standardOp_crossRegionClient_noOverrideConfig_SuccessfullyIntercepts(Consumer<MockSyncHttpClient> stubConsumer,
                                                                              Class<?> endpointProviderType) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void standardOp_crossRegionClient_existingOverrideConfig_SuccessfullyIntercepts(Consumer<MockSyncHttpClient> stubConsumer,
                                                                                    Class<?> endpointProviderType) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(o -> o.putHeader("someheader", "somevalue"))
                                                   .build();
        crossRegionClient.getObject(request);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
        assertThat(mockSyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void paginatedOp_crossRegionClient_DoesNotIntercept(Consumer<MockSyncHttpClient> stubConsumer,
                                                        Class<?> endpointProviderType)  {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        ListObjectsV2Iterable iterable =
            crossRegionClient.listObjectsV2Paginator(r -> r.bucket(BUCKET).continuationToken(TOKEN).build());
        iterable.forEach(ListObjectsV2Response::contents);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void crossRegionClient_createdWithWrapping_SuccessfullyIntercepts(Consumer<MockSyncHttpClient> stubConsumer,
                                                                      Class<?> endpointProviderType) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = clientBuilder().serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .httpClient(mockSyncHttpClient)
                       .endpointOverride(URI.create("http://localhost"))
                       .overrideConfiguration(c -> c.addExecutionInterceptor(captureInterceptor));
    }

    private static final class CaptureInterceptor implements ExecutionInterceptor {

        private EndpointProvider endpointProvider;

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
            endpointProvider = executionAttributes.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        }
    }
}
