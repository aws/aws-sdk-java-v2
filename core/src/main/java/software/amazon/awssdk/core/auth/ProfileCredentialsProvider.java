/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.auth;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.profile.Profile;
import software.amazon.awssdk.auth.profile.ProfileFile;
import software.amazon.awssdk.core.AwsSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;

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
public final class ProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private final AwsCredentialsProvider profileCredentialsProvider;
    private final RuntimeException loadException;

    private final ProfileFile profileFile;
    private final String profileName;

    /**
     * @see #builder()
     */
    private ProfileCredentialsProvider(Builder builder) {
        this.profileName = builder.profileName != null ? builder.profileName
                                                       : AwsSystemSetting.AWS_PROFILE.getStringValueOrThrow();

        // Load the profiles file
        this.profileFile = Optional.ofNullable(builder.profileFile)
                                   .orElseGet(builder.defaultProfileFileLoader);

        // Load the profile and credentials provider
        this.profileCredentialsProvider = profileFile.profile(profileName)
                                                     .flatMap(Profile::credentialsProvider)
                                                     .orElse(null);

        // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception will
        // only be raised on calls to getCredentials. We don't want to raise an exception here because it may be expected (eg. in
        // the default credential chain).
        if (profileCredentialsProvider == null) {
            String loadError = String.format("Profile file contained no credentials for profile '%s': %s",
                                             profileName, profileFile);
            this.loadException = new SdkClientException(loadError);
        } else {
            this.loadException = null;
        }
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
     * @param profileName the name of the profile to use from the {@link ProfileFile#defaultProfileFile()}
     */
    public static ProfileCredentialsProvider create(String profileName) {
        return builder().profileName(profileName).build();
    }

    /**
     * Get a builder for creating a custom {@link ProfileCredentialsProvider}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AwsCredentials getCredentials() {
        if (loadException != null) {
            throw loadException;
        }
        return profileCredentialsProvider.getCredentials();
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
        // The delegate credentials provider may be closeable (eg. if it's an STS credentials provider). In this case, we should
        // clean it up when this credentials provider is closed.
        IoUtils.closeIfCloseable(profileCredentialsProvider, null);
    }

    /**
     * A builder for creating a custom {@link ProfileCredentialsProvider}.
     */
    public static final class Builder {
        private ProfileFile profileFile;
        private String profileName;

        private Supplier<ProfileFile> defaultProfileFileLoader = ProfileFile::defaultProfileFile;

        private Builder() {}

        /**
         * Define the profile file that should be used by this credentials provider. By default, the
         * {@link ProfileFile#defaultProfileFile()} is used.
         */
        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /**
         * Similar to {@link #profileFile(ProfileFile)}, but takes a lambda to configure a new {@link ProfileFile.Builder}. This
         * removes the need to called {@link ProfileFile#builder()} and {@link ProfileFile.Builder#build()}.
         */
        public Builder profileFile(Consumer<ProfileFile.Builder> profileFile) {
            return profileFile(ProfileFile.builder().apply(profileFile).build());
        }

        /**
         * Define the name of the profile that should be used by this credentials provider. By default, the value in
         * {@link AwsSystemSetting#AWS_PROFILE} is used.
         */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Override the default configuration file to be used when the customer does not explicitly set
         * {@link #profileFile(ProfileFile)}. Use of this method is only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfileFileLoader(Supplier<ProfileFile> defaultProfileFileLoader) {
            this.defaultProfileFileLoader = defaultProfileFileLoader;
            return this;
        }

        /**
         * Create a {@link ProfileCredentialsProvider} using the configuration applied to this builder.
         */
        public ProfileCredentialsProvider build() {
            return new ProfileCredentialsProvider(this);
        }
    }
}
