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

package software.amazon.awssdk.protocol.tests.connection;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

/**
 * Tests to verify Interruption of Threads while Http Connection is in progress to make sure Resources are released.
 */
class SyncClientConnectionInterruptionTest {
    public static final String SAMPLE_BODY = "{\"StringMember"
                                             + "\":\"resultString\"}";
    private final WireMockServer mockServer = new WireMockServer(new WireMockConfiguration()
                                                                     .bindAddress("localhost")
                                                                     .dynamicPort());
    @BeforeEach
    public void setup() {
        mockServer.start();
        stubPostRequest(".*", aResponse(), "{}");
    }

    @Test
    void connectionPoolsGetsReusedWhenInterruptedWith_1_MaxConnection() throws Exception {
        Integer LONG_DELAY = 1500;

        String urlRegex = "/2016-03-11/allTypes";
        stubPostRequest(urlRegex, aResponse().withFixedDelay(LONG_DELAY), SAMPLE_BODY);
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(1).build();
        ProtocolRestJsonClient client = getClient(httpClient, Duration.ofMillis(2L * LONG_DELAY)).build();

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Future<?> toBeInterruptedFuture = executorService.submit(() -> client.allTypes());
        unInterruptedSleep(LONG_DELAY - LONG_DELAY / 5);
        toBeInterruptedFuture.cancel(true);
        // Make sure thread start the Http connections
        unInterruptedSleep(50);
        AllTypesResponse allTypesResponse = client.allTypes();
        assertThat(allTypesResponse.stringMember()).isEqualTo("resultString");
        executorService.shutdownNow();
    }

    @Test
    void connectionPoolsGetsReusedWhenInterruptedWith_Multiple_MaxConnection() throws Exception {
        Integer LONG_DELAY = 1000;
        Integer VERY_VERY_LONG_DELAY = LONG_DELAY * 5;
        stubPostRequest("/2016-03-11/allTypes", aResponse().withFixedDelay(LONG_DELAY), SAMPLE_BODY);
        stubPostRequest("/2016-03-11/JsonValuesOperation", aResponse().withFixedDelay(VERY_VERY_LONG_DELAY), SAMPLE_BODY);
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(3).build();
        Duration timeOutDuration = Duration.ofMillis(2L * LONG_DELAY);
        ProtocolRestJsonClient client = getClient(httpClient, timeOutDuration).build();

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Future<?> toBeInterruptedFuture0 = executorService.submit(() -> client.allTypes());
        Future<?> toBeInterruptedFuture1 = executorService.submit(() -> client.allTypes());
        Future<?> toBeInterruptedFuture2 = executorService.submit(() -> client.allTypes());
        unInterruptedSleep(50);
        executorService.submit(() -> client.jsonValuesOperation());
        unInterruptedSleep(LONG_DELAY / 2);
        toBeInterruptedFuture0.cancel(true);
        toBeInterruptedFuture1.cancel(true);
        toBeInterruptedFuture2.cancel(true);
        unInterruptedSleep(LONG_DELAY / 2);
        // Make sure thread start the Http connections
        AllTypesResponse allTypesResponse = client.allTypes();
        assertThat(allTypesResponse.stringMember()).isEqualTo("resultString");
        executorService.shutdownNow();
    }

    @Test
    void interruptionWhenWaitingForLease_AbortsImmediately() throws InterruptedException {
        Integer LONG_DELAY = 5000;
        ExceptionInThreadRun exceptionInThreadRun = new ExceptionInThreadRun();
        AtomicLong leaseWaitingTime = new AtomicLong(LONG_DELAY);
        stubPostRequest("/2016-03-11/allTypes", aResponse().withFixedDelay(LONG_DELAY), SAMPLE_BODY);
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(1).build();
        ProtocolRestJsonClient client = getClient(httpClient, Duration.ofMillis(2L * LONG_DELAY)).build();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(() -> client.allTypes());
        unInterruptedSleep(100);
        Thread leaseWaitingThread = new Thread(() -> {

            try {
                client.allTypes(l -> l.overrideConfiguration(
                    b -> b
                        .apiCallAttemptTimeout(Duration.ofSeconds(10))
                        .addMetricPublisher(new MetricPublisher() {
                            @Override
                            public void publish(MetricCollection metricCollection) {
                                System.out.println(metricCollection);
                                Optional<MetricRecord<?>> apiCallDuration =
                                    metricCollection.stream().filter(o -> "ApiCallDuration" .equals(o.metric().name())).findAny();
                                leaseWaitingTime.set(Duration.parse(apiCallDuration.get().value().toString()).toMillis());
                            }

                            @Override
                            public void close() {
                            }
                        })
                ));

            } catch (Exception exception) {
                exceptionInThreadRun.setException(exception);

            }
        });

        leaseWaitingThread.start();
        unInterruptedSleep(100);
        leaseWaitingThread.interrupt();
        leaseWaitingThread.join();
        assertThat(leaseWaitingTime.get()).isNotEqualTo(LONG_DELAY.longValue());
        assertThat(leaseWaitingTime.get()).isLessThan(LONG_DELAY.longValue());
        assertThat(exceptionInThreadRun.getException()).isInstanceOf(AbortedException.class);
        client.close();
    }

