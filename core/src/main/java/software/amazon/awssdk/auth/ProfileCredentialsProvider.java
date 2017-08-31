/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import java.util.Optional;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.auth.profile.Profile;
import software.amazon.awssdk.auth.profile.ProfilesFile;
import software.amazon.awssdk.auth.profile.internal.path.AwsProfileFileLocationProvider;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * Credentials provider based on AWS configuration profiles. This provider vends AWSCredentials from the profile configuration
 * file for the default profile, or for a specific, named profile.
 *
 * <p>AWS credential profiles allow you to share multiple sets of AWS security credentials between different tools like the AWS
 * SDK for Java and the AWS CLI.</p>
 *
 * <p>See http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html</p>
 *
 * @see ProfilesFile
 */
public final class ProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
    private final AwsCredentialsProvider profileCredentialsProvider;
    private final RuntimeException loadException;

    private final ProfilesFile profilesFile;
    private final String profileName;

    /**
     * Create a {@link ProfileCredentialsProvider} using the default profile and configuration file. Use {@link #builder()} for
     * defining a custom {@link ProfileCredentialsProvider}.
     */
    public ProfileCredentialsProvider() {
        this(new Builder());
    }

    /**
     * @see #builder()
     */
    private ProfileCredentialsProvider(Builder builder) {
        this.profileName = builder.profileName != null ? builder.profileName
                                                         : AwsSystemSetting.AWS_DEFAULT_PROFILE.getStringValueOrThrow();

        // Load the profiles file
        Optional<ProfilesFile> profilesFile = Optional.ofNullable(builder.profilesFile);

        if (!profilesFile.isPresent()) {
            Validate.notNull(builder.profileFileLocationProvider, "Profile file location provider must not be null.");
            profilesFile = builder.profileFileLocationProvider.getLocation().map(ProfilesFile::new);
        }

        this.profilesFile = profilesFile.orElse(null);

        // Load the profile and credentials provider
        Optional<Profile> profile = profilesFile.flatMap(f -> f.profile(profileName));
        this.profileCredentialsProvider = profile.flatMap(Profile::credentialsProvider).orElse(null);

        // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception will
        // only be raised on calls to getCredentials. We don't want to raise an exception here because it may be expected (eg. in
        // the default credential chain).
        if (profileCredentialsProvider == null) {
            String loadError = "Unable to load credentials from profiles file: ";

            if (!profilesFile.isPresent()) {
                loadError += "Credentials file could not be located.";
            } else if (!profile.isPresent()) {
                loadError += String.format("Credentials file '%s' did not contain a profile named '%s'.",
                                           profilesFile.get(), profileName);
            } else {
                loadError += String.format("In credentials file '%s', profile '%s' did not have any credentials configured.",
                                           profilesFile.get(), profileName);

            }
            this.loadException = new SdkClientException(loadError);
        } else {
            this.loadException = null;
        }
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
        return "ProfileCredentialsProvider(" + profilesFile + ", " + profileName + ")";
    }

    @Override
    public void close() {
        // The delegate credentials provider may be closeable (eg. if it's an STS credentials provider). In this case, we should
        // clean it up when this credentials provider is closed.
        if (profileCredentialsProvider instanceof AutoCloseable) {
            AutoCloseable closeableCredentialsProvider = (AutoCloseable) profileCredentialsProvider;
            IoUtils.closeQuietly(closeableCredentialsProvider, null);
        }
    }

    /**
     * A builder for creating a custom {@link ProfileCredentialsProvider}.
     */
    public static final class Builder {
        private ProfilesFile profilesFile;
        private String profileName;

        private AwsProfileFileLocationProvider profileFileLocationProvider =
                AwsProfileFileLocationProvider.DEFAULT_CREDENTIALS_LOCATION_PROVIDER;

        private Builder() {}

        /**
         * Define the profile configuration file that should be used by this credentials provider.
         *
         * By default, the result of {@link AwsProfileFileLocationProvider#DEFAULT_CREDENTIALS_LOCATION_PROVIDER} is used.
         */
        public Builder profilesConfigFile(ProfilesFile profilesFile) {
            this.profilesFile = profilesFile;
            return this;
        }

        /**
         * Define the name of the profile that should be used by this credentials provider.
         *
         * By default, the value in {@link AwsSystemSetting#AWS_DEFAULT_PROFILE} is used.
         */
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Override the default configuration file locator to be used when the customer does not explicitly set
         * {@link #profilesConfigFile(ProfilesFile)}. This is only useful for testing the default behavior.
         */
        @SdkTestInternalApi
        Builder defaultProfilesConfigFileLocator(AwsProfileFileLocationProvider profileFileLocationProvider) {
            this.profileFileLocationProvider = profileFileLocationProvider;
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
