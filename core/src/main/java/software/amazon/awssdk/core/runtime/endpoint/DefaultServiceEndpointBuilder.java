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

package software.amazon.awssdk.core.runtime.endpoint;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.regions.ServiceMetadata;

/**
 * Uses service metadata and the request region to construct an endpoint for a specific service
 */
@NotThreadSafe
@SdkProtectedApi
public final class DefaultServiceEndpointBuilder {

    private final String serviceName;
    private final String protocol;
    private Region region;

    public DefaultServiceEndpointBuilder(String serviceName, String protocol) {
        this.serviceName = serviceName;
        this.protocol = protocol;
    }

    public DefaultServiceEndpointBuilder withRegion(Region region) {
        if (region == null) {
            throw new IllegalArgumentException("Region cannot be null");
        }
        this.region = region;
        return this;
    }

    public URI getServiceEndpoint() {
        ServiceMetadata serviceMetadata = ServiceMetadata.of(serviceName);
        return withProtocol(serviceMetadata.endpointFor(region));
    }

    private URI withProtocol(URI endpointWithoutProtocol) throws IllegalArgumentException {
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
