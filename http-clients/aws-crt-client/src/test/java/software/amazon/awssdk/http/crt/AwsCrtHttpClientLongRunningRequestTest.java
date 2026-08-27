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

package software.amazon.awssdk.http.crt;

import static software.amazon.awssdk.http.LongRunningRequestTestSupport.CONFIGURED_TIMEOUT;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithIoExceptionWithinTimeBound;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubLongPolling;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubStreamingWithPauses;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientLongRunningRequestTestSuite;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.AttributeMap;

public class AwsCrtHttpClientLongRunningRequestTest extends SdkHttpClientLongRunningRequestTestSuite {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @Override
    protected SdkHttpClient createSdkHttpClient(AttributeMap config) {
        return AwsCrtHttpClient.builder().buildWithDefaults(config);
    }

    // The CRT client enforces a read/write inactivity timeout through SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT rather than
    // READ_TIMEOUT (which it ignores), so these two suite cases are re-implemented to configure that option. A stalled or
    // paused response then trips the CRT throughput monitor (AWS_ERROR_HTTP_CHANNEL_THROUGHPUT_FAILURE, a retryable
    // IOException) and fails the request within the bound instead of hanging.
    @Test
    @Override
    public void executeWhenReadTimeoutAndServerDelaysResponseFailsWithinTimeoutBound() {
        stubLongPolling(mockServer);
        SdkHttpClient client = createSdkHttpClient(fallbackTimeoutConfig());
        try {
            assertFailsWithIoExceptionWithinTimeBound(executeAsync(client), CONFIGURED_TIMEOUT);
        } finally {
            client.close();
        }
    }

    @Test
    @Override
    public void executeWhenReadTimeoutAndStreamingResponsePausesFailsWithinTimeoutBound() {
        stubStreamingWithPauses(mockServer);
        SdkHttpClient client = createSdkHttpClient(fallbackTimeoutConfig());
        try {
            assertFailsWithIoExceptionWithinTimeBound(executeAsync(client), CONFIGURED_TIMEOUT);
        } finally {
            client.close();
        }
    }

    private static AttributeMap fallbackTimeoutConfig() {
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT, CONFIGURED_TIMEOUT)
                           .build();
    }

    private CompletableFuture<Void> executeAsync(SdkHttpClient client) {
        return CompletableFuture.supplyAsync(() -> {
            executeRequest(client);
            return null;
        });
    }

    private void executeRequest(SdkHttpClient client) {
        URI uri = URI.create("http://localhost:" + mockServer.getPort());
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
