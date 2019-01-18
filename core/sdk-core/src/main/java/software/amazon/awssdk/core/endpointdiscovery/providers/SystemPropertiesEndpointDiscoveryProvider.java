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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.ToString;

/**
 * {@link EndpointDiscoveryProvider} implementation that loads endpoint discovery from the AWS_ENABLE_ENDPOINT_DISCOVERY
 * system property or environment variable.
 */
@SdkPublicApi
public final class SystemPropertiesEndpointDiscoveryProvider implements EndpointDiscoveryProvider {

    private SystemPropertiesEndpointDiscoveryProvider() {
    }

    public static SystemPropertiesEndpointDiscoveryProvider create() {
        return new SystemPropertiesEndpointDiscoveryProvider();
    }

    @Override
    public boolean resolveEndpointDiscovery() {
        return SdkSystemSetting
            .AWS_ENDPOINT_DISCOVERY_ENABLED.getBooleanValue()
                                           .orElseThrow(
                                               () -> SdkClientException.builder()
                                                                       .message("No endpoint discovery setting set.")
                                                                       .build());
    }

    @Override
    public String toString() {
        return ToString.create("SystemPropertiesEndpointDiscoveryProvider");
    }
}
