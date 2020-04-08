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

package software.amazon.awssdk.core.endpointdiscovery.providers;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;

@SdkProtectedApi
public class DefaultEndpointDiscoveryProviderChain extends EndpointDiscoveryProviderChain {
    public DefaultEndpointDiscoveryProviderChain() {
        this(ProfileFile::defaultProfileFile,
             ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow());
    }

    public DefaultEndpointDiscoveryProviderChain(SdkClientConfiguration clientConfiguration) {
        this(() -> clientConfiguration.option(SdkClientOption.PROFILE_FILE),
              clientConfiguration.option(SdkClientOption.PROFILE_NAME));
    }

    private DefaultEndpointDiscoveryProviderChain(Supplier<ProfileFile> profileFile, String profileName) {
        super(SystemPropertiesEndpointDiscoveryProvider.create(),
              ProfileEndpointDiscoveryProvider.create(profileFile, profileName));
    }
}
