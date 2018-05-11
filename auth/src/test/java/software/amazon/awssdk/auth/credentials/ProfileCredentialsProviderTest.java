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

package software.amazon.awssdk.auth.credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * Verify functionality of {@link ProfileCredentialsProvider}.
 */
public class ProfileCredentialsProviderTest {
    @Test
    public void missingProfileFileThrowsExceptionInGetCredentials() {
        ProfileCredentialsProvider provider =
                ProfileCredentialsProvider.builder()
                                          .defaultProfileFileLoader(() -> ProfileFile.builder()
                                                                                     .content(new StringInputStream(""))
                                                                                     .type(ProfileFile.Type.CONFIGURATION)
                                                                                     .build())
                                          .build();

        assertThatThrownBy(provider::getCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void missingProfileThrowsExceptionInGetCredentials() {
        ProfileFile file = profileFile("[default]\n"
                                        + "aws_access_key_id = defaultAccessKey\n"
                                        + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
                ProfileCredentialsProvider.builder().profileFile(file).profileName("foo").build();

        assertThatThrownBy(provider::getCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void profileWithoutCredentialsThrowsExceptionInGetCredentials() {
        ProfileFile file = profileFile("[default]");

        ProfileCredentialsProvider provider =
                ProfileCredentialsProvider.builder().profileFile(file).profileName("default").build();

        assertThatThrownBy(provider::getCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void presentProfileReturnsCredentials() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
                ProfileCredentialsProvider.builder().profileFile(file).profileName("default").build();

        assertThat(provider.getCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
        });
    }

    private ProfileFile profileFile(String string) {
        return ProfileFile.builder().content(new StringInputStream(string)).type(ProfileFile.Type.CONFIGURATION).build();
    }
}
