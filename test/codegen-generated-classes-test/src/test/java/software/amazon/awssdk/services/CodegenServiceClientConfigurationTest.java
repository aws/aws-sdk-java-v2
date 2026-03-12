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

package software.amazon.awssdk.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.AwsServiceClientConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonServiceClientConfiguration;
import software.amazon.awssdk.services.protocolrestjson.auth.scheme.ProtocolRestJsonAuthSchemeProvider;
import software.amazon.awssdk.services.protocolrestjson.internal.ProtocolRestJsonServiceClientConfigurationBuilder;

public class CodegenServiceClientConfigurationTest {
    private static final EndpointProvider MOCK_ENDPOINT_PROVIDER = mock(EndpointProvider.class);
    private static final IdentityProvider<AwsCredentialsIdentity> MOCK_IDENTITY_PROVIDER = mock(IdentityProvider.class);
    private static final ProtocolRestJsonAuthSchemeProvider MOCK_AUTH_SCHEME_PROVIDER =
        mock(ProtocolRestJsonAuthSchemeProvider.class);
    private static final ScheduledExecutorService MOCK_SCHEDULED_EXECUTOR_SERVICE = mock(ScheduledExecutorService.class);
    private static final Signer MOCK_SIGNER = mock(Signer.class);

    @ParameterizedTest
    @MethodSource("testCases")
    <T> void externalInternalTransforms_preserves_propertyValues(TestCase<T> testCase) {
        SdkClientConfiguration.Builder clientConfig = SdkClientConfiguration.builder();
        ProtocolRestJsonServiceClientConfigurationBuilder builder =
            new ProtocolRestJsonServiceClientConfigurationBuilder(clientConfig);

        // Verify that initially the value is null for properties with direct mapping.
        if (testCase.hasDirectMapping) {
            assertThat(testCase.getter.apply(builder)).isNull();
        }

        // Set the value
        testCase.setter.accept(builder, testCase.value);

        // Assert that we can retrieve the same value
        assertThat(testCase.getter.apply(builder)).isEqualTo(testCase.value);

        // Build the ServiceConfiguration instance
        ProtocolRestJsonServiceClientConfiguration config = builder.build();

        // Assert that we can retrieve the same value
        assertThat(testCase.dataGetter.apply(config)).isEqualTo(testCase.value);

        // Build a new builder with the created client config
        ProtocolRestJsonServiceClientConfigurationBuilder anotherBuilder =
            new ProtocolRestJsonServiceClientConfigurationBuilder(clientConfig);

        // Assert that we can retrieve the same value
        if (testCase.hasDirectMapping) {
            assertThat(testCase.getter.apply(anotherBuilder)).isEqualTo(testCase.value);
        }
    }

