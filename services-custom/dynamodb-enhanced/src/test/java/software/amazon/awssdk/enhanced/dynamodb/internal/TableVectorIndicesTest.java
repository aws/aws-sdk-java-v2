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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElementType.HASH;
import static software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElementType.INLINE_FILTER;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElement;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorIndexMetadata;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class TableVectorIndicesTest {

    @Test
    public void enhancedVectorIndices_convertsCorrectly() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .indexName("embeddings-index")
                                                          .vectorAttributeName("embedding")
                                                          .dimensions(1536)
                                                          .distanceFunction(DistanceFunction.COSINE)
                                                          .projection(Projection.builder()
                                                                                .projectionType(ProjectionType.ALL)
                                                                                .build())
                                                          .searchSchemaElements(Arrays.asList(
                                                              SearchSchemaElement.builder()
                                                                                 .attributeName("pk")
                                                                                 .searchSchemaElementType(HASH)
                                                                                 .build(),
                                                              SearchSchemaElement.builder()
                                                                                 .attributeName("category")
                                                                                 .searchSchemaElementType(INLINE_FILTER)
                                                                                 .build()))
                                                          .build();

        TableVectorIndices tableVectorIndices = new TableVectorIndices(Collections.singletonList(metadata));
        List<EnhancedVectorIndex> result = tableVectorIndices.enhancedVectorIndices();

        assertThat(result).hasSize(1);
        EnhancedVectorIndex index = result.get(0);
        assertThat(index.indexName()).isEqualTo("embeddings-index");
        assertThat(index.vectorAttributeName()).isEqualTo("embedding");
        assertThat(index.dimensions()).isEqualTo(1536);
        assertThat(index.distanceFunction()).isEqualTo(DistanceFunction.COSINE);
        assertThat(index.projection().projectionType()).isEqualTo(ProjectionType.ALL);
        assertThat(index.searchSchemaElements()).hasSize(2);
        assertThat(index.searchSchemaElements().get(0).attributeName()).isEqualTo("pk");
        assertThat(index.searchSchemaElements().get(0).searchSchemaElementType()).isEqualTo(HASH);
        assertThat(index.searchSchemaElements().get(1).attributeName()).isEqualTo("category");
        assertThat(index.searchSchemaElements().get(1).searchSchemaElementType()).isEqualTo(INLINE_FILTER);
    }

    @Test
    public void enhancedVectorIndices_defaultProjectionAll() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .indexName("no-proj-index")
                                                          .vectorAttributeName("vec")
                                                          .dimensions(128)
                                                          .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                          .build();

        TableVectorIndices tableVectorIndices = new TableVectorIndices(Collections.singletonList(metadata));
        List<EnhancedVectorIndex> result = tableVectorIndices.enhancedVectorIndices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).projection()).isNull();
    }

    @Test
    public void enhancedVectorIndices_emptyList_returnsEmpty() {
        TableVectorIndices tableVectorIndices = new TableVectorIndices(Collections.emptyList());
        List<EnhancedVectorIndex> result = tableVectorIndices.enhancedVectorIndices();

        assertThat(result).isEmpty();
    }

    @Test
    public void enhancedVectorIndices_nullList_returnsEmpty() {
        TableVectorIndices tableVectorIndices = new TableVectorIndices(null);
        List<EnhancedVectorIndex> result = tableVectorIndices.enhancedVectorIndices();

        assertThat(result).isEmpty();
    }

    @Test
    public void enhancedVectorIndices_nullSearchSchema_omitted() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .indexName("minimal-index")
                                                          .vectorAttributeName("vec")
                                                          .dimensions(64)
                                                          .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                          .build();

        TableVectorIndices tableVectorIndices = new TableVectorIndices(Collections.singletonList(metadata));
        List<EnhancedVectorIndex> result = tableVectorIndices.enhancedVectorIndices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).searchSchemaElements()).isEmpty();
    }

    @Test
    public void enhancedVectorIndices_multipleIndices_convertsAll() {
        VectorIndexMetadata first = VectorIndexMetadata.builder()
                                                       .indexName("index-1")
                                                       .vectorAttributeName("vec1")
                                                       .dimensions(128)
                                                       .distanceFunction(DistanceFunction.COSINE)
                                                       .build();

        VectorIndexMetadata second = VectorIndexMetadata.builder()
                                                        .indexName("index-2")
                                                        .vectorAttributeName("vec2")
                                                        .dimensions(256)
                                                        .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                        .build();

        VectorIndexMetadata third = VectorIndexMetadata.builder()
                                                       .indexName("index-3")
                                                       .vectorAttributeName("vec3")
                                                       .dimensions(512)
                                                       .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                       .build();

        TableVectorIndices tableVectorIndices = new TableVectorIndices(Arrays.asList(first, second, third));
        List<EnhancedVectorIndex> result = tableVectorIndices.enhancedVectorIndices();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).indexName()).isEqualTo("index-1");
        assertThat(result.get(1).indexName()).isEqualTo("index-2");
        assertThat(result.get(2).indexName()).isEqualTo("index-3");
    }

    @Test
    public void enhancedVectorIndices_preservesExplicitProjection() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .indexName("keys-only-index")
                                                          .vectorAttributeName("vec")
                                                          .dimensions(768)
                                                          .distanceFunction(DistanceFunction.COSINE)
                                                          .projection(Projection.builder()
                                                                                .projectionType(ProjectionType.KEYS_ONLY)
                                                                                .build())
                                                          .build();

        TableVectorIndices tableVectorIndices = new TableVectorIndices(Collections.singletonList(metadata));
        List<EnhancedVectorIndex> result = tableVectorIndices.enhancedVectorIndices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).projection().projectionType()).isEqualTo(ProjectionType.KEYS_ONLY);
    }
}
