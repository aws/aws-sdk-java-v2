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

package software.amazon.awssdk.auth.credentials.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkSystemSetting;

@RunWith(Parameterized.class)
public class Ec2MetadataConfigProviderEndpointResolutionTest {
    private static final String EC2_METADATA_SERVICE_URL_IPV4 = "http://169.254.169.254";
    private static final String EC2_METADATA_SERVICE_URL_IPV6 = "http://[fd00:ec2::254]";
    private static final String ENDPOINT_OVERRIDE = "http://my-custom-endpoint";

    @Parameterized.Parameter
    public TestCase testCase;

    @Parameterized.Parameters
    public static Iterable<Object> testCases() {
        return Arrays.asList(
                new TestCase().expectedEndpoint(EC2_METADATA_SERVICE_URL_IPV4),
                new TestCase().endpointMode("ipv6").expectedEndpoint(EC2_METADATA_SERVICE_URL_IPV6),
                new TestCase().endpointMode("ipv4").expectedEndpoint(EC2_METADATA_SERVICE_URL_IPV4),

                new TestCase().endpointOverride(ENDPOINT_OVERRIDE).expectedEndpoint(ENDPOINT_OVERRIDE),
                new TestCase().endpointMode("ipv4").endpointOverride(ENDPOINT_OVERRIDE).expectedEndpoint(ENDPOINT_OVERRIDE),
                new TestCase().endpointMode("ipv6").endpointOverride(ENDPOINT_OVERRIDE).expectedEndpoint(ENDPOINT_OVERRIDE),
                new TestCase().endpointMode("ipv99").endpointOverride(ENDPOINT_OVERRIDE).expectedEndpoint(ENDPOINT_OVERRIDE)
        );
    }

    @Before
    public void setup() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property());

        if (testCase.endpointMode != null) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property(), testCase.endpointMode);
        }

        if (testCase.endpointOverride != null) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), testCase.endpointOverride);
        }
    }

    @Test
    public void resolvesCorrectEndpoint() {
        assertThat(Ec2MetadataConfigProvider.builder().build().getEndpoint()).isEqualTo(testCase.expectedEndpoint);
    }

    private static class TestCase {
        private String endpointMode;
        private String endpointOverride;

        private String expectedEndpoint;

        public TestCase endpointMode(String endpointMode) {
            this.endpointMode = endpointMode;
            return this;
        }

        public TestCase endpointOverride(String endpointOverride) {
            this.endpointOverride = endpointOverride;
            return this;
        }

        public TestCase expectedEndpoint(String expectedEndpoint) {
            this.expectedEndpoint = expectedEndpoint;
            return this;
        }
    }
}