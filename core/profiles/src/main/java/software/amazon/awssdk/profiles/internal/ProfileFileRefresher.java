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
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Class used for caching and reloading ProfileFile objects from a Supplier.
 */
@SdkInternalApi
public final class ProfileFileRefresher implements SdkAutoCloseable {

    private static final String THREAD_CLASS_NAME = "sdk-profile-file-refresher";
    private final CachedSupplier<ProfileFileRefreshRecord> profileFileCache;
    private volatile ProfileFileRefreshRecord currentRefreshRecord;
    private final ProfileFileRefreshRecord emptyRefreshRecord;
    private final Supplier<ProfileFile> profileFileSupplier;
    private final Predicate<ProfileFileRefreshRecord> profileFileReloadPredicate;
    private final Function<RuntimeException, ProfileFile> exceptionHandler;
    private final Consumer<ProfileFile> onProfileFileReload;
    private final Duration staleDuration;
    private final Duration pollDuration;
    private final Duration prefetchTime;
    private final Clock clock;

    private ProfileFileRefresher(Builder builder) {
        this.staleDuration = builder.refreshDuration;
        this.pollDuration = builder.pollingDuration;
        this.prefetchTime = builder.prefetchTime;
        this.exceptionHandler = builder.exceptionHandler;
        this.clock = builder.clock;
        this.profileFileSupplier = builder.profileFileSupplier;
        this.profileFileReloadPredicate = builder.profileFileReloadPredicate;
        this.onProfileFileReload = builder.onProfileFileReload;
        CachedSupplier.Builder<ProfileFileRefreshRecord> cachedSupplierBuilder = CachedSupplier.builder(this::refreshResult)
                                                                                               .clock(this.clock);
        if (builder.asyncRefreshEnabled.booleanValue()) {
            cachedSupplierBuilder.prefetchStrategy(new NonBlocking(THREAD_CLASS_NAME));
        }
        this.profileFileCache = cachedSupplierBuilder.build();
        this.emptyRefreshRecord = ProfileFileRefreshRecord.builder()
                                                          .refreshTime(Instant.MIN)
                                                          .build();
        this.currentRefreshRecord = this.emptyRefreshRecord;
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
        return refreshResultIfStale();
    }

    private RefreshResult<ProfileFileRefreshRecord> refreshResultIfStale() {
        try {
            return reloadAsRefreshResultIfStale();
        } catch (RuntimeException exception) {
            Instant now = Instant.now();
            ProfileFile exceptionProfileFile = exceptionHandler.apply(exception);
            ProfileFileRefreshRecord refreshRecord = ProfileFileRefreshRecord.builder()
                                                                             .profileFile(exceptionProfileFile)
                                                                             .refreshTime(now)
                                                                             .build();

            return wrapIntoRefreshResult(refreshRecord, staleTime(now));
        }
    }

    private RefreshResult<ProfileFileRefreshRecord> reloadAsRefreshResultIfStale() {
        Instant now = clock.instant();
        Instant staleTime;
        ProfileFileRefreshRecord refreshRecord;

        if (shouldReloadProfileFile() || hasNotBeenPreviouslyLoaded()) {
            staleTime = staleTime(now);
            ProfileFile reloadedProfileFile = reload(profileFileSupplier, onProfileFileReload);
            refreshRecord = ProfileFileRefreshRecord.builder()
                                                    .profileFile(reloadedProfileFile)
                                                    .refreshTime(now)
                                                    .build();
        } else {
            staleTime = pollTime(now);
            refreshRecord = currentRefreshRecord;
        }

        return wrapIntoRefreshResult(refreshRecord, staleTime);
    }

