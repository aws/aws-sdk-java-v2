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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.utils.StringInputStream;

class DefaultCredentialsProviderTest {

    @Test
    void resolveCredentials_ProfileCredentialsProviderWithProfileFile_returnsCredentials() {
        DefaultCredentialsProvider provider = DefaultCredentialsProvider
            .builder()
            .profileFile(credentialFile("test", "access", "secret"))
            .profileName("test")
            .build();

        assertThat(provider.resolveCredentials()).satisfies(awsCredentials -> {
            assertThat(awsCredentials.accessKeyId()).isEqualTo("access");
            assertThat(awsCredentials.secretAccessKey()).isEqualTo("secret");
        });
    }

    @Test
    void resolveCredentials_ProfileCredentialsProviderWithProfileFileSupplier_resolvesCredentialsPerCall() {
        List<ProfileFile> profileFileList = Arrays.asList(credentialFile("test", "access", "secret"),
                                                          credentialFile("test", "modified", "update"));
        ProfileFileSupplier profileFileSupplier = supply(profileFileList);

        DefaultCredentialsProvider provider = DefaultCredentialsProvider
            .builder()
            .profileFile(profileFileSupplier)
            .profileName("test")
            .build();

        assertThat(provider.resolveCredentials()).satisfies(awsCredentials -> {
            assertThat(awsCredentials.accessKeyId()).isEqualTo("access");
            assertThat(awsCredentials.secretAccessKey()).isEqualTo("secret");
        });

        assertThat(provider.resolveCredentials()).satisfies(awsCredentials -> {
            assertThat(awsCredentials.accessKeyId()).isEqualTo("modified");
            assertThat(awsCredentials.secretAccessKey()).isEqualTo("update");
        });
    }

    @Test
    void resolveCredentials_ProfileCredentialsProviderWithProfileFileSupplier_returnsCredentials() {
        ProfileFile profileFile = credentialFile("test", "access", "secret");
        ProfileFileSupplier profileFileSupplier = ProfileFileSupplier.fixedProfileFile(profileFile);

        DefaultCredentialsProvider provider = DefaultCredentialsProvider
            .builder()
            .profileFile(profileFileSupplier)
            .profileName("test")
            .build();

        assertThat(provider.resolveCredentials()).satisfies(awsCredentials -> {
            assertThat(awsCredentials.accessKeyId()).isEqualTo("access");
            assertThat(awsCredentials.secretAccessKey()).isEqualTo("secret");
        });
    }

    @Test
    void resolveCredentials_ProfileCredentialsProviderWithSupplierProfileFile_returnsCredentials() {
        Supplier<ProfileFile> supplier = () -> credentialFile("test", "access", "secret");

        DefaultCredentialsProvider provider = DefaultCredentialsProvider
            .builder()
            .profileFile(supplier)
            .profileName("test")
            .build();

        assertThat(provider.resolveCredentials()).satisfies(awsCredentials -> {
            assertThat(awsCredentials.accessKeyId()).isEqualTo("access");
            assertThat(awsCredentials.secretAccessKey()).isEqualTo("secret");
        });
    }

    private ProfileFile credentialFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CREDENTIALS)
                          .build();
    }

    private ProfileFile credentialFile(String name, String accessKeyId, String secretAccessKey) {
        String contents = String.format("[%s]\naws_access_key_id = %s\naws_secret_access_key = %s\n",
                                        name, accessKeyId, secretAccessKey);
        return credentialFile(contents);
    }

    private static ProfileFileSupplier supply(Iterable<ProfileFile> iterable) {
        return iterable.iterator()::next;
    }

}
