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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;

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
            .fixedProfileFile(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
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
            .reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
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
            .reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
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
            .reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
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
            .reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
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
            .reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
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

        ProfileFileSupplier supplier = ProfileFileSupplier.reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS);
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
    void get_supplierBuiltByFixedProfileFile_returnsProfileFile() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileFileSupplier supplier = ProfileFileSupplier.fixedProfileFile(ProfileFile.builder()
                                                                                       .content(credentialsFilePath)
                                                                                       .type(ProfileFile.Type.CREDENTIALS)
                                                                                       .build());
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
    void get_supplierBuiltByReloadWhenModifiedAggregate_reloadsCredentials() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        Path configFilePath = generateTestConfigFile(Pair.of("region", "us-west-2"));

        ProfileFileSupplier credentialsProfileFileSupplier = ProfileFileSupplier.reloadWhenModified(credentialsFilePath,
                                                                                                    ProfileFile.Type.CREDENTIALS);
        ProfileFileSupplier configProfileFileSupplier = ProfileFileSupplier.reloadWhenModified(configFilePath,
                                                                                               ProfileFile.Type.CONFIGURATION);
        ProfileFileSupplier supplier = ProfileFileSupplier.aggregate(credentialsProfileFileSupplier, configProfileFileSupplier);

        Optional<Profile> fileOptional = supplier.get().profile("default");
        assertThat(fileOptional).isPresent();

        assertThat(fileOptional.get()).satisfies(profile -> {
            Optional<String> awsAccessKeyIdOptional = profile.property("aws_access_key_id");
            assertThat(awsAccessKeyIdOptional).isPresent();
            String awsAccessKeyId = awsAccessKeyIdOptional.get();
            assertThat(awsAccessKeyId).isEqualTo("defaultAccessKey");

            Optional<String> awsSecretAccessKeyOptional = profile.property("aws_secret_access_key");
            assertThat(awsSecretAccessKeyOptional).isPresent();
            String awsSecretAccessKey = awsSecretAccessKeyOptional.get();
            assertThat(awsSecretAccessKey).isEqualTo("defaultSecretAccessKey");

            Optional<String> regionOptional = profile.property("region");
            assertThat(regionOptional).isPresent();
            String region = regionOptional.get();
            assertThat(region).isEqualTo("us-west-2");
        });
    }

    @Test
    void get_supplierBuiltByFixedProfileFileAggregate_returnsAggregateProfileFileInstance() {
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");
        Path configFilePath = generateTestConfigFile(Pair.of("region", "us-west-2"));

        ProfileFileSupplier credentialsProfileFileSupplier
            = ProfileFileSupplier.fixedProfileFile(ProfileFile.builder()
                                                              .content(credentialsFilePath)
                                                              .type(ProfileFile.Type.CREDENTIALS)
                                                              .build());
        ProfileFileSupplier configProfileFileSupplier
            = ProfileFileSupplier.fixedProfileFile(ProfileFile.builder()
                                                              .content(configFilePath)
                                                              .type(ProfileFile.Type.CONFIGURATION)
                                                              .build());
        ProfileFileSupplier supplier = ProfileFileSupplier.aggregate(credentialsProfileFileSupplier, configProfileFileSupplier);
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

            Optional<String> regionOptional = profile.property("region");
            assertThat(regionOptional).isPresent();
            String region = regionOptional.get();
            assertThat(region).isEqualTo("us-west-2");
        });
    }

    @Test
    void aggregate_supplierReturnsSameInstanceMultipleTimesAggregatingProfileFile_aggregatesOnlyDistinctInstances() {
        ProfileFile credentialFile1 = credentialFile("test1", "key1", "secret1");
        ProfileFile credentialFile2 = credentialFile("test2", "key2", "secret2");
        ProfileFile credentialFile3 = credentialFile("test3", "key3", "secret3");
        ProfileFile credentialFile4 = credentialFile("test4", "key4", "secret4");
        ProfileFile configFile = configFile("profile test", Pair.of("region", "us-west-2"));

        List<ProfileFile> orderedCredentialsFiles
            = Arrays.asList(credentialFile1, credentialFile1, credentialFile2, credentialFile3, credentialFile3, credentialFile4,
                            credentialFile4, credentialFile4);

        ProfileFile aggregateFile1 = ProfileFile.aggregator().addFile(credentialFile1).addFile(configFile).build();
        ProfileFile aggregateFile2 = ProfileFile.aggregator().addFile(credentialFile2).addFile(configFile).build();
        ProfileFile aggregateFile3 = ProfileFile.aggregator().addFile(credentialFile3).addFile(configFile).build();
        ProfileFile aggregateFile4 = ProfileFile.aggregator().addFile(credentialFile4).addFile(configFile).build();

        List<ProfileFile> distinctAggregateFiles = Arrays.asList(aggregateFile1, aggregateFile2, aggregateFile3, aggregateFile4);

        ProfileFileSupplier supplier = ProfileFileSupplier.aggregate(supply(orderedCredentialsFiles), () -> configFile);

        List<ProfileFile> suppliedProfileFiles = Stream.generate(supplier)
                                                       .limit(orderedCredentialsFiles.size())
                                                       .filter(uniqueInstances())
                                                       .collect(Collectors.toList());

        assertThat(suppliedProfileFiles).isEqualTo(distinctAggregateFiles);
    }

    @Test
    void aggregate_supplierReturnsSameInstanceMultipleTimesAggregatingProfileFileSupplier_aggregatesOnlyDistinctInstances() {
        ProfileFile credentialFile1 = credentialFile("test1", "key1", "secret1");
        ProfileFile credentialFile2 = credentialFile("test2", "key2", "secret2");
        ProfileFile credentialFile3 = credentialFile("test3", "key3", "secret3");
        ProfileFile credentialFile4 = credentialFile("test4", "key4", "secret4");
        ProfileFile configFile1 = configFile("profile test", Pair.of("region", "us-west-1"));
        ProfileFile configFile2 = configFile("profile test", Pair.of("region", "us-west-2"));
        ProfileFile configFile3 = configFile("profile test", Pair.of("region", "us-west-3"));

        List<ProfileFile> orderedCredentialsFiles
            = Arrays.asList(credentialFile1, credentialFile1, credentialFile2, credentialFile2, credentialFile3,
                            credentialFile4, credentialFile4, credentialFile4);

        List<ProfileFile> orderedConfigFiles
            = Arrays.asList(configFile1, configFile1, configFile1, configFile2, configFile3, configFile3, configFile3,
                            configFile3);

        ProfileFile aggregateFile11 = ProfileFile.aggregator().addFile(credentialFile1).addFile(configFile1).build();
        ProfileFile aggregateFile21 = ProfileFile.aggregator().addFile(credentialFile2).addFile(configFile1).build();
        ProfileFile aggregateFile22 = ProfileFile.aggregator().addFile(credentialFile2).addFile(configFile2).build();
        ProfileFile aggregateFile33 = ProfileFile.aggregator().addFile(credentialFile3).addFile(configFile3).build();
        ProfileFile aggregateFile43 = ProfileFile.aggregator().addFile(credentialFile4).addFile(configFile3).build();

        List<ProfileFile> aggregateProfileFiles
            = Arrays.asList(aggregateFile11, aggregateFile11, aggregateFile21, aggregateFile22, aggregateFile33,
                            aggregateFile43, aggregateFile43, aggregateFile43);

        List<ProfileFile> distinctAggregateProfileFiles
            = Arrays.asList(aggregateFile11, aggregateFile21, aggregateFile22, aggregateFile33, aggregateFile43);

        ProfileFileSupplier supplier = ProfileFileSupplier.aggregate(supply(orderedCredentialsFiles), supply(orderedConfigFiles));

        List<ProfileFile> suppliedProfileFiles = Stream.generate(supplier)
                                                       .filter(Objects::nonNull)
                                                       .limit(aggregateProfileFiles.size())
                                                       .filter(uniqueInstances())
                                                       .collect(Collectors.toList());

        assertThat(suppliedProfileFiles).isEqualTo(distinctAggregateProfileFiles);
    }

    @Test
    void aggregate_duplicateOptionsGivenFixedProfileFirst_preservesPrecedence() {
        ProfileFile configFile1 = configFile("profile default", Pair.of("aws_access_key_id", "config-key"));
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileFileSupplier supplier = ProfileFileSupplier.aggregate(
            ProfileFileSupplier.fixedProfileFile(configFile1),
            ProfileFileSupplier.reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS));

        ProfileFile profileFile = supplier.get();
        String accessKeyId = profileFile.profile("default").get().property("aws_access_key_id").get();

        assertThat(accessKeyId).isEqualTo("config-key");

        generateTestCredentialsFile("defaultAccessKey2", "defaultSecretAccessKey2");

        profileFile = supplier.get();
        accessKeyId = profileFile.profile("default").get().property("aws_access_key_id").get();

        assertThat(accessKeyId).isEqualTo("config-key");
    }

    @Test
    void aggregate_duplicateOptionsGivenReloadingProfileFirst_preservesPrecedence() {
        AdjustableClock clock = new AdjustableClock();

        ProfileFile configFile1 = configFile("profile default", Pair.of("aws_access_key_id", "config-key"));
        Path credentialsFilePath = generateTestCredentialsFile("defaultAccessKey", "defaultSecretAccessKey");

        ProfileFileSupplier supplier = ProfileFileSupplier.aggregate(
            builderWithClock(clock)
                .reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
                .build(),
            ProfileFileSupplier.fixedProfileFile(configFile1));

        ProfileFile profileFile = supplier.get();
        String accessKeyId = profileFile.profile("default").get().property("aws_access_key_id").get();

        assertThat(accessKeyId).isEqualTo("defaultAccessKey");

        generateTestCredentialsFile("defaultAccessKey2", "defaultSecretAccessKey2");

        clock.tickForward(Duration.ofMillis(1_000));

        profileFile = supplier.get();
        accessKeyId = profileFile.profile("default").get().property("aws_access_key_id").get();

        assertThat(accessKeyId).isEqualTo("defaultAccessKey2");
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
            .reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS)
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

    private Path generateTestConfigFile(Pair<Object, Object>... pairs) {
        String values = Arrays.stream(pairs)
                              .map(pair -> String.format("%s=%s", pair.left(), pair.right()))
                              .collect(Collectors.joining(System.lineSeparator()));
        String contents = String.format("[default]\n%s", values);

        return generateTestFile(contents, "config.txt");
    }

    private void updateModificationTime(Path path, Instant instant) {
        try {
            Files.setLastModifiedTime(path, FileTime.from(instant));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private ProfileFile configFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    private ProfileFile configFile(String name, Pair<?, ?>... pairs) {
        String values = Arrays.stream(pairs)
                              .map(pair -> String.format("%s=%s", pair.left(), pair.right()))
                              .collect(Collectors.joining(System.lineSeparator()));
        String contents = String.format("[%s]\n%s", name, values);

        return configFile(contents);
    }

    private static <T> Predicate<T> uniqueInstances() {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return e -> {
            boolean unique = seen.stream().noneMatch(o -> o == e);
            if (unique) {
                seen.add(e);
            }

            return unique;
        };
    }

    private static ProfileFileSupplier supply(Iterable<ProfileFile> iterable) {
        return iterable.iterator()::next;
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