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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

public class ServiceClientConfigurationTest {

    @Test
    public void serviceClientConfiguration_shouldReturnCorrectRegion() {
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .region(Region.ME_SOUTH_1)
                                                            .credentialsProvider(StaticCredentialsProvider.create(
                                                                AwsBasicCredentials.create("foo", "bar")))
                                                            .build();

        String region = client.serviceClientConfiguration().region();
        assertThat(region).isEqualTo("me-south-1");
    }

    @Test
    public void serviceClientConfiguration_shouldReturnCorrectClientOverrideConfigurationFields() {
        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                                                                                       .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                                                                       .apiCallTimeout(Duration.ofSeconds(90))
                                                                                       .retryPolicy(c -> c.numRetries(4))
                                                                                       .build();

        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .overrideConfiguration(overrideConfiguration)
                                                            .build();


        assertThat(client.serviceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout().get()).isEqualTo(Duration.ofSeconds(30));
        assertThat(client.serviceClientConfiguration().overrideConfiguration().apiCallTimeout().get()).isEqualTo(Duration.ofSeconds(90));
        assertThat(client.serviceClientConfiguration().overrideConfiguration().retryPolicy().get().numRetries()).isEqualTo(4);
        assertThat(client.serviceClientConfiguration().overrideConfiguration().defaultProfileFile()).isNotPresent();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().metricPublishers()).isEmpty();
    }

    @Test
    public void serviceClientConfiguration_withoutOverrideConfiguration_shouldReturnEmptyFields () {
        ProtocolRestXmlClient client = ProtocolRestXmlClient.builder()
                                                            .build();

        assertThat(client.serviceClientConfiguration().overrideConfiguration().toString()).isEqualTo(
            "ClientOverrideConfiguration(headers={}, executionInterceptors=[], advancedOptions={})");
        assertThat(client.serviceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout()).isNotPresent();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().apiCallTimeout()).isNotPresent();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().retryPolicy()).isNotPresent();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().defaultProfileFile()).isNotPresent();
        assertThat(client.serviceClientConfiguration().overrideConfiguration().metricPublishers()).isEmpty();
    }

}
