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

package software.amazon.awssdk.regions.internal;

import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.regions.internal.model.Partition;
import software.amazon.awssdk.utils.Validate;

/**
 * A region implementation backed by the partition.
 */
public class PartitionRegionMetadata implements RegionMetadata {

    /**
     * partition where the region is present.
     */
    private final Partition partition;

    /**
     * the name of the region.
     */
    private final String region;

    public PartitionRegionMetadata(String region, Partition p) {
        this.partition = Validate.notNull(p, "partition");
        this.region = Validate.notNull(region, "region");
    }

    @Override
    public String getName() {
        return region;
    }

    @Override
    public String getDomain() {
        return partition.getDnsSuffix();
    }

    @Override
    public String getPartition() {
        return partition.getPartition();
    }

}
