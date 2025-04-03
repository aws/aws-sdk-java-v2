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
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.AttributeMap;

public class AwsCrtHttpClientWireMockTest {
    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig()
                                                          .dynamicPort());

    private static ScheduledExecutorService executorService;

    @BeforeClass
    public static void setup() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
        executorService = Executors.newScheduledThreadPool(1);
    }

    @AfterClass
    public static void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void closeClient_reuse_throwException() {
        SdkHttpClient client = AwsCrtHttpClient.create();

        client.close();
        assertThatThrownBy(() -> makeSimpleRequest(client, null)).hasMessageContaining("is closed");
    }

    @Test
    public void invalidProtocol_shouldThrowException() {
        AttributeMap attributeMap = AttributeMap.builder()
                                                .put(PROTOCOL, Protocol.HTTP2)
                                                .build();
        assertThatThrownBy(() -> AwsCrtHttpClient.builder().buildWithDefaults(attributeMap))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void sendRequest_withCollector_shouldCollectMetrics() throws Exception {

        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(10).build()) {
            MetricCollector collector = MetricCollector.create("test");
            makeSimpleRequest(client, collector);
            MetricCollection metrics = collector.collect();

            assertThat(metrics.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("AwsCommonRuntime");
            assertThat(metrics.metricValues(HttpMetric.MAX_CONCURRENCY)).containsExactly(10);
            assertThat(metrics.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES)).containsExactly(0);
            assertThat(metrics.metricValues(HttpMetric.LEASED_CONCURRENCY)).containsExactly(1);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);
        }
    }

    @Test
    public void sharedEventLoopGroup_closeOneClient_shouldNotAffectOtherClients() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.create()) {
            makeSimpleRequest(client, null);
        }

        try (SdkHttpClient anotherClient = AwsCrtHttpClient.create()) {
            makeSimpleRequest(anotherClient, null);
        }
    }

    @Test
    public void abortRequest_shouldFailTheExceptionWithIOException() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.create()) {
            String body = randomAlphabetic(10);
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withFixedDelay(1000).withBody(body)));
            SdkHttpRequest request = createRequest(uri);

            HttpExecuteRequest.Builder executeRequestBuilder = HttpExecuteRequest.builder();
            executeRequestBuilder.request(request)
                                 .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]));

            ExecutableHttpRequest executableRequest = client.prepareRequest(executeRequestBuilder.build());
            executorService.schedule(() -> executableRequest.abort(), 100, TimeUnit.MILLISECONDS);
                executableRequest.abort();
            assertThatThrownBy(() -> executableRequest.call()).isInstanceOf(IOException.class)
                .hasMessageContaining("cancelled");
        }
    }

    /**
     * Make a simple request and wait for it to finish.
     *
     * @param client Client to make request with.
     */
    private HttpExecuteResponse makeSimpleRequest(SdkHttpClient client, MetricCollector metricCollector) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody(body)));
        SdkHttpRequest request = createRequest(uri);

        HttpExecuteRequest.Builder executeRequestBuilder = HttpExecuteRequest.builder();
        executeRequestBuilder.request(request)
                             .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                             .metricCollector(metricCollector);
        ExecutableHttpRequest executableRequest = client.prepareRequest(executeRequestBuilder.build());
        return executableRequest.call();
    }
}
