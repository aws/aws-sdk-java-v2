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

package software.amazon.awssdk.services.compiledendpointrules.endpoints.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.compiledendpointrules.endpoints.CompiledEndpointRulesEndpointParams;
import software.amazon.awssdk.services.compiledendpointrules.endpoints.CompiledEndpointRulesEndpointProvider;

/**
 * Conformance test verifying that the build-time decomposed EndpointUrl (via {@code EndpointUrl.fromComponents()})
 * produces results equivalent to runtime parsing (via {@code EndpointUrl.fromString()}).

 * <p>This guards against drift between the build-time ({@code EndpointUrlCodeEmitter}) and runtime
 * ({@code EndpointUrl.fromString()}) URL decomposition logic.
 */
class EndpointUrlConformanceTest {

    private static final CompiledEndpointRulesEndpointProvider PROVIDER =
        CompiledEndpointRulesEndpointProvider.defaultProvider();

    /**
     * Test cases covering the standard region-based resolution path (uses fromComponents in generated code)
     * and the custom endpoint override path (uses fromString in generated code).
     */
    static List<TestCase> testCases() {
        return Arrays.asList(
            // Standard region resolution — exercises fromComponents code path
            TestCase.ofRegion("us-east-1", "https://compiledendpointrules.us-east-1.amazonaws.com"),
            TestCase.ofRegion("us-west-2", "https://compiledendpointrules.us-west-2.amazonaws.com"),
            TestCase.ofRegion("eu-west-1", "https://compiledendpointrules.eu-west-1.amazonaws.com"),
            TestCase.ofRegion("ap-southeast-1", "https://compiledendpointrules.ap-southeast-1.amazonaws.com"),
            TestCase.ofRegion("cn-north-1", "https://compiledendpointrules.cn-north-1.amazonaws.com.cn"),
            TestCase.ofRegion("us-gov-west-1", "https://compiledendpointrules.us-gov-west-1.amazonaws.com"),

            // Custom endpoint override — exercises fromString code path
            TestCase.ofEndpoint("us-east-1", "https://custom.example.com",
                               "https://custom.example.com"),
            TestCase.ofEndpoint("us-east-1", "https://custom.example.com:8443",
                               "https://custom.example.com:8443"),
            TestCase.ofEndpoint("us-east-1", "https://custom.example.com/base/path",
                               "https://custom.example.com/base/path"),
            TestCase.ofEndpoint("us-east-1", "http://localhost:4566",
                               "http://localhost:4566")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void resolvedEndpointUrl_componentsAreConsistentWithUri(TestCase testCase) throws Exception {
        CompletableFuture<Endpoint> future = PROVIDER.resolveEndpoint(testCase.params());
        Endpoint endpoint = future.get();
        EndpointUrl endpointUrl = endpoint.endpointUrl();

        // 1. Verify the resolved URL matches what we expect
        assertThat(endpointUrl.toUri()).isEqualTo(URI.create(testCase.expectedUrl));

        // 2. Verify component getters are consistent with toUri()
        URI uri = endpointUrl.toUri();
        assertThat(endpointUrl.scheme()).isEqualTo(uri.getScheme());
        assertThat(endpointUrl.host()).isEqualTo(uri.getHost());
        assertThat(endpointUrl.port()).isEqualTo(uri.getPort());
        String expectedPath = uri.getRawPath() != null ? uri.getRawPath() : "";
        assertThat(endpointUrl.encodedPath()).isEqualTo(expectedPath);

        // 3. Verify round-trip: reconstruct URL string from components, re-parse, and compare
        String reconstructed = endpointUrl.scheme() + "://" + endpointUrl.host()
                             + (endpointUrl.port() >= 0 ? ":" + endpointUrl.port() : "")
                             + endpointUrl.encodedPath();
        EndpointUrl reparsed = EndpointUrl.fromString(reconstructed);

        assertThat(reparsed.scheme()).isEqualTo(endpointUrl.scheme());
        assertThat(reparsed.host()).isEqualTo(endpointUrl.host());
        assertThat(reparsed.port()).isEqualTo(endpointUrl.port());
        assertThat(reparsed.encodedPath()).isEqualTo(endpointUrl.encodedPath());
        assertThat(reparsed).isEqualTo(endpointUrl);
    }

    static class TestCase {
        private final String description;
        private final Region region;
        private final String customEndpoint;
        private final String expectedUrl;

        TestCase(String description, Region region, String customEndpoint, String expectedUrl) {
            this.description = description;
            this.region = region;
            this.customEndpoint = customEndpoint;
            this.expectedUrl = expectedUrl;
        }

        static TestCase ofRegion(String regionId, String expectedUrl) {
            return new TestCase("region=" + regionId, Region.of(regionId), null, expectedUrl);
        }

        static TestCase ofEndpoint(String regionId, String endpoint, String expectedUrl) {
            return new TestCase("endpoint=" + endpoint, Region.of(regionId), endpoint, expectedUrl);
        }

        CompiledEndpointRulesEndpointParams params() {
            CompiledEndpointRulesEndpointParams.Builder builder = CompiledEndpointRulesEndpointParams.builder()
                .region(region);
            if (customEndpoint != null) {
                builder.endpoint(customEndpoint);
            }
            return builder.build();
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
