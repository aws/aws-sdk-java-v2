/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertEquals;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.reactivex.Flowable;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class JavaHttpClientNioAsyncHttpClientWireMockTest {
    private final RecordingNetworkTrafficListener wiremockTrafficListener = new RecordingNetworkTrafficListener();
    private static SdkAsyncHttpClient client = JavaHttpClientNioAsyncHttpClient.builder().build();

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .dynamicHttpsPort()
            .networkTrafficListener(wiremockTrafficListener));

    @Before
    public void methodSetup() {
        System.setProperty("jdk.internal.httpclient.debug", "true");
        wiremockTrafficListener.reset();
    }

    @After
    public void tearDownSetup() {
        System.clearProperty("jdk.internal.httpclient.debug");
    }

    @Rule
    public RetryRule retryRule = new RetryRule(10);

    public class RetryRule implements TestRule {
        private int retryCount;

        public RetryRule (int retryCount) {
            this.retryCount = retryCount;
        }

        public Statement apply(Statement base, Description description) {
            return statement(base, description);
        }

        private Statement statement(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Throwable caughtThrowable = null;

                    for (int i = 0; i < retryCount; i++) {
                        try {
                            base.evaluate();
                            return;
                        } catch (Throwable t) {
                            caughtThrowable = t;
                        }
                        System.out.println("Test failed after 10 attemps!");
                    }
                    throw caughtThrowable;
                }
            };
        }
    }

    private void makeSimpleRequest(SdkAsyncHttpClient client) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        stubFor(any(urlPathEqualTo("/"))
                .willReturn(aResponse().withBody(body)));
        SdkHttpRequest request = createRequest(uri);
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.execute(AsyncExecuteRequest.builder()
                .request(request)
                .requestContentPublisher(createProvider(""))
                .responseHandler(recorder)
                .build());
        recorder.completeFuture.get(5, TimeUnit.SECONDS);
    }

    @Test
    public void simpleMockTest() throws Exception {
        makeSimpleRequest(client);
    }

    @Test
    public void simpleMockPostTest() throws InterruptedException, ExecutionException, TimeoutException {
        // Tests whether the HttpClient really only takes content in length of that demanded in "Content-Length"
        final String content = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        stubFor(any(urlEqualTo("/echo?reversed=true"))
                .withRequestBody(equalTo(content))
                .willReturn(aResponse().withBody(StringUtils.reverse(content))));

        SdkHttpFullRequest request = createRequest(uri, "/echo", content, SdkHttpMethod.POST, singletonMap("reversed", "true"), emptyMap());
        request = request.toBuilder().putHeader("Content-Length", Integer.toString(content.length())).build();
        RecordingResponseHandler recorder = new RecordingResponseHandler();

        SdkHttpContentPublisher contentPublisher = createProvider(content);
        client.execute(AsyncExecuteRequest.builder()
                .request(request)
                .requestContentPublisher(contentPublisher)
                .responseHandler(recorder)
                .build()).join();

        recorder.completeFuture.get(2, TimeUnit.SECONDS);
        assertThat(wiremockTrafficListener.response.toString().endsWith(content));

    }

    private SdkHttpFullRequest createRequest(URI uri) {
        return createRequest(uri, "/", "", SdkHttpMethod.GET, emptyMap(), emptyMap());
    }

    private SdkHttpFullRequest createRequest(URI uri,
                                             String resourcePath,
                                             String body,
                                             SdkHttpMethod method,
                                             Map<String, String> params,
                                             Map<String, List<String>> headers) {
        String contentLength = body == null ? null : String.valueOf(body.getBytes(UTF_8).length);
        return SdkHttpFullRequest.builder()
                .uri(uri)
                .method(method)
                .encodedPath(resourcePath)
                .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                .applyMutation(b -> {
                    b.putHeader("Host", uri.getHost());
                    if (contentLength != null) {
                        b.putHeader("Content-Length", contentLength);
                    }
                })
                .applyMutation(b -> headers.forEach(b::putHeader))
                .build();
    }


    private SdkHttpContentPublisher createProvider(String body) {

        return new SdkHttpContentPublisher() {
            Flowable<ByteBuffer> flowable = Flowable.just(ByteBuffer.wrap(body.getBytes()));
            @Override
            public Optional<Long> contentLength() {
                return Optional.of((long) body.length());
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                flowable.subscribeWith(s);
            }
        };
    }


    @Test
    public void testConnectionTimeoutError() throws Exception{
        try{
            String expectedErrorMsg = "java.net.http.HttpConnectTimeoutException: HTTP connect timed out";
            SdkAsyncHttpClient customClient = JavaHttpClientNioAsyncHttpClient.builder()
                    .connectionTimeout(Duration.ofMillis(1))
                    .build();

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(makeSimpleRequestAndReturnResponseHandler(customClient).completeFuture);
            }

            assertThatThrownBy(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join())
                    .hasMessageContaining(expectedErrorMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void protocolChangedTest() {
        JavaHttpClientNioAsyncHttpClient customClient = (JavaHttpClientNioAsyncHttpClient)
                                                        JavaHttpClientNioAsyncHttpClient.builder()
                                                        .protocol(Protocol.HTTP2)
                                                        .build();
        assertEquals(customClient.getHttpClient().version(), HttpClient.Version.HTTP_2);
    }

    @Test
    public void sslParametersTest() {
        SSLParameters sslParameters = new SSLParameters();
        String[] protocols = new String[] { "TLSv1.2" };
        String[] cipherSuites = new String[] { "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256" };
        String[] applicationProtocols = new String[] { "h2", "http/1.1" };
        sslParameters.setProtocols(protocols);
        sslParameters.setCipherSuites(cipherSuites);
        sslParameters.setApplicationProtocols(applicationProtocols);

        JavaHttpClientNioAsyncHttpClient customClient = (JavaHttpClientNioAsyncHttpClient)
                                                        JavaHttpClientNioAsyncHttpClient.builder()
                                                        .configureSsl(sslParameters)
                                                        .build();
        SSLParameters testSslParameters = customClient.getHttpClient().sslParameters();
        assertThat(testSslParameters.getProtocols().equals(protocols));
        assertThat(testSslParameters.getCipherSuites().equals(cipherSuites));
        assertThat(testSslParameters.getApplicationProtocols().equals(applicationProtocols));
    }

    @Test
    public void testResponseTimeoutError() throws Exception {
        try {
            String expectedErrorMsg = "java.net.http.HttpTimeoutException: request timed out";
            SdkAsyncHttpClient customClient = JavaHttpClientNioAsyncHttpClient.builder()
                    .responseTimeout(Duration.ofMillis(500))
                    .build();

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(makeSimpleRequestAndReturnResponseHandler(customClient).completeFuture);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            assertThatThrownBy(() -> CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join())
                    .hasMessageContaining(expectedErrorMsg);

            customClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RecordingResponseHandler makeSimpleRequestAndReturnResponseHandler(SdkAsyncHttpClient client) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + mockServer.port());
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody(body).withFixedDelay(1000)));
        SdkHttpRequest request = createRequest(uri);
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.execute(AsyncExecuteRequest.builder()
                .request(request)
                .requestContentPublisher(createProvider(""))
                .responseHandler(recorder)
                .build());
        return recorder;
    }

    private static class RecordingNetworkTrafficListener implements WiremockNetworkTrafficListener {
        private final StringBuilder requests = new StringBuilder();
        private final StringBuilder response = new StringBuilder();

        @Override
        public void opened(Socket socket) {

        }

        @Override
        public void incoming(Socket socket, ByteBuffer byteBuffer) {
            requests.append(StandardCharsets.UTF_8.decode(byteBuffer));
        }

        @Override
        public void outgoing(Socket socket, ByteBuffer byteBuffer) {
            response.append(UTF_8.decode(byteBuffer));
        }

        @Override
        public void closed(Socket socket) {

        }

        public void reset() {
            requests.setLength(0);
        }
    }



}