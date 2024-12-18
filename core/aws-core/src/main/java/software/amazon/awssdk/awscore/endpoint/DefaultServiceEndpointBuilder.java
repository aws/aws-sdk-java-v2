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

package software.amazon.awssdk.awscore.endpoint;

import java.net.URI;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;

/**
 * Uses service metadata and the request region to construct an endpoint for a specific service.
 *
 * @deprecated Use {@link AwsClientEndpointProvider}. This is only used by old client versions.
 */
@NotThreadSafe
@SdkProtectedApi
@Deprecated
public final class DefaultServiceEndpointBuilder {
    private final AwsClientEndpointProvider.Builder endpointResolver = AwsClientEndpointProvider.builder();
    private Region region;

    public DefaultServiceEndpointBuilder(String serviceName, String protocol) {
        endpointResolver.serviceEndpointPrefix(serviceName)
                        .defaultProtocol(protocol);
    }

    public Region getRegion() {
        return region;
    }

    public DefaultServiceEndpointBuilder withRegion(Region region) {
        this.region = region;
        endpointResolver.region(region);
        return this;
    }

    public DefaultServiceEndpointBuilder withProfileFile(Supplier<ProfileFile> profileFile) {
        endpointResolver.profileFile(profileFile);
        return this;
    }

    public DefaultServiceEndpointBuilder withProfileFile(ProfileFile profileFile) {
        endpointResolver.profileFile(() -> profileFile);
        return this;
    }

    public DefaultServiceEndpointBuilder withProfileName(String profileName) {
        endpointResolver.profileName(profileName);
        return this;
    }

    public <T> DefaultServiceEndpointBuilder putAdvancedOption(ServiceMetadataAdvancedOption<T> option, T value) {
        endpointResolver.putAdvancedOption(option, value);
        return this;
    }

    public DefaultServiceEndpointBuilder withDualstackEnabled(Boolean dualstackEnabled) {
        endpointResolver.dualstackEnabled(dualstackEnabled);
        return this;
    }

    public DefaultServiceEndpointBuilder withFipsEnabled(Boolean fipsEnabled) {
        endpointResolver.fipsEnabled(fipsEnabled);
        return this;
    }

    public URI getServiceEndpoint() {
        return endpointResolver.build().clientEndpoint();
    }
}
