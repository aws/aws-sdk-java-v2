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

package software.amazon.awssdk.core.internal.crac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

class RegionEndpointResolverTest {

    private static final String REGION_PROPERTY = "aws.region";
    private static final String AWS_REGION_ENV = "AWS_REGION";
    private static final String AWS_DEFAULT_REGION_ENV = "AWS_DEFAULT_REGION";

    private static final EnvironmentVariableHelper ENV = new EnvironmentVariableHelper();

    @BeforeEach
    void clearSettings() {
        clearRegionSettings();
    }

    @AfterEach
    void restoreSettings() {
        clearRegionSettings();
    }

    private void clearRegionSettings() {
        ENV.reset();
        ENV.remove(AWS_REGION_ENV);
        ENV.remove(AWS_DEFAULT_REGION_ENV);
        System.clearProperty(REGION_PROPERTY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("regionResolutionCases")
    void endpoint_whenRegionConfigured_resolvesExpectedEndpoint(String description, String sysprop, String awsRegion,
                                                                String expectedRegion) {
        applyRegionSettings(sysprop, awsRegion);

        assertThat(RegionEndpointResolver.create().endpoint())
            .isEqualTo(URI.create("https://sts." + expectedRegion + ".amazonaws.com/"));
    }

    private static Stream<Arguments> regionResolutionCases() {
        return Stream.of(
            //        description                                          aws.region        AWS_REGION    expected
            arguments("nothing set -> default region",                     null,             null,         "us-east-1"),
            arguments("system property wins over AWS_REGION",              "eu-west-1",      "ap-south-1", "eu-west-1"),
            arguments("AWS_REGION used when no system property",           null,             "ap-south-1", "ap-south-1"),
            arguments("blank system property short-circuits AWS_REGION",   "   ",            "ap-south-1", "us-east-1"),
            arguments("blank AWS_REGION falls through to default",         null,             "   ",        "us-east-1"),
            arguments("chosen value is trimmed",                           "  eu-central-1  ", null,       "eu-central-1")
        );
    }

    @Test
    void endpoint_whenOnlyAwsDefaultRegionSet_ignoresItAndUsesDefault() {
        ENV.set(AWS_DEFAULT_REGION_ENV, "us-west-2");

        assertThat(RegionEndpointResolver.create().endpoint())
            .isEqualTo(URI.create("https://sts.us-east-1.amazonaws.com/"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("untrustedRegionCases")
    void endpoint_whenRegionNotHostnameCompliant_fallsBackToDefault(String description, String region) {
        System.setProperty(REGION_PROPERTY, region);

        URI endpoint = RegionEndpointResolver.create().endpoint();

        // A non-compliant region cannot reach the host; it is rejected and the default region is used instead.
        assertThat(endpoint).isEqualTo(URI.create("https://sts.us-east-1.amazonaws.com/"));
    }

    private static Stream<Arguments> untrustedRegionCases() {
        return Stream.of(
            arguments("newline is rejected",          "us-east-1\nfoo"),
            arguments("carriage return is rejected",   "us-east-1\rfoo"),
            arguments("forward slash is rejected",     "us-east-1/foo"),
            arguments("space is rejected",             "us-east-1 foo"),
            arguments("at sign is rejected",           "evil@host"),
            arguments("hash is rejected",              "host#fragment"),
            arguments("dot is rejected",               "sts.evil.com")
        );
    }

    @Test
    void endpoint_whenImdsUnreachable_resolvesWithoutCallingImds() {
        // Point IMDS at a non-routable address. A resolver that (incorrectly) called IMDS would block here;
        // the correct resolver ignores IMDS entirely and returns the default region immediately.
        String savedImdsEndpoint = System.getProperty("aws.ec2MetadataServiceEndpoint");
        System.setProperty("aws.ec2MetadataServiceEndpoint", "http://10.255.255.1");
        try {
            long startNanos = System.nanoTime();
            URI endpoint = RegionEndpointResolver.create().endpoint();
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

            assertThat(endpoint).isEqualTo(URI.create("https://sts.us-east-1.amazonaws.com/"));
            assertThat(elapsedMillis).isLessThan(1_000L);
        } finally {
            if (savedImdsEndpoint != null) {
                System.setProperty("aws.ec2MetadataServiceEndpoint", savedImdsEndpoint);
            } else {
                System.clearProperty("aws.ec2MetadataServiceEndpoint");
            }
        }
    }

    private static void applyRegionSettings(String sysprop, String awsRegion) {
        if (sysprop != null) {
            System.setProperty(REGION_PROPERTY, sysprop);
        }
        if (awsRegion != null) {
            ENV.set(AWS_REGION_ENV, awsRegion);
        }
    }
}
