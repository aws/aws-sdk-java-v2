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

package software.amazon.awssdk.auth.token.credentials;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.token.internal.ProfileTokenProviderLoader;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;

/**
 * Token provider based on AWS configuration profiles. This loads token providers that require {@link ProfileFile} configuration,
 * allowing the user to share settings between different tools like the AWS SDK for Java and the AWS CLI.
 *
 * <p>See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html</p>
 *
 * @see ProfileFile
 */
@SdkPublicApi
public final class ProfileTokenProvider implements SdkTokenProvider, SdkAutoCloseable {
    private final SdkTokenProvider tokenProvider;
    private final RuntimeException loadException;

    private final String profileName;

    /**
     * @see #builder()
     */
    private ProfileTokenProvider(BuilderImpl builder) {
        SdkTokenProvider sdkTokenProvider = null;
        RuntimeException thrownException = null;
        Supplier<ProfileFile> selectedProfileFile = null;
        String selectedProfileName = null;

        try {
            selectedProfileName = Optional.ofNullable(builder.profileName)
                                          .orElseGet(ProfileFileSystemSetting.AWS_PROFILE::getStringValueOrThrow);

            // Load the profiles file
            selectedProfileFile = Optional.ofNullable(builder.profileFile)
                                          .orElse(builder.defaultProfileFileLoader);

            // Load the profile and token provider
            sdkTokenProvider = createTokenProvider(selectedProfileFile, selectedProfileName);

        } catch (RuntimeException e) {
            // If we couldn't load the provider for some reason, save an exception describing why.
            thrownException = e;
        }

        if (thrownException != null) {
            this.loadException = thrownException;
            this.tokenProvider = null;
            this.profileName = null;
        } else {
            this.loadException = null;
            this.tokenProvider = sdkTokenProvider;
            this.profileName = selectedProfileName;
        }
    }

    /**
     * Create a {@link ProfileTokenProvider} using the {@link ProfileFile#defaultProfileFile()} and default profile name.
     * Use {@link #builder()} for defining a custom {@link ProfileTokenProvider}.
     */
    public static ProfileTokenProvider create() {
        return builder().build();
    }

    /**
     * Create a {@link ProfileTokenProvider} using the given profile name and {@link ProfileFile#defaultProfileFile()}. Use
     * {@link #builder()} for defining a custom {@link ProfileTokenProvider}.
     *
     * @param profileName the name of the profile to use from the {@link ProfileFile#defaultProfileFile()}
     */
    public static ProfileTokenProvider create(String profileName) {
        return builder().profileName(profileName).build();
    }

    /**
     * Get a builder for creating a custom {@link ProfileTokenProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public SdkToken resolveToken() {
        if (loadException != null) {
            throw loadException;
        }
        return tokenProvider.resolveToken();
    }

    @Override
    public String toString() {
        return ToString.builder("ProfileTokenProvider")
                       .add("profileName", profileName)
                       .build();
    }

    @Override
    public void close() {
        // The delegate provider may be closeable. In this case, we should clean it up when this token provider is closed.
        IoUtils.closeIfCloseable(tokenProvider, null);
    }

    private SdkTokenProvider createTokenProvider(Supplier<ProfileFile> profileFile, String profileName) {
        return new ProfileTokenProviderLoader(profileFile, profileName)
            .tokenProvider()
            .orElseThrow(() -> {
                String errorMessage = String.format("Profile file contained no information for " +
                                                    "profile '%s'", profileName);
                return SdkClientException.builder().message(errorMessage).build();
            });
    }

    /**
     * A builder for creating a custom {@link ProfileTokenProvider}.
     */
    public interface Builder {

        /**
         * Define the profile file that should be used by this token provider. By default, the
         * {@link ProfileFile#defaultProfileFile()} is used.
         */
        Builder profileFile(Supplier<ProfileFile> profileFile);

        /**
         * Define the name of the profile that should be used by this token provider. By default, the value in
         * {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         */
        Builder profileName(String profileName);

        /**
         * Create a {@link ProfileTokenProvider} using the configuration applied to this builder.
         */
        ProfileTokenProvider build();
    }

    static final class BuilderImpl implements Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        private Supplier<ProfileFile> defaultProfileFileLoader = ProfileFile::defaultProfileFile;

        BuilderImpl() {
        }

        @Override
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public void setProfileFile(Supplier<ProfileFile> profileFile) {
            profileFile(profileFile);
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
        public ProfileTokenProvider build() {
            return new ProfileTokenProvider(this);
        }

        /**
         * Override the default configuration file to be used when the customer does not explicitly set
         * profileName(profileName);
         * {@link #profileFile(Supplier<ProfileFile>)}. Use of this method is only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfileFileLoader(Supplier<ProfileFile> defaultProfileFileLoader) {
            this.defaultProfileFileLoader = defaultProfileFileLoader;
            return this;
        }
    }
}
