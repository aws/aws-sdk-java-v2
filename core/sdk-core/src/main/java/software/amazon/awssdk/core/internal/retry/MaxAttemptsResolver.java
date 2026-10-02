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

package software.amazon.awssdk.core.internal.retry;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.OptionalUtils;

/**
 * Resolves the retry max attempts from {@link SdkSystemSetting#AWS_MAX_ATTEMPTS} and {@link ProfileProperty#MAX_ATTEMPTS}.
 */
@SdkInternalApi
public class MaxAttemptsResolver {
    private Supplier<ProfileFile> profileFile;
    private String profileName;

    /**
     * Configure the profile file that should be used when determining the max attempts. The supplier is only consulted
     * if a higher-priority determinant (e.g. environment variables) does not find the setting.
     */
    public MaxAttemptsResolver profileFile(Supplier<ProfileFile> profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    /**
     * Configure the profile file name should be used when determining the max attempts.
     */
    public MaxAttemptsResolver profileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    /**
     * Resolve the max attempts based on the configured values. If not configured, returns {@code null}.
     */
    public Integer resolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(), () -> fromProfileFile(profileFile, profileName))
                            .orElse(null);
    }


    private static Optional<Integer> fromSystemSettings() {
        return SdkSystemSetting.AWS_MAX_ATTEMPTS.getIntegerValue();
    }

    private static Optional<Integer> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        profileFile = profileFile != null ? profileFile : ProfileFile::defaultProfileFile;
        profileName = profileName != null ? profileName : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.MAX_ATTEMPTS))
                          .map(Integer::parseInt);
    }
}
