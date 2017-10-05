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

import java.io.File;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.auth.profile.internal.AllProfiles;
import software.amazon.awssdk.auth.profile.internal.BasicProfile;
import software.amazon.awssdk.auth.profile.internal.BasicProfileConfigLoader;
import software.amazon.awssdk.profile.path.AwsProfileFileLocationProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Loads region information from the shared AWS config file. Uses the default profile unless
 * otherwise specified.
 */
public class AwsProfileRegionProvider extends AwsRegionProvider {

    private final String profileName;
    private final AwsProfileFileLocationProvider locationProvider;
    private final BasicProfileConfigLoader profileConfigLoader;

    public AwsProfileRegionProvider() {
        this(AwsSystemSetting.AWS_DEFAULT_PROFILE.getStringValueOrThrow());
    }

    public AwsProfileRegionProvider(String profileName) {
        this(profileName, AwsProfileFileLocationProvider.DEFAULT_CONFIG_LOCATION_PROVIDER,
             BasicProfileConfigLoader.INSTANCE);
    }

    @SdkTestInternalApi
    AwsProfileRegionProvider(String profileName, AwsProfileFileLocationProvider locationProvider,
                             BasicProfileConfigLoader configLoader) {
        this.profileName = profileName;
        this.locationProvider = locationProvider;
        this.profileConfigLoader = configLoader;
    }

    @Override
    public Region getRegion() throws SdkClientException {
        File configFile = locationProvider.getLocation();
        if (configFile != null && configFile.exists()) {
            BasicProfile profile = loadProfile(configFile);
            if (profile != null && !StringUtils.isEmpty(profile.getRegion())) {
                return Region.of(profile.getRegion());
            }
        }
        return null;
    }

    private BasicProfile loadProfile(File configFile) {
        final AllProfiles allProfiles = profileConfigLoader.loadProfiles(configFile);
        return allProfiles.getProfile(profileName);
    }

}
