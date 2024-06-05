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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.retry.AwsRetryPolicy;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.retry.RetryPolicyAdapter;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.utils.StringInputStream;

public abstract class BaseRetrySetupTest<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {

    protected WireMockServer wireMock = new WireMockServer(0);

    protected abstract BuilderT newClientBuilder();

    protected abstract AllTypesResponse callAllTypes(ClientT client, List<SdkPlugin> requestPlugins);

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("allScenarios")
    public void testAllScenarios(RetryScenario scenario) {
        stubThrottlingResponse();
        setupScenarioBefore(scenario);
        ClientT client = setupClientBuilder(scenario).build();
        List<SdkPlugin> requestPlugins = setupRequestPlugins(scenario);
        assertThatThrownBy(() -> callAllTypes(client, requestPlugins))
            .isInstanceOf(SdkException.class);
        verifyRequestCount(expectedCount(scenario.mode()));
    }

    private BuilderT setupClientBuilder(RetryScenario scenario) {
        BuilderT builder = clientBuilder();
        RetryImplementation kind = scenario.retryImplementation();
        if (kind == RetryImplementation.POLICY) {
            setupRetryPolicy(builder, scenario);
        } else if (kind == RetryImplementation.STRATEGY) {
            setupRetryStrategy(builder, scenario);
        } else {
            throw new IllegalArgumentException();
        }
        return builder;
    }

    private void setupRetryPolicy(BuilderT builder, RetryScenario scenario) {
        RetryMode mode = scenario.mode();
        RetryModeSetup setup = scenario.setup();
        switch (setup) {
            case PROFILE_USING_MODE:
                setupProfile(builder, scenario);
                break;
            case CLIENT_OVERRIDE_USING_MODE:
                builder.overrideConfiguration(o -> o.retryPolicy(mode));
                break;
            case CLIENT_OVERRIDE_USING_INSTANCE:
                builder.overrideConfiguration(o -> o.retryPolicy(AwsRetryPolicy.forRetryMode(mode)));
                break;
            case CLIENT_PLUGIN_OVERRIDE_USING_INSTANCE:
            case CLIENT_PLUGIN_OVERRIDE_USING_MODE:
                builder.addPlugin(new ConfigureRetryScenario(scenario));
                break;
        }
    }

