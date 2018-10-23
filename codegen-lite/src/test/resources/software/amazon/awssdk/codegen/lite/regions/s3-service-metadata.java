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
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class S3ServiceMetadata implements ServiceMetadata {
    private static final String ENDPOINT_PREFIX = "s3";

    private static final Map<String, String> REGION_OVERRIDDEN_ENDPOINTS = ImmutableMap.<String, String> builder()
            .put("ap-northeast-1", "s3.ap-northeast-1.amazonaws.com").put("ap-southeast-1", "s3.ap-southeast-1.amazonaws.com")
            .put("ap-southeast-2", "s3.ap-southeast-2.amazonaws.com").put("eu-west-1", "s3.eu-west-1.amazonaws.com")
            .put("s3-external-1", "s3-external-1.amazonaws.com").put("sa-east-1", "s3.sa-east-1.amazonaws.com")
            .put("us-east-1", "s3.amazonaws.com").put("us-west-1", "s3.us-west-1.amazonaws.com")
            .put("us-west-2", "s3.us-west-2.amazonaws.com").put("fips-us-gov-west-1", "s3-fips-us-gov-west-1.amazonaws.com")
            .put("us-gov-west-1", "s3.us-gov-west-1.amazonaws.com").build();

    private static final List<Region> REGIONS = Collections
            .unmodifiableList(Arrays.asList(Region.AP_NORTHEAST_1, Region.AP_NORTHEAST_2, Region.AP_SOUTH_1,
                    Region.AP_SOUTHEAST_1, Region.AP_SOUTHEAST_2, Region.CA_CENTRAL_1, Region.EU_CENTRAL_1, Region.EU_WEST_1,
                    Region.EU_WEST_2, Region.EU_WEST_3, Region.SA_EAST_1, Region.US_EAST_1, Region.US_EAST_2, Region.US_WEST_1,
                    Region.US_WEST_2, Region.CN_NORTH_1, Region.CN_NORTHWEST_1, Region.US_GOV_WEST_1));

    private static final Map<String, String> SIGNING_REGION_OVERRIDES = ImmutableMap.<String, String> builder()
            .put("s3-external-1", "us-east-1").put("fips-us-gov-west-1", "us-gov-west-1").build();

    @Override
    public List<Region> regions() {
        return REGIONS;
    }

    @Override
    public URI endpointFor(Region region) {
        return URI.create(REGION_OVERRIDDEN_ENDPOINTS.containsKey(region.id()) ? REGION_OVERRIDDEN_ENDPOINTS.get(region.id())
                : computeEndpoint(ENDPOINT_PREFIX, region));
    }

    @Override
    public Region signingRegion(Region region) {
        return Region.of(SIGNING_REGION_OVERRIDES.getOrDefault(region.id(), region.id()));
    }
}
