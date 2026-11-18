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

package software.amazon.awssdk.awscore.client.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_STRATEGY;

import java.net.URI;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class InternalDefaultsTest {
    private static final String OLD_GATE_PROPERTY = "aws.newRetries2026";
    private static final String OLD_GATE_ENV_VAR = "AWS_NEW_RETRIES_2026";
    private static final Duration SUGGESTED_DELAY = Duration.ofSeconds(60);

    @BeforeEach
    @AfterEach
    void cleanup() {
        System.clearProperty(OLD_GATE_PROPERTY);
    }

    @ParameterizedTest(name = "system prop = {0}, env var = {1}, default cfg = {2}, default mode = {3}, expected = {4}")
    @MethodSource("retrySettings")
    void buildClient_resolvesExpectedRetryStrategy(String systemProperty, String environmentVariable, Boolean defaultConfig,
                                                   RetryMode defaultRetryMode, Class<?> retryStrategyClass) {
        EnvironmentVariableHelper.run((env) -> {
            if (environmentVariable != null) {
                env.set(OLD_GATE_ENV_VAR, environmentVariable);
            }

            if (systemProperty != null) {
                System.setProperty(OLD_GATE_PROPERTY, systemProperty);
            }

            TestClient sync = new TestClientBuilder(true)
                .newRetries2026Default(defaultConfig)
                .defaultRetryMode(defaultRetryMode)
                .buildClient();

            TestClient async = new TestClientBuilder(false)
                .newRetries2026Default(defaultConfig)
                .defaultRetryMode(defaultRetryMode)
                .buildClient();

            RetryStrategy syncStrategy = sync.clientConfiguration.option(RETRY_STRATEGY);
            RetryStrategy asyncStrategy = async.clientConfiguration.option(RETRY_STRATEGY);
            assertThat(syncStrategy).isInstanceOf(retryStrategyClass);
            assertThat(asyncStrategy).isInstanceOf(retryStrategyClass);

            if (retryStrategyClass == StandardRetryStrategy.class) {
                // Retry behavior 2.1: 3 attempts, and a suggested delay is clamped to at most the backoff + 5 seconds
                assertRetries21(syncStrategy);
                assertRetries21(asyncStrategy);
            } else {
                assertThat(syncStrategy.maxAttempts()).isEqualTo(4);
                assertThat(refreshDelay(syncStrategy)).isEqualTo(SUGGESTED_DELAY);
            }
        });
    }

    private static void assertRetries21(RetryStrategy strategy) {
        assertThat(strategy.maxAttempts()).isEqualTo(3);
        assertThat(refreshDelay(strategy)).isBetween(Duration.ofSeconds(5), Duration.ofSeconds(5).plusMillis(50));
    }

    private static Duration refreshDelay(RetryStrategy strategy) {
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
        RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                   .token(token)
                                                                   .failure(SdkServiceException.builder()
                                                                                               .statusCode(500)
                                                                                               .build())
                                                                   .suggestedDelay(SUGGESTED_DELAY)
                                                                   .build();
        return strategy.refreshRetryToken(refresh).delay();
    }

    // system property, environment variable, default config, default retry mode, expected retry strategy
    static Stream<Arguments> retrySettings() {
        return Stream.of(
            Arguments.of(null, null, null, null, StandardRetryStrategy.class),

            // The removed retry behavior 2.1 opt-in setting is ignored
            Arguments.of("true", null, null, null, StandardRetryStrategy.class),
            Arguments.of("false", null, null, null, StandardRetryStrategy.class),
            Arguments.of(null, "true", null, null, StandardRetryStrategy.class),
            Arguments.of(null, "false", null, null, StandardRetryStrategy.class),
            Arguments.of(null, null, true, null, StandardRetryStrategy.class),
            Arguments.of(null, null, false, null, StandardRetryStrategy.class),
            Arguments.of("false", "false", false, null, StandardRetryStrategy.class),

            // A service default retry mode is honored
            Arguments.of(null, null, null, RetryMode.STANDARD, StandardRetryStrategy.class),
            Arguments.of(null, null, null, RetryMode.LEGACY, LegacyRetryStrategy.class),
            Arguments.of("true", null, true, RetryMode.LEGACY, LegacyRetryStrategy.class)
            );
    }

    private static class TestClient {
        private final SdkClientConfiguration clientConfiguration;

        public TestClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
        }
    }

    private static class TestClientBuilder extends AwsDefaultClientBuilder<TestClientBuilder, TestClient> {
        private final boolean sync;
        private Boolean newRetries2026Default;
        private RetryMode defaultRetryMode;

        protected TestClientBuilder(boolean sync) {
            super(mock(SdkHttpClient.Builder.class), mock(SdkAsyncHttpClient.Builder.class), null);
            this.sync = sync;
        }

        public TestClientBuilder newRetries2026Default(Boolean newRetries2026Default) {
            this.newRetries2026Default = newRetries2026Default;
            return this;
        }

        public TestClientBuilder defaultRetryMode(RetryMode defaultRetryMode) {
            this.defaultRetryMode = defaultRetryMode;
            return this;
        }

        @Override
        protected String serviceEndpointPrefix() {
            return "test-client";
        }

        @Override
        protected String signingName() {
            return "test-client";
        }

        @Override
        protected String serviceName() {
            return "test-client";
        }

        @Override
        @SuppressWarnings("deprecation")
        protected final SdkClientConfiguration mergeInternalDefaults(SdkClientConfiguration config) {
            return config.merge(c -> {
                c.option(SdkClientOption.DEFAULT_NEW_RETRIES_2026, newRetries2026Default);
                c.option(SdkClientOption.DEFAULT_RETRY_MODE, defaultRetryMode);
            });
        }

        @Override
        protected TestClient buildClient() {
            SdkClientConfiguration config;
            if (sync) {
                config = syncClientConfiguration();
            } else {
                config = asyncClientConfiguration();
            }

            return new TestClient(config);
        }

        @Override
        protected SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
            return config.toBuilder()
                         .lazyOptionIfAbsent(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, c -> {
                             URI endpoint = URI.create("https://" + serviceEndpointPrefix() + "."
                                                      + c.get(AwsClientOption.AWS_REGION) + ".amazonaws.com");
                             return ClientEndpointProvider.create(endpoint, false);
                         })
                         .build();
        }
    }
}
