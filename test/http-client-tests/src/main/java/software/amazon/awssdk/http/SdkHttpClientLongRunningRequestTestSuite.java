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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.CONFIGURED_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.CONFIGURED_TIMEOUT;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.HANG_DELAY;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.WARM_UP_PATH;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertAcquireTimeoutFailure;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithIoExceptionWithinTimeBound;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithinTimeBound;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.awaitReceivedRequests;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubHanging;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubLongPolling;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubStreamingWithPauses;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubWarmUp;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Validates that {@link SdkHttpClient} implementations fail fast rather than hanging indefinitely when timeouts
 * are configured and the server violates timing expectations.
 */
public abstract class SdkHttpClientLongRunningRequestTestSuite {

    @RegisterExtension
    public WireMockExtension mockServer = WireMockExtension.newInstance()
                                                           .options(wireMockConfig().dynamicPort())
                                                           .build();

    protected abstract SdkHttpClient createSdkHttpClient(AttributeMap config);

    /**
     * The start of the message of the exception a request fails with when it cannot acquire a connection in time, or
     * {@code null} to not check the message.
     */
    protected String expectedAcquireTimeoutMessagePrefix() {
        return null;
    }

    /**
     * The type of the cause of the exception a request fails with when it cannot acquire a connection in time, or
     * {@code null} to not check the cause.
     */
    protected Class<? extends Throwable> expectedAcquireTimeoutCause() {
        return null;
    }

    @Test
    public void executeWhenReadTimeoutAndServerDelaysResponseFailsWithinTimeoutBound() {
        stubLongPolling(mockServer);

        SdkHttpClient client = createSdkHttpClient(AttributeMap.builder()
                                                               .put(SdkHttpConfigurationOption.READ_TIMEOUT,
                                                                    CONFIGURED_TIMEOUT)
                                                               .build());
        try {
            assertFailsWithinTimeBound(executeAsync(client), CONFIGURED_TIMEOUT);
        } finally {
            client.close();
        }
    }

    @Test
    public void executeWhenReadTimeoutAndStreamingResponsePausesFailsWithinTimeoutBound() {
        stubStreamingWithPauses(mockServer);

        SdkHttpClient client = createSdkHttpClient(AttributeMap.builder()
                                                               .put(SdkHttpConfigurationOption.READ_TIMEOUT,
                                                                    CONFIGURED_TIMEOUT)
                                                               .build());
        try {
            assertFailsWithinTimeBound(executeAsync(client), CONFIGURED_TIMEOUT);
        } finally {
            client.close();
        }
    }

    @Test
    public void executeWhenConnectionAcquireTimeoutAndPoolExhaustedFailsWithinTimeoutBound() throws Exception {
        stubHanging(mockServer);
        stubWarmUp(mockServer);

        SdkHttpClient client = createSdkHttpClient(AttributeMap.builder()
                                                               .put(SdkHttpConfigurationOption.READ_TIMEOUT,
                                                                    HANG_DELAY.plusMinutes(1))
                                                               .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, 1)
                                                               .put(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT,
                                                                    CONFIGURED_ACQUIRE_TIMEOUT)
                                                               .build());
        try {
            // Open the connection first, so the holding request below does not set it up under the short acquire timeout.
            executeRequest(client, WARM_UP_PATH);
            CompletableFuture<?> firstRequest = executeAsync(client);
            awaitReceivedRequests(mockServer, 2);

            Throwable failure = assertFailsWithIoExceptionWithinTimeBound(executeAsync(client), CONFIGURED_ACQUIRE_TIMEOUT);
            // executeRequest wraps the IOException that call() throws in an UncheckedIOException.
            assertAcquireTimeoutFailure(failure instanceof UncheckedIOException ? failure.getCause() : failure,
                                        expectedAcquireTimeoutMessagePrefix(),
                                        expectedAcquireTimeoutCause());

            firstRequest.cancel(true);
        } finally {
            client.close();
        }
    }

    private CompletableFuture<Void> executeAsync(SdkHttpClient client) {
        return CompletableFuture.supplyAsync(() -> {
            executeRequest(client);
            return null;
        });
    }

    private void executeRequest(SdkHttpClient client) {
        executeRequest(client, "");
    }

    private void executeRequest(SdkHttpClient client, String path) {
        URI uri = URI.create("http://localhost:" + mockServer.getPort() + path);
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(uri)
                                                       .method(SdkHttpMethod.POST)
                                                       .putHeader("Host", uri.getHost())
                                                       .putHeader("Content-Length", "4")
                                                       .contentStreamProvider(() -> new ByteArrayInputStream(
                                                           "Body".getBytes(StandardCharsets.UTF_8)))
                                                       .build();
        try {
            HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                                   .request(request)
                                                                                   .contentStreamProvider(
                                                                                       request.contentStreamProvider()
                                                                                              .orElse(null))
                                                                                   .build())
                                                 .call();
            response.responseBody().ifPresent(body -> {
                try {
                    while (body.read() != -1) {
                        // drain body so mid-body timeouts surface
                    }
                    body.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
