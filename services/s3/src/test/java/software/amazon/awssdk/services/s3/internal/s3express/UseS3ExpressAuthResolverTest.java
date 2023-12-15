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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.s3.internal.s3express.UseS3ExpressAuthResolver;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Validate;

@RunWith(Parameterized.class)
public class UseS3ExpressAuthResolverTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @Parameterized.Parameter
    public TestData testData;

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] {
            // Test defaults
            new TestData(null, null, null, true),

            // Test different settings
            new TestData("true", null, null, false),
            new TestData("false", null, null, true),
            new TestData(null, "true", null, false),
            new TestData(null, "false", null, true),
            new TestData(null, null, "/s3_express_profile_true.tst", false),
            new TestData(null, null, "/s3_express_profile_false.tst", true),

            // Test precedence
            new TestData("true", "false", null, false),
            new TestData("false", "true", null, true),
            new TestData("false", null, "/s3_express_profile_true.tst", true),
            new TestData(null, "false", "/s3_express_profile_true.tst", true),
            new TestData("true", null, "/s3_express_profile_false.tst", false),
            new TestData(null, "true", "/s3_express_profile_false.tst", false),
            });
    }

    @After
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.property());
    }

    @Test
    public void differentCombinationOfConfigs_shouldResolveCorrectly() {
        SdkClientConfiguration.Builder configBuilder = SdkClientConfiguration.builder();

        if (testData.envVarValue != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.environmentVariable(),
                                            testData.envVarValue);
        }

        if (testData.systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.property(), testData.systemProperty);
        }

        if (testData.configFile != null) {
            String diskLocationForFile = diskLocationForConfig(testData.configFile);
            Validate.isTrue(Files.isReadable(Paths.get(diskLocationForFile)), diskLocationForFile + " is not readable.");

            ProfileFile file = ProfileFile.builder()
                                          .content(Paths.get(diskLocationForFile))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

            configBuilder.option(SdkClientOption.PROFILE_FILE, file)
                         .option(SdkClientOption.PROFILE_NAME, "s3Express_auth");
        }

        UseS3ExpressAuthResolver resolver = new UseS3ExpressAuthResolver(configBuilder.build());
        assertThat(resolver.resolve()).isEqualTo(testData.expected);
    }

    private String diskLocationForConfig(String configFileName) {
        return getClass().getResource(configFileName).getFile();
    }

    private static class TestData {
        private final String envVarValue;
        private final String systemProperty;
        private final String configFile;
        private final boolean expected;

        TestData(String systemProperty, String envVarValue, String configFile, boolean expected) {
            this.envVarValue = envVarValue;
            this.systemProperty = systemProperty;
            this.configFile = configFile;
            this.expected = expected;
        }
    }
}
