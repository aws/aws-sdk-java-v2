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
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * This class models a single partition from partitions.json.
 */
@SdkInternalApi
public final class PartitionMetadata {
    private String id;
    private PartitionOutputs outputs;
    private String regionRegex;
    private Map<String, RegionMetadata> regions;

    public PartitionMetadata() {
    }

    public PartitionMetadata(@JsonProperty(value = "id") String id,
                             @JsonProperty(value = "outputs") PartitionOutputs outputs,
                             @JsonProperty(value = "regionRegex") String regionRegex,
                             @JsonProperty(value = "regions") Map<String, RegionMetadata> regions) {
        this.id = id;
        this.outputs = outputs;
        this.regionRegex = regionRegex;
        this.regions = regions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PartitionOutputs getOutputs() {
        return outputs;
    }

    public void setOutputs(PartitionOutputs outputs) {
        this.outputs = outputs;
    }

    public String getRegionRegex() {
        return regionRegex;
    }

    public void setRegionRegex(String regionRegex) {
        this.regionRegex = regionRegex;
    }

    public Map<String, RegionMetadata> getRegions() {
        return regions;
    }

    public void setRegions(Map<String, RegionMetadata> regions) {
        this.regions = regions;
    }

    /**
     * This class models the outputs field of a partition in partitions.json.
     */
    @SdkInternalApi
    public static final class PartitionOutputs {
        private String dnsSuffix;
        private String dualStackDnsSuffix;
        private String implicitGlobalRegion;
        private String name;
        private boolean supportsDualStack;
        private boolean supportsFIPS;

        public PartitionOutputs() {
        }

        public PartitionOutputs(@JsonProperty(value = "dnsSuffix") String dnsSuffix,
                               @JsonProperty(value = "dualStackDnsSuffix") String dualStackDnsSuffix,
                               @JsonProperty(value = "implicitGlobalRegion") String implicitGlobalRegion,
                               @JsonProperty(value = "name") String name,
                               @JsonProperty(value = "supportsDualStack") boolean supportsDualStack,
                               @JsonProperty(value = "supportsFIPS") boolean supportsFIPS) {
            this.dnsSuffix = dnsSuffix;
            this.dualStackDnsSuffix = dualStackDnsSuffix;
            this.implicitGlobalRegion = implicitGlobalRegion;
            this.name = name;
            this.supportsDualStack = supportsDualStack;
            this.supportsFIPS = supportsFIPS;
        }

        public String getDnsSuffix() {
            return dnsSuffix;
        }

        public void setDnsSuffix(String dnsSuffix) {
            this.dnsSuffix = dnsSuffix;
        }

        public String getDualStackDnsSuffix() {
            return dualStackDnsSuffix;
        }

        public void setDualStackDnsSuffix(String dualStackDnsSuffix) {
            this.dualStackDnsSuffix = dualStackDnsSuffix;
        }

        public String getImplicitGlobalRegion() {
            return implicitGlobalRegion;
        }

        public void setImplicitGlobalRegion(String implicitGlobalRegion) {
            this.implicitGlobalRegion = implicitGlobalRegion;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSupportsDualStack() {
            return supportsDualStack;
        }

        public void setSupportsDualStack(boolean supportsDualStack) {
            this.supportsDualStack = supportsDualStack;
        }

        public boolean isSupportsFIPS() {
            return supportsFIPS;
        }

        public void setSupportsFIPS(boolean supportsFIPS) {
            this.supportsFIPS = supportsFIPS;
        }
    }

    /**
     * This class models a region in partitions.json.
     */
    @SdkInternalApi
    public static final class RegionMetadata {
        private String description;

        public RegionMetadata() {
        }

        public RegionMetadata(@JsonProperty(value = "description") String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