    private static Stream<Arguments> httpClientImplementation() {
        return Stream.of(Arguments.of(ApacheHttpClient.create()),
                         Arguments.of(UrlConnectionHttpClient.create()));
    }

    /**
     * Service Latency is set to high value say X.
     * Api timeout value id set to 1/3 of X.
     * And we interrupt the thread at 90% of X.
     * In this case since the ApiTimeOut first happened we should get ApiTimeOut Exception and not the interrupt.
     */
    @ParameterizedTest
    @MethodSource("httpClientImplementation")
    void interruptionDueToApiTimeOut_followed_byInterruptCausesOnlyTimeOutException(SdkHttpClient httpClient) throws InterruptedException {
        Integer SERVER_RESPONSE_DELAY = 3000;
        stubPostRequest("/2016-03-11/allTypes", aResponse().withFixedDelay(SERVER_RESPONSE_DELAY), SAMPLE_BODY);
        ExceptionInThreadRun exception = new ExceptionInThreadRun();
        ProtocolRestJsonClient client =
            getClient(httpClient, Duration.ofMillis(10)).overrideConfiguration(o -> o.retryPolicy(RetryPolicy.none())).build();
        unInterruptedSleep(100);
        // We need to creat a separate thread to interrupt it externally.
        Thread leaseWaitingThread = new Thread(() -> {
            try {
                client.allTypes(l -> l.overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMillis(SERVER_RESPONSE_DELAY / 3))));
            } catch (Exception e) {
                exception.setException(e);
            }
        });
        leaseWaitingThread.start();
        unInterruptedSleep(SERVER_RESPONSE_DELAY - SERVER_RESPONSE_DELAY / 10);
        leaseWaitingThread.interrupt();
        leaseWaitingThread.join();
        assertThat(exception.getException()).isInstanceOf(ApiCallAttemptTimeoutException.class);
        client.close();
    }

    @ParameterizedTest
    @MethodSource("httpClientImplementation")
    void sdkClientInterrupted_while_connectionIsInProgress(SdkHttpClient httpClient) throws InterruptedException {
        Integer SERVER_RESPONSE_DELAY = 3000;
        stubPostRequest("/2016-03-11/allTypes", aResponse().withFixedDelay(SERVER_RESPONSE_DELAY), SAMPLE_BODY);
        ExceptionInThreadRun exception = new ExceptionInThreadRun();
        ProtocolRestJsonClient client =
            getClient(httpClient, Duration.ofMillis(10)).overrideConfiguration(o -> o.retryPolicy(RetryPolicy.none())).build();
        unInterruptedSleep(100);
        // We need to creat a separate thread to interrupt it externally.
        Thread leaseWaitingThread = new Thread(() -> {
            try {
                client.allTypes(l -> l.overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMillis(SERVER_RESPONSE_DELAY * 3))));
            } catch (Exception e) {
                exception.setException(e);
            }
        });
        leaseWaitingThread.start();
        unInterruptedSleep(SERVER_RESPONSE_DELAY - SERVER_RESPONSE_DELAY / 10);
        leaseWaitingThread.interrupt();
        leaseWaitingThread.join();
        assertThat(exception.getException()).isInstanceOf(AbortedException.class);
        client.close();
    }

    private class ExceptionInThreadRun {
        private Exception exception;
        public Exception getException() {
            return exception;
        }
        public void setException(Exception exception) {
            this.exception = exception;
        }
    }

    static void unInterruptedSleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException("This test sleep is not be interrupted");
        }
    }

    private void stubPostRequest(String urlRegex, ResponseDefinitionBuilder LONG_DELAY, String body) {
        mockServer.stubFor(post(urlMatching(urlRegex))
                               .willReturn(LONG_DELAY
                                               .withStatus(200)
                                               .withBody(body)));
    }
    private ProtocolRestJsonClientBuilder getClient(SdkHttpClient httpClient, Duration timeOutDuration) {
        return ProtocolRestJsonClient.builder()
                                                              .credentialsProvider(
                                                                  StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                              .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                                              .httpClient(httpClient)
                                                              .overrideConfiguration(o -> o.apiCallTimeout(timeOutDuration));

    }
}
