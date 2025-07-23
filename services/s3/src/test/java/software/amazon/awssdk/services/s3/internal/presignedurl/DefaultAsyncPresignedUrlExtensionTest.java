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

package software.amazon.awssdk.services.s3.internal.presignedurl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.xml.AwsS3ProtocolFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;

/**
 * Tests for {@link DefaultAsyncPresignedUrlExtension} using MockAsyncHttpClient to verify HTTP interactions.
 */
class DefaultAsyncPresignedUrlExtensionTest {

    private static final String TEST_CONTENT = "test-content";
    private static final URI DEFAULT_ENDPOINT = URI.create("https://defaultendpoint.com");
    private static final String TEST_URL = "https://test-bucket.s3.us-east-1.amazonaws.com/test-key?" +
                                           "X-Amz-Date=20250707T000000Z&" +
                                           "X-Amz-Signature=test-signature-value&" +
                                           "X-Amz-Algorithm=AWS4-HMAC-SHA256&" +
                                           "X-Amz-SignedHeaders=host&" +
                                           "X-Amz-Security-Token=test-session-token&" +
                                           "X-Amz-Credential=AKIAIOSFODNN7EXAMPLE%2F20250707%2Fus-east-1%2Fs3%2Faws4_request&" +
                                           "X-Amz-Expires=86400";

    private MockAsyncHttpClient mockHttpClient;
    private DefaultAsyncPresignedUrlExtension presignedUrlExtension;
    private PresignedUrlDownloadRequest testRequest;
    private AwsProtocolMetadata protocolMetadata;
    private AwsS3ProtocolFactory protocolFactory;
    private AsyncClientHandler clientHandler;

    @BeforeEach
    void setUp() throws Exception {
        mockHttpClient = new MockAsyncHttpClient();
        URL testPresignedUrl = new URL(TEST_URL);
        testRequest = PresignedUrlDownloadRequest.builder()
                                                  .presignedUrl(testPresignedUrl)
                                                  .build();

        SdkClientConfiguration clientConfiguration = getDefaultSdkConfigs();
        protocolMetadata = AwsProtocolMetadata.builder()
                                              .serviceProtocol(AwsServiceProtocol.REST_XML)
                                              .build();
        protocolFactory = initProtocolFactory(clientConfiguration);
        clientHandler = new AwsAsyncClientHandler(clientConfiguration);

        presignedUrlExtension = new DefaultAsyncPresignedUrlExtension(
            clientHandler, protocolFactory, clientConfiguration, protocolMetadata);
    }

    private static Stream<Arguments> httpResponseTestCases() {
        return Stream.of(
            Arguments.of(
                "Success response",
                createSuccessResponse(),
                true,
                null
            ),
            Arguments.of(
                "404 Not Found response",
                createErrorResponse(404, "NoSuchKey", "The specified key does not exist."),
                false,
                NoSuchKeyException.class
            ),
            Arguments.of(
                "403 Invalid Object State response",
                createErrorResponse(403, "InvalidObjectState", "The operation is not valid for the object's storage class."),
                false,
                InvalidObjectStateException.class
            ),
            Arguments.of(
                "Generic error response",
                createErrorResponse(500, "InternalError", "We encountered an internal error. Please try again."),
                false,
                S3Exception.class
            )
        );
    }

    private static Stream<Arguments> requestConfigurationTestCases() {
        return Stream.of(
            Arguments.of(
                "Basic request",
                (Consumer<PresignedUrlDownloadRequest.Builder>) builder ->
                    builder.presignedUrl(createTestUrl()),
                null
            ),
            Arguments.of(
                "Request with range header",
                (Consumer<PresignedUrlDownloadRequest.Builder>) builder ->
                    builder.presignedUrl(createTestUrl()).range("bytes=0-1024"),
                "bytes=0-1024"
            )
        );
    }

    private static Stream<Arguments> additionalTestCases() {
        return Stream.of(
            Arguments.of("Custom transformer test", "CUSTOM_TRANSFORMER"),
            Arguments.of("Metrics collection test", "METRICS_COLLECTION")
        );
    }

