/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.auth.profile.ProfileResourceLoader;
import software.amazon.awssdk.regions.Region;

public class AwsProfileRegionProviderTest {
    private String initialDefaultProfile;
    private String initialProfileLocation;

    @Before
    public void setup() {
        this.initialDefaultProfile = AwsSystemSetting.AWS_DEFAULT_PROFILE.getStringValue().orElse(null);
        this.initialProfileLocation = AwsSystemSetting.AWS_CONFIG_FILE.getStringValue().orElse(null);
    }

    @After
    public void teardown() {
        if (initialDefaultProfile == null) {
            System.clearProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property());
        } else {
            System.setProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property(), initialDefaultProfile);
        }

        if (initialProfileLocation == null) {
            System.clearProperty(AwsSystemSetting.AWS_CONFIG_FILE.property());
        } else {
            System.setProperty(AwsSystemSetting.AWS_CONFIG_FILE.property(), initialProfileLocation);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void nonExistentDefaultConfigFile_ThrowsIllegalStateException() {
        System.setProperty(AwsSystemSetting.AWS_CONFIG_FILE.property(), "/var/tmp/this/is/invalid.txt");
        new AwsProfileRegionProvider().getRegion();
    }

    @Test
    public void profilePresentAndRegionIsSet_ProvidesCorrectRegion() {
        System.setProperty(AwsSystemSetting.AWS_DEFAULT_PROFILE.property(), "test");
        System.setProperty(AwsSystemSetting.AWS_CONFIG_FILE.property(),
                           ProfileResourceLoader.profilesContainingOtherConfiguration().asPath().toString());
        assertThat(new AwsProfileRegionProvider().getRegion()).isEqualTo(Region.of("saa"));
    }
}
