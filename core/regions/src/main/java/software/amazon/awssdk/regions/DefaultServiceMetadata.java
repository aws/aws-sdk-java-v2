/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ImmutableMap;

@SdkPublicApi
public class DefaultServiceMetadata implements ServiceMetadata {

    private static final Map<String, String> REGION_OVERRIDDEN_ENDPOINTS = ImmutableMap.<String, String>builder().build();

    private static final List<Region> REGIONS = new ArrayList<>();

    private static final Map<String, String> SIGNING_REGION_OVERRIDES = ImmutableMap.<String, String>builder().build();

    private final String endpointPrefix;

    public DefaultServiceMetadata(String endpointPrefix) {
        this.endpointPrefix = endpointPrefix;
    }


    @Override
    public URI endpointFor(Region region) {
        return URI.create(computeEndpoint(endpointPrefix, new HashMap<>(), region));
    }

    @Override
    public Region signingRegion(Region region) {
        return region;
    }

    @Override
    public List<Region> regions() {
        return REGIONS;
    }

    @Override
    public List<ServicePartitionMetadata> servicePartitions() {
        return Collections.emptyList();
    }
}
