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

package software.amazon.awssdk.auth.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Path;
import org.junit.Test;

/**
 * Validate the functionality of {@link ProfilesFile}.
 */
public class ProfilesFileTest {
    @Test
    public void profilesFileWithValidContentLoadsCorrectly() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.basicProfile().asPath());

        assertThat(profiles.profile("default")).hasValueSatisfying(profile -> {
            assertThat(profile.region()).isNotPresent();
            assertThat(profile.credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.getCredentials()).satisfies(credentials -> {
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                });
            });
        });
    }

    @Test
    public void invalidProfilesFilesThrowExceptions() {
        assertThrowsIllegalStateException(ProfileResourceLoader.profilesWithTwoAccessKeyUnderSameProfile());
        assertThrowsIllegalStateException(ProfileResourceLoader.profileNameWithNoBraces());
        assertThrowsIllegalStateException(ProfileResourceLoader.profileNameWithNoOpeningBraces());
        assertThrowsIllegalStateException(ProfileResourceLoader.profileNameWithNoClosingBraces());
        assertThrowsIllegalStateException(ProfileResourceLoader.profilesWithNoProfileName());
        assertThrowsIllegalStateException(ProfileResourceLoader.profilesWithSameProfileName());
        assertThrowsIllegalStateException(ProfileResourceLoader.profilesContainingOtherConfiguration());
    }

    private void assertThrowsIllegalStateException(ProfileResourceLoader profile) {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> new ProfilesFile(profile.asPath()));
    }
}
