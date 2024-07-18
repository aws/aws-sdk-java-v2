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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClientTest.customHttpResponseWithUnknownErrorCode;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionAsyncClientTest.successHttpResponse;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionRedirectTestBase.CHANGED_CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionRedirectTestBase.CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionRedirectTestBase.OVERRIDE_CONFIGURED_REGION;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
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
    void setUp() {
        mockSyncHttpClient = new MockSyncHttpClient();
        captureInterceptor = new CaptureInterceptor();
        defaultS3Client = clientBuilder().build();
    }


    private static Stream<Arguments> stubSuccessfulRedirectResponses() {
        Consumer<MockSyncHttpClient> redirectStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()), successHttpResponse());

        Consumer<MockSyncHttpClient> successStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());

        return Stream.of(
            Arguments.of(redirectStubConsumer,
                         BucketEndpointProvider.class,
                         "Redirect Error with region in x-amz-bucket-header"),
            Arguments.of(successStubConsumer,
                         BucketEndpointProvider.class,
                         "Success response" ));
    }

    public static Stream<Arguments> stubFailureResponses() {

        List<SdkHttpMethod> noregionOnHeadBucketHttpMethodListMethodList = Arrays.asList(SdkHttpMethod.GET, SdkHttpMethod.HEAD);
        List<SdkHttpMethod> regionOnHeadBucketHttpMethodList = Arrays.asList(SdkHttpMethod.GET, SdkHttpMethod.HEAD, SdkHttpMethod.GET);
        List<String> noRegionOnHeadBucket = Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                                          OVERRIDE_CONFIGURED_REGION.toString());

        List<String> regionOnHeadBucket = Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                                        OVERRIDE_CONFIGURED_REGION.toString(),
                                                        CROSS_REGION.id());

        Consumer<MockSyncHttpClient> redirectFailedWithNoRegionFailure = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, null),
                                             customHttpResponseWithUnknownErrorCode(301, null),
                                              successHttpResponse(), successHttpResponse());



        return Stream.of(
            Arguments.of(redirectFailedWithNoRegionFailure,
                         301,
                         2,
                         noRegionOnHeadBucket,
                         noregionOnHeadBucketHttpMethodListMethodList
            )
        );
    }

    private static Stream<Arguments> stubOverriddenEndpointProviderResponses() {
        Consumer<MockSyncHttpClient> redirectStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()), successHttpResponse());

        Consumer<MockSyncHttpClient> successStubConsumer = mockSyncHttpClient ->
            mockSyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());

        return Stream.of(
            Arguments.of(redirectStubConsumer, BucketEndpointProvider.class, CROSS_REGION,
                         "Redirect error with Region in x-amz-bucket-region header"),
            Arguments.of(successStubConsumer, BucketEndpointProvider.class, OVERRIDE_CONFIGURED_REGION,
                         "Success response.")
        );
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClientWithNoOverrideConfig_when_StandardOperationIsPerformed_then_SuccessfullyIntercepts(
        Consumer<MockSyncHttpClient> stubConsumer,
        Class<?> endpointProviderType,
        String testCaseName) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClientWithExistingOverrideConfig_when_StandardOperationIsPerformed_then_SuccessfullyIntercepts(
        Consumer<MockSyncHttpClient> stubConsumer,
        Class<?> endpointProviderType,
        String testCaseName) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        GetObjectRequest request = getObjectBuilder()
                                                   .build();
        crossRegionClient.getObject(request);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
        assertThat(mockSyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClient_when_PaginatedOperationIsPerformed_then_DoesNotIntercept(Consumer<MockSyncHttpClient> stubConsumer,
                                                        Class<?> endpointProviderType,
                                                        String testCaseName)  {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = new S3CrossRegionSyncClient(defaultS3Client);
        ListObjectsV2Iterable iterable =
            crossRegionClient.listObjectsV2Paginator(r -> r.bucket(BUCKET).continuationToken(TOKEN).build());
        iterable.forEach(ListObjectsV2Response::contents);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClientCreatedWithWrapping_when_OperationIsPerformed_then_SuccessfullyIntercepts(Consumer<MockSyncHttpClient> stubConsumer,
                                                                      Class<?> endpointProviderType,
                                                                      String testCaseName) {
        stubConsumer.accept(mockSyncHttpClient);


        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest(name = "{index} - {3}.")
    @MethodSource("stubOverriddenEndpointProviderResponses")
    void given_CrossRegionClientWithCustomEndpointProvider_when_StandardOperationIsPerformed_then_UsesCustomEndpoint(Consumer<MockSyncHttpClient> stubConsumer,
                                                                           Class<?> endpointProviderType,
                                                                           Region region,
                                                                           String testCaseName) {
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

    @ParameterizedTest(name = "{index} - {3}.")
    @MethodSource("stubOverriddenEndpointProviderResponses")
    void given_CrossRegionClientWithCustomEndpointProvider_when_StandardOperationIsPerformed_then_UsesCustomEndpointInClient(Consumer<MockSyncHttpClient> stubConsumer,
                                                                          Class<?> endpointProviderType,
                                                                          Region region,
                                                                          String testCaseName) {
        stubConsumer.accept(mockSyncHttpClient);
        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(true)
                                                    .endpointProvider(new TestEndpointProvider())
                                                    .region(OVERRIDE_CONFIGURED_REGION)
                                                    .build();
        GetObjectRequest request = getObjectBuilder()
                                                   .build();
        crossRegionClient.getObject(request);
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
        assertThat(mockSyncHttpClient.getLastRequest().headers().get("someheader")).isNotNull();
        assertThat(mockSyncHttpClient.getLastRequest().encodedPath()).contains("test_prefix_");
        assertThat(mockSyncHttpClient.getLastRequest().host()).contains(region.id());
    }
    @Test
    void given_US_EAST_1_Client_resolvesToGlobalEndpoints_when_crossRegion_is_False(){
        mockSyncHttpClient.stubResponses(successHttpResponse());
        S3Client s3Client = clientBuilder().region(Region.US_EAST_1).build();
        s3Client.getObject(getObjectBuilder().build());
        assertThat(mockSyncHttpClient.getLastRequest().host()).isEqualTo("bucket.s3.amazonaws.com");
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(DefaultS3EndpointProvider.class);
    }

    @Test
    void given_US_EAST_1_Client_resolveToRegionalEndpoints_when_crossRegion_is_True(){
        mockSyncHttpClient.stubResponses(successHttpResponse());
        S3Client s3Client = clientBuilder().crossRegionAccessEnabled(true).region(Region.US_EAST_1).build();
        s3Client.getObject(getObjectBuilder().build());
        assertThat(mockSyncHttpClient.getLastRequest().host()).isEqualTo("bucket.s3.us-east-1.amazonaws.com");
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"us-east-1", "us-east-2", "us-west-1"})
    void given_AnyRegion_Client_Updates_the_useGlobalEndpointFlag_asFalse(String region) {
        mockSyncHttpClient.stubResponses(successHttpResponse());
        S3EndpointProvider mockEndpointProvider = Mockito.mock(S3EndpointProvider.class);

        when(mockEndpointProvider.resolveEndpoint(ArgumentMatchers.any(S3EndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder().url(URI.create("https://bucket.s3.amazonaws.com")).build()));

        S3Client s3Client = clientBuilder().crossRegionAccessEnabled(true)
                                           .region(Region.of(region))
                                           .endpointProvider(mockEndpointProvider).build();
        s3Client.getObject(getObjectBuilder().build());
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
        ArgumentCaptor<S3EndpointParams> collectionCaptor = ArgumentCaptor.forClass(S3EndpointParams.class);
        verify(mockEndpointProvider,  atLeastOnce()).resolveEndpoint(collectionCaptor.capture());
        collectionCaptor.getAllValues().forEach(resolvedParams ->{
            assertThat(resolvedParams.region()).isEqualTo(Region.of(region));
            assertThat(resolvedParams.useGlobalEndpoint()).isFalse();
        });
    }

    @Test
    void given_globalRegion_Client_Updates_region_to_useast1_and_useGlobalEndpointFlag_as_False() {
        String region = Region.AWS_GLOBAL.id();
        mockSyncHttpClient.stubResponses(successHttpResponse());
        S3EndpointProvider mockEndpointProvider = Mockito.mock(S3EndpointProvider.class);

        when(mockEndpointProvider.resolveEndpoint(ArgumentMatchers.any(S3EndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder().url(URI.create("https://bucket.s3.amazonaws.com")).build()));

        S3Client s3Client = clientBuilder().crossRegionAccessEnabled(true)
                                           .region(Region.of(region))
                                           .endpointProvider(mockEndpointProvider).build();
        s3Client.getObject(getObjectBuilder().build());
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
        ArgumentCaptor<S3EndpointParams> collectionCaptor = ArgumentCaptor.forClass(S3EndpointParams.class);
        verify(mockEndpointProvider,  atLeastOnce()).resolveEndpoint(collectionCaptor.capture());
        collectionCaptor.getAllValues().forEach(resolvedParams ->{
            assertThat(resolvedParams.region()).isEqualTo(Region.US_EAST_1);
            assertThat(resolvedParams.useGlobalEndpoint()).isFalse();
        });
    }

    private static GetObjectRequest.Builder getObjectBuilder() {
        return GetObjectRequest.builder()
                               .bucket(BUCKET)
                               .key(KEY)
                               .overrideConfiguration(o -> o.putHeader("someheader", "somevalue"));
    }

    @Test
    void given_CrossRegionClientWithFallbackCall_when_RegionNameNotPresent_then_CallsHeadObject() {
        mockSyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301,  null ),
                                         customHttpResponseWithUnknownErrorCode(301,  CROSS_REGION.id() ),
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
    void given_crossRegionClient_when_redirectError_then_redirectsAfterCaching() {
        mockSyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()),
                                         successHttpResponse(),
                                         successHttpResponse(),
                                         customHttpResponseWithUnknownErrorCode(301,  CHANGED_CROSS_REGION.id()),
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
    void given_CrossRegionClient_when_noRegionInHeader_thenFallBackToRegionInHeadBucket() {
        mockSyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301,  null ),
                                         customHttpResponseWithUnknownErrorCode(301,  CROSS_REGION.id() ),
                                         successHttpResponse(),
                                         successHttpResponse(),
                                         customHttpResponseWithUnknownErrorCode(301,  null),
                                         customHttpResponseWithUnknownErrorCode(301,  CHANGED_CROSS_REGION.id()),
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

    @ParameterizedTest(name = "{index} - Status code = {1} with HeadBucket bucket regions {3}.")
    @MethodSource("stubFailureResponses")
    void given_CrossRegionClient_when_CallsHeadBucketErrors_then_ShouldTerminateTheAPI(
        Consumer<MockSyncHttpClient> stubFailureResponses,
        Integer statusCode, Integer numberOfRequests,
        List<String> redirectedBucketRegions,
        List<SdkHttpMethod> sdkHttpMethods) {

        stubFailureResponses.accept(mockSyncHttpClient);
        S3Client crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();

        String description = String.format("Status Code: %d", statusCode);
        assertThatExceptionOfType(S3Exception.class)
            .isThrownBy(() -> crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY)))
            .withMessageContaining(description);

        List<SdkHttpRequest> requests = mockSyncHttpClient.getRequests();
        assertThat(requests).hasSize(numberOfRequests);

        assertThat(requests.stream().map(req -> req.host().substring(10,req.host().length() - 14 )).collect(Collectors.toList()))
            .isEqualTo(redirectedBucketRegions);

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(sdkHttpMethods);
    }

    @Test
    void given_CrossRegionClient_when_CallsHeadObjectWithNoRegion_then_ShouldTerminateHeadBucketAPI() {
        mockSyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301,  null ),
                                         customHttpResponseWithUnknownErrorCode(301,  null ),
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
    void given_CrossRegionClient_when_StandardOperation_then_ContainsUserAgent() {
        mockSyncHttpClient.stubResponses(successHttpResponse());
        S3Client crossRegionClient = clientBuilder().crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY));
        assertThat(mockSyncHttpClient.getLastRequest().firstMatchingHeader("User-Agent").get()).contains("hll/cross-region");
    }

    @Test
    void given_SimpleClient_when_StandardOperation_then_DoesNotContainCrossRegionUserAgent() {
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
