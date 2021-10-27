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

package software.amazon.awssdk.regions.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.regions.GeneratedPartitionMetadataProvider;
import software.amazon.awssdk.regions.GeneratedRegionMetadataProvider;
import software.amazon.awssdk.regions.GeneratedServiceMetadataProvider;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.PartitionMetadataProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.regions.RegionMetadataProvider;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServiceMetadataProvider;

/**
 * Internal class for determining where to load region and service
 * metadata from. Currently only generated region metadata is supported.
 */
@SdkInternalApi
public final class MetadataLoader {

    private static final RegionMetadataProvider REGION_METADATA_PROVIDER = new GeneratedRegionMetadataProvider();

    private static final ServiceMetadataProvider SERVICE_METADATA_PROVIDER = new GeneratedServiceMetadataProvider();

    private static final PartitionMetadataProvider PARTITION_METADATA_PROVIDER = new GeneratedPartitionMetadataProvider();

    private MetadataLoader() {
    }

    public static PartitionMetadata partitionMetadata(Region region) {
        return PARTITION_METADATA_PROVIDER.partitionMetadata(region);
    }

    public static PartitionMetadata partitionMetadata(String partition) {
        return PARTITION_METADATA_PROVIDER.partitionMetadata(partition);
    }

    public static RegionMetadata regionMetadata(Region region) {
        return REGION_METADATA_PROVIDER.regionMetadata(region);
    }

    public static ServiceMetadata serviceMetadata(String service) {
        return SERVICE_METADATA_PROVIDER.serviceMetadata(service);
    }
}
