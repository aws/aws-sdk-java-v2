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

/**
 * Unit tests for {@link RegionEndpointResolver}.
 *
 * <p>Region resolution precedence under test (system property first, per the SDK's {@code SystemSetting} convention):
 * {@code aws.region} system property, then {@code AWS_REGION} env, then {@code AWS_DEFAULT_REGION} env, then the
 * {@code us-east-1} default.
 */
class RegionEndpointResolverTest {

    private static final String REGION_PROPERTY = "aws.region";
    private static final String AWS_REGION_ENV = "AWS_REGION";
    private static final String AWS_DEFAULT_REGION_ENV = "AWS_DEFAULT_REGION";

    private static final EnvironmentVariableHelper ENV = new EnvironmentVariableHelper();

    @BeforeEach
    void clearSettings() {
        ENV.reset();
        System.clearProperty(REGION_PROPERTY);
    }

    @AfterEach
    void restoreSettings() {
        ENV.reset();
        System.clearProperty(REGION_PROPERTY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("regionResolutionCases")
    void stsEndpoint_whenRegionConfigured_resolvesExpectedEndpoint(String description, String sysprop, String awsRegion,
                                                                   String awsDefaultRegion, String expectedRegion) {
        applyRegionSettings(sysprop, awsRegion, awsDefaultRegion);

        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts." + expectedRegion + ".amazonaws.com/"));
    }

    private static Stream<Arguments> regionResolutionCases() {
        return Stream.of(
            //        description                                      aws.region      AWS_REGION    AWS_DEFAULT_REGION  expected
            arguments("nothing set -> default region",                 null,           null,         null,               "us-east-1"),
            arguments("system property wins over both env vars",       "eu-west-1",    "ap-south-1", "us-west-2",        "eu-west-1"),
            arguments("AWS_REGION wins over AWS_DEFAULT_REGION",        null,           "ap-south-1", "us-west-2",        "ap-south-1"),
            arguments("AWS_DEFAULT_REGION used when nothing else set",  null,           null,         "us-west-2",        "us-west-2"),
            arguments("blank system property falls through to AWS_REGION", "   ",       "ap-south-1", null,               "ap-south-1"),
            arguments("blank AWS_REGION falls through to AWS_DEFAULT_REGION", null,      "   ",        "us-west-2",        "us-west-2"),
            arguments("chosen value is trimmed",                       "  eu-central-1  ", null,      null,               "eu-central-1")
        );
    }

    @Test
    void stsEndpoint_whenImdsUnreachable_resolvesWithoutCallingImds() {
        // Point IMDS at a non-routable address. A resolver that (incorrectly) called IMDS would block here;
        // the correct resolver ignores IMDS entirely and returns the default region immediately.
        String savedImdsEndpoint = System.getProperty("aws.ec2MetadataServiceEndpoint");
        System.setProperty("aws.ec2MetadataServiceEndpoint", "http://10.255.255.1");
        try {
            long startNanos = System.nanoTime();
            URI endpoint = RegionEndpointResolver.create().stsEndpoint();
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

    private static void applyRegionSettings(String sysprop, String awsRegion, String awsDefaultRegion) {
        if (sysprop != null) {
            System.setProperty(REGION_PROPERTY, sysprop);
        }
        if (awsRegion != null) {
            ENV.set(AWS_REGION_ENV, awsRegion);
        }
        if (awsDefaultRegion != null) {
            ENV.set(AWS_DEFAULT_REGION_ENV, awsDefaultRegion);
        }
    }
}
