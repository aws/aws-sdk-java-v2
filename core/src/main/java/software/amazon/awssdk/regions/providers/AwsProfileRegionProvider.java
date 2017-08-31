/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.auth.profile.Profile;
import software.amazon.awssdk.auth.profile.ProfilesFile;
import software.amazon.awssdk.profile.path.AwsProfileFileLocationProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Loads region information from the shared AWS config file. Uses the default profile unless
 * otherwise specified.
 */
@SdkInternalApi
public class AwsProfileRegionProvider extends AwsRegionProvider { // TODO: Package protected?

    private final String profileName;
    private final AwsProfileFileLocationProvider locationProvider;

    public AwsProfileRegionProvider() {
        this(AwsSystemSetting.AWS_DEFAULT_PROFILE.getStringValueOrThrow(),
             AwsProfileFileLocationProvider.DEFAULT_CONFIG_LOCATION_PROVIDER);
    }

    @SdkTestInternalApi
    AwsProfileRegionProvider(String profileName, AwsProfileFileLocationProvider locationProvider) {
        this.profileName = profileName;
        this.locationProvider = locationProvider;
    }

    @Override
    public Region getRegion() throws SdkClientException {
        return locationProvider.getLocation()
                               .map(ProfilesFile::new)
                               .flatMap(f -> f.getProfile(profileName))
                               .flatMap(Profile::region)
                               .orElse(null);
    }
}
