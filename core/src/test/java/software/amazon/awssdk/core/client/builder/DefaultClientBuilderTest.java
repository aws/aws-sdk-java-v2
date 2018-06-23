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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;

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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Validate the functionality of the {@link SdkDefaultClientBuilder}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultClientBuilderTest {

    private static final AttributeMap MOCK_DEFAULTS = AttributeMap
            .builder()
            .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(10))
            .build();

    private static final String ENDPOINT_PREFIX = "prefix";
    private static final URI DEFAULT_ENDPOINT = URI.create("https://defaultendpoint.com");
    private static final URI ENDPOINT = URI.create("https://example.com");
    private static final NoOpSigner TEST_SIGNER = new NoOpSigner();

    @Mock
    private SdkHttpClient.Builder defaultHttpClientFactory;

    @Mock
    private SdkAsyncHttpClient.Builder defaultAsyncHttpClientFactory;

    @Before
    public void setup() {
        when(defaultHttpClientFactory.buildWithDefaults(any())).thenReturn(mock(SdkHttpClient.class));
        when(defaultAsyncHttpClientFactory.buildWithDefaults(any())).thenReturn(mock(SdkAsyncHttpClient.class));
    }

    @Test
    public void buildIncludesServiceDefaults() {
        TestClient client = testClientBuilder().build();
        assertThat(client.clientConfiguration.option(SIGNER))
                .isEqualTo(TEST_SIGNER);
    }

    @Test
    public void buildWithEndpointShouldHaveCorrectEndpointAndSigningRegion() {
        TestClient client = testClientBuilder().endpointOverride(ENDPOINT).build();

        assertThat(client.clientConfiguration.option(SdkClientOption.ENDPOINT)).isEqualTo(ENDPOINT);
    }

    @Test
    public void noClientProvided_DefaultHttpClientIsManagedBySdk() {
        TestClient client = testClientBuilder().build();
        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientFactory, times(1)).buildWithDefaults(any());
    }

    @Test
    public void noAsyncClientProvided_DefaultAsyncHttpClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder().build();
        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, times(1)).buildWithDefaults(any());
    }

    @Test
    public void clientFactoryProvided_ClientIsManagedBySdk() {
        TestClient client = testClientBuilder().httpClientBuilder(new SdkHttpClient.Builder() {
                    @Override
                    public SdkHttpClient buildWithDefaults(AttributeMap serviceDefaults) {
                        assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                        return mock(SdkHttpClient.class);
                    }
                })
                .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void asyncHttpClientFactoryProvided_ClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
                .asyncHttpClientBuilder(serviceDefaults -> {
                    assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
                    return mock(SdkAsyncHttpClient.class);
                })
                .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void explicitClientProvided_ClientIsNotManagedBySdk() {
        TestClient client = testClientBuilder()

                .httpClient(mock(SdkHttpClient.class))
                .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
                .isInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void explicitAsyncHttpClientProvided_ClientIsNotManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
                .asyncHttpClient(mock(SdkAsyncHttpClient.class))
                .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
                .isInstanceOf(SdkDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, never()).buildWithDefaults(any());
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
                                           .advancedOption(SIGNER, TEST_SIGNER)
                                           .build();

        return new TestClientBuilder().overrideConfiguration(overrideConfig);
    }

    private SdkDefaultClientBuilder<TestAsyncClientBuilder, TestAsyncClient> testAsyncClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                                           .advancedOption(SIGNER, TEST_SIGNER)
                                           .build();

        return new TestAsyncClientBuilder().overrideConfiguration(overrideConfig);
    }

    private static class TestClient {
        private final SdkClientConfiguration clientConfiguration;

        private TestClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
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
        protected SdkClientConfiguration mergeChildDefaults(SdkClientConfiguration configuration) {
            return configuration.merge(c -> c.option(SdkAdvancedClientOption.SIGNER, TEST_SIGNER));
        }

        @Override
        protected AttributeMap childHttpConfig() {
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
        protected SdkClientConfiguration mergeChildDefaults(SdkClientConfiguration configuration) {
            return configuration.merge(c -> c.option(SdkClientOption.ENDPOINT, DEFAULT_ENDPOINT));
        }

        @Override
        protected AttributeMap childHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }

    private static class TestAsyncClient {
        private final SdkClientConfiguration clientConfiguration;

        private TestAsyncClient(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
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
        protected SdkClientConfiguration mergeChildDefaults(SdkClientConfiguration configuration) {
            return configuration.merge(c -> c.option(SdkClientOption.ENDPOINT, DEFAULT_ENDPOINT));
        }

        @Override
        protected AttributeMap childHttpConfig() {
            return MOCK_DEFAULTS;
        }
    }
}
