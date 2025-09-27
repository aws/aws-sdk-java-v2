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

package software.amazon.awssdk.services.sqs.batchmanager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.util.concurrent.RateLimiter;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;


/**
 * Tests the batching efficiency of {@link SqsAsyncBatchManager} under various load scenarios.
 */
public class BatchingEfficiencyUnderLoadTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/MyQueue";
    private static final int CONCURRENT_THREADS = 50;
    private static final int MAX_BATCH_SIZE = 10;
    private static final int SEND_FREQUENCY_MILLIS = 5;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .configureStaticDsl(true)
                                                         .build();

    private SqsAsyncClient client;
    private SqsAsyncBatchManager batchManager;

    @BeforeEach
    void setUp() {
        client = SqsAsyncClient.builder()
                               .endpointOverride(URI.create("http://localhost:" + wireMock.getPort()))
                               .checksumValidationEnabled(false)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create("key", "secret")))
                               .build();

        batchManager = SqsAsyncBatchManager.builder()
                                           .client(client)
                                           .scheduledExecutor(Executors.newScheduledThreadPool(10))
                                           .overrideConfiguration(config -> config
                                               .sendRequestFrequency(Duration.ofMillis(SEND_FREQUENCY_MILLIS))
                                               .maxBatchSize(MAX_BATCH_SIZE))
                                           .build();
    }

    @AfterEach
    void tearDown() {
        batchManager.close();
        client.close();
    }

    /**
     * Test runs heavy load and expects average batch sizes to be close to max.
     */
    @Test
    void sendMessage_whenHighLoadScenario_shouldEfficientlyBatchMessages() throws Exception {
        int expectedBatchSize = 25; // more than double the actual max of 10
        int rateLimit = 1000 / SEND_FREQUENCY_MILLIS * expectedBatchSize;
        int messageCount = rateLimit * 2; // run it for 2 seconds
        runThroughputTest(messageCount, rateLimit);

        // Then: Verify messages were efficiently batched
        List<LoggedRequest> batchRequests = findAll(postRequestedFor(anyUrl()));

        // Calculate batching metrics
        List<Integer> batchSizes = batchRequests.stream()
                                                .map(req -> req.getBodyAsString().split("\"Id\"").length - 1)
                                                .collect(Collectors.toList());

        double avgBatchSize = batchSizes.stream()
                                        .mapToInt(Integer::intValue)
                                        .average()
                                        .orElse(0);

        double fullBatchRatio = batchSizes.stream()
                                          .filter(size -> size >= 9)
                                          .count() / (double) batchSizes.size();
        
        // Assert efficient batching
        assertThat(avgBatchSize)
            .as("Average batch size")
            .isGreaterThan(8.0);


        assertThat(fullBatchRatio)
            .as("Ratio of nearly full batches (9-10 messages)")
            .isGreaterThan(0.8);

        assertThat((double)batchRequests.size())
            .as("Total batch requests for %d messages", messageCount)
            .isLessThan(messageCount / 5d);
    }

    /**
     * Test runs a load that should cause an average batch size of 5.
     */
    @Test
    void sendMessage_whenMediumLoadScenario_shouldCreateHalfSizeBatches() throws Exception {
        int expectedBatchSize = 5;
        int rateLimit = 1000 / SEND_FREQUENCY_MILLIS * expectedBatchSize;
        int messageCount = rateLimit * 2; // run it for 2 seconds
        runThroughputTest(messageCount, rateLimit);

        // Then: Verify batches were roughly half max size
        List<LoggedRequest> batchRequests = findAll(postRequestedFor(anyUrl()));

        // Calculate batching metrics
        List<Integer> batchSizes = batchRequests.stream()
                                                .map(req -> req.getBodyAsString().split("\"Id\"").length - 1)
                                                .collect(Collectors.toList());

        double avgBatchSize = batchSizes.stream()
                                        .mapToInt(Integer::intValue)
                                        .average()
                                        .orElse(0);
        
        // Assert batch expected range
        assertThat(avgBatchSize)
            .as("Average batch size")
            .isLessThan(7.0)
            .isGreaterThan(3.0);

        assertThat((double)batchRequests.size())
            .as("Total batch requests for %d messages", messageCount)
            .isLessThan(messageCount / 3d);
    }

    @Test
    void sendMessage_whenLowLoadScenario_shouldCreateSmallBatches() throws Exception {
        int expectedBatchSize = 1;
        int rateLimit = 1000 / SEND_FREQUENCY_MILLIS * expectedBatchSize;
        int messageCount = rateLimit * 2; // run it for 2 seconds
        runThroughputTest(messageCount, rateLimit);

        // Then: Verify batches were roughly half max size
        List<LoggedRequest> batchRequests = findAll(postRequestedFor(anyUrl()));

        // Calculate batching metrics
        List<Integer> batchSizes = batchRequests.stream()
                                                .map(req -> req.getBodyAsString().split("\"Id\"").length - 1)
                                                .collect(Collectors.toList());

        double avgBatchSize = batchSizes.stream()
                                        .mapToInt(Integer::intValue)
                                        .average()
                                        .orElse(0);

        // Assert batch expected range
        assertThat(avgBatchSize)
            .as("Average batch size")
            .isLessThan(2.0);

        assertThat((double)batchRequests.size())
            .as("Total batch requests for %d messages", messageCount)
            .isGreaterThan(messageCount * .5);
    }

    private void runThroughputTest(int messageCount, int rateLimit) throws InterruptedException {
        // Given: SQS returns success for batch requests
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{\"Successful\": []}")));

        // When: Send rateLimit messages per second concurrently (using 50 threads)
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);

        // Rate limit to spread it out over a couple seconds; enough time to make
        // any orphaned scheduled flushes obvious.
        RateLimiter rateLimiter = RateLimiter.create(rateLimit);

        for (int i = 0; i < messageCount; i++) {
            String messageBody = String.valueOf(i);
            rateLimiter.acquire();
            executor.execute(() -> {
                try {
                    batchManager.sendMessage(builder ->
                                                 builder.queueUrl(QUEUE_URL)
                                                        .messageBody(messageBody));
                } catch (Exception ignored) {
                    // Test will fail on assertions if messages aren't sent
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}