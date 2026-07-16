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
import java.util.concurrent.CompletableFuture;
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
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Verifies connection pool behavior when requests are aborted in between.
 */
public class AwsCrtHttpClientAbortBehaviorTest {

    @RegisterExtension
    static WireMockExtension mockServer = WireMockExtension.newInstance()
                                                           .options(wireMockConfig().dynamicPort())
                                                           .build();

    private static ScheduledExecutorService scheduler;

    @BeforeAll
    static void setup() {
        scheduler = Executors.newScheduledThreadPool(1);
    }

    @AfterAll
    static void tearDown() {
        scheduler.shutdown();
    }

    /**
     * Verifies that aborting in-flight requests evicts connections from the pool —
     * the next request succeeds and LEASED_CONCURRENCY is 1.
     */
    @Test
    void syncClient_whenRequestAborted_connectionIsEvictedFromPool(WireMockRuntimeInfo wm) throws Exception {
        URI uri = URI.create("http://localhost:" + wm.getHttpPort());

        try (SdkHttpClient client = AwsCrtHttpClient.builder().maxConcurrency(3).build()) {
            stubUnresponsiveServer();
            executeAndAbort(client, uri, 3);

            // allow cancel() callbacks to complete before asserting pool state
            Thread.sleep(200);

            stubResponsiveServer();
            int successCount = 0;
            MetricCollector collector = MetricCollector.create("test");
            for (int i = 0; i < 3; i++) {
                try {
                    client.prepareRequest(syncRequest(uri, i == 0 ? collector : null)).call();
                    successCount++;
                } catch (Exception e) {
                    // connection not evicted
                }
            }

            assertThat(successCount).as("%d/%d requests succeeded after aborts", successCount, 3).isEqualTo(3);
            assertThat(collector.collect().metricValues(HttpMetric.LEASED_CONCURRENCY))
                .as("LEASED_CONCURRENCY must be 1 after aborts, not %d", 4)
                .containsExactly(1);
        }
    }

    /**
     * Verifies that when an async request future completes exceptionally, the connection is
     * evicted from the pool and LEASED_CONCURRENCY is 1 for the next request.
     */
    @Test
    void asyncClient_whenRequestAborted_connectionIsEvictedFromPool(WireMockRuntimeInfo wm) throws Exception {
        URI uri = URI.create("http://localhost:" + wm.getHttpPort());

        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(3).build()) {
            stubUnresponsiveServer();
            for (int i = 0; i < 3; i++) {
                RecordingResponseHandler recorder = new RecordingResponseHandler();
                CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder()
                                                                                   .request(createRequest(uri))
                                                                                   .requestContentPublisher(createProvider(""))
                                                                                   .responseHandler(recorder)
                                                                                   .build());
                // abort() equivalent for async: complete the future exceptionally after stream is acquired
                scheduler.schedule(() -> future.completeExceptionally(new RuntimeException("timeout")),
                                   100, TimeUnit.MILLISECONDS);
                try {
                    future.get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // expected
                }
                // wait for the response handler to finish so cancel() has completed before next iteration
                try {
                    recorder.completeFuture().get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // expected — handler receives the error
                }
            }

            stubResponsiveServer();
            MetricCollector collector = MetricCollector.create("test");
            RecordingResponseHandler recorder = new RecordingResponseHandler();
            client.execute(AsyncExecuteRequest.builder()
                                              .request(createRequest(uri))
                                              .requestContentPublisher(createProvider(""))
                                              .responseHandler(recorder)
                                              .metricCollector(collector)
                                              .build());
            recorder.completeFuture().get(5, TimeUnit.SECONDS);

            assertThat(collector.collect().metricValues(HttpMetric.LEASED_CONCURRENCY))
                .as("LEASED_CONCURRENCY must be 1 after exceptionally-completed futures, not %d", 4)
                .containsExactly(1);
        }
    }

    private void executeAndAbort(SdkHttpClient client, URI uri, int count) {
        for (int i = 0; i < count; i++) {
            ExecutableHttpRequest req = client.prepareRequest(syncRequest(uri, null));
            // abort() must be called from another thread while call() is blocking
            scheduler.schedule(req::abort, 100, TimeUnit.MILLISECONDS);
            try {
                req.call();
            } catch (Exception e) {
                // expected — aborted
            }
        }
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

    private void stubUnresponsiveServer() {
        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withFixedDelay(5000).withBody("slow")));
    }

    private void stubResponsiveServer() {
        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("OK")));
    }
}
