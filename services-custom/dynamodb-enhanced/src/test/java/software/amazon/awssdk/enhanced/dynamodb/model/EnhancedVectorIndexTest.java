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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

public class EnhancedVectorIndexTest {

    private static final SearchSchemaElement HASH_ELEMENT =
        SearchSchemaElement.builder()
                           .attributeName("pk")
                           .searchSchemaElementType(SearchSchemaElementType.HASH)
                           .build();

    private static final SearchSchemaElement FILTER_ELEMENT =
        SearchSchemaElement.builder()
                           .attributeName("category")
                           .searchSchemaElementType(SearchSchemaElementType.INLINE_FILTER)
                           .build();

    private static final Projection PROJECTION =
        Projection.builder().projectionType(ProjectionType.ALL).build();

    @Test
    public void builder_minimal() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder().build();

        assertThat(index.indexName()).isNull();
        assertThat(index.vectorAttributeName()).isNull();
        assertThat(index.dimensions()).isEqualTo(0);
        assertThat(index.distanceFunction()).isNull();
        assertThat(index.projection()).isNull();
        assertThat(index.searchSchemaElements()).isEmpty();
    }

    @Test
    public void builder_maximal() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder()
                                                       .indexName("my-vector-index")
                                                       .vectorAttributeName("embedding")
                                                       .dimensions(128)
                                                       .distanceFunction(DistanceFunction.COSINE)
                                                       .projection(PROJECTION)
                                                       .searchSchemaElements(Arrays.asList(HASH_ELEMENT, FILTER_ELEMENT))
                                                       .build();

        assertThat(index.indexName()).isEqualTo("my-vector-index");
        assertThat(index.vectorAttributeName()).isEqualTo("embedding");
        assertThat(index.dimensions()).isEqualTo(128);
        assertThat(index.distanceFunction()).isEqualTo(DistanceFunction.COSINE);
        assertThat(index.projection()).isEqualTo(PROJECTION);
        assertThat(index.searchSchemaElements()).containsExactly(HASH_ELEMENT, FILTER_ELEMENT);
    }

    @Test
    public void toBuilder_roundTrip() {
        EnhancedVectorIndex original = EnhancedVectorIndex.builder()
                                                          .indexName("idx")
                                                          .vectorAttributeName("vec")
                                                          .dimensions(256)
                                                          .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                          .projection(PROJECTION)
                                                          .searchSchemaElements(Arrays.asList(HASH_ELEMENT))
                                                          .build();

        EnhancedVectorIndex rebuilt = original.toBuilder().build();

        assertThat(rebuilt).isEqualTo(original);
    }

    @Test
    public void equals_sameObject() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder().indexName("idx").build();

        assertThat(index).isSameAs(index);
        assertThat(index.equals(index)).isTrue();
    }

    @Test
    public void equals_equalObjects() {
        EnhancedVectorIndex index1 = EnhancedVectorIndex.builder()
                                                        .indexName("idx")
                                                        .dimensions(64)
                                                        .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                        .build();

        EnhancedVectorIndex index2 = EnhancedVectorIndex.builder()
                                                        .indexName("idx")
                                                        .dimensions(64)
                                                        .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                        .build();

        assertThat(index1).isEqualTo(index2);
    }

    @Test
    public void equals_null_returnsFalse() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder().indexName("idx").build();

        assertThat(index.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder().indexName("idx").build();

        assertThat(index.equals("not-an-index")).isFalse();
    }

    @Test
    public void equals_differentIndexName_returnsFalse() {
        EnhancedVectorIndex index1 = EnhancedVectorIndex.builder().indexName("a").build();
        EnhancedVectorIndex index2 = EnhancedVectorIndex.builder().indexName("b").build();

        assertThat(index1).isNotEqualTo(index2);
    }

    @Test
    public void equals_differentDimensions_returnsFalse() {
        EnhancedVectorIndex index1 = EnhancedVectorIndex.builder().indexName("idx").dimensions(64).build();
        EnhancedVectorIndex index2 = EnhancedVectorIndex.builder().indexName("idx").dimensions(128).build();

        assertThat(index1).isNotEqualTo(index2);
    }

    @Test
    public void equals_differentDistanceFunction_returnsFalse() {
        EnhancedVectorIndex index1 = EnhancedVectorIndex.builder()
                                                        .indexName("idx")
                                                        .distanceFunction(DistanceFunction.COSINE)
                                                        .build();

        EnhancedVectorIndex index2 = EnhancedVectorIndex.builder()
                                                        .indexName("idx")
                                                        .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                        .build();

        assertThat(index1).isNotEqualTo(index2);
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        EnhancedVectorIndex index1 = EnhancedVectorIndex.builder()
                                                        .indexName("idx")
                                                        .dimensions(64)
                                                        .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                        .build();

        EnhancedVectorIndex index2 = EnhancedVectorIndex.builder()
                                                        .indexName("idx")
                                                        .dimensions(64)
                                                        .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                        .build();

        assertThat(index1.hashCode()).isEqualTo(index2.hashCode());
    }

    @Test
    public void searchSchemaElements_nullDefaults_toEmptyList() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder()
                                                       .searchSchemaElements((List<SearchSchemaElement>) null)
                                                       .build();

        assertThat(index.searchSchemaElements()).isEmpty();
    }

    @Test
    public void searchSchemaElements_varargs() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder()
                                                       .searchSchemaElements(HASH_ELEMENT, FILTER_ELEMENT)
                                                       .build();

        assertThat(index.searchSchemaElements()).containsExactly(HASH_ELEMENT, FILTER_ELEMENT);
    }

    @Test
    public void addSearchSchemaElement_consumerOverload() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder()
                                                       .addSearchSchemaElement(b -> b.attributeName("pk")
                                                                                     .searchSchemaElementType(
                                                                                         SearchSchemaElementType.HASH))
                                                       .build();

        assertThat(index.searchSchemaElements()).hasSize(1);
        assertThat(index.searchSchemaElements().get(0).attributeName()).isEqualTo("pk");
        assertThat(index.searchSchemaElements().get(0).searchSchemaElementType()).isEqualTo(SearchSchemaElementType.HASH);
    }

    @Test
    public void projection_consumerOverload() {
        EnhancedVectorIndex index = EnhancedVectorIndex.builder()
                                                       .projection(b -> b.projectionType(ProjectionType.KEYS_ONLY))
                                                       .build();

        assertThat(index.projection().projectionType()).isEqualTo(ProjectionType.KEYS_ONLY);
    }
}
