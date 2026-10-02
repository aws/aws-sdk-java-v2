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

package software.amazon.awssdk.services.endpointproviders;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.EndpointCapturingInterceptor;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;

/**
 * Tests that the generated client builder resolves a default endpoint when no override is configured,
 * and correctly uses endpoint overrides from client configuration, system properties, and environment sources.
 */
public class ClientEndpointResolutionTest {

    private static final String SERVICE_SYSTEM_PROPERTY = "aws.endpointUrlRestJsonEndpointProviders";

    @AfterEach
    void cleanup() {
        System.clearProperty(SERVICE_SYSTEM_PROPERTY);
    }

    @Test
    void clientBuild_withRegionOnly_resolvesEndpointViaEndpoints2() {
        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();

        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .build();

        try {
            client.operationWithNoInputOrOutput(r -> {});
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // Expected
        }

        assertThat(interceptor.endpoints())
            .singleElement()
            .asString()
            .contains("us-west-2")
            .contains("amazonaws.com");
    }

    @Test
    void clientBuild_withSystemPropertyOverride_usesOverrideEndpoint() {
        System.setProperty(SERVICE_SYSTEM_PROPERTY, "https://custom-override.example.com");

        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();

        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .build();

        try {
            client.operationWithNoInputOrOutput(r -> {});
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // Expected
        }

        assertThat(interceptor.endpoints())
            .singleElement()
            .asString()
            .startsWith("https://custom-override.example.com");
    }

    @Test
    void clientBuild_withEndpointOverride_usesClientOverride() {
        EndpointCapturingInterceptor interceptor = new EndpointCapturingInterceptor();

        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
            .region(Region.US_WEST_2)
            .endpointOverride(URI.create("https://my-override.example.com"))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .build();

        try {
            client.operationWithNoInputOrOutput(r -> {});
        } catch (EndpointCapturingInterceptor.CaptureCompletedException e) {
            // Expected
        }

        assertThat(interceptor.endpoints())
            .singleElement()
            .asString()
            .startsWith("https://my-override.example.com");
    }

}
