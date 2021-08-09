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

import java.nio.file.Paths;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringInputStream;

public class WebIdentityTokenCredentialProviderTest {
    @Test
    public void createAssumeRoleWithWebIdentityTokenCredentialsProviderViaProfileSucceeds() {
        String webIdentityTokenPath = Paths.get("/src/test/token.jwt").toAbsolutePath().toString();
        String profileContent =
                "[profile test]\n"
                + "web_identity_token_file="+ webIdentityTokenPath +"\n"
                + "role_arn=arn:aws:iam::123456789012:role/testRole";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            assertThat(new ProfileCredentialsUtils(profile, profiles::profile).credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider).isInstanceOf(SdkAutoCloseable.class);
                assertThat(credentialsProvider).hasFieldOrProperty("stsClient");
                ((SdkAutoCloseable) credentialsProvider).close();
            });
        });
    }
}
