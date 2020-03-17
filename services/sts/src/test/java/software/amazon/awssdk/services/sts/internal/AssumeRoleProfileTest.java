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

package software.amazon.awssdk.services.sts.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sts.AssumeRoleIntegrationTest;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Verify some basic functionality of {@link StsProfileCredentialsProviderFactory} via the way customers will encounter it:
 * the {@link ProfileFile}. The full functionality is verified via
 * {@link AssumeRoleIntegrationTest#profileCredentialsProviderCanAssumeRoles()}.
 */
public class AssumeRoleProfileTest {
    @Test
    public void createAssumeRoleCredentialsProviderViaProfileSucceeds() {
        String profileContent =
                "[profile source]\n"
                + "aws_access_key_id=defaultAccessKey\n"
                + "aws_secret_access_key=defaultSecretAccessKey\n"
                + "\n"
                + "[profile test]\n"
                + "source_profile=source\n"
                + "role_arn=arn:aws:iam::123456789012:role/testRole";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            assertThat(new ProfileCredentialsUtils(profile, profiles::profile).credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider).isInstanceOf(SdkAutoCloseable.class);
                ((SdkAutoCloseable) credentialsProvider).close();
            });
        });
    }

    @Test
    public void assumeRoleOutOfOrderDefinitionSucceeds() {
        String profileContent =
                "[profile child]\n"
                + "source_profile=parent\n"
                + "role_arn=arn:aws:iam::123456789012:role/testRole\n"
                + "[profile source]\n"
                + "aws_access_key_id=defaultAccessKey\n"
                + "aws_secret_access_key=defaultSecretAccessKey\n"
                + "[profile parent]\n"
                + "source_profile=source\n"
                + "role_arn=arn:aws:iam::123456789012:role/testRole";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        assertThat(profiles.profile("child")).isPresent();
    }
}
