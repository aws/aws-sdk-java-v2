package software.amazon.awssdk.regions.servicemetadata;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServicePartitionMetadata;
import software.amazon.awssdk.regions.internal.DefaultServicePartitionMetadata;
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class S3ServiceMetadata implements ServiceMetadata {
    private static final String ENDPOINT_PREFIX = "s3";

    private static final Map<String, String> PARTITION_OVERRIDDEN_ENDPOINTS = ImmutableMap.<String, String> builder().build();

    private static final Map<String, String> REGION_OVERRIDDEN_ENDPOINTS = ImmutableMap.<String, String> builder()
        .put("ap-northeast-1", "s3.ap-northeast-1.amazonaws.com").put("ap-southeast-1", "s3.ap-southeast-1.amazonaws.com")
        .put("ap-southeast-2", "s3.ap-southeast-2.amazonaws.com").put("eu-west-1", "s3.eu-west-1.amazonaws.com")
        .put("sa-east-1", "s3.sa-east-1.amazonaws.com").put("us-east-1", "s3.amazonaws.com")
        .put("us-west-1", "s3.us-west-1.amazonaws.com").put("us-west-2", "s3.us-west-2.amazonaws.com")
        .put("fips-us-gov-west-1", "s3-fips-us-gov-west-1.amazonaws.com")
        .put("us-gov-west-1", "s3.us-gov-west-1.amazonaws.com").build();

    private static final List<Region> REGIONS = Collections.unmodifiableList(
        Arrays.asList(Region.of("ap-northeast-1"),
                      Region.of("ap-northeast-2"), Region.of("ap-northeast-3"), Region.of("ap-south-1"), Region.of("ap-southeast-1"),
                      Region.of("ap-southeast-2"), Region.of("ca-central-1"), Region.of("eu-central-1"), Region.of("eu-west-1"),
                      Region.of("eu-west-2"), Region.of("eu-west-3"), Region.of("sa-east-1"), Region.of("us-east-1"),
                      Region.of("us-east-2"), Region.of("us-west-1"), Region.of("us-west-2"), Region.of("cn-north-1"),
                      Region.of("cn-northwest-1"), Region.of("fips-us-gov-west-1"), Region.of("us-gov-west-1")));

    private static final Map<String, String> SIGNING_REGION_OVERRIDES = ImmutableMap.<String, String> builder()
        .put("fips-us-gov-west-1", "us-gov-west-1").build();

    private static final List<ServicePartitionMetadata> PARTITIONS = Collections.unmodifiableList(Arrays.asList(
        new DefaultServicePartitionMetadata("aws", null), new DefaultServicePartitionMetadata("aws-cn", null),
        new DefaultServicePartitionMetadata("aws-us-gov", null)));

    @Override
    public List<Region> regions() {
        return REGIONS;
    }

    @Override
    public URI endpointFor(Region region) {
        return URI.create(REGION_OVERRIDDEN_ENDPOINTS.containsKey(region.id()) ? REGION_OVERRIDDEN_ENDPOINTS.get(region.id())
                                                                               : computeEndpoint(ENDPOINT_PREFIX, PARTITION_OVERRIDDEN_ENDPOINTS, region));
    }

    @Override
    public Region signingRegion(Region region) {
        return Region.of(SIGNING_REGION_OVERRIDES.getOrDefault(region.id(), region.id()));
    }

    @Override
    public List<ServicePartitionMetadata> servicePartitions() {
        return PARTITIONS;
    }
}
