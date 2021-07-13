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

package software.amazon.awssdk.auth.credentials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Verify functionality of {@link ProfileCredentialsProvider}.
 */
public class ProfileCredentialsProviderTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Test
    public void missingCredentialsFileThrowsExceptionInGetCredentials() {
        ProfileCredentialsProvider provider =
                new ProfileCredentialsProvider.BuilderImpl()
                        .defaultProfileFileLoader(() -> { throw new IllegalStateException(); })
                        .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void missingProfileFileThrowsExceptionInGetCredentials() {
        ProfileCredentialsProvider provider =
            new ProfileCredentialsProvider.BuilderImpl()
                .defaultProfileFileLoader(() -> ProfileFile.builder()
                                                           .content(new StringInputStream(""))
                                                           .type(ProfileFile.Type.CONFIGURATION)
                                                           .build())
                .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void missingProfileThrowsExceptionInGetCredentials() {
        ProfileFile file = profileFile("[default]\n"
                                        + "aws_access_key_id = defaultAccessKey\n"
                                        + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
                ProfileCredentialsProvider.builder().profileFile(file).profileName("foo").build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void profileWithoutCredentialsThrowsExceptionInGetCredentials() {
        ProfileFile file = profileFile("[default]");

        ProfileCredentialsProvider provider =
                ProfileCredentialsProvider.builder().profileFile(file).profileName("default").build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    public void presentProfileReturnsCredentials() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
                ProfileCredentialsProvider.builder().profileFile(file).profileName("default").build();

        assertThat(provider.resolveCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
        });
    }

    @Test
    public void profileWithWebIdentityToken() {
        String token = "/User/home/test";

        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey\n"
                                       + "web_identity_token_file = " + token);

        assertThat(file.profile("default").get().property(ProfileProperty.WEB_IDENTITY_TOKEN_FILE).get()).isEqualTo(token);
    }

    @Test
    public void profileReloaded() throws IOException, InterruptedException {
        File file = tempDir.newFile();
        Files.write(file.toPath(), ("[foo]\n"
                                    + "aws_access_key_id = key_1\n"
                                    + "aws_secret_access_key = secret_1").getBytes());
        ProfileFile profileFile = ProfileFile.builder().content(file.toPath()).type(ProfileFile.Type.CREDENTIALS).build();

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder().profileFile(profileFile).profileName("foo").build();

        assertThat(provider.resolveCredentials()).isEqualTo(AwsBasicCredentials.create("key_1", "secret_1"));

        Files.write(file.toPath(), ("[foo]\n"
                                    + "aws_access_key_id = key_2\n"
                                    + "aws_secret_access_key = secret_2").getBytes());
        // Manually bump the last modified version because file writing is not enough
        // if it happens within a very small amount of time on some platforms
        file.setLastModified(file.lastModified() + 60_000);

        assertThat(provider.resolveCredentials()).isEqualTo(AwsBasicCredentials.create("key_2", "secret_2"));
    }

    private ProfileFile profileFile(String string) {
        return ProfileFile.builder().content(new StringInputStream(string)).type(ProfileFile.Type.CONFIGURATION).build();
    }
}
