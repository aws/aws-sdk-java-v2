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

import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElement;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.SearchSchemaElementType;
import software.amazon.awssdk.services.dynamodb.model.VectorDistanceFunction;
import software.amazon.awssdk.services.dynamodb.model.VectorIndex;

public class VectorIndexUtilsTest {

    @Test
    public void toVectorIndex_mapsAllFields() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("embeddings-index")
                                                                     .vectorAttributeName("embedding")
                                                                     .dimensions(1536)
                                                                     .distanceFunction(DistanceFunction.COSINE)
                                                                     .projection(Projection.builder()
                                                                                           .projectionType(ProjectionType.ALL)
                                                                                           .build())
                                                                     .addSearchSchemaElement(b -> b.attributeName("id")
                                                                                                   .searchSchemaElementType(HASH))
                                                                     .addSearchSchemaElement(b -> b.attributeName("category")
                                                                                                   .searchSchemaElementType(INLINE_FILTER))
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.indexName()).isEqualTo("embeddings-index");
        assertThat(vectorIndex.vectorAttribute().attributeName()).isEqualTo("embedding");
        assertThat(vectorIndex.dimensions()).isEqualTo(1536L);
        assertThat(vectorIndex.distanceFunction()).isEqualTo(VectorDistanceFunction.COSINE);
        assertThat(vectorIndex.projection().projectionType()).isEqualTo(ProjectionType.ALL);
        assertThat(vectorIndex.searchSchema()).hasSize(2);
        assertThat(vectorIndex.searchSchema().get(0).attributeName()).isEqualTo("id");
        assertThat(vectorIndex.searchSchema().get(0).searchSchemaElementType()).isEqualTo(SearchSchemaElementType.HASH);
        assertThat(vectorIndex.searchSchema().get(1).attributeName()).isEqualTo("category");
        assertThat(vectorIndex.searchSchema().get(1).searchSchemaElementType()).isEqualTo(SearchSchemaElementType.INLINE_FILTER);
    }

    @Test
    public void toVectorIndex_emptySearchSchema_omitsSearchSchemaField() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("simple-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(128)
                                                                     .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                                     .projection(Projection.builder()
                                                                                           .projectionType(ProjectionType.KEYS_ONLY)
                                                                                           .build())
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.indexName()).isEqualTo("simple-index");
        assertThat(vectorIndex.dimensions()).isEqualTo(128L);
        assertThat(vectorIndex.hasSearchSchema()).isFalse();
    }

    @Test
    public void toVectorIndex_dotProduct_mapsDistanceFunction() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("dp-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(256)
                                                                     .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.distanceFunction()).isEqualTo(VectorDistanceFunction.DOT_PRODUCT);
    }

    @Test
    public void toVectorIndex_nullProjection_defaultsToAll() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("no-proj-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(64)
                                                                     .distanceFunction(DistanceFunction.COSINE)
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.projection().projectionType()).isEqualTo(ProjectionType.ALL);
    }

    @Test
    public void toVectorIndex_nullDistanceFunction_returnsNullDistanceFunction() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("null-df-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(64)
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.distanceFunction()).isNull();
    }

    @Test
    public void toVectorIndex_euclidean_mapsDistanceFunction() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("euclidean-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(256)
                                                                     .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.distanceFunction()).isEqualTo(VectorDistanceFunction.EUCLIDEAN);
    }

    @Test
    public void toVectorIndex_nullSearchSchemaElementType_returnsNullElementType() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("null-schema-type-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(64)
                                                                     .distanceFunction(DistanceFunction.COSINE)
                                                                     .addSearchSchemaElement(SearchSchemaElement.builder()
                                                                                                                .attributeName(
                                                                                                                    "pk")
                                                                                                                .build())
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.searchSchema()).hasSize(1);
        assertThat(vectorIndex.searchSchema().get(0).attributeName()).isEqualTo("pk");
        assertThat(vectorIndex.searchSchema().get(0).searchSchemaElementType()).isNull();
    }

    @Test
    public void toVectorIndex_hashSearchSchemaElementType_mapsToHash() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("hash-schema-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(64)
                                                                     .distanceFunction(DistanceFunction.COSINE)
                                                                     .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                                   .searchSchemaElementType(HASH))
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.searchSchema().get(0).searchSchemaElementType())
            .isEqualTo(SearchSchemaElementType.HASH);
    }

    @Test
    public void toVectorIndex_inlineFilterSearchSchemaElementType_mapsToInlineFilter() {
        EnhancedVectorIndex enhancedVectorIndex = EnhancedVectorIndex.builder()
                                                                     .indexName("filter-schema-index")
                                                                     .vectorAttributeName("vec")
                                                                     .dimensions(64)
                                                                     .distanceFunction(DistanceFunction.COSINE)
                                                                     .addSearchSchemaElement(b -> b.attributeName("category")
                                                                                                   .searchSchemaElementType(INLINE_FILTER))
                                                                     .build();

        VectorIndex vectorIndex = VectorIndexUtils.toVectorIndex(enhancedVectorIndex);

        assertThat(vectorIndex.searchSchema().get(0).searchSchemaElementType())
            .isEqualTo(SearchSchemaElementType.INLINE_FILTER);
    }
}
