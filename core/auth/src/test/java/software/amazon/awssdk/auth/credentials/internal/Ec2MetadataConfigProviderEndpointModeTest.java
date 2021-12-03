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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class Ec2MetadataConfigProviderEndpointModeTest {
    private static final String TEST_PROFILES_PATH_PREFIX = "/software/amazon/awssdk/auth/credentials/internal/ec2metadataconfigprovider/";
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    private static final String CUSTOM_PROFILE = "myprofile";

    public static Stream<Arguments> testData() {
        return Stream.of(
            arguments(null, null, null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV4, null),
            arguments("ipv4", null, null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV4, null),
            arguments("IPv4", null, null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV4, null),
            arguments("ipv6", null, null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV6, null),
            arguments("IPv6", null, null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV6, null),
            arguments("Ipv99", null, null, null, null, null, IllegalArgumentException.class),

            arguments(null, "ipv4", null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV4, null),
            arguments(null, "IPv4", null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV4, null),
            arguments(null, "ipv6", null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV6, null),
            arguments(null, "IPv6", null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV6, null),
            arguments(null, "Ipv99", null, null, null, null, IllegalArgumentException.class),

            arguments(null, null, TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv6", null, null,
                      Ec2MetadataConfigProvider.EndpointMode.IPV6, null),
            arguments(null, null, TEST_PROFILES_PATH_PREFIX + "endpoint_mode_invalidValue", null, null, null,
                      IllegalArgumentException.class),

            // System property takes highest precedence
            arguments("ipv4", "ipv6", null, null, null, Ec2MetadataConfigProvider.EndpointMode.IPV6, null),
            arguments(null, "ipv6", TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv4", null, null,
                      Ec2MetadataConfigProvider.EndpointMode.IPV6, null),

            // env var has higher precedence than shared config
            arguments("ipv6", null, TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv4", null, null,
                      Ec2MetadataConfigProvider.EndpointMode.IPV6, null),

            // Test custom profile supplier and custom profile name
            arguments(null, null, TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv6_custom_profile", null, CUSTOM_PROFILE,
                      Ec2MetadataConfigProvider.EndpointMode.IPV6, null),
            arguments(null, null, null, customProfileFile(), CUSTOM_PROFILE, Ec2MetadataConfigProvider.EndpointMode.IPV6, null)
        );
    }

    @BeforeEach
    @AfterEach
    public void cleanup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property());
    }

    @ParameterizedTest
    @MethodSource("testData")
    void resolvesCorrectEndpointMode(String envEndpointMode,
                                     String systemPropertyEndpointMode,
                                     String sharedConfigFile,
                                     ProfileFile customProfileFile,
                                     String customProfileName,
                                     Ec2MetadataConfigProvider.EndpointMode expectedEndpointMode,
                                     Class<? extends Throwable> expectedException) {
        if (envEndpointMode != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.environmentVariable(),
                                            envEndpointMode);
        }

        if (systemPropertyEndpointMode != null) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property(),
                               systemPropertyEndpointMode);
        }
        if (sharedConfigFile != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(ProfileFileSystemSetting.AWS_CONFIG_FILE.environmentVariable(),
                                            getTestFilePath(sharedConfigFile));
        }

        Ec2MetadataConfigProvider configProvider = Ec2MetadataConfigProvider.builder()
                                                                            .profileFile(customProfileFile == null ? null :
                                                                                         () -> customProfileFile)
                                                                            .profileName(customProfileName)
                                                                            .build();

        if (expectedException != null) {
            assertThatThrownBy(configProvider::getEndpointMode).isInstanceOf(expectedException);
        } else {
            assertThat(configProvider.getEndpointMode()).isEqualTo(expectedEndpointMode);
        }
    }

    private static String getTestFilePath(String testFile) {
        return Ec2MetadataConfigProviderEndpointModeTest.class.getResource(testFile).getFile();
    }

    private static ProfileFile customProfileFile() {
        String content = "[profile myprofile]\n" +
                "ec2_metadata_service_endpoint_mode=ipv6\n";

        return ProfileFile.builder()
                .type(ProfileFile.Type.CONFIGURATION)
                .content(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                .build();
    }
}
