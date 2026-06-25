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

package software.amazon.awssdk.core.internal.http.loader;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.internal.crac.WarmUpRequest;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.apache.ApacheSdkHttpService;
import software.amazon.awssdk.http.apache5.Apache5SdkHttpService;

/**
 * Integration tests for {@link SyncHttpClientWarmer} against the real sync HTTP clients on this module's test classpath
 * (Apache and Apache5) and a WireMock server. Each test pins discovery to one real client so the warm-up exercises that
 * client implementation specifically.
 *
 * <p>The stub mirrors what a real {@code GET https://sts.<region>.amazonaws.com/} returns: a {@code 302} redirect with an
 * empty body. The warm-up must not follow the redirect, so exactly one GET is expected.
 */
public class SyncHttpClientWarmerIntegrationTest {

    private static final int WARM_UP_CYCLES = 20;
    private static final int STS_REDIRECT_STATUS = 302;
    private static final String STS_REDIRECT_LOCATION = "https://aws.amazon.com/iam";

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    @Test
    public void warmAll_sendsWarmUpRequestThroughApache() {
        assertWarmUpRequestIssued(new ApacheSdkHttpService());
    }

    @Test
    public void warmAll_sendsWarmUpRequestThroughApache5() {
        assertWarmUpRequestIssued(new Apache5SdkHttpService());
    }

    @Test
    public void warmAll_repeatedCycles_doNotLeakFileDescriptors() {
        org.junit.Assume.assumeTrue("FD check only runs where /proc/self/fd is available", openFdCountAvailable());
        stubStsRedirect();
        URI endpoint = endpoint();

        long before = openFdCount();
        for (int i = 0; i < WARM_UP_CYCLES; i++) {
            warmer(new ApacheSdkHttpService(), endpoint).warmAll();
        }
        long after = openFdCount();

        assertThat(after - before).isLessThan(WARM_UP_CYCLES);
    }

    @Test
    public void warmAll_whenServerUnreachable_swallowsAndDoesNotThrow() {
        int unusedPort = mockServer.port();
        mockServer.stop();
        URI endpoint = URI.create("http://localhost:" + unusedPort + "/");

        assertThatCode(() -> warmer(new ApacheSdkHttpService(), endpoint).warmAll()).doesNotThrowAnyException();
    }

    private void assertWarmUpRequestIssued(SdkHttpService service) {
        stubStsRedirect();

        warmer(service, endpoint()).warmAll();

        // One GET to "/", and nothing else: the warm-up issued the GET and did not follow the redirect.
        mockServer.verify(1, getRequestedFor(urlPathEqualTo("/")));
        mockServer.verify(1, anyRequestedFor(anyUrl()));
    }

    // Warmer pinned to a single real service, warmed against the given (WireMock) endpoint.
    private static SyncHttpClientWarmer warmer(SdkHttpService service, URI endpoint) {
        SdkServiceLoader loader = new SdkServiceLoader() {
            @Override
            @SuppressWarnings("unchecked")
            <T> Iterator<T> loadServices(Class<T> clazz) {
                return (Iterator<T>) Collections.singletonList(service).iterator();
            }
        };
        Supplier<URI> endpointProvider = () -> endpoint;
        return new SyncHttpClientWarmer(loader, endpointProvider, WarmUpRequest.get());
    }

    private URI endpoint() {
        return URI.create("http://localhost:" + mockServer.port() + "/");
    }

    private void stubStsRedirect() {
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse()
            .withStatus(STS_REDIRECT_STATUS)
            .withHeader("Location", STS_REDIRECT_LOCATION)));
    }

    private static boolean openFdCountAvailable() {
        return new File("/proc/self/fd").isDirectory();
    }

    private static long openFdCount() {
        File[] fds = new File("/proc/self/fd").listFiles();
        return fds == null ? 0 : fds.length;
    }
}
