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

package software.amazon.awssdk.core.internal.useragent;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.OptionalUtils;

@SdkInternalApi
public final class AppIdResolver {

    private Supplier<ProfileFile> profileFile;
    private String profileName;

    private AppIdResolver() {
    }

    public static AppIdResolver create() {
        return new AppIdResolver();
    }

    public AppIdResolver profileFile(Supplier<ProfileFile> profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    public AppIdResolver profileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    public Optional<String> resolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(),
                                          () -> fromProfileFile(profileFile, profileName));
    }

    private Optional<String> fromSystemSettings() {
        return SdkSystemSetting.AWS_SDK_UA_APP_ID.getStringValue();
    }

    private Optional<String> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        profileFile = profileFile != null ? profileFile : ProfileFile::defaultProfileFile;
        profileName = profileName != null ? profileName : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.SDK_UA_APP_ID));
    }
}
