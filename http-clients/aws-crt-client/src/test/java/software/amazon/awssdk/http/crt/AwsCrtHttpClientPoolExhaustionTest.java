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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Tests that verify the CRT HTTP client's connection pool does not permanently
 * exhaust when requests are aborted (via timeout or explicit abort), and that
 * concurrency metrics are reported even on failed/timed-out requests.
 */
public class AwsCrtHttpClientPoolExhaustionTest {

    private static final int MAX_CONCURRENCY = 3;

    @RegisterExtension
    static WireMockExtension mockServer = WireMockExtension.newInstance()
                                                           .options(wireMockConfig().dynamicPort())
                                                           .build();

    private static ScheduledExecutorService scheduler;

    @BeforeAll
    static void setup() {
        scheduler = Executors.newScheduledThreadPool(2);
    }

    @AfterAll
    static void tearDown() {
        scheduler.shutdown();
    }

    /**
     * Verifies that after aborting all pool connections, the pool recovers and
     * can serve new requests.
     */
    @Test
    void syncClient_poolRecoversAfterRepeatedAborts(WireMockRuntimeInfo wm) throws Exception {
        URI uri = URI.create("http://localhost:" + wm.getHttpPort());

        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(MAX_CONCURRENCY).build()) {
            // Phase 1: Abort MAX_CONCURRENCY requests (exhausts all pool slots)
            stubSlowServer(2000);
            for (int i = 0; i < MAX_CONCURRENCY; i++) {
                ExecutableHttpRequest req = client.prepareRequest(syncRequest(uri, null));
                scheduler.schedule(req::abort, 50, TimeUnit.MILLISECONDS);
                try {
                    req.call();
                } catch (Exception e) {
                    // expected — aborted
                }
            }

            // Wait for cancel callbacks to propagate
            Thread.sleep(300);

            // Phase 2: Verify pool can serve new requests
            stubFastServer();
            int successCount = 0;
            for (int i = 0; i < MAX_CONCURRENCY; i++) {
                try {
                    client.prepareRequest(syncRequest(uri, null)).call();
                    successCount++;
                } catch (Exception e) {
                    // pool exhausted — connection leaked
                }
            }

            assertThat(successCount)
                .as("All %d requests should succeed after pool recovery (was %d)", MAX_CONCURRENCY, successCount)
                .isEqualTo(MAX_CONCURRENCY);
        }
    }

    /**
     * Verifies that after multiple rounds of aborts (more than maxConcurrency total),
     * the pool continues to function. This catches progressive leaks where each abort
     * leaks one slot.
     */
    @Test
    void syncClient_poolDoesNotProgressivelyLeak(WireMockRuntimeInfo wm) throws Exception {
        URI uri = URI.create("http://localhost:" + wm.getHttpPort());
        int totalAborts = MAX_CONCURRENCY * 3; // 9 aborts with pool size 3

        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(MAX_CONCURRENCY).build()) {
            stubSlowServer(2000);
            for (int i = 0; i < totalAborts; i++) {
                ExecutableHttpRequest req = client.prepareRequest(syncRequest(uri, null));
                scheduler.schedule(req::abort, 50, TimeUnit.MILLISECONDS);
                try {
                    req.call();
                } catch (Exception e) {
                    // expected
                }
            }

            Thread.sleep(300);

            // Verify pool still works after 9 aborts (3x the pool size)
            stubFastServer();
            int successCount = 0;
            for (int i = 0; i < MAX_CONCURRENCY; i++) {
                try {
                    client.prepareRequest(syncRequest(uri, null)).call();
                    successCount++;
                } catch (Exception e) {
                    // leaked
                }
            }

            assertThat(successCount).isEqualTo(MAX_CONCURRENCY);
        }
    }

    /**
     * Verifies that concurrency metrics (AvailableConcurrency, LeasedConcurrency,
     * MaxConcurrency, PendingConcurrencyAcquires) are reported even when the request
     * fails due to timeout/abort.
     */
    @Test
    void syncClient_metricsReportedOnAbortedRequest(WireMockRuntimeInfo wm) throws Exception {
        URI uri = URI.create("http://localhost:" + wm.getHttpPort());

        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(MAX_CONCURRENCY).build()) {
            stubSlowServer(2000);

            MetricCollector collector = MetricCollector.create("test");
            ExecutableHttpRequest req = client.prepareRequest(syncRequest(uri, collector));
            scheduler.schedule(req::abort, 50, TimeUnit.MILLISECONDS);
            try {
                req.call();
            } catch (Exception e) {
                // expected — aborted
            }

            Thread.sleep(200);

            MetricCollection metrics = collector.collect();

            // Recursively search the metric tree for MAX_CONCURRENCY
            boolean foundConcurrencyMetrics = hasMetricAnywhere(metrics, HttpMetric.MAX_CONCURRENCY);

            assertThat(foundConcurrencyMetrics)
                .as("Concurrency metrics should be reported even on aborted requests")
                .isTrue();
        }
    }

    private boolean hasMetricAnywhere(MetricCollection collection, software.amazon.awssdk.metrics.SdkMetric<?> metric) {
        if (!collection.metricValues(metric).isEmpty()) {
            return true;
        }
        for (MetricCollection child : collection.children()) {
            if (hasMetricAnywhere(child, metric)) {
                return true;
            }
        }
        return false;
    }

    private HttpExecuteRequest syncRequest(URI uri, MetricCollector collector) {
        HttpExecuteRequest.Builder builder = HttpExecuteRequest.builder()
                                                               .request(createRequest(uri))
                                                               .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]));
        if (collector != null) {
            builder.metricCollector(collector);
        }
        return builder.build();
    }

    private void stubSlowServer(int delayMs) {
        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(
            aResponse().withFixedDelay(delayMs).withBody("slow")));
    }

    private void stubFastServer() {
        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(
            aResponse().withStatus(200).withBody("OK")));
    }
}
