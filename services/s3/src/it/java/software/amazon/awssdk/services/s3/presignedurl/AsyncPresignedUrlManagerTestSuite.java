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

import static org.apache.commons.lang3.RandomStringUtils.randomAscii;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
    protected static AsyncPresignedUrlManager presignedUrlManager;
    protected static String testBucket;

    @TempDir
    static Path temporaryFolder;

    protected static String testGetObjectKey;
    protected static String testLargeObjectKey;
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
        testObjectContent = "Hello AsyncPresignedUrlManager Integration Test";
        testLargeObjectContent = randomAscii(5 * 1024 * 1024).getBytes(StandardCharsets.UTF_8);
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicFunctionalityTestData")
    void getObject_withValidPresignedUrl_returnsContent(String testDescription, 
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
    void getObject_withValidPresignedUrl_savesContentToFile() throws Exception {
        PresignedUrlGetObjectRequest request = createRequestForKey(testGetObjectKey);
        Path downloadFile = temporaryFolder.resolve("download-" + UUID.randomUUID() + ".txt");
        CompletableFuture<GetObjectResponse> future =
            presignedUrlManager.getObject(request, downloadFile);
        GetObjectResponse response = future.get();

        assertThat(response).isNotNull();
        assertThat(downloadFile).exists();
        assertThat(downloadFile).hasContent(testObjectContent);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rangeTestData")
    void getObject_withRangeRequest_returnsSpecifiedRange(String testDescription, 
                       String range, 
                       String expectedContent) throws Exception {
        PresignedUrlGetObjectRequest request = createRequestForKey(testGetObjectKey, range);

        CompletableFuture<ResponseBytes<GetObjectResponse>> future =
            presignedUrlManager.getObject(request, AsyncResponseTransformer.toBytes());
        ResponseBytes<GetObjectResponse> response = future.get();

        assertThat(response.asUtf8String()).isEqualTo(expectedContent);
    }

    @Test
    void getObject_withMultipleRangeRequestsConcurrently_returnsCorrectContent() throws Exception {
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
    void getObject_withLargeObjectToFile_savesCompleteContentAndCollectsMetrics() throws Exception {
        List<MetricCollection> collectedMetrics = new ArrayList<>();
        MetricPublisher metricPublisher = new MetricPublisher() {
            @Override
            public void publish(MetricCollection metricCollection) {
                collectedMetrics.add(metricCollection);
            }
            @Override
            public void close() {}
        };

        try (S3AsyncClient clientWithMetrics = s3AsyncClientBuilder()
                                                           .overrideConfiguration(o -> o.addMetricPublisher(metricPublisher))
                                                           .build()) {
            
            AsyncPresignedUrlManager metricsManager = clientWithMetrics.presignedUrlManager();
            PresignedUrlGetObjectRequest request = createRequestForKey(testLargeObjectKey);
            Path downloadFile = temporaryFolder.resolve("large-download-with-metrics-" + UUID.randomUUID() + ".bin");
            
            CompletableFuture<GetObjectResponse> future =
                metricsManager.getObject(request, downloadFile);
            GetObjectResponse response = future.get(60, TimeUnit.SECONDS);
            
            assertThat(response).isNotNull();
            assertThat(downloadFile).exists();
            assertThat(downloadFile.toFile().length()).isEqualTo(testLargeObjectContent.length);
            assertThat(collectedMetrics).isNotEmpty();
        }
    }

    static Stream<Arguments> basicFunctionalityTestData() {
        return Stream.of(
            Arguments.of("getObject_withValidUrl_returnsContent", 
                        testGetObjectKey, testObjectContent),
            Arguments.of("getObject_withValidLargeObjectUrl_returnsContent", 
                        testLargeObjectKey, null)
        );
    }

    static Stream<Arguments> rangeTestData() {
        String content = "Hello AsyncPresignedUrlManager Integration Test";
        return Stream.of(
            Arguments.of("getObject_withPrefix10BytesRange_returnsFirst10Bytes",
                        "bytes=0-9", content.substring(0, 10)),
            Arguments.of("getObject_withSuffix10BytesRange_returnsLast10Bytes",
                        "bytes=-10", content.substring(content.length() - 10)),
            Arguments.of("getObject_withMiddle10BytesRange_returnsMiddle10Bytes",
                        "bytes=10-19", content.substring(10, 20)),
            Arguments.of("getObject_withSingleByteRange_returnsSingleByte",
                        "bytes=0-0", content.substring(0, 1))
        );
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
