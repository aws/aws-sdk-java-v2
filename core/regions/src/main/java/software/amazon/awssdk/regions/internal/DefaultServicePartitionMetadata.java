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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServicePartitionMetadata;

@SdkInternalApi
public class DefaultServicePartitionMetadata implements ServicePartitionMetadata {
    private final String partition;
    private final Region globalRegionForPartition;

    public DefaultServicePartitionMetadata(String partition,
                                           Region globalRegionForPartition) {
        this.partition = partition;
        this.globalRegionForPartition = globalRegionForPartition;
    }

    @Override
    public PartitionMetadata partition() {
        return PartitionMetadata.of(partition);
    }

    @Override
    public Optional<Region> globalRegion() {
        return Optional.ofNullable(globalRegionForPartition);
    }
}
