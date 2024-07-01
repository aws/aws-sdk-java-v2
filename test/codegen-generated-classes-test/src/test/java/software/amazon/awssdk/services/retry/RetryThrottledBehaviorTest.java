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

package software.amazon.awssdk.services.retry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.internal.backoff.ExponentialDelayWithJitter;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

public abstract class RetryThrottledBehaviorTest<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {
    static ComputedNextInt RANDOM;
    static final Duration THROTTLED_BASE_DELAY = Duration.ofMillis(200);
    static final Duration THROTTLED_MAX_DELAY = Duration.ofSeconds(20);

    protected WireMockServer wireMock = new WireMockServer(0);

    protected abstract BuilderT newClientBuilder();

    protected abstract AllTypesResponse callAllTypes(ClientT client);

    private BuilderT clientBuilder() {
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        return newClientBuilder()
            .credentialsProvider(credentialsProvider)
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    @Test
    public void retryStrategyThrottlingBehavior() {
        RetryStrategy retryStrategy = SdkDefaultRetryStrategy
            .forRetryMode(RetryMode.STANDARD)
            .toBuilder()
            .backoffStrategy(BackoffStrategy.retryImmediately())
            .throttlingBackoffStrategy(new ExponentialDelayWithJitter(() -> RANDOM,
                                                                      THROTTLED_BASE_DELAY,
                                                                      THROTTLED_MAX_DELAY))
            .build();

        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryStrategy(retryStrategy))
            .build();
        stubForThrottling();
        verifyThrottling(client);
    }

    @Test
    public void retryStrategyNonThrottlingBehavior() {
        RetryStrategy retryStrategy = SdkDefaultRetryStrategy
            .forRetryMode(RetryMode.STANDARD)
            .toBuilder()
            .backoffStrategy(BackoffStrategy.retryImmediately())
            .throttlingBackoffStrategy(new ExponentialDelayWithJitter(() -> RANDOM,
                                                                      THROTTLED_BASE_DELAY,
                                                                      THROTTLED_MAX_DELAY))
            .build();

        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryStrategy(retryStrategy))
            .build();
        stubForNonThrottling();
        verifyNonThrottling(client);
    }

    @Test
    public void retryPolicyThrottlingBehavior() {
        RetryPolicy retryPolicy = RetryPolicy.forRetryMode(RetryMode.STANDARD)
                                             .toBuilder()
                                             .backoffStrategy(software.amazon.awssdk.core.retry.backoff.BackoffStrategy.none())
                                             .throttlingBackoffStrategy(createFullJitterBackoffStrategy(THROTTLED_BASE_DELAY,
                                                                                                        THROTTLED_MAX_DELAY,
                                                                                                        RANDOM))
                                             .build();
        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryPolicy(retryPolicy))
            .build();
        stubForThrottling();
        verifyThrottling(client);
    }

    @Test
    public void retryPolicyNonThrottlingBehavior() {
        RetryPolicy retryPolicy = RetryPolicy.forRetryMode(RetryMode.STANDARD)
                                             .toBuilder()
                                             .backoffStrategy(software.amazon.awssdk.core.retry.backoff.BackoffStrategy.none())
                                             .throttlingBackoffStrategy(createFullJitterBackoffStrategy(THROTTLED_BASE_DELAY,
                                                                                                        THROTTLED_MAX_DELAY,
                                                                                                        RANDOM))
                                             .build();
        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryPolicy(retryPolicy))
            .build();
        stubForNonThrottling();
        verifyNonThrottling(client);
    }

    void verifyThrottling(ClientT client) {
        Instant start = Instant.now();
        assertThrows(Exception.class, () -> callAllTypes(client));
        Instant end = Instant.now();
        Duration elapsed = Duration.between(start, end);
        assertTrue(elapsed.toMillis() >= THROTTLED_BASE_DELAY.multipliedBy(3).toMillis());
        assertEquals(2, RANDOM.invocations.get());
        verifyRequestCount(3);
    }

    void verifyNonThrottling(ClientT client) {
        Instant start = Instant.now();
        assertThrows(Exception.class, () -> callAllTypes(client));
        Instant end = Instant.now();
        Duration elapsed = Duration.between(start, end);
        assertTrue(elapsed.toMillis() < THROTTLED_BASE_DELAY.multipliedBy(3).toMillis());
        assertEquals(0, RANDOM.invocations.get());
        verifyRequestCount(3);
    }

    @BeforeEach
    private void beforeEach() {
        RANDOM = new ComputedNextInt(bound -> bound - 1);
        wireMock.start();
    }

    @AfterEach
    private void afterEach() {
        wireMock.stop();
    }

    private void stubForThrottling() {
        wireMock.stubFor(post(anyUrl())
                             .willReturn(aResponse().withStatus(429)));
    }

    private void stubForNonThrottling() {
        wireMock.stubFor(post(anyUrl())
                             .willReturn(aResponse().withStatus(500)));
    }

    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }

    static class SyncRunner extends RetryThrottledBehaviorTest<ProtocolRestJsonClient, ProtocolRestJsonClientBuilder> {
        @Override
        protected ProtocolRestJsonClientBuilder newClientBuilder() {
            return ProtocolRestJsonClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonClient client) {
            AllTypesRequest.Builder requestBuilder = AllTypesRequest.builder();
            return client.allTypes(requestBuilder.build());
        }
    }

    static class AsyncRunner extends RetryThrottledBehaviorTest<ProtocolRestJsonAsyncClient, ProtocolRestJsonAsyncClientBuilder> {
        @Override
        protected ProtocolRestJsonAsyncClientBuilder newClientBuilder() {
            return ProtocolRestJsonAsyncClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonAsyncClient client) {
            try {
                AllTypesRequest.Builder requestBuilder = AllTypesRequest.builder();
                return client.allTypes(requestBuilder.build()).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw e;
            }
        }
    }

    static class ComputedNextInt extends Random {
        final Function<Integer, Integer> compute;
        final AtomicInteger invocations = new AtomicInteger(0);

        ComputedNextInt(Function<Integer, Integer> compute) {
            this.compute = compute;
        }

        @Override
        public int nextInt(int bound) {
            invocations.incrementAndGet();
            return compute.apply(bound);
        }
    }

    static FullJitterBackoffStrategy createFullJitterBackoffStrategy(Duration base, Duration max, Random random) {
        try {
            Constructor<FullJitterBackoffStrategy> ctor = FullJitterBackoffStrategy.class.getDeclaredConstructor(Duration.class,
                                                                                                                 Duration.class,
                                                                                                                 Random.class);
            ctor.setAccessible(true);
            return ctor.newInstance(base, max, random);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
