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

package software.amazon.awssdk.awscore.endpoint;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Validate;

/**
 * A resolver for the default value of whether the SDK should use dualstack endpoints. This checks environment variables,
 * system properties and the profile file for the relevant configuration options when {@link #isDualstackEnabled()} is invoked.
 */
@SdkProtectedApi
public class DualstackEnabledProvider {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    private DualstackEnabledProvider(Builder builder) {
        this.profileFile = Validate.paramNotNull(builder.profileFile, "profileFile");
        this.profileName = builder.profileName;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true when dualstack should be used, false when dualstack should not be used, and empty when there is no global
     * dualstack configuration.
     */
    public Optional<Boolean> isDualstackEnabled() {
        Optional<Boolean> setting = SdkSystemSetting.AWS_USE_DUALSTACK_ENDPOINT.getBooleanValue();
        if (setting.isPresent()) {
            return setting;
        }

        ProfileFile profileFile = this.profileFile.get();
        Optional<Profile> profile = profileFile
            .profile(profileName());
        return profile
                          .flatMap(p -> p.booleanProperty(ProfileProperty.USE_DUALSTACK_ENDPOINT));
    }

    private String profileName() {
        return profileName != null ? profileName : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
    }

    public static final class Builder {
        private Supplier<ProfileFile> profileFile = ProfileFile::defaultProfileFile;
        private String profileName;

        private Builder() {
        }

        public Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public DualstackEnabledProvider build() {
            return new DualstackEnabledProvider(this);
        }
    }
}
