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

import static software.amazon.awssdk.utils.cache.CachedSupplier.StaleValueBehavior.STRICT;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.internal.ProfileCredentialsUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.RefreshResult;

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
public final class ProfileCredentialsProvider
    implements AwsCredentialsProvider,
               SdkAutoCloseable,
               ToCopyableBuilder<ProfileCredentialsProvider.Builder, ProfileCredentialsProvider> {
    private final AtomicReference<AwsCredentials> credentials;
    private final RuntimeException loadException;

    private final AtomicReference<ProfileFile> profileFile;
    private final String profileName;
    private final Supplier<ProfileFile> defaultProfileFileLoader;

    private final CachedSupplier<AwsCredentials> credentialsCache;
    private final Clock clock;
    private final Duration refreshInterval;
    private final Duration pollingInterval;

    /**
     * @see #builder()
     */
    private ProfileCredentialsProvider(BuilderImpl builder) {
        RuntimeException thrownException = null;
        ProfileFile selectedProfile = null;
        String selectedProfileName = null;

        try {
            selectedProfileName = builder.profileName != null ? builder.profileName
                                                              : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();

            // Load the profiles file
            selectedProfile = Optional.ofNullable(builder.profileFile)
                                      .orElseGet(builder.defaultProfileFileLoader);
        } catch (RuntimeException e) {
            // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception
            // will only be raised on calls to resolveCredentials. We don't want to raise an exception here because it may be
            // expected (eg. in the default credential chain).
            thrownException = e;
        }

        this.loadException = thrownException;
        this.credentials = new AtomicReference<>();
        this.profileFile = new AtomicReference<>(selectedProfile);
        this.profileName = selectedProfileName;
        this.defaultProfileFileLoader = builder.defaultProfileFileLoader;
        this.clock = builder.clock;
        this.credentialsCache = CachedSupplier.builder(this::refreshCredentials)
                                              .staleValueBehavior(STRICT)
                                              .clock(this.clock)
                                              .build();
        this.refreshInterval = builder.refreshInterval;
        this.pollingInterval = builder.pollingInterval;
    }

    /**
     * Create a {@link ProfileCredentialsProvider} using the {@link ProfileFile#defaultProfileFile()} and default profile name.
     * Use {@link #builder()} for defining a custom {@link ProfileCredentialsProvider}.
     */
    public static ProfileCredentialsProvider create() {
        return builder().build();
    }

    /**
     * Create a {@link ProfileCredentialsProvider} using the given profile name and {@link ProfileFile#defaultProfileFile()}. Use
     * {@link #builder()} for defining a custom {@link ProfileCredentialsProvider}.
     *
     * @param profileName the name of the profile to use from the {@link ProfileFile#defaultProfileFile()}
     */
    public static ProfileCredentialsProvider create(String profileName) {
        return builder().profileName(profileName).build();
    }

    /**
     * Get a builder for creating a custom {@link ProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentialsCache.get();
    }

    @Override
    public String toString() {
        return ToString.builder("ProfileCredentialsProvider")
                       .add("profileName", profileName)
                       .add("profileFile", profileFile)
                       .build();
    }

    @Override
    public void close() {
        credentialsCache.close();
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

    private boolean needToReloadCredentials() {
        return Objects.isNull(credentials.get()) || profileFile.get().isStale();
    }

    private RefreshResult<AwsCredentials> refreshCredentials() {
        if (loadException != null) {
            throw loadException;
        }

        Instant now = clock.instant();
        Instant staleTime;
        AwsCredentials nextCredentials;
        if (needToReloadCredentials()) {
            synchronized (this) {
                ProfileFile previousProfileFile = profileFile.get();
                ProfileFile newProfileFile = previousProfileFile.reload();
                profileFile.set(newProfileFile);

                AwsCredentialsProvider credentialsProvider = createCredentialsProvider(newProfileFile, this.profileName);
                nextCredentials = credentialsProvider.resolveCredentials();
                credentials.set(nextCredentials);

                // The delegate credentials provider may be closeable (eg. if it's an STS credentials provider). In this case,
                // we should clean it up when this credentials provider is closed.
                IoUtils.closeIfCloseable(credentialsProvider, null);
            }
            staleTime = staleTime(now);
        } else {
            nextCredentials = credentials.get();
            staleTime = pollTime(now);
        }

        return RefreshResult.builder(nextCredentials)
                            .staleTime(staleTime)
                            .build();
    }

    private Instant staleTime(Instant now) {
        if (Objects.isNull(now) || Objects.isNull(refreshInterval)) {
            return Instant.MAX;
        }

        return now.plus(refreshInterval);
    }

    private Instant pollTime(Instant now) {
        if (Objects.isNull(now) || Objects.isNull(pollingInterval)) {
            return staleTime(now);
        }

        return now.plus(pollingInterval);
    }

    /**
     * A builder for creating a custom {@link ProfileCredentialsProvider}.
     */
    public interface Builder extends CopyableBuilder<Builder, ProfileCredentialsProvider> {

        /**
         * Define the profile file that should be used by this credentials provider. By default, the
         * {@link ProfileFile#defaultProfileFile()} is used.
         */
        Builder profileFile(ProfileFile profileFile);

        /**
         * Similar to {@link #profileFile(ProfileFile)}, but takes a lambda to configure a new {@link ProfileFile.Builder}. This
         * removes the need to called {@link ProfileFile#builder()} and {@link ProfileFile.Builder#build()}.
         */
        Builder profileFile(Consumer<ProfileFile.Builder> profileFile);

        /**
         * Define the name of the profile that should be used by this credentials provider. By default, the value in
         * {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         */
        Builder profileName(String profileName);

        /**
         * Define the frequency with which the credentials provider will check whether the profile file may have outstanding
         * changes and needs to be reloaded.
         *
         * @param refreshInterval Once credentials are loaded, the credentials provider will suspend refreshing and
         *                        only restart until this period has elapsed.
         * @param pollingInterval Once the credentials provider has started refreshing, the profile file will be checked
         *                        for changes with this frequency.
         */
        Builder refresh(Duration refreshInterval, Duration pollingInterval);

        /**
         * Create a {@link ProfileCredentialsProvider} using the configuration applied to this builder.
         */
        @Override
        ProfileCredentialsProvider build();
    }

    static final class BuilderImpl implements Builder {
        private ProfileFile profileFile;
        private String profileName;
        private Supplier<ProfileFile> defaultProfileFileLoader = ProfileFile::defaultProfileFile;
        private Clock clock = Clock.systemUTC();
        private Duration refreshInterval;
        private Duration pollingInterval;

        BuilderImpl() {
        }

        BuilderImpl(ProfileCredentialsProvider provider) {
            this.profileFile = provider.profileFile.get();
            this.profileName = provider.profileName;
            this.defaultProfileFileLoader = provider.defaultProfileFileLoader;
            this.clock = provider.clock;
            this.refreshInterval = provider.refreshInterval;
            this.pollingInterval = provider.pollingInterval;
        }

        @Override
        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public void setProfileFile(ProfileFile profileFile) {
            profileFile(profileFile);
        }

        @Override
        public Builder profileFile(Consumer<ProfileFile.Builder> profileFile) {
            return profileFile(ProfileFile.builder().applyMutation(profileFile).build());
        }

        @Override
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public void setProfileName(String profileName) {
            profileName(profileName);
        }

        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public Builder refresh(Duration refreshInterval, Duration pollingInterval) {
            this.refreshInterval = refreshInterval;
            this.pollingInterval = pollingInterval;
            return this;
        }

        @Override
        public ProfileCredentialsProvider build() {
            return new ProfileCredentialsProvider(this);
        }

        /**
         * Override the default configuration file to be used when the customer does not explicitly set
         * profileName(profileName);
         * {@link #profileFile(ProfileFile)}. Use of this method is only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfileFileLoader(Supplier<ProfileFile> defaultProfileFileLoader) {
            this.defaultProfileFileLoader = defaultProfileFileLoader;
            return this;
        }
    }
}
