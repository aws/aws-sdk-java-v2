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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsSessionCredentials;
import software.amazon.awssdk.auth.profile.internal.ProfileProperties;
import software.amazon.awssdk.regions.Region;

/**
 * Validate the functionality of {@link ProfilesFile}.
 */
public class ProfilesFileTest {
    @Test
    public void validProfileFilesDoNotThrowExceptions() {
        new ProfilesFile(ProfileResourceLoader.basicProfile().asPath());
        new ProfilesFile(ProfileResourceLoader.basicProfile2().asPath());
        new ProfilesFile(ProfileResourceLoader.profilesContainingOtherConfiguration().asPath());
        new ProfilesFile(ProfileResourceLoader.profileNameWithSpaces().asPath());
        new ProfilesFile(ProfileResourceLoader.profileWithEmptyAccessKey().asPath());
        new ProfilesFile(ProfileResourceLoader.profileWithEmptySecretKey().asPath());
        new ProfilesFile(ProfileResourceLoader.profileWithRole().asPath());
        new ProfilesFile(ProfileResourceLoader.profilesWithComments().asPath());
    }

    @Test
    public void invalidProfileFilesThrowExceptions() {
        assertThrowsIllegalStateException(ProfileResourceLoader.profilesWithTwoAccessKeyUnderSameProfile());
        assertThrowsIllegalStateException(ProfileResourceLoader.profileNameWithNoBraces());
        assertThrowsIllegalStateException(ProfileResourceLoader.profileNameWithNoOpeningBraces());
        assertThrowsIllegalStateException(ProfileResourceLoader.profileNameWithNoClosingBraces());
        assertThrowsIllegalStateException(ProfileResourceLoader.profilesWithNoProfileName());
        assertThrowsIllegalStateException(ProfileResourceLoader.profilesWithSameProfileName());
    }

    @Test
    public void roleProfileWithMissingSourceThrowsExceptionWhenLoadingCredentials() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.roleProfileMissingSource().asPath());

        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            assertThatThrownBy(profile::credentialsProvider).hasMessageContaining("parent profile does not exist");
        });
    }

    @Test
    public void roleProfileWithParentThatHasNoCredentialsThrowsExceptionWhenLoadingCredentials() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.roleProfileWithSourceHavingNoCredentials().asPath());

        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            assertThatThrownBy(profile::credentialsProvider).hasMessageContaining("parent profile has no credentials configured");
        });
    }

    @Test
    public void profilesFileWithRegionLoadsCorrectly() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.basicProfile().asPath());

        assertThat(profiles.profile("profile-with-region")).hasValueSatisfying(profile -> {
            assertThat(profile.region()).hasValue(Region.of("us-east-1"));
        });
    }

    @Test
    public void profilesFileWithRegionLoadsCorrectlyFromInputStream() throws IOException {
        ProfilesFile profiles = new ProfilesFile(Files.newInputStream(ProfileResourceLoader.basicProfile().asPath()));
        assertThat(profiles.profile("profile-with-region")).isPresent();
    }

    @Test
    public void profilesFileWithStaticCredentialsLoadsCorrectly() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.basicProfile().asPath());

        assertThat(profiles.profile("default")).hasValueSatisfying(profile -> {
            assertThat(profile.name()).isEqualTo("default");
            assertThat(profile.property(ProfileProperties.AWS_ACCESS_KEY_ID)).hasValue("defaultAccessKey");
            assertThat(profile.toString()).contains("default");
            assertThat(profile.region()).isNotPresent();
            assertThat(profile.credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.getCredentials()).satisfies(credentials -> {
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                });
            });
        });
    }

    public void profilesFileWithProfilePrefixLoadsCorrectly() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.basicProfile().asPath());

        assertThat(profiles.profile("profile-with-profile-prefix")).hasValueSatisfying(profile -> {
            assertThat(profile.name()).isEqualTo("profile-with-profile-prefix");
        });
    }

    @Test
    public void profilesFileWithSessionCredentialsLoadsCorrectly() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.basicProfile().asPath());

        assertThat(profiles.profile("profile-with-session-token")).hasValueSatisfying(profile -> {
            assertThat(profile.region()).isNotPresent();
            assertThat(profile.credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider.getCredentials()).satisfies(credentials -> {
                    assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
                    assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
                    assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
                    assertThat(((AwsSessionCredentials) credentials).sessionToken()).isEqualTo("awsSessionToken");
                });
            });
        });
    }

    @Test
    public void profileFileWithAssumeRoleThrowsExceptionWhenRetrievingCredentialsProvider() {
        ProfilesFile profiles = new ProfilesFile(ProfileResourceLoader.basicProfile().asPath());

        assertThat(profiles.profile("profile-with-assume-role")).hasValueSatisfying(profile -> {
            assertThat(profile.region()).isNotPresent();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(profile::credentialsProvider);
        });
    }

    @Test
    public void toStringIncludesLocation() {
        Path profileLocation = ProfileResourceLoader.basicProfile().asPath();
        ProfilesFile profiles = new ProfilesFile(profileLocation);
        assertThat(profiles.toString()).contains(profileLocation.toString());
    }

    private void assertThrowsIllegalStateException(ProfileResourceLoader profile) {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> new ProfilesFile(profile.asPath()));
    }
}
