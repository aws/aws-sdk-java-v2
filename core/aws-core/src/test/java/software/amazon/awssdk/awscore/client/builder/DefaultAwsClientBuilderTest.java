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

import com.google.common.collect.ImmutableSet;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.internal.defaultsmode.AutoDefaultsModeDiscovery;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
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

    private static final String ENDPOINT_PREFIX = "s3";
    private static final String SIGNING_NAME = "demo";
    private static final String SERVICE_NAME = "Demo";
    private static final Signer TEST_SIGNER = Aws4Signer.create();
    private static final URI ENDPOINT = URI.create("https://example.com");

    @Mock
    private SdkHttpClient.Builder defaultHttpClientBuilder;

    @Mock
    private SdkAsyncHttpClient.Builder defaultAsyncHttpClientFactory;

    @Mock
    private AutoDefaultsModeDiscovery autoModeDiscovery;

    @Before
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

        assertThat(client.clientConfiguration.option(SdkClientOption.ENDPOINT))
            .hasToString("https://" + ENDPOINT_PREFIX + ".us-west-1.amazonaws.com");
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

        assertThat(client.clientConfiguration.option(SdkClientOption.ENDPOINT)).isEqualTo(ENDPOINT);
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
        // Mutating properties might not have bean equivalents. This is probably fine, since very few customers require
        // bean-equivalent methods and it's not clear what they'd expect them to be named anyway. Ignore these methods for now.
        Set<String> NON_BEAN_EQUIVALENT_METHODS = ImmutableSet.of("putAuthScheme", "addPlugin");

        AwsClientBuilder<TestClientBuilder, TestClient> builder = testClientBuilder();

        BeanInfo beanInfo = Introspector.getBeanInfo(builder.getClass());
        Method[] clientBuilderMethods = AwsClientBuilder.class.getDeclaredMethods();

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
}
