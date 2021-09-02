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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

@RunWith(Parameterized.class)
public class Ec2MetadataConfigProviderEndpointOverrideTest {
    private static final String TEST_PROFILES_PATH_PREFIX = "/software/amazon/awssdk/auth/credentials/internal/ec2metadataconfigprovider/";
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @Parameterized.Parameter
    public TestCase testCase;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object> testCases() {
        return Arrays.<Object>asList(
                new TestCase().expectedEndpointOverride(null),

                new TestCase().envEndpointOverride("my-custom-imds").expectedEndpointOverride("my-custom-imds"),

                new TestCase().systemPropertyEndpointOverride("my-custom-imds").expectedEndpointOverride("my-custom-imds"),

                new TestCase().sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_override")
                        .expectedEndpointOverride("my-custom-imds-profile"),

                // System property takes highest precedence
                new TestCase().systemPropertyEndpointOverride("my-systemprop-endpoint").envEndpointOverride("my-env-endpoint")
                        .expectedEndpointOverride("my-systemprop-endpoint"),
                new TestCase().systemPropertyEndpointOverride("my-systemprop-endpoint").sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_override")
                        .expectedEndpointOverride("my-systemprop-endpoint"),
//
                // env var has higher precedence than shared config
                new TestCase().envEndpointOverride("my-env-endpoint").sharedConfigFile(TEST_PROFILES_PATH_PREFIX + "endpoint_override")
                        .expectedEndpointOverride("my-env-endpoint")

        );
    }

    @Before
    public void setup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());

        if (testCase.envEndpointOverride != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.environmentVariable(),
                    testCase.envEndpointOverride);
        }

        if (testCase.systemPropertyEndpointOverride != null) {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                    testCase.systemPropertyEndpointOverride);
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
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    @Test
    public void resolvesCorrectEndpointOverride() {
        String endpointOverride = Ec2MetadataConfigProvider.builder().build().getEndpointOverride();

        assertThat(endpointOverride).isEqualTo(testCase.expectedEndpointOverride);
    }

    private static String getTestFilePath(String testFile) {
        return Ec2MetadataConfigProviderEndpointOverrideTest.class.getResource(testFile).getFile();
    }

    private static class TestCase {
        private String envEndpointOverride;
        private String systemPropertyEndpointOverride;

        private String sharedConfigFile;

        private String expectedEndpointOverride;
        private Class<? extends Throwable> expectedException;

        public TestCase envEndpointOverride(String envEndpointOverride) {
            this.envEndpointOverride = envEndpointOverride;
            return this;
        }
        public TestCase systemPropertyEndpointOverride(String systemPropertyEndpointOverride) {
            this.systemPropertyEndpointOverride = systemPropertyEndpointOverride;
            return this;
        }

        public TestCase sharedConfigFile(String sharedConfigFile) {
            this.sharedConfigFile = sharedConfigFile;
            return this;
        }

        public TestCase expectedEndpointOverride(String expectedEndpointOverride) {
            this.expectedEndpointOverride = expectedEndpointOverride;
            return this;
        }

        public TestCase expectedException(Class<? extends Throwable> expectedException) {
            this.expectedException = expectedException;
            return this;
        }

        @Override
        public String toString() {
            return "TestCase{" +
                    "envEndpointOverride='" + envEndpointOverride + '\'' +
                    ", systemPropertyEndpointOverride='" + systemPropertyEndpointOverride + '\'' +
                    ", sharedConfigFile='" + sharedConfigFile + '\'' +
                    ", expectedEndpointOverride='" + expectedEndpointOverride + '\'' +
                    ", expectedException=" + expectedException +
                    '}';
        }
    }
}
