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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
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
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Verifies that a nonProxyHosts entry with an unsupported wildcard placement is logged at WARN by the CRT client, from all
 * three input sources, while the supported forms are not.
 */
class CrtNonProxyHostsWildcardWarningTest {

    private static final String WARN_PREFIX = "Unsupported wildcard in nonProxyHosts entry";

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
    private SdkAsyncHttpClient client;

    @AfterEach
    public void teardown() {
        environmentVariableHelper.reset();
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.nonProxyHosts");
        if (client != null) {
            client.close();
        }
        EventLoopGroup.closeStaticDefault();
        HostResolver.closeStaticDefault();
    }

    @ParameterizedTest(name = "builder \"{0}\" warns")
    @ValueSource(strings = {"192.168.*", "internal*", "in*ternal", "*foo.com"})
    void unsupportedWildcard_builder_logsWarnOnce(String entry) {
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            client = AwsCrtAsyncHttpClient.builder()
                                          .proxyConfiguration(ProxyConfiguration.builder()
                                                                                .host("localhost")
                                                                                .port(1234)
                                                                                .nonProxyHosts(single(entry))
                                                                                .build())
                                          .build();
            assertWarnedOnceFor(logCaptor, entry);
        }
    }

    @ParameterizedTest(name = "http.nonProxyHosts \"{0}\" warns")
    @ValueSource(strings = {"192.168.*", "internal*", "in*ternal", "*foo.com"})
    void unsupportedWildcard_systemProperty_logsWarnOnce(String entry) {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "1234");
        System.setProperty("http.nonProxyHosts", entry);
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            client = AwsCrtAsyncHttpClient.builder()
                                          .proxyConfiguration(ProxyConfiguration.builder().build())
                                          .build();
            assertWarnedOnceFor(logCaptor, entry);
        }
    }

    @ParameterizedTest(name = "no_proxy \"{0}\" warns")
    @ValueSource(strings = {"192.168.*", "internal*", "in*ternal", "*foo.com"})
    void unsupportedWildcard_environmentVariable_logsWarnOnce(String entry) {
        environmentVariableHelper.set("http_proxy", "http://localhost:1234");
        environmentVariableHelper.set("no_proxy", entry);
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            client = AwsCrtAsyncHttpClient.builder()
                                          .proxyConfiguration(ProxyConfiguration.builder()
                                                                                .useSystemPropertyValues(false)
                                                                                .build())
                                          .build();
            assertWarnedOnceFor(logCaptor, entry);
        }
    }

    @ParameterizedTest(name = "supported \"{0}\" does not warn")
    @ValueSource(strings = {"example.com", "*.internal.example.com", "*", "10.0.0.0/8"})
    void supportedForm_builder_doesNotWarn(String entry) {
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            client = AwsCrtAsyncHttpClient.builder()
                                          .proxyConfiguration(ProxyConfiguration.builder()
                                                                                .host("localhost")
                                                                                .port(1234)
                                                                                .nonProxyHosts(single(entry))
                                                                                .build())
                                          .build();
            assertThat(warnRecords(logCaptor)).isEmpty();
        }
    }

    @Test
    void unsupportedWildcard_multipleRequests_warnsExactlyOnce() throws Exception {
        WireMockServer mockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        mockServer.start();
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody("hello")));
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            client = AwsCrtAsyncHttpClient.builder()
                                          .proxyConfiguration(ProxyConfiguration.builder()
                                                                                .host("localhost")
                                                                                .port(mockServer.port())
                                                                                .nonProxyHosts(single("192.168.*"))
                                                                                .build())
                                          .build();

            sendRequest(URI.create("http://localhost:" + mockServer.port()));
            sendRequest(URI.create("http://localhost:" + mockServer.port()));

            assertWarnedOnceFor(logCaptor, "192.168.*");
        } finally {
            mockServer.stop();
        }
    }

    private static Set<String> single(String entry) {
        return Stream.of(entry).collect(toSet());
    }

    private static List<LogEvent> warnRecords(LogCaptor logCaptor) {
        return logCaptor.loggedEvents().stream()
                        .filter(e -> e.getMessage().getFormattedMessage().contains(WARN_PREFIX))
                        .collect(Collectors.toList());
    }

    private static void assertWarnedOnceFor(LogCaptor logCaptor, String entry) {
        assertThat(warnRecords(logCaptor))
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getMessage().getFormattedMessage()).contains(WARN_PREFIX + " '" + entry + "'");
            });
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
