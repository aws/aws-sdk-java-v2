/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
public final class ElasticacheServiceMetadata implements ServiceMetadata {
    private static final String ENDPOINT_PREFIX = "elasticache";

    private static final Map<String, String> PARTITION_OVERRIDDEN_ENDPOINTS = ImmutableMap.<String, String>builder().build();

    private static final Map<String, String> REGION_OVERRIDDEN_ENDPOINTS = ImmutableMap.<String, String>builder()
            .put("fips", "elasticache-fips.us-gov-west-1.amazonaws.com").build();

    private static final List<Region> REGIONS = Collections.unmodifiableList(Arrays.asList(Region.of("ap-northeast-1"),
            Region.of("ap-northeast-2"), Region.of("ap-south-1"), Region.of("ap-southeast-1"), Region.of("ap-southeast-2"),
            Region.of("ca-central-1"), Region.of("eu-central-1"), Region.of("eu-north-1"), Region.of("eu-west-1"),
            Region.of("eu-west-2"), Region.of("eu-west-3"), Region.of("sa-east-1"), Region.of("us-east-1"),
            Region.of("us-east-2"), Region.of("us-west-1"), Region.of("us-west-2"), Region.of("cn-north-1"),
            Region.of("cn-northwest-1"), Region.of("fips"), Region.of("us-gov-east-1"), Region.of("us-gov-west-1")));

    private static final Map<String, String> SIGNING_REGION_OVERRIDES = ImmutableMap.<String, String>builder()
            .put("fips", "us-gov-west-1").build();

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
}
