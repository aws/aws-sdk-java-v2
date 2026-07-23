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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.liveEventLoopGroups;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.newEventLoopGroups;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.waitForEventLoopGroupsReleased;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.AttributeMap;

public class AwsCrtAsyncHttpClientWireMockTest {
    @RegisterExtension
    static WireMockExtension mockServer = WireMockExtension.newInstance()
                                                           .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
                                                           .build();

    @BeforeAll
    public static void setup() {
        System.setProperty("aws.crt.debugnative", "true");
        Log.initLoggingToStdout(Log.LogLevel.Warn);
    }

    @Test
    public void closeClient_reuse_throwException(WireMockRuntimeInfo wm) {
        SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.create();

        client.close();
        assertThatThrownBy(() -> makeSimpleRequest(client, wm)).hasMessageContaining("is closed");
    }

    @Test
    public void sendRequest_withCollector_shouldCollectMetrics(WireMockRuntimeInfo wm) throws Exception {

        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().maxConcurrency(10).build()) {
            RecordingResponseHandler recorder = makeSimpleRequest(client, wm);
            MetricCollection metrics = recorder.collector().collect();

            assertThat(metrics.metricValues(HttpMetric.HTTP_CLIENT_NAME)).containsExactly("AwsCommonRuntime");
            assertThat(metrics.metricValues(HttpMetric.MAX_CONCURRENCY)).containsExactly(10);
            assertThat(metrics.metricValues(HttpMetric.PENDING_CONCURRENCY_ACQUIRES)).containsExactly(0);
            assertThat(metrics.metricValues(HttpMetric.LEASED_CONCURRENCY)).containsExactly(1);
            assertThat(metrics.metricValues(HttpMetric.AVAILABLE_CONCURRENCY)).containsExactly(0);
        }
    }

    @Test
    public void sharedEventLoopGroup_closeOneClient_shouldNotAffectOtherClients(WireMockRuntimeInfo wm) throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.create()) {
            makeSimpleRequest(client, wm);
        }

        try (SdkAsyncHttpClient anotherClient = AwsCrtAsyncHttpClient.create()) {
            makeSimpleRequest(anotherClient, wm);
        }
    }

    @Test
    public void tlsNegotiationTimeout_customValue_clientStartsSuccessfully(WireMockRuntimeInfo wm) throws Exception {
        AttributeMap defaults = AttributeMap.builder().put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true).build();
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder()
                                                              .tlsNegotiationTimeout(Duration.ofSeconds(3))
                                                              .buildWithDefaults(defaults)) {
            makeSimpleHttpsRequest(client, wm);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1})
    public void numEventLoopThreads_notGreaterThanOne_shouldThrowException(int value) {
        assertThatThrownBy(() -> AwsCrtAsyncHttpClient.builder().numEventLoopThreads(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("numEventLoopThreads must be greater than 1");
    }

    @Test
    public void numEventLoopThreads_null_shouldBeAccepted() {
        assertThatCode(() -> AwsCrtAsyncHttpClient.builder().numEventLoopThreads(null))
            .doesNotThrowAnyException();
    }

    @Test
    public void defaultBuilder_sharesStaticDefaultEventLoopGroup() {
        warmUpStaticDefaultEventLoopGroup();
        Set<CrtResource> before = liveEventLoopGroups();

        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.create();
             SdkAsyncHttpClient anotherClient = AwsCrtAsyncHttpClient.create()) {
            assertThat(newEventLoopGroups(before)).isEmpty();
        }
    }

    @Test
    public void numEventLoopThreads_createsPrivateGroupsNotShared() {
        warmUpStaticDefaultEventLoopGroup();
        Set<CrtResource> before = liveEventLoopGroups();

        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().numEventLoopThreads(2).build();
             SdkAsyncHttpClient anotherClient = AwsCrtAsyncHttpClient.builder().numEventLoopThreads(2).build()) {
            assertThat(newEventLoopGroups(before)).hasSize(2);
        }
    }

    @Test
    public void numEventLoopThreads_executesRequest(WireMockRuntimeInfo wm) throws Exception {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().numEventLoopThreads(2).build()) {
            RecordingResponseHandler recorder = makeSimpleRequest(client, wm);
            assertThat(recorder.responses().get(0).statusCode()).isEqualTo(200);
        }
    }

    @Test
    public void numEventLoopThreads_closeReleasesPrivateGroup() {
        warmUpStaticDefaultEventLoopGroup();
        Set<CrtResource> before = liveEventLoopGroups();
        SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().numEventLoopThreads(2).build();
        Set<CrtResource> privateGroup = newEventLoopGroups(before);
        assertThat(privateGroup).hasSize(1);

        client.close();

        assertThat(waitForEventLoopGroupsReleased(privateGroup, Duration.ofSeconds(30)))
            .as("private event-loop group should be released on close")
            .isTrue();
    }

    @ParameterizedTest
    @CsvSource({"4, true", "1, false"})
    public void numEventLoopThreads_warnsOnlyWhenExcessive(int multipleOfProcessors, boolean expectWarning) {
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        int size = Math.max(2, multipleOfProcessors * processors);
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN);
             SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().numEventLoopThreads(size).build()) {
            if (expectWarning) {
                assertThat(logCaptor.loggedEvents()).anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getMessage().getFormattedMessage())
                        .contains("numEventLoopThreads")
                        .contains("private event-loop group");
                });
            } else {
                assertThat(logCaptor.loggedEvents()).noneSatisfy(event ->
                    assertThat(event.getMessage().getFormattedMessage()).contains("numEventLoopThreads"));
            }
        }
    }

    private void warmUpStaticDefaultEventLoopGroup() {
        // A default client and a private-group client both lazily create the shared static default group (via the host
        // resolver), so create it up front to keep the before/after group diff stable.
        AwsCrtAsyncHttpClient.create().close();
    }

    private RecordingResponseHandler makeSimpleHttpsRequest(SdkAsyncHttpClient client, WireMockRuntimeInfo wm) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("https://localhost:" + wm.getHttpsPort());
        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody(body)));
        SdkHttpRequest request = createRequest(uri);
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.execute(AsyncExecuteRequest.builder()
                                          .request(request)
                                          .requestContentPublisher(createProvider(""))
                                          .responseHandler(recorder)
                                          .build());
        recorder.completeFuture().get(5, TimeUnit.SECONDS);
        return recorder;
    }

    /**
     * Make a simple async request and wait for it to finish.
     *
     * @param client Client to make request with.
     */
    private RecordingResponseHandler makeSimpleRequest(SdkAsyncHttpClient client, WireMockRuntimeInfo wm) throws Exception {
        String body = randomAlphabetic(10);
        URI uri = URI.create("http://localhost:" + wm.getHttpPort());
        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withBody(body)));
        SdkHttpRequest request = createRequest(uri);
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        client.execute(AsyncExecuteRequest.builder()
                                          .request(request)
                                          .requestContentPublisher(createProvider(""))
                                          .responseHandler(recorder)
                                          .metricCollector(recorder.collector())
                                          .build());
        recorder.completeFuture().get(5, TimeUnit.SECONDS);
        return recorder;
    }
}