    private <T> RefreshResult<T> wrapIntoRefreshResult(T value, Instant staleTime) {
        return RefreshResult.builder(value)
                            .staleTime(staleTime)
                            .prefetchTime(prefetchTime(staleTime))
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

    private Instant staleTime(Instant now) {
        if (Objects.isNull(now) || Objects.isNull(staleDuration)) {
            return Instant.MAX;
        }

        return now.plus(staleDuration);
    }

    private Instant pollTime(Instant now) {
        if (Objects.isNull(now) || Objects.isNull(pollDuration)) {
            return staleTime(now);
        }

        return now.plus(pollDuration);
    }

    private Instant prefetchTime(Instant staleTime) {
        if (Objects.isNull(staleTime) || Objects.isNull(prefetchTime)) {
            return null;
        }

        return staleTime.minus(prefetchTime);
    }

    private boolean isNewProfileFile(ProfileFile profileFile) {
        return currentRefreshRecord.profileFile != profileFile;
    }

    private boolean shouldReloadProfileFile() {
        return profileFileReloadPredicate.test(currentRefreshRecord);
    }

    private boolean hasNotBeenPreviouslyLoaded() {
        return currentRefreshRecord == emptyRefreshRecord;
    }


    /**
     * Convenience method for specifying an argument to {@link Builder#profileFileReloadPredicate(Predicate)}. This predicate 
     * returns true when the time given occurs prior to the file modified timestamp.
     * @param contentLocation A Path object representing the file to test against.
     * @return A Predicate instance.
     * @see ProfileFileRefreshRecord#diskFileHasUpdatedModificationTime(Path)
     */
    public static Predicate<Instant> diskFileHasUpdatedModificationTime(Path contentLocation) {
        return instant -> {
            try {
                Instant lastModifiedInstant = getFileLastModifiedTimeAsInstant(contentLocation);
                return instant.isBefore(lastModifiedInstant);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static Instant getFileLastModifiedTimeAsInstant(Path contentLocation) throws IOException {
        return Files.getLastModifiedTime(contentLocation).toInstant();
    }


    public static final class Builder {

        private Supplier<ProfileFile> profileFileSupplier;
        private Predicate<ProfileFileRefreshRecord> profileFileReloadPredicate;
        private Consumer<ProfileFile> onProfileFileReload = p -> { };
        private Function<RuntimeException, ProfileFile> exceptionHandler;
        private Duration refreshDuration;
        private Duration pollingDuration;
        private Duration prefetchTime;
        private Boolean asyncRefreshEnabled = Boolean.FALSE;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        /**
         * Define the mechanism for loading profile files.
         *
         * @param profileFileSupplier Supplier for generating a ProfileFile instance.
         */
        public Builder profileFileSupplier(Supplier<ProfileFile> profileFileSupplier) {
            this.profileFileSupplier = profileFileSupplier;
            return this;
        }

        /**
         * Define the condition for reloading a profile file.
         *
         * @param predicate Predicate for determining whether to execute the profileFileSupplier.
         */
        public Builder profileFileReloadPredicate(Predicate<ProfileFileRefreshRecord> predicate) {
            this.profileFileReloadPredicate = predicate;
            return this;
        }

        /**
         * Define the frequency with which the profile file refresher will check whether the profile file may have outstanding
         * changes and needs to be reloaded.
         *
         * @param refreshDuration Once credentials are loaded, the credentials provider will suspend refreshing and
         *                        only restart until this period has elapsed.
         */
        public Builder refreshDuration(Duration refreshDuration) {
            this.refreshDuration = refreshDuration;
            return this;
        }

        /**
         * Define the frequency with which the profile file refresher will check whether the profile file may have outstanding
         * changes and needs to be reloaded.
         *
         * @param pollingDuration Once the credentials provider has started refreshing, the profile file will be checked
         *                        for changes with this frequency.
         */
        public Builder pollingDuration(Duration pollingDuration) {
            this.pollingDuration = pollingDuration;
            return this;
        }

        /**
         * Configure the amount of time, relative to refresh and polling time, that the cached profile file is considered
         * close to stale and should be updated. See {@link #asyncRefreshEnabled}.
         */
        public Builder prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
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
         * Configure whether this refresher should fetch tokens asynchronously in the background. If this is true, threads are
         * less likely to block when {@link #refreshIfStale()} is called, but additional resources are used to maintain the
         * provider.
         *
         * <p>By default, this is disabled.</p>
         */
        public Builder asyncRefreshEnabled(Boolean asyncRefreshEnabled) {
            this.asyncRefreshEnabled = asyncRefreshEnabled;
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
         * Adds a custom action to perform when a profile file is reloaded. This action is executed when both the cache is stale
         * and the disk file associated with the profile file has been modified since the last load.
         *
         * @param consumer The action to perform.
         */
        public Builder addOnProfileFileReload(Consumer<ProfileFile> consumer) {
            this.onProfileFileReload = Objects.isNull(this.onProfileFileReload) ? consumer :
                                       this.onProfileFileReload.andThen(consumer);
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

        /**
         * Specifies whether this instance was created before the modified timestamp of a given file.
         * @param path The Path object to the file to test against.
         */
        public boolean wasCreatedBeforeFileModified(Path path) {
            return diskFileHasUpdatedModificationTime(path).test(refreshTime);
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
