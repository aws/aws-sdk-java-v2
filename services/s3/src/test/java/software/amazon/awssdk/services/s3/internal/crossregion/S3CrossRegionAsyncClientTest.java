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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectTestBase.CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectTestBase.OVERRIDE_CONFIGURED_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectTestBase.X_AMZ_BUCKET_REGION;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;

class S3CrossRegionAsyncClientTest {

    private static final String RESPONSE = "<Res>response</Res>";
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String TOKEN = "token";
    private final MockAsyncHttpClient mockAsyncHttpClient = new MockAsyncHttpClient();
    private CaptureInterceptor captureInterceptor;
    private S3AsyncClient s3Client;

    @BeforeEach
    void before() {
        captureInterceptor = new CaptureInterceptor();
        s3Client = clientBuilder().build();
    }

    public static Stream<Arguments> stubResponses() {
        Consumer<MockAsyncHttpClient> redirectStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION), successHttpResponse());

        Consumer<MockAsyncHttpClient> successStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());

        return Stream.of(
            Arguments.of(redirectStubConsumer, BucketEndpointProvider.class),
            Arguments.of(successStubConsumer, DefaultS3EndpointProvider.class)
        );
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void standardOp_crossRegionClient_noOverrideConfig_SuccessfullyIntercepts(Consumer<MockAsyncHttpClient> stubConsumer,
                                                                              Class<?> endpointProviderType) {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void standardOp_crossRegionClient_existingOverrideConfig_SuccessfullyIntercepts(Consumer<MockAsyncHttpClient> stubConsumer,
                                                                                    Class<?> endpointProviderType) {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(o -> o.putHeader("someheader", "somevalue"))
                                                   .build();
        crossRegionClient.getObject(request, AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
        assertThat(mockAsyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void paginatedOp_crossRegionClient_DoesIntercept(Consumer<MockAsyncHttpClient> stubConsumer,
                                                     Class<?> endpointProviderType) throws Exception {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        ListObjectsV2Publisher publisher =
            crossRegionClient.listObjectsV2Paginator(r -> r.bucket(BUCKET).continuationToken(TOKEN).build());
        CompletableFuture<Void> future = publisher.subscribe(ListObjectsV2Response::contents);
        future.get();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest
    @MethodSource("stubResponses")
    void crossRegionClient_createdWithWrapping_SuccessfullyIntercepts(Consumer<MockAsyncHttpClient> stubConsumer,
                                                                      Class<?> endpointProviderType) {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = clientBuilder().serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @Test
    void crossRegionClient_CallsHeadObject_when_regionNameNotPresentInFallBackCall() {
        mockAsyncHttpClient.stubResponses(customHttpResponse(301,  null ),
                                         customHttpResponse(301,  CROSS_REGION ),
                                         successHttpResponse(), successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(3);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     OVERRIDE_CONFIGURED_REGION.toString(),
                                     CROSS_REGION));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET,
                                     SdkHttpMethod.HEAD,
                                     SdkHttpMethod.GET));

        // Resetting the mock client to capture the new API request for second S3 Call.
        mockAsyncHttpClient.reset();
        mockAsyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        List<SdkHttpRequest> postCacheRequests = mockAsyncHttpClient.getRequests();

        assertThat(postCacheRequests.stream()
                                    .map(req -> req.host().substring(10,req.host().length() - 14 ))
                                    .collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(CROSS_REGION));
        assertThat(postCacheRequests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

    }

    @Test
    void crossRegionClient_CallsHeadObjectErrors_shouldTerminateTheAPI() {
        mockAsyncHttpClient.stubResponses(customHttpResponse(301,  null ),
                                         customHttpResponse(400,  null ),
                                         successHttpResponse(), successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION)
                           .serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();

        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(() -> crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join())
            .withMessageContaining("Endpoint resolution failed");

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(2);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     OVERRIDE_CONFIGURED_REGION.toString()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET,
                                     SdkHttpMethod.HEAD));
    }

    @Test
    void crossRegionClient_CallsHeadObjectWithNoRegion_shouldTerminateHeadBucketAPI() {
        mockAsyncHttpClient.stubResponses(customHttpResponse(301,  null ),
                                          customHttpResponse(301,  null ),
                                          successHttpResponse(), successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION)
                           .serviceConfiguration(c -> c.crossRegionAccessEnabled(true)).build();

        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(() -> crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join())
            .withMessageContaining("Endpoint resolution failed")
            .withCauseInstanceOf(SdkClientException.class).withRootCauseExactlyInstanceOf(S3Exception.class);

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(2);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     OVERRIDE_CONFIGURED_REGION.toString()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET,
                                     SdkHttpMethod.HEAD));
    }

    private S3AsyncClientBuilder clientBuilder() {
        return S3AsyncClient.builder()
                            .httpClient(mockAsyncHttpClient)
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


    public static HttpExecuteResponse successHttpResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(RESPONSE)))
                                  .build();
    }

    public static HttpExecuteResponse customHttpResponse(int statusCode, String bucket_region) {
        SdkHttpFullResponse.Builder httpResponseBuilder = SdkHttpResponse.builder();
        if (StringUtils.isNotBlank(bucket_region)) {
            httpResponseBuilder.appendHeader(X_AMZ_BUCKET_REGION, bucket_region);
        }
        return HttpExecuteResponse.builder()
                                  .response(httpResponseBuilder.statusCode(statusCode).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(RESPONSE)))
                                  .build();
    }
}
