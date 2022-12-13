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

package software.amazon.awssdk.auth.token.credentials;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.StringInputStream;

class ProfileTokenProviderTest {

    @Test
    void missingProfileFile_throwsException() {
        ProfileTokenProvider provider =
            new ProfileTokenProvider.BuilderImpl()
                .defaultProfileFileLoader(() -> ProfileFile.builder()
                                                           .content(new StringInputStream(""))
                                                           .type(ProfileFile.Type.CONFIGURATION)
                                                           .build())
                .build();

        assertThatThrownBy(provider::resolveToken).isInstanceOf(SdkClientException.class);
    }

    @Test
    void emptyProfileFile_throwsException() {
        ProfileTokenProvider provider =
            new ProfileTokenProvider.BuilderImpl()
                .defaultProfileFileLoader(() -> ProfileFile.builder()
                                                           .content(new StringInputStream(""))
                                                           .type(ProfileFile.Type.CONFIGURATION)
                                                           .build())
                .build();

        assertThatThrownBy(provider::resolveToken).isInstanceOf(SdkClientException.class);
    }

    @Test
    void missingProfile_throwsException() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileTokenProvider provider =
            ProfileTokenProvider.builder().profileFile(() -> file).profileName("sso").build();

        assertThatThrownBy(provider::resolveToken).isInstanceOf(SdkClientException.class);
    }

    @Test
    void compatibleProfileSettings_callsLoader() {
        ProfileFile file = profileFile("[default]");

        ProfileTokenProvider provider =
            ProfileTokenProvider.builder().profileFile(() -> file).profileName("default").build();

        assertThatThrownBy(provider::resolveToken).hasMessageContaining("does not have sso_session property");
    }

    @Test
    void resolveToken_profileFileSupplier_suppliesObjectPerCall() {
        ProfileFile file1 = profileFile("[profile sso]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey\n"
                                       + "sso_session = xyz");
        ProfileFile file2 = profileFile("[profile sso]\n"
                                       + "aws_access_key_id = modifiedAccessKey\n"
                                       + "aws_secret_access_key = modifiedSecretAccessKey\n"
                                       + "sso_session = xyz");
        Supplier<ProfileFile> supplier = Mockito.mock(Supplier.class);

        ProfileTokenProvider provider =
            ProfileTokenProvider.builder().profileFile(supplier).profileName("sso").build();

        Mockito.when(supplier.get()).thenReturn(file1, file2);
        assertThatThrownBy(provider::resolveToken).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(provider::resolveToken).isInstanceOf(IllegalArgumentException.class);

        Mockito.verify(supplier, Mockito.times(2)).get();
    }

    private ProfileFile profileFile(String string) {
        return ProfileFile.builder()
                          .content(new StringInputStream(string))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }
}
