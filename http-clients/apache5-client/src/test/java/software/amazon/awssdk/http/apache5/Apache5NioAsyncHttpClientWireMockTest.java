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

package software.amazon.awssdk.http.apache5;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.AttributeMap;

public class Apache5NioAsyncHttpClientWireMockTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort());

    private static SdkAsyncHttpClient client;

    @BeforeClass
    public static void setup() {
        client = Apache5NioAsyncHttpClient.builder()
                                          .buildWithDefaults(AttributeMap.builder()
                                                                         .put(TRUST_ALL_CERTIFICATES, true)
                                                                         .build());
    }

    @AfterClass
    public static void teardown() {
        client.close();
    }

    @Test
    public void simpleGetRequest_returns200() throws Exception {
        stubFor(any(urlPathEqualTo("/test"))
                    .willReturn(aResponse().withStatus(200).withBody("hello")));

        AtomicInteger statusCode = new AtomicInteger();
        AtomicReference<String> body = new AtomicReference<>("");
        CompletableFuture<Void> future = new CompletableFuture<>();

        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(URI.create("http://localhost:" + mockServer.port() + "/test"))
                                                       .build();

        client.execute(AsyncExecuteRequest.builder()
                                          .request(request)
                                          .requestContentPublisher(new EmptyPublisher())
                                          .responseHandler(new SdkAsyncHttpResponseHandler() {
                                              @Override
                                              public void onHeaders(SdkHttpResponse headers) {
                                                  statusCode.set(headers.statusCode());
                                              }

                                              @Override
                                              public void onStream(Publisher<ByteBuffer> stream) {
                                                  stream.subscribe(new Subscriber<ByteBuffer>() {
                                                      private Subscription sub;

                                                      @Override
                                                      public void onSubscribe(Subscription s) {
                                                          sub = s;
                                                          s.request(Long.MAX_VALUE);
                                                      }

                                                      @Override
                                                      public void onNext(ByteBuffer byteBuffer) {
                                                          byte[] bytes = new byte[byteBuffer.remaining()];
                                                          byteBuffer.get(bytes);
                                                          body.set(body.get() + new String(bytes));
                                                      }

                                                      @Override
                                                      public void onError(Throwable t) {
                                                          future.completeExceptionally(t);
                                                      }

                                                      @Override
                                                      public void onComplete() {
                                                          future.complete(null);
                                                      }
                                                  });
                                              }

                                              @Override
                                              public void onError(Throwable error) {
                                                  future.completeExceptionally(error);
                                              }
                                          })
                                          .build());

        future.get(10, TimeUnit.SECONDS);

        assertThat(statusCode.get()).isEqualTo(200);
        assertThat(body.get()).isEqualTo("hello");
    }

    @Test
    public void postRequest_withBody_returns200() throws Exception {
        stubFor(any(urlPathEqualTo("/post"))
                    .willReturn(aResponse().withStatus(200).withBody("ok")));

        AtomicInteger statusCode = new AtomicInteger();
        CompletableFuture<Void> future = new CompletableFuture<>();

        byte[] bodyBytes = "request-body".getBytes();
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.POST)
                                                       .uri(URI.create("http://localhost:" + mockServer.port() + "/post"))
                                                       .build();

        client.execute(AsyncExecuteRequest.builder()
                                          .request(request)
                                          .requestContentPublisher(new SimpleBodyPublisher(bodyBytes))
                                          .responseHandler(new SdkAsyncHttpResponseHandler() {
                                              @Override
                                              public void onHeaders(SdkHttpResponse headers) {
                                                  statusCode.set(headers.statusCode());
                                              }

                                              @Override
                                              public void onStream(Publisher<ByteBuffer> stream) {
                                                  stream.subscribe(new Subscriber<ByteBuffer>() {
                                                      @Override
                                                      public void onSubscribe(Subscription s) {
                                                          s.request(Long.MAX_VALUE);
                                                      }

                                                      @Override
                                                      public void onNext(ByteBuffer b) {
                                                      }

                                                      @Override
                                                      public void onError(Throwable t) {
                                                          future.completeExceptionally(t);
                                                      }

                                                      @Override
                                                      public void onComplete() {
                                                          future.complete(null);
                                                      }
                                                  });
                                              }

                                              @Override
                                              public void onError(Throwable error) {
                                                  future.completeExceptionally(error);
                                              }
                                          })
                                          .build());

        future.get(10, TimeUnit.SECONDS);
        assertThat(statusCode.get()).isEqualTo(200);
    }

    /**
     * Simple publisher that emits a single byte array.
     */
    private static final class SimpleBodyPublisher implements software.amazon.awssdk.http.async.SdkHttpContentPublisher {
        private final byte[] data;

        SimpleBodyPublisher(byte[] data) {
            this.data = data;
        }

        @Override
        public java.util.Optional<Long> contentLength() {
            return java.util.Optional.of((long) data.length);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                private boolean sent = false;

                @Override
                public void request(long n) {
                    if (!sent) {
                        sent = true;
                        subscriber.onNext(ByteBuffer.wrap(data));
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                }
            });
        }
    }
}
