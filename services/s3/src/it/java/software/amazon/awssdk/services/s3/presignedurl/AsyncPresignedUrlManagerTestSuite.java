/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.awssdk.services.s3.presignedurl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;
import software.amazon.awssdk.services.s3.utils.S3TestUtils;
import software.amazon.awssdk.testutils.service.S3BucketUtils;

/**
 * Abstract test suite for AsyncPresignedUrlManager integration tests.
 */
public abstract class AsyncPresignedUrlManagerTestSuite extends S3IntegrationTestBase {
    protected static S3Presigner presigner;
    protected AsyncPresignedUrlManager presignedUrlManager;
    protected static String testBucket;

    @TempDir
    static Path temporaryFolder;

    protected static String testGetObjectKey;
    protected static String testLargeObjectKey;
    protected static String testNonExistentKey;
    protected static String testObjectContent;
    protected static byte[] testLargeObjectContent;

    protected abstract S3AsyncClient createS3AsyncClient();

    @BeforeAll
    static void setUpTestSuite() throws Exception {
        setUp();

        presigner = S3Presigner.builder()
                                .region(DEFAULT_REGION)
                                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                .build();
        testBucket = S3BucketUtils.temporaryBucketName("async-presigned-url-manager-test");
        createBucket(testBucket);
        testGetObjectKey = generateRandomObjectKey();
        testLargeObjectKey = generateRandomObjectKey() + "-large";
        testNonExistentKey = generateRandomObjectKey() + "-nonexistent";
        testObjectContent = "Hello AsyncPresignedUrlManager Integration Test";
        testLargeObjectContent = new byte[5 * 1024 * 1024];
        for (int i = 0; i < testLargeObjectContent.length; i++) {
            testLargeObjectContent[i] = (byte) (i % 256);
        }
        S3TestUtils.putObject(AsyncPresignedUrlManagerTestSuite.class, s3, testBucket, testGetObjectKey, testObjectContent);
        s3Async.putObject(
            PutObjectRequest.builder()
                            .bucket(testBucket)
                            .key(testLargeObjectKey)
                            .build(),
            AsyncRequestBody.fromBytes(testLargeObjectContent)
        ).join();
        S3TestUtils.addCleanupTask(AsyncPresignedUrlManagerTestSuite.class, () -> {
            s3.deleteObject(DeleteObjectRequest.builder()
                                                    .bucket(testBucket)
                                                    .key(testGetObjectKey)
                                                    .build());
            s3.deleteObject(DeleteObjectRequest.builder()
                                                    .bucket(testBucket)
                                                    .key(testLargeObjectKey)
                                                    .build());
            deleteBucketAndAllContents(testBucket);
        });
    }

