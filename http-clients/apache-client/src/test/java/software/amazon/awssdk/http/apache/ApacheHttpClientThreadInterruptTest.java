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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.IoUtils;

public class ApacheHttpClientThreadInterruptTest {

    private final WireMockServer mockServer = new WireMockServer(new WireMockConfiguration()
                                                                     .dynamicPort()
                                                                     .dynamicHttpsPort());

    @BeforeEach
    public void setup() {
        mockServer.start();
        mockServer.stubFor(get(urlMatching(".*")).willReturn(aResponse()
                                                                 .withStatus(200).withBody("hello")));

    }

    @Test
    void connectionPoolsGetsReusedWhenInterrupted() throws Exception {

        Integer LONG_DELAY = 5000;
        mockServer.stubFor(get(urlMatching("/test/longDelay")).willReturn(aResponse().withFixedDelay(LONG_DELAY)
                                                                                     .withStatus(200).withBody("delay")));
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(3)
                                                   .connectionTimeout(Duration.ofMillis(1000))
                                                   .build();
        String urlIndex = "/test/longDelay";
        SdkHttpMethod httpMethod = SdkHttpMethod.GET;
        Runnable asyncHttpCall = () -> {
            try {
                HttpExecuteResponse httpExecuteResponse =
                    httpClient.prepareRequest(getSdkHttpRequest(urlIndex, httpMethod)).call();
                IoUtils.toUtf8String(httpExecuteResponse.responseBody().get());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(asyncHttpCall);
        executorService.submit(asyncHttpCall);
        Future<?> toBeInterruptedFuture = executorService.submit(asyncHttpCall);
        // Make sure thread start the Http connections
        Thread.sleep(100);
        toBeInterruptedFuture.cancel(true);
        HttpExecuteResponse httpExecuteResponse = httpClient.prepareRequest(getSdkHttpRequest(urlIndex, httpMethod)).call();
        String actualResult = IoUtils.toUtf8String(httpExecuteResponse.responseBody().get());
        assertThat(actualResult).isEqualTo("delay");
        executorService.shutdownNow();
    }

    @Test
    void timeOutOfTheHttpConnectionWhenLeasingIsCompletelyUsed() throws Exception {


        Integer LONG_DELAY = 5000;
        mockServer.stubFor(get(urlMatching("/test/longDelay")).willReturn(aResponse().withFixedDelay(LONG_DELAY)
                                                                                     .withStatus(200).withBody("hello")));
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(3)
                                                   .connectionTimeout(Duration.ofMillis(1000))
                                                   .build();
        int maxConnection = 3;
        String urlIndex = "/test/longDelay";
        SdkHttpMethod httpMethod = SdkHttpMethod.GET;
        Runnable asyncHttpCall = () -> {
            try {
                httpClient.prepareRequest(getSdkHttpRequest(urlIndex, httpMethod)).call();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(asyncHttpCall);
        executorService.submit(asyncHttpCall);
        executorService.submit(asyncHttpCall);

        // Make sure thread start the Http connections
        Thread.sleep(100);

        assertThatExceptionOfType(ConnectionPoolTimeoutException.class)
            .isThrownBy(() -> httpClient.prepareRequest(getSdkHttpRequest(urlIndex, httpMethod)).call())
            .withMessage("Timeout waiting for connection from pool");
    }


    @Test
    void interruptWhileWaitingOnLease() throws Exception {
        AtomicBoolean isRequestAborted = new AtomicBoolean(false);
        Integer LONG_DELAY = 3000;
        mockServer.stubFor(get(urlMatching("/test/longDelay")).willReturn(aResponse().withFixedDelay(LONG_DELAY)
                                                                                     .withStatus(200).withBody("delay")));
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(3)
                                                   .connectionTimeout(Duration.ofMillis(1000))
                                                   .build();
        String urlIndex = "/test/longDelay";
        SdkHttpMethod httpMethod = SdkHttpMethod.GET;
        // Three thread started that will consume the leasing concurrency.
        new Thread(() -> callHttpApiWithUrlIndex(httpClient, urlIndex, httpMethod)).start();
        new Thread(() -> callHttpApiWithUrlIndex(httpClient, urlIndex, httpMethod)).start();
        new Thread(() -> callHttpApiWithUrlIndex(httpClient, urlIndex, httpMethod)).start();
        // Sleep to ensure Http connections are leased and connection is established
        Thread.sleep(200);
        CompletableFuture.supplyAsync(() -> {
                             Thread thread = new Thread(() -> {
                                 try {
                                     httpClient.prepareRequest(getSdkHttpRequest(urlIndex, httpMethod)).call();
                                 } catch (IOException e) {
                                     e.printStackTrace();
                                     if (e instanceof RequestAbortedException) {
                                         isRequestAborted.set(true);
                                     }
                                 }
                             });
                             thread.start();

                             return thread;
                         }).thenAcceptAsync(thread ->
                                            {
                                                // Sleep to make sure The Http get call is in leasing state
                                                sleep(100L);
                                                thread.interrupt();
                                            })
                         .join();

        Thread.sleep(200);
        assertThat(isRequestAborted.get()).isTrue();
    }


    /**
     * 1. Start HTTP request in a separate thread say T1
     * 2. Start 2 more thread , make sure they read the Response body such that it is closed at the end.
     * 3. Start 4th Thread in main
     * 4. Interrupt the first thread after its execution  time
     * 5, The 4th Thread should be given the pool access and complete without error
     */
    @Test
    void interruptingAThreadWhichAlreadyFinishedHttpCall() throws Exception {
        AtomicBoolean isRequestAborted = new AtomicBoolean(false);
        Integer LONG_DELAY = 100;
        mockServer.stubFor(get(urlMatching("/test/longDelay")).willReturn(aResponse().withFixedDelay(LONG_DELAY)
                                                                                     .withStatus(200).withBody("delay")));
        SdkHttpClient httpClient = ApacheHttpClient.builder().maxConnections(3)
                                                   .connectionTimeout(Duration.ofMillis(3000))
                                                   .build();
        String urlIndex = "/test/longDelay";
        SdkHttpMethod httpMethod = SdkHttpMethod.GET;

        CompletableFuture<Void> interuptedAfteCompletion = CompletableFuture.supplyAsync(() -> {
            Thread thread = new Thread(() -> {
                try {
                    httpClient.prepareRequest(getSdkHttpRequest(urlIndex, httpMethod)).call();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (e instanceof RequestAbortedException) {
                        isRequestAborted.set(true);
                    }
                }
            });
            thread.start();

            return thread;
        }).thenAcceptAsync(thread -> {
            // Sleep to make sure The Http get call is in leasing state
            sleep(1000L);
            thread.interrupt();
        });
        Thread.sleep(10);
        // Three thread started that will consume the leasing concurrency.
        new Thread(() -> {
            HttpExecuteResponse httpExecuteResponse = callHttpApiWithUrlIndex(httpClient, urlIndex, httpMethod);
            try {
                IoUtils.toUtf8String(httpExecuteResponse.responseBody().get());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ).start();
        new Thread(() -> {
            HttpExecuteResponse httpExecuteResponse = callHttpApiWithUrlIndex(httpClient, urlIndex, httpMethod);
            try {
                IoUtils.toUtf8String(httpExecuteResponse.responseBody().get());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        ).start();


        Thread.sleep(10);
        HttpExecuteResponse httpExecuteResponse = callHttpApiWithUrlIndex(httpClient, urlIndex, httpMethod);
        String actualResult = IoUtils.toUtf8String(httpExecuteResponse.responseBody().get());
        assertThat(actualResult).isEqualTo("delay");
        Thread.sleep(200);
        interuptedAfteCompletion.join();
        assertThat(isRequestAborted.get()).isFalse();
    }

    /**
     *  Aborting the request twice should not cause Exception
     */
    void interuptRequestTwice()
    {


    }

    private HttpExecuteResponse callHttpApiWithUrlIndex(SdkHttpClient httpClient, String urlIndex, SdkHttpMethod httpMethod) {
        try {

            return httpClient.prepareRequest(getSdkHttpRequest(urlIndex, httpMethod)).call();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpExecuteRequest getSdkHttpRequest(String urlIndex, SdkHttpMethod httpMethod) {
        SdkHttpRequest sdkRequest = SdkHttpRequest.builder()
                                                  .uri(URI.create("http://localhost:" + mockServer.port() + urlIndex))
                                                  .method(httpMethod)
                                                  .build();

        return HttpExecuteRequest.builder().request(sdkRequest).build();
    }


    private void sleep(Long sleepInMillis) {
        try {
            Thread.sleep(sleepInMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
