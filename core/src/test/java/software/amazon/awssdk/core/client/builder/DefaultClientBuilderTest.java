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

package software.amazon.awssdk.core.client.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.config.SdkAdvancedClientOption.SIGNER_PROVIDER;

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
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.SdkImmutableAsyncClientConfiguration;
import software.amazon.awssdk.core.config.SdkImmutableSyncClientConfiguration;
import software.amazon.awssdk.core.config.defaults.SdkClientConfigurationDefaults;
import software.amazon.awssdk.core.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClientFactory;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Validate the functionality of the {@link SdkDefaultClientBuilder}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultClientBuilderTest {

    private static final AttributeMap MOCK_DEFAULTS = AttributeMap
            .builder()
            .put(SdkHttpConfigurationOption.SOCKET_TIMEOUT, Duration.ofSeconds(10))
            .build();

    private static final String ENDPOINT_PREFIX = "prefix";
    private static final URI DEFEAULT_ENDPOINT = URI.create("https://defaultendpoint.com");
    private static final URI ENDPOINT = URI.create("https://example.com");
    private static final NoOpSignerProvider TEST_SIGNER_PROVIDER = new NoOpSignerProvider();

    @Mock
    private SdkHttpClientFactory defaultHttpClientFactory;

    @Mock
    private SdkAsyncHttpClientFactory defaultAsyncHttpClientFactory;

    @Before
    public void setup() {
        when(defaultHttpClientFactory.createHttpClientWithDefaults(any())).thenReturn(mock(SdkHttpClient.class));
        when(defaultAsyncHttpClientFactory.createHttpClientWithDefaults(any())).thenReturn(mock(SdkAsyncHttpClient.class));
    }

    @Test
    public void buildIncludesServiceDefaults() {
        TestClient client = testClientBuilder().build();
        assertThat(client.syncClientConfiguration.overrideConfiguration().advancedOption(SIGNER_PROVIDER))
                .isEqualTo(TEST_SIGNER_PROVIDER);
    }

    @Test
    public void buildWithEndpointShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().endpointOverride(ENDPOINT).build();

        assertThat(client.syncClientConfiguration.endpoint()).isEqualTo(ENDPOINT);
    }

    @Test
    public void buildWithoutRegionOrEndpointOrDefaultProviderThrowsException() {
        assertThatNullPointerException().isThrownBy(() -> new TestClientWithoutEndpointDefaultBuilder().build()).withMessageContaining("The 'endpoint' must be configured in the client builder");
    }

    @Test
    public void noClientProvided_DefaultHttpClientIsManagedBySdk() {
        TestClient client = testClientBuilder().build();
        assertThat(client.syncClientConfiguration.httpClient())
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientFactory, times(1)).createHttpClientWithDefaults(any());
    }

    @Test
    public void noAsyncClientProvided_DefaultAsyncHttpClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder().build();
        assertThat(client.asyncClientConfiguration.asyncHttpClient())
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, times(1)).createHttpClientWithDefaults(any());
    }

    @Test
    public void clientFactoryProvided_ClientIsManagedBySdk() {
        TestClient client = testClientBuilder().httpConfiguration(ClientHttpConfiguration.builder()
                                                          .httpClientFactory(serviceDefaults -> {
                                                              assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                                                              return mock(SdkHttpClient.class);
                                                          })
                                                          .build())
                .build();
        assertThat(client.syncClientConfiguration.httpClient())
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientFactory, never()).createHttpClientWithDefaults(any());
    }

    @Test
    public void asyncHttpClientFactoryProvided_ClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
                .asyncHttpConfiguration(ClientAsyncHttpConfiguration
                                                .builder()
                                                .httpClientFactory(serviceDefaults -> {
                                                    assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                                                    return mock(SdkAsyncHttpClient.class);
                                                })
                                                .build())
                .build();
        assertThat(client.asyncClientConfiguration.asyncHttpClient())
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, never()).createHttpClientWithDefaults(any());
    }

    @Test
    public void explicitClientProvided_ClientIsNotManagedBySdk() {
        TestClient client = testClientBuilder()

                .httpConfiguration(ClientHttpConfiguration.builder()
                                                          .httpClient(mock(SdkHttpClient.class))
                                                          .build())
                .build();
        assertThat(client.syncClientConfiguration.httpClient())
                .isInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientFactory, never()).createHttpClientWithDefaults(any());
    }

    @Test
    public void explicitAsyncHttpClientProvided_ClientIsNotManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
                .asyncHttpConfiguration(ClientAsyncHttpConfiguration
                                                .builder()
                                                .httpClient(mock(SdkAsyncHttpClient.class))
                                                .build())
                .build();
        assertThat(client.asyncClientConfiguration.asyncHttpClient())
                .isInstanceOf(SdkDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, never()).createHttpClientWithDefaults(any());
    }

    @Test
    public void clientBuilderFieldsHaveBeanEquivalents() throws Exception {
        SdkClientBuilder<TestClientBuilder, TestClient> builder = testClientBuilder();

        BeanInfo beanInfo = Introspector.getBeanInfo(builder.getClass());
        Method[] clientBuilderMethods = SdkClientBuilder.class.getDeclaredMethods();

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

    private SdkDefaultClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                                           .advancedOption(SIGNER_PROVIDER, TEST_SIGNER_PROVIDER)
                                           .build();

        return new TestClientBuilder().overrideConfiguration(overrideConfig);
    }

    private SdkDefaultClientBuilder<TestAsyncClientBuilder, TestAsyncClient> testAsyncClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                                           .advancedOption(SIGNER_PROVIDER, TEST_SIGNER_PROVIDER)
                                           .build();

        return new TestAsyncClientBuilder().overrideConfiguration(overrideConfig);
    }

    private static class TestClient {
        private final SdkImmutableSyncClientConfiguration syncClientConfiguration;

        private TestClient(SdkImmutableSyncClientConfiguration syncClientConfiguration) {
            this.syncClientConfiguration = syncClientConfiguration;
        }
    }

    private class TestClientWithoutEndpointDefaultBuilder extends SdkDefaultClientBuilder<TestClientBuilder, TestClient>
        implements SdkClientBuilder<TestClientBuilder, TestClient> {

        public TestClientWithoutEndpointDefaultBuilder() {
            super(defaultHttpClientFactory, null);
        }

        @Override
        protected TestClient buildClient() {
            return new TestClient(super.syncClientConfiguration());
        }

        @Override
        protected SdkClientConfigurationDefaults serviceDefaults() {
            return new SdkClientConfigurationDefaults() {
                @Override
                protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                    builder.advancedOption(SIGNER_PROVIDER, TEST_SIGNER_PROVIDER);
                }
            };
        }

        @Override
        protected AttributeMap serviceSpecificHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }

    private class TestClientBuilder extends SdkDefaultClientBuilder<TestClientBuilder, TestClient>
        implements SdkClientBuilder<TestClientBuilder, TestClient> {

        public TestClientBuilder() {
            super(defaultHttpClientFactory, null);
        }

        @Override
        protected TestClient buildClient() {
            return new TestClient(super.syncClientConfiguration());
        }

        @Override
        protected SdkClientConfigurationDefaults serviceDefaults() {
            return new SdkClientConfigurationDefaults() {
                @Override
                protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                    ClientOverrideConfiguration config = builder.build();
                    builder.advancedOption(SIGNER_PROVIDER, applyDefault(config.advancedOption(SIGNER_PROVIDER), () -> null));
                }

                @Override
                protected URI getEndpointDefault() {
                    return DEFEAULT_ENDPOINT;
                }
            };
        }

        @Override
        protected AttributeMap serviceSpecificHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }

    private static class TestAsyncClient {
        private final SdkImmutableAsyncClientConfiguration asyncClientConfiguration;

        private TestAsyncClient(SdkImmutableAsyncClientConfiguration asyncClientConfiguration) {
            this.asyncClientConfiguration = asyncClientConfiguration;
        }
    }

    private class TestAsyncClientBuilder extends SdkDefaultClientBuilder<TestAsyncClientBuilder, TestAsyncClient>
        implements SdkClientBuilder<TestAsyncClientBuilder, TestAsyncClient> {

        public TestAsyncClientBuilder() {
            super(defaultHttpClientFactory, defaultAsyncHttpClientFactory);
        }

        @Override
        protected TestAsyncClient buildClient() {
            return new TestAsyncClient(super.asyncClientConfiguration());
        }

        @Override
        protected SdkClientConfigurationDefaults serviceDefaults() {
            return new SdkClientConfigurationDefaults() {
                @Override
                protected void applyOverrideDefaults(ClientOverrideConfiguration.Builder builder) {
                    ClientOverrideConfiguration config = builder.build();
                    builder.advancedOption(SIGNER_PROVIDER, applyDefault(config.advancedOption(SIGNER_PROVIDER), () -> null));
                }

                @Override
                protected URI getEndpointDefault() {
                    return DEFEAULT_ENDPOINT;
                }
            };
        }

        @Override
        protected AttributeMap serviceSpecificHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }
}
