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
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.services.protocolrestxmlinternalplugins.ProtocolRestXmlInternalPluginsAsyncClient;
import software.amazon.awssdk.services.protocolrestxmlinternalplugins.ProtocolRestXmlInternalPluginsClient;

public class ServiceClientConfigurationUsingInternalPluginsTest {
    private static final SdkPlugin NOOP_PLUGIN = config -> {};

    private static final URI ENDPOINT_OVERRIDE_INTERNAL_TEST_PLUGIN_URI = URI.create("http://127.0.0.1");

    @Test
    void syncClientWithExternalPluginEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        URI uri = URI.create("https://www.amazon.com/");
        ProtocolRestXmlInternalPluginsClient client = ProtocolRestXmlInternalPluginsClient.builder()
                                                                           .addPlugin(config -> config.endpointOverride(uri))
                                                                           .build();

        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(uri);
    }

    @Test
    void syncClientWithoutExternalPluginEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        ProtocolRestXmlInternalPluginsClient client = ProtocolRestXmlInternalPluginsClient.builder()
                                                            .addPlugin(NOOP_PLUGIN)
                                                            .build();
        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(ENDPOINT_OVERRIDE_INTERNAL_TEST_PLUGIN_URI);
    }

    @Test
    void syncClientWithoutEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        ProtocolRestXmlInternalPluginsClient client = ProtocolRestXmlInternalPluginsClient.builder()
                                                                                          .build();
        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(ENDPOINT_OVERRIDE_INTERNAL_TEST_PLUGIN_URI);
    }

    @Test
    void asyncClientWithExternalPluginEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        URI uri = URI.create("https://www.amazon.com/");
        ProtocolRestXmlInternalPluginsAsyncClient client = ProtocolRestXmlInternalPluginsAsyncClient.builder()
                                                                                               .addPlugin(config -> config.endpointOverride(uri))
                                                                                               .build();

        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(uri);
    }

    @Test
    void asyncClientWithoutExternalPluginEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        ProtocolRestXmlInternalPluginsAsyncClient client = ProtocolRestXmlInternalPluginsAsyncClient.builder()
                                                                                          .addPlugin(NOOP_PLUGIN)
                                                                                          .build();
        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(ENDPOINT_OVERRIDE_INTERNAL_TEST_PLUGIN_URI);
    }

    @Test
    void asyncClientWithoutEndpointOverride_serviceClientConfiguration_shouldReturnCorrectEndpointOverride() {
        ProtocolRestXmlInternalPluginsAsyncClient client = ProtocolRestXmlInternalPluginsAsyncClient.builder()
                                                                                          .build();
        URI endpointOverride = client.serviceClientConfiguration().endpointOverride().orElse(null);
        assertThat(endpointOverride).isEqualTo(ENDPOINT_OVERRIDE_INTERNAL_TEST_PLUGIN_URI);
    }
}
