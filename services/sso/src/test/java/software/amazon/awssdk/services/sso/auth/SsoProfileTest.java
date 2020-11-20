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

package software.amazon.awssdk.services.sso.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Validate the completeness of sso profile properties consumed by the {@link ProfileCredentialsUtils}.
 */
public class SsoProfileTest {

    @Test
    public void createSsoCredentialsProvider_SsoAccountIdMissing_throwException() {
        String profileContent = "[profile foo]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_role_name=SampleRole\n" +
                                "sso_start_url=https://d-abc123.awsapps.com/start-beta\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        assertThat(profiles.profile("foo")).hasValueSatisfying(profile -> {
            assertThatThrownBy(() -> new ProfileCredentialsUtils(profile, profiles::profile).credentialsProvider())
                .hasMessageContaining("Profile property 'sso_account_id' was not configured");
        });
    }

    @Test
    public void createSsoCredentialsProvider_SsoRegionMissing_throwException() {
        String profileContent = "[profile foo]\n" +
                                "sso_account_id=012345678901\n" +
                                "sso_role_name=SampleRole\n" +
                                "sso_start_url=https://d-abc123.awsapps.com/start-beta\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        assertThat(profiles.profile("foo")).hasValueSatisfying(profile -> {
            assertThatThrownBy(() -> new ProfileCredentialsUtils(profile, profiles::profile).credentialsProvider())
                .hasMessageContaining("Profile property 'sso_region' was not configured");
        });
    }

    @Test
    public void createSsoCredentialsProvider_SsoRoleNameMissing_throwException() {
        String profileContent = "[profile foo]\n" +
                                "sso_account_id=012345678901\n" +
                                "sso_region=us-east-1\n" +
                                "sso_start_url=https://d-abc123.awsapps.com/start-beta\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        assertThat(profiles.profile("foo")).hasValueSatisfying(profile -> {
            assertThatThrownBy(() -> new ProfileCredentialsUtils(profile, profiles::profile).credentialsProvider())
                .hasMessageContaining("Profile property 'sso_role_name' was not configured");
        });
    }

    @Test
    public void createSsoCredentialsProvider_SsoStartUrlMissing_throwException() {
        String profileContent = "[profile foo]\n" +
                                "sso_account_id=012345678901\n" +
                                "sso_region=us-east-1\n" +
                                "sso_role_name=SampleRole\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        assertThat(profiles.profile("foo")).hasValueSatisfying(profile -> {
            assertThatThrownBy(() -> new ProfileCredentialsUtils(profile, profiles::profile).credentialsProvider())
                .hasMessageContaining("Profile property 'sso_start_url' was not configured");
        });
    }
}
