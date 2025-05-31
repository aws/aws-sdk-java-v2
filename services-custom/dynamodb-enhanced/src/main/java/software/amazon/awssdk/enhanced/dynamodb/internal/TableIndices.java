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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@SdkInternalApi
public class TableIndices {
    private final List<IndexMetadata> indices;

    public TableIndices(List<IndexMetadata> indices) {
        this.indices = indices;
    }

    public List<EnhancedLocalSecondaryIndex> localSecondaryIndices() {
        return Collections.unmodifiableList(indices.stream()
                                                   .filter(index -> !TableMetadata.primaryIndexName().equals(index.name()))
                                                   .filter(index -> !index.partitionKey().isPresent())
                                                   .map(TableIndices::mapIndexMetadataToEnhancedLocalSecondaryIndex)
                                                   .collect(Collectors.toList()));
    }

    public List<EnhancedGlobalSecondaryIndex> globalSecondaryIndices() {
        return Collections.unmodifiableList(indices.stream()
                                                   .filter(index -> !TableMetadata.primaryIndexName().equals(index.name()))
                                                   .filter(index -> index.partitionKey().isPresent())
                                                   .map(TableIndices::mapIndexMetadataToEnhancedGlobalSecondaryIndex)
                                                   .collect(Collectors.toList()));
    }

    private static EnhancedLocalSecondaryIndex mapIndexMetadataToEnhancedLocalSecondaryIndex(IndexMetadata indexMetadata) {
        return EnhancedLocalSecondaryIndex.builder()
                                          .indexName(indexMetadata.name())
                                          .projection(pb -> pb.projectionType(ProjectionType.ALL))
                                          .build();
    }

    private static EnhancedGlobalSecondaryIndex mapIndexMetadataToEnhancedGlobalSecondaryIndex(IndexMetadata indexMetadata) {
        return EnhancedGlobalSecondaryIndex.builder()
                                           .indexName(indexMetadata.name())
                                           .projection(pb -> pb.projectionType(ProjectionType.ALL))
                                           .build();
    }
}
