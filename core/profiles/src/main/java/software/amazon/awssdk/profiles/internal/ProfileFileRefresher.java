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

package software.amazon.awssdk.profiles.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Class used for caching and reloading ProfileFile objects from a Supplier.
 */
@SdkInternalApi
public final class ProfileFileRefresher implements SdkAutoCloseable {

    private static final ProfileFileRefreshRecord EMPTY_REFRESH_RECORD = ProfileFileRefreshRecord.builder()
                                                                                                 .refreshTime(Instant.MIN)
                                                                                                 .build();
    private final CachedSupplier<ProfileFileRefreshRecord> profileFileCache;
    private volatile ProfileFileRefreshRecord currentRefreshRecord;
    private final Supplier<ProfileFile> profileFile;
    private final Path profileFilePath;
    private final Function<RuntimeException, ProfileFile> exceptionHandler;
    private final Consumer<ProfileFile> onProfileFileReload;
    private final Clock clock;

    private ProfileFileRefresher(Builder builder) {
        this.exceptionHandler = builder.exceptionHandler;
        this.clock = builder.clock;
        this.profileFile = builder.profileFile;
        this.profileFilePath = builder.profileFilePath;
        this.onProfileFileReload = builder.onProfileFileReload;
        this.profileFileCache = CachedSupplier.builder(this::refreshResult)
                                              .clock(this.clock)
                                              .build();
        this.currentRefreshRecord = EMPTY_REFRESH_RECORD;
    }

    /**
     * Builder method to construct instance of ProfileFileRefresher.
     */
    public static ProfileFileRefresher.Builder builder() {
        return new ProfileFileRefresher.Builder();
    }

    /**
     * Retrieves the cache value or refreshes it if stale.
     */
    public ProfileFile refreshIfStale() {
        ProfileFileRefreshRecord cachedOrRefreshedRecord = profileFileCache.get();
        ProfileFile cachedOrRefreshedProfileFile = cachedOrRefreshedRecord.profileFile;
        if (isNewProfileFile(cachedOrRefreshedProfileFile)) {
            currentRefreshRecord = cachedOrRefreshedRecord;
        }

        return cachedOrRefreshedProfileFile;
    }

    @Override
    public void close() {
        profileFileCache.close();
    }

    private RefreshResult<ProfileFileRefreshRecord> refreshResult() {
        try {
            return reloadAsRefreshResultIfStale();
        } catch (RuntimeException exception) {
            Instant now = Instant.now();
            Instant staleTime = now;
            ProfileFile exceptionProfileFile = exceptionHandler.apply(exception);
            ProfileFileRefreshRecord refreshRecord = ProfileFileRefreshRecord.builder()
                                                                             .profileFile(exceptionProfileFile)
                                                                             .refreshTime(now)
                                                                             .build();

            return wrapIntoRefreshResult(refreshRecord, staleTime);
        }
    }

    private RefreshResult<ProfileFileRefreshRecord> reloadAsRefreshResultIfStale() {
        Instant now = clock.instant();
        Instant staleTime = now;
        ProfileFileRefreshRecord refreshRecord;

        if (canReloadProfileFile() || hasNotBeenPreviouslyLoaded()) {
            ProfileFile reloadedProfileFile = reload(profileFile, onProfileFileReload);
            refreshRecord = ProfileFileRefreshRecord.builder()
                                                    .profileFile(reloadedProfileFile)
                                                    .refreshTime(now)
                                                    .build();
        } else {
            refreshRecord = currentRefreshRecord;
        }

        return wrapIntoRefreshResult(refreshRecord, staleTime);
    }

    private <T> RefreshResult<T> wrapIntoRefreshResult(T value, Instant staleTime) {
        return RefreshResult.builder(value)
                            .staleTime(staleTime)
                            .build();
    }

    private static ProfileFile reload(Supplier<ProfileFile> supplier) {
        return supplier.get();
    }

    private static ProfileFile reload(Supplier<ProfileFile> supplier, Consumer<ProfileFile> consumer) {
        ProfileFile reloadedProfileFile = reload(supplier);
        consumer.accept(reloadedProfileFile);

        return reloadedProfileFile;
    }

    private boolean isNewProfileFile(ProfileFile profileFile) {
        return currentRefreshRecord.profileFile != profileFile;
    }

    private boolean canReloadProfileFile() {
        if (Objects.isNull(profileFilePath)) {
            return false;
        }

        try {
            Instant lastModifiedInstant = Files.getLastModifiedTime(profileFilePath).toInstant();
            return currentRefreshRecord.refreshTime.isBefore(lastModifiedInstant);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean hasNotBeenPreviouslyLoaded() {
        return currentRefreshRecord == EMPTY_REFRESH_RECORD;
    }


    public static final class Builder {

        private Supplier<ProfileFile> profileFile;
        private Path profileFilePath;
        private Consumer<ProfileFile> onProfileFileReload = p -> { };
        private Function<RuntimeException, ProfileFile> exceptionHandler;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileFilePath(Path profileFilePath) {
            this.profileFilePath = profileFilePath;
            return this;
        }

        /**
         * Sets a clock for managing stale and prefetch durations.
         */
        @SdkTestInternalApi
        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * @param exceptionHandler Handler which takes action when a Runtime exception occurs while loading a profile file.
         *                         Handler can return a previously stored profile file or throw back the exception.
         */
        public Builder exceptionHandler(Function<RuntimeException, ProfileFile> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Sets a custom action to perform when a profile file is reloaded. This action is executed when both the cache is stale
         * and the disk file associated with the profile file has been modified since the last load.
         *
         * @param consumer The action to perform.
         */
        public Builder onProfileFileReload(Consumer<ProfileFile> consumer) {
            this.onProfileFileReload = consumer;
            return this;
        }

        public ProfileFileRefresher build() {
            return new ProfileFileRefresher(this);
        }
    }

    /**
     * Class used to encapsulate additional refresh information.
     */
    public static final class ProfileFileRefreshRecord {
        private final Instant refreshTime;
        private final ProfileFile profileFile;

        private ProfileFileRefreshRecord(Builder builder) {
            this.profileFile = builder.profileFile;
            this.refreshTime = builder.refreshTime;
        }

        /**
         * The refreshed ProfileFile instance.
         */
        public ProfileFile profileFile() {
            return profileFile;
        }

        /**
         * The time at which the RefreshResult was created.
         */
        public Instant refreshTime() {
            return refreshTime;
        }

        static Builder builder() {
            return new Builder();
        }

        private static final class Builder {
            private Instant refreshTime;
            private ProfileFile profileFile;

            Builder refreshTime(Instant refreshTime) {
                this.refreshTime = refreshTime;
                return this;
            }

            Builder profileFile(ProfileFile profileFile) {
                this.profileFile = profileFile;
                return this;
            }

            ProfileFileRefreshRecord build() {
                return new ProfileFileRefreshRecord(this);
            }
        }
    }

}
