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

package software.amazon.awssdk.services.s3.internal.endpoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;
import org.apache.hc.core5.http.Chars;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.utils.Validate;

@RunWith(Parameterized.class)
public class UseGlobalEndpointResolverTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @Parameterized.Parameter
    public TestData testData;

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] {
            // Test defaults
            new TestData(null, null, null, null, true),

            // Test precedence
            new TestData("regional", null, null, null, false),
            new TestData("test", "regional", "/s3_regional_config_profile.tst", "regional",
                         true),
            new TestData(null, "regional", "/s3_regional_config_profile.tst", "non-regional",
                         false),
            new TestData(null, null, "/s3_regional_config_profile.tst", "non-regional", false),
            new TestData(null, null, null, "regional", false),

            // Test capitalization standardization
            new TestData("rEgIONal", null, null, null, false),
            new TestData(null, "rEgIONal", null, null, false),
            new TestData(null, null, "/s3_regional_config_profile_mixed_case.tst", null, false),
            new TestData(null, null, null, "rEgIONal", false),

            // Test other value
            new TestData("othervalue", null, null, null, true),
            new TestData(null, "dafsad", null, null, true),
            new TestData(null, null, "/s3_regional_config_profile_non_regional.tst", null, true),
            new TestData(null, null, null, "somehtingelse", true),

            // Test property value is regional, but resolve region is not IAD
            new TestData("regional", null, null, null, Region.US_WEST_2, false),
            new TestData(null, "regional", null, null, Region.US_WEST_2, false),
            new TestData(null, null, "/s3_regional_config_profile.tst", null, Region.US_WEST_2, false),
            new TestData(null, null, null, "regional", Region.US_WEST_2, false),
            });
    }


    @After
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property());
    }

    @Test
    public void differentCombinationOfConfigs_shouldResolveCorrectly() {
        SdkClientConfiguration.Builder configBuilder = SdkClientConfiguration.builder();

        configBuilder.option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, testData.advancedOption);

        if (testData.envVarValue != null) {
            ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.environmentVariable(),
                                            testData.envVarValue);
        }

        if (testData.systemProperty != null) {
            System.setProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property(), testData.systemProperty);
        }

        if (testData.configFile != null) {
            String diskLocationForFile = diskLocationForConfig(testData.configFile);
            Validate.isTrue(Files.isReadable(Paths.get(diskLocationForFile)), diskLocationForFile + " is not readable.");

            ProfileFile file = ProfileFile.builder()
                                          .content(Paths.get(diskLocationForFile))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

            configBuilder.option(SdkClientOption.PROFILE_FILE_SUPPLIER, () -> file)
                         .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint");
        }

        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(configBuilder.build());
        assertThat(resolver.resolve(Region.US_EAST_1)).isEqualTo(testData.expected);
    }

    private String diskLocationForConfig(String configFileName) {
        return getClass().getResource(configFileName).getFile();
    }

    private static class TestData {
        private final String envVarValue;
        private final String systemProperty;
        private final String configFile;
        private final String advancedOption;
        private final Region inputRegion;
        private final boolean expected;

        TestData(String systemProperty, String envVarValue, String configFile, String advancedOption, boolean expected) {
            this(systemProperty, envVarValue, configFile, advancedOption, Region.US_EAST_1, expected);
        }

        TestData(String systemProperty, String envVarValue, String configFile, String advancedOption,
                 Region inputRegion, boolean expected) {
            this.envVarValue = envVarValue;
            this.systemProperty = systemProperty;
            this.configFile = configFile;
            this.advancedOption = advancedOption;
            this.inputRegion = inputRegion;
            this.expected = expected;
        }
    }
}
