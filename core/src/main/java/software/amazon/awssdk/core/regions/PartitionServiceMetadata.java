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

package software.amazon.awssdk.core.regions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.core.internal.region.model.CredentialScope;
import software.amazon.awssdk.core.internal.region.model.Endpoint;
import software.amazon.awssdk.core.internal.region.model.Partition;
import software.amazon.awssdk.core.internal.region.model.Service;

public class PartitionServiceMetadata implements ServiceMetadata {

    private static final String SERVICE = "{service}";
    private static final String REGION = "{region}";
    private static final String DNS_SUFFIX = "{dnsSuffix}";

    private final String service;
    private final Map<String, Partition> servicePartitionData;

    public PartitionServiceMetadata(String service,
                                    Map<String, Partition> servicePartitionData) {
        this.service = service;
        this.servicePartitionData = servicePartitionData;
    }

    @Override
    public URI endpointFor(Region region) {
        RegionMetadata regionMetadata = RegionMetadata.of(region);
        Endpoint endpoint = computeEndpoint(service, region);
        return URI.create(endpoint.getHostname()
                                  .replace(SERVICE, service)
                                  .replace(REGION, region.value())
                                  .replace(DNS_SUFFIX, regionMetadata.getDomain()));
    }

    @Override
    public Region signingRegion(Region region) {
        CredentialScope credentialScope = computeEndpoint(service, region).getCredentialScope();
        return Region.of(credentialScope != null && credentialScope.getRegion() != null ?
                                 credentialScope.getRegion() : region.value());
    }

    private Endpoint computeEndpoint(String serviceName, Region region) {
        RegionMetadata regionMetadata = RegionMetadata.of(region);
        Partition partitionData = servicePartitionData.get(regionMetadata.getPartition());
        Service service = partitionData.getServices().get(serviceName);

        return partitionData.getDefaults()
                            .merge(service != null ? service.getDefaults() : null)
                            .merge(service != null && service.getEndpoints() != null ?
                                           service.getEndpoints().get(region.value()) : null);
    }

    @Override
    public List<Region> regions() {
        List<Region> regions = new ArrayList<>();
        servicePartitionData.entrySet().stream()
                                   .forEach(p -> {
                                       Service serviceData = p.getValue().getServices().get(service);
                                       if (serviceData != null) {
                                           serviceData.getEndpoints().keySet().stream().forEach(r -> regions.add(Region.of(r)));
                                       }
                                   });

        return regions;
    }
}
