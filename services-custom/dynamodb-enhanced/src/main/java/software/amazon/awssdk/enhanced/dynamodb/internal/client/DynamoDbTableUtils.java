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

package software.amazon.awssdk.enhanced.dynamodb.internal.client;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.IndexMetadata;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@SdkInternalApi
public final class DynamoDbTableUtils {

    private DynamoDbTableUtils() {
    }

    public static Map<IndexType, List<IndexMetadata>> splitSecondaryIndicesToLocalAndGlobalOnes(TableSchema<?> tableSchema) {
        Collection<IndexMetadata> indices = tableSchema.tableMetadata().indices();
        return indices.stream()
                      .filter(index -> !TableMetadata.primaryIndexName().equals(index.name()))
                      .collect(Collectors.groupingBy(metadata -> {
                          String partitionKeyName = metadata.partitionKey().map(KeyAttributeMetadata::name).orElse(null);
                          if (partitionKeyName == null) {
                              return IndexType.LSI;
                          }
                          return IndexType.GSI;
                      }));
    }

    public static List<EnhancedLocalSecondaryIndex> extractLocalSecondaryIndices(Map<IndexType, List<IndexMetadata>> indicesGroups) {
        return indicesGroups.getOrDefault(IndexType.LSI, emptyList()).stream()
                            .map(DynamoDbTableUtils::mapIndexMetadataToEnhancedLocalSecondaryIndex)
                            .collect(Collectors.toList());
    }

    public static EnhancedLocalSecondaryIndex mapIndexMetadataToEnhancedLocalSecondaryIndex(IndexMetadata indexMetadata) {
        return EnhancedLocalSecondaryIndex.builder()
                                          .indexName(indexMetadata.name())
                                          .projection(pb -> pb.projectionType(ProjectionType.ALL))
                                          .build();
    }

    public static List<EnhancedGlobalSecondaryIndex> extractGlobalSecondaryIndices(Map<IndexType, List<IndexMetadata>> indicesGroups) {
        return indicesGroups.getOrDefault(IndexType.GSI, emptyList()).stream()
                            .map(DynamoDbTableUtils::mapIndexMetadataToEnhancedGlobalSecondaryIndex)
                            .collect(Collectors.toList());
    }

    public static EnhancedGlobalSecondaryIndex mapIndexMetadataToEnhancedGlobalSecondaryIndex(IndexMetadata indexMetadata) {
        return EnhancedGlobalSecondaryIndex.builder()
                                           .indexName(indexMetadata.name())
                                           .projection(pb -> pb.projectionType(ProjectionType.ALL))
                                           .build();
    }

}
