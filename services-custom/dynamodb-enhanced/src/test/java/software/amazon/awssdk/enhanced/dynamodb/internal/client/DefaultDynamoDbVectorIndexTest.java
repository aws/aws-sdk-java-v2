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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbVectorIndexTest {
    private static final String TABLE_NAME = "table-name";
    private static final String VECTOR_INDEX_NAME = "vector-index-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockExtension;

    private final TableSchema<EnhancedDocument> tableSchema = DocumentTableSchema.builder().build();

    private DefaultDynamoDbVectorIndex<EnhancedDocument> createVectorIndex() {
        return new DefaultDynamoDbVectorIndex<>(mockDynamoDbClient, mockExtension, tableSchema,
                                                TABLE_NAME, VECTOR_INDEX_NAME);
    }

    private DefaultDynamoDbVectorIndex<EnhancedDocument> createVectorIndex(String tableName, String indexName) {
        return new DefaultDynamoDbVectorIndex<>(mockDynamoDbClient, mockExtension, tableSchema,
                                                tableName, indexName);
    }

    private void stubSearchVectors() {
        when(mockDynamoDbClient.searchVectors(any(SearchVectorsRequest.class)))
            .thenReturn(SearchVectorsResponse.builder()
                                             .searchResults(Collections.emptyList())
                                             .build());
    }

    private SearchVectorsEnhancedRequest defaultRequest() {
        return SearchVectorsEnhancedRequest.builder()
                                           .searchVector(new float[] {1.0f, 2.0f, 3.0f})
                                           .topK(10)
                                           .build();
    }

    @Test
    public void searchVectorsWithResponse_createsOperationAndExecutes() {
        stubSearchVectors();
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        SearchVectorsEnhancedResponse<EnhancedDocument> response =
            vectorIndex.searchVectorsWithResponse(defaultRequest());

        assertThat(response.results()).isEmpty();
        verify(mockDynamoDbClient).searchVectors(any(SearchVectorsRequest.class));
    }

    @Test
    public void searchVectorsWithResponse_consumerOverload() {
        stubSearchVectors();
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        SearchVectorsEnhancedResponse<EnhancedDocument> response =
            vectorIndex.searchVectorsWithResponse(
                b -> b.searchVector(new float[] {1.0f, 2.0f, 3.0f}).topK(10));

        assertThat(response.results()).isEmpty();
        verify(mockDynamoDbClient).searchVectors(any(SearchVectorsRequest.class));
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.equals(vectorIndex)).isTrue();
    }

    @Test
    public void equals_equalObjects_returnsTrue() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex();
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex2 = createVectorIndex();

        assertThat(vectorIndex1.equals(vectorIndex2)).isTrue();
    }

    @Test
    public void equals_differentTableName_returnsFalse() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex(TABLE_NAME, VECTOR_INDEX_NAME);
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex2 = createVectorIndex("other-table", VECTOR_INDEX_NAME);

        assertThat(vectorIndex1.equals(vectorIndex2)).isFalse();
    }

    @Test
    public void equals_differentIndexName_returnsFalse() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex(TABLE_NAME, VECTOR_INDEX_NAME);
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex2 = createVectorIndex(TABLE_NAME, "other-index");

        assertThat(vectorIndex1.equals(vectorIndex2)).isFalse();
    }

    @Test
    public void equals_nullObject_returnsFalse() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.equals("not-a-vector-index")).isFalse();
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex();
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex2 = createVectorIndex();

        assertThat(vectorIndex1.hashCode()).isEqualTo(vectorIndex2.hashCode());
    }

    @Test
    public void hashCode_differentObjects_differentHashCode() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex(TABLE_NAME, VECTOR_INDEX_NAME);
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex2 = createVectorIndex("other-table", "other-index");

        assertThat(vectorIndex1.hashCode()).isNotEqualTo(vectorIndex2.hashCode());
    }

    @Test
    public void tableSchema_returnsSameInstance() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.tableSchema()).isSameAs(tableSchema);
    }

    @Test
    public void tableName_returnsCorrectValue() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.tableName()).isEqualTo(TABLE_NAME);
    }

    @Test
    public void indexName_returnsCorrectValue() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.indexName()).isEqualTo(VECTOR_INDEX_NAME);
    }

    @Test
    public void mapperExtension_returnsSameInstance() {
        DefaultDynamoDbVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.mapperExtension()).isSameAs(mockExtension);
    }
}
