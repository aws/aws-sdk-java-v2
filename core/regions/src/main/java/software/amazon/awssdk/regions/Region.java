/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.regions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * An Amazon Web Services region that hosts a set of Amazon services.
 *
 * <p>An instance of this class can be retrieved by referencing one of the static constants defined in this class
 * (eg. {@link Region#US_EAST_1}) or by using the {@link Region#of(String)} method if the region you want is not
 * included in this release of the SDK.</p>
 *
 * <p>Each AWS region corresponds to a separate geographical location where a set of Amazon services is deployed. These regions
 * (except for the special {@link #AWS_GLOBAL} and {@link #AWS_CN_GLOBAL} regions) are separate from each other, with their own
 * set of resources. This means a resource created in one region (eg. an SQS queue) is not available in another region.</p>
 *
 * <p>To programmatically determine whether a particular service is deployed to a region, you can use the {@code serviceMetadata}
 * method on the service's client interface. Additional metadata about a region can be discovered using
 * {@link RegionMetadata#of(Region)}.</p>
 *
 * <p>The {@link Region#id()} will be used as the signing region for all requests to AWS services unless an explicit region
 * override is available in {@link RegionMetadata}. This id will also be used to construct the endpoint for accessing a
 * service unless an explicit endpoint is available for that region in {@link RegionMetadata}.</p>
 */
@SdkPublicApi
public final class Region {

    // AWS Partition Regions

    public static final Region AP_NORTHEAST_1 = Region.of("ap-northeast-1");
    public static final Region AP_NORTHEAST_2 = Region.of("ap-northeast-2");
    public static final Region AP_SOUTH_1 = Region.of("ap-south-1");
    public static final Region AP_SOUTHEAST_1 = Region.of("ap-southeast-1");
    public static final Region AP_SOUTHEAST_2 = Region.of("ap-southeast-2");

    public static final Region CA_CENTRAL_1 = Region.of("ca-central-1");

    public static final Region EU_CENTRAL_1 = Region.of("eu-central-1");
    public static final Region EU_WEST_1 = Region.of("eu-west-1");
    public static final Region EU_WEST_2 = Region.of("eu-west-2");

    public static final Region SA_EAST_1 = Region.of("sa-east-1");

    public static final Region US_EAST_1 = Region.of("us-east-1");
    public static final Region US_EAST_2 = Region.of("us-east-2");
    public static final Region US_WEST_1 = Region.of("us-west-1");
    public static final Region US_WEST_2 = Region.of("us-west-2");

    public static final Region AWS_GLOBAL = Region.of("aws-global", true);

    // AWS CN Partition Regions

    public static final Region CN_NORTH_1 = Region.of("cn-north-1");
    public static final Region CN_NORTHWEST_1 = Region.of("cn-northwest-1");
    public static final Region AWS_CN_GLOBAL = Region.of("aws-cn-global", true);

    // AWS Gov Cloud Partition Regions
    public static final Region US_GOV_WEST_1 = Region.of("us-gov-west-1");
    public static final Region AWS_US_GOV_GLOBAL = Region.of("aws-us-gov-global", true);

    private static final List<Region> REGIONS = Collections.unmodifiableList(Arrays.asList(
            AP_NORTHEAST_1,
            AP_NORTHEAST_2,
            AP_SOUTH_1,
            AP_SOUTHEAST_1,
            AP_SOUTHEAST_2,
            CA_CENTRAL_1,
            EU_CENTRAL_1,
            EU_WEST_1,
            EU_WEST_2,
            SA_EAST_1,
            US_EAST_1,
            US_EAST_2,
            US_WEST_1,
            US_WEST_2,
            AWS_GLOBAL,
            CN_NORTH_1,
            CN_NORTHWEST_1,
            AWS_CN_GLOBAL,
            US_GOV_WEST_1,
            AWS_US_GOV_GLOBAL));

    private final boolean isGlobalRegion;
    private final String id;

    private Region(String id, boolean isGlobalRegion) {
        this.id = id;
        this.isGlobalRegion = isGlobalRegion;
    }

    /**
     * Load the region associated with the provided region name.
     *
     * <p>This API is idempotent and will always return the same region instance if the same input value is provided.
     * For example, the following conditions will always evaluated to true:
     * {@code Region.of("us-east-1") == Region.of("us-east-1")}, {@code Region.US_EAST_1 == Region.of("us-east-1")}.</p>
     *
     * @param value The name of the region to load. Can't be null, empty or blank
     * @return The region associated with the provided name.
     */
    public static Region of(String value) {
        return of(value, false);
    }

    /**
     * @return the unique identifier for the region
     */
    public String id() {
        return this.id;
    }

    private static Region of(String value, boolean isGlobalRegion) {
        Validate.paramNotBlank(value, "region");
        return RegionCache.put(value, isGlobalRegion);
    }

    /**
     * Retrieve an unmodifiable list of the public regions in this release of the AWS SDK.
     */
    public static List<Region> regions() {
        return REGIONS;
    }

    public boolean isGlobalRegion() {
        return isGlobalRegion;
    }

    private static class RegionCache {

        private static final ConcurrentHashMap<String, Region> VALUES = new ConcurrentHashMap<>();

        private static Region put(String value, boolean isGlobalRegion) {
            return VALUES.computeIfAbsent(value, v -> new Region(value, isGlobalRegion));
        }
    }
}
