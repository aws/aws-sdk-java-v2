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
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbAsyncVectorIndexTest {
    private static final String TABLE_NAME = "table-name";
    private static final String VECTOR_INDEX_NAME = "vector-index-name";

    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    private final TableSchema<EnhancedDocument> tableSchema = DocumentTableSchema.builder().build();

    private DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> createVectorIndex() {
        return new DefaultDynamoDbAsyncVectorIndex<>(mockDynamoDbAsyncClient,
                                                     mockDynamoDbEnhancedClientExtension,
                                                     tableSchema,
                                                     TABLE_NAME,
                                                     VECTOR_INDEX_NAME);
    }

    @Test
    public void searchVectorsWithResponse_createsOperationAndExecutes() {
        SearchVectorsResponse serviceResponse = SearchVectorsResponse.builder()
                                                                     .searchResults(Collections.emptyList())
                                                                     .build();
        when(mockDynamoDbAsyncClient.searchVectors(any(SearchVectorsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(serviceResponse));

        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(new float[] {1.0f, 2.0f})
                                                                           .topK(5)
                                                                           .build();

        SearchVectorsEnhancedResponse<EnhancedDocument> result =
            vectorIndex.searchVectorsWithResponse(request).join();

        assertThat(result).isNotNull();
        assertThat(result.results()).isEmpty();
    }

    @Test
    public void searchVectorsWithResponse_consumerOverload() {
        SearchVectorsResponse serviceResponse = SearchVectorsResponse.builder()
                                                                     .searchResults(Collections.emptyList())
                                                                     .build();
        when(mockDynamoDbAsyncClient.searchVectors(any(SearchVectorsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(serviceResponse));

        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        SearchVectorsEnhancedResponse<EnhancedDocument> result =
            vectorIndex.searchVectorsWithResponse(b -> b.searchVector(new float[] {1.0f, 2.0f}).topK(5)).join();

        assertThat(result).isNotNull();
        assertThat(result.results()).isEmpty();
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.equals(vectorIndex)).isTrue();
    }

    @Test
    public void equals_equalObjects_returnsTrue() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex();
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex2 = createVectorIndex();

        assertThat(vectorIndex1).isEqualTo(vectorIndex2);
    }

    @Test
    public void equals_differentTableName_returnsFalse() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex();
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex2 =
            new DefaultDynamoDbAsyncVectorIndex<>(mockDynamoDbAsyncClient,
                                                  mockDynamoDbEnhancedClientExtension,
                                                  tableSchema,
                                                  "different-table",
                                                  VECTOR_INDEX_NAME);

        assertThat(vectorIndex1).isNotEqualTo(vectorIndex2);
    }

    @Test
    public void equals_differentIndexName_returnsFalse() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex();
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex2 =
            new DefaultDynamoDbAsyncVectorIndex<>(mockDynamoDbAsyncClient,
                                                  mockDynamoDbEnhancedClientExtension,
                                                  tableSchema,
                                                  TABLE_NAME,
                                                  "different-index");

        assertThat(vectorIndex1).isNotEqualTo(vectorIndex2);
    }

    @Test
    public void equals_nullObject_returnsFalse() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex).isNotEqualTo(null);
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex).isNotEqualTo("a string");
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex();
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex2 = createVectorIndex();

        assertThat(vectorIndex1.hashCode()).isEqualTo(vectorIndex2.hashCode());
    }

    @Test
    public void hashCode_differentObjects_differentHashCode() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex1 = createVectorIndex();
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex2 =
            new DefaultDynamoDbAsyncVectorIndex<>(mockDynamoDbAsyncClient,
                                                  mockDynamoDbEnhancedClientExtension,
                                                  tableSchema,
                                                  "different-table",
                                                  "different-index");

        assertThat(vectorIndex1.hashCode()).isNotEqualTo(vectorIndex2.hashCode());
    }

    @Test
    public void tableSchema_returnsSameInstance() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.tableSchema()).isSameAs(tableSchema);
    }

    @Test
    public void tableName_returnsCorrectValue() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.tableName()).isEqualTo(TABLE_NAME);
    }

    @Test
    public void indexName_returnsCorrectValue() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.indexName()).isEqualTo(VECTOR_INDEX_NAME);
    }

    @Test
    public void mapperExtension_returnsSameInstance() {
        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = createVectorIndex();

        assertThat(vectorIndex.mapperExtension()).isSameAs(mockDynamoDbEnhancedClientExtension);
    }
}
