package software.amazon.awssdk.regions;

import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.regionmetadata.AfSouth1;
import software.amazon.awssdk.regions.regionmetadata.ApEast1;
import software.amazon.awssdk.regions.regionmetadata.ApNortheast1;
import software.amazon.awssdk.regions.regionmetadata.ApNortheast2;
import software.amazon.awssdk.regions.regionmetadata.ApNortheast3;
import software.amazon.awssdk.regions.regionmetadata.ApSouth1;
import software.amazon.awssdk.regions.regionmetadata.ApSouth2;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast1;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast2;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast3;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast4;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast5;
import software.amazon.awssdk.regions.regionmetadata.ApSoutheast7;
import software.amazon.awssdk.regions.regionmetadata.AwsCnGlobal;
import software.amazon.awssdk.regions.regionmetadata.AwsGlobal;
import software.amazon.awssdk.regions.regionmetadata.AwsIsoBGlobal;
import software.amazon.awssdk.regions.regionmetadata.AwsIsoFGlobal;
import software.amazon.awssdk.regions.regionmetadata.AwsIsoGlobal;
import software.amazon.awssdk.regions.regionmetadata.AwsUsGovGlobal;
import software.amazon.awssdk.regions.regionmetadata.CaCentral1;
import software.amazon.awssdk.regions.regionmetadata.CaWest1;
import software.amazon.awssdk.regions.regionmetadata.CnNorth1;
import software.amazon.awssdk.regions.regionmetadata.CnNorthwest1;
import software.amazon.awssdk.regions.regionmetadata.EuCentral1;
import software.amazon.awssdk.regions.regionmetadata.EuCentral2;
import software.amazon.awssdk.regions.regionmetadata.EuIsoeWest1;
import software.amazon.awssdk.regions.regionmetadata.EuNorth1;
import software.amazon.awssdk.regions.regionmetadata.EuSouth1;
import software.amazon.awssdk.regions.regionmetadata.EuSouth2;
import software.amazon.awssdk.regions.regionmetadata.EuWest1;
import software.amazon.awssdk.regions.regionmetadata.EuWest2;
import software.amazon.awssdk.regions.regionmetadata.EuWest3;
import software.amazon.awssdk.regions.regionmetadata.EuscDeEast1;
import software.amazon.awssdk.regions.regionmetadata.IlCentral1;
import software.amazon.awssdk.regions.regionmetadata.MeCentral1;
import software.amazon.awssdk.regions.regionmetadata.MeSouth1;
import software.amazon.awssdk.regions.regionmetadata.MxCentral1;
import software.amazon.awssdk.regions.regionmetadata.SaEast1;
import software.amazon.awssdk.regions.regionmetadata.UsEast1;
import software.amazon.awssdk.regions.regionmetadata.UsEast2;
import software.amazon.awssdk.regions.regionmetadata.UsGovEast1;
import software.amazon.awssdk.regions.regionmetadata.UsGovWest1;
import software.amazon.awssdk.regions.regionmetadata.UsIsoEast1;
import software.amazon.awssdk.regions.regionmetadata.UsIsoWest1;
import software.amazon.awssdk.regions.regionmetadata.UsIsobEast1;
import software.amazon.awssdk.regions.regionmetadata.UsIsofEast1;
import software.amazon.awssdk.regions.regionmetadata.UsIsofSouth1;
import software.amazon.awssdk.regions.regionmetadata.UsWest1;
import software.amazon.awssdk.regions.regionmetadata.UsWest2;
import software.amazon.awssdk.utils.ImmutableMap;

@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public final class GeneratedRegionMetadataProvider implements RegionMetadataProvider {
    private static final Map<Region, RegionMetadata> REGION_METADATA = ImmutableMap.<Region, RegionMetadata> builder()
                                                                                   .put(Region.AF_SOUTH_1, new AfSouth1()).put(Region.AP_EAST_1, new ApEast1())
                                                                                   .put(Region.AP_NORTHEAST_1, new ApNortheast1()).put(Region.AP_NORTHEAST_2, new ApNortheast2())
                                                                                   .put(Region.AP_NORTHEAST_3, new ApNortheast3()).put(Region.AP_SOUTH_1, new ApSouth1())
                                                                                   .put(Region.AP_SOUTH_2, new ApSouth2()).put(Region.AP_SOUTHEAST_1, new ApSoutheast1())
                                                                                   .put(Region.AP_SOUTHEAST_2, new ApSoutheast2()).put(Region.AP_SOUTHEAST_3, new ApSoutheast3())
                                                                                   .put(Region.AP_SOUTHEAST_4, new ApSoutheast4()).put(Region.AP_SOUTHEAST_5, new ApSoutheast5())
                                                                                   .put(Region.AP_SOUTHEAST_7, new ApSoutheast7()).put(Region.AWS_GLOBAL, new AwsGlobal())
                                                                                   .put(Region.CA_CENTRAL_1, new CaCentral1()).put(Region.CA_WEST_1, new CaWest1())
                                                                                   .put(Region.EU_CENTRAL_1, new EuCentral1()).put(Region.EU_CENTRAL_2, new EuCentral2())
                                                                                   .put(Region.EU_NORTH_1, new EuNorth1()).put(Region.EU_SOUTH_1, new EuSouth1()).put(Region.EU_SOUTH_2, new EuSouth2())
                                                                                   .put(Region.EU_WEST_1, new EuWest1()).put(Region.EU_WEST_2, new EuWest2()).put(Region.EU_WEST_3, new EuWest3())
                                                                                   .put(Region.IL_CENTRAL_1, new IlCentral1()).put(Region.ME_CENTRAL_1, new MeCentral1())
                                                                                   .put(Region.ME_SOUTH_1, new MeSouth1()).put(Region.MX_CENTRAL_1, new MxCentral1())
                                                                                   .put(Region.SA_EAST_1, new SaEast1()).put(Region.US_EAST_1, new UsEast1()).put(Region.US_EAST_2, new UsEast2())
                                                                                   .put(Region.US_WEST_1, new UsWest1()).put(Region.US_WEST_2, new UsWest2())
                                                                                   .put(Region.AWS_CN_GLOBAL, new AwsCnGlobal()).put(Region.CN_NORTH_1, new CnNorth1())
                                                                                   .put(Region.CN_NORTHWEST_1, new CnNorthwest1()).put(Region.AWS_US_GOV_GLOBAL, new AwsUsGovGlobal())
                                                                                   .put(Region.US_GOV_EAST_1, new UsGovEast1()).put(Region.US_GOV_WEST_1, new UsGovWest1())
                                                                                   .put(Region.AWS_ISO_GLOBAL, new AwsIsoGlobal()).put(Region.US_ISO_EAST_1, new UsIsoEast1())
                                                                                   .put(Region.US_ISO_WEST_1, new UsIsoWest1()).put(Region.AWS_ISO_B_GLOBAL, new AwsIsoBGlobal())
                                                                                   .put(Region.US_ISOB_EAST_1, new UsIsobEast1()).put(Region.EU_ISOE_WEST_1, new EuIsoeWest1())
                                                                                   .put(Region.AWS_ISO_F_GLOBAL, new AwsIsoFGlobal()).put(Region.US_ISOF_EAST_1, new UsIsofEast1())
                                                                                   .put(Region.US_ISOF_SOUTH_1, new UsIsofSouth1()).put(Region.EUSC_DE_EAST_1, new EuscDeEast1()).build();

    public RegionMetadata regionMetadata(Region region) {
        return REGION_METADATA.get(region);
    }
}
