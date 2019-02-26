package software.amazon.awssdk.regions;

import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.regionmetadata.ApNortheast1;
import software.amazon.awssdk.regions.regionmetadata.ApNortheast2;
import software.amazon.awssdk.regions.regionmetadata.ApNortheast3;
import software.amazon.awssdk.regions.regionmetadata.ApSouth1;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast1;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast2;
import software.amazon.awssdk.regions.regionmetadata.CaCentral1;
import software.amazon.awssdk.regions.regionmetadata.CnNorth1;
import software.amazon.awssdk.regions.regionmetadata.CnNorthwest1;
import software.amazon.awssdk.regions.regionmetadata.EuCentral1;
import software.amazon.awssdk.regions.regionmetadata.EuWest1;
import software.amazon.awssdk.regions.regionmetadata.EuWest2;
import software.amazon.awssdk.regions.regionmetadata.EuWest3;
import software.amazon.awssdk.regions.regionmetadata.SaEast1;
import software.amazon.awssdk.regions.regionmetadata.UsEast1;
import software.amazon.awssdk.regions.regionmetadata.UsEast2;
import software.amazon.awssdk.regions.regionmetadata.UsGovWest1;
import software.amazon.awssdk.regions.regionmetadata.UsWest1;
import software.amazon.awssdk.regions.regionmetadata.UsWest2;
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedRegionMetadataProvider implements RegionMetadataProvider {
    private static final Map<Region, RegionMetadata> REGION_METADATA = ImmutableMap.<Region, RegionMetadata> builder()
            .put(Region.AP_NORTHEAST_1, new ApNortheast1()).put(Region.AP_NORTHEAST_2, new ApNortheast2())
            .put(Region.AP_NORTHEAST_3, new ApNortheast3()).put(Region.AP_SOUTH_1, new ApSouth1())
            .put(Region.AP_SOUTHEAST_1, new ApSoutheast1()).put(Region.AP_SOUTHEAST_2, new ApSoutheast2())
            .put(Region.CA_CENTRAL_1, new CaCentral1()).put(Region.EU_CENTRAL_1, new EuCentral1())
            .put(Region.EU_WEST_1, new EuWest1()).put(Region.EU_WEST_2, new EuWest2()).put(Region.EU_WEST_3, new EuWest3())
            .put(Region.SA_EAST_1, new SaEast1()).put(Region.US_EAST_1, new UsEast1()).put(Region.US_EAST_2, new UsEast2())
            .put(Region.US_WEST_1, new UsWest1()).put(Region.US_WEST_2, new UsWest2()).put(Region.CN_NORTH_1, new CnNorth1())
            .put(Region.CN_NORTHWEST_1, new CnNorthwest1()).put(Region.US_GOV_WEST_1, new UsGovWest1()).build();

    public RegionMetadata regionMetadata(Region region) {
        return REGION_METADATA.get(region);
    }
}
