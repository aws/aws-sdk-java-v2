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

package software.amazon.awssdk.services.sts.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import software.amazon.awssdk.auth.profile.ProfilesFile;
import software.amazon.awssdk.services.sts.AssumeRoleIntegrationTest;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Verify some basic functionality of {@link StsProfileCredentialsProviderFactory} via the way customers will encounter it:
 * the {@link ProfilesFile}. The full functionality is verified via
 * {@link AssumeRoleIntegrationTest#profileCredentialsProviderCanAssumeRoles()}.
 */
public class AssumeRoleProfileTest {
    private static final String ASSUME_ROLE_PROFILE =
            "[source]\n"
            + "aws_access_key_id=defaultAccessKey\n"
            + "aws_secret_access_key=defaultSecretAccessKey\n"
            + "\n"
            + "[test]\n"
            + "region=us-east-1\n"
            + "source_profile=source\n"
            + "role_arn=arn:aws:iam::123456789012:role/testRole";

    @Test
    public void createAssumeRoleCredentialsProviderViaProfileSucceeds() {
        ProfilesFile profiles = new ProfilesFile(new ByteArrayInputStream(ASSUME_ROLE_PROFILE.getBytes(StandardCharsets.UTF_8)));
        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            assertThat(profile.credentialsProvider()).hasValueSatisfying(credentialsProvider -> {
                assertThat(credentialsProvider).isInstanceOf(SdkAutoCloseable.class);
                ((SdkAutoCloseable) credentialsProvider).close();
            });
        });
    }
}
