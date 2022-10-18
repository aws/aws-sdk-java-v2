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
import java.util.function.Supplier;
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
    void resolveCredentials_profileModifiedNoRefreshIntervalRequestWithinJitterPeriod_doesNotReloadCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        ProfileFile file = profileFile(credentialsFilePath);

        AdjustableClock clock = new AdjustableClock();
        Duration intervalWithinJitter = Duration.ofMillis(100);
        ProfileCredentialsProvider provider = builderWithClock(clock)
            .profileFile(file)
            .profileName("default")
            .build();
        AwsCredentials credentials1 = provider.resolveCredentials();

        credentialsFilePath = generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        clock.tickForward(intervalWithinJitter);
        AwsCredentials credentials2 = provider.resolveCredentials();

        assertThat(credentials2).isSameAs(credentials1);
    }

    @Test
    void resolveCredentials_profileModifiedNoRefreshIntervalRequestOutsideJitterPeriod_doesNotReloadCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        ProfileFile file = profileFile(credentialsFilePath);

        AdjustableClock clock = new AdjustableClock();
        Duration intervalOutsideJitter = Duration.ofMinutes(10);
        ProfileCredentialsProvider provider = builderWithClock(clock)
            .profileFile(file)
            .profileName("default")
            .build();
        AwsCredentials credentials1 = provider.resolveCredentials();

        credentialsFilePath = generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        clock.tickForward(intervalOutsideJitter);
        AwsCredentials credentials2 = provider.resolveCredentials();

        assertThat(credentials2).isSameAs(credentials1);
    }

    @Test
    void resolveCredentials_profileModifiedBeforeRefreshIntervalExpires_doesNotReloadCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        ProfileFile file = profileFile(credentialsFilePath);

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(10);
        ProfileCredentialsProvider provider = ProfileCredentialsProvider.builder()
                                                                        .profileFile(file)
                                                                        .profileName("default")
                                                                        .refresh(refreshInterval, Duration.ofSeconds(1))
                                                                        .build();
        AwsCredentials credentials1 = provider.resolveCredentials();

        credentialsFilePath = generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        clock.tickForward(refreshInterval.dividedBy(2));
        AwsCredentials credentials2 = provider.resolveCredentials();

        assertThat(credentials2).isSameAs(credentials1);
    }

    @Test
    void resolveCredentials_profileModifiedAfterRefreshIntervalExpires_reloadsCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        ProfileFile file = profileFile(credentialsFilePath);

        AdjustableClock clock = new AdjustableClock();
        Duration refreshInterval = Duration.ofSeconds(15);
        ProfileCredentialsProvider provider = builderWithClock(clock)
            .profileFile(file)
            .profileName("default")
            .refresh(refreshInterval, Duration.ofSeconds(1))
            .build();
        AwsCredentials credentials1 = provider.resolveCredentials();

        credentialsFilePath = generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(refreshInterval.plusSeconds(10));

        assertThat(provider.resolveCredentials()).satisfies(credentials -> {
            assertThat(credentials.accessKeyId()).isEqualTo("modifiedAccessKey");
            assertThat(credentials.secretAccessKey()).isEqualTo("modifiedSecretAccessKey");
        });
    }

    @Test
    void resolveCredentials_errorIsThrownWithinConstructor_rethrowsErrorInResolveCredentials() throws IOException {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        ProfileFile file = profileFile(credentialsFilePath);
        Files.deleteIfExists(credentialsFilePath);

        ProfileCredentialsProvider provider = builderWithConstructorException(() -> { throw new IllegalArgumentException(); })
            .build();

        assertThatThrownBy(() -> provider.resolveCredentials()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void resolveCredentials_errorIsThrownWithinResolveCredentials_throwsException() throws IOException {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        ProfileFile file = profileFile(credentialsFilePath);
        ProfileCredentialsProvider provider = ProfileCredentialsProvider.builder()
                                                                        .profileFile(file)
                                                                        .profileName("default")
                                                                        .build();

        Files.deleteIfExists(credentialsFilePath);

        assertThatThrownBy(() -> provider.resolveCredentials()).isInstanceOf(RuntimeException.class);
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
        return generateTestFile(contents, "creds.txt");
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

    private ProfileCredentialsProvider.Builder builderWithConstructorException(Supplier<ProfileFile> supplier) {
        ProfileCredentialsProvider.Builder builder = ProfileCredentialsProvider.builder();
        ProfileCredentialsProvider.BuilderImpl builderImpl = (ProfileCredentialsProvider.BuilderImpl) builder;
        builderImpl.profileFile((ProfileFile) null);
        builderImpl.defaultProfileFileLoader(supplier);

        return builder;
    }

    private static class AdjustableClock extends Clock {
        private Instant time;

        public AdjustableClock() {
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