    public static List<TestCase<?>> testCases() throws Exception {
        return Arrays.asList(
            TestCase.<Region>builder()
                    .option(AwsClientOption.AWS_REGION)
                    .value(Region.US_WEST_2)
                    .setter(ProtocolRestJsonServiceClientConfiguration.Builder::region)
                    .getter(ProtocolRestJsonServiceClientConfiguration.Builder::region)
                    .dataGetter(AwsServiceClientConfiguration::region)
                    .build(),
            TestCase.<URI>builder()
                    .option(SdkClientOption.ENDPOINT)
                    .value(new URI("http://localhost:8080"))
                    .setter(ProtocolRestJsonServiceClientConfiguration.Builder::endpointOverride)
                    .getter(ProtocolRestJsonServiceClientConfiguration.Builder::endpointOverride)
                    .dataGetter(x -> x.endpointOverride().orElse(null))
                    .build(),
            TestCase.<ClientEndpointProvider>builder()
                    .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER)
                    .value(ClientEndpointProvider.forEndpointOverride(new URI("http://localhost:8080")))
                    .setter((b, p) -> b.endpointOverride(p.clientEndpoint()))
                    .getter(b -> b.endpointOverride() == null ? null : ClientEndpointProvider.forEndpointOverride(b.endpointOverride()))
                    .dataGetter(x -> x.endpointOverride().map(ClientEndpointProvider::forEndpointOverride).orElse(null))
                    .build(),
            TestCase.<EndpointProvider>builder()
                    .option(SdkClientOption.ENDPOINT_PROVIDER)
                    .value(MOCK_ENDPOINT_PROVIDER)
                    .setter(ProtocolRestJsonServiceClientConfiguration.Builder::endpointProvider)
                    .getter(ProtocolRestJsonServiceClientConfiguration.Builder::endpointProvider)
                    .dataGetter(x -> x.endpointProvider().orElse(null))
                    .build(),
            TestCase.<IdentityProvider<? extends AwsCredentialsIdentity>>builder()
                    .option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER)
                    .value(MOCK_IDENTITY_PROVIDER)
                    .setter(ProtocolRestJsonServiceClientConfiguration.Builder::credentialsProvider)
                    .getter(ProtocolRestJsonServiceClientConfiguration.Builder::credentialsProvider)
                    .dataGetter(AwsServiceClientConfiguration::credentialsProvider)
                    .build(),
            TestCase.<AuthSchemeProvider>builder()
                    .option(SdkClientOption.AUTH_SCHEME_PROVIDER)
                    .value(MOCK_AUTH_SCHEME_PROVIDER)
                    .setter((b, p) -> b.authSchemeProvider((ProtocolRestJsonAuthSchemeProvider) p))
                    .getter(ProtocolRestJsonServiceClientConfiguration.Builder::authSchemeProvider)
                    .dataGetter(ProtocolRestJsonServiceClientConfiguration::authSchemeProvider)
                    .build(),
            // Override configuration gets tricky
            TestCase.<ScheduledExecutorService>builder()
                    .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE)
                    .value(MOCK_SCHEDULED_EXECUTOR_SERVICE)
                    .setter((b, p) -> b.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                                   .scheduledExecutorService(p)
                                                   .build()))
                    .getter(b -> b.overrideConfiguration().scheduledExecutorService().orElse(null))
                    .dataGetter(d -> d.overrideConfiguration().scheduledExecutorService().orElse(null))
                    .withoutDirectMapping()
                    .build(),
            TestCase.<Signer>builder()
                    .option(SdkAdvancedClientOption.SIGNER)
                    .value(MOCK_SIGNER)
                    .setter((b, p) -> b.overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                                   .putAdvancedOption(SdkAdvancedClientOption.SIGNER, p)
                                                   .build()))
                    .getter(b -> b.overrideConfiguration().advancedOption(SdkAdvancedClientOption.SIGNER).orElse(null))
                    .dataGetter(d -> d.overrideConfiguration().advancedOption(SdkAdvancedClientOption.SIGNER).orElse(null))
                    .withoutDirectMapping()
                    .build()
        );
    }

    static class TestCase<T> {
        private final ClientOption<T> option;
        private final T value;
        private final BiConsumer<ProtocolRestJsonServiceClientConfiguration.Builder, T> setter;
        private final Function<ProtocolRestJsonServiceClientConfiguration.Builder, T> getter;
        private Function<ProtocolRestJsonServiceClientConfiguration, T> dataGetter;
        private final boolean hasDirectMapping;

        public TestCase(Builder<T> builder) {
            this.option = builder.option;
            this.value = builder.value;
            this.setter = builder.setter;
            this.getter = builder.getter;
            this.dataGetter = builder.dataGetter;
            this.hasDirectMapping = builder.hasDirectMapping;
        }

        public static <T> Builder<T> builder() {
            return new Builder();
        }

        static class Builder<T> {
            private ClientOption<T> option;
            private T value;
            private BiConsumer<ProtocolRestJsonServiceClientConfiguration.Builder, T> setter;
            private Function<ProtocolRestJsonServiceClientConfiguration.Builder, T> getter;
            private Function<ProtocolRestJsonServiceClientConfiguration, T> dataGetter;
            private boolean hasDirectMapping = true;

            Builder<T> option(ClientOption<T> option) {
                this.option = option;
                return this;
            }

            Builder<T> value(T value) {
                this.value = value;
                return this;
            }

            Builder<T> setter(BiConsumer<ProtocolRestJsonServiceClientConfiguration.Builder, T> setter) {
                this.setter = setter;
                return this;
            }

            Builder<T> getter(Function<ProtocolRestJsonServiceClientConfiguration.Builder, T> getter) {
                this.getter = getter;
                return this;
            }

            Builder<T> dataGetter(Function<ProtocolRestJsonServiceClientConfiguration, T> dataGetter) {
                this.dataGetter = dataGetter;
                return this;
            }

            Builder<T> withoutDirectMapping() {
                this.hasDirectMapping = false;
                return this;
            }

            TestCase<T> build() {
                return new TestCase<>(this);
            }
        }
    }
}
