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
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Metadata of all partitions.
 */
@SdkInternalApi
public final class Partitions {

    /**
     * the version of json schema for the partition metadata.
     */
    private String version;

    /**
     * list of partitions.
     */
    private List<Partition> partitions;

    public Partitions() {
    }

    public Partitions(@JsonProperty(value = "version") String version,
                      @JsonProperty(value = "partitions") List<Partition> partitions) {
        this.version = Validate.paramNotNull(version, "version");
        this.partitions = Validate.paramNotNull(partitions, "version");
    }

    /**
     * returns the version of the json schema for the partition metadata document.
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * returns the list of all partitions loaded from the partition metadata document.
     */
    public List<Partition> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<Partition> partitions) {
        this.partitions = partitions;
    }
}
