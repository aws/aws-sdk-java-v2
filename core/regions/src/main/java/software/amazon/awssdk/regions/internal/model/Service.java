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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.util.ValidationUtils;

/**
 * Endpoint configuration for a service in a partition.
 */
@SdkInternalApi
public class Service {

    /**
     * endpoint configuration for every region in a partition.
     */
    private Map<String, Endpoint> endpoints;

    /**
     * default endpoint configuration for a service across all regions in the
     * partition
     */
    private Endpoint defaults;

    /**
     * the region name if the service is enabled partition wide.
     */
    private String partitionEndpoint;

    /**
     * Returns true if the service is regionalized.
     */
    private boolean isRegionalized;

    public Service() {}

    public Service(@JsonProperty(value = "endpoints") Map<String, Endpoint> endpoints) {
        this.endpoints = ValidationUtils.assertNotNull(endpoints, "endpoints");
    }

    /**
     * Returns the endpoints configuration for all regions in a partition
     * that service supports.
     */
    public Map<String, Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * returns the default endpoints configuration for all regions in a
     * partition.
     */
    public Endpoint getDefaults() {
        return defaults;
    }

    /**
     * Sets the default endpoints configuration for all regions in a
     * partition.
     */
    public void setDefaults(Endpoint defaults) {
        this.defaults = defaults;
    }

    /**
     * returns the region name if the service is enabled partition wide.
     */
    public String getPartitionEndpoint() {
        return partitionEndpoint;
    }

    /**
     * sets the region name if the service is enabled partition wide.
     */
    @JsonProperty(value = "partitionEndpoint")
    public void setPartitionEndpoint(String partitionEndpoint) {
        this.partitionEndpoint = partitionEndpoint;
    }

    /**
     * returns true if the service is regionalized.
     */
    public boolean isRegionalized() {
        return isRegionalized;
    }

    /**
     * sets the regionalized property for a service..
     */
    @JsonProperty(value = "isRegionalized")
    public void setIsRegionalized(boolean regionalized) {
        isRegionalized = regionalized;
    }

    /**
     * A convienient method that returns true if a service has a partition
     * wide endpoint available.
     */
    public boolean isPartitionWideEndpointAvailable() {
        return this.partitionEndpoint != null;
    }
}
