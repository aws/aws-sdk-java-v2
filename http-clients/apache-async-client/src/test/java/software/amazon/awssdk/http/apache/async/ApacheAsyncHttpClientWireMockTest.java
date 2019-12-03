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

package software.amazon.awssdk.http.apache.async;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.core5.http.HttpHost;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;

@RunWith(MockitoJUnitRunner.class)
public class ApacheAsyncHttpClientWireMockTest extends SdkHttpClientTestSuite {
    @Rule
    public WireMockRule mockProxyServer = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        return wrapAsyncClient(ApacheAsyncHttpClient.builder().build());
    }

    @Test
    public void noSslException_WhenCertCheckingDisabled() throws Exception {
        SdkHttpClient client = wrapAsyncClient(ApacheAsyncHttpClient.builder()
                                                                    .buildWithDefaults(AttributeMap.builder()
                                                                                                   .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                                                                                   .build()));

        testForResponseCodeUsingHttps(client, HttpURLConnection.HTTP_OK);
    }

    @Test
    public void routePlannerIsInvoked() throws Exception {
        mockProxyServer.resetToDefaultMappings();
        mockProxyServer.addStubMapping(WireMock.any(urlPathEqualTo("/"))
                                               .willReturn(aResponse().proxiedFrom("http://localhost:" + mockServer.port()))
                                               .build());

        InetAddress localHost = InetAddress.getLocalHost();
        SdkAsyncHttpClient client = ApacheAsyncHttpClient
            .builder()
            .httpRoutePlanner(
                (host, context) ->
                    new HttpRoute(
                        new HttpHost("https", localHost, mockProxyServer.httpsPort())
                    )
            )
            .buildWithDefaults(AttributeMap.builder()
                                           .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                           .build());

        testForResponseCodeUsingHttps(wrapAsyncClient(client), HttpURLConnection.HTTP_OK);

        mockProxyServer.verify(1, RequestPatternBuilder.allRequests());
    }

    @Test
    @Ignore("broken: SdkCancellationException thrown; CompletableFuture#join never returns")
    @ReviewBeforeRelease("This test needs to be fixed")
    public void credentialPlannerIsInvoked() throws Exception {
        mockProxyServer.addStubMapping(WireMock.any(urlPathEqualTo("/"))
                                               .willReturn(aResponse()
                                                               .withHeader("WWW-Authenticate", "Basic realm=\"proxy server\"")
                                                               .withStatus(401))
                                               .build());

        mockProxyServer.addStubMapping(WireMock.any(urlPathEqualTo("/"))
                                               .withBasicAuth("foo", "bar")
                                               .willReturn(aResponse()
                                                               .proxiedFrom("http://localhost:" + mockServer.port()))
                                               .build());

        InetAddress localHost = InetAddress.getLocalHost();
        SdkAsyncHttpClient client = ApacheAsyncHttpClient
            .builder()
            .credentialsProvider((authScope, context) -> new UsernamePasswordCredentials("foo", "bar".toCharArray()))
            .httpRoutePlanner(
                (host, context) ->
                    new HttpRoute(
                        new HttpHost("https", localHost, mockProxyServer.httpsPort())
                    )
            )
            .buildWithDefaults(AttributeMap.builder()
                                           .put(TRUST_ALL_CERTIFICATES, Boolean.TRUE)
                                           .build());
        testForResponseCodeUsingHttps(wrapAsyncClient(client), HttpURLConnection.HTTP_OK);

        mockProxyServer.verify(2, RequestPatternBuilder.allRequests());
    }

    private static SdkHttpClient wrapAsyncClient(SdkAsyncHttpClient asyncHttpClient) {
        return new SdkHttpClient() {
            @Override
            public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
                SdkHttpContentPublisher publisher = null;
                if (request.contentStreamProvider().isPresent()) {
                    try {
                        ContentStreamProvider contentStreamProvider = request.contentStreamProvider().get();
                        publisher = new SimpleHttpContentPublisher(IoUtils.toByteArray(contentStreamProvider.newStream()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                AtomicReference<SdkHttpResponse> headers = new AtomicReference<>(null);
                AtomicReference<Publisher<ByteBuffer>> stream = new AtomicReference<>(null);
                AtomicReference<Throwable> error = new AtomicReference<>();
                AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
                                                                             .request(request.httpRequest())
                                                                             .requestContentPublisher(publisher)
                                                                             .responseHandler(new SdkAsyncHttpResponseHandler() {
                                                                                 @Override
                                                                                 public void onHeaders(SdkHttpResponse h) {
                                                                                     headers.set(h);
                                                                                 }

                                                                                 @Override
                                                                                 public void onStream(Publisher<ByteBuffer> s) {
                                                                                     stream.set(s);
                                                                                 }

                                                                                 @Override
                                                                                 public void onError(Throwable e) {
                                                                                     error.set(e);
                                                                                 }
                                                                             })
                                                                             .build();
                return new ExecutableHttpRequest() {
                    @Override
                    public HttpExecuteResponse call() throws IOException {
                        CompletableFuture<Void> cf = asyncHttpClient.execute(asyncExecuteRequest);
                        try {
                            cf.join();
                        } catch (CompletionException ex) {
                            if (ex.getCause() instanceof IOException) {
                                throw (IOException) ex.getCause();
                            } else {
                                throw ex;
                            }
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (WritableByteChannel channel = Channels.newChannel(baos)) {
                            Flowable.fromPublisher(stream.get()).forEach(channel::write);
                        }
                        AbortableInputStream responseBody = null;
                        if (baos.toByteArray().length > 0) {
                            responseBody = AbortableInputStream.create(new ByteArrayInputStream(baos.toByteArray()));
                        }
                        return HttpExecuteResponse.builder()
                                                  .response(headers.get())
                                                  .responseBody(responseBody)
                                                  .build();
                    }

                    @Override
                    public void abort() {

                    }
                };
            }

            @Override
            public void close() {
                asyncHttpClient.close();
            }
        };
    }

    private static final class SimpleHttpContentPublisher implements SdkHttpContentPublisher {
        private final byte[] content;
        private final int length;

        public SimpleHttpContentPublisher(byte[] content) {
            this.content = content;
            this.length = content.length;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of(Long.valueOf(length));
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new SubscriptionImpl(s));
        }

        private class SubscriptionImpl implements Subscription {
            private boolean running = true;
            private final Subscriber<? super ByteBuffer> s;

            private SubscriptionImpl(Subscriber<? super ByteBuffer> s) {
                this.s = s;
            }

            @Override
            public void request(long n) {
                if (running) {
                    running = false;
                    if (n <= 0) {
                        s.onError(new IllegalArgumentException("Demand must be positive"));
                    } else {
                        s.onNext(ByteBuffer.wrap(content));
                        s.onComplete();
                    }
                }
            }

            @Override
            public void cancel() {
                running = false;
            }
        }
    }
}
