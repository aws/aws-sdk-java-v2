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

package software.amazon.awssdk.regions.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.LogCaptor;
import org.junit.jupiter.api.condition.JRE;

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

    @Test
    @EnabledForJreRange(min = JRE.JAVA_8, max = JRE.JAVA_16)
    public void profilePresentAndRegionIsSet_ProvidesCorrectRegion_withException() throws URISyntaxException {
        // Set up test configuration
        String testFile = "/profileconfig/test-profiles.tst";
        settingsHelper.set(ProfileFileSystemSetting.AWS_PROFILE, "test");
        settingsHelper.set(ProfileFileSystemSetting.AWS_CONFIG_FILE, Paths.get(getClass().getResource(testFile).toURI()).toString());

        SecurityManager originalSecurityManager = System.getSecurityManager();

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            // Set up security manager that blocks access to credentials file
            SecurityManager securityManager = new SecurityManager() {
                @Override
                public void checkPermission(Permission perm) {
                    if (perm instanceof java.io.FilePermission) {
                        String path = perm.getName();
                        if (path.contains(".aws") && path.contains("credentials")) {
                            throw new AccessControlException("Access to AWS credentials denied");
                        }
                    }
                }
            };

            System.setSecurityManager(securityManager);
            // Test the region provider behavior
            assertThat(new AwsProfileRegionProvider().getRegion()).isEqualTo(Region.of("saa"));

            List<LogEvent> logEvents = logCaptor.loggedEvents();
            assertThat(logEvents).hasSize(1);
            assertThat(logEvents.get(0).getMessage().getFormattedMessage())
                .contains("Security restrictions prevented access to profile file: Access to AWS credentials denied");
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }
}
