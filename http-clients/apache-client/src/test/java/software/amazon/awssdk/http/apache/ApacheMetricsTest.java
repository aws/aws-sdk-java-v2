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

package software.amazon.awssdk.http.apache;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.HttpMetric.CONCURRENCY_ACQUIRE_DURATION;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;


public class ApacheMetricsTest {
    private static WireMockServer wireMockServer;
    private SdkHttpClient client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws IOException {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
    }

    @Before
    public void methodSetup() {
        wireMockServer.stubFor(any(urlMatching(".*")).willReturn(aResponse().withStatus(200).withBody("{}")));
    }

    @AfterClass
    public static void teardown() throws IOException {
        wireMockServer.stop();
    }

    @After
    public void methodTeardown() {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test
    public void concurrencyAcquireDurationIsRecorded() throws IOException {
        client = ApacheHttpClient.create();
        MetricCollector collector = MetricCollector.create("test");
        makeRequestWithMetrics(client, collector);

        MetricCollection collection = collector.collect();

        assertThat(collection.metricValues(CONCURRENCY_ACQUIRE_DURATION)).isNotEmpty();
    }

    private HttpExecuteResponse makeRequestWithMetrics(SdkHttpClient httpClient, MetricCollector metricCollector) throws IOException {
        SdkHttpRequest httpRequest = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .protocol("http")
                                                       .host("localhost:" + wireMockServer.port())
                                                       .build();

        HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                       .request(httpRequest)
                                                       .metricCollector(metricCollector)
                                                       .build();

        return httpClient.prepareRequest(request).call();
    }
}
