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

import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private final EnvironmentVariableHelper env = new EnvironmentVariableHelper();
    private String savedRegionProperty;

    @BeforeEach
    void setup() {
        savedRegionProperty = System.getProperty(REGION_PROPERTY);
        System.clearProperty(REGION_PROPERTY);
        env.remove(AWS_REGION_ENV);
        env.remove(AWS_DEFAULT_REGION_ENV);
    }

    @AfterEach
    void teardown() {
        env.reset();
        if (savedRegionProperty != null) {
            System.setProperty(REGION_PROPERTY, savedRegionProperty);
        } else {
            System.clearProperty(REGION_PROPERTY);
        }
    }

    @Test
    void stsEndpoint_noConfiguration_usesDefaultRegion() {
        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts.us-east-1.amazonaws.com/"));
    }

    @Test
    void stsEndpoint_systemProperty_takesPrecedenceOverEnvVars() {
        System.setProperty(REGION_PROPERTY, "eu-west-1");
        env.set(AWS_REGION_ENV, "ap-south-1");
        env.set(AWS_DEFAULT_REGION_ENV, "us-west-2");

        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts.eu-west-1.amazonaws.com/"));
    }

    @Test
    void stsEndpoint_awsRegionEnv_takesPrecedenceOverDefaultRegionEnv() {
        env.set(AWS_REGION_ENV, "ap-south-1");
        env.set(AWS_DEFAULT_REGION_ENV, "us-west-2");

        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts.ap-south-1.amazonaws.com/"));
    }

    @Test
    void stsEndpoint_awsDefaultRegionEnv_usedWhenNothingElseSet() {
        env.set(AWS_DEFAULT_REGION_ENV, "us-west-2");

        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts.us-west-2.amazonaws.com/"));
    }

    @Test
    void stsEndpoint_blankSystemProperty_fallsThroughToNextTier() {
        System.setProperty(REGION_PROPERTY, "   ");
        env.set(AWS_REGION_ENV, "ap-south-1");

        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts.ap-south-1.amazonaws.com/"));
    }

    @Test
    void stsEndpoint_blankAwsRegionEnv_fallsThroughToDefaultRegionEnv() {
        env.set(AWS_REGION_ENV, "   ");
        env.set(AWS_DEFAULT_REGION_ENV, "us-west-2");

        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts.us-west-2.amazonaws.com/"));
    }

    @Test
    void stsEndpoint_configuredRegionIsTrimmed() {
        System.setProperty(REGION_PROPERTY, "  eu-central-1  ");

        assertThat(RegionEndpointResolver.create().stsEndpoint())
            .isEqualTo(URI.create("https://sts.eu-central-1.amazonaws.com/"));
    }

    @Test
    void stsEndpoint_doesNotCallImds() {
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
}