    private static Stream<Arguments> invalidUrlTestCases() {
        return Stream.of(
            Arguments.of("Invalid URL format", "not-a-url"),
            Arguments.of("Empty HTTP URL", "http://"),
            Arguments.of("Empty HTTPS URL", "https://"),
            Arguments.of("Empty string", "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("httpResponseTestCases")
    void given_AsyncPresignedUrlExtension_when_GetObjectWithDifferentHttpResponses_then_ShouldHandleSuccessAndErrorsCorrectly(
            String testName,
            HttpExecuteResponse response,
            boolean expectSuccess,
            Class<? extends Exception> expectedExceptionType) {
        
        mockHttpClient.stubNextResponse(response);
        
        if (expectSuccess) {
            assertSuccessfulGetObject(testRequest);
        } else {
            CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
                presignedUrlExtension.getObject(testRequest, AsyncResponseTransformer.toBytes());
            
            assertThatThrownBy(future::join)
                .hasCauseInstanceOf(expectedExceptionType);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestConfigurationTestCases")
    void given_AsyncPresignedUrlExtension_when_GetObjectWithDifferentRequestConfigurations_then_ShouldSetCorrectHeaders(
            String testName,
            Consumer<PresignedUrlDownloadRequest.Builder> requestCustomizer,
            String expectedRangeHeader) throws ExecutionException, InterruptedException {
        
        mockHttpClient.stubNextResponse(createSuccessResponse());
        
        PresignedUrlDownloadRequest.Builder builder = PresignedUrlDownloadRequest.builder();
        requestCustomizer.accept(builder);
        PresignedUrlDownloadRequest request = builder.build();
        
        CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
            presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> result = future.get();
        
        assertThat(result).isNotNull();
        assertThat(result.asUtf8String()).isEqualTo(TEST_CONTENT);
        
        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest.method()).isEqualTo(SdkHttpMethod.GET);
        
        if (expectedRangeHeader != null) {
            assertThat(lastRequest.firstMatchingHeader("Range"))
                .isPresent()
                .contains(expectedRangeHeader);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("additionalTestCases")
    void given_AsyncPresignedUrlExtension_when_ExecutingDifferentScenarios_then_ShouldBehaveCorrectly(
            String testName, String testType) throws Exception {
        
        switch (testType) {
            case "CUSTOM_TRANSFORMER":
                mockHttpClient.stubNextResponse(createSuccessResponse());
                CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
                    presignedUrlExtension.getObject(testRequest, AsyncResponseTransformer.toBytes());
                ResponseBytes<GetObjectResponse> result = future.get();
                assertThat(result.asUtf8String()).isEqualTo(TEST_CONTENT);
                break;
                
            case "METRICS_COLLECTION":
                MetricPublisher mockPublisher = mock(MetricPublisher.class);
                SdkClientConfiguration clientConfigWithMetrics = getDefaultSdkConfigs().toBuilder()
                    .option(SdkClientOption.METRIC_PUBLISHERS, Collections.singletonList(mockPublisher))
                    .build();
                DefaultAsyncPresignedUrlExtension extensionWithMetrics = new DefaultAsyncPresignedUrlExtension(
                    clientHandler, protocolFactory, clientConfigWithMetrics, protocolMetadata);
                mockHttpClient.stubNextResponse(createSuccessResponse());
                CompletableFuture<ResponseBytes<GetObjectResponse>> metricsFuture = 
                    extensionWithMetrics.getObject(testRequest, AsyncResponseTransformer.toBytes());
                metricsFuture.get();
                
                verify(mockPublisher, atLeastOnce()).publish(any(MetricCollection.class));
                ArgumentCaptor<MetricCollection> metricsCaptor = ArgumentCaptor.forClass(MetricCollection.class);
                verify(mockPublisher).publish(metricsCaptor.capture());
                MetricCollection capturedMetrics = metricsCaptor.getValue();
                assertThat(capturedMetrics.metricValues(CoreMetric.SERVICE_ID)).contains("S3");
                assertThat(capturedMetrics.metricValues(CoreMetric.OPERATION_NAME)).contains("PresignedUrlDownload");
                break;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUrlTestCases")
    void given_AsyncPresignedUrlExtension_when_GetObjectWithInvalidUrl_then_ShouldThrowException(
            String testName, String invalidUrlString) {
        assertThatThrownBy(() -> {
            URL invalidUrl = new URL(invalidUrlString);
            PresignedUrlDownloadRequest invalidRequest = PresignedUrlDownloadRequest.builder()
                                                                                     .presignedUrl(invalidUrl)
                                                                                     .build();
            presignedUrlExtension.getObject(invalidRequest, AsyncResponseTransformer.toBytes()).join();
        }).satisfiesAnyOf(
            ex -> assertThat(ex).isInstanceOf(java.net.MalformedURLException.class),
            ex -> assertThat(ex).isInstanceOf(SdkClientException.class),
            ex -> assertThat(ex).isInstanceOf(java.util.concurrent.CompletionException.class)
                    .extracting(Throwable::getCause)
                    .isInstanceOf(SdkClientException.class)
        );
    }

    private SdkClientConfiguration getDefaultSdkConfigs() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.ASYNC_HTTP_CLIENT, mockHttpClient)
                                     .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, Collections.emptyMap())
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, Collections.emptyList())
                                     .option(SdkClientOption.RETRY_STRATEGY, AwsRetryStrategy.doNotRetry())
                                     .option(SdkAdvancedClientOption.USER_AGENT_PREFIX, "")
                                     .option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, "")
                                     .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                     .option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, AnonymousCredentialsProvider.create())
                                     .option(AwsClientOption.AWS_REGION, Region.US_EAST_2)
                                     .option(AwsClientOption.SIGNING_REGION, Region.US_EAST_2)
                                     .option(AwsClientOption.SERVICE_SIGNING_NAME, Region.AP_EAST_2.toString())
                                     .option(AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION, false)
                                     .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, Executors.newScheduledThreadPool(1))
                                     .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                                     .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                                             software.amazon.awssdk.core.ClientEndpointProvider.forEndpointOverride(DEFAULT_ENDPOINT))
                                     .build();
    }

