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

package software.amazon.awssdk.imds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static software.amazon.awssdk.imds.EndpointMode.IPV4;
import static software.amazon.awssdk.imds.EndpointMode.IPV6;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.imds.internal.DefaultEc2MetadataEndpointProvider;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Test Class to test the endpoint resolution functionality.
 */
class EndpointProviderTest {

    private EnvironmentVariableHelper settingsHelper;

    @BeforeEach
    void init() {
        settingsHelper = new EnvironmentVariableHelper();
    }

    @AfterEach
    void reset() {
        settingsHelper.reset();
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property());
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    private static Stream<Arguments> provideEndpointAndEndpointModes() {
        String testIpv4Url = "http://90:90:90:90";
        String testIpv6Url = "http://[9876:ec2::123]";
        return Stream.of(
            arguments(null, true, testIpv4Url, true, "testIPv6", testIpv4Url),
            arguments(null, true, testIpv6Url, true, "testIPv4", testIpv6Url),

            arguments(null, true, testIpv4Url, false, "testIPv6", testIpv4Url),
            arguments(null, true, testIpv6Url, false, "testIPv4", testIpv6Url),

            Arguments.of(null, false, "unused", true, "testIPv6", "[1234:ec2::456]"),
            Arguments.of(null, false, "unused", true, "testIPv4", "http://42.42.42.42"),

            arguments(IPV4, false, "unused", false, "unused", IPV4.getServiceEndpoint()),
            arguments(IPV6, false, "unused", false, "unused", IPV6.getServiceEndpoint())
        );
    }

    @ParameterizedTest
    @MethodSource("provideEndpointAndEndpointModes")
    void validateResolveEndpoint(EndpointMode endpointMode,
                                 boolean setEnvVariable,
                                 String envEndpoint,
                                 boolean setConfigFile,
                                 String profile,
                                 String expectedValue)
        throws URISyntaxException {

        if (setEnvVariable) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), envEndpoint);
        }

        if (setConfigFile) {
            String testFile = "/profile-config/test-profiles.tst";
            settingsHelper.set(ProfileFileSystemSetting.AWS_PROFILE, profile);
            settingsHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE,
                               Paths.get(getClass().getResource(testFile).toURI()).toString());
        }

        DefaultEc2MetadataEndpointProvider endpointProvider = DefaultEc2MetadataEndpointProvider.builder().build();
        String endpoint = endpointProvider.resolveEndpoint(endpointMode);
        assertThat(endpoint).isEqualTo(expectedValue);
    }

    private static Stream<Arguments> provideEndpointModes() {
        return Stream.of(
            arguments(false, "unused", false, "unused", IPV4),
            arguments(true, "IPv4", true, "IPv4", IPV4),
            arguments(true, "IPv6", true, "IPv6", IPV6),
            arguments(true, "IPv6", true, "IPv4", IPV6),
            arguments(true, "IPv4", true, "IPv6", IPV4),
            arguments(false, "unused", true, "IPv6", IPV6),
            arguments(false, "unused", true, "IPv4", IPV4),
            arguments(true, "IPv6", false, "unused", IPV6),
            arguments(true, "IPv4", false, "unused", IPV4)
        );
    }

    @ParameterizedTest
    @MethodSource("provideEndpointModes")
     void endpointModeCheck(boolean useEnvVariable, String envVarValue, boolean useConfigFile, String configFileValue,
                            EndpointMode expectedValue)
        throws URISyntaxException {

        if (useEnvVariable) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property(), envVarValue);
        }

        if (useConfigFile) {
            String testFile = "/profile-config/test-profiles.tst";
            settingsHelper.set(ProfileFileSystemSetting.AWS_PROFILE, "test" + configFileValue);
            settingsHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE,
                           Paths.get(getClass().getResource(testFile).toURI()).toString());
        }

        DefaultEc2MetadataEndpointProvider endpointProvider = DefaultEc2MetadataEndpointProvider.builder().build();
        EndpointMode endpointMode = endpointProvider.resolveEndpointMode();

        assertThat(endpointMode).isEqualTo(expectedValue);
    }

}
