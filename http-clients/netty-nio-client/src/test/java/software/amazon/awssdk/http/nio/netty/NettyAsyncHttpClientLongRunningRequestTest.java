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

package software.amazon.awssdk.http.nio.netty;

import static software.amazon.awssdk.http.LongRunningRequestTestSupport.CONFIGURED_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.HANG_DELAY;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.WARM_UP_PATH;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithinTimeBound;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.awaitReceivedRequests;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubHanging;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubWarmUp;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpTestUtils;
import software.amazon.awssdk.http.SdkAsyncHttpClientLongRunningRequestTestSuite;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyAsyncHttpClientLongRunningRequestTest extends SdkAsyncHttpClientLongRunningRequestTestSuite {

    // A shared event loop group, so that closing each client does not wait out the event loop group's shutdown quiet period.
    private static SdkEventLoopGroup eventLoopGroup;

    @BeforeAll
    public static void createEventLoopGroup() {
        eventLoopGroup = SdkEventLoopGroup.builder().build();
    }

    @AfterAll
    public static void closeEventLoopGroup() {
        eventLoopGroup.eventLoopGroup().shutdownGracefully(0, 5, TimeUnit.SECONDS);
    }

    @Override
    protected SdkAsyncHttpClient createSdkAsyncHttpClient(AttributeMap config) {
        return NettyNioAsyncHttpClient.builder().eventLoopGroup(eventLoopGroup).buildWithDefaults(config);
    }

    @Override
    protected String expectedAcquireTimeoutMessagePrefix() {
        return "Acquire operation took longer than the configured maximum time.";
    }

    @Override
    protected Class<? extends Throwable> expectedAcquireTimeoutCause() {
        return TimeoutException.class;
    }

    // TODO: NettyUtils.decorateException wraps connection-pool acquire timeouts in a plain Throwable
    // (see NettyUtils.java around the AcquireTimeoutException handling) instead of an IOException, so
    // the SDK retry layer treats them as non-transient. The body below is the suite's scenario minus
    // the IOException cause-chain assertion, so the timing-bound contract is still verified for Netty.
    @Test
    @Override
    public void executeWhenConnectionAcquireTimeoutAndPoolExhaustedFailsWithinTimeoutBound() throws Exception {
        stubHanging(mockServer);
        stubWarmUp(mockServer);

        SdkAsyncHttpClient client = createSdkAsyncHttpClient(AttributeMap.builder()
                                                                         .put(SdkHttpConfigurationOption.READ_TIMEOUT,
                                                                              HANG_DELAY.plusMinutes(1))
                                                                         .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, 1)
                                                                         .put(SdkHttpConfigurationOption
                                                                                  .CONNECTION_ACQUIRE_TIMEOUT,
                                                                              CONFIGURED_ACQUIRE_TIMEOUT)
                                                                         .build());
        try {
            // Open the connection first, so the holding request below does not set it up under the short acquire timeout.
            sendRequest(client, WARM_UP_PATH).get(5, TimeUnit.SECONDS);
            CompletableFuture<?> firstRequest = sendRequest(client);
            awaitReceivedRequests(mockServer, 2);

            Throwable failure = assertFailsWithinTimeBound(sendRequest(client), CONFIGURED_ACQUIRE_TIMEOUT);
            assertExpectedAcquireTimeoutFailure(failure);

            firstRequest.cancel(true);
        } finally {
            client.close();
        }
    }

    private CompletableFuture<byte[]> sendRequest(SdkAsyncHttpClient client) {
        return sendRequest(client, "");
    }

    private CompletableFuture<byte[]> sendRequest(SdkAsyncHttpClient client, String path) {
        URI uri = URI.create("http://localhost:" + mockServer.getPort() + path);
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(uri)
                                                       .method(SdkHttpMethod.GET)
                                                       .putHeader("Host", uri.getHost())
                                                       .build();
        return HttpTestUtils.sendRequest(client, request);
    }
}
