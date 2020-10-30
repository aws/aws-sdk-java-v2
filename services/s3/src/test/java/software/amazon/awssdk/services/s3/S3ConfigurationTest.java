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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.profiles.ProfileFileSystemSetting.AWS_CONFIG_FILE;
import static software.amazon.awssdk.services.s3.S3SystemSetting.AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS;
import static software.amazon.awssdk.services.s3.S3SystemSetting.AWS_S3_USE_ARN_REGION;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class S3ConfigurationTest {

    private final EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    @After
    public void clearSystemProperty() {
        System.clearProperty(AWS_S3_USE_ARN_REGION.property());
        System.clearProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property());
        System.clearProperty(AWS_CONFIG_FILE.property());
        helper.reset();
    }

    @Test
    public void createConfiguration_minimal() {
        S3Configuration config = S3Configuration.builder().build();
        assertThat(config.accelerateModeEnabled()).isEqualTo(false);
        assertThat(config.checksumValidationEnabled()).isEqualTo(true);
        assertThat(config.chunkedEncodingEnabled()).isEqualTo(true);
        assertThat(config.dualstackEnabled()).isEqualTo(false);
        assertThat(config.multiRegionEnabled()).isEqualTo(true);
        assertThat(config.pathStyleAccessEnabled()).isEqualTo(false);
        assertThat(config.useArnRegionEnabled()).isEqualTo(false);
    }

    @Test
    public void multiRegionEnabled_enabledInConfigOnly_shouldResolveCorrectly() {
        S3Configuration config = S3Configuration.builder().multiRegionEnabled(true).build();
        assertThat(config.multiRegionEnabled()).isEqualTo(true);
    }

    @Test
    public void multiRegionEnabled_disabledInConfigOnly_shouldResolveCorrectly() {
        S3Configuration config = S3Configuration.builder().multiRegionEnabled(false).build();
        assertThat(config.multiRegionEnabled()).isEqualTo(false);
    }

    @Test
    public void multiRegionEnabled_enabledInConfig_shouldResolveToConfigCorrectly() {
        System.setProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property(), "true");
        String trueProfileConfig = getClass().getResource("internal/settingproviders/ProfileFile_true").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), trueProfileConfig);
        S3Configuration config = S3Configuration.builder().multiRegionEnabled(true).build();
        assertThat(config.multiRegionEnabled()).isEqualTo(true);
    }

    @Test
    public void multiRegionEnabled_disabledInProviders_shouldResolveToFalse() {
        System.setProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property(), "true");
        String profileConfig = getClass().getResource("internal/settingproviders/ProfileFile_true").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), profileConfig);
        S3Configuration config = S3Configuration.builder().build();
        assertThat(config.multiRegionEnabled()).isEqualTo(false);
    }

    @Test
    public void multiRegionEnabled_notDisabledInProviders_shouldResolveToTrue() {
        System.setProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property(), "false");
        String profileConfig = getClass().getResource("internal/settingproviders/ProfileFile_false").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), profileConfig);
        S3Configuration config = S3Configuration.builder().build();
        assertThat(config.multiRegionEnabled()).isEqualTo(true);
    }

}
