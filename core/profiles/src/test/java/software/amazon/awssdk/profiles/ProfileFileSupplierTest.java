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
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
    void refreshIfStale_profileFileFixed_doesNotReloadProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileFileSupplier supplier = ProfileFileSupplier.builder()
                                                          .fixedProfileFile(credentialsFilePath)
                                                          .build();

        ProfileFile file1 = supplier.profileFile();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");

        ProfileFile file2 = supplier.profileFile();

        Assertions.assertThat(file2).isSameAs(file1);
    }

    @Test
    void refreshIfStale_profileModified_reloadsProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = ProfileFileSupplier.builder()
                                                          .reloadWhenModified(credentialsFilePath)
                                                          .clock(clock)
                                                          .build();

        Duration duration = Duration.ofSeconds(10);
        ProfileFile file1 = supplier.profileFile();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file2 = supplier.profileFile();

        Assertions.assertThat(file2).isNotSameAs(file1);
    }

    @Test
    void refreshIfStale_profileModifiedOnceButRefreshedMultipleTimes_reloadsProfileFileOnce() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = ProfileFileSupplier.builder()
                                                          .reloadWhenModified(credentialsFilePath)
                                                          .clock(clock)
                                                          .build();
        ProfileFile file1 = supplier.profileFile();

        clock.tickForward(Duration.ofSeconds(5));
        ProfileFile file2 = supplier.profileFile();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(Duration.ofSeconds(5));
        ProfileFile file3 = supplier.profileFile();

        Assertions.assertThat(file2).isSameAs(file1);
        Assertions.assertThat(file3).isNotSameAs(file2);
    }

    @Test
    void refreshIfStale_profileModifiedMultipleTimes_reloadsProfileFileOncePerChange() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = ProfileFileSupplier.builder()
                                                          .reloadWhenModified(credentialsFilePath)
                                                          .clock(clock)
                                                          .build();
        Duration duration = Duration.ofSeconds(5);

        ProfileFile file1 = supplier.profileFile();

        clock.tickForward(duration);
        ProfileFile file2 = supplier.profileFile();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file3 = supplier.profileFile();

        generateTestCredentialsFile("updatedAccessKey", "updatedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file4 = supplier.profileFile();

        clock.tickForward(duration);
        ProfileFile file5 = supplier.profileFile();

        Assertions.assertThat(file2).isSameAs(file1);
        Assertions.assertThat(file3).isNotSameAs(file2);
        Assertions.assertThat(file4).isNotSameAs(file3);
        Assertions.assertThat(file5).isSameAs(file4);
    }

    @Test
    void refreshIfStale_givenOnLoadAction_callsActionOncePerNewProfileFile() {
        int actualProfilesCount = 3;
        AtomicInteger blockCount = new AtomicInteger();

        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        AdjustableClock clock = new AdjustableClock();
        ProfileFileSupplier supplier = ProfileFileSupplier.builder()
                                                          .reloadWhenModified(credentialsFilePath)
                                                          .clock(clock)
                                                          .onProfileFileLoad(f -> blockCount.incrementAndGet())
                                                          .build();
        Duration duration = Duration.ofSeconds(5);

        ProfileFile file1 = supplier.profileFile();

        clock.tickForward(duration);
        ProfileFile file2 = supplier.profileFile();

        generateTestCredentialsFile("modifiedAccessKey", "modifiedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file3 = supplier.profileFile();

        generateTestCredentialsFile("updatedAccessKey", "updatedSecretAccessKey");
        updateModificationTime(credentialsFilePath, clock.instant().plusMillis(1));

        clock.tickForward(duration);
        ProfileFile file4 = supplier.profileFile();

        clock.tickForward(duration);
        ProfileFile file5 = supplier.profileFile();

        Assertions.assertThat(blockCount.get()).isEqualTo(actualProfilesCount);
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