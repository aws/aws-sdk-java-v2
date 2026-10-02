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
import static software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.defaultsmode.AutoDefaultsModeDiscovery;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Verifies the {@code AWS_ENABLE_DEFAULT_SOCKET_TIMEOUT_2026} rollout gate applied in
 * {@link AwsDefaultClientBuilder#resolveHttpClientConfig} to the codegen-baked
 * {@link SdkHttpConfigurationOption#SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT}.
 */
@ExtendWith(MockitoExtension.class)
class AwsDefaultClientBuilderReadWriteTimeoutTest {

    private static final String GATE_PROPERTY = SdkSystemSetting.AWS_ENABLE_DEFAULT_SOCKET_TIMEOUT_2026.property();
    private static final Duration PARTIAL_TIER = Duration.ofMinutes(15);
    private static final Duration FLAT_DEFAULT = Duration.ofMinutes(5);

    @Mock(lenient = true)
    private SdkHttpClient.Builder defaultHttpClientBuilder;

    @Mock(lenient = true)
    private SdkAsyncHttpClient.Builder defaultAsyncHttpClientFactory;

    @Mock(lenient = true)
    private AutoDefaultsModeDiscovery autoModeDiscovery;

    private String savedProperty;

    @BeforeEach
    void setup() {
        savedProperty = System.getProperty(GATE_PROPERTY);
        System.clearProperty(GATE_PROPERTY);
    }

    @AfterEach
    void teardown() {
        if (savedProperty != null) {
            System.setProperty(GATE_PROPERTY, savedProperty);
        } else {
            System.clearProperty(GATE_PROPERTY);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gateScenarios")
    void resolvesReadWriteTimeout(String scenario, String gateProperty, boolean codegenGateDefault,
                                  Duration bakedTier, Duration expected) {
        if (gateProperty != null) {
            System.setProperty(GATE_PROPERTY, gateProperty);
        }
        AttributeMap serviceHttpConfig = bakedTier == null ? AttributeMap.empty() : bakedServiceHttpConfig(bakedTier);

        AttributeMap resolved = resolvedServiceDefaults(serviceHttpConfig, codegenGateDefault);

        assertThat(resolved.get(SdkHttpConfigurationOption.SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT)).isEqualTo(expected);
    }

    @Test
    void unlistedService_gateOff_leavesOptionAbsent() {
        AttributeMap resolved = resolvedServiceDefaults(AttributeMap.empty(), false);

        assertThat(resolved.containsKey(SdkHttpConfigurationOption.SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT)).isFalse();
    }

    private static Stream<Arguments> gateScenarios() {
        return Stream.of(
            Arguments.of("gate off, partial tier baked -> overridden to ZERO", null, false, PARTIAL_TIER, Duration.ZERO),
            Arguments.of("gate on, unlisted service -> flat 5-minute default", "true", false, null, FLAT_DEFAULT),
            Arguments.of("gate on, fully-exempt tier baked -> ZERO", "true", false, Duration.ZERO, Duration.ZERO),
            Arguments.of("gate on, partial tier baked -> 15 minutes", "true", false, PARTIAL_TIER, PARTIAL_TIER),
            Arguments.of("gate on via codegen default, unlisted -> flat 5-minute default", null, true, null, FLAT_DEFAULT),
            Arguments.of("gate property false overrides codegen default -> ZERO", "false", true, PARTIAL_TIER, Duration.ZERO)
        );
    }

    private static AttributeMap bakedServiceHttpConfig(Duration tier) {
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.SDK_INTERNAL_FALLBACK_READ_WRITE_TIMEOUT, tier)
                           .build();
    }

    private AttributeMap resolvedServiceDefaults(AttributeMap bakedServiceHttpConfig, boolean codegenGateDefault) {
        AtomicReference<AttributeMap> captured = new AtomicReference<>();
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        new TestClientBuilder(bakedServiceHttpConfig, codegenGateDefault)
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .overrideConfiguration(overrideConfig)
            .region(Region.US_WEST_1)
            .httpClientBuilder((SdkHttpClient.Builder) serviceDefaults -> {
                captured.set(serviceDefaults);
                return mock(SdkHttpClient.class);
            })
            .build();

        return captured.get();
    }

    private static class TestClient {
    }

    private class TestClientBuilder extends AwsDefaultClientBuilder<TestClientBuilder, TestClient>
        implements AwsClientBuilder<TestClientBuilder, TestClient> {

        private final AttributeMap bakedServiceHttpConfig;
        private final boolean codegenGateDefault;

        TestClientBuilder(AttributeMap bakedServiceHttpConfig, boolean codegenGateDefault) {
            super(defaultHttpClientBuilder, defaultAsyncHttpClientFactory, autoModeDiscovery);
            this.bakedServiceHttpConfig = bakedServiceHttpConfig;
            this.codegenGateDefault = codegenGateDefault;
        }

        @Override
        protected TestClient buildClient() {
            syncClientConfiguration();
            return new TestClient();
        }

        @Override
        protected SdkClientConfiguration mergeInternalDefaults(SdkClientConfiguration config) {
            if (!codegenGateDefault) {
                return config;
            }
            return config.merge(c -> c.option(SdkClientOption.DEFAULT_ENABLE_SOCKET_TIMEOUT_2026, true));
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

        @Override
        protected AttributeMap serviceHttpConfig() {
            return bakedServiceHttpConfig;
        }

        @Override
        protected String serviceEndpointPrefix() {
            return "test";
        }

        @Override
        protected String signingName() {
            return "test";
        }

        @Override
        protected String serviceName() {
            return "test";
        }
    }
}
