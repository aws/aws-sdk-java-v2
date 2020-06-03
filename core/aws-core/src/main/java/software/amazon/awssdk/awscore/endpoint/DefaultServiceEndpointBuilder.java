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
import java.net.URISyntaxException;
import java.util.List;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.utils.Validate;

/**
 * Uses service metadata and the request region to construct an endpoint for a specific service
 */
@NotThreadSafe
@SdkProtectedApi
// TODO We may not need this anymore, we should default to AWS partition when resolving
// a region we don't know about yet.
public final class DefaultServiceEndpointBuilder {
    private final String serviceName;
    private final String protocol;

    private Region region;
    private ProfileFile profileFile;
    private String profileName;

    public DefaultServiceEndpointBuilder(String serviceName, String protocol) {
        this.serviceName = Validate.paramNotNull(serviceName, "serviceName");
        this.protocol = Validate.paramNotNull(protocol, "protocol");
    }

    public DefaultServiceEndpointBuilder withRegion(Region region) {
        if (region == null) {
            throw new IllegalArgumentException("Region cannot be null");
        }
        this.region = region;
        return this;
    }

    public DefaultServiceEndpointBuilder withProfileFile(ProfileFile profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    public DefaultServiceEndpointBuilder withProfileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    public URI getServiceEndpoint() {
        ServiceMetadata serviceMetadata = ServiceMetadata.of(serviceName)
                                                         .reconfigure(c -> c.profileFile(() -> profileFile)
                                                                            .profileName(profileName));
        URI endpoint = addProtocolToServiceEndpoint(serviceMetadata.endpointFor(region));

        if (endpoint.getHost() == null) {
            String error = "Configured region (" + region + ") resulted in an invalid URI: " + endpoint;

            List<Region> exampleRegions = serviceMetadata.regions();
            if (!exampleRegions.isEmpty()) {
                error += " Valid region examples: " + exampleRegions;
            }

            throw SdkClientException.create(error);
        }

        return endpoint;
    }

    private URI addProtocolToServiceEndpoint(URI endpointWithoutProtocol) throws IllegalArgumentException {
        try {
            return new URI(protocol + "://" + endpointWithoutProtocol);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Region getRegion() {
        return region;
    }
}
