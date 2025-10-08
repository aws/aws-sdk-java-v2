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
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Functional tests to verify RFC 9110 compliance for Expect: 100-continue header behavior.
 * <p>
 * Per RFC 9110 Section 10.1.1, clients MUST NOT send 100-continue for requests without content.
 * These tests verify the header is correctly omitted for zero-length bodies and included for
 * non-empty bodies in both sync and async S3 operations.
 */
@WireMockTest(httpsEnabled = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Expect100ContinueHeaderTest {

    private static final String BUCKET = "test-bucket";
    private static final String KEY = "test-key";
    private static final String UPLOAD_ID = "test-upload-id";

    private S3Client syncClient;
    private S3AsyncClient asyncClient;

    @BeforeEach
    void setup(WireMockRuntimeInfo wmRuntimeInfo) {
        URI endpointOverride = URI.create(wmRuntimeInfo.getHttpsBaseUrl());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("akid", "skid"));

        SdkHttpClient apacheHttpClient = ApacheHttpClient.builder()
                                                         .buildWithDefaults(AttributeMap.builder()
                                                                                        .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                                        .build());

        syncClient = S3Client.builder()
                             .httpClient(apacheHttpClient)
                             .region(Region.US_EAST_1)
                             .endpointOverride(endpointOverride)
                             .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                             .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                             .forcePathStyle(true)
                             .credentialsProvider(credentialsProvider)
                             .build();

        SdkAsyncHttpClient nettyHttpClient = NettyNioAsyncHttpClient.builder()
                                                                    .buildWithDefaults(AttributeMap.builder()
                                                                                                   .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                                                   .build());

        asyncClient = S3AsyncClient.builder()
                                   .httpClient(nettyHttpClient)
                                   .region(Region.US_EAST_1)
                                   .endpointOverride(endpointOverride)
                                   .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                                   .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                                   .forcePathStyle(true)
                                   .credentialsProvider(credentialsProvider)
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

    @ParameterizedTest(name = "{0}_withEmptyBody_doesNotSendExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_withEmptyBody_doesNotSendExpectHeader(String operationType,
                                                             Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(RequestBody.empty());
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withEmptyStringBody_doesNotSendExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_withEmptyStringBody_doesNotSendExpectHeader(String operationType,
                                                                   Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(RequestBody.fromString(""));
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withNonEmptyBody_sendsExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_withNonEmptyBody_sendsExpectHeader(String operationType,
                                                          Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(RequestBody.fromString("test content"));
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withContentProviderEmptyBody_sendsExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_withContentProviderEmptyBody_sendsExpectHeader(String operationType,
                                                                      Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        ContentStreamProvider emptyProvider = () -> new ByteArrayInputStream(new byte[0]);
        RequestBody emptyBody = RequestBody.fromContentProvider(emptyProvider, "application/octet-stream");
        executeRequest.apply(emptyBody);
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withContentProviderNonEmptyBody_sendsExpectHeader")
    @MethodSource("syncRequestProvider")
    void syncOperation_withContentProviderNonEmptyBody_sendsExpectHeader(String operationType,
                                                                         Function<RequestBody, Void> executeRequest) {
        stubAnyPutRequest();
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        ContentStreamProvider contentProvider = () -> new ByteArrayInputStream(content);
        RequestBody providerBody = RequestBody.fromContentProvider(contentProvider, "application/octet-stream");
        executeRequest.apply(providerBody);
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withEmptyBody_doesNotSendExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_withEmptyBody_doesNotSendExpectHeader(String operationType,
                                                              Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.empty()).join();
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withEmptyStringBody_doesNotSendExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_withEmptyStringBody_doesNotSendExpectHeader(String operationType,
                                                                    Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.fromString("")).join();
        assertExpectHeaderNotPresent();
    }

    @ParameterizedTest(name = "{0}_withNonEmptyBody_sendsExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_withNonEmptyBody_sendsExpectHeader(String operationType,
                                                           Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        executeRequest.apply(AsyncRequestBody.fromString("test content")).join();
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withPublisherEmptyBody_sendsExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_withPublisherEmptyBody_sendsExpectHeader(String operationType,
                                                                 Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        Publisher<ByteBuffer> emptyPublisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    subscriber.onComplete();
                }

                @Override
                public void cancel() {
                }
            });
        };

        AsyncRequestBody emptyBody = AsyncRequestBody.fromPublisher(emptyPublisher);
        executeRequest.apply(emptyBody).join();
        assertExpectHeaderPresent();
    }

    @ParameterizedTest(name = "{0}_withPublisherNonEmptyBody_sendsExpectHeader")
    @MethodSource("asyncRequestProvider")
    void asyncOperation_withPublisherNonEmptyBody_sendsExpectHeader(String operationType,
                                                                    Function<AsyncRequestBody, CompletableFuture<?>> executeRequest) {
        stubAnyPutRequest();
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        Publisher<ByteBuffer> contentPublisher = subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                private boolean sent = false;

                @Override
                public void request(long n) {
                    if (!sent && n > 0) {
                        sent = true;
                        subscriber.onNext(ByteBuffer.wrap(content));
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                }
            });
        };

        AsyncRequestBody publisherBody = AsyncRequestBody.fromPublisher(contentPublisher);
        executeRequest.apply(publisherBody).join();
        assertExpectHeaderPresent();
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

    private PutObjectRequest.Builder basePutObjectRequest() {
        return PutObjectRequest.builder()
                               .bucket(BUCKET)
                               .key(KEY);
    }

    private UploadPartRequest.Builder baseUploadPartRequest() {
        return UploadPartRequest.builder()
                                .bucket(BUCKET)
                                .key(KEY)
                                .uploadId(UPLOAD_ID)
                                .partNumber(1);
    }

    private void stubAnyPutRequest() {
        stubFor(put(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("ETag", "\"test-etag\"")));
    }

    private void assertExpectHeaderNotPresent() {
        LoggedRequest request = getSingleCapturedRequest();

        assertThat(request.header("Expect") == null || !request.header("Expect").isPresent())
            .as("Expect header should not be present for empty body per RFC 9110 Section 10.1.1")
            .isTrue();
    }

    private void assertExpectHeaderPresent() {
        LoggedRequest request = getSingleCapturedRequest();

        assertThat(request.header("Expect"))
            .as("Expect: 100-continue header should be present for non-empty body")
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
}
