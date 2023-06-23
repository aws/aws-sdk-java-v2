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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
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
                                                                     .bindAddress("localhost").dynamicPort());

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @BeforeEach
    public void setup() {
        mockServer.start();
        stubPostRequest(".*", aResponse(), "{}");
    }

    @AfterAll
    public static void cleanUp(){
        executorService.shutdownNow();
    }

    @Test
    void connectionPoolsGetsReusedWhenInterruptedWith_1_MaxConnection() throws Exception {
        Integer responseDelay = 1500;

        String urlRegex = "/2016-03-11/allTypes";
        stubPostRequest(urlRegex, aResponse().withFixedDelay(responseDelay), SAMPLE_BODY);
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(1).build();
        ProtocolRestJsonClient client = getClient(httpClient, Duration.ofMillis(2L * responseDelay)).build();

        Future<?> toBeInterruptedFuture = executorService.submit(() -> client.allTypes());
        unInterruptedSleep(responseDelay - responseDelay / 5);
        toBeInterruptedFuture.cancel(true);
        // Make sure thread start the Http connections
        unInterruptedSleep(50);
        AllTypesResponse allTypesResponse = client.allTypes();
        assertThat(allTypesResponse.stringMember()).isEqualTo("resultString");
        executorService.shutdownNow();
    }

    @Test
    void interruptionWhenWaitingForLease_AbortsImmediately() throws InterruptedException {
        Integer responseDelay = 50000;
        ExceptionInThreadRun exceptionInThreadRun = new ExceptionInThreadRun();
        AtomicLong leaseWaitingTime = new AtomicLong(responseDelay);
        stubPostRequest("/2016-03-11/allTypes", aResponse().withFixedDelay(responseDelay), SAMPLE_BODY);
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(1).build();
        ProtocolRestJsonClient client = getClient(httpClient, Duration.ofMillis(2L * responseDelay)).build();
        executorService.submit(() -> client.allTypes());
        // 1 Sec sleep to make sure Thread 1 is picked for executing Http connection
        unInterruptedSleep(1000);
        Thread leaseWaitingThread = new Thread(() -> {

            try {
                client.allTypes(l -> l.overrideConfiguration(
                    b -> b
                        .addMetricPublisher(new MetricPublisher() {
                            @Override
                            public void publish(MetricCollection metricCollection) {
                                Optional<MetricRecord<?>> apiCallDuration =
                                    metricCollection.stream().filter(o -> "ApiCallDuration".equals(o.metric().name())).findAny();
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
        // 1 sec sleep to make sure Http connection execution is initialized for Thread 2 , in this case it will wait for lease
        // and immediately terminate on interrupt
        unInterruptedSleep(1000);
        leaseWaitingThread.interrupt();
        leaseWaitingThread.join();
        assertThat(leaseWaitingTime.get()).isNotEqualTo(responseDelay.longValue());
        assertThat(leaseWaitingTime.get()).isLessThan(responseDelay.longValue());
        assertThat(exceptionInThreadRun.getException()).isInstanceOf(AbortedException.class);
        client.close();
    }

    /**
     * Service Latency is set to high value say X.
     * Api timeout value id set to 1/3 of X.
     * And we interrupt the thread at 90% of X.
     * In this case since the ApiTimeOut first happened we should get ApiTimeOut Exception and not the interrupt.
     */
    @Test
    void interruptionDueToApiTimeOut_followed_byInterruptCausesOnlyTimeOutException() throws InterruptedException {
        SdkHttpClient httpClient = ApacheHttpClient.create();
        Integer responseDelay = 3000;
        stubPostRequest("/2016-03-11/allTypes", aResponse().withFixedDelay(responseDelay), SAMPLE_BODY);
        ExceptionInThreadRun exception = new ExceptionInThreadRun();
        ProtocolRestJsonClient client =
            getClient(httpClient, Duration.ofMillis(10))
                .overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.none()))
                .build();
        unInterruptedSleep(100);
        // We need to creat a separate thread to interrupt it externally.
        Thread leaseWaitingThread = new Thread(() -> {
            try {
                client.allTypes(l -> l.overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMillis(responseDelay / 3))));
            } catch (Exception e) {
                exception.setException(e);
            }
        });
        leaseWaitingThread.start();
        unInterruptedSleep(responseDelay - responseDelay / 10);
        leaseWaitingThread.interrupt();
        leaseWaitingThread.join();
        assertThat(exception.getException()).isInstanceOf(ApiCallAttemptTimeoutException.class);
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
