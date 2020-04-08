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

package software.amazon.awssdk.regions.providers;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;

/**
 * Loads region information from the {@link ProfileFile#defaultProfileFile()} using the default profile name.
 */
@SdkProtectedApi
public final class AwsProfileRegionProvider implements AwsRegionProvider {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;

    public AwsProfileRegionProvider() {
        this(null, null);
    }

    public AwsProfileRegionProvider(Supplier<ProfileFile> profileFile, String profileName) {
        this.profileFile = profileFile != null ? profileFile : ProfileFile::defaultProfileFile;
        this.profileName = profileName != null ? profileName : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
    }

    @Override
    public Region getRegion() {
        return profileFile.get()
                          .profile(profileName)
                          .map(p -> p.properties().get(ProfileProperty.REGION))
                          .map(Region::of)
                          .orElseThrow(() -> SdkClientException.builder()
                                                               .message("No region provided in profile: " + profileName)
                                                               .build());
    }
}

