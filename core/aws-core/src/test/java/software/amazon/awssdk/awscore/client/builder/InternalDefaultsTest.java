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
import static software.amazon.awssdk.core.client.config.SdkClientOption.NEW_RETRIES_2026_ENABLED;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_STRATEGY;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class InternalDefaultsTest {
    private static String newRetries2026Save;

    @BeforeAll
    static void setup() {
        newRetries2026Save = System.getProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
    }

    @BeforeEach
    void methodSetup() {
        System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
    }

    @AfterAll
    static void teardown() {
        if (newRetries2026Save != null) {
            System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), newRetries2026Save);
        } else {
            System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        }
    }

    @ParameterizedTest(name = "system prop = {0}, env var = {1}, default cfg = {2}, expected = {3}")
    @MethodSource("newRetries2026Settings")
    void buildClient_precedenceIsCorrect(String systemProperty, String environmentVariable, Boolean defaultConfig,
                                         Class<?> retryStrategyClass, boolean newRetries2026Enabled) {
        EnvironmentVariableHelper.run((env) -> {
            if (environmentVariable != null) {
                env.set(SdkSystemSetting.AWS_NEW_RETRIES_2026.environmentVariable(), environmentVariable);
            }

            if (systemProperty != null) {
                System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), systemProperty);
            }

            TestClient sync = new TestClientBuilder(true)
                .newRetries2026Default(defaultConfig)
                .buildClient();

            TestClient async = new TestClientBuilder(false)
                .newRetries2026Default(defaultConfig)
                .buildClient();

            assertThat(sync.clientConfiguration.option(RETRY_STRATEGY)).isInstanceOf(retryStrategyClass);
            assertThat(async.clientConfiguration.option(RETRY_STRATEGY)).isInstanceOf(retryStrategyClass);

            assertThat(sync.clientConfiguration.option(NEW_RETRIES_2026_ENABLED)).isEqualTo(newRetries2026Enabled);
            assertThat(async.clientConfiguration.option(NEW_RETRIES_2026_ENABLED)).isEqualTo(newRetries2026Enabled);
        });
    }

    // system property, environment variable, default config, expected retry strategy
    static Stream<Arguments> newRetries2026Settings() {
        return Stream.of(
            Arguments.of(null, null, null, LegacyRetryStrategy.class, false),

            Arguments.of("true", null, null, StandardRetryStrategy.class, true),
            Arguments.of("false", null, null, LegacyRetryStrategy.class, false),
            Arguments.of(null, "true", null, StandardRetryStrategy.class, true),
            Arguments.of(null, "false", null, LegacyRetryStrategy.class, false),
            Arguments.of(null, null, true, StandardRetryStrategy.class, true),
            Arguments.of(null, null, false, LegacyRetryStrategy.class, false),

            Arguments.of("true", null, false, StandardRetryStrategy.class, true),
            Arguments.of(null, "true", false, StandardRetryStrategy.class, true)
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

        protected TestClientBuilder(boolean sync) {
            super(mock(SdkHttpClient.Builder.class), mock(SdkAsyncHttpClient.Builder.class), null);
            this.sync = sync;
        }

        public TestClientBuilder newRetries2026Default(Boolean newRetries2026Default) {
            this.newRetries2026Default = newRetries2026Default;
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
        protected final SdkClientConfiguration mergeInternalDefaults(SdkClientConfiguration config) {
            return config.merge(c -> {
                c.option(SdkClientOption.DEFAULT_NEW_RETRIES_2026, newRetries2026Default);
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
    }
}
