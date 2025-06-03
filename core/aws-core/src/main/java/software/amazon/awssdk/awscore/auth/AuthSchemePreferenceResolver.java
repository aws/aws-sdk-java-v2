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

package software.amazon.awssdk.awscore.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * A resolver for the default value of auth scheme preference. This checks environment variables,
 * system properties and the profile file for the relevant configuration options when
 * {@link #resolveAuthSchemePreference()} is invoked.
 */
@SdkProtectedApi
public final class AuthSchemePreferenceResolver {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    private AuthSchemePreferenceResolver(Builder builder) {
        this.profileFile = Validate.getOrDefault(builder.profileFile, () -> ProfileFile::defaultProfileFile);
        this.profileName = Validate.getOrDefault(builder.profileName,
                                                 ProfileFileSystemSetting.AWS_PROFILE::getStringValueOrThrow);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Resolve the auth scheme preference based on the following order of precedence:
     * 1. System settings (jvm and then environment).
     * 2. Profile file
     *
     * @return The resolved, ordered list of auth scheme preferences or an empty list if no values are found.
     */
    public List<String> resolveAuthSchemePreference() {
        List<String> systemSettingList = fromSystemSetting();
        if (systemSettingList != null && !systemSettingList.isEmpty()) {
            return systemSettingList;
        }

        List<String> profileFilePrefList = fromProfileFile();
        if (!CollectionUtils.isNullOrEmpty(profileFilePrefList)) {
            return profileFilePrefList;
        }

        return Collections.emptyList();
    }

    private List<String> fromSystemSetting() {
        Optional<String> value = SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.getStringValue();
        if (value.isPresent()) {
            return parseAuthSchemeList(value.get());
        }
        return Collections.emptyList();
    }

    private List<String> fromProfileFile() {
        ProfileFile profileFile = this.profileFile.get();

        Optional<Profile> profile = profileFile.profile(profileName);

        String unformattedAuthSchemePreferenceList =
            profile
                .flatMap(p -> p.property(ProfileProperty.AUTH_SCHEME_PREFERENCE))
                .orElse(null);

        return unformattedAuthSchemePreferenceList != null
               ? parseAuthSchemeList(unformattedAuthSchemePreferenceList)
               : Collections.emptyList();
    }

    public static final class Builder {
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        public AuthSchemePreferenceResolver.Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public AuthSchemePreferenceResolver.Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public AuthSchemePreferenceResolver build() {
            return new AuthSchemePreferenceResolver(this);
        }
    }

    private static List<String> parseAuthSchemeList(String unformattedList) {
        if (StringUtils.isEmpty(unformattedList)) {
            return Collections.emptyList();
        }

        return Arrays.asList(unformattedList.replaceAll("\\s+", "").split(","));
    }
}