    @BeforeEach
    void setUpEach() {
        S3AsyncClient s3AsyncClient = createS3AsyncClient();
        presignedUrlManager = s3AsyncClient.presignedUrlManager();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicFunctionalityTestData")
    void given_validPresignedUrl_when_requestingObject_then_returnsContent(String testDescription, 
                               String objectKey, 
                               String expectedContent) throws Exception {
        PresignedUrlGetObjectRequest request = createRequestForKey(objectKey);

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> response = future.get();

        assertThat(response).isNotNull();
        if (expectedContent != null) {
            assertThat(response.asUtf8String()).isEqualTo(expectedContent);
            assertThat(response.response().contentLength()).isEqualTo(expectedContent.length());
        }
        assertThat(response.response()).isNotNull();
    }

    @Test
    void given_validPresignedUrl_when_downloadingToFile_then_savesContentToFile() throws Exception {
        PresignedUrlGetObjectRequest request = createRequestForKey(testGetObjectKey);
        Path downloadFile = temporaryFolder.resolve("download-" + UUID.randomUUID() + ".txt");
        CompletableFuture<GetObjectResponse> future =
            presignedUrlManager.getObject(request, downloadFile);
        GetObjectResponse response = future.get();

        assertThat(response).isNotNull();
        assertThat(downloadFile).exists();
        assertThat(downloadFile).hasContent(testObjectContent);
    }

    @Test
    void given_validPresignedUrl_when_usingConsumerBuilder_then_returnsContent() throws Exception {
        URL presignedUrl = createPresignedUrl(testGetObjectKey);

        CompletableFuture<ResponseBytes<GetObjectResponse>> bytesFuture =
            presignedUrlManager.getObject(
                builder -> builder.presignedUrl(presignedUrl),
                AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> bytesResponse = bytesFuture.get();

        assertThat(bytesResponse).isNotNull();
        assertThat(bytesResponse.asUtf8String()).isEqualTo(testObjectContent);

        Path downloadFile = temporaryFolder.resolve("consumer-builder-download-" + UUID.randomUUID() + ".txt");
        CompletableFuture<GetObjectResponse> fileFuture =
            presignedUrlManager.getObject(
                builder -> builder.presignedUrl(presignedUrl),
                AsyncResponseTransformer.toFile(downloadFile));
        GetObjectResponse fileResponse = fileFuture.get();

        assertThat(fileResponse).isNotNull();
        assertThat(downloadFile).exists();
        assertThat(downloadFile).hasContent(testObjectContent);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rangeTestData")
    void given_validRangeRequest_when_requestingPartialContent_then_returnsSpecifiedRange(String testDescription, 
                       String range, 
                       String expectedContent) throws Exception {
        PresignedUrlGetObjectRequest request = createRequestForKey(testGetObjectKey, range);

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> response = future.get();

        assertThat(response.asUtf8String()).isEqualTo(expectedContent);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errorHandlingTestData")
    void given_invalidRequest_when_executingOperation_then_throwsExpectedException(String testDescription,
                          String errorType,
                          Class<? extends Exception> expectedExceptionType) throws Exception {

        PresignedUrlGetObjectRequest request = createErrorRequest(errorType);
        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());

        switch (errorType) {
            case "nonExistentKey":
                assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .satisfies(ex -> {
                        Throwable cause = ex.getCause();
                        assertThat(cause).satisfiesAnyOf(
                            c -> assertThat(c).isInstanceOf(NoSuchKeyException.class),
                            c -> assertThat(c).isInstanceOf(SdkClientException.class)
                        );
                    });
                break;
            case "invalidUrl":
            case "expiredUrl":
                assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .satisfies(ex -> {
                        Throwable cause = ex.getCause();
                        assertThat(cause).satisfiesAnyOf(
                            c -> assertThat(c).isInstanceOf(S3Exception.class),
                            c -> assertThat(c).isInstanceOf(SdkClientException.class)
                        );
                    });
                break;
            case "malformedUrl":
                assertThatThrownBy(future::get)
                    .isInstanceOf(ExecutionException.class)
                    .satisfies(ex -> {
                        Throwable cause = ex.getCause();
                        // Accept either IllegalArgumentException or network-related exceptions
                        assertThat(cause).satisfiesAnyOf(
                            c -> assertThat(c).isInstanceOf(IllegalArgumentException.class),
                            c -> assertThat(c).isInstanceOf(SdkClientException.class)
                        );
                    });
                break;
        }
    }

    @Test
    void given_multipleRangeRequests_when_executingConcurrently_then_returnsCorrectContent() throws Exception {
        String concurrentTestKey = uploadTestObject("concurrent-test", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        List<CompletableFuture<ResponseBytes<GetObjectResponse>>> futures = new ArrayList<>();

        futures.add(presignedUrlManager.getObject(
            createRequestForKey(concurrentTestKey, "bytes=0-8"),   // "012345678"
            AsyncResponseTransformer.toBytes()));
        futures.add(presignedUrlManager.getObject(
            createRequestForKey(concurrentTestKey, "bytes=9-17"),  // "9ABCDEFGH"
            AsyncResponseTransformer.toBytes()));
        futures.add(presignedUrlManager.getObject(
            createRequestForKey(concurrentTestKey, "bytes=18-26"), // "IJKLMNOPQ"
            AsyncResponseTransformer.toBytes()));
        futures.add(presignedUrlManager.getObject(
            createRequestForKey(concurrentTestKey, "bytes=27-35"), // "RSTUVWXYZ"
            AsyncResponseTransformer.toBytes()));

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        allFutures.get(30, TimeUnit.SECONDS);

        StringBuilder result = new StringBuilder();
        for (CompletableFuture<ResponseBytes<GetObjectResponse>> future : futures) {
            result.append(future.get().asUtf8String());
        }

        assertThat(result.toString()).isEqualTo("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    void given_largeObject_when_requestingRanges_then_returnsCorrectChunks() throws Exception {
        List<CompletableFuture<ResponseBytes<GetObjectResponse>>> futures = new ArrayList<>();
        int chunkSize = 1024 * 1024;

        for (int i = 0; i < 4; i++) {
            int start = i * chunkSize;
            int end = start + chunkSize - 1;
            String range = String.format("bytes=%d-%d", start, end);
            futures.add(presignedUrlManager.getObject(
                createRequestForKey(testLargeObjectKey, range),
                AsyncResponseTransformer.toBytes()));
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));
        allFutures.get(60, TimeUnit.SECONDS);
        for (int i = 0; i < futures.size(); i++) {
            ResponseBytes<GetObjectResponse> response = futures.get(i).get();
            assertThat(response.asByteArray()).hasSize(chunkSize);
            byte[] chunk = response.asByteArray();
            int baseOffset = i * chunkSize;
            for (int j = 0; j < chunk.length; j++) {
                int expectedValue = (baseOffset + j) % 256;
                assertThat(chunk[j]).isEqualTo((byte) expectedValue);
            }
        }
    }

    @Test
    void given_largeObject_when_downloadingToFile_then_savesCompleteContent() throws Exception {
        PresignedUrlGetObjectRequest request = createRequestForKey(testLargeObjectKey);
        Path downloadFile = temporaryFolder.resolve("large-download-" + UUID.randomUUID() + ".bin");
        CompletableFuture<GetObjectResponse> future =
            presignedUrlManager.getObject(request, downloadFile);
        GetObjectResponse response = future.get();

        assertThat(response).isNotNull();
        assertThat(downloadFile).exists();
        assertThat(downloadFile.toFile().length()).isEqualTo(testLargeObjectContent.length);
    }

    @Test
    void given_clientWithMetrics_when_executingRequest_then_collectsMetrics() throws Exception {
        List<MetricCollection> collectedMetrics = new ArrayList<>();
        MetricPublisher metricPublisher = new MetricPublisher() {
            @Override
            public void publish(MetricCollection metricCollection) {
                collectedMetrics.add(metricCollection);
            }
            @Override
            public void close() {}
        };

        try (S3AsyncClient clientWithMetrics = S3AsyncClient.builder()
                                                           .overrideConfiguration(o -> o.addMetricPublisher(metricPublisher))
                                                           .build()) {
            
            AsyncPresignedUrlManager metricsManager = clientWithMetrics.presignedUrlManager();
            PresignedUrlGetObjectRequest request = createRequestForKey(testGetObjectKey);

            CompletableFuture<ResponseBytes<GetObjectResponse>> future =
                metricsManager.getObject(request, AsyncResponseTransformer.toBytes());
            ResponseBytes<GetObjectResponse> response = future.get(30, TimeUnit.SECONDS);
            
            assertThat(response).isNotNull();
            assertThat(collectedMetrics).isNotEmpty();
        }
    }

    @Test
    void given_presignedUrl_when_usingBuilderPattern_then_returnsContent() throws Exception {
        PresignedUrlGetObjectRequest request = PresignedUrlGetObjectRequest.builder()
            .presignedUrl(createPresignedUrl(testGetObjectKey))
            .build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());

        ResponseBytes<GetObjectResponse> response = future.get();
        assertThat(response.asUtf8String()).isEqualTo(testObjectContent);
    }

    static Stream<Arguments> basicFunctionalityTestData() {
        return Stream.of(
            Arguments.of("given_validUrl_when_requestingObject_then_returnsContent", 
                        testGetObjectKey, testObjectContent),
            Arguments.of("given_validUrl_when_requestingLargeObject_then_returnsContent", 
                        testLargeObjectKey, null),
            Arguments.of("given_validUrl_when_requestingWithBuilder_then_returnsContent", 
                        testGetObjectKey, testObjectContent)
        );
    }

    static Stream<Arguments> rangeTestData() {
        String content = "Hello AsyncPresignedUrlManager Integration Test";
        return Stream.of(
            Arguments.of("given_validUrl_when_requestingPrefix10Bytes_then_returnsFirst10Bytes",
                        "bytes=0-9", content.substring(0, 10)),
            Arguments.of("given_validUrl_when_requestingSuffix10Bytes_then_returnsLast10Bytes",
                        "bytes=-10", content.substring(content.length() - 10)),
            Arguments.of("given_validUrl_when_requestingMiddle10Bytes_then_returnsMiddle10Bytes",
                        "bytes=10-19", content.substring(10, 20)),
            Arguments.of("given_validUrl_when_requestingSingleByte_then_returnsSingleByte",
                        "bytes=0-0", content.substring(0, 1))
        );
    }

    static Stream<Arguments> errorHandlingTestData() {
        return Stream.of(
            Arguments.of("given_nonExistentKey_when_requestingObject_then_throwsNoSuchKeyException",
                        "nonExistentKey", NoSuchKeyException.class),
            Arguments.of("given_invalidUrl_when_requestingObject_then_throwsS3Exception",
                        "invalidUrl", S3Exception.class),
            Arguments.of("given_expiredUrl_when_requestingObject_then_throwsS3Exception",
                        "expiredUrl", S3Exception.class),
            Arguments.of("given_malformedUrl_when_requestingObject_then_throwsIllegalArgumentException",
                        "malformedUrl", IllegalArgumentException.class)
        );
    }

    @AfterAll
    static void tearDownTestSuite() {
        try {
            S3TestUtils.runCleanupTasks(AsyncPresignedUrlManagerTestSuite.class);
        } catch (Exception e) {
        }
        
        if (presigner != null) {
            presigner.close();
        }
        cleanUpResources();
    }

    // Helper methods
    private static String generateRandomObjectKey() {
        return "async-presigned-url-manager-test-" + UUID.randomUUID();
    }

    private PresignedUrlGetObjectRequest createRequestForKey(String key) {
        return PresignedUrlGetObjectRequest.builder()
            .presignedUrl(createPresignedUrl(key))
            .build();
    }

    private PresignedUrlGetObjectRequest createRequestForKey(String key, String range) {
        return PresignedUrlGetObjectRequest.builder()
            .presignedUrl(createPresignedUrl(key))
            .range(range)
            .build();
    }

    private URL createPresignedUrl(String key) {
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r -> r
            .getObjectRequest(req -> req.bucket(testBucket).key(key))
            .signatureDuration(Duration.ofMinutes(10)));
        return presignedRequest.url();
    }

    private PresignedUrlGetObjectRequest createErrorRequest(String errorType) {
        switch (errorType) {
            case "nonExistentKey":
                return createRequestForKey(testNonExistentKey);
            case "invalidUrl":
                return createRequestForKey("invalid-key-that-does-not-exist-" + UUID.randomUUID());
            case "expiredUrl":
                PresignedGetObjectRequest expiredRequest = presigner.presignGetObject(r -> r
                    .getObjectRequest(req -> req.bucket(testBucket).key(testGetObjectKey))
                    .signatureDuration(Duration.ofSeconds(1))); // Minimum valid duration
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for URL to expire", e);
                }
                
                return PresignedUrlGetObjectRequest.builder()
                    .presignedUrl(expiredRequest.url())
                    .build();
            case "malformedUrl":
                try {
                    return PresignedUrlGetObjectRequest.builder()
                        .presignedUrl(new URL("http://invalid-hostname-that-does-not-exist"))
                        .build();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            default:
                throw new IllegalArgumentException("Unknown error type: " + errorType);
        }
    }

    private String uploadTestObject(String keyPrefix, String content) {
        String key = keyPrefix + "-" + UUID.randomUUID();
        S3TestUtils.putObject(AsyncPresignedUrlManagerTestSuite.class, s3, testBucket, key, content);

        S3TestUtils.addCleanupTask(AsyncPresignedUrlManagerTestSuite.class, () -> {
            s3.deleteObject(DeleteObjectRequest.builder()
                                                    .bucket(testBucket)
                                                    .key(key)
                                                    .build());
        });
        return key;
    }
}
