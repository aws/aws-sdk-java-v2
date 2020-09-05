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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.AtLeast;
import org.mockito.internal.verification.Times;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.RecordingResponseHandler;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionReaperTest {
    private static final WiremockNetworkTrafficListener TRAFFIC_LISTENER = Mockito.mock(WiremockNetworkTrafficListener.class);

    @Rule
    public final WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort()
                                                                            .dynamicHttpsPort()
                                                                            .networkTrafficListener(TRAFFIC_LISTENER));

    @Before
    public void methodSetup() {
        reset(TRAFFIC_LISTENER);
    }

    @Test
    public void idleConnectionReaperDoesNotReapActiveConnections() throws InterruptedException {
        Duration maxIdleTime = Duration.ofSeconds(2);

        try(SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                               .connectionMaxIdleTime(maxIdleTime)
                                                               .buildWithDefaults(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS)) {
            Instant end = Instant.now().plus(maxIdleTime.plusSeconds(1));

            // Send requests for longer than the max-idle time, ensuring no connections are closed.
            while (Instant.now().isBefore(end)) {
                makeRequest(client);
                Thread.sleep(100);
                verify(TRAFFIC_LISTENER, new Times(0)).closed(any());
            }

            // Do nothing for longer than the max-idle time, ensuring connections are closed.
            Thread.sleep(maxIdleTime.plusSeconds(1).toMillis());

            verify(TRAFFIC_LISTENER, new AtLeast(1)).closed(any());
        }

    }

    @Test
    public void oldConnectionReaperReapsActiveConnections() throws InterruptedException {
        Duration connectionTtl = Duration.ofMillis(200);

        try (SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                                .connectionTimeToLive(connectionTtl)
                                                                .buildWithDefaults(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS)) {

            Instant end = Instant.now().plus(Duration.ofSeconds(5));

            verify(TRAFFIC_LISTENER, new Times(0)).closed(any());

            // Send requests frequently, validating that connections are still being closed.
            while (Instant.now().isBefore(end)) {
                makeRequest(client);
                Thread.sleep(100);
            }

            verify(TRAFFIC_LISTENER, new AtLeast(20)).closed(any());
        }
    }

    @Test
    public void noReapingWorks() throws InterruptedException {
        try (SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder()
                                                                .connectionMaxIdleTime(Duration.ofMillis(10))
                                                                .useIdleConnectionReaper(false)
                                                                .buildWithDefaults(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS)) {


            verify(TRAFFIC_LISTENER, new Times(0)).closed(any());
            makeRequest(client);

            Thread.sleep(2_000);

            verify(TRAFFIC_LISTENER, new Times(0)).closed(any());
        }
    }


    private void makeRequest(SdkAsyncHttpClient client) {
        stubFor(WireMock.any(urlPathEqualTo("/")).willReturn(aResponse().withBody(randomAlphabetic(10))));

        URI uri = URI.create("http://localhost:" + mockServer.port());
        client.execute(AsyncExecuteRequest.builder()
                                          .request(SdkHttpRequest.builder()
                                                                 .uri(uri)
                                                                 .method(SdkHttpMethod.GET)
                                                                 .encodedPath("/")
                                                                 .putHeader("Host", uri.getHost())
                                                                 .build())
                                          .requestContentPublisher(new EmptyPublisher())
                                          .responseHandler(new RecordingResponseHandler())
                                          .build())
              .join();
    }

}
