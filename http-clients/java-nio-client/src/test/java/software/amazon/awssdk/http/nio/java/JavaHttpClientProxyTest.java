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
import static junit.framework.TestCase.*;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.reactivex.Flowable;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.*;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class JavaHttpClientProxyTest {

    private final RecordingNetworkTrafficListener wiremockTrafficListener = new RecordingNetworkTrafficListener();

    @Before
    public void setup() {
        clearProxyProperties();
    }

    @After
    public static void cleanup() {
        clearProxyProperties();
    }

    @Test
    public void testFieldsWhenSystemPropertyEnabled() {
        System.setProperty("http.proxyHost", "foo.com");
        System.setProperty("http.proxyPort", Integer.toString(7777));
        System.setProperty("http.proxyUser", "foo");
        System.setProperty("http.proxyPassword", "bar");

        SdkProxyConfig sdkProxyConfig = SdkProxyConfig.builder().useSystemPropertyValues(true).build();

        assertEquals("foo.com", sdkProxyConfig.getHost());
        assertEquals(7777, sdkProxyConfig.getPort());
        assertEquals("foo", sdkProxyConfig.getUsername());
        assertEquals("bar", sdkProxyConfig.getPassword());
    }


    @Test
    public void testFieldsWhenSystemPropertyDisabledAndNoSettings() {
        System.setProperty("http.proxyHost", "foo.com");
        System.setProperty("http.proxyPort", Integer.toString(7777));
        System.setProperty("http.proxyUser", "foo");
        System.setProperty("http.proxyPassword", "bar");

        SdkProxyConfig sdkProxyConfig = SdkProxyConfig.builder().useSystemPropertyValues(false).build();

        assertNull(sdkProxyConfig.getHost());
        assertEquals(0, sdkProxyConfig.getPort());
        assertNull(sdkProxyConfig.getUsername());
        assertNull(sdkProxyConfig.getPassword());
    }

    @Test
    public void testSettingFields_SystemPropertyEnabled() {
        System.setProperty("http.proxyHost", "foo.com");
        System.setProperty("http.proxyPort", Integer.toString(7777));
        System.setProperty("http.proxyUser", "foo");
        System.setProperty("http.proxyPassword", "bar");

        SdkProxyConfig sdkProxyConfig = SdkProxyConfig.builder()
                .host("notfoo.com")
                .port(8888)
                .useSystemPropertyValues(true)
                .build();

        assertEquals("notfoo.com", sdkProxyConfig.getHost());
        assertEquals(8888, sdkProxyConfig.getPort());
        assertEquals("foo", sdkProxyConfig.getUsername());
        assertEquals("bar", sdkProxyConfig.getPassword());
    }

    @Test
    public void testSettingProxyConfigurationsWithProvidedObjects() {
        System.setProperty("http.proxyHost", "foo.com");
        System.setProperty("http.proxyPort", Integer.toString(7777));
        System.setProperty("http.proxyUser", "foo");
        System.setProperty("http.proxyPassword", "bar");

        ProxySelector proxySelector = ProxySelector.of(new InetSocketAddress("http://localhost", 1234));
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("foo", "bar".toCharArray());
            }
        };
        SdkProxyConfig sdkProxyConfig = SdkProxyConfig.builder()
                .proxySelector(proxySelector)
                .authenticator(authenticator)
                .build();
        assertEquals(proxySelector, sdkProxyConfig.getProxySelector());
        assertEquals(authenticator, sdkProxyConfig.getAuthenticator());
    }

    @Rule
    public WireMockRule mockProxy = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .dynamicHttpsPort()
            .networkTrafficListener(wiremockTrafficListener));

    @Test
    public void proxyHTTPMockTest() {
        ProxySelector proxySelector = ProxySelector.of(new InetSocketAddress("localhost", mockProxy.port()));
        String body = "Proxy Testing!";
        String desHost = "s3.amazonaws.com";
        URI uri = URI.create("http://"+desHost);
        SdkHttpRequest request = createRequest(uri, "/", body, SdkHttpMethod.GET, emptyMap(), emptyMap());

        stubFor(any(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withBody(body)));

        SdkProxyConfig sdkProxyConfig = SdkProxyConfig.builder()
                .proxySelector(proxySelector)
                .build();
        SdkAsyncHttpClient myClient = JavaHttpClientNioAsyncHttpClient.builder()
                                                                    .proxyConfig(sdkProxyConfig)
                                                                    .build();
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        myClient.execute(AsyncExecuteRequest.builder()
                .request(request)
                .requestContentPublisher(createProvider(body))
                .responseHandler(recorder)
                .build());

        recorder.completeFuture.join();


        String capturedString = wiremockTrafficListener.requests.toString();
        int methodPos = capturedString.indexOf("GET");
        String requestLine = capturedString.substring(methodPos);
        assertTrue(requestLine.contains("GET"));
        assertTrue(requestLine.contains(desHost));
        assertTrue(requestLine.contains("HTTP/1.1"));
    }

    @Test
    public void proxyHTTPSMockTest() {
        ProxySelector proxySelector = ProxySelector.of(new InetSocketAddress("localhost", mockProxy.port()));
        String body = "Proxy Testing!";
        String desHost = "s3.amazonaws.com";
        URI uri = URI.create("https://"+desHost);
        SdkHttpRequest request = createRequest(uri, "/", body, SdkHttpMethod.GET, emptyMap(), emptyMap());

        stubFor(any(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withBody(body)));

        SdkProxyConfig sdkProxyConfig = SdkProxyConfig.builder()
                                                .proxySelector(proxySelector)
                                                .build();
        SdkAsyncHttpClient myClient = JavaHttpClientNioAsyncHttpClient.builder()
                                                .proxyConfig(sdkProxyConfig)
                                                .build();
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        myClient.execute(AsyncExecuteRequest.builder()
                                            .request(request)
                                            .requestContentPublisher(createProvider(body))
                                            .responseHandler(recorder)
                                            .build());

        // Since we only have to check the request arriving at the proxy, the absent response can be ignored
        try {
            recorder.completeFuture.join();
        } catch (Exception ignored) {}

        String capturedString = wiremockTrafficListener.requests.toString();
        int methodPos = capturedString.indexOf("CONNECT");
        String requestLine = capturedString.substring(methodPos);
        assertTrue(requestLine.contains("CONNECT"));
        assertTrue(requestLine.contains(desHost));
        assertTrue(requestLine.contains("HTTP/1.1"));
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

    private static void clearProxyProperties() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
    }
}
