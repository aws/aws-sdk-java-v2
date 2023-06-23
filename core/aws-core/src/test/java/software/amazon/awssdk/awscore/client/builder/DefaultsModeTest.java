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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION;
import static software.amazon.awssdk.awscore.client.config.AwsClientOption.DEFAULTS_MODE;
import static software.amazon.awssdk.core.client.config.SdkClientOption.DEFAULT_RETRY_MODE;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_POLICY;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_STRATEGY;
import static software.amazon.awssdk.regions.ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT;

import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.awscore.internal.defaultsmode.AutoDefaultsModeDiscovery;
import software.amazon.awssdk.awscore.internal.defaultsmode.DefaultsModeConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

@RunWith(MockitoJUnitRunner.class)
public class DefaultsModeTest {

    private static final AttributeMap SERVICE_DEFAULTS = AttributeMap
        .builder()
        .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(10))
        .build();

    private static final String ENDPOINT_PREFIX = "test";
    private static final String SIGNING_NAME = "test";
    private static final String SERVICE_NAME = "test";

    @Mock
    private SdkHttpClient.Builder defaultHttpClientBuilder;

    @Mock
    private SdkAsyncHttpClient.Builder defaultAsyncHttpClientBuilder;

    @Mock
    private AutoDefaultsModeDiscovery autoModeDiscovery;

    @Test
    public void defaultClient_shouldUseLegacyModeWithExistingDefaults() {
        TestClient client = testClientBuilder()
            .region(Region.US_WEST_2)
            .httpClientBuilder((SdkHttpClient.Builder) serviceDefaults -> {
                assertThat(serviceDefaults).isEqualTo(SERVICE_DEFAULTS);
                return mock(SdkHttpClient.class);
            })
            .build();

        assertThat(client.clientConfiguration.option(DEFAULTS_MODE)).isEqualTo(DefaultsMode.LEGACY);
        assertThat(SdkDefaultRetryStrategy.retryMode(client.clientConfiguration.option(RETRY_STRATEGY))).isEqualTo(RetryMode.defaultRetryMode());
        assertThat(client.clientConfiguration.option(DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT)).isNull();
    }

    @Test
    public void nonLegacyDefaultsMode_shouldApplySdkDefaultsAndHttpDefaults() {
        DefaultsMode targetMode = DefaultsMode.IN_REGION;

        TestClient client =
            testClientBuilder().region(Region.US_WEST_1)
                               .defaultsMode(targetMode)
                               .httpClientBuilder((SdkHttpClient.Builder) serviceDefaults -> {
                                   AttributeMap defaultHttpConfig = DefaultsModeConfiguration.defaultHttpConfig(targetMode);
                                   AttributeMap mergedDefaults = SERVICE_DEFAULTS.merge(defaultHttpConfig);
                                   assertThat(serviceDefaults).isEqualTo(mergedDefaults);
                                   return mock(SdkHttpClient.class);
                               }).build();

        assertThat(client.clientConfiguration.option(DEFAULTS_MODE)).isEqualTo(targetMode);

        AttributeMap attributes = DefaultsModeConfiguration.defaultConfig(targetMode);

        assertThat(SdkDefaultRetryStrategy.retryMode(client.clientConfiguration.option(RETRY_STRATEGY))).isEqualTo(attributes.get(DEFAULT_RETRY_MODE));
        assertThat(client.clientConfiguration.option(DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT)).isEqualTo("regional");
    }

    @Test
    public void nonLegacyDefaultsModeAsyncClient_shouldApplySdkDefaultsAndHttpDefaults() {
        DefaultsMode targetMode = DefaultsMode.IN_REGION;

        TestAsyncClient client =
            testAsyncClientBuilder().region(Region.US_WEST_1)
                                    .defaultsMode(targetMode)
                                    .httpClientBuilder((SdkHttpClient.Builder) serviceDefaults -> {
                                        AttributeMap defaultHttpConfig = DefaultsModeConfiguration.defaultHttpConfig(targetMode);
                                        AttributeMap mergedDefaults = SERVICE_DEFAULTS.merge(defaultHttpConfig);
                                        assertThat(serviceDefaults).isEqualTo(mergedDefaults);
                                        return mock(SdkHttpClient.class);
                                    }).build();

        assertThat(client.clientConfiguration.option(DEFAULTS_MODE)).isEqualTo(targetMode);

        AttributeMap attributes = DefaultsModeConfiguration.defaultConfig(targetMode);

        assertThat(SdkDefaultRetryStrategy.retryMode(client.clientConfiguration.option(RETRY_STRATEGY))).isEqualTo(attributes.get(DEFAULT_RETRY_MODE));
    }

    @Test
    public void clientOverrideRetryMode_shouldTakePrecedence() {
        TestClient client =
            testClientBuilder().region(Region.US_WEST_1)
                               .defaultsMode(DefaultsMode.IN_REGION)
                               .overrideConfiguration(o -> o.retryPolicy(RetryMode.LEGACY))
                               .build();
        assertThat(client.clientConfiguration.option(DEFAULTS_MODE)).isEqualTo(DefaultsMode.IN_REGION);
        assertThat(client.clientConfiguration.option(RETRY_POLICY).retryMode()).isEqualTo(RetryMode.LEGACY);
    }

    @Test
    public void autoMode_shouldResolveDefaultsMode() {
        DefaultsMode expectedMode = DefaultsMode.IN_REGION;
        when(autoModeDiscovery.discover(any(Region.class))).thenReturn(expectedMode);
        TestClient client =
            testClientBuilder().region(Region.US_WEST_1)
                               .defaultsMode(DefaultsMode.AUTO)
                               .build();

        assertThat(client.clientConfiguration.option(DEFAULTS_MODE)).isEqualTo(expectedMode);
    }

    private static class TestClient {
        private final SdkClientConfiguration clientConfiguration;

        public TestClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
        }
    }

    private AwsClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        return new TestClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                      .overrideConfiguration(overrideConfig);
    }

    private AwsClientBuilder<TestAsyncClientBuilder, TestAsyncClient> testAsyncClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        return new TestAsyncClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                      .overrideConfiguration(overrideConfig);
    }

    private class TestClientBuilder extends AwsDefaultClientBuilder<TestClientBuilder, TestClient>
        implements AwsClientBuilder<TestClientBuilder, TestClient> {

        public TestClientBuilder() {
            super(defaultHttpClientBuilder, defaultAsyncHttpClientBuilder, autoModeDiscovery);
        }

        @Override
        protected TestClient buildClient() {
            return new TestClient(super.syncClientConfiguration());
        }

        @Override
        protected String serviceEndpointPrefix() {
            return ENDPOINT_PREFIX;
        }

        @Override
        protected String signingName() {
            return SIGNING_NAME;
        }

        @Override
        protected String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        protected AttributeMap serviceHttpConfig() {
            return SERVICE_DEFAULTS;
        }
    }

    private class TestAsyncClientBuilder extends AwsDefaultClientBuilder<TestAsyncClientBuilder, TestAsyncClient>
        implements AwsClientBuilder<TestAsyncClientBuilder, TestAsyncClient> {

        public TestAsyncClientBuilder() {
            super(defaultHttpClientBuilder, defaultAsyncHttpClientBuilder, autoModeDiscovery);
        }

        @Override
        protected TestAsyncClient buildClient() {
            return new TestAsyncClient(super.asyncClientConfiguration());
        }

        @Override
        protected String serviceEndpointPrefix() {
            return ENDPOINT_PREFIX;
        }

        @Override
        protected String signingName() {
            return SIGNING_NAME;
        }

        @Override
        protected String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        protected AttributeMap serviceHttpConfig() {
            return SERVICE_DEFAULTS;
        }
    }

    private static class TestAsyncClient {
        private final SdkClientConfiguration clientConfiguration;

        private TestAsyncClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
        }
    }
}
