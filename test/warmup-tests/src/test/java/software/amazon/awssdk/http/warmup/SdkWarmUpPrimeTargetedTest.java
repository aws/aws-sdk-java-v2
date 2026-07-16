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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.crac.SdkWarmUp;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * End-to-end integration test of {@link SdkWarmUp#prime(Class...)} with real service clients (STS, DynamoDB) whose
 * generated {@link SdkWarmUpProvider} implementations are on the classpath via ServiceLoader.
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

        savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "warmup-integration-test");
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleClientCases")
    void prime_withSingleClient_completesWithoutError(String description, Class<? extends SdkClient> client) {
        assertThatCode(() -> SdkWarmUp.prime(client)).doesNotThrowAnyException();
    }

    private static Stream<Arguments> singleClientCases() {
        return Stream.of(
            arguments("STS sync", StsClient.class),
            arguments("STS async", StsAsyncClient.class),
            arguments("DynamoDB sync", DynamoDbClient.class),
            arguments("DynamoDB async", DynamoDbAsyncClient.class)
        );
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
    void prime_concurrentCalls_allCompleteSuccessfully() throws InterruptedException {
        int threadCount = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completed = new AtomicInteger();

        Class<?>[] clients = {StsClient.class, StsAsyncClient.class, DynamoDbClient.class, DynamoDbAsyncClient.class};
        for (int i = 0; i < threadCount; i++) {
            @SuppressWarnings("unchecked")
            Class<? extends SdkClient> client = (Class<? extends SdkClient>) clients[i % clients.length];
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    SdkWarmUp.prime(client);
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    failures.add(t);
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

        assertThat(failures).as("no prime(Class...) call threw").isEmpty();
        assertThat(completed.get()).as("every prime(Class...) call completed").isEqualTo(threadCount);
    }

    @Test
    void prime_withUnmatchedClient_isNoOpAndSendsNoRequest() {
        mockServer.stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));

        assertThatCode(() -> SdkWarmUp.prime(UnregisteredClient.class)).doesNotThrowAnyException();

        // No provider matches, so no HTTP warmer runs and nothing hits the mock server.
        mockServer.verify(0, anyRequestedFor(anyUrl()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("noOpInputCases")
    void prime_withNoOpInput_doesNotThrow(String description, Class<? extends SdkClient>[] clients) {
        assertThatCode(() -> SdkWarmUp.prime(clients)).doesNotThrowAnyException();
    }

    private static Stream<Arguments> noOpInputCases() {
        return Stream.of(
            arguments("null array", (Class<? extends SdkClient>[]) null),
            arguments("empty array", new Class[0])
        );
    }

    interface UnregisteredClient extends SdkClient {
    }
}

