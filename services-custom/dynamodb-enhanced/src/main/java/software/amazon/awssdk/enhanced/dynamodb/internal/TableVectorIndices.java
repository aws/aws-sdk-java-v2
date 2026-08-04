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
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElement;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorIndexMetadata;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Converts {@link VectorIndexMetadata} instances from table metadata into {@link EnhancedVectorIndex} instances for use in table
 * creation operations. This mirrors the pattern used by {@link TableIndices} for secondary indexes.
 */
@SdkInternalApi
public final class TableVectorIndices {
    private final List<VectorIndexMetadata> vectorIndexMetadataList;

    public TableVectorIndices(List<VectorIndexMetadata> vectorIndexMetadataList) {
        this.vectorIndexMetadataList = vectorIndexMetadataList;
    }

    public List<EnhancedVectorIndex> enhancedVectorIndices() {
        if (CollectionUtils.isNullOrEmpty(vectorIndexMetadataList)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(
            vectorIndexMetadataList.stream()
                                   .map(TableVectorIndices::toEnhancedVectorIndex)
                                   .collect(Collectors.toList()));
    }

    private static EnhancedVectorIndex toEnhancedVectorIndex(VectorIndexMetadata metadata) {
        EnhancedVectorIndex.Builder builder = EnhancedVectorIndex.builder()
                                                                 .indexName(metadata.indexName())
                                                                 .vectorAttributeName(metadata.vectorAttributeName())
                                                                 .dimensions(metadata.dimensions())
                                                                 .distanceFunction(metadata.distanceFunction())
                                                                 .projection(metadata.projection());

        List<SearchSchemaElement> schemaElements = metadata.searchSchemaElements();
        if (!CollectionUtils.isNullOrEmpty(schemaElements)) {
            builder.searchSchemaElements(schemaElements);
        }

        return builder.build();
    }
}
