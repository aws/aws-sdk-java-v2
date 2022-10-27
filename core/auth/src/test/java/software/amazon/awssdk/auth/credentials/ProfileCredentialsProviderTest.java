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
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
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
    void missingCredentialsFileThrowsExceptionInGetCredentials() {
        ProfileCredentialsProvider provider =
            new ProfileCredentialsProvider.BuilderImpl()
                .defaultProfileFileLoader(() -> { throw new IllegalStateException(); })
                .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingProfileFileThrowsExceptionInGetCredentials() {
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
    void missingProfileThrowsExceptionInGetCredentials() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder().profileFile(file).profileName("foo").build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void profileWithoutCredentialsThrowsExceptionInGetCredentials() {
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
    void resolveCredentials_missingCredentialsFileWithCustomExceptionHandler_ThrowsExceptionInGetCredentials() {
        try (ProfileCredentialsProvider provider =
                 new ProfileCredentialsProvider.BuilderImpl()
                     .defaultProfileFileLoader(() -> { throw new IllegalStateException(); })
                     .exceptionHandler(e -> { throw SdkClientException.builder().cause(e).build(); })
                     .build()) {

            assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
        }
    }

    @Test
    void resolveCredentials_missingProfileFileCausesExceptionInConstructor_throwsException() {
        ProfileCredentialsProvider provider =
            new ProfileCredentialsProvider.BuilderImpl()
                .profileFileSupplier(() -> ProfileFile.builder()
                                                      .content(new StringInputStream(""))
                                                      .type(ProfileFile.Type.CONFIGURATION)
                                                      .build())
                .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveCredentials_missingProfileFileCausesExceptionInMethod_throwsException() {
        ProfileCredentialsProvider.BuilderImpl builder = new ProfileCredentialsProvider.BuilderImpl();
        builder.defaultProfileFileLoader(() -> ProfileFile.builder()
                                                          .content(new StringInputStream(""))
                                                          .type(ProfileFile.Type.CONFIGURATION)
                                                          .build());
        builder.defaultProfileFileReloadPredicate(r -> true);
        ProfileCredentialsProvider provider = builder.build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void resolveCredentials_missingProfile_throwsException() {
        ProfileFile file = profileFile("[default]\n"
                                       + "aws_access_key_id = defaultAccessKey\n"
                                       + "aws_secret_access_key = defaultSecretAccessKey");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder()
                                      .profileFileSupplier(() -> file)
                                      .profileFileReloadPredicate(rec -> true)
                                      .profileName("foo")
                                      .build();

        assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
    }

    @Test
    void resolveCredentials_profileWithoutCredentials_throwsException() {
        ProfileFile file = profileFile("[default]");

        ProfileCredentialsProvider provider =
            ProfileCredentialsProvider.builder()
                                      .profileFileSupplier(() -> file)
                                      .profileFileReloadPredicate(rec -> true)
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
                                      .profileFileSupplier(() -> file)
                                      .profileName("default")
                                      .profileFileReloadPredicate(rec -> true)
                                      .build();

        assertThat(provider.resolveCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("defaultAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("defaultSecretAccessKey");
        });
    }

    @Test
    void resolveCredentials_profileModifiedNoRefreshIntervalRequestWithinJitterPeriod_doesNotReloadCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration intervalWithinJitter = Duration.ofMillis(100);
        ProfileCredentialsProvider provider = builderWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(ProfileCredentialsProvider.wasRefreshedBeforeFileModificationTime(credentialsFilePath))
            .profileName("default")
            .build();
        AwsCredentials credentials1 = provider.resolveCredentials();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        clock.tickForward(intervalWithinJitter);
        AwsCredentials credentials2 = provider.resolveCredentials();

        assertThat(credentials2).isSameAs(credentials1);
    }

    @Test
    void resolveCredentials_profileModifiedNoRefreshIntervalRequestOutsideJitterPeriod_doesNotReloadCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration intervalOutsideJitter = Duration.ofMinutes(10);
        ProfileCredentialsProvider provider = builderWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .profileName("default")
            .build();
        AwsCredentials credentials1 = provider.resolveCredentials();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        clock.tickForward(intervalOutsideJitter);
        AwsCredentials credentials2 = provider.resolveCredentials();

        assertThat(credentials2).isSameAs(credentials1);
    }

    @Test
    void resolveCredentials_profileModifiedBeforeRefreshIntervalExpires_doesNotReloadCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(10);
        ProfileCredentialsProvider provider = ProfileCredentialsProvider
            .builder()
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .profileName("default")
            .refreshDuration(refreshInterval)
            .pollingDuration(Duration.ofSeconds(1))
            .build();
        AwsCredentials credentials1 = provider.resolveCredentials();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        clock.tickForward(refreshInterval.dividedBy(2));
        AwsCredentials credentials2 = provider.resolveCredentials();

        assertThat(credentials2).isSameAs(credentials1);
    }

    @Test
    void resolveCredentials_profileModifiedAfterRefreshIntervalExpires_reloadsCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(15);
        ProfileCredentialsProvider provider = builderWithClock(clock)
            .profileFileSupplier(() -> profileFile(credentialsFilePath))
            .profileFileReloadPredicate(record -> record.wasCreatedBeforeFileModified(credentialsFilePath))
            .profileName("default")
            .refreshDuration(refreshInterval)
            .pollingDuration(Duration.ofSeconds(1))
            .build();
        provider.resolveCredentials();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(refreshInterval.plusSeconds(10));

        assertThat(provider.resolveCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("modifiedAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("modifiedSecretAccessKey");
        });
    }

    @Test
    void create_noProfileName_returnsProfileCredentialsProviderToResolveWithDefaults() {
        ProfileCredentialsProvider provider = ProfileCredentialsProvider.create();
        String toString = provider.toString();

        assertThat(toString).satisfies(s -> {
            assertThat(s).contains("profileName=default");
            assertThat(s).contains("profileFileSupplier");
        });
    }

    @Test
    void create_givenProfileName_returnsProfileCredentialsProviderToResolveForGivenName() {
        ProfileCredentialsProvider provider = ProfileCredentialsProvider.create("override");
        String toString = provider.toString();

        assertThat(toString).satisfies(s -> {
            assertThat(s).contains("profileName=override");
            assertThat(s).contains("profileFileSupplier");
        });
    }

    @Test
    void toString_anyProfileCredentialsProvider_returnsStringWithExxpectedParameters() {
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
            assertThat(s).contains("profileFileSupplier");
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

    private ProfileFile profileFile(Path path) {
        return ProfileFile.builder().content(path).type(ProfileFile.Type.CREDENTIALS).build();
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

    private void updateModificationTime(Path path, Instant instant) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(instant));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProfileCredentialsProvider.Builder builderWithClock(Clock clock) {
        ProfileCredentialsProvider.Builder builder = ProfileCredentialsProvider.builder();
        ProfileCredentialsProvider.BuilderImpl builderImpl = (ProfileCredentialsProvider.BuilderImpl) builder;
        builderImpl.clock(clock);

        return builder;
    }

    private static class AdjustableClock extends Clock {
        private Instant time;

        AdjustableClock() {
            this.time = Instant.now();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return time;
        }

        public void tickForward(TemporalAmount amount) {
            time = time.plus(amount);
        }

        public void tickBackward(TemporalAmount amount) {
            time = time.minus(amount);
        }
    }
}
