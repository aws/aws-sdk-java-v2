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

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileLocation;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.internal.ProfileFileRefresher;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Credentials provider based on AWS configuration profiles. This loads credentials from a {@link ProfileFile}, allowing you to
 * share multiple sets of AWS security credentials between different tools like the AWS SDK for Java and the AWS CLI.
 *
 * <p>See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html</p>
 *
 * <p>If this credentials provider is loading assume-role credentials from STS, it should be cleaned up with {@link #close()} if
 * it is no longer being used.</p>
 *
 * @see ProfileFile
 */
@SdkPublicApi
public final class ReloadingProfileCredentialsProvider
    implements AwsCredentialsProvider,
               SdkAutoCloseable,
               ToCopyableBuilder<ReloadingProfileCredentialsProvider.Builder, ReloadingProfileCredentialsProvider> {
    private AwsCredentials credentials;
    private final RuntimeException loadException;
    private final ProfileFileRefresher profileFileRefresher;
    private final Supplier<ProfileFile> profileFileSupplier;
    private final Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> reloadPredicate;
    private final String profileName;
    private final Supplier<ProfileFile> defaultProfileFileLoader;
    private final Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> defaultProfileFileReloadPredicate;
    private final Clock clock;
    private final Duration refreshDuration;
    private final Duration pollingDuration;
    private final Duration prefetchTime;

    /**
     * @see #builder()
     */
    private ReloadingProfileCredentialsProvider(BuilderImpl builder) {
        this.defaultProfileFileLoader = builder.defaultProfileFileLoader;
        this.defaultProfileFileReloadPredicate = builder.defaultProfileFileReloadPredicate;
        this.profileFileSupplier = builder.profileFileSupplier;
        this.reloadPredicate = builder.profileFileReloadPredicate;
        this.clock = builder.clock;
        this.refreshDuration = builder.refreshDuration;
        this.pollingDuration = builder.pollingDuration;
        this.prefetchTime = builder.prefetchTime;

        RuntimeException thrownException = null;
        String selectedProfileName = null;
        ProfileFileRefresher profileRefresher = null;

        try {
            selectedProfileName = Optional.ofNullable(builder.profileName)
                                          .orElseGet(ProfileFileSystemSetting.AWS_PROFILE::getStringValueOrThrow);

            Supplier<ProfileFile> selectedProfile = Optional.ofNullable(builder.profileFileSupplier)
                                                            .orElse(builder.defaultProfileFileLoader);

            Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> selectedPredicate
                = Optional.ofNullable(builder.profileFileReloadPredicate).orElse(builder.defaultProfileFileReloadPredicate);

            profileRefresher = ProfileFileRefresher.builder()
                                                   .profileFileSupplier(selectedProfile)
                                                   .profileFileReloadPredicate(selectedPredicate)
                                                   .exceptionHandler(builder.exceptionHandler)
                                                   .addOnProfileFileReload(this::handleProfileFileReload)
                                                   .refreshDuration(builder.refreshDuration)
                                                   .pollingDuration(builder.pollingDuration)
                                                   .prefetchTime(builder.prefetchTime)
                                                   .clock(builder.clock)
                                                   .asyncRefreshEnabled(builder.asyncRefreshEnabled)
                                                   .build();

        } catch (RuntimeException e) {
            // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception
            // will only be raised on calls to resolveCredentials. We don't want to raise an exception here because it may be
            // expected (eg. in the default credential chain).
            thrownException = e;
        }

        this.loadException = thrownException;
        this.profileName = selectedProfileName;
        this.profileFileRefresher = profileRefresher;
    }

    /**
     * Create a {@link ReloadingProfileCredentialsProvider} using the {@link ProfileFile#defaultProfileFile()} and default
     * profile name. Use {@link #builder()} for defining a custom {@link ReloadingProfileCredentialsProvider}.
     */
    public static ReloadingProfileCredentialsProvider create() {
        return builder().build();
    }

    /**
     * Create a {@link ReloadingProfileCredentialsProvider} using the given profile name and
     * {@link ProfileFile#defaultProfileFile()}. Use {@link #builder()} for defining a custom
     * {@link ReloadingProfileCredentialsProvider}.
     *
     * @param profileName the name of the profile to use from the {@link ProfileFile#defaultProfileFile()}
     */
    public static ReloadingProfileCredentialsProvider create(String profileName) {
        return builder().profileName(profileName).build();
    }

    /**
     * Get a builder for creating a custom {@link ReloadingProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        if (loadException != null) {
            throw loadException;
        }

        refreshProfileFile();

        return credentials;
    }

    private void handleProfileFileReload(ProfileFile profileFile) {
        updateCredentials(profileFile, profileName);
    }

    private void updateCredentials(ProfileFile profileFile, String profileName) {
        AwsCredentialsProvider credentialsProvider = createCredentialsProvider(profileFile, profileName);

        this.credentials = credentialsProvider.resolveCredentials();

        IoUtils.closeIfCloseable(credentialsProvider, null);
    }

    private ProfileFile refreshProfileFile() {
        return profileFileRefresher.refreshIfStale();
    }

    @Override
    public String toString() {
        return ToString.builder("ProfileCredentialsProvider")
                       .add("profileName", profileName)
                       .add("profileFile", profileFileSupplier)
                       .build();
    }

    @Override
    public void close() {
        profileFileRefresher.close();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    private AwsCredentialsProvider createCredentialsProvider(ProfileFile profileFile, String profileName) {
        // Load the profile and credentials provider
        return profileFile.profile(profileName)
                          .flatMap(p -> new ProfileCredentialsUtils(profileFile, p, profileFile::profile).credentialsProvider())
                          .orElseThrow(() -> {
                              String errorMessage = String.format("Profile file contained no credentials for " +
                                                                  "profile '%s': %s", profileName, profileFile);
                              return SdkClientException.builder().message(errorMessage).build();
                          });
    }

    public static Predicate<Instant> diskFileHasUpdatedModificationTime(Path contentLocation) {
        return ProfileFileRefresher.diskFileHasUpdatedModificationTime(contentLocation);
    }

    /**
     * A builder for creating a custom {@link ReloadingProfileCredentialsProvider}.
     */
    public interface Builder extends CopyableBuilder<Builder, ReloadingProfileCredentialsProvider> {

        /**
         * Define the name of the profile that should be used by this credentials provider. By default, the value in
         * {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         */
        Builder profileName(String profileName);

        /**
         * Define the mechanism for loading profile files.
         *
         * @param profileFileSupplier Supplier for generating a ProfileFile instance.
         */
        Builder profileFileSupplier(Supplier<ProfileFile> profileFileSupplier);

        /**
         * Define the condition for reloading a profile file.
         *
         * @param predicate Predicate for determining whether to execute the profileFileSupplier.
         */
        Builder profileFileReloadPredicate(Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> predicate);

        /**
         * Define the frequency with which the credentials provider will check whether the profile file may have outstanding
         * changes and needs to be reloaded.
         *
         * <p>The automatic refreshing of the ProfileFile can be disabled by setting value to null</p>
         *
         * @param refreshDuration Once credentials are loaded, the credentials provider will suspend refreshing and
         *                        only restart until this period has elapsed.
         */
        Builder refreshDuration(Duration refreshDuration);

        /**
         * Define the frequency with which the credentials provider will check whether the profile file may have outstanding
         * changes and needs to be reloaded.
         *
         * @param pollingDuration Once the credentials provider has started refreshing, the profile file will be checked
         *                        for changes with this frequency.
         */
        Builder pollingDuration(Duration pollingDuration);

        /**
         * Configure the amount of time, relative to refresh and polling time, that the cached profile file is considered
         * close to stale and should be updated. See {@link #asyncRefreshEnabled}.
         */
        Builder prefetchTime(Duration prefetchTime);

        /**
         * @param exceptionHandler Handler which takes action when a Runtime exception occurs while loading a profile file.
         *                         Handler can return a previously stored profile file or throw back the exception.
         */
        Builder exceptionHandler(Function<RuntimeException, ProfileFile> exceptionHandler);

        /**
         * Configure whether this refresher should fetch tokens asynchronously in the background. If this is true, threads are
         * less likely to block when {@link #resolveCredentials()} is called, but additional resources are used to maintain the
         * provider.
         *
         * <p>By default, this is disabled.</p>
         */
        Builder asyncRefreshEnabled(Boolean asyncRefreshEnabled);

        /**
         * Create a {@link ReloadingProfileCredentialsProvider} using the configuration applied to this builder.
         */
        @Override
        ReloadingProfileCredentialsProvider build();
    }

    static final class BuilderImpl implements Builder {
        private Supplier<ProfileFile> profileFileSupplier;
        private Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> profileFileReloadPredicate;
        private String profileName;
        private Supplier<ProfileFile> defaultProfileFileLoader = ProfileFile::defaultProfileFile;
        private Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> defaultProfileFileReloadPredicate
            = refreshRecord -> refreshRecord.wasCreatedBeforeFileModified(ProfileFileLocation.credentialsFilePath());
        private Function<RuntimeException, ProfileFile> exceptionHandler = e -> { throw e; };
        private Clock clock = Clock.systemUTC();
        private Duration refreshDuration;
        private Duration pollingDuration;
        private Duration prefetchTime;
        private Boolean asyncRefreshEnabled = Boolean.FALSE;

        BuilderImpl() {
        }

        BuilderImpl(ReloadingProfileCredentialsProvider provider) {
            this.profileName = provider.profileName;
            this.defaultProfileFileLoader = provider.defaultProfileFileLoader;
            this.defaultProfileFileReloadPredicate = provider.defaultProfileFileReloadPredicate;
            this.profileFileSupplier = provider.profileFileSupplier;
            this.profileFileReloadPredicate = provider.reloadPredicate;
            this.clock = provider.clock;
            this.refreshDuration = provider.refreshDuration;
            this.pollingDuration = provider.pollingDuration;
            this.prefetchTime = provider.prefetchTime;
        }

        @Override
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public void setProfileName(String profileName) {
            profileName(profileName);
        }

        @Override
        public Builder profileFileSupplier(Supplier<ProfileFile> profileFileSupplier) {
            this.profileFileSupplier = profileFileSupplier;
            return this;
        }

        public void setProfileFileSupplier(Supplier<ProfileFile> supplier) {
            profileFileSupplier(supplier);
        }

        @Override
        public Builder profileFileReloadPredicate(Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> predicate) {
            this.profileFileReloadPredicate = predicate;
            return this;
        }

        public void setProfileFileReloadPredicate(Predicate<ProfileFileRefresher.ProfileFileRefreshRecord> predicate) {
            profileFileReloadPredicate(predicate);
        }

        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public Builder refreshDuration(Duration refreshDuration) {
            this.refreshDuration = refreshDuration;
            return this;
        }

        public void setRefreshDuration(Duration refreshDuration) {
            refreshDuration(refreshDuration);
        }

        @Override
        public Builder pollingDuration(Duration pollingDuration) {
            this.pollingDuration = pollingDuration;
            return this;
        }

        public void setPollingDuration(Duration pollingDuration) {
            refreshDuration(pollingDuration);
        }

        @Override
        public Builder prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        public void setPrefetchTime(Duration prefetchTime) {
            prefetchTime(prefetchTime);
        }

        @Override
        public Builder exceptionHandler(Function<RuntimeException, ProfileFile> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        public void setExceptionHandler(Function<RuntimeException, ProfileFile> exceptionHandler) {
            exceptionHandler(exceptionHandler);
        }

        @Override
        public Builder asyncRefreshEnabled(Boolean asyncRefreshEnabled) {
            this.asyncRefreshEnabled = asyncRefreshEnabled;
            return this;
        }

        public void setAsyncRefreshEnabled(Boolean asyncRefreshEnabled) {
            asyncRefreshEnabled(asyncRefreshEnabled);
        }

        @Override
        public ReloadingProfileCredentialsProvider build() {
            return new ReloadingProfileCredentialsProvider(this);
        }

        /**
         * Override the default configuration file to be used when the customer does not explicitly set
         * profileFileSupplier(supplier);
         * {@link #profileFileSupplier(Supplier)}. Use of this method is only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfileFileLoader(Supplier<ProfileFile> defaultProfileFileLoader) {
            this.defaultProfileFileLoader = defaultProfileFileLoader;
            return this;
        }

        /**
         * Override the location of the default configuration file to be used when the customer does not explicitly set
         * profileFileReloadPredicate(predicate);
         * {@link #profileFileReloadPredicate(Predicate)}. Use of this method is only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfileFileReloadPredicate(Predicate<ProfileFileRefresher.ProfileFileRefreshRecord>
                                                    defaultProfileFileReloadPredicate) {
            this.defaultProfileFileReloadPredicate = defaultProfileFileReloadPredicate;
            return this;
        }
    }

}
