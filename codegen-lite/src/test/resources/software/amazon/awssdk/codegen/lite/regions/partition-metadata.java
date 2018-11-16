package software.amazon.awssdk.regions.partitionmetadata;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.PartitionMetadata;

@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public final class AwsPartitionMetadata implements PartitionMetadata {
    private static final String DNS_SUFFIX = "amazonaws.com";

    private static final String HOSTNAME = "{service}.{region}.{dnsSuffix}";

    private static final String ID = "aws";

    private static final String NAME = "AWS Standard";

    private static final String REGION_REGEX = "^(us|eu|ap|sa|ca)\\-\\w+\\-\\d+$";

    @Override
    public String dnsSuffix() {
        return DNS_SUFFIX;
    }

    @Override
    public String hostname() {
        return HOSTNAME;
    }

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
}
