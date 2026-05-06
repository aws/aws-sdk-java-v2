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

package software.amazon.awssdk.http.apache5.internal.conn;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.pool.PoolStats;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SafePoolingHttpClientConnectionManagerTest {
    private static WireMockServer wm;

    @BeforeAll
    static void setup() {
        wm = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wm.start();
    }

    @AfterAll
    static void teardown() {
        wm.stop();
    }

    @Test
    void leaseInterrupted_doesNotLeakConnections_probabilistic() throws IOException, InterruptedException, ParseException {
        wm.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(204)));
        int rounds = 16;
        for (int i = 0; i < rounds; ++i) {
            leaseInterrupted_doesNotLeakConnections();
        }
    }

    private void leaseInterrupted_doesNotLeakConnections() throws InterruptedException, IOException, ParseException {
        int numRequests = 10_000;

        SafePoolingHttpClientConnectionManager connectionManager = SafePoolingHttpClientConnectionManagerBuilder.create().build();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(40);

        try (CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build()) {
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(100);

            try {
                List<Future<?>> futures = new ArrayList<>(numRequests);
                CountDownLatch go = new CountDownLatch(numRequests);

                for (int i = 0; i < numRequests; i++) {
                    Future<?> f =
                        executor.submit(
                            () -> {
                                go.countDown();
                                try {
                                    doRequest(httpClient);
                                    successCount.incrementAndGet();
                                } catch (Exception e) {
                                    errorCount.incrementAndGet();
                                }
                            });
                    futures.add(f);
                }

                go.await();

                for (Future<?> future : futures) {
                    future.cancel(true);
                }
            } finally {
                executor.shutdown();
                executor.awaitTermination(100, TimeUnit.SECONDS);
            }
            connectionManager.closeExpired();
            assertNoLeases(connectionManager);
            doRequest(httpClient);
        }
    }

    private static void doRequest(CloseableHttpClient httpClient) throws IOException, ParseException {
        HttpGet httpGet = new HttpGet("http://localhost:" + wm.port());
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                EntityUtils.toString(entity);
            }
        }
    }

    private static void assertNoLeases(SafePoolingHttpClientConnectionManager manager) {
        PoolStats totalStats = manager.getTotalStats();
        assertThat(totalStats.getLeased()).as("Leased connection count").isEqualTo(0);
    }
}
