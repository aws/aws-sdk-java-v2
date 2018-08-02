/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.regions.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class AwsProfileRegionProviderTest {

    @Rule
    public EnvironmentVariableHelper settingsHelper = new EnvironmentVariableHelper();

    @Test
    public void nonExistentDefaultConfigFile_ThrowsException() {
        settingsHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE, "/var/tmp/this/is/invalid.txt");
        settingsHelper.set(ProfileFileSystemSetting.AWS_SHARED_CREDENTIALS_FILE, "/var/tmp/this/is/also.invalid.txt");
        assertThatThrownBy(() -> new AwsProfileRegionProvider().getRegion())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("No region provided in profile: default");
    }

    @Test
    public void profilePresentAndRegionIsSet_ProvidesCorrectRegion() throws URISyntaxException {
        String testFile = "/profileconfig/test-profiles.tst";

        settingsHelper.set(ProfileFileSystemSetting.AWS_PROFILE, "test");
        settingsHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE, Paths.get(getClass().getResource(testFile).toURI()).toString());
        assertThat(new AwsProfileRegionProvider().getRegion()).isEqualTo(Region.of("saa"));
    }
}
