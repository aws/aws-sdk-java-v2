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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.regions.EndpointTag;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceEndpointKey;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.utils.Lazy;
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
    private Supplier<ProfileFile> profileFile;
    private String profileName;
    private Boolean dualstackEnabled;
    private Boolean fipsEnabled;

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

    public DefaultServiceEndpointBuilder withProfileFile(Supplier<ProfileFile> profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    public DefaultServiceEndpointBuilder withProfileFile(ProfileFile profileFile) {
        this.profileFile = () -> profileFile;
        return this;
    }

    public DefaultServiceEndpointBuilder withProfileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    public DefaultServiceEndpointBuilder withDualstackEnabled(Boolean dualstackEnabled) {
        this.dualstackEnabled = dualstackEnabled;
        return this;
    }

    public DefaultServiceEndpointBuilder withFipsEnabled(Boolean fipsEnabled) {
        this.fipsEnabled = fipsEnabled;
        return this;
    }

    public URI getServiceEndpoint() {
        if (profileFile == null) {
            profileFile = new Lazy<>(ProfileFile::defaultProfileFile)::getValue;
        }

        if (profileName == null) {
            profileName = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        }

        if (dualstackEnabled == null) {
            dualstackEnabled = DualstackEnabledProvider.builder()
                                                       .profileFile(profileFile)
                                                       .profileName(profileName)
                                                       .build()
                                                       .isDualstackEnabled()
                                                       .orElse(false);
        }

        if (fipsEnabled == null) {
            fipsEnabled = FipsEnabledProvider.builder()
                                             .profileFile(profileFile)
                                             .profileName(profileName)
                                             .build()
                                             .isFipsEnabled()
                                             .orElse(false);
        }



        List<EndpointTag> endpointTags = new ArrayList<>();
        if (dualstackEnabled) {
            endpointTags.add(EndpointTag.DUALSTACK);
        }
        if (fipsEnabled) {
            endpointTags.add(EndpointTag.FIPS);
        }

        ServiceMetadata serviceMetadata = ServiceMetadata.of(serviceName)
                                                         .reconfigure(c -> c.profileFile(profileFile)
                                                                            .profileName(profileName));
        URI endpoint = addProtocolToServiceEndpoint(serviceMetadata.endpointFor(ServiceEndpointKey.builder()
                                                                                                  .region(region)
                                                                                                  .tags(endpointTags)
                                                                                                  .build()));

        if (endpoint.getHost() == null) {
            String error = "Configured region (" + region + ") and tags (" + endpointTags + ") resulted in an invalid URI: "
                           + endpoint + ". This is usually caused by an invalid region configuration.";

            List<Region> exampleRegions = serviceMetadata.regions();
            if (!exampleRegions.isEmpty()) {
                error += " Valid regions: " + exampleRegions;
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
