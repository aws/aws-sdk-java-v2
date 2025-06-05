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
import software.amazon.awssdk.annotations.SdkInternalApi;
import java.util.List;
import software.amazon.awssdk.utils.Validate;

/**
 * This class models the AWS partitions metadata from partitions.json.
 */
@SdkInternalApi
public final class PartitionsMetadata {
    private List<PartitionMetadata> partitions;
    private String version;

    public PartitionsMetadata() {
    }

    public PartitionsMetadata(@JsonProperty(value = "partitions") List<PartitionMetadata> partitions,
                             @JsonProperty(value = "version") String version) {
        this.partitions = Validate.paramNotNull(partitions, "partitions");
        this.version = Validate.paramNotNull(version, "version");
    }

    public List<PartitionMetadata> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<PartitionMetadata> partitions) {
        this.partitions = partitions;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
