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

package software.amazon.awssdk.services.s3.internal.usearnregion;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Loads useArnRegion configuration from the {@link ProfileFile#defaultProfileFile()} using the default profile name.
 */
@SdkInternalApi
public final class ProfileUseArnRegionProvider implements UseArnRegionProvider {
    /**
     * Property name for specifying whether or not use arn region should be enabled.
     */
    private static final String AWS_USE_ARN_REGION = "s3_use_arn_region";

    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    private ProfileUseArnRegionProvider(Supplier<ProfileFile> profileFile, String profileName) {
        this.profileFile = profileFile;
        this.profileName = profileName;
    }

    public static ProfileUseArnRegionProvider create() {
        return new ProfileUseArnRegionProvider(ProfileFile::defaultProfileFile,
                                               ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow());
    }

    public static ProfileUseArnRegionProvider create(ProfileFile profileFile, String profileName) {
        return new ProfileUseArnRegionProvider(() -> profileFile, profileName);
    }

    @Override
    public Optional<Boolean> resolveUseArnRegion() {
        return profileFile.get()
                          .profile(profileName)
                          .map(p -> p.properties().get(AWS_USE_ARN_REGION))
                          .map(StringUtils::safeStringToBoolean);
    }
}

