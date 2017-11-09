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

package software.amazon.awssdk.core.internal.region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.region.model.Partition;
import software.amazon.awssdk.core.regions.PartitionServiceMetadata;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.core.regions.RegionMetadata;
import software.amazon.awssdk.core.regions.ServiceMetadata;
import software.amazon.awssdk.core.regions.ServiceMetadataProvider;
import software.amazon.awssdk.utils.Validate;

/**
 * Region metadata provider based on partitions.
 */
@SdkInternalApi
public final class PartitionMetadataProvider implements RegionMetadataProvider, ServiceMetadataProvider {

    private static final String DEFAULT_PARTITION = "aws";

    private final Map<String, Partition> partitionMap = new HashMap<>();

    private final Map<String, RegionMetadata> regionMetadata = new ConcurrentHashMap<>();

    private final Map<String, ServiceMetadata> serviceMetadata = new ConcurrentHashMap<>();

    public PartitionMetadataProvider(List<Partition> partitions) {
        Validate.notNull(partitions, "partitions");

        partitions.forEach(p -> partitionMap.put(p.getPartition(), p));
    }

    @Override
    public RegionMetadata getRegionMetadata(Region region) {

        if (region == null) {
            return null;
        }

        final RegionMetadata regionFromCache = getRegionFromCache(region);

        return regionFromCache != null ? regionFromCache : createNewRegion(region);
    }

    @Override
    public ServiceMetadata getServiceMetadata(String serviceEndpointPrefix) {

        if (serviceEndpointPrefix == null) {
            return null;
        }

        final ServiceMetadata serviceMetadataFromCache = getServiceMetadataFromCache(serviceEndpointPrefix);

        return serviceMetadataFromCache != null ? serviceMetadataFromCache : createNewServiceMetadata(serviceEndpointPrefix);
    }

    private RegionMetadata createNewRegion(Region region) {
        return partitionMap.values()
                .stream()
                .filter(p -> p.hasRegion(region.value()))
                .map(p -> cacheRegion(region, p))
                .findFirst()
                .orElseGet(() -> cacheRegion(region, partitionMap.get(DEFAULT_PARTITION)));
    }

    private RegionMetadata getRegionFromCache(Region region) {
        return regionMetadata.get(region.value());
    }

    private RegionMetadata cacheRegion(Region region, Partition p) {
        return regionMetadata.computeIfAbsent(region.value(), ignored -> new PartitionRegionMetadata(region.value(), p));
    }

    private ServiceMetadata createNewServiceMetadata(String serviceEndpointPrefix) {
        return serviceMetadata.computeIfAbsent(
                serviceEndpointPrefix, ignored -> new PartitionServiceMetadata(serviceEndpointPrefix, partitionMap));
    }

    private ServiceMetadata getServiceMetadataFromCache(String serviceEndpointPrefix) {
        return serviceMetadata.get(serviceEndpointPrefix);
    }
}
