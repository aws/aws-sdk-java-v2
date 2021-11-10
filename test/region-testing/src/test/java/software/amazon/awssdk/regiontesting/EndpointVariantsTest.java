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

package software.amazon.awssdk.regiontesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.regions.EndpointTag.DUALSTACK;
import static software.amazon.awssdk.regions.EndpointTag.FIPS;

import java.net.URI;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.regions.EndpointTag;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceEndpointKey;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.utils.Validate;

@RunWith(Parameterized.class)
public class EndpointVariantsTest {
    @Parameterized.Parameter
    public TestCase testCase;

    @Test
    public void resolvesCorrectEndpoint() {
        if (testCase instanceof SuccessCase) {
            URI endpoint = ServiceMetadata.of(testCase.service).endpointFor(testCase.endpointKey);
            assertThat(endpoint).isEqualTo(((SuccessCase) testCase).endpoint);
        } else {
            FailureCase failureCase = Validate.isInstanceOf(FailureCase.class, testCase, "Unknown case type %s.", getClass());
            assertThatThrownBy(() -> ServiceMetadata.of(testCase.service).endpointFor(testCase.endpointKey))
                .hasMessageContaining(failureCase.exceptionMessage);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<TestCase> testCases() {
        return Arrays.asList(new SuccessCase("default-pattern-service", "us-west-2", "default-pattern-service.us-west-2.amazonaws.com"),
                             new SuccessCase("default-pattern-service", "us-west-2", "default-pattern-service-fips.us-west-2.amazonaws.com", FIPS),
                             new SuccessCase("default-pattern-service", "af-south-1", "default-pattern-service.af-south-1.amazonaws.com"),
                             new SuccessCase("default-pattern-service", "af-south-1", "default-pattern-service-fips.af-south-1.amazonaws.com", FIPS),
                             new SuccessCase("global-service", "aws-global", "global-service.amazonaws.com"),
                             new SuccessCase("global-service", "aws-global", "global-service-fips.amazonaws.com", FIPS),
                             new SuccessCase("override-variant-service", "us-west-2", "override-variant-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-variant-service", "us-west-2", "fips.override-variant-service.us-west-2.new.dns.suffix", FIPS),
                             new SuccessCase("override-variant-service", "af-south-1", "override-variant-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-variant-service", "af-south-1", "fips.override-variant-service.af-south-1.new.dns.suffix", FIPS),
                             new SuccessCase("override-variant-dns-suffix-service", "us-west-2", "override-variant-dns-suffix-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-variant-dns-suffix-service", "us-west-2", "override-variant-dns-suffix-service-fips.us-west-2.new.dns.suffix", FIPS),
                             new SuccessCase("override-variant-dns-suffix-service", "af-south-1", "override-variant-dns-suffix-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-variant-dns-suffix-service", "af-south-1", "override-variant-dns-suffix-service-fips.af-south-1.new.dns.suffix", FIPS),
                             new SuccessCase("override-variant-hostname-service", "us-west-2", "override-variant-hostname-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-variant-hostname-service", "us-west-2", "fips.override-variant-hostname-service.us-west-2.amazonaws.com", FIPS),
                             new SuccessCase("override-variant-hostname-service", "af-south-1", "override-variant-hostname-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-variant-hostname-service", "af-south-1", "fips.override-variant-hostname-service.af-south-1.amazonaws.com", FIPS),
                             new SuccessCase("override-endpoint-variant-service", "us-west-2", "override-endpoint-variant-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-endpoint-variant-service", "us-west-2", "fips.override-endpoint-variant-service.us-west-2.amazonaws.com", FIPS),
                             new SuccessCase("override-endpoint-variant-service", "af-south-1", "override-endpoint-variant-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-endpoint-variant-service", "af-south-1", "override-endpoint-variant-service-fips.af-south-1.amazonaws.com", FIPS),
                             new SuccessCase("default-pattern-service", "us-west-2", "default-pattern-service.us-west-2.amazonaws.com"),
                             new SuccessCase("default-pattern-service", "us-west-2", "default-pattern-service.us-west-2.api.aws", DUALSTACK),
                             new SuccessCase("default-pattern-service", "af-south-1", "default-pattern-service.af-south-1.amazonaws.com"),
                             new SuccessCase("default-pattern-service", "af-south-1", "default-pattern-service.af-south-1.api.aws", DUALSTACK),
                             new SuccessCase("global-service", "aws-global", "global-service.amazonaws.com"),
                             new SuccessCase("global-service", "aws-global", "global-service.api.aws", DUALSTACK),
                             new SuccessCase("override-variant-service", "us-west-2", "override-variant-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-variant-service", "us-west-2", "override-variant-service.dualstack.us-west-2.new.dns.suffix", DUALSTACK),
                             new SuccessCase("override-variant-service", "af-south-1", "override-variant-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-variant-service", "af-south-1", "override-variant-service.dualstack.af-south-1.new.dns.suffix", DUALSTACK),
                             new SuccessCase("override-variant-dns-suffix-service", "us-west-2", "override-variant-dns-suffix-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-variant-dns-suffix-service", "us-west-2", "override-variant-dns-suffix-service.us-west-2.new.dns.suffix", DUALSTACK),
                             new SuccessCase("override-variant-dns-suffix-service", "af-south-1", "override-variant-dns-suffix-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-variant-dns-suffix-service", "af-south-1", "override-variant-dns-suffix-service.af-south-1.new.dns.suffix", DUALSTACK),
                             new SuccessCase("override-variant-hostname-service", "us-west-2", "override-variant-hostname-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-variant-hostname-service", "us-west-2", "override-variant-hostname-service.dualstack.us-west-2.api.aws", DUALSTACK),
                             new SuccessCase("override-variant-hostname-service", "af-south-1", "override-variant-hostname-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-variant-hostname-service", "af-south-1", "override-variant-hostname-service.dualstack.af-south-1.api.aws", DUALSTACK),
                             new SuccessCase("override-endpoint-variant-service", "us-west-2", "override-endpoint-variant-service.us-west-2.amazonaws.com"),
                             new SuccessCase("override-endpoint-variant-service", "us-west-2", "override-endpoint-variant-service.dualstack.us-west-2.amazonaws.com", DUALSTACK),
                             new SuccessCase("override-endpoint-variant-service", "af-south-1", "override-endpoint-variant-service.af-south-1.amazonaws.com"),
                             new SuccessCase("override-endpoint-variant-service", "af-south-1", "override-endpoint-variant-service.af-south-1.api.aws", DUALSTACK),
                             new SuccessCase("multi-variant-service", "us-west-2", "multi-variant-service.us-west-2.amazonaws.com"),
                             new SuccessCase("multi-variant-service", "us-west-2", "multi-variant-service.dualstack.us-west-2.api.aws", DUALSTACK),
                             new SuccessCase("multi-variant-service", "us-west-2", "fips.multi-variant-service.us-west-2.amazonaws.com", FIPS),
                             new SuccessCase("multi-variant-service", "us-west-2", "fips.multi-variant-service.dualstack.us-west-2.new.dns.suffix", FIPS, DUALSTACK),
                             new SuccessCase("multi-variant-service", "af-south-1", "multi-variant-service.af-south-1.amazonaws.com"),
                             new SuccessCase("multi-variant-service", "af-south-1", "multi-variant-service.dualstack.af-south-1.api.aws", DUALSTACK),
                             new SuccessCase("multi-variant-service", "af-south-1", "fips.multi-variant-service.af-south-1.amazonaws.com", FIPS),
                             new SuccessCase("multi-variant-service", "af-south-1", "fips.multi-variant-service.dualstack.af-south-1.new.dns.suffix", FIPS, DUALSTACK),
                             new FailureCase("some-service", "us-iso-east-1", "No endpoint known for [dualstack] in us-iso-east-1", DUALSTACK),
                             new FailureCase("some-service", "us-iso-east-1", "No endpoint known for [fips] in us-iso-east-1", FIPS),
                             new FailureCase("some-service", "us-iso-east-1", "No endpoint known for [fips, dualstack] in us-iso-east-1", FIPS, DUALSTACK),

                             // These test case outcomes deviate from the other SDKs, because we ignore "global" services when a
                             // non-global region is specified. We might consider changing this behavior, since the storm of
                             // global services becoming regionalized is dying down.
                             new SuccessCase("global-service", "foo", "global-service.foo.amazonaws.com"),
                             new SuccessCase("global-service", "foo", "global-service-fips.foo.amazonaws.com", FIPS),
                             new SuccessCase("global-service", "foo", "global-service.foo.amazonaws.com"),
                             new SuccessCase("global-service", "foo", "global-service.foo.api.aws", DUALSTACK));
    }

    public static class TestCase {
        private final String service;
        private final ServiceEndpointKey endpointKey;

        public TestCase(String service, String region, EndpointTag... tags) {
            this.service = service;
            this.endpointKey = ServiceEndpointKey.builder().region(Region.of(region)).tags(tags).build();
        }
    }

    public static class SuccessCase extends TestCase {
        private final URI endpoint;

        public SuccessCase(String service, String region, String endpoint, EndpointTag... tags) {
            super(service, region, tags);
            this.endpoint = URI.create(endpoint);
        }

        @Override
        public String toString() {
            return "Positive Case: " + endpoint.toString();
        }
    }

    private static class FailureCase extends TestCase {
        private final String exceptionMessage;

        public FailureCase(String service, String region, String exceptionMessage, EndpointTag... tags) {
            super(service, region, tags);
            this.exceptionMessage = exceptionMessage;
        }

        @Override
        public String toString() {
            return "Negative Case: " + exceptionMessage;
        }
    }
}
