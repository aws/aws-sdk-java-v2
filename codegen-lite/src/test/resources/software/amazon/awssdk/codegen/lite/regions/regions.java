package software.amazon.awssdk.regions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An Amazon Web Services region that hosts a set of Amazon services.
 * <p>
 * An instance of this class can be retrieved by referencing one of the static constants defined in this class (eg.
 * {@link Region#US_EAST_1}) or by using the {@link Region#of(String)} method if the region you want is not included in
 * this release of the SDK.
 * </p>
 * <p>
 * Each AWS region corresponds to a separate geographical location where a set of Amazon services is deployed. These
 * regions (except for the special {@link #AWS_GLOBAL} and {@link #AWS_CN_GLOBAL} regions) are separate from each other,
 * with their own set of resources. This means a resource created in one region (eg. an SQS queue) is not available in
 * another region.
 * </p>
 * <p>
 * To programmatically determine whether a particular service is deployed to a region, you can use the
 * {@code serviceMetadata} method on the service's client interface. Additional metadata about a region can be
 * discovered using {@link RegionMetadata#of(Region)}.
 * </p>
 * <p>
 * The {@link Region#id()} will be used as the signing region for all requests to AWS services unless an explicit region
 * override is available in {@link RegionMetadata}. This id will also be used to construct the endpoint for accessing a
 * service unless an explicit endpoint is available for that region in {@link RegionMetadata}.
 * </p>
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public final class Region {
    public static final Region ME_CENTRAL_1 = Region.of("me-central-1");

    public static final Region AWS_CN_GLOBAL = Region.of("aws-cn-global", true);

    public static final Region US_ISOF_SOUTH_1 = Region.of("us-isof-south-1");

    public static final Region US_WEST_1 = Region.of("us-west-1");

    public static final Region US_WEST_2 = Region.of("us-west-2");

    public static final Region AF_SOUTH_1 = Region.of("af-south-1");

    public static final Region AP_NORTHEAST_3 = Region.of("ap-northeast-3");

    public static final Region AP_NORTHEAST_2 = Region.of("ap-northeast-2");

    public static final Region AP_NORTHEAST_1 = Region.of("ap-northeast-1");

    public static final Region ME_SOUTH_1 = Region.of("me-south-1");

    public static final Region SA_EAST_1 = Region.of("sa-east-1");

    public static final Region CN_NORTH_1 = Region.of("cn-north-1");

    public static final Region AP_SOUTHEAST_1 = Region.of("ap-southeast-1");

    public static final Region AP_SOUTHEAST_2 = Region.of("ap-southeast-2");

    public static final Region AP_SOUTHEAST_3 = Region.of("ap-southeast-3");

    public static final Region AP_SOUTHEAST_4 = Region.of("ap-southeast-4");

    public static final Region AP_SOUTHEAST_5 = Region.of("ap-southeast-5");

    public static final Region US_EAST_1 = Region.of("us-east-1");

    public static final Region US_EAST_2 = Region.of("us-east-2");

    public static final Region AP_SOUTHEAST_7 = Region.of("ap-southeast-7");

    public static final Region CN_NORTHWEST_1 = Region.of("cn-northwest-1");

    public static final Region AP_SOUTH_2 = Region.of("ap-south-2");

    public static final Region AP_SOUTH_1 = Region.of("ap-south-1");

    public static final Region EU_SOUTH_1 = Region.of("eu-south-1");

    public static final Region EU_SOUTH_2 = Region.of("eu-south-2");

    public static final Region US_GOV_EAST_1 = Region.of("us-gov-east-1");

    public static final Region IL_CENTRAL_1 = Region.of("il-central-1");

    public static final Region CA_CENTRAL_1 = Region.of("ca-central-1");

    public static final Region MX_CENTRAL_1 = Region.of("mx-central-1");

    public static final Region EU_CENTRAL_1 = Region.of("eu-central-1");

    public static final Region US_ISO_WEST_1 = Region.of("us-iso-west-1");

    public static final Region EUSC_DE_EAST_1 = Region.of("eusc-de-east-1");

    public static final Region EU_CENTRAL_2 = Region.of("eu-central-2");

    public static final Region EU_ISOE_WEST_1 = Region.of("eu-isoe-west-1");

    public static final Region AWS_GLOBAL = Region.of("aws-global", true);

    public static final Region EU_NORTH_1 = Region.of("eu-north-1");

    public static final Region EU_WEST_3 = Region.of("eu-west-3");

    public static final Region EU_WEST_2 = Region.of("eu-west-2");

    public static final Region EU_WEST_1 = Region.of("eu-west-1");

    public static final Region AWS_ISO_GLOBAL = Region.of("aws-iso-global", true);

    public static final Region AP_EAST_1 = Region.of("ap-east-1");

    public static final Region CA_WEST_1 = Region.of("ca-west-1");

    public static final Region US_GOV_WEST_1 = Region.of("us-gov-west-1");

    public static final Region US_ISO_EAST_1 = Region.of("us-iso-east-1");

    public static final Region AWS_ISO_B_GLOBAL = Region.of("aws-iso-b-global", true);

    public static final Region AWS_ISO_F_GLOBAL = Region.of("aws-iso-f-global", true);

    public static final Region AWS_US_GOV_GLOBAL = Region.of("aws-us-gov-global", true);

    public static final Region US_ISOB_EAST_1 = Region.of("us-isob-east-1");

    public static final Region US_ISOF_EAST_1 = Region.of("us-isof-east-1");

    private static final List<Region> REGIONS = Collections.unmodifiableList(Arrays.asList(ME_CENTRAL_1, AWS_CN_GLOBAL,
            US_ISOF_SOUTH_1, US_WEST_1, US_WEST_2, AF_SOUTH_1, AP_NORTHEAST_3, AP_NORTHEAST_2, AP_NORTHEAST_1, ME_SOUTH_1,
            SA_EAST_1, CN_NORTH_1, AP_SOUTHEAST_1, AP_SOUTHEAST_2, AP_SOUTHEAST_3, AP_SOUTHEAST_4, AP_SOUTHEAST_5, US_EAST_1,
            US_EAST_2, AP_SOUTHEAST_7, CN_NORTHWEST_1, AP_SOUTH_2, AP_SOUTH_1, EU_SOUTH_1, EU_SOUTH_2, US_GOV_EAST_1,
            IL_CENTRAL_1, CA_CENTRAL_1, MX_CENTRAL_1, EU_CENTRAL_1, US_ISO_WEST_1, EUSC_DE_EAST_1, EU_CENTRAL_2, EU_ISOE_WEST_1,
            AWS_GLOBAL, EU_NORTH_1, EU_WEST_3, EU_WEST_2, EU_WEST_1, AWS_ISO_GLOBAL, AP_EAST_1, CA_WEST_1, US_GOV_WEST_1,
            US_ISO_EAST_1, AWS_ISO_B_GLOBAL, AWS_ISO_F_GLOBAL, AWS_US_GOV_GLOBAL, US_ISOB_EAST_1, US_ISOF_EAST_1));

    private final boolean isGlobalRegion;

    private final String id;

    private Region(String id, boolean isGlobalRegion) {
        this.id = id;
        this.isGlobalRegion = isGlobalRegion;
    }

    public static Region of(String value) {
        return of(value, false);
    }

    private static Region of(String value, boolean isGlobalRegion) {
        Validate.paramNotBlank(value, "region");
        String urlEncodedValue = SdkHttpUtils.urlEncode(value);
        return RegionCache.put(urlEncodedValue, isGlobalRegion);
    }

    public static List<Region> regions() {
        return REGIONS;
    }

    public String id() {
        return this.id;
    }

    public RegionMetadata metadata() {
        return RegionMetadata.of(this);
    }

    public boolean isGlobalRegion() {
        return isGlobalRegion;
    }

    @Override
    public String toString() {
        return id;
    }

    private static class RegionCache {
        private static final ConcurrentHashMap<String, Region> VALUES = new ConcurrentHashMap<>();

        private RegionCache() {
        }

        private static Region put(String value, boolean isGlobalRegion) {
            return VALUES.computeIfAbsent(value, v -> new Region(value, isGlobalRegion));
        }
    }
}