    private void setupRetryStrategy(BuilderT builder, RetryScenario scenario) {
        RetryMode mode = scenario.mode();
        // Note, we don't setup the request level plugins, those need to be added at request time and not when we build the
        // client.
        switch (scenario.setup()) {
            case PROFILE_USING_MODE:
                setupProfile(builder, scenario);
                break;
            case CLIENT_OVERRIDE_USING_MODE:
                builder.overrideConfiguration(o -> o.retryStrategy(mode));
                break;
            case CLIENT_OVERRIDE_USING_INSTANCE:
                builder.overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.forRetryMode(mode)));
                break;
            case CLIENT_PLUGIN_OVERRIDE_USING_INSTANCE:
            case CLIENT_PLUGIN_OVERRIDE_USING_MODE:
                builder.addPlugin(new ConfigureRetryScenario(scenario));
                break;
        }
    }

    private void setupProfile(BuilderT builder, RetryScenario scenario) {
        String modeName = scenario.modeExternalName();
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile retry_test]\n" +
                                                                            "retry_mode = " + modeName))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        builder.overrideConfiguration(o -> o.defaultProfileFile(profileFile)
                                            .defaultProfileName("retry_test")).build();

    }

    private List<SdkPlugin> setupRequestPlugins(RetryScenario scenario) {
        List<SdkPlugin> plugins = new ArrayList<>();
        RetryModeSetup setup = scenario.setup();
        if (setup == RetryModeSetup.REQUEST_PLUGIN_OVERRIDE_USING_MODE
            || setup == RetryModeSetup.REQUEST_PLUGIN_OVERRIDE_USING_INSTANCE) {
            plugins.add(new ConfigureRetryScenario(scenario));
        }
        // Plugin to validate the scenarios, must go after plugin to the configure the retry
        // scenario.
        plugins.add(new ValidateRetryScenario(scenario));
        return plugins;
    }

    private BuilderT clientBuilder() {
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        return newClientBuilder().credentialsProvider(credentialsProvider)
                                 .region(Region.US_EAST_1)
                                 .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    private void setupScenarioBefore(RetryScenario scenario) {
        if (scenario.setup() == RetryModeSetup.SYSTEM_PROPERTY_USING_MODE) {
            System.setProperty("aws.retryMode", scenario.modeExternalName());
        }
    }

    @BeforeEach
    private void beforeEach() {
        wireMock.start();
    }

    @AfterEach
    private void afterEach() {
        System.clearProperty("aws.retryMode");
        wireMock.stop();
    }

    private static int expectedCount(RetryMode mode) {
        switch (mode) {
            case ADAPTIVE:
            case ADAPTIVE_V2:
            case STANDARD:
                return 3;
            case LEGACY:
                return 4;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }

    private void stubThrottlingResponse() {
        wireMock.stubFor(post(anyUrl())
                             .willReturn(aResponse().withStatus(429)));
    }

    /**
     * For each base scenario we add each possible setup of the retry mode.
     */
    private static List<RetryScenario> allScenarios() {
        List<RetryScenario> result = new ArrayList<>();
        for (RetryScenario scenario : baseScenarios()) {
            for (RetryModeSetup setupMode : RetryModeSetup.values()) {
                RetryScenario newScenario = scenario.toBuilder().setup(setupMode).build();
                if (isSupportedScenario(newScenario)) {
                    result.add(newScenario);
                }
            }
        }
        return result;
    }

    /**
     * Not all scenarios are supported, this methods filter those that are not.
     */
    private static boolean isSupportedScenario(RetryScenario scenario) {
        // Profile now only returns strategies, not policies, except for ADAPTIVE mode for which an adapter
        // is used. That case is tested using RetryImplementation.STRATEGY.
        if (scenario.retryImplementation() == RetryImplementation.POLICY
            && scenario.setup() == RetryModeSetup.PROFILE_USING_MODE) {
            return false;
        }

        // Using system properties  only returns strategies, not policies, except for ADAPTIVE mode for
        // which an adapter is used. That case is tested using RetryImplementation.STRATEGY.
        if (scenario.retryImplementation() == RetryImplementation.POLICY
            && scenario.setup() == RetryModeSetup.SYSTEM_PROPERTY_USING_MODE) {
            return false;
        }

        // System property or profile do not support the internal "adaptive_v2" name, only adaptive,
        // and it's mapped to adaptive_v2. We mark here adaptive using profile or system property
        // and map in the tests "adaptive_v2" to "adaptive" such that everything comes together at
        // the end.
        if (scenario.mode() == RetryMode.ADAPTIVE
            && (scenario.setup() == RetryModeSetup.PROFILE_USING_MODE
                || scenario.setup() == RetryModeSetup.SYSTEM_PROPERTY_USING_MODE)) {
            return false;
        }

        // Retry policies only support the legacy ADAPTIVE mode.
        if (scenario.retryImplementation() == RetryImplementation.POLICY
            && scenario.mode() == RetryMode.ADAPTIVE_V2) {
            return false;
        }

        return true;
    }

    /**
     * Base retry scenarios.
     */
    private static List<RetryScenario> baseScenarios() {
        return Arrays.asList(
            // Retry Policy
            RetryScenario.builder()
                         .mode(RetryMode.LEGACY)
                         .retryImplementation(RetryImplementation.POLICY)
                         .expectedClass(RetryPolicy.class)
                         .build()
            , RetryScenario.builder()
                           .mode(RetryMode.STANDARD)
                           .retryImplementation(RetryImplementation.POLICY)
                           .expectedClass(RetryPolicy.class)
                           .build()
            , RetryScenario.builder()
                           .mode(RetryMode.ADAPTIVE)
                           .retryImplementation(RetryImplementation.POLICY)
                           .expectedClass(RetryPolicy.class)
                           .build()
            // Retry Strategy
            , RetryScenario.builder()
                           .mode(RetryMode.LEGACY)
                           .retryImplementation(RetryImplementation.STRATEGY)
                           .expectedClass(LegacyRetryStrategy.class)
                           .build()
            , RetryScenario.builder()
                           .mode(RetryMode.STANDARD)
                           .retryImplementation(RetryImplementation.STRATEGY)
                           .expectedClass(StandardRetryStrategy.class)
                           .build()
            , RetryScenario.builder()
                           .mode(RetryMode.ADAPTIVE)
                           .retryImplementation(RetryImplementation.STRATEGY)
                           .expectedClass(RetryPolicyAdapter.class)
                           .build()
            , RetryScenario.builder()
                           .mode(RetryMode.ADAPTIVE_V2)
                           .retryImplementation(RetryImplementation.STRATEGY)
                           .expectedClass(AdaptiveRetryStrategy.class)
                           .build()
        );
    }

    static class RetryScenario {
        private final RetryMode mode;
        private final Class<?> expectedClass;
        private final RetryModeSetup setup;
        private final RetryImplementation retryImplementation;

        RetryScenario(Builder builder) {
            this.mode = builder.mode;
            this.expectedClass = builder.expectedClass;
            this.setup = builder.setup;
            this.retryImplementation = builder.retryImplementation;
        }

        public RetryMode mode() {
            return mode;
        }

        public Class<?> expectedClass() {
            return expectedClass;
        }

        public RetryModeSetup setup() {
            return setup;
        }

        public RetryImplementation retryImplementation() {
            return retryImplementation;
        }

        public Builder toBuilder() {
            return new Builder(this);
        }

        /**
         * Returns the name used externally of the given mode. This name is used in the profile `retry_mode` setting or in the
         * system property. Externally, "adaptive" gets mapped to RetryMode.ADAPTIVE_V2, and "adaptive_v2" an internal name
         * only and not supported externally.
         */
        public String modeExternalName() {
            switch (mode) {
                case ADAPTIVE:
                case ADAPTIVE_V2:
                    return "adaptive";
                case LEGACY:
                    return "legacy";
                case STANDARD:
                    return "standard";
                default:
                    throw new RuntimeException("Unsupported mode: " + mode);
            }
        }

        @Override
        public String toString() {
            return mode + " " + retryImplementation + " " + setup;
        }

        public static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private RetryMode mode;
            private Class<?> expectedClass;
            private RetryModeSetup setup;
            private RetryImplementation retryImplementation;

            public Builder() {
            }

            public Builder(RetryScenario retrySetup) {
                this.mode = retrySetup.mode;
                this.expectedClass = retrySetup.expectedClass;
                this.setup = retrySetup.setup;
                this.retryImplementation = retrySetup.retryImplementation;
            }

            public Builder mode(RetryMode mode) {
                this.mode = mode;
                return this;
            }

            public Builder expectedClass(Class<?> expectedClass) {
                this.expectedClass = expectedClass;
                return this;
            }

            public Builder setup(RetryModeSetup setup) {
                this.setup = setup;
                return this;
            }

            public Builder retryImplementation(RetryImplementation retryImplementation) {
                this.retryImplementation = retryImplementation;
                return this;
            }

            public RetryScenario build() {
                return new RetryScenario(this);
            }
        }
    }

    enum RetryModeSetup {
        CLIENT_OVERRIDE_USING_MODE,
        CLIENT_OVERRIDE_USING_INSTANCE,
        CLIENT_PLUGIN_OVERRIDE_USING_MODE,
        CLIENT_PLUGIN_OVERRIDE_USING_INSTANCE,
        REQUEST_PLUGIN_OVERRIDE_USING_MODE,
        REQUEST_PLUGIN_OVERRIDE_USING_INSTANCE,
        PROFILE_USING_MODE,
        SYSTEM_PROPERTY_USING_MODE,
    }

    enum RetryImplementation {
        POLICY, STRATEGY
    }

    static class ConfigureRetryScenario implements SdkPlugin {
        private RetryScenario scenario;

        ConfigureRetryScenario(RetryScenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public void configureClient(SdkServiceClientConfiguration.Builder config) {
            RetryModeSetup setup = scenario.setup();
            if (setup == RetryModeSetup.CLIENT_PLUGIN_OVERRIDE_USING_MODE
                || setup == RetryModeSetup.REQUEST_PLUGIN_OVERRIDE_USING_MODE) {
                if (scenario.retryImplementation() == RetryImplementation.POLICY) {
                    config.overrideConfiguration(o -> o.retryPolicy(scenario.mode()));
                } else if (scenario.retryImplementation() == RetryImplementation.STRATEGY) {
                    config.overrideConfiguration(o -> o.retryStrategy(scenario.mode()));
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (setup == RetryModeSetup.CLIENT_PLUGIN_OVERRIDE_USING_INSTANCE
                       || setup == RetryModeSetup.REQUEST_PLUGIN_OVERRIDE_USING_INSTANCE) {
                if (scenario.retryImplementation() == RetryImplementation.POLICY) {
                    config.overrideConfiguration(o -> o.retryPolicy(AwsRetryPolicy.forRetryMode(scenario.mode())));
                } else if (scenario.retryImplementation() == RetryImplementation.STRATEGY) {
                    config.overrideConfiguration(o -> o.retryStrategy(AwsRetryStrategy.forRetryMode(scenario.mode())));
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    static class ValidateRetryScenario implements SdkPlugin {
        private RetryScenario scenario;

        public ValidateRetryScenario(RetryScenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public void configureClient(SdkServiceClientConfiguration.Builder config) {
            if (scenario.retryImplementation() == RetryImplementation.POLICY) {
                assertThat(config.overrideConfiguration().retryPolicy()).isNotEmpty();
                RetryPolicy policy = config.overrideConfiguration().retryPolicy().get();
                assertThat(policy.retryMode()).isEqualTo(scenario.mode());
                assertThat(policy).isInstanceOf(scenario.expectedClass());
            } else if (scenario.retryImplementation() == RetryImplementation.STRATEGY) {
                assertThat(config.overrideConfiguration().retryPolicy()).isEmpty();
                assertThat(config.overrideConfiguration().retryStrategy()).isNotEmpty();
                RetryStrategy strategy = config.overrideConfiguration().retryStrategy().get();
                assertThat(SdkDefaultRetryStrategy.retryMode(strategy)).isEqualTo(scenario.mode());
                assertThat(strategy).isInstanceOf(scenario.expectedClass());
            }
        }
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("boom!");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }
    }
}
