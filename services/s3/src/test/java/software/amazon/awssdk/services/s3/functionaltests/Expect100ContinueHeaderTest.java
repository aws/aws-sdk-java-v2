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
package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Functional tests to verify RFC 9110 compliance for Expect: 100-continue header behavior.
 * <p>
 * Per RFC 9110 Section 10.1.1, clients MUST NOT send 100-continue for requests without content.
 * These tests verify the header is correctly omitted for zero-length bodies and included for
 * non-empty bodies in both sync and async S3 operations.
 * <p>
 * Also verifies that {@link S3Configuration#expectContinueEnabled()} suppresses the header
 * for all put operations regardless of body content, across all HTTP client types.
 */
@WireMockTest(httpsEnabled = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Expect100ContinueHeaderTest {

    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private static final String UPLOAD_ID = "test-upload-id";

    private S3Client syncClient;
    private S3AsyncClient asyncClient;

    @BeforeEach
    void setup(WireMockRuntimeInfo wmRuntimeInfo) {
        URI endpointOverride = URI.create(wmRuntimeInfo.getHttpsBaseUrl());

        syncClient = S3Client.builder()
                             .httpClient(ApacheHttpClient.builder().buildWithDefaults(trustAllCerts()))
                             .region(Region.US_EAST_1)
                             .endpointOverride(endpointOverride)
                             .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                             .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                             .forcePathStyle(true)
                             .serviceConfiguration(S3Configuration.builder()
                                                                  .expectContinueThresholdInBytes(0L)
                                                                  .build())
                             .credentialsProvider(staticCredentials())
                             .build();

        asyncClient = S3AsyncClient.builder()
                                   .httpClient(NettyNioAsyncHttpClient.builder().buildWithDefaults(trustAllCerts()))
                                   .region(Region.US_EAST_1)
                                   .endpointOverride(endpointOverride)
                                   .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                                   .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                                   .forcePathStyle(true)
                                   .serviceConfiguration(S3Configuration.builder()
                                                                        .expectContinueThresholdInBytes(0L)
                                                                        .build())
                                   .credentialsProvider(staticCredentials())
                                   .build();
    }

    @AfterEach
    void teardown() {
        if (syncClient != null) {
            syncClient.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
    }

    // -----------------------------------------------------------------------
    // RFC 9110 compliance: empty body vs non-empty body (uses default clients)
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "{0}_withEmptyBody_doesNotSendExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_whenEmptyBody_shouldNotSendExpectHeader(
            String operationType, Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(RequestBody.empty());
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withEmptyStringBody_doesNotSendExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_whenEmptyStringBody_shouldNotSendExpectHeader(
            String operationType, Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(RequestBody.fromString(""));
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withNonEmptyBody_sendsExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_whenNonEmptyBody_shouldSendExpectHeader(
            String operationType, Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(RequestBody.fromString("test content"));
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withContentProviderEmptyBody_sendsExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_whenContentProviderEmptyBody_shouldSendExpectHeader(
            String operationType, Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        ContentStreamProvider emptyProvider = () -> new ByteArrayInputStream(new byte[0]);
        executeRequest.apply(RequestBody.fromContentProvider(emptyProvider, "application/octet-stream"));
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withContentProviderNonEmptyBody_sendsExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_whenContentProviderNonEmptyBody_shouldSendExpectHeader(
            String operationType, Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        ContentStreamProvider contentProvider = () -> new ByteArrayInputStream(content);
        executeRequest.apply(RequestBody.fromContentProvider(contentProvider, "application/octet-stream"));
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withEmptyBody_doesNotSendExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_whenEmptyBody_shouldNotSendExpectHeader(
            String operationType, Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.empty()).join();
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withEmptyStringBody_doesNotSendExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_whenEmptyStringBody_shouldNotSendExpectHeader(
            String operationType, Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.fromString("")).join();
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withNonEmptyBody_sendsExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_whenNonEmptyBody_shouldSendExpectHeader(
            String operationType, Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.fromString("test content")).join();
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withPublisherEmptyBody_sendsExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_whenPublisherEmptyBody_shouldSendExpectHeader(
            String operationType, Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.fromPublisher(emptyPublisher())).join();
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withPublisherNonEmptyBody_sendsExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_whenPublisherNonEmptyBody_shouldSendExpectHeader(
            String operationType, Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.fromPublisher(contentPublisher("test content"))).join();
        assertExpectHeaderPresent();
    }

    // -----------------------------------------------------------------------
    // expectContinueEnabled: parameterized across HTTP client types
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "PutObject: {0}")
    @MethodSource("expectContinueConfigProvider")
    void putObject_whenExpectContinueConfigured_shouldMatchExpectedBehavior(
            String testName, String clientType, S3Configuration config,
            boolean expectHeaderPresent, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        stubAnyPutRequest();
        executeS3Operation(clientType, wmRuntimeInfo, config, true);
        assertExpectHeader(expectHeaderPresent);
    }

    @ParameterizedTest(name = "UploadPart: {0}")
    @MethodSource("expectContinueConfigProvider")
    void uploadPart_whenExpectContinueConfigured_shouldMatchExpectedBehavior(
            String testName, String clientType, S3Configuration config,
            boolean expectHeaderPresent, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        stubAnyPutRequest();
        executeS3Operation(clientType, wmRuntimeInfo, config, false);
        assertExpectHeader(expectHeaderPresent);
    }

    // -----------------------------------------------------------------------
    // S3 operation execution helper
    // -----------------------------------------------------------------------

    private void executeS3Operation(String clientType, WireMockRuntimeInfo wmInfo,
                                    S3Configuration config, boolean isPutObject) throws Exception {
        switch (clientType) {
            case "APACHE":
            case "APACHE_EC_DISABLED":
            case "APACHE_EC_ENABLED":
            case "URL_CONNECTION":
            case "CRT_SYNC": {
                try (S3Client client = buildSyncClient(clientType, wmInfo, config)) {
                    if (isPutObject) {
                        client.putObject(basePutObjectRequest().build(), RequestBody.fromString("test content"));
                    } else {
                        client.uploadPart(baseUploadPartRequest().build(), RequestBody.fromString("test content"));
                    }
                }
                break;
            }
            case "NETTY":
            case "CRT_ASYNC": {
                try (S3AsyncClient client = buildAsyncClient(clientType, wmInfo, config)) {
                    if (isPutObject) {
                        client.putObject(basePutObjectRequest().build(),
                                         AsyncRequestBody.fromString("test content")).join();
                    } else {
                        client.uploadPart(baseUploadPartRequest().build(),
                                          AsyncRequestBody.fromString("test content")).join();
                    }
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown client type: " + clientType);
        }
    }

    // -----------------------------------------------------------------------
    // Test data providers
    // -----------------------------------------------------------------------

    private static Stream<Arguments> expectContinueConfigProvider() {
        S3Configuration disabledConfig = S3Configuration.builder()
                                                        .expectContinueEnabled(false)
                                                        .build();
        S3Configuration enabledConfig = S3Configuration.builder()
                                                       .expectContinueEnabled(true)
                                                       .expectContinueThresholdInBytes(0L)
                                                       .build();

        return Stream.of(
            // Apache: default — both interceptor and Apache add the header
            Arguments.of("Apache - default config", "APACHE", null, true),
            Arguments.of("Apache - S3Config enabled=true", "APACHE", enabledConfig, true),
            // Apache: S3Config enabled=false stops the interceptor, but Apache still adds the header independently
            Arguments.of("Apache - S3Config enabled=false (Apache still adds)", "APACHE", disabledConfig, true),
            // Apache with ec=false + S3Config enabled=false: fully suppressed
            Arguments.of("Apache(ec=false) + S3Config enabled=false", "APACHE_EC_DISABLED", disabledConfig, false),
            // Apache with ec=true + S3Config enabled=false: Apache still adds the header independently
            Arguments.of("Apache(ec=true) + S3Config enabled=false", "APACHE_EC_ENABLED", disabledConfig, true),

            // URL Connection: only the interceptor adds the header
            Arguments.of("UrlConnection - default config", "URL_CONNECTION", null, true),
            Arguments.of("UrlConnection - S3Config enabled=false", "URL_CONNECTION", disabledConfig, false),

            // CRT sync (generic HTTP client): only the interceptor adds the header
            Arguments.of("CrtSync - default config", "CRT_SYNC", null, true),
            Arguments.of("CrtSync - S3Config enabled=false", "CRT_SYNC", disabledConfig, false),

            // Netty: only the interceptor adds the header
            Arguments.of("Netty - default config", "NETTY", null, true),
            Arguments.of("Netty - S3Config enabled=true", "NETTY", enabledConfig, true),
            Arguments.of("Netty - S3Config enabled=false", "NETTY", disabledConfig, false),

            // CRT async (generic HTTP client): only the interceptor adds the header
            Arguments.of("CrtAsync - default config", "CRT_ASYNC", null, true),
            Arguments.of("CrtAsync - S3Config enabled=false", "CRT_ASYNC", disabledConfig, false)
        );
    }

    private Stream<Arguments> syncRequestProvider() {
        return Stream.of(
            Arguments.of("PutObject", (Function<RequestBody, Void>) body -> {
                syncClient.putObject(basePutObjectRequest().build(), body);
                return null;
            }),
            Arguments.of("UploadPart", (Function<RequestBody, Void>) body -> {
                syncClient.uploadPart(baseUploadPartRequest().build(), body);
                return null;
            })
        );
    }

    private Stream<Arguments> asyncRequestProvider() {
        return Stream.of(
            Arguments.of("PutObject", (Function<AsyncRequestBody, CompletableFuture<?>>) body ->
                asyncClient.putObject(basePutObjectRequest().build(), body)),
            Arguments.of("UploadPart", (Function<AsyncRequestBody, CompletableFuture<?>>) body ->
                asyncClient.uploadPart(baseUploadPartRequest().build(), body))
        );
    }

    // -----------------------------------------------------------------------
    // Client builders
    // -----------------------------------------------------------------------

    private S3Client buildSyncClient(String clientType, WireMockRuntimeInfo wmInfo, S3Configuration config) {
        SdkHttpClient httpClient;
        switch (clientType) {
            case "APACHE":
                httpClient = ApacheHttpClient.builder().buildWithDefaults(trustAllCerts());
                break;
            case "APACHE_EC_ENABLED":
                httpClient = ApacheHttpClient.builder()
                                             .expectContinueEnabled(true)
                                             .buildWithDefaults(trustAllCerts());
                break;
            case "APACHE_EC_DISABLED":
                httpClient = ApacheHttpClient.builder()
                                             .expectContinueEnabled(false)
                                             .buildWithDefaults(trustAllCerts());
                break;
            case "URL_CONNECTION":
                httpClient = UrlConnectionHttpClient.builder().buildWithDefaults(trustAllCerts());
                break;
            case "CRT_SYNC":
                httpClient = AwsCrtHttpClient.builder().buildWithDefaults(trustAllCerts());
                break;
            default:
                throw new IllegalArgumentException("Not a sync client type: " + clientType);
        }

        return S3Client.builder()
                       .httpClient(httpClient)
                       .region(Region.US_EAST_1)
                       .endpointOverride(URI.create(wmInfo.getHttpsBaseUrl()))
                       .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                       .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                       .forcePathStyle(true)
                       .serviceConfiguration(config != null ? config
                                                      : S3Configuration.builder()
                                                                       .expectContinueThresholdInBytes(0L)
                                                                       .build())
                       .credentialsProvider(staticCredentials())
                       .build();
    }

    private S3AsyncClient buildAsyncClient(String clientType, WireMockRuntimeInfo wmInfo, S3Configuration config) {
        SdkAsyncHttpClient httpClient;
        switch (clientType) {
            case "NETTY":
                httpClient = NettyNioAsyncHttpClient.builder().buildWithDefaults(trustAllCerts());
                break;
            case "CRT_ASYNC":
                httpClient = AwsCrtAsyncHttpClient.builder().buildWithDefaults(trustAllCerts());
                break;
            default:
                throw new IllegalArgumentException("Not an async client type: " + clientType);
        }

        return S3AsyncClient.builder()
                            .httpClient(httpClient)
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create(wmInfo.getHttpsBaseUrl()))
                            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                            .forcePathStyle(true)
                            .serviceConfiguration(config != null ? config
                                                           : S3Configuration.builder()
                                                                            .expectContinueThresholdInBytes(0L)
                                                                            .build())
                            .credentialsProvider(staticCredentials())
                            .build();
    }

    // -----------------------------------------------------------------------
    // Request builders
    // -----------------------------------------------------------------------

    private static PutObjectRequest.Builder basePutObjectRequest() {
        return PutObjectRequest.builder().bucket(BUCKET).key(KEY);
    }

    private static UploadPartRequest.Builder baseUploadPartRequest() {
        return UploadPartRequest.builder().bucket(BUCKET).key(KEY).uploadId(UPLOAD_ID).partNumber(1);
    }

    // -----------------------------------------------------------------------
    // Publisher helpers
    // -----------------------------------------------------------------------

    private static Publisher<ByteBuffer> emptyPublisher() {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
            }
        });
    }

    private static Publisher<ByteBuffer> contentPublisher(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean sent = false;

            @Override
            public void request(long n) {
                if (!sent && n > 0) {
                    sent = true;
                    subscriber.onNext(ByteBuffer.wrap(bytes));
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
            }
        });
    }

    // -----------------------------------------------------------------------
    // Stubs and assertions
    // -----------------------------------------------------------------------

    private void stubAnyPutRequest() {
        stubFor(put(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("ETag", "\"test-etag\"")));
    }

    private void assertExpectHeader(boolean expectPresent) {
        if (expectPresent) {
            assertExpectHeaderPresent();
        } else {
            assertExpectHeaderNotPresent();
        }
    }

    private void assertExpectHeaderNotPresent() {
        LoggedRequest request = getSingleCapturedRequest();
        assertThat(request.header("Expect") == null || !request.header("Expect").isPresent())
            .as("Expect header should not be present")
            .isTrue();
    }

    private void assertExpectHeaderPresent() {
        LoggedRequest request = getSingleCapturedRequest();
        assertThat(request.header("Expect"))
            .as("Expect: 100-continue header should be present")
            .isNotNull()
            .satisfies(header -> assertThat(header.firstValue()).isEqualTo("100-continue"));
    }

    private LoggedRequest getSingleCapturedRequest() {
        List<LoggedRequest> requests = findAll(putRequestedFor(anyUrl()));
        assertThat(requests)
            .as("Expected exactly one HTTP request to be captured")
            .hasSize(1);
        return requests.get(0);
    }

    // -----------------------------------------------------------------------
    // Common helpers
    // -----------------------------------------------------------------------

    private static StaticCredentialsProvider staticCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    private static AttributeMap trustAllCerts() {
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                           .build();
    }
}
