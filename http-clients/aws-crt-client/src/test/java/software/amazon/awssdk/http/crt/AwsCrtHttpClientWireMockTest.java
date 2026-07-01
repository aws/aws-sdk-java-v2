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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToIgnoreCase;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.PROTOCOL;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
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

    @Test
    public void putRequest_withInputStreamBody_serverReceivesBody() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.create()) {
            String body = "hello pull pump";
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(put(urlPathEqualTo("/sink")).willReturn(aResponse().withStatus(200)));

            SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                           .uri(uri)
                                                           .method(SdkHttpMethod.PUT)
                                                           .encodedPath("/sink")
                                                           .putHeader("Host", uri.getHost())
                                                           .putHeader("Content-Length", Integer.toString(bodyBytes.length))
                                                           .build();

            HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                                                                  .request(request)
                                                                  .contentStreamProvider(() -> new ByteArrayInputStream(bodyBytes))
                                                                  .build();

            HttpExecuteResponse response = client.prepareRequest(executeRequest).call();

            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
            verify(putRequestedFor(urlPathEqualTo("/sink"))
                       .withHeader("Content-Length", equalTo(Integer.toString(bodyBytes.length)))
                       .withRequestBody(equalToIgnoreCase(body)));
        }
    }

    @Test
    public void inputStreamThrows_connectionReturnedToPool_subsequentRequestSucceeds() throws Exception {
        // Bound the pool to a single connection: if the failed request leaks its connection, the
        // second call() either fails to acquire (with the explicit timeout below) or blocks until
        // the test framework times out. Either manifests as a deterministic failure rather than a hang.
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(put(urlPathEqualTo("/sink")).willReturn(aResponse().withStatus(200)));

            IOException expected = new IOException("simulated upstream failure");
            SdkHttpFullRequest failingRequest = SdkHttpFullRequest.builder()
                                                                  .uri(uri)
                                                                  .method(SdkHttpMethod.PUT)
                                                                  .encodedPath("/sink")
                                                                  .putHeader("Host", uri.getHost())
                                                                  .putHeader("Content-Length", "100")
                                                                  .build();
            HttpExecuteRequest failingExecute =
                HttpExecuteRequest.builder()
                                  .request(failingRequest)
                                  .contentStreamProvider(() -> new InputStream() {
                                      @Override
                                      public int read() throws IOException {
                                          throw expected;
                                      }

                                      @Override
                                      public int read(byte[] b, int off, int len) throws IOException {
                                          throw expected;
                                      }
                                  })
                                  .build();

            assertThatThrownBy(() -> client.prepareRequest(failingExecute).call())
                .isInstanceOf(IOException.class);

            // If the previous failure leaked the connection, this second call would fail to acquire
            // (bounded by the connectionAcquisitionTimeout configured above) instead of hanging.
            String body = "second request body";
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            SdkHttpFullRequest okRequest = SdkHttpFullRequest.builder()
                                                             .uri(uri)
                                                             .method(SdkHttpMethod.PUT)
                                                             .encodedPath("/sink")
                                                             .putHeader("Host", uri.getHost())
                                                             .putHeader("Content-Length", Integer.toString(bodyBytes.length))
                                                             .build();
            HttpExecuteRequest okExecute = HttpExecuteRequest.builder()
                                                              .request(okRequest)
                                                              .contentStreamProvider(() -> new ByteArrayInputStream(bodyBytes))
                                                              .build();

            HttpExecuteResponse response = client.prepareRequest(okExecute).call();
            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        }
    }

    @Test
    public void abortMidRequest_connectionReturnedToPool_subsequentRequestSucceeds() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withFixedDelay(2000).withBody("hello")));
            stubFor(put(urlPathEqualTo("/sink")).willReturn(aResponse().withStatus(200)));

            SdkHttpRequest delayedRequest = createRequest(uri);
            HttpExecuteRequest delayedExecute = HttpExecuteRequest.builder()
                                                                   .request(delayedRequest)
                                                                   .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                                   .build();

            ExecutableHttpRequest abortable = client.prepareRequest(delayedExecute);
            executorService.schedule(abortable::abort, 100, TimeUnit.MILLISECONDS);
            assertThatThrownBy(abortable::call).isInstanceOf(IOException.class).hasMessageContaining("cancelled");

            String body = "after abort";
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            SdkHttpFullRequest okRequest = SdkHttpFullRequest.builder()
                                                             .uri(uri)
                                                             .method(SdkHttpMethod.PUT)
                                                             .encodedPath("/sink")
                                                             .putHeader("Host", uri.getHost())
                                                             .putHeader("Content-Length", Integer.toString(bodyBytes.length))
                                                             .build();
            HttpExecuteRequest okExecute = HttpExecuteRequest.builder()
                                                              .request(okRequest)
                                                              .contentStreamProvider(() -> new ByteArrayInputStream(bodyBytes))
                                                              .build();
            HttpExecuteResponse response = client.prepareRequest(okExecute).call();
            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        }
    }

    @Test
    public void serverResetsConnection_connectionReturnedToPool_subsequentRequestSucceeds() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(put(urlPathEqualTo("/sink"))
                        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

            byte[] bodyBytes = randomAlphabetic(64).getBytes(StandardCharsets.UTF_8);
            SdkHttpFullRequest failingRequest = SdkHttpFullRequest.builder()
                                                                  .uri(uri)
                                                                  .method(SdkHttpMethod.PUT)
                                                                  .encodedPath("/sink")
                                                                  .putHeader("Host", uri.getHost())
                                                                  .putHeader("Content-Length", Integer.toString(bodyBytes.length))
                                                                  .build();
            HttpExecuteRequest failingExecute = HttpExecuteRequest.builder()
                                                                    .request(failingRequest)
                                                                    .contentStreamProvider(() -> new ByteArrayInputStream(bodyBytes))
                                                                    .build();

            assertThatThrownBy(() -> client.prepareRequest(failingExecute).call())
                .isInstanceOf(IOException.class);

            stubFor(put(urlPathEqualTo("/sink2")).willReturn(aResponse().withStatus(200)));
            byte[] okBytes = "ok".getBytes(StandardCharsets.UTF_8);
            SdkHttpFullRequest okRequest = SdkHttpFullRequest.builder()
                                                             .uri(uri)
                                                             .method(SdkHttpMethod.PUT)
                                                             .encodedPath("/sink2")
                                                             .putHeader("Host", uri.getHost())
                                                             .putHeader("Content-Length", Integer.toString(okBytes.length))
                                                             .build();
            HttpExecuteRequest okExecute = HttpExecuteRequest.builder()
                                                              .request(okRequest)
                                                              .contentStreamProvider(() -> new ByteArrayInputStream(okBytes))
                                                              .build();

            HttpExecuteResponse response = client.prepareRequest(okExecute).call();
            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        }
    }

    @Test
    public void interruptDuringCall_connectionReturnedToPool_subsequentRequestSucceeds() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withFixedDelay(2000).withBody("hello")));
            stubFor(put(urlPathEqualTo("/sink")).willReturn(aResponse().withStatus(200)));

            SdkHttpRequest delayedRequest = createRequest(uri);
            HttpExecuteRequest delayedExecute = HttpExecuteRequest.builder()
                                                                   .request(delayedRequest)
                                                                   .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                                   .build();

            CountDownLatch workerDone = new CountDownLatch(1);
            AtomicReference<Throwable> workerError = new AtomicReference<>();
            ExecutorService worker = Executors.newSingleThreadExecutor();
            try {
                Future<?> inFlight = worker.submit(() -> {
                    try {
                        client.prepareRequest(delayedExecute).call();
                    } catch (Throwable t) {
                        workerError.set(t);
                    } finally {
                        workerDone.countDown();
                    }
                });

                // Give call() time to enter joinInterruptibly() before we interrupt.
                Thread.sleep(100);
                inFlight.cancel(true);

                assertThat(workerDone.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(workerError.get())
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("cancelled");
            } finally {
                worker.shutdownNow();
                worker.awaitTermination(5, TimeUnit.SECONDS);
            }

            // If the interrupt leaked the connection, this second call() would block on acquire and fail
            // when connectionAcquisitionTimeout (10s above) elapses.
            String body = "after-interrupt";
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            SdkHttpFullRequest okRequest = SdkHttpFullRequest.builder()
                                                             .uri(uri)
                                                             .method(SdkHttpMethod.PUT)
                                                             .encodedPath("/sink")
                                                             .putHeader("Host", uri.getHost())
                                                             .putHeader("Content-Length", Integer.toString(bodyBytes.length))
                                                             .build();
            HttpExecuteRequest okExecute = HttpExecuteRequest.builder()
                                                              .request(okRequest)
                                                              .contentStreamProvider(() -> new ByteArrayInputStream(bodyBytes))
                                                              .build();
            HttpExecuteResponse response = client.prepareRequest(okExecute).call();
            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        }
    }

    @Test
    public void acquireTimeoutThenHolderCancelled_connectionReturnedToPool_subsequentRequestSucceeds() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(2))
                                                    .build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withFixedDelay(60_000).withBody("hello")));

            SdkHttpRequest holderRequest = createRequest(uri);
            HttpExecuteRequest holderExecute = HttpExecuteRequest.builder()
                                                                  .request(holderRequest)
                                                                  .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                                  .build();
            ExecutableHttpRequest holder = client.prepareRequest(holderExecute);

            SdkHttpRequest racerRequest = createRequest(uri);
            HttpExecuteRequest racerExecute = HttpExecuteRequest.builder()
                                                                 .request(racerRequest)
                                                                 .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                                 .build();

            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<HttpExecuteResponse> holderFuture = pool.submit(holder::call);
                // Give the holder time to acquire the only slot before the racer tries.
                Thread.sleep(500);

                Future<HttpExecuteResponse> racerFuture = pool.submit(() -> client.prepareRequest(racerExecute).call());
                // CRT surfaces the acquire-timeout as HttpException; CrtHttpRequest.call() rethrows
                // it directly (does not wrap in IOException).
                assertThatThrownBy(() -> racerFuture.get(5, TimeUnit.SECONDS))
                    .hasMessageContaining("acquire");

                // Release the slot via the same closeConnection path the other leak tests exercise.
                holder.abort();
                assertThatThrownBy(() -> holderFuture.get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(IOException.class);
            } finally {
                pool.shutdownNow();
                pool.awaitTermination(5, TimeUnit.SECONDS);
            }

            // If the slot didn't reclaim, this third call() blocks on acquire and fails when the
            // 2s connectionAcquisitionTimeout above elapses.
            stubFor(put(urlPathEqualTo("/sink")).willReturn(aResponse().withStatus(200)));
            byte[] okBytes = "ok".getBytes(StandardCharsets.UTF_8);
            SdkHttpFullRequest okRequest = SdkHttpFullRequest.builder()
                                                             .uri(uri)
                                                             .method(SdkHttpMethod.PUT)
                                                             .encodedPath("/sink")
                                                             .putHeader("Host", uri.getHost())
                                                             .putHeader("Content-Length", Integer.toString(okBytes.length))
                                                             .build();
            HttpExecuteRequest okExecute = HttpExecuteRequest.builder()
                                                              .request(okRequest)
                                                              .contentStreamProvider(() -> new ByteArrayInputStream(okBytes))
                                                              .build();

            HttpExecuteResponse response = client.prepareRequest(okExecute).call();
            assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        }
    }

    /**
     * Regression test for the deadlock the pull-pump fix addresses. On master, the request body's
     * {@code InputStream.read(...)} ran on the CRT event-loop thread (via the body callback), which
     * meant a body sourced from a {@code GET}'s {@code ResponseInputStream} on the same event loop
     * could deadlock: the GET held the event loop while the PUT body waited for it.
     *
     * <p>Pull-pump moves the read to the caller (sync) thread. This test verifies that load-bearing
     * claim by recording the thread that performs the body read and asserting it is the caller
     * thread - not a CRT event-loop thread. Failure of either the assertion or the test timeout
     * (a hang) is the deadlock signal.
     */
    @Test
    public void putBodyReadHappensOnCallerThread_notOnCrtEventLoop() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(10))
                                                    .build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(put(urlPathEqualTo("/sink")).willReturn(aResponse().withStatus(200)));

            byte[] bodyBytes = "body-on-caller".getBytes(StandardCharsets.UTF_8);
            AtomicReference<String> readThreadName = new AtomicReference<>();
            SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                           .uri(uri)
                                                           .method(SdkHttpMethod.PUT)
                                                           .encodedPath("/sink")
                                                           .putHeader("Host", uri.getHost())
                                                           .putHeader("Content-Length", Integer.toString(bodyBytes.length))
                                                           .build();
            HttpExecuteRequest executeRequest =
                HttpExecuteRequest.builder()
                                  .request(request)
                                  .contentStreamProvider(() -> new ByteArrayInputStream(bodyBytes) {
                                      @Override
                                      public synchronized int read(byte[] b, int off, int len) {
                                          readThreadName.compareAndSet(null, Thread.currentThread().getName());
                                          return super.read(b, off, len);
                                      }
                                  })
                                  .build();

            String callerThreadName = Thread.currentThread().getName();
            HttpExecuteResponse response = client.prepareRequest(executeRequest).call();
            assertThat(response.httpResponse().statusCode()).isEqualTo(200);

            String observed = readThreadName.get();
            assertThat(observed)
                .as("body read should happen on the caller thread, not the CRT event loop")
                .isNotNull()
                .isEqualTo(callerThreadName)
                .doesNotContainIgnoringCase("AwsEventLoop")
                .doesNotContainIgnoringCase("aws-event-loop");
        }
    }

    /**
     * Stress companion to {@link #putBodyReadHappensOnCallerThread_notOnCrtEventLoop}. Issues a
     * delayed GET (response delayed server-side) and a PUT in parallel through the same
     * {@code maxConcurrency(1)} client. On master, sequencing them through a single connection
     * with the body read tied to the event-loop thread could deadlock; here both calls must
     * complete within the test timeout.
     */
    @Test
    public void getInFlight_concurrentPut_bothComplete() throws Exception {
        try (SdkHttpClient client = AwsCrtHttpClient.builder()
                                                    .maxConcurrency(1)
                                                    .connectionAcquisitionTimeout(Duration.ofSeconds(15))
                                                    .build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(any(urlPathEqualTo("/slow"))
                        .willReturn(aResponse().withFixedDelay(2_000).withBody("hello")));
            stubFor(put(urlPathEqualTo("/sink")).willReturn(aResponse().withStatus(200)));

            SdkHttpRequest getRequest = SdkHttpFullRequest.builder()
                                                          .uri(uri)
                                                          .method(SdkHttpMethod.GET)
                                                          .encodedPath("/slow")
                                                          .putHeader("Host", uri.getHost())
                                                          .build();
            HttpExecuteRequest getExecute = HttpExecuteRequest.builder()
                                                              .request(getRequest)
                                                              .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                                              .build();

            byte[] putBytes = "put-body".getBytes(StandardCharsets.UTF_8);
            SdkHttpFullRequest putRequest = SdkHttpFullRequest.builder()
                                                              .uri(uri)
                                                              .method(SdkHttpMethod.PUT)
                                                              .encodedPath("/sink")
                                                              .putHeader("Host", uri.getHost())
                                                              .putHeader("Content-Length", Integer.toString(putBytes.length))
                                                              .build();
            HttpExecuteRequest putExecute = HttpExecuteRequest.builder()
                                                              .request(putRequest)
                                                              .contentStreamProvider(() -> new ByteArrayInputStream(putBytes))
                                                              .build();

            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Callable<HttpExecuteResponse> getTask = () -> client.prepareRequest(getExecute).call();
                Callable<HttpExecuteResponse> putTask = () -> client.prepareRequest(putExecute).call();
                Future<HttpExecuteResponse> getFuture = pool.submit(getTask);
                Future<HttpExecuteResponse> putFuture = pool.submit(putTask);

                HttpExecuteResponse getResponse = getFuture.get(15, TimeUnit.SECONDS);
                HttpExecuteResponse putResponse = putFuture.get(15, TimeUnit.SECONDS);
                assertThat(getResponse.httpResponse().statusCode()).isEqualTo(200);
                assertThat(putResponse.httpResponse().statusCode()).isEqualTo(200);
            } finally {
                pool.shutdownNow();
                pool.awaitTermination(5, TimeUnit.SECONDS);
            }
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
