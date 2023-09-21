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

package software.amazon.awssdk.core.client.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_PREFIX;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_SUFFIX;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ADDITIONAL_HTTP_HEADERS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_ATTEMPT_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ENDPOINT_OVERRIDDEN;
import static software.amazon.awssdk.core.client.config.SdkClientOption.EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.core.client.config.SdkClientOption.EXECUTION_INTERCEPTORS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.METRIC_PUBLISHERS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.PROFILE_FILE;
import static software.amazon.awssdk.core.client.config.SdkClientOption.PROFILE_FILE_SUPPLIER;
import static software.amazon.awssdk.core.client.config.SdkClientOption.PROFILE_NAME;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_POLICY;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SCHEDULED_EXECUTOR_SERVICE;
import static software.amazon.awssdk.core.internal.SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE;

import com.google.common.collect.ImmutableSet;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.ScheduledExecutorUtils.UnmanagedScheduledExecutorService;
import software.amazon.awssdk.utils.StringInputStream;

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
    public void overrideConfigurationIsNeverNull() {
        ClientOverrideConfiguration config = testClientBuilder().overrideConfiguration();
        assertThat(config).isNotNull();

        config = testClientBuilder().overrideConfiguration((ClientOverrideConfiguration) null).overrideConfiguration();
        assertThat(config).isNotNull();
    }

    @Test
    public void overrideConfigurationReturnsSetValues() {
        List<ExecutionInterceptor> interceptors = new ArrayList<>();
        RetryPolicy retryPolicy = RetryPolicy.builder().build();
        Map<String, List<String>> headers = new HashMap<>();
        List<MetricPublisher> metricPublishers = new ArrayList<>();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        Signer signer = (request, execAttributes) -> request;
        String suffix = "suffix";
        String prefix = "prefix";
        Duration apiCallTimeout = Duration.ofMillis(42);
        Duration apiCallAttemptTimeout = Duration.ofMillis(43);
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream(""))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        String profileName = "name";
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
            .executionInterceptors(interceptors)
            .retryPolicy(retryPolicy)
            .headers(headers)
            .putAdvancedOption(SIGNER, signer)
            .putAdvancedOption(USER_AGENT_SUFFIX, suffix)
            .putAdvancedOption(USER_AGENT_PREFIX, prefix)
            .apiCallTimeout(apiCallTimeout)
            .apiCallAttemptTimeout(apiCallAttemptTimeout)
            .putAdvancedOption(DISABLE_HOST_PREFIX_INJECTION, Boolean.TRUE)
            .defaultProfileFile(profileFile)
            .defaultProfileName(profileName)
            .metricPublishers(metricPublishers)
            .executionAttributes(executionAttributes)
            .putAdvancedOption(ENDPOINT_OVERRIDDEN_OVERRIDE, Boolean.TRUE)
            .scheduledExecutorService(scheduledExecutorService)
            .build();

        TestClientBuilder builder = testClientBuilder().overrideConfiguration(overrideConfig);
        ClientOverrideConfiguration builderOverrideConfig = builder.overrideConfiguration();

        assertThat(builderOverrideConfig.executionInterceptors()).isEqualTo(interceptors);
        assertThat(builderOverrideConfig.retryPolicy()).isEqualTo(Optional.of(retryPolicy));
        assertThat(builderOverrideConfig.headers()).isEqualTo(headers);
        assertThat(builderOverrideConfig.advancedOption(SIGNER)).isEqualTo(Optional.of(signer));
        assertThat(builderOverrideConfig.advancedOption(USER_AGENT_SUFFIX)).isEqualTo(Optional.of(suffix));
        assertThat(builderOverrideConfig.apiCallTimeout()).isEqualTo(Optional.of(apiCallTimeout));
        assertThat(builderOverrideConfig.apiCallAttemptTimeout()).isEqualTo(Optional.of(apiCallAttemptTimeout));
        assertThat(builderOverrideConfig.advancedOption(DISABLE_HOST_PREFIX_INJECTION)).isEqualTo(Optional.of(Boolean.TRUE));
        assertThat(builderOverrideConfig.defaultProfileFile()).isEqualTo(Optional.of(profileFile));
        assertThat(builderOverrideConfig.defaultProfileName()).isEqualTo(Optional.of(profileName));
        assertThat(builderOverrideConfig.metricPublishers()).isEqualTo(metricPublishers);
        assertThat(builderOverrideConfig.executionAttributes().getAttributes()).isEqualTo(executionAttributes.getAttributes());
        assertThat(builderOverrideConfig.advancedOption(ENDPOINT_OVERRIDDEN_OVERRIDE)).isEqualTo(Optional.of(Boolean.TRUE));
        assertThat(builderOverrideConfig.scheduledExecutorService().get()).isEqualTo(scheduledExecutorService);
    }

    @Test
    public void overrideConfigurationOmitsUnsetValues() {
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                                                                                .build();

        TestClientBuilder builder = testClientBuilder().overrideConfiguration(overrideConfig);
        ClientOverrideConfiguration builderOverrideConfig = builder.overrideConfiguration();

        assertThat(builderOverrideConfig.executionInterceptors()).isEmpty();
        assertThat(builderOverrideConfig.retryPolicy()).isEmpty();
        assertThat(builderOverrideConfig.headers()).isEmpty();
        assertThat(builderOverrideConfig.advancedOption(SIGNER)).isEmpty();
        assertThat(builderOverrideConfig.advancedOption(USER_AGENT_SUFFIX)).isEmpty();
        assertThat(builderOverrideConfig.apiCallTimeout()).isEmpty();
        assertThat(builderOverrideConfig.apiCallAttemptTimeout()).isEmpty();
        assertThat(builderOverrideConfig.advancedOption(DISABLE_HOST_PREFIX_INJECTION)).isEmpty();
        assertThat(builderOverrideConfig.defaultProfileFile()).isEmpty();
        assertThat(builderOverrideConfig.defaultProfileName()).isEmpty();
        assertThat(builderOverrideConfig.metricPublishers()).isEmpty();
        assertThat(builderOverrideConfig.executionAttributes().getAttributes()).isEmpty();
        assertThat(builderOverrideConfig.advancedOption(ENDPOINT_OVERRIDDEN_OVERRIDE)).isEmpty();
        assertThat(builderOverrideConfig.scheduledExecutorService()).isEmpty();
    }

    @Test
    public void buildIncludesClientOverrides() {
        List<ExecutionInterceptor> interceptors = new ArrayList<>();
        ExecutionInterceptor interceptor = new ExecutionInterceptor() {};
        interceptors.add(interceptor);

        RetryPolicy retryPolicy = RetryPolicy.builder().build();
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        Map<String, List<String>> headers = new HashMap<>();
        List<String> headerValues = new ArrayList<>();
        headerValues.add("value");
        headers.put("client-override-test", headerValues);

        List<MetricPublisher> metricPublishers = new ArrayList<>();
        MetricPublisher metricPublisher = new MetricPublisher() {
            @Override
            public void publish(MetricCollection metricCollection) {

            }

            @Override
            public void close() {

            }
        };
        metricPublishers.add(metricPublisher);

        ExecutionAttribute<String> execAttribute = new ExecutionAttribute<>("test");
        ExecutionAttributes executionAttributes = ExecutionAttributes.builder().put(execAttribute, "value").build();

        Signer signer = (request, execAttributes) -> request;
        String suffix = "suffix";
        String prefix = "prefix";
        Duration apiCallTimeout = Duration.ofMillis(42);
        Duration apiCallAttemptTimeout = Duration.ofMillis(43);
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream(""))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        String profileName = "name";

        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
            .executionInterceptors(interceptors)
            .retryPolicy(retryPolicy)
            .headers(headers)
            .putAdvancedOption(SIGNER, signer)
            .putAdvancedOption(USER_AGENT_SUFFIX, suffix)
            .putAdvancedOption(USER_AGENT_PREFIX, prefix)
            .apiCallTimeout(apiCallTimeout)
            .apiCallAttemptTimeout(apiCallAttemptTimeout)
            .putAdvancedOption(DISABLE_HOST_PREFIX_INJECTION, Boolean.TRUE)
            .defaultProfileFile(profileFile)
            .defaultProfileName(profileName)
            .metricPublishers(metricPublishers)
            .executionAttributes(executionAttributes)
            .putAdvancedOption(ENDPOINT_OVERRIDDEN_OVERRIDE, Boolean.TRUE)
            .scheduledExecutorService(scheduledExecutorService)
            .build();

        SdkClientConfiguration config =
            testClientBuilder().overrideConfiguration(overrideConfig).build().clientConfiguration;

        assertThat(config.option(EXECUTION_INTERCEPTORS)).contains(interceptor);
        assertThat(config.option(RETRY_POLICY)).isEqualTo(retryPolicy);
        assertThat(config.option(ADDITIONAL_HTTP_HEADERS).get("client-override-test")).isEqualTo(headerValues);
        assertThat(config.option(SIGNER)).isEqualTo(signer);
        assertThat(config.option(USER_AGENT_SUFFIX)).isEqualTo(suffix);
        assertThat(config.option(USER_AGENT_PREFIX)).isEqualTo(prefix);
        assertThat(config.option(API_CALL_TIMEOUT)).isEqualTo(apiCallTimeout);
        assertThat(config.option(API_CALL_ATTEMPT_TIMEOUT)).isEqualTo(apiCallAttemptTimeout);
        assertThat(config.option(DISABLE_HOST_PREFIX_INJECTION)).isEqualTo(Boolean.TRUE);
        assertThat(config.option(PROFILE_FILE)).isEqualTo(profileFile);
        assertThat(config.option(PROFILE_FILE_SUPPLIER).get()).isEqualTo(profileFile);
        assertThat(config.option(PROFILE_NAME)).isEqualTo(profileName);
        assertThat(config.option(METRIC_PUBLISHERS)).contains(metricPublisher);
        assertThat(config.option(EXECUTION_ATTRIBUTES).getAttribute(execAttribute)).isEqualTo("value");
        assertThat(config.option(ENDPOINT_OVERRIDDEN)).isEqualTo(Boolean.TRUE);
        UnmanagedScheduledExecutorService customScheduledExecutorService =
            (UnmanagedScheduledExecutorService) config.option(SCHEDULED_EXECUTOR_SERVICE);
        assertThat(customScheduledExecutorService.scheduledExecutorService()).isEqualTo(scheduledExecutorService);
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
    public void buildWithEndpointWithoutScheme_shouldThrowException() {
        assertThatThrownBy(() -> testClientBuilder().endpointOverride(URI.create("localhost")).build())
            .hasMessageContaining("The URI scheme of endpointOverride must not be null");

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
        TestClient client = testClientBuilder().httpClientBuilder((SdkHttpClient.Builder) serviceDefaults -> {
            Assertions.assertThat(serviceDefaults).isEqualTo(MOCK_DEFAULTS);
            return mock(SdkHttpClient.class);
        })
                .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT))
                .isNotInstanceOf(SdkDefaultClientBuilder.NonManagedSdkHttpClient.class);
        verify(defaultHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void asyncHttpClientFactoryProvided_ClientIsManagedBySdk() {
        TestAsyncClient client = testAsyncClientBuilder()
                .httpClientBuilder((SdkAsyncHttpClient.Builder) serviceDefaults -> {
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
                .httpClient(mock(SdkAsyncHttpClient.class))
                .build();
        assertThat(client.clientConfiguration.option(SdkClientOption.ASYNC_HTTP_CLIENT))
                .isInstanceOf(SdkDefaultClientBuilder.NonManagedSdkAsyncHttpClient.class);
        verify(defaultAsyncHttpClientFactory, never()).buildWithDefaults(any());
    }

    @Test
    public void clientBuilderFieldsHaveBeanEquivalents() throws Exception {
        // Mutating properties might not have bean equivalents. This is probably fine, since very few customers require
        // bean-equivalent methods and it's not clear what they'd expect them to be named anyway. Ignore these methods for now.
        Set<String> NON_BEAN_EQUIVALENT_METHODS = ImmutableSet.of("addPlugin");
        SdkClientBuilder<TestClientBuilder, TestClient> builder = testClientBuilder();

        BeanInfo beanInfo = Introspector.getBeanInfo(builder.getClass());
        Method[] clientBuilderMethods = SdkClientBuilder.class.getDeclaredMethods();

        Arrays.stream(clientBuilderMethods).filter(m -> !m.isSynthetic()).forEach(builderMethod -> {
            String propertyName = builderMethod.getName();

            if (NON_BEAN_EQUIVALENT_METHODS.contains(propertyName)) {
                return;
            }

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

    @Test
    public void defaultProfileFileSupplier_isStaticOrHasIdentityCaching() {
        SdkClientConfiguration config =
            testClientBuilder().build().clientConfiguration;

        Supplier<ProfileFile> defaultProfileFileSupplier = config.option(PROFILE_FILE_SUPPLIER);
        ProfileFile firstGet = defaultProfileFileSupplier.get();
        ProfileFile secondGet = defaultProfileFileSupplier.get();

        assertThat(secondGet).isSameAs(firstGet);
    }

    private SdkDefaultClientBuilder<TestClientBuilder, TestClient> testClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                                           .putAdvancedOption(SIGNER, TEST_SIGNER)
                                           .build();

        return new TestClientBuilder().overrideConfiguration(overrideConfig);
    }

    private SdkDefaultClientBuilder<TestAsyncClientBuilder, TestAsyncClient> testAsyncClientBuilder() {
        ClientOverrideConfiguration overrideConfig =
                ClientOverrideConfiguration.builder()
                                           .putAdvancedOption(SIGNER, TEST_SIGNER)
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
