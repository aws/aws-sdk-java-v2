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

package software.amazon.awssdk.regions.internal.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * This class models a AWS partition and contains all metadata about it.
 */
@SdkInternalApi
public final class Partition {

    /**
     * The name of the partition.
     */
    private String partition;

    /**
     * Supported regions.
     */
    private Map<String, PartitionRegion> regions;

    /**
     * Supported services;
     */
    private Map<String, Service> services;

    /**
     * description of the partition.
     */
    private String partitionName;

    /**
     * dns suffix for the endpoints in the partition.
     */
    private String dnsSuffix;

    /**
     * region name regex for regions in the partition.
     */
    private String regionRegex;

    /**
     * default endpoint configuration.
     */
    private Endpoint defaults;

    public Partition() {}

    public Partition(@JsonProperty(value = "partition") String partition,
                     @JsonProperty(value = "regions") Map<String, PartitionRegion>
                             regions,
                     @JsonProperty(value = "services") Map<String,
                             Service> services) {
        this.partition = Validate.paramNotNull(partition, "Partition");
        this.regions = regions;
        this.services = services;
    }

    /**
     * Returns the name of the partition.
     */
    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    /**
     * Returns the description of the partition.
     */
    public String getPartitionName() {
        return partitionName;
    }

    /**
     * Sets the description of the partition.
     */
    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    /**
     * Returns the dns suffix of the partition.
     */
    public String getDnsSuffix() {
        return dnsSuffix;
    }

    /**
     * Sets the dns suffix of the partition.
     */
    public void setDnsSuffix(String dnsSuffix) {
        this.dnsSuffix = dnsSuffix;
    }

    /**
     * Returns the regex for the regions in the partition.
     */
    public String getRegionRegex() {
        return regionRegex;
    }

    /**
     * Sets the regex for the regions in the partition.
     */
    public void setRegionRegex(String regionRegex) {
        this.regionRegex = regionRegex;
    }

    /**
     * Returns the default endpoint configuration of the partition.
     */
    public Endpoint getDefaults() {
        return defaults;
    }

    /**
     * Sets the default endpoint configuration of the partition.
     */
    public void setDefaults(Endpoint defaults) {
        this.defaults = defaults;
    }

    /**
     * Returns the set of regions associated with the partition.
     */
    public Map<String, PartitionRegion> getRegions() {
        return regions;
    }

    public void setRegions(Map<String, PartitionRegion> regions) {
        this.regions = regions;
    }

    /**
     * Returns the set of services supported by the partition.
     */
    public Map<String, Service> getServices() {
        return services;
    }

    public void setServices(Map<String, Service> services) {
        this.services = services;
    }

    /**
     * Returns true if the region is explicitly configured in the partition
     * or if the region matches the {@link #regionRegex} of the partition.
     */
    public boolean hasRegion(String region) {
        return regions.containsKey(region) || matchesRegionRegex(region) || hasServiceEndpoint(region);
    }

    private boolean matchesRegionRegex(String region) {
        final Pattern p = Pattern.compile(regionRegex);
        return p.matcher(region).matches();
    }

    @Deprecated
    private boolean hasServiceEndpoint(String endpoint) {
        for (Service s : services.values()) {
            if (s.getEndpoints().containsKey(endpoint)) {
                return true;
            }
        }
        return false;
    }
}
