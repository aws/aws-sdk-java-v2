package software.amazon.awssdk.regions.regionmetadata;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.RegionMetadata;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class UsEast1 implements RegionMetadata {
    private static final String NAME = "us-east-1";

    private static final String DOMAIN = "amazonaws.com";

    private static final String DESCRIPTION = "US East (N. Virginia)";

    private static final String PARTITION = "aws";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String domain() {
        return DOMAIN;
    }

    @Override
    public String description() {
        return DESCRIPTION;
    }

    @Override
    public String partition() {
        return PARTITION;
    }
}
