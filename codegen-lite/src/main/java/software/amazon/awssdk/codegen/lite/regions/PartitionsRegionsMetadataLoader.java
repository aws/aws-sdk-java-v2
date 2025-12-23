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

package software.amazon.awssdk.codegen.lite.regions;

import com.fasterxml.jackson.jr.ob.JSON;
import java.io.File;
import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.lite.regions.model.PartitionsRegionsMetadata;

/**
 * Loads and parses the partitions.json file into {@link PartitionsRegionsMetadata}.
 */
@SdkInternalApi
public final class PartitionsRegionsMetadataLoader {

    private PartitionsRegionsMetadataLoader() {
    }

    public static PartitionsRegionsMetadata build(File path) {
        return loadPartitionFromStream(path, path.toString());
    }

    private static PartitionsRegionsMetadata loadPartitionFromStream(File stream, String location) {

        try {
            return JSON.std.with(JSON.Feature.USE_IS_GETTERS)
                           .beanFrom(PartitionsRegionsMetadata.class, stream);

        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Error while loading partitions file from " + location, e);
        }
    }
}
