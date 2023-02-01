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

package software.amazon.awssdk.imds.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static software.amazon.awssdk.imds.EndpointMode.IPV4;
import static software.amazon.awssdk.imds.EndpointMode.IPV6;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;

/**
 * Test Class to test the endpoint resolution functionality.
 */
class EndpointProviderTest {

    @AfterEach
    void reset() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property());
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
    }

    private static Stream<Arguments> provideEndpointAndEndpointModes() {
        String testIpv4Url = "http://90:90:90:90";
        String testIpv6Url = "http://[9876:ec2::123]";
        return Stream.of(
            arguments(null, true, testIpv4Url, true, "testIPv6", testIpv4Url),
            arguments(null, true, testIpv6Url, true, "testIPv4", testIpv6Url),

            arguments(null, true, testIpv4Url, false, "testIPv6", testIpv4Url),
            arguments(null, true, testIpv6Url, false, "testIPv4", testIpv6Url),

            arguments(null, false, "unused", true, "testIPv6", "[1234:ec2::456]"),
            arguments(null, false, "unused", true, "testIPv4", "http://42.42.42.42"),

            arguments(IPV4, false, "unused", false, "unused", "http://169.254.169.254"),
            arguments(IPV6, false, "unused", false, "unused", "http://[fd00:ec2::254]")
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
            SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
                ProfileFileSystemSetting.AWS_PROFILE.environmentVariable(),
                profile);
            SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
                ProfileFileSystemSetting.AWS_CONFIG_FILE.environmentVariable(),
                Paths.get(getClass().getResource(testFile).toURI()).toString());
        }

        Ec2MetadataEndpointProvider endpointProvider = Ec2MetadataEndpointProvider.builder().build();
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
            SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
                ProfileFileSystemSetting.AWS_PROFILE.environmentVariable(),
                "test" + configFileValue);

            SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride(
                ProfileFileSystemSetting.AWS_CONFIG_FILE.environmentVariable(),
                Paths.get(getClass().getResource(testFile).toURI()).toString());
        }

        Ec2MetadataEndpointProvider endpointProvider = Ec2MetadataEndpointProvider.builder().build();
        EndpointMode endpointMode = endpointProvider.resolveEndpointMode();

        assertThat(endpointMode).isEqualTo(expectedValue);
    }

    @Test
    void endpointFromBuilder_withIpv4_shouldBesetCorrectly() {
        ProfileFile.Builder content = ProfileFile.builder()
            .type(ProfileFile.Type.CONFIGURATION)
                                                 .content(Paths.get("src/test/resources/profile-config/test-profiles.tst"));
        Ec2MetadataEndpointProvider provider = Ec2MetadataEndpointProvider.builder()
                                                                          .profileFile(content::build)
                                                                          .profileName("testIPv4")
                                                                          .build();
        assertThat(provider.resolveEndpointMode()).isEqualTo(IPV4);
        assertThat(provider.resolveEndpoint(IPV4)).isEqualTo("http://42.42.42.42");
    }

    @Test
    void endpointFromBuilder_withIpv6_shouldBesetCorrectly() {
        ProfileFile.Builder content = ProfileFile.builder()
                                                 .type(ProfileFile.Type.CONFIGURATION)
                                                 .content(Paths.get("src/test/resources/profile-config/test-profiles.tst"));
        Ec2MetadataEndpointProvider provider = Ec2MetadataEndpointProvider.builder()
                                                                          .profileFile(content::build)
                                                                          .profileName("testIPv6")
                                                                          .build();
        assertThat(provider.resolveEndpointMode()).isEqualTo(IPV6);
        assertThat(provider.resolveEndpoint(IPV6)).isEqualTo("[1234:ec2::456]");
    }


}
