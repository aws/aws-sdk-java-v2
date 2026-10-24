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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.io.CloseMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.ConnectionPoolShutdownException;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache5.internal.conn.IdleConnectionCloser;
import software.amazon.awssdk.http.apache5.internal.conn.IdleConnectionReaper;
import software.amazon.awssdk.http.apache5.internal.conn.RecreatingHttpClientConnectionManager;
import software.amazon.awssdk.http.apache5.internal.conn.SafePoolingHttpClientConnectionManager;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Which connection manager {@link Apache5HttpClient} installs in each mode, and how it is registered with the reaper.
 */
public class Apache5HttpClientRecoveryWiringTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .build();

    private final List<SdkHttpClient> clients = new ArrayList<>();
    private final List<ExecutorService> executors = new ArrayList<>();

    @BeforeEach
    public void setup() {
        wireMock.stubFor(any(urlMatching(".*")).willReturn(aResponse().withStatus(200).withBody("ok")));
    }

    @AfterEach
    public void teardown() {
        executors.forEach(ExecutorService::shutdownNow);
        clients.forEach(SdkHttpClient::close);
    }

    /** The default builder, an explicit null, and DISABLED. */
    private static Stream<Arguments> defaultNullOrDisabledBuilders() {
        return Stream.of(Arguments.of("default", Apache5HttpClient.builder()),
                         Arguments.of("null", Apache5HttpClient.builder().connectionPoolRecoveryMode(null)),
                         Arguments.of("DISABLED", Apache5HttpClient.builder()
                                                                   .connectionPoolRecoveryMode(
                                                                       ConnectionPoolRecoveryMode.DISABLED)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("defaultNullOrDisabledBuilders")
    public void build_defaultNullOrDisabledMode_installsPlainPoolingManager(String name,
                                                                           Apache5HttpClient.Builder builder) {
        Apache5HttpClient client = build(builder);

        // DISABLED is the code path that predates recovery: no SDK code between Apache and its pool.
        assertThat(client.connectionManager()).isNotInstanceOf(RecreatingHttpClientConnectionManager.class);
        assertThat(client.connectionManager()).isExactlyInstanceOf(SafePoolingHttpClientConnectionManager.class);
    }

    @ParameterizedTest
    @EnumSource(value = ConnectionPoolRecoveryMode.class, names = {"WITH_BACKOFF", "ALWAYS"})
    public void build_withBackoffOrAlwaysMode_installsRecreatingManager(ConnectionPoolRecoveryMode mode) {
        Apache5HttpClient client = build(Apache5HttpClient.builder().connectionPoolRecoveryMode(mode));

        assertThat(client.connectionManager()).isExactlyInstanceOf(RecreatingHttpClientConnectionManager.class);
    }

    @Test
    public void close_disabled_deregistersPoolFromReaper() {
        Apache5HttpClient client = build(Apache5HttpClient.builder().useIdleConnectionReaper(true));
        IdleConnectionCloser cm = (IdleConnectionCloser) client.connectionManager();

        // The pool object itself was registered.
        assertThat(IdleConnectionReaper.getInstance().deregisterConnectionManager(cm)).isTrue();
        IdleConnectionReaper.getInstance().registerConnectionManager(cm, 60_000);

        client.close();

        assertThat(IdleConnectionReaper.getInstance().deregisterConnectionManager(cm)).isFalse();
    }

    @Test
    public void close_withBackoff_deregistersWrapperFromReaper() {
        // Equivalence guard: also passes with the earlier always-on recovery, by design.
        Apache5HttpClient client = build(Apache5HttpClient.builder()
                                                        .useIdleConnectionReaper(true)
                                                        .connectionPoolRecoveryMode(
                                                            ConnectionPoolRecoveryMode.WITH_BACKOFF));
        IdleConnectionCloser cm = (IdleConnectionCloser) client.connectionManager();

        assertThat(IdleConnectionReaper.getInstance().deregisterConnectionManager(cm)).isTrue();
        IdleConnectionReaper.getInstance().registerConnectionManager(cm, 60_000);

        client.close();

        assertThat(IdleConnectionReaper.getInstance().deregisterConnectionManager(cm)).isFalse();
    }

    /**
     * A close from a thread whose last request has finished has nothing to be attributed to, so it destroys the current
     * pool as it always did. That only holds if the client cleared the thread's record of the pool it last leased from.
     */
    @Test
    public void close_onThreadWithNoRequestInProgress_closesCurrentPool() throws Exception {
        Apache5HttpClient client = build(Apache5HttpClient.builder()
                                                          .connectionPoolRecoveryMode(ConnectionPoolRecoveryMode.WITH_BACKOFF));
        HttpClientConnectionManager cm = client.connectionManager();
        ExecutorService otherThread = newExecutor();
        ExecutorService requestThread = newExecutor();

        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
        otherThread.submit(() -> cm.close(CloseMode.IMMEDIATE)).get(10, TimeUnit.SECONDS);
        assertThat(requestThread.submit(() -> execute(client, request()).httpResponse().statusCode())
                                .get(10, TimeUnit.SECONDS))
            .isEqualTo(200);

        cm.close(CloseMode.IMMEDIATE);

        // The second pool was destroyed inside the backoff window.
        Throwable thrown = catchThrowable(() -> execute(client, request()));
        assertThat(thrown).isInstanceOf(ConnectionPoolShutdownException.class);
        assertThat(((ConnectionPoolShutdownException) thrown).reason()).isEqualTo(Reason.RECOVERY_BACKOFF);
    }

    private Apache5HttpClient build(Apache5HttpClient.Builder builder) {
        Apache5HttpClient client = (Apache5HttpClient) builder.build();
        clients.add(client);
        return client;
    }

    private ExecutorService newExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executors.add(executor);
        return executor;
    }

    private SdkHttpFullRequest request() {
        URI uri = URI.create("http://localhost:" + wireMock.getPort());
        byte[] content = "Body".getBytes(StandardCharsets.UTF_8);
        return SdkHttpFullRequest.builder()
                                 .uri(uri)
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", uri.getHost())
                                 .putHeader("Content-Length", Integer.toString(content.length))
                                 .contentStreamProvider(() -> new ByteArrayInputStream(content))
                                 .build();
    }

    private static HttpExecuteResponse execute(SdkHttpClient client, SdkHttpFullRequest request) throws IOException {
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(request)
                                                    .contentStreamProvider(request.contentStreamProvider().orElse(null))
                                                    .build())
                  .call();
        response.responseBody().ifPresent(IoUtils::drainInputStream);
        return response;
    }
}
