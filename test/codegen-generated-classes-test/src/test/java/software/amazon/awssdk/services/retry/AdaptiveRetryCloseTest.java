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

package software.amazon.awssdk.services.retry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.json.JsonAsyncClient;

class AdaptiveRetryCloseTest {
    private static final String CLOSING_THREAD_NAME = "adaptive-retry-close";

    private WireMockServer wireMock;
    private JsonAsyncClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().bindAddress("127.0.0.1").dynamicPort());
        wireMock.start();
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "application/x-amz-json-1.0")
            .withHeader("x-amzn-ErrorType", "ThrottlingException")
            .withBody("{\"__type\":\"ThrottlingException\",\"message\":\"Rate exceeded\"}")));
        client = JsonAsyncClient.builder()
                                .endpointOverride(URI.create("http://127.0.0.1:" + wireMock.port()))
                                .region(Region.US_EAST_1)
                                .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("akid", "skid")))
                                .overrideConfiguration(c -> c.retryStrategy(AwsRetryStrategy.adaptiveRetryStrategy(true)))
                                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void close_whenQueuedRequestReentersClient_completes() throws InterruptedException {
        JsonAsyncClient requestClient = client;
        AtomicBoolean callbackRanOnClosingThread = new AtomicBoolean();
        request(requestClient).handle((r, t) -> null).join();
        for (int i = 0; i < 200; i++) {
            request(requestClient).whenComplete((r, t) -> {
                if (Thread.currentThread().getName().equals(CLOSING_THREAD_NAME)) {
                    callbackRanOnClosingThread.set(true);
                }
                request(requestClient);
            });
        }
        Thread.sleep(1_500L);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        JsonAsyncClient closingClient = requestClient;
        Thread closingThread = new Thread(() -> {
            try {
                closingClient.close();
            } catch (Throwable t) {
                failure.set(t);
            }
        }, CLOSING_THREAD_NAME);
        closingThread.setDaemon(true);
        closingThread.start();
        client = null;
        closingThread.join(5_000L);

        assertThat(closingThread.isAlive()).isFalse();
        assertThat(failure.get()).isNull();
        assertThat(callbackRanOnClosingThread).isTrue();
    }

    private CompletableFuture<?> request(JsonAsyncClient requestClient) {
        return requestClient.allType(r -> {});
    }
}
