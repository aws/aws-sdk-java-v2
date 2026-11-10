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

public class VectorIndexMetadataTest {

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
        VectorIndexMetadata metadata = VectorIndexMetadata.builder().build();

        assertThat(metadata.indexName()).isNull();
        assertThat(metadata.vectorAttributeName()).isNull();
        assertThat(metadata.dimensions()).isEqualTo(0);
        assertThat(metadata.distanceFunction()).isNull();
        assertThat(metadata.projection()).isNull();
        assertThat(metadata.searchSchemaElements()).isEmpty();
    }

    @Test
    public void builder_maximal() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .indexName("my-vector-index")
                                                          .vectorAttributeName("embedding")
                                                          .dimensions(128)
                                                          .distanceFunction(DistanceFunction.COSINE)
                                                          .projection(PROJECTION)
                                                          .searchSchemaElements(Arrays.asList(HASH_ELEMENT, FILTER_ELEMENT))
                                                          .build();

        assertThat(metadata.indexName()).isEqualTo("my-vector-index");
        assertThat(metadata.vectorAttributeName()).isEqualTo("embedding");
        assertThat(metadata.dimensions()).isEqualTo(128);
        assertThat(metadata.distanceFunction()).isEqualTo(DistanceFunction.COSINE);
        assertThat(metadata.projection()).isEqualTo(PROJECTION);
        assertThat(metadata.searchSchemaElements()).containsExactly(HASH_ELEMENT, FILTER_ELEMENT);
    }

    @Test
    public void toBuilder_roundTrip() {
        VectorIndexMetadata original = VectorIndexMetadata.builder()
                                                          .indexName("idx")
                                                          .vectorAttributeName("vec")
                                                          .dimensions(256)
                                                          .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                          .projection(PROJECTION)
                                                          .searchSchemaElements(Arrays.asList(HASH_ELEMENT))
                                                          .build();

        VectorIndexMetadata rebuilt = original.toBuilder().build();

        assertThat(rebuilt).isEqualTo(original);
    }

    @Test
    public void fromEnhancedVectorIndex_mapsAllFields() {
        List<SearchSchemaElement> elements = Arrays.asList(HASH_ELEMENT, FILTER_ELEMENT);

        EnhancedVectorIndex enhancedIndex = EnhancedVectorIndex.builder()
                                                               .indexName("my-index")
                                                               .vectorAttributeName("embedding")
                                                               .dimensions(512)
                                                               .distanceFunction(DistanceFunction.EUCLIDEAN)
                                                               .projection(PROJECTION)
                                                               .searchSchemaElements(elements)
                                                               .build();

        VectorIndexMetadata metadata = VectorIndexMetadata.fromEnhancedVectorIndex(enhancedIndex);

        assertThat(metadata.indexName()).isEqualTo("my-index");
        assertThat(metadata.vectorAttributeName()).isEqualTo("embedding");
        assertThat(metadata.dimensions()).isEqualTo(512);
        assertThat(metadata.distanceFunction()).isEqualTo(DistanceFunction.EUCLIDEAN);
        assertThat(metadata.projection()).isEqualTo(PROJECTION);
        assertThat(metadata.searchSchemaElements()).containsExactly(HASH_ELEMENT, FILTER_ELEMENT);
    }

    @Test
    public void searchSchemaElements_nullDefaults_toEmptyList() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .searchSchemaElements(null)
                                                          .build();

        assertThat(metadata.searchSchemaElements()).isEmpty();
    }

    @Test
    public void equals_sameObject() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder().indexName("idx").build();

        assertThat(metadata).isSameAs(metadata);
        assertThat(metadata.equals(metadata)).isTrue();
    }

    @Test
    public void equals_equalObjects() {
        VectorIndexMetadata metadata1 = VectorIndexMetadata.builder()
                                                           .indexName("idx")
                                                           .dimensions(64)
                                                           .distanceFunction(DistanceFunction.COSINE)
                                                           .build();

        VectorIndexMetadata metadata2 = VectorIndexMetadata.builder()
                                                           .indexName("idx")
                                                           .dimensions(64)
                                                           .distanceFunction(DistanceFunction.COSINE)
                                                           .build();

        assertThat(metadata1).isEqualTo(metadata2);
    }

    @Test
    public void equals_null_returnsFalse() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder().indexName("idx").build();

        assertThat(metadata.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder().indexName("idx").build();

        assertThat(metadata.equals("not-metadata")).isFalse();
    }

    @Test
    public void equals_differentIndexName_returnsFalse() {
        VectorIndexMetadata metadata1 = VectorIndexMetadata.builder().indexName("a").build();
        VectorIndexMetadata metadata2 = VectorIndexMetadata.builder().indexName("b").build();

        assertThat(metadata1).isNotEqualTo(metadata2);
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        VectorIndexMetadata metadata1 = VectorIndexMetadata.builder()
                                                           .indexName("idx")
                                                           .dimensions(64)
                                                           .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                           .build();

        VectorIndexMetadata metadata2 = VectorIndexMetadata.builder()
                                                           .indexName("idx")
                                                           .dimensions(64)
                                                           .distanceFunction(DistanceFunction.DOT_PRODUCT)
                                                           .build();

        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());
    }

    @Test
    public void addSearchSchemaElement_accumulatesElements() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .addSearchSchemaElement(HASH_ELEMENT)
                                                          .addSearchSchemaElement(FILTER_ELEMENT)
                                                          .build();

        assertThat(metadata.searchSchemaElements()).containsExactly(HASH_ELEMENT, FILTER_ELEMENT);
    }

    @Test
    public void addSearchSchemaElement_initializesListOnFirstCall() {
        VectorIndexMetadata metadata = VectorIndexMetadata.builder()
                                                          .addSearchSchemaElement(HASH_ELEMENT)
                                                          .build();

        assertThat(metadata.searchSchemaElements()).hasSize(1);
        assertThat(metadata.searchSchemaElements()).containsExactly(HASH_ELEMENT);
    }
}
