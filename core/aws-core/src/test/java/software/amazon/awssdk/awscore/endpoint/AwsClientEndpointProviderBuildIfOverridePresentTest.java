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

package software.amazon.awssdk.awscore.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientEndpointProvider;

class AwsClientEndpointProviderBuildIfOverridePresentTest {

    private static final String TEST_SYSTEM_PROPERTY = "aws.endpointUrlTestService";

    @AfterEach
    void cleanup() {
        System.clearProperty(TEST_SYSTEM_PROPERTY);
    }

    @Test
    void buildIfOverridePresent_withClientEndpointOverride_returnsProviderWithOverride() {
        URI override = URI.create("https://custom-endpoint.example.com");

        Optional<ClientEndpointProvider> result = AwsClientEndpointProvider.builder()
            .clientEndpointOverride(override)
            .buildIfOverridePresent();

        assertThat(result).isPresent();
        assertThat(result.get().clientEndpoint()).isEqualTo(override);
        assertThat(result.get().isEndpointOverridden()).isTrue();
    }

    @Test
    void buildIfOverridePresent_withSystemPropertyEndpoint_returnsProviderWithEndpoint() {
        System.setProperty(TEST_SYSTEM_PROPERTY, "https://sys-prop-endpoint.example.com");

        Optional<ClientEndpointProvider> result = AwsClientEndpointProvider.builder()
            .serviceEndpointOverrideSystemProperty(TEST_SYSTEM_PROPERTY)
            .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_TEST_SERVICE")
            .serviceProfileProperty("testservice")
            .buildIfOverridePresent();

        assertThat(result).isPresent();
        assertThat(result.get().clientEndpoint())
            .isEqualTo(URI.create("https://sys-prop-endpoint.example.com"));
        assertThat(result.get().isEndpointOverridden()).isTrue();
    }

    @Test
    void buildIfOverridePresent_withNoOverrideOrEnvironment_returnsEmpty() {
        Optional<ClientEndpointProvider> result = AwsClientEndpointProvider.builder()
            .serviceEndpointOverrideSystemProperty("aws.endpointUrlNonExistent")
            .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_NON_EXISTENT")
            .serviceProfileProperty("nonexistent")
            .buildIfOverridePresent();

        assertThat(result).isEmpty();
    }

    @Test
    void buildIfOverridePresent_withNoParamsConfigured_returnsEmpty() {
        Optional<ClientEndpointProvider> result = AwsClientEndpointProvider.builder()
            .buildIfOverridePresent();

        assertThat(result).isEmpty();
    }

    @Test
    void buildIfOverridePresent_withoutServiceMetadataParams_returnsProviderWithOverride() {
        URI override = URI.create("https://override.example.com");

        Optional<ClientEndpointProvider> result = AwsClientEndpointProvider.builder()
            .clientEndpointOverride(override)
            .buildIfOverridePresent();

        assertThat(result).isPresent();
        assertThat(result.get().clientEndpoint()).isEqualTo(override);
    }
}
