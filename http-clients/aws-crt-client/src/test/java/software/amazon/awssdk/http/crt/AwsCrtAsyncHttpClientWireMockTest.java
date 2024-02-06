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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.utils.AttributeMap;

public class AwsCrtAsyncHttpClientWireMockTest {
    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig()
                                                          .dynamicPort());

    @BeforeClass
    public static void setup() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @AfterClass
    public static void tearDown() {
        // Verify there is no resource leak.
        CrtResource.waitForNoResources();
    }

    @Test
    public void closeClient_reuse_throwException() {
        SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.create();

        client.close();
        assertThatThrownBy(() -> makeSimpleRequest(client)).hasMessageContaining("is closed");
    }

    @Test
    public void invalidProtocol_shouldThrowException() {
        AttributeMap attributeMap = AttributeMap.builder()
                                                .put(PROTOCOL, Protocol.HTTP2)
                                                .build();
        assertThatThrownBy(() -> AwsCrtAsyncHttpClient.builder().buildWithDefaults(attributeMap))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void sendRequest_withCollector_shouldCollectMetrics() throws Exception {

        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(10).build()) {
            RecordingResponseHandler recorder = makeSimpleRequest(client);
            MetricCollection metrics = recorder.collector().collect();

            assertThat(metrics.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("AwsCommonRuntime");
            assertThat(metrics.metricValues(HttpMetric.MAX_CONCURRENCY)).containsExactly(10);
            assertThat(metrics.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES)).containsExactly(0);
            assertThat(metrics.metricValues(HttpMetric.LEASED_CONCURRENCY)).containsExactly(1);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);
        }
    }

    @Test
    public void sharedEventLoopGroup_closeOneClient_shouldNotAffectOtherClients() throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.create()) {
            makeSimpleRequest(client);
        }

        try (SdkAsyncHttpClient anotherClient = AwsCrtAsyncHttpClient.create()) {
            makeSimpleRequest(anotherClient);
        }
    }

    /**
     * Make a simple async request and wait for it to finish.
     *
     * @param client Client to make request with.
     */
    private RecordingResponseHandler makeSimpleRequest(SdkAsyncHttpClient client) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody(body)));
        SdkHttpRequest request = createRequest(uri);
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.execute(AsyncExecuteRequest.builder()
                                          .request(request)
                                          .requestContentPublisher(createProvider(""))
                                          .responseHandler(recorder)
                                          .metricCollector(recorder.collector())
                                          .build());
        recorder.completeFuture().get(5, TimeUnit.SECONDS);
        return recorder;
    }
}
