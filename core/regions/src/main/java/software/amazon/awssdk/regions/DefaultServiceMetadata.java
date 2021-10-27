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
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.regions.internal.util.ServiceMetadataUtils;

@SdkPublicApi
public class DefaultServiceMetadata implements ServiceMetadata {
    private final String endpointPrefix;

    public DefaultServiceMetadata(String endpointPrefix) {
        this.endpointPrefix = endpointPrefix;
    }

    @Override
    public URI endpointFor(ServiceEndpointKey key) {
        PartitionMetadata partition = PartitionMetadata.of(key.region());
        PartitionEndpointKey endpointKey = PartitionEndpointKey.builder().tags(key.tags()).build();
        String hostname = partition.hostname(endpointKey);
        String dnsName = partition.dnsSuffix(endpointKey);
        return ServiceMetadataUtils.endpointFor(hostname, endpointPrefix, key.region().id(), dnsName);
    }

    @Override
    public Region signingRegion(ServiceEndpointKey key) {
        return key.region();
    }

    @Override
    public List<Region> regions() {
        return Collections.emptyList();
    }

    @Override
    public List<ServicePartitionMetadata> servicePartitions() {
        return Collections.emptyList();
    }
}
