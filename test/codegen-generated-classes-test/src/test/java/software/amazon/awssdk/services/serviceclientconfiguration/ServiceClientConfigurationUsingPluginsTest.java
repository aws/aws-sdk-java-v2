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

package software.amazon.awssdk.services.serviceclientconfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlServiceClientConfiguration;
import software.amazon.awssdk.services.protocolrestxml.endpoints.ProtocolRestXmlEndpointProvider;

// The same battery of tests as in `ServiceClientConfigurationTest` but using plugins.
public class ServiceClientConfigurationUsingPluginsTest {
    private static final SdkPlugin NOOP_PLUGIN = config -> {};

    @Test
    void syncClient_serviceClientConfiguration_shouldReturnCorrectRegion() {
        SdkPlugin testPlugin = config -> {
            if (config instanceof ProtocolRestXmlServiceClientConfiguration.Builder) {
                ProtocolRestXmlServiceClientConfiguration.Builder builder =
                    (ProtocolRestXmlServiceClientConfiguration.Builder) config;
                builder.region(Region.ME_SOUTH_1);
            }
        };
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .addPlugin(testPlugin)
                                                            .build();

        Region region = client.serviceClientConfiguration().region();
        assertThat(region).isEqualTo(Region.ME_SOUTH_1);
    }