    private AwsS3ProtocolFactory initProtocolFactory(SdkClientConfiguration configuration) {
        return AwsS3ProtocolFactory.builder()
                                   .registerModeledException(
                                       ExceptionMetadata.builder().errorCode("NoSuchKey")
                                                        .exceptionBuilderSupplier(NoSuchKeyException::builder)
                                                        .httpStatusCode(404).build())
                                   .registerModeledException(
                                       ExceptionMetadata.builder().errorCode("InvalidObjectState")
                                                        .exceptionBuilderSupplier(InvalidObjectStateException::builder)
                                                        .httpStatusCode(403).build())
                                   .clientConfiguration(configuration)
                                   .defaultServiceExceptionSupplier(S3Exception::builder)
                                   .build();
    }

    private static URL createTestUrl() {
        try {
            return new URL(TEST_URL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertSuccessfulGetObject(PresignedUrlDownloadRequest request) {
        try {
            CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
                presignedUrlExtension.getObject(request, AsyncResponseTransformer.toBytes());
            ResponseBytes<GetObjectResponse> result = future.get();
            
            assertThat(result).isNotNull();
            assertThat(result.asUtf8String()).isEqualTo(TEST_CONTENT);

            SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
            assertThat(lastRequest.method()).isEqualTo(SdkHttpMethod.GET);
            assertThat(lastRequest.getUri().toString()).contains("test-bucket.s3.us-east-1.amazonaws.com/test-key");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpExecuteResponse createSuccessResponse() {
        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
                                                              .statusCode(200)
                                                              .putHeader("Content-Length", "12")
                                                              .putHeader("ETag", "\"test-etag\"")
                                                              .putHeader("Content-Type", "text/plain")
                                                              .build();
        return HttpExecuteResponse.builder()
                                  .response(httpResponse)
                                  .responseBody(AbortableInputStream.create(
                                      new ByteArrayInputStream(TEST_CONTENT.getBytes(StandardCharsets.UTF_8))))
                                  .build();
    }

    private static HttpExecuteResponse createErrorResponse(int statusCode, String errorCode, String errorMessage) {
        String errorContent = String.format(
            "<e><Code>%s</Code><Message>%s</Message></e>",
            errorCode, errorMessage);
        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
                                                              .statusCode(statusCode)
                                                              .putHeader("x-amz-request-id", "test-request-id")
                                                              .putHeader("x-amz-id-2", "test-extended-request-id")
                                                              .build();
        return HttpExecuteResponse.builder()
                                  .response(httpResponse)
                                  .responseBody(AbortableInputStream.create(
                                      new ByteArrayInputStream(errorContent.getBytes(StandardCharsets.UTF_8))))
                                  .build();
    }
}