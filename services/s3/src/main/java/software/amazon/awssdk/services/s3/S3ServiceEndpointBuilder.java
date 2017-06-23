/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3;

import java.net.URI;
import java.net.URISyntaxException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.runtime.endpoint.DefaultServiceEndpointBuilder;

public class S3ServiceEndpointBuilder {

    public static URI getEndpoint(S3AdvancedConfiguration advancedConfiguration, Region region) {

        RegionMetadata metadata = RegionMetadata.of(region);
        if (advancedConfiguration != null && advancedConfiguration.dualstackEnabled()) {
            return dualstackEndpoint(metadata);
        } else if (advancedConfiguration != null && advancedConfiguration.accelerateModeEnabled()) {
            return accelerateEndpoint(advancedConfiguration, metadata);
        }

        return new DefaultServiceEndpointBuilder("s3", "https").withRegion(region).getServiceEndpoint();
    }

    private static URI dualstackEndpoint(RegionMetadata metadata) {
        String serviceEndpoint = String.format("%s.%s.%s.%s", "s3", "dualstack", metadata.getName(), metadata.getDomain());
        return toUri(serviceEndpoint);
    }

    private static URI accelerateEndpoint(S3AdvancedConfiguration advancedConfiguration, RegionMetadata metadata) {
        if (advancedConfiguration.dualstackEnabled()) {
            return toUri("s3-accelerate.dualstack." + metadata.getDomain());
        }

        return toUri("s3-accelerate." + metadata.getDomain());
    }

    private static URI toUri(String endpoint) throws IllegalArgumentException {
        try {
            return new URI(String.format("%s://%s", "https", endpoint));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
