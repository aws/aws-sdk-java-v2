/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.awscore.config.AwsAdvancedClientOption.ENABLE_DEFAULT_REGION_DETECTION;
import static software.amazon.awssdk.awscore.config.AwsAdvancedClientOption.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.core.config.SdkAdvancedClientOption.SIGNER;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.config.AwsImmutableAsyncClientConfiguration;
import software.amazon.awssdk.awscore.config.AwsImmutableSyncClientConfiguration;
import software.amazon.awssdk.awscore.config.defaults.AwsClientConfigurationDefaults;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Validate the functionality of the {@link AwsDefaultClientBuilder}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultAwsClientBuilderTest {

    private static final AttributeMap MOCK_DEFAULTS = AttributeMap
        .builder()
        .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(10))
        .build();

    private static final String ENDPOINT_PREFIX = "prefix";
    private static final String SIGNING_NAME = "demo";
    private static final Signer TEST_SIGNER = Aws4Signer.create();
    private static final URI ENDPOINT = URI.create("https://example.com");

    @Mock
    private SdkHttpClient.Builder defaultHttpClientBuilder;

    @Mock
    private SdkAsyncHttpClient.Builder defaultAsyncHttpClientFactory;

    @Before
    public void setup() {
        when(defaultHttpClientBuilder.buildWithDefaults(any())).thenReturn(mock(SdkHttpClient.class));
        when(defaultAsyncHttpClientFactory.buildWithDefaults(any())).thenReturn(mock(SdkAsyncHttpClient.class));
    }

    @Test
    public void buildIncludesServiceDefaults() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).build();
        assertThat(client.syncClientConfiguration.overrideConfiguration().advancedOption(SIGNER))
            .isEqualTo(TEST_SIGNER);
        assertThat(client.signingRegion).isNotNull();
    }

    @Test
    public void buildWithRegionShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).build();

        assertThat(client.syncClientConfiguration.endpoint())
            .hasToString("https://" + ENDPOINT_PREFIX + ".us-west-1.amazonaws.com");
        assertThat(client.signingRegion).isEqualTo(Region.US_WEST_1);
        assertThat(client.syncClientConfiguration.overrideConfiguration().advancedOption(SERVICE_SIGNING_NAME))
            .isEqualTo(SIGNING_NAME);
    }

    @Test
    public void buildWithEndpointShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().region(Region.US_WEST_1).endpointOverride(ENDPOINT).build();

        assertThat(client.syncClientConfiguration.endpoint()).isEqualTo(ENDPOINT);
        assertThat(client.signingRegion).isEqualTo(Region.US_WEST_1);
        assertThat(client.syncClientConfiguration.overrideConfiguration().advancedOption(SERVICE_SIGNING_NAME))
            .isEqualTo(SIGNING_NAME);
    }

    @Test
    public void buildWithoutRegionOrEndpointOrDefaultProviderThrowsException() {
        assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> testClientBuilder().build());
    }

    @Test
    public void noClientProvided_DefaultHttpClientIsManagedBySdk() {
        TestClient client = testClientBuilder().region(Region.US_WEST_2).build();
        assertThat(client.syncClientConfiguration.httpClient())
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientBuilder, times(1)).buildWithDefaults(any());
    }

    @Test
    public void noAsyncClientProvided_DefaultAsyncHttpClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder().region(Region.US_WEST_2).build();
        assertThat(client.asyncClientConfiguration.asyncHttpClient())
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, times(1)).buildWithDefaults(any());
    }

    @Test
    public void clientFactoryProvided_ClientIsManagedBySdk() {
        TestClient client = testClientBuilder()
            .region(Region.US_WEST_2)
            .httpClientBuilder(serviceDefaults -> {
                assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                return mock(SdkHttpClient.class);
            })
            .build();
        assertThat(client.syncClientConfiguration.httpClient())
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientBuilder, never()).buildWithDefaults(any());
    }

    @Test
    public void asyncHttpClientFactoryProvided_ClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
            .region(Region.US_WEST_2)
            .asyncHttpClientBuilder(serviceDefaults -> {
                assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                return mock(SdkAsyncHttpClient.class);
            })
            .build();
        assertThat(client.asyncClientConfiguration.asyncHttpClient())
            .isNotInstanceOf(AwsDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void explicitClientProvided_ClientIsNotManagedBySdk() {
        TestClient client = testClientBuilder()
            .region(Region.US_WEST_2)
            .httpClient(mock(SdkHttpClient.class))
            .build();
        assertThat(client.syncClientConfiguration.httpClient())
            .isInstanceOf(AwsDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientBuilder, never()).buildWithDefaults(any());
    }

    @Test
    public void explicitAsyncHttpClientProvided_ClientIsNotManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
            .region(Region.US_WEST_2)
            .asyncHttpClient(mock(SdkAsyncHttpClient.class))
            .build();
        assertThat(client.asyncClientConfiguration.asyncHttpClient())
            .isInstanceOf(AwsDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
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

    private AwsClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .advancedOption(SIGNER, TEST_SIGNER)
                                       .advancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        return new TestClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                      .overrideConfiguration(overrideConfig);
    }

    private AwsClientBuilder<TestAsyncClientBuilder, TestAsyncClient> testAsyncClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
            ClientOverrideConfiguration.builder()
                                       .advancedOption(SIGNER, TEST_SIGNER)
                                       .advancedOption(ENABLE_DEFAULT_REGION_DETECTION, false)
                                       .build();

        return new TestAsyncClientBuilder().credentialsProvider(AnonymousCredentialsProvider.create())
                                           .overrideConfiguration(overrideConfig);
    }

    private static class TestClient {
        private final AwsImmutableSyncClientConfiguration syncClientConfiguration;
        private final Region signingRegion;

        private TestClient(AwsImmutableSyncClientConfiguration syncClientConfiguration,
                           Region signingRegion) {
            this.syncClientConfiguration = syncClientConfiguration;
            this.signingRegion = signingRegion;
        }
    }

    private class TestClientBuilder extends AwsDefaultClientBuilder<TestClientBuilder, TestClient>
        implements AwsClientBuilder<TestClientBuilder, TestClient> {

        public TestClientBuilder() {
            super(defaultHttpClientBuilder, null);
        }

        @Override
        protected TestClient buildClient() {
            return new TestClient(super.syncClientConfiguration(),
                                  super.signingRegion());
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
        protected AwsClientConfigurationDefaults serviceDefaults() {
            return new AwsClientConfigurationDefaults() {
                @Override
                protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                    ClientOverrideConfiguration config = builder.build();
                    builder.advancedOption(SIGNER, applyDefault(config.advancedOption(SIGNER), () -> null));
                }
            };
        }

        @Override
        protected AttributeMap serviceSpecificHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }

    private static class TestAsyncClient {
        private final AwsImmutableAsyncClientConfiguration asyncClientConfiguration;

        private TestAsyncClient(AwsImmutableAsyncClientConfiguration asyncClientConfiguration) {
            this.asyncClientConfiguration = asyncClientConfiguration;
        }
    }

    private class TestAsyncClientBuilder extends AwsDefaultClientBuilder<TestAsyncClientBuilder, TestAsyncClient>
        implements AwsClientBuilder<TestAsyncClientBuilder, TestAsyncClient> {

        public TestAsyncClientBuilder() {
            super(defaultHttpClientBuilder, defaultAsyncHttpClientFactory);
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
        protected AwsClientConfigurationDefaults serviceDefaults() {
            return new AwsClientConfigurationDefaults() {
                @Override
                protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                    ClientOverrideConfiguration config = builder.build();
                    builder.advancedOption(SIGNER, applyDefault(config.advancedOption(SIGNER), () -> null));
                }
            };
        }

        @Override
        protected AttributeMap serviceSpecificHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }
}
