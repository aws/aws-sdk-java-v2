/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.regions.providers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.profile.Profile;
import software.amazon.awssdk.auth.profile.ProfileFile;
import software.amazon.awssdk.core.AwsSystemSetting;
import software.amazon.awssdk.core.regions.Region;

/**
 * Loads region information from the {@link ProfileFile#defaultProfileFile()} using the default profile name.
 */
@SdkInternalApi
class AwsProfileRegionProvider implements AwsRegionProvider {

    private final String profileName = AwsSystemSetting.AWS_PROFILE.getStringValueOrThrow();

    @Override
    public Region getRegion() {
        return ProfileFile.defaultProfileFile()
                          .profile(profileName)
                          .flatMap(Profile::region)
                          .orElse(null);
    }
}

