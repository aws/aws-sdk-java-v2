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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
    private final Supplier<RefreshResult<ProfileFile>> profileFileRefreshResultSupplier;
    private final CachedSupplier<ProfileFile> profileFileCache;
    private final Function<RuntimeException, ProfileFile> exceptionHandler;
    private final Duration staleDuration;
    private final Duration pollDuration;
    private final Duration prefetchDuration;
    private final Clock clock;

    private ProfileFileRefresher(Builder builder) {
        this.staleDuration = builder.refreshDuration;
        this.pollDuration = builder.pollingDuration;
        this.prefetchDuration = builder.prefetchDuration;
        this.exceptionHandler = builder.exceptionHandler;
        this.clock = builder.clock;
        this.profileFileRefreshResultSupplier = resultByReloadingProfileFileSupplier(builder.profileFile,
                                                                                     builder.onProfileFileReload);
        CachedSupplier.Builder<ProfileFile> cachedSupplierBuilder = CachedSupplier.builder(this::refreshResult)
                                                                                  .clock(this.clock);
        if (builder.asyncRefreshEnabled.booleanValue()) {
            cachedSupplierBuilder.prefetchStrategy(new NonBlocking(THREAD_CLASS_NAME));
        }
        this.profileFileCache = cachedSupplierBuilder.build();
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
        return profileFileCache.get();
    }

    private RefreshResult<ProfileFile> refreshResult() {
        return refreshAndGetResultFromSupplier();
    }

    @Override
    public void close() {
        profileFileCache.close();
    }

    /**
     * Creates a new instance of a ProfileFile object if the underlying disk file has been modified.
     */
    public static ProfileFile reloadIfStale(ProfileFile profileFile) {
        return reloadIfStale(profileFile, p -> { });
    }

    /**
     * Creates a new instance of a ProfileFile object if the underlying disk file has been modified and executes an action
     * and passes the newly created object to it.
     */
    public static ProfileFile reloadIfStale(ProfileFile profileFile, Consumer<ProfileFile> onReload) {
        if (profileFile.isStale()) {
            return reload(profileFile, onReload);
        }

        return profileFile;
    }

    /**
     * Creates a new instance of a ProfileFile object if the underlying disk file has been modified and prepares it for
     * assignment to the backing cache. In addition, the newly created ProfileFile instance is passed as an argument to a
     * custom action.
     */
    public RefreshResult<ProfileFile> refreshResultByReloadingIfStale(ProfileFile profileFile, Consumer<ProfileFile> onReload) {
        ProfileFile resultProfileFile = profileFile;
        Instant now = clock.instant();
        Instant staleTime;

        if (profileFile.isStale()) {
            resultProfileFile = reload(profileFile, onReload);
            staleTime = staleTime(now);
        } else {
            staleTime = pollTime(now);
        }

        return wrapIntoRefreshResult(resultProfileFile, staleTime);
    }

    private static ProfileFile reload(ProfileFile profileFile) {
        ProfileFile.Aggregator aggregator = ProfileFile.aggregator();
        for (ProfileFile.Builder.BuildDetails details : profileFile.getBuildDetails()) {
            ProfileFile.Builder builder = details.getBuilder();
            aggregator.addFile(builder.build());
        }
        return aggregator.build();
    }

    private static ProfileFile reload(ProfileFile profileFile, Consumer<ProfileFile> consumer) {
        ProfileFile reloadedProfileFile = reload(profileFile);
        consumer.accept(reloadedProfileFile);

        return reloadedProfileFile;
    }

    private Supplier<RefreshResult<ProfileFile>> resultByReloadingProfileFileSupplier(ProfileFile profileFile,
                                                                              Consumer<ProfileFile> onReload) {
        return () -> refreshResultByReloadingIfStale(profileFile, onReload);
    }

    private RefreshResult<ProfileFile> refreshAndGetResultFromSupplier() {
        try {
            return profileFileRefreshResultSupplier.get();
        } catch (RuntimeException exception) {
            ProfileFile profileFile = exceptionHandler.apply(exception);
            Instant now = Instant.now();
            Instant staleTime = staleTime(now);

            return wrapIntoRefreshResult(profileFile, staleTime);
        }
    }

    private RefreshResult<ProfileFile> wrapIntoRefreshResult(ProfileFile profileFile, Instant staleTime) {
        return RefreshResult.builder(profileFile)
                            .staleTime(staleTime)
                            .prefetchTime(prefetchTime(staleTime))
                            .build();
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
        if (Objects.isNull(staleTime) || Objects.isNull(prefetchDuration)) {
            return null;
        }

        return staleTime.minus(prefetchDuration);
    }

    public static class Builder {

        private Consumer<ProfileFile> onProfileFileReload = p -> { };
        private Function<RuntimeException, ProfileFile> exceptionHandler;
        private Duration refreshDuration;
        private Duration pollingDuration;
        private Duration prefetchDuration;
        private ProfileFile profileFile;
        private Boolean asyncRefreshEnabled = false;
        private Clock clock = Clock.systemUTC();

        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /**
         * Define the frequency with which the profile file refresher will check whether the profile file may have outstanding
         * changes and needs to be reloaded.
         *
         * @param refreshDuration Once credentials are loaded, the credentials provider will suspend refreshing and
         *                        only restart until this period has elapsed.
         * @param pollingDuration Once the credentials provider has started refreshing, the profile file will be checked
         *                        for changes with this frequency.
         */
        public Builder refresh(Duration refreshDuration, Duration pollingDuration) {
            this.refreshDuration = refreshDuration;
            this.pollingDuration = pollingDuration;
            return this;
        }

        /**
         * Configure the amount of time, relative to refresh and polling time, that the cached profile file is considered
         * close to stale and should be updated. See {@link #asyncRefreshEnabled}.
         */
        public Builder prefetchTime(Duration prefetchDuration) {
            this.prefetchDuration = prefetchDuration;
            return this;
        }

        /**
         * Sets a clock for managing stale and prefetch durations.
         */
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
}
