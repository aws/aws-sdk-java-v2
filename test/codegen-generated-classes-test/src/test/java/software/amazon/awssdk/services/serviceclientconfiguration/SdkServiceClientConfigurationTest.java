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

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

public class SdkServiceClientConfigurationTest {

    @Test
    public void syncClient_serviceClientConfiguration_shouldReturnCorrectClientOverrideConfigurationFields() {
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                                                                                       .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                                                                       .apiCallTimeout(Duration.ofSeconds(90))
                                                                                       .retryPolicy(c -> c.numRetries(4))
                                                                                       .build();

        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .overrideConfiguration(overrideConfiguration)
                                                            .build();


        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout().get()).isEqualTo(Duration.ofSeconds(30));
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallTimeout().get()).isEqualTo(Duration.ofSeconds(90));
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().retryPolicy().get().numRetries()).isEqualTo(4);
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().defaultProfileFile()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().metricPublishers()).isEmpty();
    }

    @Test
    public void syncClient_serviceClientConfiguration_withoutOverrideConfiguration_shouldReturnEmptyFields () {
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .build();

        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().toString()).isEqualTo(
            "ClientOverrideConfiguration(headers={}, executionInterceptors=[], advancedOptions={})");
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallTimeout()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().retryPolicy()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().defaultProfileFile()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().metricPublishers()).isEmpty();
    }

    @Test
    public void asyncClient_serviceClientConfiguration_shouldReturnCorrectClientOverrideConfigurationFields() {
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                                                                                       .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                                                                       .apiCallTimeout(Duration.ofSeconds(90))
                                                                                       .retryPolicy(c -> c.numRetries(4))
                                                                                       .build();

        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .overrideConfiguration(overrideConfiguration)
                                                                      .build();


        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout().get()).isEqualTo(Duration.ofSeconds(30));
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallTimeout().get()).isEqualTo(Duration.ofSeconds(90));
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().retryPolicy().get().numRetries()).isEqualTo(4);
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().defaultProfileFile()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().metricPublishers()).isEmpty();
    }

    @Test
    public void asyncClient_serviceClientConfiguration_withoutOverrideConfiguration_shouldReturnEmptyFields () {
        ProtocolRestXmlAsyncClient client = ProtocolRestXmlAsyncClient.builder()
                                                                      .build();

        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().toString()).isEqualTo(
            "ClientOverrideConfiguration(headers={}, executionInterceptors=[], advancedOptions={})");
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().apiCallTimeout()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().retryPolicy()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().defaultProfileFile()).isNotPresent();
        assertThat(client.sdkServiceClientConfiguration().overrideConfiguration().metricPublishers()).isEmpty();
    }

}
