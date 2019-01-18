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

package software.amazon.awssdk.core.endpointdiscovery.providers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public class ProfileEndpointDiscoveryProvider implements EndpointDiscoveryProvider {

    private final String profileName = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();

    private ProfileEndpointDiscoveryProvider() {}

    public static ProfileEndpointDiscoveryProvider create() {
        return new ProfileEndpointDiscoveryProvider();
    }

    @Override
    public boolean resolveEndpointDiscovery() {
        return ProfileFile.defaultProfileFile()
                          .profile(profileName)
                          .map(p -> p.properties().get(ProfileProperty.ENDPOINT_DISCOVERY_ENABLED))
                          .map(Boolean::parseBoolean)
                          .orElseThrow(() -> SdkClientException.builder()
                                                               .message("No endpoint discovery setting provided in profile: " +
                                                                        profileName)
                                                               .build());
    }

    @Override
    public String toString() {
        return ToString.create("ProfileEndpointDiscoveryProvider");
    }
}
