package software.amazon.awssdk.regions;

import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.partitionmetadata.AwsCnPartitionMetadata;
import software.amazon.awssdk.regions.partitionmetadata.AwsIsoBPartitionMetadata;
import software.amazon.awssdk.regions.partitionmetadata.AwsIsoPartitionMetadata;
import software.amazon.awssdk.regions.partitionmetadata.AwsPartitionMetadata;
import software.amazon.awssdk.regions.partitionmetadata.AwsUsGovPartitionMetadata;
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedPartitionMetadataProvider implements PartitionMetadataProvider {
    private static final Map<String, PartitionMetadata> PARTITION_METADATA = ImmutableMap.<String, PartitionMetadata> builder()
                                                                                         .put("aws", new AwsPartitionMetadata()).put("aws-cn", new AwsCnPartitionMetadata())
                                                                                         .put("aws-us-gov", new AwsUsGovPartitionMetadata()).put("aws-iso", new AwsIsoPartitionMetadata())
                                                                                         .put("aws-iso-b", new AwsIsoBPartitionMetadata()).build();

    public PartitionMetadata partitionMetadata(String partition) {
        return PARTITION_METADATA.get(partition);
    }

    public PartitionMetadata partitionMetadata(Region region) {
        return PARTITION_METADATA.values().stream().filter(p -> region.id().matches(p.regionRegex())).findFirst()
                                 .orElse(new AwsPartitionMetadata());
    }
}
