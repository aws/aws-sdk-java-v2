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

package software.amazon.awssdk.services.s3.presignedurl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.services.s3.utils.S3TestUtils;
import software.amazon.awssdk.testutils.service.S3BucketUtils;

/**
 * Integration test verifying SDK doesn't override presigner-set headers.
 */
class AsyncPresignedUrlExtensionSignedHeadersIntegrationTest extends S3IntegrationTestBase {

    private static final long PART_SIZE = 8 * 1024 * 1024L;
    private static final String FAKE_ETAG = "\"d41d8cd98f00b204e9800998ecf8427e\"";

    private static S3Presigner presigner;
    private static String testBucket;
    private static String testKey;

    @BeforeAll
    static void setUpFixture() throws Exception {
        setUp();
        presigner = S3Presigner.builder()
                               .region(DEFAULT_REGION)
                               .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                               .build();

        testBucket = S3BucketUtils.temporaryBucketName("signed-headers-test");
        createBucket(testBucket);

        // Upload object > 8MB to test multipart
        testKey = "signed-headers-test-object";
        byte[] largeContent = RandomStringUtils.randomAscii(12 * 1024 * 1024).getBytes(StandardCharsets.UTF_8);
        s3Async.putObject(
            PutObjectRequest.builder().bucket(testBucket).key(testKey).build(),
            AsyncRequestBody.fromBytes(largeContent)
        ).join();

        S3TestUtils.addCleanupTask(AsyncPresignedUrlExtensionSignedHeadersIntegrationTest.class, () -> {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(testBucket).key(testKey).build());
            deleteBucketAndAllContents(testBucket);
        });
    }

    @AfterAll
    static void tearDownFixture() {
        presigner.close();
        S3TestUtils.runCleanupTasks(AsyncPresignedUrlExtensionSignedHeadersIntegrationTest.class);
    }

    @Test
    void getObject_withPresignedRangeAndMultipartClient_shouldUseSinglePart() {
        PresignedGetObjectRequest presigned = presign(r -> r.range("bytes=0-1023"));

        DownloadResult result = download(presigned, true);

        assertThat(result.bytes).hasSize(1024);
        assertThat(result.statusCode).isEqualTo(206);
        assertThat(result.apiCallCount).isEqualTo(1);
    }

    @Test
    void getObject_withPresignedIfMatchAndMultipartClient_shouldMultipartSucceed() {
        String realEtag = getRealEtag();
        PresignedGetObjectRequest presigned = presign(r -> r.ifMatch(realEtag));

        DownloadResult result = download(presigned, true);

        assertThat(result.bytes.length).isGreaterThan((int) PART_SIZE);
        assertThat(result.apiCallCount).isGreaterThan(1);
    }

    @Test
    void getObject_withNoSignedRange_shouldMultipartSucceed() {
        PresignedGetObjectRequest presigned = presign(r -> {});

        DownloadResult result = download(presigned, true);

        assertThat(result.bytes.length).isGreaterThan((int) PART_SIZE);
        assertThat(result.apiCallCount).isGreaterThan(1);
    }

    @Test
    void getObject_withPresignedFakeIfMatch_shouldThrowS3Exception() {
        PresignedGetObjectRequest presigned = presign(r -> r.ifMatch(FAKE_ETAG));

        assertThatThrownBy(() -> download(presigned, false))
            .hasCauseInstanceOf(S3Exception.class);
    }

    @Test
    void build_withPresignedUrlHavingSignedRange_shouldThrow() {
        PresignedGetObjectRequest presigned = presign(r -> r.range("bytes=0-1023"));

        assertThatThrownBy(() ->
                               PresignedUrlDownloadRequest.builder()
                                                          .presignedUrl(presigned.url())
                                                          .build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("presignedGetObjectRequest()");
    }

    @Test
    void build_withConflictingRange_shouldThrow() {
        PresignedGetObjectRequest presigned = presign(r -> r.range("bytes=0-1023"));

        assertThatThrownBy(() ->
                               PresignedUrlDownloadRequest.builder()
                                                          .presignedGetObjectRequest(presigned)
                                                          .range("bytes=500-999")
                                                          .build()
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("conflicts with signed value");
    }

    // --- Helpers ---

    private PresignedGetObjectRequest presign(
        java.util.function.Consumer<software.amazon.awssdk.services.s3.model.GetObjectRequest.Builder> customizer) {
        return presigner.presignGetObject(
            GetObjectPresignRequest.builder()
                                   .signatureDuration(Duration.ofMinutes(10))
                                   .getObjectRequest(r -> {
                                       r.bucket(testBucket).key(testKey);
                                       customizer.accept(r);
                                   })
                                   .build());
    }

    private String getRealEtag() {
        PresignedGetObjectRequest presigned = presign(r -> {});
        return download(presigned, false).response.eTag();
    }

    private DownloadResult download(PresignedGetObjectRequest presigned, boolean multipart) {
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
                                                                         .presignedGetObjectRequest(presigned)
                                                                         .build();
        return executeDownload(request, multipart);
    }

    private DownloadResult executeDownload(PresignedUrlDownloadRequest request, boolean multipart) {
        List<MetricCollection> metrics = new ArrayList<>();
        S3AsyncClientBuilder clientBuilder = S3AsyncClient.builder()
                                                          .region(DEFAULT_REGION)
                                                          .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                          .overrideConfiguration(c -> c.addMetricPublisher(new MetricPublisher() {
                                                              @Override
                                                              public void publish(MetricCollection metricCollection) {
                                                                  metrics.add(metricCollection);
                                                              }

                                                              @Override
                                                              public void close() {
                                                              }
                                                          }));

        if (multipart) {
            clientBuilder.multipartEnabled(true)
                         .multipartConfiguration(c -> c.minimumPartSizeInBytes(PART_SIZE));
        }

        S3AsyncClient client = clientBuilder.build();
        try {
            ResponseBytes<GetObjectResponse> result = client.presignedUrlExtension()
                                                            .getObject(request, AsyncResponseTransformer.toBytes())
                                                            .join();

            return new DownloadResult(
                result.asByteArray(),
                result.response().sdkHttpResponse().statusCode(),
                metrics.size(),
                result.response());
        } finally {
            client.close();
        }
    }

    private static class DownloadResult {
        final byte[] bytes;
        final int statusCode;
        final int apiCallCount;
        final GetObjectResponse response;

        DownloadResult(byte[] bytes, int statusCode, int apiCallCount, GetObjectResponse response) {
            this.bytes = bytes;
            this.statusCode = statusCode;
            this.apiCallCount = apiCallCount;
            this.response = response;
        }
    }
}
