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

package software.amazon.awssdk.awscore.internal.auth;


import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class Sigv4aSigningRegionSetProvider {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final Lazy<Set<String>> regionSet;

    private Sigv4aSigningRegionSetProvider(Builder builder) {
        this.profileFile = Validate.paramNotNull(builder.profileFile, "profileFile");
        this.profileName = builder.profileName;
        this.regionSet = new Lazy<>(this::regionSet);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> resolveRegionSet() {
        return this.regionSet.getValue();
    }

    /**
     * Resolves the SIGV4A signing region set configuration.
     * Returns a Set of non-empty strings if the configuration is set, or null if not  configured
     */
    private Set<String> regionSet() {
        Optional<String> setting = SdkSystemSetting.AWS_SIGV4A_SIGNING_REGION_SET.getStringValue();
        if (setting.isPresent()) {
            return parseRegionSet(setting.get());
        }
        ProfileFile file = this.profileFile.get();
        Optional<Profile> profile = file.profile(profileName());
        if (profile.isPresent()) {
            Optional<String> property = profile.get().property(ProfileProperty.SIGV4A_SIGNING_REGION_SET);
            if (property.isPresent()) {
                return parseRegionSet(property.get());
            }
        }
        return Collections.emptySet();
    }

    private Set<String> parseRegionSet(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }

        return Arrays.stream(value.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
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

        public Sigv4aSigningRegionSetProvider build() {
            return new Sigv4aSigningRegionSetProvider(this);
        }
    }
}