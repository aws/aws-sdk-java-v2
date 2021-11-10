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
import software.amazon.awssdk.regions.internal.MetadataLoader;

/**
 * Metadata about a partition such as aws or aws-cn.
 *
 * <p>This is useful for building meta-functionality around AWS services. Partition metadata helps to provide
 * data about regions which may not yet be in the endpoints.json file but have a specific prefix.</p>
 */
@SdkPublicApi
public interface PartitionMetadata {
    /**
     * Returns the DNS suffix, such as amazonaws.com for this partition. This is the DNS suffix with no
     * {@link EndpointTag}s.
     *
     * @return The DNS suffix for this partition with no endpoint tags.
     * @see #dnsSuffix(PartitionEndpointKey)
     */
    default String dnsSuffix() {
        return dnsSuffix(PartitionEndpointKey.builder().build());
    }

    /**
     * Returns the DNS suffix, such as amazonaws.com for this partition. This returns the DNS suffix associated with the tags in
     * the provided {@link PartitionEndpointKey}.
     *
     * @return The DNS suffix for this partition with the endpoint tags specified in the endpoint key, or null if one is not
     * known.
     */
    default String dnsSuffix(PartitionEndpointKey key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the hostname pattern, such as {service}.{region}.{dnsSuffix} for this partition. This is the hostname pattern
     * with no {@link EndpointTag}s.
     *
     * @return The hostname pattern for this partition with no endpoint tags.
     * @see #hostname(PartitionEndpointKey)
     */
    default String hostname() {
        return hostname(PartitionEndpointKey.builder().build());
    }

    /**
     * Returns the hostname pattern, such as {service}.{region}.{dnsSuffix} for this partition. This returns the hostname
     * associated with the tags in the provided {@link PartitionEndpointKey}.
     *
     * @return The hostname pattern for this partition with the endpoint tags specified in the endpoint key, or null if one is
     * not known.
     */
    default String hostname(PartitionEndpointKey key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the identifier for this partition, such as aws.
     *
     * @return The identifier for this partition.
     */
    String id();

    /**
     * Returns the partition name for this partition, such as AWS Standard
     *
     * @return The name of this partition
     */
    String name();

    /**
     * Returns the region regex used for pattern matching for this partition.
     *
     * @return The region regex of this partition.
     */
    String regionRegex();

    /**
     * Retrieves the partition metadata for a given partition.
     *
     * @param partition The partition to get metadata for.
     *
     * @return {@link PartitionMetadata} for the given partition.
     */
    static PartitionMetadata of(String partition) {
        return MetadataLoader.partitionMetadata(partition);
    }

    /**
     * Retrieves the partition metadata for a given region.
     *
     * @param region The region to get the partition metadata for.
     *
     * @return {@link PartitionMetadata} for the given region.
     */
    static PartitionMetadata of(Region region) {
        return MetadataLoader.partitionMetadata(region);
    }
}
