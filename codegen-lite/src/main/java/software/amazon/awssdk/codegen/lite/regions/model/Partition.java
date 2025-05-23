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

package software.amazon.awssdk.codegen.lite.regions.model;

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

    /**
     * The partition id.
     */
    private String id;

    /**
     * Configuration outputs for the partition.
     */
    private PartitionOutputs outputs;

    public Partition() {
    }

    public Partition(@JsonProperty(value = "partition") String partition,
                     @JsonProperty(value = "regions") Map<String, PartitionRegion>
                             regions,
                     @JsonProperty(value = "services") Map<String,
                             Service> services,
                     @JsonProperty(value = "id") String id,
                     @JsonProperty(value = "outputs") PartitionOutputs outputs,
                     @JsonProperty(value = "regionRegex") String regionRegex) {
        this.partition = Validate.paramNotNull(partition, "Partition");
        this.regions = regions;
        this.services = services;
        this.id = id;
        this.outputs = outputs;
        this.regionRegex = regionRegex;
    }

    /**
     * Returns the partition ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the partition id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the configuration outputs for the partition.
     */
    public PartitionOutputs getOutputs() {
        return outputs;
    }

    /**
     * Sets the configuration outputs for the partition.
     */
    public void setOutputs(PartitionOutputs outputs) {
        this.outputs = outputs;
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
        Pattern p = Pattern.compile(regionRegex);
        return p.matcher(region).matches();
    }

    public static class PartitionOutputs {
        private String dnsSuffix;
        private String dualStackDnsSuffix;
        private String implicitGlobalRegion;
        private String name;
        private boolean supportsDualStack;
        private boolean supportsFIPS;

        @JsonProperty("dnsSuffix")
        public String getDnsSuffix() {
            return dnsSuffix;
        }

        public void setDnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
        }

        @JsonProperty("dualStackDnsSuffix")
        public String getDualStackDnsSuffix() {
            return dualStackDnsSuffix;
        }

        public void setDualStackDnsSuffix(String dualStackDnsSuffix) {
            this.dualStackDnsSuffix = dualStackDnsSuffix;
        }

        @JsonProperty("implicitGlobalRegion")
        public String getImplicitGlobalRegion() {
            return implicitGlobalRegion;
        }

        public void setImplicitGlobalRegion(String implicitGlobalRegion) {
            this.implicitGlobalRegion = implicitGlobalRegion;
        }

        @JsonProperty("name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty("supportsDualStack")
        public boolean getSupportsDualStack() {
            return supportsDualStack;
        }

        public void setSupportsDualStack(boolean supportsDualStack) {
            this.supportsDualStack = supportsDualStack;
        }

        @JsonProperty("supportsFIPS")
        public boolean getSupportsFIPS() {
            return supportsFIPS;
        }

        public void setSupportsFIPS(boolean supportsFIPS) {
            this.supportsFIPS = supportsFIPS;
        }
    }

    public static class Region {
        private String description;

        @JsonProperty("description")
        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
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
