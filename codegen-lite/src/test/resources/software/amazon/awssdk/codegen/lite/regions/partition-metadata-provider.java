/*
 * Copyright 2013-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.regions;

import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.partitionmetadata.AwsCnPartitionMetadata;
import software.amazon.awssdk.regions.partitionmetadata.AwsPartitionMetadata;
import software.amazon.awssdk.regions.partitionmetadata.AwsUsGovPartitionMetadata;
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedPartitionMetadataProvider implements PartitionMetadataProvider {
    private static final Map<String, PartitionMetadata> PARTITION_METADATA = ImmutableMap.<String, PartitionMetadata> builder()
            .put("aws", new AwsPartitionMetadata()).put("aws-cn", new AwsCnPartitionMetadata())
            .put("aws-us-gov", new AwsUsGovPartitionMetadata()).build();

    public PartitionMetadata partitionMetadata(String partition) {
        return PARTITION_METADATA.get(partition);
    }

    public PartitionMetadata partitionMetadata(Region region) {
        return PARTITION_METADATA.values().stream().filter(p -> region.id().matches(p.regionRegex())).findFirst()
                .orElse(new AwsPartitionMetadata());
    }
}
