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

/**
 * A collection of metadata about a region. This can be loaded using the {@link #of(Region)} method.
 */
@SdkPublicApi
public interface RegionMetadata {

    /**
     * The unique system ID for this region; ex: &quot;us-east-1&quot;.
     *
     * @return The unique system ID for this region.
     */
    String id();

    /**
     * Returns the domain for this region; ex: &quot;amazonaws.com&quot;.
     *
     * @return The domain for this region.
     */
    String domain();

    /**
     * Returns the metadata for this region's partition.
     */
    PartitionMetadata partition();

    /**
     * Returns the description of this region; ex: &quot;US East (N. Virginia)&quot;.
     *
     * @return The description for this region
     */
    String description();

    /**
     * Returns the region metadata pertaining to the given region.
     *
     * @param region The region to get the metadata for.
     * @return The metadata for that region.
     */
    static RegionMetadata of(Region region) {
        return MetadataLoader.regionMetadata(region);
    }
}
