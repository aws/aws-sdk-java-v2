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
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

@RunWith(Parameterized.class)
public class Ec2MetadataConfigProviderEndpointModeTest {
    private static final String TEST_PROFILES_PATH_PREFIX = "/software/amazon/awssdk/auth/credentials/internal/ec2metadataconfigprovider/";
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    private static final String CUSTOM_PROFILE = "myprofile";

    @Parameterized.Parameter
    public TestCase testCase;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object> testCases() {
        return Arrays.asList(
                new TestCase().expectedEndpointMode(null).expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV4),

                new TestCase().envEndpointMode("ipv4").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV4),
                new TestCase().envEndpointMode("IPv4").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV4),
                new TestCase().envEndpointMode("ipv6").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),
                new TestCase().envEndpointMode("IPv6").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),
                new TestCase().envEndpointMode("Ipv99").expectedException(IllegalArgumentException.class),

                new TestCase().systemPropertyEndpointMode("ipv4").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV4),
                new TestCase().systemPropertyEndpointMode("IPv4").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV4),
                new TestCase().systemPropertyEndpointMode("ipv6").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),
                new TestCase().systemPropertyEndpointMode("IPv6").expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),
                new TestCase().systemPropertyEndpointMode("Ipv99").expectedException(IllegalArgumentException.class),

                new TestCase().sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv6")
                        .expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),
                new TestCase().sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_mode_invalidValue")
                        .expectedException(IllegalArgumentException.class),

                // System property takes highest precedence
                new TestCase().systemPropertyEndpointMode("ipv6").envEndpointMode("ipv4")
                        .expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),
                new TestCase().systemPropertyEndpointMode("ipv6").sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv4")
                        .expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),

                // env var has higher precedence than shared config
                new TestCase().envEndpointMode("ipv6").sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv4")
                        .expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),

                // Test custom profile supplier and custom profile name
                new TestCase().sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_mode_ipv6_custom_profile")
                        .customProfileName(CUSTOM_PROFILE).expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6),
                new TestCase().customProfileFile(Ec2MetadataConfigProviderEndpointModeTest::customProfileFile)
                        .customProfileName(CUSTOM_PROFILE).expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode.IPV6)
        );
    }

    @Before
    public void setup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property());

        if (testCase.envEndpointMode != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.environmentVariable(),
                    testCase.envEndpointMode);
        }

        if (testCase.systemPropertyEndpointMode != null) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property(),
                    testCase.systemPropertyEndpointMode);
        }
        if (testCase.sharedConfigFile != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(ProfileFileSystemSetting.AWS_CONFIG_FILE.environmentVariable(),
                    getTestFilePath(testCase.sharedConfigFile));
        }

        if (testCase.expectedException != null) {
            thrown.expect(testCase.expectedException);
        }
    }

    @After
    public void teardown() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT_MODE.property());
    }

    @Test
    public void resolvesCorrectEndpointMode() {
        Ec2MetadataConfigProvider configProvider = Ec2MetadataConfigProvider.builder()
                .profileFile(testCase.customProfileFile)
                .profileName(testCase.customProfileName)
                .build();

        assertThat(configProvider.getEndpointMode()).isEqualTo(testCase.expectedEndpointMode);
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

    private static class TestCase {
        private String envEndpointMode;
        private String systemPropertyEndpointMode;

        private String sharedConfigFile;

        private Supplier<ProfileFile> customProfileFile;

        private String customProfileName;

        private Ec2MetadataConfigProvider.EndpointMode expectedEndpointMode;
        private Class<? extends Throwable> expectedException;

        public TestCase envEndpointMode(String envEndpointMode) {
            this.envEndpointMode = envEndpointMode;
            return this;
        }
        public TestCase systemPropertyEndpointMode(String systemPropertyEndpointMode) {
            this.systemPropertyEndpointMode = systemPropertyEndpointMode;
            return this;
        }

        public TestCase sharedConfigFile(String sharedConfigFile) {
            this.sharedConfigFile = sharedConfigFile;
            return this;
        }

        public TestCase customProfileFile(Supplier<ProfileFile> customProfileFile) {
            this.customProfileFile = customProfileFile;
            return this;
        }

        private TestCase customProfileName(String customProfileName) {
            this.customProfileName = customProfileName;
            return this;
        }

        public TestCase expectedEndpointMode(Ec2MetadataConfigProvider.EndpointMode expectedEndpointMode) {
            this.expectedEndpointMode = expectedEndpointMode;
            return this;
        }

        public TestCase expectedException(Class<? extends Throwable> expectedException) {
            this.expectedException = expectedException;
            return this;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "envEndpointMode='" + envEndpointMode + '\'' +
                    ", systemPropertyEndpointMode='" + systemPropertyEndpointMode + '\'' +
                    ", sharedConfigFile='" + sharedConfigFile + '\'' +
                    ", customProfileFile=" + customProfileFile +
                    ", customProfileName='" + customProfileName + '\'' +
                    ", expectedEndpointMode=" + expectedEndpointMode +
                    ", expectedException=" + expectedException +
                    '}';
        }
    }
}
