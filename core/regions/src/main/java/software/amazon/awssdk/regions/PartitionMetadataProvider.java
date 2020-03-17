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

package software.amazon.awssdk.regions;

import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public interface PartitionMetadataProvider {

    /**
     * Returns the partition metadata for a given partition.
     *
     * @param partition The partition to find partition metadata for.
     * @return {@link PartitionMetadata} for the given partition
     */
    PartitionMetadata partitionMetadata(String partition);

    /**
     * Returns the partition metadata for a given region.
     *
     * @param region The region to find partition metadata for.
     * @return {@link PartitionMetadata} for the given region.
     */
    PartitionMetadata partitionMetadata(Region region);
}