    @Test
    void syncClientWithEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        URI uri = URI.create("https://www.amazon.com/");
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .addPlugin(config -> config.endpointOverride(uri))
                                                            .build();

        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(uri);
    }

    @Test
    void syncClientWithoutEndpointOverride_serviceClientConfiguration_shouldReturnEmptyOptional() {
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .addPlugin(NOOP_PLUGIN)
                                                            .build();
        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isNull();
    }

    @Test
    void syncClient_serviceClientConfiguration_shouldReturnCorrectClientOverrideConfigurationFields() {
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                                                                                       .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                                                                       .apiCallTimeout(Duration.ofSeconds(90))
                                                                                       .retryPolicy(c -> c.numRetries(4))
                                                                                       .build();

        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .addPlugin(config -> config.overrideConfiguration(overrideConfiguration))
                                                            .build();

        ClientOverrideConfiguration result = client.serviceClientConfiguration().overrideConfiguration();
        assertThat(result.apiCallAttemptTimeout().get()).isEqualTo(Duration.ofSeconds(30));
        assertThat(result.apiCallTimeout().get()).isEqualTo(Duration.ofSeconds(90));
        assertThat(result.retryPolicy().get().numRetries()).isEqualTo(4);
        assertThat(result.defaultProfileFile()).hasValue(ProfileFile.defaultProfileFile());
        assertThat(result.metricPublishers()).isEmpty();
    }

    @Test
    void syncClient_serviceClientConfiguration_withoutOverrideConfiguration_shouldReturnEmptyFields() {
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .addPlugin(NOOP_PLUGIN)
                                                            .build();

        ClientOverrideConfiguration overrideConfiguration = client.serviceClientConfiguration().overrideConfiguration();
        assertThat(overrideConfiguration.apiCallAttemptTimeout()).isNotPresent();
        assertThat(overrideConfiguration.apiCallTimeout()).isNotPresent();
        assertThat(overrideConfiguration.retryPolicy().get().numRetries()).isEqualTo(3);
        assertThat(overrideConfiguration.defaultProfileFile()).hasValue(ProfileFile.defaultProfileFile());
        assertThat(overrideConfiguration.metricPublishers()).isEmpty();
    }

    @Test
    void syncClientWithEndpointProvider_serviceClientConfiguration_shouldReturnCorrectEndpointProvider() {
        ProtocolRestXmlEndpointProvider clientEndpointProvider = ProtocolRestXmlEndpointProvider.defaultProvider();
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .addPlugin(config -> config.endpointProvider(clientEndpointProvider))
                                                            .build();

        EndpointProvider endpointProvider = client.serviceClientConfiguration().endpointProvider().orElse(null);
        assertThat(endpointProvider).isEqualTo(clientEndpointProvider);
    }

    @Test
    void syncClientWithoutEndpointProvider_serviceClientConfiguration_shouldReturnDefaultEndpointProvider() {
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .build();

        EndpointProvider endpointProvider = client.serviceClientConfiguration().endpointProvider().orElse(null);
        assertThat(endpointProvider instanceof ProtocolRestXmlEndpointProvider).isTrue();
    }

    @Test
    void asyncClient_serviceClientConfiguration_shouldReturnCorrectRegion() {
        SdkPlugin testPlugin = config -> {
            if (config instanceof ProtocolRestXmlServiceClientConfiguration.Builder) {
                ProtocolRestXmlServiceClientConfiguration.Builder builder =
                    (ProtocolRestXmlServiceClientConfiguration.Builder) config;

                builder.region(Region.ME_SOUTH_1);
            }
        };
        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .addPlugin(testPlugin)
                                                                      .build();

        Region region = client.serviceClientConfiguration().region();
        assertThat(region).isEqualTo(Region.ME_SOUTH_1);
    }

    @Test
    void asyncClientWithEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        URI uri = URI.create("https://www.amazon.com/");
        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .addPlugin(config -> config.endpointOverride(uri))
                                                                      .build();

        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(uri);
    }

    @Test
    void asyncClientWithoutEndpointOverride_serviceClientConfiguration_shouldReturnEmptyOptional() {
        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .addPlugin(NOOP_PLUGIN)
                                                                      .build();

        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isNull();
    }

    @Test
    void asyncClient_serviceClientConfiguration_shouldReturnCorrectClientOverrideConfigurationFields() {
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                                                                                       .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                                                                       .apiCallTimeout(Duration.ofSeconds(90))
                                                                                       .retryPolicy(c -> c.numRetries(4))
                                                                                       .build();

        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .addPlugin(config -> config.overrideConfiguration(overrideConfiguration))
                                                                      .build();

        ClientOverrideConfiguration result = client.serviceClientConfiguration().overrideConfiguration();
        assertThat(result.apiCallAttemptTimeout().get()).isEqualTo(Duration.ofSeconds(30));
        assertThat(result.apiCallTimeout().get()).isEqualTo(Duration.ofSeconds(90));
        assertThat(result.retryPolicy().get().numRetries()).isEqualTo(4);
        assertThat(result.defaultProfileFile()).hasValue(ProfileFile.defaultProfileFile());
        assertThat(result.metricPublishers()).isEmpty();
    }

    @Test
    void asyncClient_serviceClientConfiguration_withoutOverrideConfiguration_shouldReturnEmptyFieldsAndDefaults() {
        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .addPlugin(NOOP_PLUGIN)
                                                                      .build();

        ClientOverrideConfiguration result = client.serviceClientConfiguration().overrideConfiguration();
        assertThat(result.apiCallAttemptTimeout()).isNotPresent();
        assertThat(result.apiCallTimeout()).isNotPresent();
        assertThat(result.retryPolicy().get().numRetries()).isEqualTo(3);
        assertThat(result.defaultProfileFile()).hasValue(ProfileFile.defaultProfileFile());
        assertThat(result.metricPublishers()).isEmpty();
    }


    @Test
    void asyncClientWithEndpointProvider_serviceClientConfiguration_shouldReturnCorrectEndpointProvider() {
        ProtocolRestXmlEndpointProvider clientEndpointProvider = ProtocolRestXmlEndpointProvider.defaultProvider();
        SdkPlugin testPlugin = config -> config.endpointProvider(clientEndpointProvider);
        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .addPlugin(testPlugin)
                                                                      .build();

        EndpointProvider endpointProvider = client.serviceClientConfiguration().endpointProvider().orElse(null);
        assertThat(endpointProvider).isEqualTo(clientEndpointProvider);
    }

    @Test
    void asyncClientWithoutEndpointProvider_serviceClientConfiguration_shouldReturnDefault() {
        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .addPlugin(NOOP_PLUGIN)
                                                                      .build();

        EndpointProvider endpointProvider = client.serviceClientConfiguration().endpointProvider().orElse(null);
        assertThat(endpointProvider instanceof ProtocolRestXmlEndpointProvider).isTrue();
    }
}
