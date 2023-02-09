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

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Verify functionality of {@link ProfileCredentialsProvider}.
 */
public class ProfileCredentialsProviderTest {

    private static FileSystem jimfs;
    private static Path testDirectory;

    @BeforeAll
    public static void setup() {
        jimfs = Jimfs.newFileSystem();
        testDirectory = jimfs.getPath("test");
    }

    @AfterAll
    public static void tearDown() {
        try {
            jimfs.close();
        } catch (IOException e) {
            // no-op
        }
    }

    @Test
    void missingCredentialsFileThrowsExceptionInResolveCredentials() {
        ProfileCredentialsProvider provider =
            new ProfileCredentialsProvider.BuilderImpl()
                .defaultProfileFileLoader(() -> { throw new IllegalStateException(); })
                .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingProfileFileThrowsExceptionInResolveCredentials() {
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
    void missingProfileThrowsExceptionInResolveCredentials() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder().profileFile(file).profileName("foo").build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void profileWithoutCredentialsThrowsExceptionInResolveCredentials() {
        ProfileFile file = profileFile("[default]");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder().profileFile(file).profileName("default").build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void presentProfileReturnsCredentials() {
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
    void profileWithWebIdentityToken() {
        String token = "/User/home/test";

        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey\n"
                                       + "web_identity_token_file = " + token);

        assertThat(file.profile("default").get().property(ProfileProperty.WEB_IDENTITY_TOKEN_FILE).get()).isEqualTo(token);
    }

    @Test
    void resolveCredentials_missingProfileFileCausesExceptionInMethod_throwsException() {
        ProfileCredentialsProvider.BuilderImpl builder = new ProfileCredentialsProvider.BuilderImpl();
        builder.defaultProfileFileLoader(() -> ProfileFile.builder()
                                                          .content(new StringInputStream(""))
                                                          .type(ProfileFile.Type.CONFIGURATION)
                                                          .build());
        ProfileCredentialsProvider provider = builder.build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void resolveCredentials_missingProfile_throwsException() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        try (ProfileCredentialsProvider provider =
                 ProfileCredentialsProvider.builder()
                                           .profileFile(() -> file)
                                           .profileName("foo")
                                           .build()) {

            assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
        }
    }

    @Test
    void resolveCredentials_profileWithoutCredentials_throwsException() {
        ProfileFile file = profileFile("[default]");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder()
                                      .profileFile(() -> file)
                                      .profileName("default")
                                      .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void resolveCredentials_presentProfile_returnsCredentials() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder()
                                      .profileFile(() -> file)
                                      .profileName("default")
                                      .build();

        assertThat(provider.resolveCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
        });
    }

    @Test
    void resolveCredentials_presentProfileFileSupplier_returnsCredentials() {
        Path path = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder()
                                      .profileFile(ProfileFileSupplier.reloadWhenModified(path, ProfileFile.Type.CREDENTIALS))
                                      .profileName("default")
                                      .build();

        assertThat(provider.resolveCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
        });
    }

    @Test
    void resolveCredentials_presentSupplierProfileFile_returnsCredentials() {
        Supplier<ProfileFile> supplier = () -> profileFile("[default]\naws_access_key_id = defaultAccessKey\n"
                                                           + "aws_secret_access_key = defaultSecretAccessKey\n");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder()
                                      .profileFile(supplier)
                                      .profileName("default")
                                      .build();

        assertThat(provider.resolveCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
        });
    }

    @Test
    void create_noProfileName_returnsProfileCredentialsProviderToResolveWithDefaults() {
        ProfileCredentialsProvider provider = ProfileCredentialsProvider.create();
        String toString = provider.toString();

        assertThat(toString).satisfies(s -> assertThat(s).contains("profileName=default"));
    }

    @Test
    void create_givenProfileName_returnsProfileCredentialsProviderToResolveForGivenName() {
        ProfileCredentialsProvider provider = ProfileCredentialsProvider.create("override");
        String toString = provider.toString();

        assertThat(toString).satisfies(s -> assertThat(s).contains("profileName=override"));
    }

    @Test
    void toString_anyProfileCredentialsProviderAfterResolvingCredentialsFileDoesExists_returnsProfileFile() {
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider.BuilderImpl()
            .defaultProfileFileLoader(() -> profileFile("[default]\naws_access_key_id = not-set\n"
                                                        + "aws_secret_access_key = not-set\n"))
            .build();
        provider.resolveCredentials();
        String toString = provider.toString();

        assertThat(toString).satisfies(s -> {
            assertThat(s).contains("profileName=default");
            assertThat(s).contains("profileFile=");
        });
    }

    @Test
    void toString_anyProfileCredentialsProviderAfterResolvingCredentialsFileDoesNotExist_throwsException() {
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider.BuilderImpl()
            .defaultProfileFileLoader(() -> ProfileFile.builder()
                                                       .content(new StringInputStream(""))
                                                       .type(ProfileFile.Type.CONFIGURATION)
                                                       .build())
            .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void toString_anyProfileCredentialsProviderBeforeResolvingCredentials_doesNotReturnProfileFile() {
        ProfileCredentialsProvider provider =
            new ProfileCredentialsProvider.BuilderImpl()
                .defaultProfileFileLoader(() -> ProfileFile.builder()
                                                           .content(new StringInputStream(""))
                                                           .type(ProfileFile.Type.CONFIGURATION)
                                                           .build())
                .build();

        String toString = provider.toString();

        assertThat(toString).satisfies(s -> {
            assertThat(s).contains("profileName");
            assertThat(s).doesNotContain("profileFile");
        });
    }

    @Test
    void toBuilder_fromCredentialsProvider_returnsBuilderCapableOfProducingSimilarProvider() {
        ProfileCredentialsProvider provider1 = ProfileCredentialsProvider.create("override");
        ProfileCredentialsProvider provider2 = provider1.toBuilder().build();

        String provider1ToString = provider1.toString();
        String provider2ToString = provider2.toString();
        assertThat(provider1ToString).isEqualTo(provider2ToString);
    }

    private ProfileFile profileFile(String string) {
        return ProfileFile.builder().content(new StringInputStream(string)).type(ProfileFile.Type.CONFIGURATION).build();
    }

    private Path generateTestFile(String contents, String filename) {
        try {
            Files.createDirectories(testDirectory);
            return Files.write(testDirectory.resolve(filename), contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path generateTestCredentialsFile(String accessKeyId, String secretAccessKey) {
        String contents = String.format("[default]\naws_access_key_id = %s\naws_secret_access_key = %s\n",
                                        accessKeyId, secretAccessKey);
        return generateTestFile(contents, "credentials.txt");
    }

}
