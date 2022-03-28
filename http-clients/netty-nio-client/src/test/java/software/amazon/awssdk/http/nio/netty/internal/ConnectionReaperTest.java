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

package software.amazon.awssdk.http.nio.netty.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.AtLeast;
import org.mockito.internal.verification.Times;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.http.ConnectionCountingTrafficListener;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.RecordingResponseHandler;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionReaperTest {
    private static final ConnectionCountingTrafficListener TRAFFIC_LISTENER = new ConnectionCountingTrafficListener();

    @Rule
    public final WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort()
                                                                            .dynamicHttpsPort()
                                                                            .networkTrafficListener(TRAFFIC_LISTENER));

    @Test
    public void idleConnectionReaperDoesNotReapActiveConnections() throws InterruptedException {
        Duration maxIdleTime = Duration.ofSeconds(2);

        try(SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                               .connectionMaxIdleTime(maxIdleTime)
                                                               .buildWithDefaults(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS)) {
            Instant end = Instant.now().plus(maxIdleTime.plusSeconds(1));

            // Send requests for longer than the max-idle time, ensuring no connections are closed.
            int connectionCount = TRAFFIC_LISTENER.openedConnections();
            while (Instant.now().isBefore(end)) {
                makeRequest(client);
                Thread.sleep(100);
            }

            assertThat(TRAFFIC_LISTENER.openedConnections()).isEqualTo(connectionCount + 1);

            // Do nothing for longer than the max-idle time, ensuring connections are closed.
            Thread.sleep(maxIdleTime.plusSeconds(1).toMillis());

            makeRequest(client);
            assertThat(TRAFFIC_LISTENER.openedConnections()).isEqualTo(connectionCount + 2);
        }

    }

    @Test
    public void oldConnectionReaperReapsActiveConnections() throws InterruptedException {
        Duration connectionTtl = Duration.ofMillis(200);

        try (SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                                .connectionTimeToLive(connectionTtl)
                                                                .buildWithDefaults(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS)) {

            Instant end = Instant.now().plus(Duration.ofSeconds(5));

            int connectionCount = TRAFFIC_LISTENER.openedConnections();

            // Send requests frequently, validating that new connections are being opened.
            while (Instant.now().isBefore(end)) {
                makeRequest(client);
                Thread.sleep(100);
            }

            assertThat(TRAFFIC_LISTENER.openedConnections()).isGreaterThanOrEqualTo(connectionCount + 15);
        }
    }

    @Test
    public void noReapingWorks() throws InterruptedException {
        try (SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                                .connectionMaxIdleTime(Duration.ofMillis(10))
                                                                .useIdleConnectionReaper(false)
                                                                .buildWithDefaults(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS)) {


            int connectionCount = TRAFFIC_LISTENER.openedConnections();
            makeRequest(client);
            Thread.sleep(2_000);
            makeRequest(client);
            assertThat(TRAFFIC_LISTENER.openedConnections()).isEqualTo(connectionCount + 1);
        }
    }


    private void makeRequest(SdkAsyncHttpClient client) {
        stubFor(WireMock.any(anyUrl()).willReturn(aResponse().withBody(randomAlphabetic(10))));

        RecordingResponseHandler handler = new RecordingResponseHandler();

        URI uri = URI.create("http://localhost:" + mockServer.port());
        client.execute(AsyncExecuteRequest.builder()
                                          .request(SdkHttpRequest.builder()
                                                                 .uri(uri)
                                                                 .method(SdkHttpMethod.GET)
                                                                 .encodedPath("/")
                                                                 .putHeader("Host", uri.getHost())
                                                                 .putHeader("Content-Length", "0")
                                                                 .build())
                                          .requestContentPublisher(new EmptyPublisher())
                                          .responseHandler(handler)
                                          .build())
              .join();

        assertThat(handler.fullResponseAsString()).hasSize(10);
    }

}
