package software.amazon.awssdk.regions.partitionmetadata;

import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.EndpointTag;
import software.amazon.awssdk.regions.PartitionEndpointKey;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.utils.ImmutableMap;

@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public final class AwsPartitionMetadata implements PartitionMetadata {
    private static final Map<PartitionEndpointKey, String> DNS_SUFFIXES = ImmutableMap.<PartitionEndpointKey, String> builder()
                                                                                      .put(PartitionEndpointKey.builder().build(), "amazonaws.com")
                                                                                      .put(PartitionEndpointKey.builder().tags(EndpointTag.of("fips")).build(), "amazonaws.com")
                                                                                      .put(PartitionEndpointKey.builder().tags(EndpointTag.of("dualstack"), EndpointTag.of("fips")).build(), "api.aws")
                                                                                      .put(PartitionEndpointKey.builder().tags(EndpointTag.of("dualstack")).build(), "api.aws").build();

    private static final Map<PartitionEndpointKey, String> HOSTNAMES = ImmutableMap
        .<PartitionEndpointKey, String> builder()
        .put(PartitionEndpointKey.builder().build(), "{service}.{region}.{dnsSuffix}")
        .put(PartitionEndpointKey.builder().tags(EndpointTag.of("fips")).build(), "{service}-fips.{region}.{dnsSuffix}")
        .put(PartitionEndpointKey.builder().tags(EndpointTag.of("dualstack"), EndpointTag.of("fips")).build(),
             "{service}-fips.{region}.{dnsSuffix}")
        .put(PartitionEndpointKey.builder().tags(EndpointTag.of("dualstack")).build(), "{service}.{region}.{dnsSuffix}")
        .build();

    private static final String ID = "aws";

    private static final String NAME = "AWS Standard";

    private static final String REGION_REGEX = "^(us|eu|ap|sa|ca|me|af)\\-\\w+\\-\\d+$";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String regionRegex() {
        return REGION_REGEX;
    }

    @Override
    public String dnsSuffix(PartitionEndpointKey key) {
        return DNS_SUFFIXES.get(key);
    }

    @Override
    public String hostname(PartitionEndpointKey key) {
        return HOSTNAMES.get(key);
    }
}
