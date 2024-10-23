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
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionRedirectTestBase.CHANGED_CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionRedirectTestBase.CROSS_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionRedirectTestBase.OVERRIDE_CONFIGURED_REGION;
import static software.amazon.awssdk.services.s3.internal.crossregion.S3CrossRegionRedirectTestBase.X_AMZ_BUCKET_REGION;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.DefaultS3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Response;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.StringUtils;

class S3CrossRegionAsyncClientTest {

    private static final String ILLEGAL_LOCATION_CONSTRAINT_EXCEPTION_ERROR_CODE = "IllegalLocationConstraintException";
    private static final String ERROR_RESPONSE_FORMAT = "<Error>\\n\\t<Code>%s</Code>\\n</Error>";
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String TOKEN = "token";
    private MockAsyncHttpClient mockAsyncHttpClient;
    private CaptureInterceptor captureInterceptor;
    private S3AsyncClient s3Client;

    @BeforeEach
    void setUp() {
        mockAsyncHttpClient = new MockAsyncHttpClient();
        captureInterceptor = new CaptureInterceptor();
        s3Client = clientBuilder().build();
    }

    private static Stream<Arguments> stubSuccessfulRedirectResponses() {
        Consumer<MockAsyncHttpClient> redirectStubConsumer = mockAsyncHttpClient ->
            mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()), successHttpResponse());

        Consumer<MockAsyncHttpClient> successStubConsumer = mockAsyncHttpClient ->
            mockAsyncHttpClient.stubResponses(successHttpResponse(), successHttpResponse());

        Consumer<MockAsyncHttpClient> tempRedirectStubConsumer = mockAsyncHttpClient ->
            mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(307, CROSS_REGION.id()), successHttpResponse());

        Consumer<MockAsyncHttpClient> locationConstraintExceptionConsumer = mockAsyncHttpClient ->
            mockAsyncHttpClient.stubResponses(customHttpResponse(400,
                                                                 ILLEGAL_LOCATION_CONSTRAINT_EXCEPTION_ERROR_CODE,
                                                                 CROSS_REGION.id()), successHttpResponse());


        return Stream.of(
            Arguments.of(redirectStubConsumer, BucketEndpointProvider.class, "Redirect Error with region in x-amz-bucket-header"),
            Arguments.of(successStubConsumer, BucketEndpointProvider.class, "Success response" ),
            Arguments.of(tempRedirectStubConsumer, BucketEndpointProvider.class,
                         "Permanent redirect Error with region in x-amz-bucket-header" ),
            Arguments.of(locationConstraintExceptionConsumer, BucketEndpointProvider.class,
                         "Redirect error: 400 IllegalLocationConstraintException with region in x-amz-bucket-header" )
        );
    }


    private static Stream<Arguments> stubFailureResponses() {

        List<SdkHttpMethod> noregionOnHeadBucketHttpMethodListMethodList = Arrays.asList(SdkHttpMethod.GET, SdkHttpMethod.HEAD);
        List<SdkHttpMethod> regionOnHeadBucketHttpMethodList = Arrays.asList(SdkHttpMethod.GET, SdkHttpMethod.HEAD, SdkHttpMethod.GET);
        List<String> noRegionOnHeadBucket = Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                                          OVERRIDE_CONFIGURED_REGION.toString());

        List<String> regionOnHeadBucket = Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                                        OVERRIDE_CONFIGURED_REGION.toString(),
                                                        CROSS_REGION.id());

        Consumer<MockAsyncHttpClient> redirectFailedWithNoRegionFailure = mockAsyncHttpClient ->
            mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, null),
                                              customHttpResponseWithUnknownErrorCode(301, null),
                                              successHttpResponse(), successHttpResponse());
        return Stream.of(
            Arguments.of(redirectFailedWithNoRegionFailure, 301, 2, noRegionOnHeadBucket, noregionOnHeadBucketHttpMethodListMethodList)
        );
    }

    public static HttpExecuteResponse successHttpResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .build())
                                  .build();
    }

    public static HttpExecuteResponse customHttpResponseWithUnknownErrorCode(int statusCode, String bucket_region) {
        return customHttpResponse(statusCode, "UnknownError", bucket_region);
    }

    public static HttpExecuteResponse customHttpResponse(int statusCode, String errorCode, String bucket_region) {
        SdkHttpFullResponse.Builder httpResponseBuilder = SdkHttpResponse.builder();
        if (StringUtils.isNotBlank(bucket_region)) {
            httpResponseBuilder.appendHeader(X_AMZ_BUCKET_REGION, bucket_region);
        }
        return HttpExecuteResponse.builder()
                                  .response(httpResponseBuilder.statusCode(statusCode).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(String.format(ERROR_RESPONSE_FORMAT, errorCode))))
                                  .build();
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClientWithNoOverrideConfig_when_StandardOperationIsPerformed_then_SuccessfullyIntercepts(Consumer<MockAsyncHttpClient> stubConsumer,
                                                                              Class<?> endpointProviderType,
                                                                              String testCase) {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClientWithExistingOverrideConfig_when_StandardOperationIsPerformed_then_SuccessfullyIntercepts(Consumer<MockAsyncHttpClient> stubConsumer,
                                                                                    Class<?> endpointProviderType,
                                                                                    String testCase) {
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

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClient_when_PaginatedOperationIsPerformed_then_DoesNotIntercept(Consumer<MockAsyncHttpClient> stubConsumer,
                                                     Class<?> endpointProviderType,
                                                     String testCase) throws Exception {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = new S3CrossRegionAsyncClient(s3Client);
        ListObjectsV2Publisher publisher =
            crossRegionClient.listObjectsV2Paginator(r -> r.bucket(BUCKET).continuationToken(TOKEN).build());
        CompletableFuture<Void> future = publisher.subscribe(ListObjectsV2Response::contents);
        future.get();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionClientCreatedWithWrapping_when_OperationIsPerformed_then_SuccessfullyIntercepts(Consumer<MockAsyncHttpClient> stubConsumer,
                                                                      Class<?> endpointProviderType,
                                                                      String testCase) {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = clientBuilder().crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @Test
    void given_CrossRegionClient_when_CallsHeadObjectWithRegionNameNotPresentInFallbackCall_then_RegionNameExtractedFromHeadBucket() {
        mockAsyncHttpClient.reset();
        mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, null),
                                          customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()),
                                          successHttpResponse(), successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(3);

        assertThat(requests.stream().map(req -> req.host().substring(10, req.host().length() - 14)).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.id(),
                                     OVERRIDE_CONFIGURED_REGION.id(),
                                     CROSS_REGION.id()));

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
                                    .map(req -> req.host().substring(10, req.host().length() - 14))
                                    .collect(Collectors.toList()))
            .isEqualTo(Collections.singletonList(CROSS_REGION.id()));
        assertThat(postCacheRequests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Collections.singletonList(SdkHttpMethod.GET));
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
    }

    @ParameterizedTest(name = "{index} - Status code = {1} with HeadBucket bucket regions {3}.")
    @MethodSource("stubFailureResponses")
    void given_CrossRegionClient_when_CallsHeadObjectErrors_then_ShouldTerminateTheAPI(
        Consumer<MockAsyncHttpClient> stubFailureResponses,
        Integer statusCode, Integer numberOfRequests,
        List<String> redirectedBuckets,
        List<SdkHttpMethod> sdkHttpMethods) {

        stubFailureResponses.accept(mockAsyncHttpClient);

        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION)
                           .crossRegionAccessEnabled(true).build();

        String errorMessage = String.format("software.amazon.awssdk.services.s3.model.S3Exception: "
                                            + "(Service: S3, Status Code: %d, Request ID: null)"
                , statusCode);
        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(() -> crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join())
            .withMessageContaining(errorMessage);

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(numberOfRequests);

        assertThat(requests.stream().map(req -> req.host().substring(10, req.host().length() - 14)).collect(Collectors.toList()))
            .isEqualTo(redirectedBuckets);

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(sdkHttpMethods);
    }

    @Test
    void given_CrossRegionClient_when_CallsHeadObjectWithNoRegion_then_ShouldTerminateHeadBucketAPI() {
        mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, null),
                                          customHttpResponseWithUnknownErrorCode(301, null),
                                          successHttpResponse(), successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION)
                           .crossRegionAccessEnabled(true).build();

        assertThatExceptionOfType(CompletionException.class)
            .isThrownBy(() -> crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join())
            .withMessageContaining("software.amazon.awssdk.services.s3.model.S3Exception: (Service: S3, Status Code: 301, "
                                   + "Request ID: null)")
            .withCauseInstanceOf(S3Exception.class).withRootCauseExactlyInstanceOf(S3Exception.class);

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(2);

        assertThat(requests.stream().map(req -> req.host().substring(10, req.host().length() - 14)).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     OVERRIDE_CONFIGURED_REGION.toString()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET,
                                     SdkHttpMethod.HEAD));
    }

    @Test
    void given_CrossRegionClient_when_FutureIsCancelled_then_ShouldCancelTheThread() {
        mockAsyncHttpClient.reset();
        mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, null),
                                          customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()),
                                          successHttpResponse(), successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> completableFuture =
            crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes());

        completableFuture.cancel(true);
        assertThat(completableFuture.isCancelled()).isTrue();
    }

    @Test
    void given_CrossRegionClient_when_RedirectsAfterCaching_then_ExpectedBehaviorOccurs() {
        mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()),
                                          successHttpResponse(),
                                          successHttpResponse(),
                                          customHttpResponseWithUnknownErrorCode(301, CHANGED_CROSS_REGION.id()),
                                          successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();

        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(5);

        assertThat(requests.stream().map(req -> req.host().substring(10, req.host().length() - 14)).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(OVERRIDE_CONFIGURED_REGION.toString(),
                                     CROSS_REGION.toString(),
                                     CROSS_REGION.toString(),
                                     CROSS_REGION.toString(),
                                     CHANGED_CROSS_REGION.toString()));

        assertThat(requests.stream().map(req -> req.method()).collect(Collectors.toList()))
            .isEqualTo(Arrays.asList(SdkHttpMethod.GET, SdkHttpMethod.GET, SdkHttpMethod.GET, SdkHttpMethod.GET,
                                     SdkHttpMethod.GET));
    }

    @Test
    void given_CrossRegionClient_when_RedirectsAfterCaching_withFallbackRedirectWithNoRegion_then_RetriedCallWithRegionSucceeds() {
        mockAsyncHttpClient.stubResponses(customHttpResponseWithUnknownErrorCode(301, null),
                                          customHttpResponseWithUnknownErrorCode(301, CROSS_REGION.id()),
                                          successHttpResponse(),
                                          successHttpResponse(),
                                          customHttpResponseWithUnknownErrorCode(301, null),
                                          customHttpResponseWithUnknownErrorCode(301, CHANGED_CROSS_REGION.id()),
                                          successHttpResponse());
        S3AsyncClient crossRegionClient =
            clientBuilder().endpointOverride(null).region(OVERRIDE_CONFIGURED_REGION).crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();

        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);

        List<SdkHttpRequest> requests = mockAsyncHttpClient.getRequests();
        assertThat(requests).hasSize(7);

        assertThat(requests.stream().map(req -> req.host().substring(10, req.host().length() - 14)).collect(Collectors.toList()))
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
    void given_CrossRegionClient_when_StandardOperation_then_ContainsUserAgent() {
        mockAsyncHttpClient.stubResponses(successHttpResponse());

        S3AsyncClient crossRegionClient = clientBuilder().crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(mockAsyncHttpClient.getLastRequest().firstMatchingHeader("User-Agent").get()).contains("hll/cross-region");
    }



    @ParameterizedTest(name = "{index} - {2}.")
    @MethodSource("stubSuccessfulRedirectResponses")
    void given_CrossRegionAccessEnabled_when_SuccessfulResponse_then_EndpointIsUpdated(Consumer<MockAsyncHttpClient> stubConsumer,
                                                                                       Class<?> endpointProviderType,
                                                                                       String testCase) {
        stubConsumer.accept(mockAsyncHttpClient);
        S3AsyncClient crossRegionClient = clientBuilder().crossRegionAccessEnabled(true).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(endpointProviderType);
    }

    @Test
    void given_SimpleClient_when_StandardOperation_then_DoesNotContainCrossRegionUserAgent() {
        mockAsyncHttpClient.stubResponses(successHttpResponse());
        S3AsyncClient crossRegionClient = clientBuilder().crossRegionAccessEnabled(false).build();
        crossRegionClient.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(mockAsyncHttpClient.getLastRequest().firstMatchingHeader("User-Agent").get()).doesNotContain("hll/cross");
    }

    @Test
    void given_US_EAST_1_Client_resolvesToGlobalEndpoints_when_crossRegion_is_False(){
        mockAsyncHttpClient.stubResponses(successHttpResponse());
        S3AsyncClient s3Client = clientBuilder().region(Region.US_EAST_1).build();
        s3Client.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(mockAsyncHttpClient.getLastRequest().host()).isEqualTo("bucket.s3.amazonaws.com");
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(DefaultS3EndpointProvider.class);
    }

    @Test
    void given_US_EAST_1_Client_resolveToRegionalEndpoints_when_crossRegion_is_True(){
        mockAsyncHttpClient.stubResponses(successHttpResponse());
        S3AsyncClient s3Client = clientBuilder().crossRegionAccessEnabled(true).region(Region.US_EAST_1).build();
        s3Client.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(mockAsyncHttpClient.getLastRequest().host()).isEqualTo("bucket.s3.us-east-1.amazonaws.com");
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"us-east-1", "us-east-2", "us-west-1"})
    void given_AnyRegion_Client_Updates_the_useGlobalEndpointFlag_asFalse(String region) {
        mockAsyncHttpClient.stubResponses(successHttpResponse());
        S3EndpointProvider mockEndpointProvider = Mockito.mock(S3EndpointProvider.class);

        when(mockEndpointProvider.resolveEndpoint(ArgumentMatchers.any(S3EndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder().url(URI.create("https://bucket.s3.amazonaws.com")).build()));

        S3AsyncClient s3Client = clientBuilder().crossRegionAccessEnabled(true)
                                                .region(Region.of(region))
                                                .endpointProvider(mockEndpointProvider).build();
        s3Client.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
        ArgumentCaptor<S3EndpointParams> collectionCaptor = ArgumentCaptor.forClass(S3EndpointParams.class);
        verify(mockEndpointProvider, atLeastOnce()).resolveEndpoint(collectionCaptor.capture());
        collectionCaptor.getAllValues().forEach(resolvedParams -> {
            assertThat(resolvedParams.region()).isEqualTo(Region.of(region));
            assertThat(resolvedParams.useGlobalEndpoint()).isFalse();
        });
    }

    @Test
    void given_globalRegion_Client_Updates_region_to_useast1_and_useGlobalEndpointFlag_as_False() {
        String region = Region.AWS_GLOBAL.id();
        mockAsyncHttpClient.stubResponses(successHttpResponse());
        S3EndpointProvider mockEndpointProvider = Mockito.mock(S3EndpointProvider.class);

        when(mockEndpointProvider.resolveEndpoint(ArgumentMatchers.any(S3EndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder().url(URI.create("https://bucket.s3.amazonaws.com")).build()));

        S3AsyncClient s3Client = clientBuilder().crossRegionAccessEnabled(true)
                                                .region(Region.of(region))
                                                .endpointProvider(mockEndpointProvider).build();
        s3Client.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBytes()).join();
        assertThat(captureInterceptor.endpointProvider).isInstanceOf(BucketEndpointProvider.class);
        ArgumentCaptor<S3EndpointParams> collectionCaptor = ArgumentCaptor.forClass(S3EndpointParams.class);
        verify(mockEndpointProvider, atLeastOnce()).resolveEndpoint(collectionCaptor.capture());
        collectionCaptor.getAllValues().forEach(resolvedParams -> {
            assertThat(resolvedParams.region()).isEqualTo(Region.US_EAST_1);
            assertThat(resolvedParams.useGlobalEndpoint()).isFalse();
        });

    }

    private S3AsyncClientBuilder clientBuilder() {
        return S3AsyncClient.builder()
                            .httpClient(mockAsyncHttpClient)
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
