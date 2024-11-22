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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.awscore.client.config.AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION;
import static software.amazon.awssdk.awscore.client.config.AwsClientOption.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.awscore.client.config.AwsClientOption.SIGNING_REGION;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.defaultsmode.AutoDefaultsModeDiscovery;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.internal.BaseRetryStrategy;
import software.amazon.awssdk.utils.AttributeMap;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Validate the functionality of the {@link AwsDefaultClientBuilder}.
 */
@ExtendWith(MockitoExtension.class)
public class DefaultAwsClientBuilderTest {

    private static final AttributeMap MOCK_DEFAULTS = AttributeMap
        .builder()
        .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(10))
        .build();

    private static final String ENDPOINT_PREFIX = "s3";
    private static final String SIGNING_NAME = "demo";
    private static final String SERVICE_NAME = "Demo";
    private static final Signer TEST_SIGNER = Aws4Signer.create();
    private static final URI ENDPOINT = URI.create("https://example.com");

    @Mock(lenient = true)
    private SdkHttpClient.Builder defaultHttpClientBuilder;

    @Mock(lenient = true)
    private SdkAsyncHttpClient.Builder defaultAsyncHttpClientFactory;

    @Mock
    private AutoDefaultsModeDiscovery autoModeDiscovery;

    @BeforeEach
    public void setup() {
        when(defaultHttpClientBuilder.buildWithDefaults(any())).thenReturn(mock(SdkHttpClient.class));
        when(defaultAsyncHttpClientFactory.buildWithDefaults(any())).thenReturn(mock(SdkAsyncHttpClient.class));
    }

    @Test
    public void buildIncludesServiceDefaults() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).build();
        assertThat(client.clientConfiguration.option(SIGNER)).isEqualTo(TEST_SIGNER);
        assertThat(client.clientConfiguration.option(SIGNING_REGION)).isNotNull();
        assertThat(client.clientConfiguration.option(SdkClientOption.SERVICE_NAME)).isEqualTo(SERVICE_NAME);
    }

    @Test
    public void buildWithRegionShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).build();

        assertThat(client.clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint())
            .hasToString("https://" + ENDPOINT_PREFIX + ".us-west-1.amazonaws.com");
        assertThat(client.clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).isEndpointOverridden())
            .isEqualTo(false);
        assertThat(client.clientConfiguration.option(SdkClientOption.ENDPOINT))
            .hasToString("https://" + ENDPOINT_PREFIX + ".us-west-1.amazonaws.com");
        assertThat(client.clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN))
            .isEqualTo(false);
        assertThat(client.clientConfiguration.option(SIGNING_REGION)).isEqualTo(Region.US_WEST_1);
        assertThat(client.clientConfiguration.option(SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME);
    }

    @Test
    public void buildWithFipsRegionThenNonFipsFipsEnabledRemainsSet() {
        TestClient client = testClientBuilder()
            .region(Region.of("us-west-2-fips")) // first call to setter sets the flag
            .region(Region.of("us-west-2"))// second call should clear
            .build();

        assertThat(client.clientConfiguration.option(AwsClientOption.AWS_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(client.clientConfiguration.option(AwsClientOption.FIPS_ENDPOINT_ENABLED)).isTrue();
    }

    @Test
    public void buildWithSetFipsTrueAndNonFipsRegionFipsEnabledRemainsSet() {
        TestClient client = testClientBuilder()
            .fipsEnabled(true)
            .region(Region.of("us-west-2"))
            .build();

        assertThat(client.clientConfiguration.option(AwsClientOption.AWS_REGION)).isEqualTo(Region.US_WEST_2);
        assertThat(client.clientConfiguration.option(AwsClientOption.FIPS_ENDPOINT_ENABLED)).isTrue();
    }

    @Test
    public void buildWithEndpointShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).endpointOverride(ENDPOINT).build();

        assertThat(client.clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint())
            .isEqualTo(ENDPOINT);
        assertThat(client.clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).isEndpointOverridden())
            .isEqualTo(true);
        assertThat(client.clientConfiguration.option(SdkClientOption.ENDPOINT)).isEqualTo(ENDPOINT);
        assertThat(client.clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN))
            .isEqualTo(true);
        assertThat(client.clientConfiguration.option(SIGNING_REGION)).isEqualTo(Region.US_WEST_1);
        assertThat(client.clientConfiguration.option(SERVICE_SIGNING_NAME)).isEqualTo(SIGNING_NAME);
    }

    @Test
    public void noClientProvided_DefaultHttpClientIsManagedBySdk() {
        TestClient client = testClientBuilder().region(Region.US_WEST_2).build();
        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientBuilder, times(1)).buildWithDefaults(any());
    }

    @Test
    public void noAsyncClientProvided_DefaultAsyncHttpClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder().region(Region.US_WEST_2).build();
        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, times(1)).buildWithDefaults(any());
    }

    @Test
    public void clientFactoryProvided_ClientIsManagedBySdk() {
        TestClient client = testClientBuilder()
            .region(Region.US_WEST_2)
            .httpClientBuilder((SdkHttpClient.Builder) serviceDefaults -> {
                assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                return mock(SdkHttpClient.class);
            })
            .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientBuilder, never()).buildWithDefaults(any());
    }

    @Test
    public void asyncHttpClientFactoryProvided_ClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
            .region(Region.US_WEST_2)
            .httpClientBuilder((SdkAsyncHttpClient.Builder) serviceDefaults -> {
                assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                return mock(SdkAsyncHttpClient.class);
            })
            .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void explicitClientProvided_ClientIsNotManagedBySdk() {
        String clientName = "foobarsync";
        SdkHttpClient sdkHttpClient = mock(SdkHttpClient.class);
        TestClient client = testClientBuilder()
            .region(Region.US_WEST_2)
            .httpClient(sdkHttpClient)
            .build();
        when(sdkHttpClient.clientName()).thenReturn(clientName);
        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
            .isInstanceOf(AwsDefaultClientBuilder.NonManagedSdkHttpClient.class);

        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT).clientName())
            .isEqualTo(clientName);
        verify(defaultHttpClientBuilder, never()).buildWithDefaults(any());
    }

    @Test
    public void explicitAsyncHttpClientProvided_ClientIsNotManagedBySdk() {
        String clientName = "foobarasync";
        SdkAsyncHttpClient sdkAsyncHttpClient = mock(SdkAsyncHttpClient.class);
        TestAsyncClient client = testAsyncClientBuilder()
            .region(Region.US_WEST_2)
            .httpClient(sdkAsyncHttpClient)
            .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
            .isInstanceOf(AwsDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);

        when(sdkAsyncHttpClient.clientName()).thenReturn(clientName);

        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
            .isInstanceOf(AwsDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);

        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT).clientName())
            .isEqualTo(clientName);
        verify(defaultAsyncHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void clientBuilderFieldsHaveBeanEquivalents() throws Exception {
        AwsClientBuilder<TestClientBuilder, TestClient> builder = testClientBuilder();

        BeanInfo beanInfo = Introspector.getBeanInfo(builder.getClass());
        Method[] clientBuilderMethods = AwsClientBuilder.class.getDeclaredMethods();

        Arrays.stream(clientBuilderMethods).filter(m -> !m.isSynthetic()).forEach(builderMethod -> {
            String propertyName = builderMethod.getName();

            Optional<PropertyDescriptor> propertyForMethod =
                Arrays.stream(beanInfo.getPropertyDescriptors())
                      .filter(property -> property.getName().equals(propertyName))
                      .findFirst();

            assertThat(propertyForMethod).as(propertyName + " property").hasValueSatisfying(property -> {
                assertThat(property.getReadMethod()).as(propertyName + " getter").isNull();
                assertThat(property.getWriteMethod()).as(propertyName + " setter").isNotNull();
            });
        });
    }

    @ParameterizedTest(name = "{0} - expectedPredicateAmount: {2}")
    @MethodSource("retryArguments")
    public void retryStrategyConfiguration_shouldAddDefaultPredicatesWhenRequired(
        String testDescription, RetryStrategy retryStrategy, int expectedPredicateAmount) {
        TestClientBuilder builder = testClientBuilder()
            .region(Region.US_EAST_1)
            .overrideConfiguration(c -> c.retryStrategy(retryStrategy));
        TestClient client = builder.build();

        ClientOverrideConfiguration conf = client.clientConfiguration.asOverrideConfiguration();
        assertThat(conf.retryStrategy()).isPresent();

        RetryStrategy configuredRetryStrategy = conf.retryStrategy().get();
        BaseRetryStrategy baseRetryStrategy = (BaseRetryStrategy) configuredRetryStrategy;
        assertThat(baseRetryStrategy.shouldAddDefaults(AwsRetryStrategy.DEFAULTS_NAME)).isFalse();
        assertThat(baseRetryStrategy.shouldAddDefaults(SdkDefaultRetryStrategy.DEFAULTS_NAME)).isFalse();
        assertThat(baseRetryStrategy.retryPredicates()).hasSize(expectedPredicateAmount);
    }

    private static Stream<Arguments> retryArguments() {
        return Stream.of(
            Arguments.of("Standard - static method creation",
                         SdkDefaultRetryStrategy.standardRetryStrategy(), 9),
            Arguments.of("Standard - builder with retryOnException",
                         StandardRetryStrategy.builder()
                                              .retryOnException(TestException.class)
                                              .backoffStrategy(BackoffStrategy.retryImmediately())
                                              .throttlingBackoffStrategy(BackoffStrategy.retryImmediately())
                                              .build(), 10),
            Arguments.of("Standard - useClientDefaults=false",
                         StandardRetryStrategy.builder()
                                              .retryOnException(TestException.class)
                                              .backoffStrategy(BackoffStrategy.retryImmediately())
                                              .throttlingBackoffStrategy(BackoffStrategy.retryImmediately())
                                              .useClientDefaults(false)
                                              .build(), 1),
            Arguments.of("Adaptive - static method creation",
                         SdkDefaultRetryStrategy.adaptiveRetryStrategy(), 9),
            Arguments.of("Adaptive - builder with retryOnException",
                         AdaptiveRetryStrategy.builder()
                                              .retryOnException(TestException.class)
                                              .backoffStrategy(BackoffStrategy.retryImmediately())
                                              .throttlingBackoffStrategy(BackoffStrategy.retryImmediately())
                                              .build(), 10),
            Arguments.of("Adaptive - useClientDefaults=false",
                         AdaptiveRetryStrategy.builder()
                                              .retryOnException(TestException.class)
                                              .backoffStrategy(BackoffStrategy.retryImmediately())
                                              .throttlingBackoffStrategy(BackoffStrategy.retryImmediately())
                                              .useClientDefaults(false)
                                              .build(), 1),
            Arguments.of("Legacy - static method creation",
                         SdkDefaultRetryStrategy.legacyRetryStrategy(), 9),
            Arguments.of("Legacy - builder with retryOnException",
                         LegacyRetryStrategy.builder()
                                            .retryOnException(TestException.class)
                                            .backoffStrategy(BackoffStrategy.retryImmediately())
                                            .throttlingBackoffStrategy(BackoffStrategy.retryImmediately())
                                            .build(), 10),
            Arguments.of("Legacy - useClientDefaults=false",
                         LegacyRetryStrategy.builder()
                                            .retryOnException(TestException.class)
                                            .backoffStrategy(BackoffStrategy.retryImmediately())
                                            .throttlingBackoffStrategy(BackoffStrategy.retryImmediately())
                                            .useClientDefaults(false)
                                            .build(), 1)
        );
    }

    private AwsClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(SIGNER, TEST_SIGNER)
                                       .putAdvancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        return new TestClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                      .overrideConfiguration(overrideConfig);
    }

    private AwsClientBuilder<TestAsyncClientBuilder, TestAsyncClient> testAsyncClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(SIGNER, TEST_SIGNER)
                                       .putAdvancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        return new TestAsyncClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                           .overrideConfiguration(overrideConfig);
    }

    private static class TestClient {
        private final SdkClientConfiguration clientConfiguration;

        public TestClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
        }
    }

    private class TestClientBuilder extends AwsDefaultClientBuilder<TestClientBuilder, TestClient>
        implements AwsClientBuilder<TestClientBuilder, TestClient> {

        public TestClientBuilder() {
            super(defaultHttpClientBuilder, null, autoModeDiscovery);
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
            return MOCK_DEFAULTS;
        }
    }

    private static class TestAsyncClient {
        private final SdkClientConfiguration clientConfiguration;

        private TestAsyncClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
        }
    }

    private class TestAsyncClientBuilder extends AwsDefaultClientBuilder<TestAsyncClientBuilder, TestAsyncClient>
        implements AwsClientBuilder<TestAsyncClientBuilder, TestAsyncClient> {

        public TestAsyncClientBuilder() {
            super(defaultHttpClientBuilder, defaultAsyncHttpClientFactory, autoModeDiscovery);
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
            return MOCK_DEFAULTS;
        }
    }

    private static class TestException extends Throwable {
    }
}
