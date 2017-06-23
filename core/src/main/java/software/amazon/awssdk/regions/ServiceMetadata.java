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

package software.amazon.awssdk.regions;

import java.net.URI;
import java.util.List;

/**
 * Metadata about a service, like S3, DynamoDB, etc.
 *
 * <p>This is useful for building meta-functionality around AWS services. For example, UIs that list the available regions for a
 * service would use the {@link #regions()} method for a service.</p>
 *
 * <p>This is usually created by calling the {@code serviceMetadata} method on the service client's interface, but can also be
 * created by calling the {@link #of(String)} method and providing the service's unique endpoint prefix.</p>
 */
public interface ServiceMetadata {
    /**
     * Retrieve the AWS endpoint that should be used for this service in the provided region.
     *
     * @param region The region that should be used to load the service endpoint.
     * @return The region-specific endpoint for this service.
     */
    URI endpointFor(Region region);

    /**
     * Retrieve the region that should be used for message signing when communicating with this service in the provided region.
     * For most services, this will match the provided region, but it may differ for unusual services or when using a region that
     * does not correspond to a physical location, like {@link Region#AWS_GLOBAL}.
     *
     * @param region The region from which the signing region should be derived.
     * @return The region that should be used for signing messages when communicating with this service in the requested region.
     */
    Region signingRegion(Region region);

    /**
     * Retrieve the list of regions this service is currently available in.
     *
     * @return The list of regions this service is currently available in.
     */
    List<Region> regions();

    /**
     * Load the service metadata for the provided service endpoint prefix. This should only be used when you do not wish to have
     * a dependency on the service for which you are retrieving the metadata. When you have a dependency on the service client,
     * the metadata should instead be loaded using the service client's {@code serviceMetadata()} method.
     *
     * @param serviceEndpointPrefix The service-specific endpoint prefix of the service about which you wish to load metadata.
     * @return The service metadata for the requested service.
     */
    static ServiceMetadata of(String serviceEndpointPrefix) {
        return RegionMetadataLoader.getServiceMetadata(serviceEndpointPrefix);
    }
}
