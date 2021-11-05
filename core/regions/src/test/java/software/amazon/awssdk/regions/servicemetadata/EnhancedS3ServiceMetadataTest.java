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

package software.amazon.awssdk.regions.servicemetadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Validate;

@RunWith(Parameterized.class)
public class EnhancedS3ServiceMetadataTest {
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();
    private static final URI S3_GLOBAL_ENDPOINT = URI.create("s3.amazonaws.com");
    private static final URI S3_IAD_REGIONAL_ENDPOINT = URI.create("s3.us-east-1.amazonaws.com");

    private ServiceMetadata enhancedMetadata = new EnhancedS3ServiceMetadata();

    @Parameterized.Parameter
    public TestData testData;

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] {
            // Test defaults
            new TestData(null, null, null, null, S3_GLOBAL_ENDPOINT),

            // Test precedence
            new TestData("regional", null, null, null, S3_IAD_REGIONAL_ENDPOINT),
            new TestData("test", "regional", "/profileconfig/s3_regional_config_profile.tst", "regional",
                         S3_GLOBAL_ENDPOINT),
            new TestData(null, "regional", "/profileconfig/s3_regional_config_profile.tst", "non-regional",
                         S3_IAD_REGIONAL_ENDPOINT),
            new TestData(null, null, "/profileconfig/s3_regional_config_profile.tst", "non-regional", S3_IAD_REGIONAL_ENDPOINT),
            new TestData(null, null, null, "regional", S3_IAD_REGIONAL_ENDPOINT),

            // Test capitalization standardization
            new TestData("rEgIONal", null, null, null, S3_IAD_REGIONAL_ENDPOINT),
            new TestData(null, "rEgIONal", null, null, S3_IAD_REGIONAL_ENDPOINT),
            new TestData(null, null, "/profileconfig/s3_regional_config_profile_mixed_case.tst", null, S3_IAD_REGIONAL_ENDPOINT),
            new TestData(null, null, null, "rEgIONal", S3_IAD_REGIONAL_ENDPOINT),

            // Test other value
            new TestData("othervalue", null, null, null, S3_GLOBAL_ENDPOINT),
            new TestData(null, "dafsad", null, null, S3_GLOBAL_ENDPOINT),
            new TestData(null, null, "/profileconfig/s3_regional_config_profile_non_regional.tst", null, S3_GLOBAL_ENDPOINT),
            new TestData(null, null, null, "somehtingelse", S3_GLOBAL_ENDPOINT),
            });
    }


    @After
    public void methodSetup() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property());
    }

    @Test
    public void differentCombinationOfConfigs_shouldResolveCorrectly() {
        enhancedMetadata =
            new EnhancedS3ServiceMetadata().reconfigure(c -> c.putAdvancedOption(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT,
                                                                                 testData.advancedOption));
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

            enhancedMetadata = enhancedMetadata.reconfigure(c -> c.profileFile(() -> file)
                                                                  .profileName("regional_s3_endpoint")
                                                                  .putAdvancedOption(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT,
                                                                                     testData.advancedOption));
        }

        URI result = enhancedMetadata.endpointFor(Region.US_EAST_1);
        assertThat(result).isEqualTo(testData.expected);
    }

    private String diskLocationForConfig(String configFileName) {
        return getClass().getResource(configFileName).getFile();
    }

    private static class TestData {
        private final String envVarValue;
        private final String systemProperty;
        private final String configFile;
        private final String advancedOption;
        private final URI expected;

        TestData(String systemProperty, String envVarValue, String configFile, String advancedOption, URI expected) {
            this.envVarValue = envVarValue;
            this.systemProperty = systemProperty;
            this.configFile = configFile;
            this.advancedOption = advancedOption;
            this.expected = expected;
        }
    }
}
