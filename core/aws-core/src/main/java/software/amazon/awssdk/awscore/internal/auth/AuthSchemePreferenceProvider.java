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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class AuthSchemePreferenceProvider {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    private AuthSchemePreferenceProvider(Builder builder) {
        this.profileFile = Validate.paramNotNull(builder.profileFile, "profileFile");
        this.profileName = builder.profileName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> resolveAuthSchemePreference() {
        List<String> systemSettingList = fromSystemSetting();
        if (systemSettingList != null && !systemSettingList.isEmpty()) {
            return systemSettingList;
        }

        List<String> profileFilePrefList = fromProfileFile();
        if (profileFilePrefList != null && !profileFilePrefList.isEmpty()) {
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
        private Supplier<ProfileFile> profileFile = ProfileFile::defaultProfileFile;
        private String profileName;

        public AuthSchemePreferenceProvider.Builder profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        public AuthSchemePreferenceProvider.Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public AuthSchemePreferenceProvider build() {
            return new AuthSchemePreferenceProvider(this);
        }
    }

    private static List<String> parseAuthSchemeList(String unformattedList) {
        if (unformattedList == null) {
            return Collections.emptyList();
        }

        unformattedList = unformattedList.replaceAll("\\s+", "");
        String[] splitByTabs = unformattedList.split("\t");
        String finalFormat = String.join("", splitByTabs);
        return Arrays.asList(finalFormat.split(","));
    }
}
