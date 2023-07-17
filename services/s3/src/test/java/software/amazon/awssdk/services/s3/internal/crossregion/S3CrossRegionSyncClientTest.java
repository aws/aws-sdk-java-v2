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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClientTest.customHttpResponse;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClientTest.successHttpResponse;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectTestBase.CHANGED_CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectTestBase.CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3DecoratorRedirectTestBase.OVERRIDE_CONFIGURED_REGION;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

class S3CrossRegionSyncClientTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String TOKEN = "token";

    private  MockSyncHttpClient mockSyncHttpClient ;
    private CaptureInterceptor captureInterceptor;
    private S3Client defaultS3Client;

    @BeforeEach
    void before() {
        mockSyncHttpClient = new MockSyncHttpClient();
        captureInterceptor = new CaptureInterceptor();
        defaultS3Client = clientBuilder().build();
    }


    private static Stream<Arguments> stubResponses() {
        Consumer<MockSyncHttpClient> redirectStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION.id()), successHttpResponse());

        Consumer<MockSyncHttpClient> successStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());

        return Stream.of(
            Arguments.of(redirectStubConsumer, BucketEndpointProvider.class),
            Arguments.of(successStubConsumer, DefaultS3EndpointProvider.class)
        );
    }

    private static Stream<Arguments> stubOverriddenEndpointProviderResponses() {
        Consumer<MockSyncHttpClient> redirectStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION.id()), successHttpResponse());

        Consumer<MockSyncHttpClient> successStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());

        return Stream.of(
            Arguments.of(redirectStubConsumer, BucketEndpointProvider.class, CROSS_REGION),
            Arguments.of(successStubConsumer, TestEndpointProvider.class, OVERRIDE_CONFIGURED_REGION)
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
        
        
        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest
    @MethodSource("stubOverriddenEndpointProviderResponses")
    void standardOp_crossRegionClient_takesCustomEndpointProviderInRequest(Consumer<MockSyncHttpClient> stubConsumer,
                                                                           Class<?> endpointProviderType,
                                                                           Region region) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(true)
                                                    .endpointProvider(new TestEndpointProvider())
                                                    .region(OVERRIDE_CONFIGURED_REGION)
                                                    .build();
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(o -> o.putHeader("someheader", "somevalue")
                                                                                .endpointProvider(new TestEndpointProvider()))
                                                   .build();
        crossRegionClient.getObject(request);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
        assertThat(mockSyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
        assertThat(mockSyncHttpClient.getLastRequest().encodedPath()).contains("test_prefix_");
        assertThat(mockSyncHttpClient.getLastRequest().host()).contains(region.id());
    }

    @ParameterizedTest
    @MethodSource("stubOverriddenEndpointProviderResponses")
    void standardOp_crossRegionClient_takesCustomEndpointProviderInClient(Consumer<MockSyncHttpClient> stubConsumer,
                                                                          Class<?> endpointProviderType,
                                                                          Region region) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(true)
                                                    .endpointProvider(new TestEndpointProvider())
                                                    .region(OVERRIDE_CONFIGURED_REGION)
                                                    .build();
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket(BUCKET)
                                                   .key(KEY)
                                                   .overrideConfiguration(o -> o.putHeader("someheader", "somevalue"))
                                                   .build();
        crossRegionClient.getObject(request);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
        assertThat(mockSyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
        assertThat(mockSyncHttpClient.getLastRequest().encodedPath()).contains("test_prefix_");
        assertThat(mockSyncHttpClient.getLastRequest().host()).contains(region.id());
    }

    @Test
    void crossRegionClient_CallsHeadObject_when_regionNameNotPresentInFallBackCall() {
        mockSyncHttpClient.stubResponses(customHttpResponse(301,  null ),
                                         customHttpResponse(301,  CROSS_REGION.id() ),
                                         successHttpResponse(), successHttpResponse());
        S3Client crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

        List<SdkHttpRequest> requests = mockSyncHttpClient.getRequests();
        assertThat(requests).hasSize(3);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     OVERRIDE_CONFIGURED_REGION.toString(),
                                     CROSS_REGION.id()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET,
                                     SdkHttpMethod.HEAD,
                                     SdkHttpMethod.GET));

        // Resetting the mock client to capture the new API request for second S3 Call.
        mockSyncHttpClient.reset();
        mockSyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        List<SdkHttpRequest> postCacheRequests = mockSyncHttpClient.getRequests();

        assertThat(postCacheRequests.stream()
                                    .map(req -> req.host().substring(10,req.host().length() - 14 ))
                                    .collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(CROSS_REGION.id()));
        assertThat(postCacheRequests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

    }

    @Test
    void crossRegionClient_when_redirectsAfterCaching() {
        mockSyncHttpClient.stubResponses(customHttpResponse(301, CROSS_REGION.id()),
                                         successHttpResponse(),
                                         successHttpResponse(),
                                         customHttpResponse(301,  CHANGED_CROSS_REGION.id()),
                                         successHttpResponse());
        S3Client crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

        List<SdkHttpRequest> requests = mockSyncHttpClient.getRequests();
        assertThat(requests).hasSize(5);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     CROSS_REGION.toString(),
                                     CROSS_REGION.toString(),
                                     CROSS_REGION.toString(),
                                     CHANGED_CROSS_REGION.toString()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET,SdkHttpMethod.GET,SdkHttpMethod.GET,SdkHttpMethod.GET,SdkHttpMethod.GET));
    }

    @Test
    void crossRegionClient_when_redirectsAfterCaching_withFallBackRedirectWithNoRegion() {
        mockSyncHttpClient.stubResponses(customHttpResponse(301,  null ),
                                         customHttpResponse(301,  CROSS_REGION.id() ),
                                         successHttpResponse(),
                                         successHttpResponse(),
                                         customHttpResponse(301,  null),
                                         customHttpResponse(301,  CHANGED_CROSS_REGION.id()),
                                         successHttpResponse());
        S3Client crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

        List<SdkHttpRequest> requests = mockSyncHttpClient.getRequests();
        assertThat(requests).hasSize(7);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(
                OVERRIDE_CONFIGURED_REGION.toString(), OVERRIDE_CONFIGURED_REGION.toString(), CROSS_REGION.toString(),
                CROSS_REGION.toString(),
                CROSS_REGION.toString(), OVERRIDE_CONFIGURED_REGION.toString(),
                CHANGED_CROSS_REGION.toString()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET, SdkHttpMethod.HEAD, SdkHttpMethod.GET,
                                     SdkHttpMethod.GET,
                                     SdkHttpMethod.GET, SdkHttpMethod.HEAD, SdkHttpMethod.GET));
    }

    @Test
    void crossRegionClient_CallsHeadObjectErrors_shouldTerminateTheAPI() {
        mockSyncHttpClient.stubResponses(customHttpResponse(301,  null ),
                                         customHttpResponse(400,  null ),
                                         successHttpResponse(), successHttpResponse());
        S3Client crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();

        assertThatExceptionOfType(S3Exception.class)
            .isThrownBy(() -> crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY)))
            .withMessageContaining("Status Code: 400");

        List<SdkHttpRequest> requests = mockSyncHttpClient.getRequests();
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
        mockSyncHttpClient.stubResponses(customHttpResponse(301,  null ),
                                         customHttpResponse(301,  null ),
                                         successHttpResponse(), successHttpResponse());
        S3Client crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();

        assertThatExceptionOfType(S3Exception.class)
            .isThrownBy(() -> crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY)))
            .withMessageContaining("Status Code: 301");

        List<SdkHttpRequest> requests = mockSyncHttpClient.getRequests();
        assertThat(requests).hasSize(2);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     OVERRIDE_CONFIGURED_REGION.toString()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET,
                                     SdkHttpMethod.HEAD));
    }


    @Test
    void standardOp_crossRegionClient_containUserAgent() {
        mockSyncHttpClient.stubResponses(successHttpResponse());
        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(mockSyncHttpClient.getLastRequest().firstMatchingHeader("User-Agent").get()).contains("hll/cross-region");
    }

    @Test
    void standardOp_simpleClient_doesNotContainCrossRegionUserAgent() {
        mockSyncHttpClient.stubResponses(successHttpResponse());
        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(false).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(mockSyncHttpClient.getLastRequest().firstMatchingHeader("User-Agent").get())
            .doesNotContain("hll/cross-region");
    }

    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .httpClient(mockSyncHttpClient)
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
