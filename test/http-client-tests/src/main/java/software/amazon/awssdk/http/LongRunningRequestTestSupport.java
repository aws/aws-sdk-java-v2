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
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
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
    public static final Duration CONFIGURED_ACQUIRE_TIMEOUT = Duration.ofSeconds(1);
    public static final String WARM_UP_PATH = "/warm-up";

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

    /**
     * Answers {@link #WARM_UP_PATH} at once, so a test can open a connection before it exhausts the pool.
     */
    public static void stubWarmUp(WireMockExtension mockServer) {
        mockServer.stubFor(any(urlPathEqualTo(WARM_UP_PATH)).willReturn(aResponse().withStatus(200).withBody("hello")));
    }

    /**
     * Waits until the server has received {@code count} requests, so a request is known to hold its connection.
     */
    public static void awaitReceivedRequests(WireMockExtension mockServer, int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (mockServer.getAllServeEvents().size() < count && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertThat(mockServer.getAllServeEvents()).hasSize(count);
    }

    /**
     * Asserts that the request fails within {@code expectedTimeout} plus {@link #TIME_BOUND_SAFETY_MARGIN}.
     *
     * @return the exception the request failed with
     */
    public static Throwable assertFailsWithinTimeBound(CompletableFuture<?> future, Duration expectedTimeout) {
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
            return e.getCause();
        }
    }

    /**
     * Same time-bound check as {@link #assertFailsWithinTimeBound} but also asserts that the failure cause chain
     * contains an {@link IOException}. The SDK retry layer relies on IOException-wrapping to classify failures as
     * transient, so HTTP clients must surface acquire/connect failures as (or wrapping) {@code IOException}.
     *
     * @return the exception the request failed with
     */
    public static Throwable assertFailsWithIoExceptionWithinTimeBound(CompletableFuture<?> future, Duration expectedTimeout) {
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
            if (!causeChainContains(e, IOException.class)) {
                throw new AssertionError(
                    "Expected failure cause chain to contain IOException so the SDK retry layer treats the error as "
                    + "transient, but found: " + describeCauseChain(e), e);
            }
            return e.getCause();
        }
    }

    /**
     * Asserts the client-specific parts of an acquire-timeout failure. A {@code null} expectation is not checked.
     */
    public static void assertAcquireTimeoutFailure(Throwable failure,
                                                   String expectedMessagePrefix,
                                                   Class<? extends Throwable> expectedCause) {
        if (expectedMessagePrefix != null) {
            assertThat(failure).hasMessageStartingWith(expectedMessagePrefix);
        }
        if (expectedCause != null) {
            assertThat(failure).hasCauseInstanceOf(expectedCause);
        }
    }

    private static boolean causeChainContains(Throwable t, Class<? extends Throwable> type) {
        Throwable current = t;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                return false;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String describeCauseChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable current = t;
        while (current != null) {
            if (sb.length() > 0) {
                sb.append(" -> ");
            }
            sb.append(current.getClass().getName());
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return sb.toString();
    }
}
