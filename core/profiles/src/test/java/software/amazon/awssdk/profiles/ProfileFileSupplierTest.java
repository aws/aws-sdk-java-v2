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

package software.amazon.awssdk.profiles;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.profiles.ProfileFileSupplierBuilder;

class ProfileFileSupplierTest {

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
    void get_profileFileFixed_doesNotReloadProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileFileSupplier supplier = builder()
            .fixedProfileFile(credentialsFilePath)
            .build();

        ProfileFile file1 = supplier.get();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        ProfileFile file2 = supplier.get();

        assertThat(file2).isSameAs(file1);
    }

    @Test
    void get_profileModifiedWithinJitterPeriod_doesNotReloadCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        Duration durationWithinJitter = Duration.ofMillis(10);
        ProfileFileSupplier supplier = builderWithClock(clock)
            .reloadWhenModified(credentialsFilePath)
            .build();

        ProfileFile file1 = supplier.get();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plus(durationWithinJitter));

        clock.tickForward(durationWithinJitter);
        ProfileFile file2 = supplier.get();

        assertThat(file2).isSameAs(file1);
    }

    @Test
    void get_profileModifiedOutsideJitterPeriod_reloadsCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();

        ProfileFileSupplier supplier = builderWithClock(clock)
            .reloadWhenModified(credentialsFilePath)
            .build();

        Duration durationOutsideJitter = Duration.ofSeconds(1);

        supplier.get();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plus(durationOutsideJitter));

        clock.tickForward(durationOutsideJitter);

        Optional<Profile> fileOptional = supplier.get().profile("default");
        assertThat(fileOptional).isPresent();

        assertThat(fileOptional.get()).satisfies(profile -> {
            Optional<String> awsAccessKeyIdOptional = profile.property("aws_access_key_id");
            assertThat(awsAccessKeyIdOptional).isPresent();
            String awsAccessKeyId = awsAccessKeyIdOptional.get();
            assertThat(awsAccessKeyId).isEqualTo("modifiedAccessKey");

            Optional<String> awsSecretAccessKeyOptional = profile.property("aws_secret_access_key");
            assertThat(awsSecretAccessKeyOptional).isPresent();
            String awsSecretAccessKey = awsSecretAccessKeyOptional.get();
            assertThat(awsSecretAccessKey).isEqualTo("modifiedSecretAccessKey");
        });
    }

    @Test
    void get_profileModified_reloadsProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = builderWithClock(clock)
            .reloadWhenModified(credentialsFilePath)
            .build();

        Duration duration = Duration.ofSeconds(10);
        ProfileFile file1 = supplier.get();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file2 = supplier.get();

        assertThat(file2).isNotSameAs(file1);
    }

    @Test
    void get_profileModifiedOnceButRefreshedMultipleTimes_reloadsProfileFileOnce() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = builderWithClock(clock)
            .reloadWhenModified(credentialsFilePath)
            .build();
        ProfileFile file1 = supplier.get();

        clock.tickForward(Duration.ofSeconds(5));
        ProfileFile file2 = supplier.get();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(Duration.ofSeconds(5));
        ProfileFile file3 = supplier.get();

        assertThat(file2).isSameAs(file1);
        assertThat(file3).isNotSameAs(file2);
    }

    @Test
    void get_profileModifiedMultipleTimes_reloadsProfileFileOncePerChange() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = builderWithClock(clock)
            .reloadWhenModified(credentialsFilePath)
            .build();
        Duration duration = Duration.ofSeconds(5);

        ProfileFile file1 = supplier.get();

        clock.tickForward(duration);
        ProfileFile file2 = supplier.get();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file3 = supplier.get();

        generateTestCredentialsFile("updatedAccessKey", "updatedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file4 = supplier.get();

        clock.tickForward(duration);
        ProfileFile file5 = supplier.get();

        assertThat(file2).isSameAs(file1);
        assertThat(file3).isNotSameAs(file2);
        assertThat(file4).isNotSameAs(file3);
        assertThat(file5).isSameAs(file4);
    }

    @Test
    void get_supplierBuiltByReloadWhenModified_loadsProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileFileSupplier supplier = ProfileFileSupplier.reloadWhenModified(credentialsFilePath);
        ProfileFile file = supplier.get();

        Optional<Profile> profileOptional = file.profile("default");
        assertThat(profileOptional).isPresent();

        assertThat(profileOptional.get()).satisfies(profile -> {
            Optional<String> awsAccessKeyIdOptional = profile.property("aws_access_key_id");
            assertThat(awsAccessKeyIdOptional).isPresent();
            String awsAccessKeyId = awsAccessKeyIdOptional.get();
            assertThat(awsAccessKeyId).isEqualTo("defaultAccessKey");

            Optional<String> awsSecretAccessKeyOptional = profile.property("aws_secret_access_key");
            assertThat(awsSecretAccessKeyOptional).isPresent();
            String awsSecretAccessKey = awsSecretAccessKeyOptional.get();
            assertThat(awsSecretAccessKey).isEqualTo("defaultSecretAccessKey");
        });
    }

    @Test
    void get_supplierBuiltByFixedProfileFilePath_loadsProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileFileSupplier supplier = ProfileFileSupplier.fixedProfileFile(credentialsFilePath);
        ProfileFile file = supplier.get();

        Optional<Profile> profileOptional = file.profile("default");
        assertThat(profileOptional).isPresent();

        assertThat(profileOptional.get()).satisfies(profile -> {
            Optional<String> awsAccessKeyIdOptional = profile.property("aws_access_key_id");
            assertThat(awsAccessKeyIdOptional).isPresent();
            String awsAccessKeyId = awsAccessKeyIdOptional.get();
            assertThat(awsAccessKeyId).isEqualTo("defaultAccessKey");

            Optional<String> awsSecretAccessKeyOptional = profile.property("aws_secret_access_key");
            assertThat(awsSecretAccessKeyOptional).isPresent();
            String awsSecretAccessKey = awsSecretAccessKeyOptional.get();
            assertThat(awsSecretAccessKey).isEqualTo("defaultSecretAccessKey");
        });
    }

    @Test
    void get_supplierBuiltByFixedProfileFileObject_returnsProfileFileInstance() {
        ProfileFile file = ProfileFile.defaultProfileFile();
        ProfileFileSupplier supplier = ProfileFileSupplier.fixedProfileFile(file);

        assertThat(supplier.get()).isSameAs(file);
    }

    @Test
    void wrapIntoNullableSupplier_nonNullProfileFile_returnsNonNullSupplier() {
        ProfileFile file = ProfileFile.defaultProfileFile();
        ProfileFileSupplier supplier = ProfileFileSupplier.wrapIntoNullableSupplier(file);

        assertThat(supplier).isNotNull();
    }

    @Test
    void wrapIntoNullableSupplier_nullProfileFile_returnsNullSupplier() {
        ProfileFile file = null;
        ProfileFileSupplier supplier = ProfileFileSupplier.wrapIntoNullableSupplier(file);

        assertThat(supplier).isNull();
    }

    @Test
    void fixedProfileFile_nullProfileFile_returnsNonNullSupplier() {
        ProfileFile file = null;
        ProfileFileSupplier supplier = ProfileFileSupplier.fixedProfileFile(file);

        assertThat(supplier).isNotNull();
    }

    @Test
    void get_givenOnLoadAction_callsActionOncePerNewProfileFile() {
        int actualProfilesCount = 3;
        AtomicInteger blockCount = new AtomicInteger();

        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = builderWithClock(clock)
            .reloadWhenModified(credentialsFilePath)
            .onProfileFileLoad(f -> blockCount.incrementAndGet())
            .build();
        Duration duration = Duration.ofSeconds(5);

        supplier.get();

        clock.tickForward(duration);
        supplier.get();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        supplier.get();

        generateTestCredentialsFile("updatedAccessKey", "updatedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        supplier.get();

        clock.tickForward(duration);
        supplier.get();

        assertThat(blockCount.get()).isEqualTo(actualProfilesCount);
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

    private ProfileFileSupplierBuilder builder() {
        return new ProfileFileSupplierBuilder();
    }

    private ProfileFileSupplierBuilder builderWithClock(Clock clock) {
        return new ProfileFileSupplierBuilder().clock(clock);
    }

    private static final class AdjustableClock extends Clock {
        private Instant time;

        private AdjustableClock() {
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