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


import java.util.Objects;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * Context class that defines the required properties for creation of a Credentials provider.
 */
@SdkProtectedApi
public final class ProfileProviderCredentialsContext {

    private final Profile profile;
    private final ProfileFile profileFile;

    private ProfileProviderCredentialsContext(Profile profile, ProfileFile profileFile) {
        this.profile = profile;
        this.profileFile = profileFile;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Getter method for profile.
     * @return The profile that should be used to load the configuration necessary to create the credential provider.
     */
    public Profile profile() {
        return profile;
    }

    /**
     * Getter for profileFile.
     * @return ProfileFile that has the profile which is used to create the credential provider.
     */
    public ProfileFile profileFile() {
        return profileFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProfileProviderCredentialsContext that = (ProfileProviderCredentialsContext) o;
        return Objects.equals(profile, that.profile) && Objects.equals(profileFile, that.profileFile);
    }

    @Override
    public int hashCode() {
        int result = profile != null ? profile.hashCode() : 0;
        result = 31 * result + (profileFile != null ? profileFile.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private Profile profile;
        private ProfileFile profileFile;

        private Builder() {
        }

        /**
         * Builder interface to set profile.
         * @param profile The profile that should be used to load the configuration necessary to create the credential provider.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder profile(Profile profile) {
            this.profile = profile;
            return this;
        }

        /**
         * Builder interface to set ProfileFile.
         * @param profileFile The ProfileFile that has the profile which is used to create the credential provider. This is *
         *                    required to fetch the titles like sso-session defined in profile property* *
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public ProfileProviderCredentialsContext build() {
            return new ProfileProviderCredentialsContext(profile, profileFile);
        }
    }
}
