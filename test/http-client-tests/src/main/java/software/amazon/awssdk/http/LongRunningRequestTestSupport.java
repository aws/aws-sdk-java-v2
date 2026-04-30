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

package software.amazon.awssdk.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Shared helpers for the long-running request test suites.
 */
public final class LongRunningRequestTestSupport {

    public static final Duration CONFIGURED_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration SERVER_DELAY = Duration.ofSeconds(10);
    public static final Duration TIME_BOUND_SAFETY_MARGIN = Duration.ofSeconds(10);
    public static final Duration HANG_DELAY = Duration.ofMinutes(1);

    private LongRunningRequestTestSupport() {
    }

    /**
     * Simulates long-polling and synchronous-invoke-with-extended-wait (SFN getActivityTask, SQS receiveMessage,
     * Lambda invoke): server delays the entire response.
     */
    public static void stubLongPolling(WireMockExtension mockServer) {
        mockServer.stubFor(any(urlPathEqualTo("/"))
                               .willReturn(aResponse().withStatus(200)
                                                      .withBody("hello")
                                                      .withFixedDelay((int) SERVER_DELAY.toMillis())));
    }

    /**
     * Simulates streaming APIs (Transcribe Streaming, Kinesis subscribeToShard): server dribbles body chunks with
     * gaps between them.
     */
    public static void stubStreamingWithPauses(WireMockExtension mockServer) {
        mockServer.stubFor(any(urlPathEqualTo("/"))
                               .willReturn(aResponse().withStatus(200)
                                                      .withBody("hello")
                                                      .withChunkedDribbleDelay(2, (int) SERVER_DELAY.toMillis())));
    }

    /**
     * Hangs indefinitely so a holding request can exhaust a single-slot pool while another racing request
     * exercises the acquire timeout.
     */
    public static void stubHanging(WireMockExtension mockServer) {
        mockServer.stubFor(any(urlPathEqualTo("/"))
                               .willReturn(aResponse().withStatus(200)
                                                      .withBody("hello")
                                                      .withFixedDelay((int) HANG_DELAY.toMillis())));
    }

    public static void assertFailsWithinTimeBound(CompletableFuture<?> future, Duration expectedTimeout) {
        Duration maxWait = expectedTimeout.plus(TIME_BOUND_SAFETY_MARGIN);

        try {
            future.get(maxWait.toMillis(), TimeUnit.MILLISECONDS);
            throw new AssertionError("Expected request to throw an exception but it completed successfully");
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AssertionError(
                "Expected request to fail within " + maxWait + " but it was still running - client appears to hang",
                e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Unexpected interruption while waiting for request to fail", e);
        } catch (ExecutionException e) {
            // expected
        }
    }
}
