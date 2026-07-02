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

class AwsClientEndpointProviderResolveFromOverridesTest {

    private static final String TEST_SYSTEM_PROPERTY = "aws.endpointUrlTestService";

    @AfterEach
    void cleanup() {
        System.clearProperty(TEST_SYSTEM_PROPERTY);
    }

    @Test
    void resolveFromOverrides_withClientEndpointOverride_returnsOverrideUri() {
        URI override = URI.create("https://custom-endpoint.example.com");

        Optional<URI> result = AwsClientEndpointProvider.builder()
            .clientEndpointOverride(override)
            .resolveFromOverrides();

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(override);
    }

    @Test
    void resolveFromOverrides_withSystemPropertyEndpoint_returnsEndpointUri() {
        System.setProperty(TEST_SYSTEM_PROPERTY, "https://sys-prop-endpoint.example.com");

        Optional<URI> result = AwsClientEndpointProvider.builder()
            .serviceEndpointOverrideSystemProperty(TEST_SYSTEM_PROPERTY)
            .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_TEST_SERVICE")
            .serviceProfileProperty("testservice")
            .resolveFromOverrides();

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(URI.create("https://sys-prop-endpoint.example.com"));
    }

    @Test
    void resolveFromOverrides_withNoOverrideOrEnvironment_returnsEmpty() {
        Optional<URI> result = AwsClientEndpointProvider.builder()
            .serviceEndpointOverrideSystemProperty("aws.endpointUrlNonExistent")
            .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_NON_EXISTENT")
            .serviceProfileProperty("nonexistent")
            .resolveFromOverrides();

        assertThat(result).isEmpty();
    }

    @Test
    void resolveFromOverrides_withNoParamsConfigured_returnsEmpty() {
        Optional<URI> result = AwsClientEndpointProvider.builder()
            .resolveFromOverrides();

        assertThat(result).isEmpty();
    }

}
