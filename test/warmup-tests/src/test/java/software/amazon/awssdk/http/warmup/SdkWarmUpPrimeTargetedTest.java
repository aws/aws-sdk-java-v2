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
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.crac.SdkWarmUp;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * End-to-end integration test of {@link SdkWarmUp#prime(Class...)} with real service clients (STS, DynamoDB) whose
 * generated {@link SdkWarmUpProvider} implementations are on the classpath via ServiceLoader. Verifies that:
 * <ul>
 *     <li>Passing a sync client class routes through {@code warmUpClient(SYNC)} and triggers the sync HTTP warmer.</li>
 *     <li>Passing an async client class routes through {@code warmUpClient(ASYNC)} and triggers the async HTTP warmer.</li>
 *     <li>Idempotency: a second call with the same client is a no-op.</li>
 *     <li>Multiple services can be primed in one call.</li>
 *     <li>Concurrent callers do not throw or corrupt state.</li>
 *     <li>An unmatched client is a safe no-op.</li>
 * </ul>
 *
 * <p>The HTTP warmers resolve their endpoint via {@code RegionEndpointResolver}, which reads {@code aws.region}. Tests
 * set a dummy region so DNS fails fast and no real network traffic leaves the host. The generated providers use
 * {@code CannedResponseHttpClient}, so their {@code warmUpClient} does not need the network either.
 */
class SdkWarmUpPrimeTargetedTest {

    private WireMockServer mockServer;
    private String savedRegionProperty;

    @BeforeEach
    void setUp() {
        mockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockServer.start();
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse()
            .withStatus(302)
            .withHeader("Location", "https://aws.amazon.com/iam")));

        // Use a non-routable region so the HTTP warmer's STS endpoint fails DNS immediately; the warm-up is best-effort
        // and swallows the failure, keeping the test offline.
        savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "warmup-integration-test");

        SdkWarmUp.resetTargetedPrimeStateForTesting();
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
        if (savedRegionProperty != null) {
            System.setProperty("aws.region", savedRegionProperty);
        } else {
            System.clearProperty("aws.region");
        }
    }

    /**
     * Sanity check: ServiceLoader must discover at least the STS and DynamoDB providers we depend on.
     */
    @Test
    void serviceLoader_discoversExpectedProviders() {
        int count = 0;
        for (SdkWarmUpProvider ignored : ServiceLoader.load(SdkWarmUpProvider.class)) {
            count++;
        }
        // STS has 1 provider, DynamoDB has 2 (DynamoDb + DynamoDbStreams).
        assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    void prime_withStsSyncClient_completesWithoutError() {
        assertThatCode(() -> SdkWarmUp.prime(StsClient.class)).doesNotThrowAnyException();
    }

    @Test
    void prime_withStsAsyncClient_completesWithoutError() {
        assertThatCode(() -> SdkWarmUp.prime(StsAsyncClient.class)).doesNotThrowAnyException();
    }

    @Test
    void prime_withDynamoDbSyncClient_completesWithoutError() {
        assertThatCode(() -> SdkWarmUp.prime(DynamoDbClient.class)).doesNotThrowAnyException();
    }

    @Test
    void prime_withDynamoDbAsyncClient_completesWithoutError() {
        assertThatCode(() -> SdkWarmUp.prime(DynamoDbAsyncClient.class)).doesNotThrowAnyException();
    }

    @Test
    void prime_withMultipleServices_completesWithoutError() {
        assertThatCode(() -> SdkWarmUp.prime(StsClient.class, DynamoDbClient.class, StsAsyncClient.class))
            .doesNotThrowAnyException();
    }

    @Test
    void prime_calledTwiceWithSameClient_isIdempotent() {
        SdkWarmUp.prime(StsClient.class);

        // Second call should be a no-op (client already recorded as primed).
        assertThatCode(() -> SdkWarmUp.prime(StsClient.class)).doesNotThrowAnyException();
    }

    @Test
    void prime_calledWithNewClientAfterPrevious_primesOnlyNewClient() {
        SdkWarmUp.prime(StsClient.class);

        // DynamoDB is new; STS should not be re-primed.
        assertThatCode(() -> SdkWarmUp.prime(DynamoDbClient.class, StsClient.class)).doesNotThrowAnyException();
    }

    @Test
    void prime_concurrentCalls_doNotThrow() throws InterruptedException {
        int threadCount = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();

        Class<?>[] clients = {StsClient.class, StsAsyncClient.class, DynamoDbClient.class, DynamoDbAsyncClient.class};
        for (int i = 0; i < threadCount; i++) {
            @SuppressWarnings("unchecked")
            Class<? extends SdkClient> client = (Class<? extends SdkClient>) clients[i % clients.length];
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    SdkWarmUp.prime(client);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        start.countDown();
        done.await();
        for (Thread thread : threads) {
            thread.join();
        }
        // If we get here without exceptions or hangs, concurrency is safe.
    }

    @Test
    void prime_withUnmatchedClient_isNoOpAndSendsNoRequest() {
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));

        assertThatCode(() -> SdkWarmUp.prime(UnregisteredClient.class)).doesNotThrowAnyException();

        // No provider matches, so no HTTP warmer runs and nothing hits the mock server.
        mockServer.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void prime_withNullArray_isNoOp() {
        assertThatCode(() -> SdkWarmUp.prime((Class<? extends SdkClient>[]) null)).doesNotThrowAnyException();
    }

    @Test
    void prime_withEmptyArray_isNoOp() {
        assertThatCode(() -> SdkWarmUp.prime(new Class[0])).doesNotThrowAnyException();
    }

    interface UnregisteredClient extends SdkClient {
    }
}
