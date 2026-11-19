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
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Verifies that a wildcard {@code http.nonProxyHosts} entry keeps matching hosts OFF the proxy on the CRT client. The routing
 * decision is observed via the mock proxy's request journal, so the destination host does not need to resolve.
 */
class CrtProxyWireMockTest {
    private SdkAsyncHttpClient client;

    private final WireMockServer mockProxy = new WireMockServer(new WireMockConfiguration().dynamicPort());
    private final WireMockServer mockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());

    @BeforeEach
    public void setup() {
        mockProxy.start();
        mockServer.start();
        mockProxy.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("hello")));
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("hello")));
    }

    @AfterEach
    public void teardown() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
        mockServer.stop();
        mockProxy.stop();
        if (client != null) {
            client.close();
        }
        EventLoopGroup.closeStaticDefault();
        HostResolver.closeStaticDefault();
    }

    @Test
    void suffixWildcardNonProxyHost_systemProperty_bypassesProxy() throws Exception {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", Integer.toString(mockProxy.port()));
        System.setProperty("http.nonProxyHosts", "*.internal.example.com");

        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder().build())
                                      .build();

        sendRequest(URI.create("http://api.internal.example.com"));

        mockProxy.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void suffixWildcardNonProxyHost_rootDomainHost_bypassesProxy() throws Exception {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", Integer.toString(mockProxy.port()));
        System.setProperty("http.nonProxyHosts", "*.internal.example.com");

        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder().build())
                                      .build();

        sendRequest(URI.create("http://internal.example.com"));

        mockProxy.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void exactHostNonProxyHost_systemProperty_bypassesProxy() throws Exception {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", Integer.toString(mockProxy.port()));
        System.setProperty("http.nonProxyHosts", "api.internal.example.com");

        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder().build())
                                      .build();

        sendRequest(URI.create("http://api.internal.example.com"));

        mockProxy.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void nonMatchingHost_withSuffixWildcardNonProxyHost_isProxied() throws Exception {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", Integer.toString(mockProxy.port()));
        System.setProperty("http.nonProxyHosts", "*.internal.example.com");

        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder().build())
                                      .build();

        sendRequest(URI.create("http://api.external.example.com"));

        mockProxy.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void suffixWildcardNonProxyHost_environmentVariable_bypassesProxy() throws Exception {
        EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
        try {
            environmentVariableHelper.set("http_proxy", "http://localhost:" + mockProxy.port());
            environmentVariableHelper.set("no_proxy", "*.internal.example.com");

            client = AwsCrtAsyncHttpClient.builder()
                                          .proxyConfiguration(ProxyConfiguration.builder()
                                                                                .useSystemPropertyValues(false)
                                                                                .build())
                                          .build();

            sendRequest(URI.create("http://api.internal.example.com"));

            mockProxy.verify(0, anyRequestedFor(anyUrl()));
        } finally {
            environmentVariableHelper.reset();
        }
    }

    // Both the comma and comma-space no_proxy spellings must route the same: the wildcard entry (*.foo.com) bypasses
    // sub.foo.com, and the second entry (a.com) - which carries a leading space in the comma-space form - bypasses a.com
    // after the CRT-side whitespace trim.
    @ParameterizedTest(name = "no_proxy=\"{0}\"")
    @ValueSource(strings = {"*.foo.com,a.com", "*.foo.com, a.com"})
    void commaSeparatedNonProxyHosts_environmentVariable_bypassesProxy(String noProxy) throws Exception {
        EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
        try {
            environmentVariableHelper.set("http_proxy", "http://localhost:" + mockProxy.port());
            environmentVariableHelper.set("no_proxy", noProxy);

            client = AwsCrtAsyncHttpClient.builder()
                                          .proxyConfiguration(ProxyConfiguration.builder()
                                                                                .useSystemPropertyValues(false)
                                                                                .build())
                                          .build();

            sendRequest(URI.create("http://sub.foo.com"));
            sendRequest(URI.create("http://a.com"));

            mockProxy.verify(0, anyRequestedFor(anyUrl()));
        } finally {
            environmentVariableHelper.reset();
        }
    }

    @Test
    void bareWildcardNonProxyHost_systemProperty_reachesServerDirectly() throws Exception {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", Integer.toString(mockProxy.port()));
        System.setProperty("http.nonProxyHosts", "*");

        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder().build())
                                      .build();

        sendRequest(URI.create("http://localhost:" + mockServer.port()));

        mockProxy.verify(0, anyRequestedFor(anyUrl()));
        mockServer.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void nonLeadingWildcardNonProxyHost_hostPrefix_isProxied() throws Exception {
        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder()
                                                                            .host("localhost")
                                                                            .port(mockProxy.port())
                                                                            .nonProxyHosts(Stream.of("internal*").collect(toSet()))
                                                                            .build())
                                      .build();

        sendRequest(URI.create("http://internalservice.example.com"));

        mockProxy.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void nonLeadingWildcardNonProxyHost_ipPrefix_isProxied() throws Exception {
        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder()
                                                                            .host("localhost")
                                                                            .port(mockProxy.port())
                                                                            .nonProxyHosts(Stream.of("192.168.*").collect(toSet()))
                                                                            .build())
                                      .build();

        sendRequest(URI.create("http://192.168.1.1"));

        mockProxy.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void nonLeadingWildcardNonProxyHost_notReinterpretedAsSuffixWildcard_apexProxied() throws Exception {
        client = AwsCrtAsyncHttpClient.builder()
                                      .proxyConfiguration(ProxyConfiguration.builder()
                                                                            .host("localhost")
                                                                            .port(mockProxy.port())
                                                                            .nonProxyHosts(Stream.of("*foo.com").collect(toSet()))
                                                                            .build())
                                      .build();

        sendRequest(URI.create("http://foo.com"));

        mockProxy.verify(1, anyRequestedFor(anyUrl()));
    }

    private void sendRequest(URI uri) throws Exception {
        CompletableFuture<Boolean> streamReceived = new CompletableFuture<>();
        AtomicReference<SdkHttpResponse> response = new AtomicReference<>(null);
        AtomicReference<Throwable> error = new AtomicReference<>(null);
        Subscriber<ByteBuffer> subscriber = CrtHttpClientTestUtils.createDummySubscriber();
        SdkAsyncHttpResponseHandler handler =
            CrtHttpClientTestUtils.createTestResponseHandler(response, streamReceived, error, subscriber);
        SdkHttpRequest request = CrtHttpClientTestUtils.createRequest(uri, "/", null, SdkHttpMethod.GET, emptyMap());

        client.execute(AsyncExecuteRequest.builder()
                                          .request(request)
                                          .responseHandler(handler)
                                          .requestContentPublisher(new EmptyPublisher())
                                          .build())
              .exceptionally(t -> null)
              .get(60, TimeUnit.SECONDS);
    }
}
