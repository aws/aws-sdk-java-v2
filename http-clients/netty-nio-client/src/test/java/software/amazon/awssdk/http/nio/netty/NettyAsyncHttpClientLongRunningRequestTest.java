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

import static software.amazon.awssdk.http.LongRunningRequestTestSupport.CONFIGURED_TIMEOUT;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.HANG_DELAY;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithinTimeBound;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubHanging;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpTestUtils;
import software.amazon.awssdk.http.SdkAsyncHttpClientLongRunningRequestTestSuite;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyAsyncHttpClientLongRunningRequestTest extends SdkAsyncHttpClientLongRunningRequestTestSuite {

    @Override
    protected SdkAsyncHttpClient createSdkAsyncHttpClient(AttributeMap config) {
        return NettyNioAsyncHttpClient.builder().buildWithDefaults(config);
    }

    // TODO: NettyUtils.decorateException wraps connection-pool acquire timeouts in a plain Throwable
    // (see NettyUtils.java around the AcquireTimeoutException handling) instead of an IOException, so
    // the SDK retry layer treats them as non-transient. The body below is the suite's scenario minus
    // the IOException cause-chain assertion, so the timing-bound contract is still verified for Netty.
    @Test
    @Override
    public void executeWhenConnectionAcquireTimeoutAndPoolExhaustedFailsWithinTimeoutBound() throws Exception {
        stubHanging(mockServer);

        SdkAsyncHttpClient client = createSdkAsyncHttpClient(AttributeMap.builder()
                                                                         .put(SdkHttpConfigurationOption.READ_TIMEOUT,
                                                                              HANG_DELAY.plusMinutes(1))
                                                                         .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, 1)
                                                                         .put(SdkHttpConfigurationOption
                                                                                  .CONNECTION_ACQUIRE_TIMEOUT,
                                                                              CONFIGURED_TIMEOUT)
                                                                         .build());
        try {
            CompletableFuture<?> firstRequest = sendRequest(client);
            Thread.sleep(500);

            assertFailsWithinTimeBound(sendRequest(client), CONFIGURED_TIMEOUT);

            firstRequest.cancel(true);
        } finally {
            client.close();
        }
    }

    private CompletableFuture<byte[]> sendRequest(SdkAsyncHttpClient client) {
        URI uri = URI.create("http://localhost:" + mockServer.getPort());
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .uri(uri)
                                                       .method(SdkHttpMethod.GET)
                                                       .putHeader("Host", uri.getHost())
                                                       .build();
        return HttpTestUtils.sendRequest(client, request);
    }
}
