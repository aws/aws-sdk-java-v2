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

package software.amazon.awssdk.http.warmup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.util.ServiceLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.internal.http.loader.AsyncHttpClientWarmer;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.http.crt.AwsCrtSdkHttpService;
import software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService;

/**
 * Verifies the CRaC async warm-up sends its GET through each real async HTTP client. Every async client is exercised here, in
 * one place, instead of a separate test in each client module. The stub mirrors a real STS {@code GET} (302 redirect, empty
 * body); the warm-up must not follow the redirect, so exactly one request is expected.
 */
class AsyncHttpClientWarmUpTest {

    // The async HTTP clients on this module's classpath: netty-nio, aws-crt async. Hardcoded so a broken ServiceLoader that
    // discovers nothing fails the test instead of passing a trivial verify(0).
    private static final int ASYNC_CLIENT_COUNT = 2;

    private WireMockServer mockServer;

    static SdkAsyncHttpService[] asyncClients() {
        return new SdkAsyncHttpService[] {
            new NettySdkAsyncHttpService(),
            new AwsCrtSdkHttpService()
        };
    }

    @BeforeEach
    void setUp() {
        mockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockServer.start();
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    @ParameterizedTest
    @MethodSource("asyncClients")
    void warmAll_sendsWarmUpGetThroughClient(SdkAsyncHttpService service) {
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse()
            .withStatus(302)
            .withHeader("Location", "https://aws.amazon.com/iam")));

        URI endpoint = URI.create("http://localhost:" + mockServer.port() + "/");
        AsyncHttpClientWarmer.forService(() -> endpoint, service).warmAll();

        mockServer.verify(1, getRequestedFor(urlPathEqualTo("/")));
        mockServer.verify(1, anyRequestedFor(anyUrl()));
    }

    /**
     * Exercises the real classpath-discovery path used by {@code prime()}: {@code warmAll()} discovers every async
     * {@link SdkAsyncHttpService} on the classpath via {@link ServiceLoader} and warms each. Confirms discovery finds all
     * {@value #ASYNC_CLIENT_COUNT} clients and that each receives exactly one warm-up GET.
     */
    @Test
    void warmAll_whenDiscoveringFromClasspath_warmsEveryAsyncClient() {
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse()
            .withStatus(302)
            .withHeader("Location", "https://aws.amazon.com/iam")));

        int discoveredClients = 0;
        for (SdkAsyncHttpService ignored : ServiceLoader.load(SdkAsyncHttpService.class)) {
            discoveredClients++;
        }
        // Guard against a broken ServiceLoader: if discovery silently finds 0, verify(0) below would pass trivially.
        assertThat(discoveredClients).isEqualTo(ASYNC_CLIENT_COUNT);

        URI endpoint = URI.create("http://localhost:" + mockServer.port() + "/");
        AsyncHttpClientWarmer.create(() -> endpoint).warmAll();

        mockServer.verify(ASYNC_CLIENT_COUNT, getRequestedFor(urlPathEqualTo("/")));
        mockServer.verify(ASYNC_CLIENT_COUNT, anyRequestedFor(anyUrl()));
    }
}
