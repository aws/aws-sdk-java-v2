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

package software.amazon.awssdk.services.s3.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.TaggingDirective;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Verifies that the CRT-based S3 client publishes CRT native request telemetry to client-level and request-level
 * {@link MetricPublisher}s. Each underlying CRT request attempt is published as its own
 * {@code ApiCall -> ApiCallAttempt -> HttpClient} {@link MetricCollection}, so a multipart transfer yields several.
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public class S3CrtClientMetricPublisherIntegrationTest extends S3IntegrationTestBase {
    private static final String BUCKET = temporaryBucketName(S3CrtClientMetricPublisherIntegrationTest.class);
    private static final String SMALL_KEY = "small-single-part";
    private static final String LARGE_KEY = "large-multipart";
    private static final long PART_SIZE = 8L * 1024 * 1024;
    // Below the 8MB threshold -> single underlying GET.
    private static final int SMALL_SIZE = 100 * 1024;
    // Comfortably above the threshold -> the meta-request fans out into multiple underlying part requests.
    private static final int LARGE_SIZE = 17 * 1024 * 1024;

    private static final CapturingMetricPublisher CLIENT_PUBLISHER = new CapturingMetricPublisher();
    private static S3AsyncClient crtClient;

    @BeforeAll
    public static void setup() throws Exception {
        S3IntegrationTestBase.setUp();
        S3IntegrationTestBase.createBucket(BUCKET);

        crtClient = S3AsyncClient.crtBuilder()
                                 .region(S3IntegrationTestBase.DEFAULT_REGION)
                                 .credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)
                                 .minimumPartSizeInBytes(PART_SIZE)
                                 .thresholdInBytes(PART_SIZE)
                                 .addMetricPublisher(CLIENT_PUBLISHER)
                                 .build();

        S3IntegrationTestBase.s3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(SMALL_KEY).build(),
                                           new RandomTempFile(SMALL_SIZE).toPath());
        S3IntegrationTestBase.s3.putObject(PutObjectRequest.builder().bucket(BUCKET).key(LARGE_KEY).build(),
                                           new RandomTempFile(LARGE_SIZE).toPath());
    }

    @AfterAll
    public static void cleanup() {
        crtClient.close();
        S3IntegrationTestBase.deleteBucketAndAllContents(BUCKET);
    }

    @BeforeEach
    public void resetPublisher() {
        CLIENT_PUBLISHER.clear();
    }

    @Test
    void singlePartGetObject_publishesApiCallMetrics() throws InterruptedException {
        crtClient.getObject(b -> b.bucket(BUCKET).key(SMALL_KEY), AsyncResponseTransformer.toBytes()).join();

        List<MetricCollection> collections = CLIENT_PUBLISHER.awaitAtLeast(1, Duration.ofSeconds(30));
        assertThat(collections).isNotEmpty();
        collections.forEach(S3CrtClientMetricPublisherIntegrationTest::assertIsCrtApiCallCollection);
    }

    @Test
    void multipartGetObject_publishesOneCollectionPerUnderlyingRequest() throws InterruptedException {
        crtClient.getObject(b -> b.bucket(BUCKET).key(LARGE_KEY), AsyncResponseTransformer.toBytes()).join();

        // A multipart download fans out into several underlying requests, so we expect more than one collection.
        List<MetricCollection> collections = CLIENT_PUBLISHER.awaitAtLeast(2, Duration.ofSeconds(60));
        assertThat(collections).hasSizeGreaterThanOrEqualTo(2);
        collections.forEach(S3CrtClientMetricPublisherIntegrationTest::assertIsCrtApiCallCollection);
    }

    @Test
    void putObject_publishesApiCallMetrics() throws InterruptedException {
        crtClient.putObject(b -> b.bucket(BUCKET).key("put-metrics-key"),
                            AsyncRequestBody.fromBytes(new byte[SMALL_SIZE])).join();

        List<MetricCollection> collections = CLIENT_PUBLISHER.awaitAtLeast(1, Duration.ofSeconds(30));
        assertThat(collections).isNotEmpty();
        collections.forEach(S3CrtClientMetricPublisherIntegrationTest::assertIsCrtApiCallCollection);
    }

    @Test
    void failedGetObject_publishesUnsuccessfulApiCallMetrics() throws InterruptedException {
        try {
            crtClient.getObject(b -> b.bucket(BUCKET).key("does-not-exist-" + System.nanoTime()),
                                AsyncResponseTransformer.toBytes()).join();
        } catch (RuntimeException expected) {
            // 404 - the metrics for the failed attempt should still be published below.
        }

        List<MetricCollection> collections = CLIENT_PUBLISHER.awaitAtLeast(1, Duration.ofSeconds(30));
        assertThat(collections).isNotEmpty();
        collections.forEach(S3CrtClientMetricPublisherIntegrationTest::assertIsCrtApiCallCollection);
        assertThat(collections).anySatisfy(
            c -> assertThat(c.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).contains(false));
    }

    @Test
    void requestLevelPublisher_overridesClientLevelPublisher() throws InterruptedException {
        CapturingMetricPublisher clientPublisher = new CapturingMetricPublisher();
        CapturingMetricPublisher requestPublisher = new CapturingMetricPublisher();

        try (S3AsyncClient client = crtClientWith(clientPublisher)) {
            client.getObject(b -> b.bucket(BUCKET).key(SMALL_KEY)
                                   .overrideConfiguration(o -> o.addMetricPublisher(requestPublisher)),
                             AsyncResponseTransformer.toBytes()).join();

            // The request-level publisher receives the CRT telemetry ...
            List<MetricCollection> requestCollections = requestPublisher.awaitAtLeast(1, Duration.ofSeconds(30));
            assertThat(requestCollections).isNotEmpty();
            requestCollections.forEach(S3CrtClientMetricPublisherIntegrationTest::assertIsCrtApiCallCollection);

            // ... and this client's own client-level publisher receives nothing, since request-level takes precedence.
            assertThat(clientPublisher.awaitAtLeast(1, Duration.ofSeconds(1))).isEmpty();
        }
    }

    @Test
    void copyObjectWithRequestLevelPublisher_publishesSubRequestTelemetryToIt() throws InterruptedException {
        CapturingMetricPublisher clientPublisher = new CapturingMetricPublisher();
        CapturingMetricPublisher requestPublisher = new CapturingMetricPublisher();
        String destinationKey = "copy-dest-" + System.nanoTime();

        try (S3AsyncClient client = crtClientWith(clientPublisher)) {
            client.copyObject(b -> b.sourceBucket(BUCKET).sourceKey(LARGE_KEY)
                                    .destinationBucket(BUCKET).destinationKey(destinationKey)
                                    .overrideConfiguration(o -> o.addMetricPublisher(requestPublisher)))
                  .join();

            List<MetricCollection> collections = requestPublisher.awaitAtLeast(2, Duration.ofSeconds(60));
            assertThat(collections).hasSizeGreaterThanOrEqualTo(2);
            collections.forEach(S3CrtClientMetricPublisherIntegrationTest::assertIsCrtApiCallCollection);

            // request-level takes precedence, so this client's own client-level publisher sees none of the sub-requests.
            assertThat(clientPublisher.awaitAtLeast(1, Duration.ofSeconds(1))).isEmpty();
        }
    }

    @Test
    void copyObjectWithTaggingDirective_requestLevelPublisher_receivesTaggingSubRequestMetrics() throws InterruptedException {
        // Tag the source so a taggingDirective(COPY) copy actually issues GetObjectTagging + PutObjectTagging sub-requests.
        S3IntegrationTestBase.s3.putObjectTagging(
            r -> r.bucket(BUCKET).key(LARGE_KEY)
                  .tagging(t -> t.tagSet(Tag.builder().key("env").value("t").build())));

        CapturingMetricPublisher requestPublisher = new CapturingMetricPublisher();
        String destinationKey = "copy-tagged-dest-" + System.nanoTime();

        try (S3AsyncClient client = crtClientWith(new CapturingMetricPublisher())) {
            client.copyObject(b -> b.sourceBucket(BUCKET).sourceKey(LARGE_KEY)
                                    .destinationBucket(BUCKET).destinationKey(destinationKey)
                                    .taggingDirective(TaggingDirective.COPY)
                                    .overrideConfiguration(o -> o.addMetricPublisher(requestPublisher)))
                  .join();

            List<MetricCollection> collections = requestPublisher.awaitAtLeast(2, Duration.ofSeconds(60));
            collections.forEach(S3CrtClientMetricPublisherIntegrationTest::assertIsCrtApiCallCollection);
            assertThat(collections).anySatisfy(
                c -> assertThat(c.metricValues(CoreMetric.OPERATION_NAME)).contains("GetObjectTagging"));
            assertThat(collections).anySatisfy(
                c -> assertThat(c.metricValues(CoreMetric.OPERATION_NAME)).contains("PutObjectTagging"));
        }
    }

    private static S3AsyncClient crtClientWith(MetricPublisher clientLevelPublisher) {
        return S3AsyncClient.crtBuilder()
                            .region(S3IntegrationTestBase.DEFAULT_REGION)
                            .credentialsProvider(AwsTestBase.CREDENTIALS_PROVIDER_CHAIN)
                            .minimumPartSizeInBytes(PART_SIZE)
                            .thresholdInBytes(PART_SIZE)
                            .addMetricPublisher(clientLevelPublisher)
                            .build();
    }

    private static void assertIsCrtApiCallCollection(MetricCollection apiCall) {
        assertThat(apiCall.name()).isEqualTo("ApiCall");
        assertThat(apiCall.metricValues(CoreMetric.SERVICE_ID)).containsExactly("S3");
        assertThat(apiCall.metricValues(CoreMetric.OPERATION_NAME)).isNotEmpty();
        assertThat(apiCall.metricValues(CoreMetric.API_CALL_DURATION)).isNotEmpty();

        MetricCollection attempt = apiCall.childrenWithName("ApiCallAttempt").findFirst().orElse(null);
        assertThat(attempt).as("ApiCallAttempt child").isNotNull();

        MetricCollection httpClient = attempt.childrenWithName("HttpClient").findFirst().orElse(null);
        assertThat(httpClient).as("HttpClient child").isNotNull();
        assertThat(httpClient.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("s3crt");
    }

    /**
     * Thread-safe capture of published collections. {@code onTelemetry} fires on CRT native threads and can lag the
     * completion of the operation future, so tests poll {@link #awaitAtLeast(int, Duration)} rather than reading
     * immediately.
     */
    private static final class CapturingMetricPublisher implements MetricPublisher {
        private final List<MetricCollection> collections = new CopyOnWriteArrayList<>();

        @Override
        public void publish(MetricCollection metricCollection) {
            collections.add(metricCollection);
        }

        @Override
        public void close() {
        }

        void clear() {
            collections.clear();
        }

        List<MetricCollection> awaitAtLeast(int min, Duration timeout) throws InterruptedException {
            long deadlineNanos = System.nanoTime() + timeout.toNanos();
            while (collections.size() < min && System.nanoTime() < deadlineNanos) {
                Thread.sleep(100);
            }
            return new ArrayList<>(collections);
        }
    }
}
