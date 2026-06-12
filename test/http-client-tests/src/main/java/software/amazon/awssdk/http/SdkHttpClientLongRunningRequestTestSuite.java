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
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.CONFIGURED_TIMEOUT;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.HANG_DELAY;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.assertFailsWithinTimeBound;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.executeAsync;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubHanging;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubLongPolling;
import static software.amazon.awssdk.http.LongRunningRequestTestSupport.stubStreamingWithPauses;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.http.LongRunningRequestTestSupport.TestRequestExecution;
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

    @Test
    public void executeWhenReadTimeoutAndServerDelaysResponseFailsWithinTimeoutBound() {
        stubLongPolling(mockServer);

        SdkHttpClient client = createSdkHttpClient(AttributeMap.builder()
                                                               .put(SdkHttpConfigurationOption.READ_TIMEOUT,
                                                                    CONFIGURED_TIMEOUT)
                                                               .build());
        try {
            assertFailsWithinTimeBound(executeAsync(client, mockServer), CONFIGURED_TIMEOUT);
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
            assertFailsWithinTimeBound(executeAsync(client, mockServer), CONFIGURED_TIMEOUT);
        } finally {
            client.close();
        }
    }

    @Test
    public void executeWhenConnectionAcquireTimeoutAndPoolExhaustedFailsWithinTimeoutBound() throws Exception {
        stubHanging(mockServer);

        SdkHttpClient client = createSdkHttpClient(AttributeMap.builder()
                                                               .put(SdkHttpConfigurationOption.READ_TIMEOUT,
                                                                    HANG_DELAY.plusMinutes(1))
                                                               .put(SdkHttpConfigurationOption.MAX_CONNECTIONS, 1)
                                                               .put(SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT,
                                                                    CONFIGURED_TIMEOUT)
                                                               .build());
        try {
            TestRequestExecution firstRequest = executeAsync(client, mockServer);
            Thread.sleep(500);

            assertFailsWithinTimeBound(executeAsync(client, mockServer), CONFIGURED_TIMEOUT);

            firstRequest.future().cancel(true);
        } finally {
            client.close();
        }
    }
}
