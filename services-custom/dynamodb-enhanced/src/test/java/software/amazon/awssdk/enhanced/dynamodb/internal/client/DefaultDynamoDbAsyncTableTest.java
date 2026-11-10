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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.LocalSecondaryIndex;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbAsyncTableTest {
    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void index_constructsCorrectMappedIndex() {
        DefaultDynamoDbAsyncTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithIndices.getTableSchema(),
                                            TABLE_NAME);

        DefaultDynamoDbAsyncIndex<FakeItemWithIndices> dynamoDbMappedIndex = dynamoDbMappedTable.index("gsi_1");

        assertThat(dynamoDbMappedIndex.dynamoDbClient()).isSameAs(mockDynamoDbAsyncClient);
        assertThat(dynamoDbMappedIndex.mapperExtension()).isSameAs(mockDynamoDbEnhancedClientExtension);
        assertThat(dynamoDbMappedIndex.tableSchema()).isSameAs(FakeItemWithIndices.getTableSchema());
        assertThat(dynamoDbMappedIndex.tableName()).isEqualTo(TABLE_NAME);
        assertThat(dynamoDbMappedIndex.indexName()).isEqualTo("gsi_1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void index_invalidIndex_throwsIllegalArgumentException() {
        DefaultDynamoDbAsyncTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithIndices.getTableSchema(),
                                            TABLE_NAME);

        dynamoDbMappedTable.index("invalid");
    }

    @Test
    public void keyFrom_primaryIndex_partitionAndSort() {
        FakeItemWithSort item = FakeItemWithSort.createUniqueFakeItemWithSort();
        DefaultDynamoDbAsyncTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithSort.getTableSchema(),
                                            "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue()).isEqualTo(stringValue(item.getId()));
        assertThat(key.sortKeyValue()).isEqualTo(Optional.of(stringValue(item.getSort())));
    }

    @Test
    public void keyFrom_primaryIndex_partitionOnly() {
        FakeItem item = FakeItem.createUniqueFakeItem();
        DefaultDynamoDbAsyncTable<FakeItem> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItem.getTableSchema(),
                                            "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue()).isEqualTo(stringValue(item.getId()));
        assertThat(key.sortKeyValue()).isEqualTo(Optional.empty());
    }

    @Test
    public void keyFrom_primaryIndex_partitionAndNullSort() {
        FakeItemWithSort item = FakeItemWithSort.createUniqueFakeItemWithoutSort();
        DefaultDynamoDbAsyncTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithSort.getTableSchema(),
                                            "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue()).isEqualTo(stringValue(item.getId()));
        assertThat(key.sortKeyValue()).isEqualTo(Optional.empty());
    }

    @Test
    public void createTable_doesNotTreatPrimaryIndexAsAnyOfSecondaryIndexes() {
        DefaultDynamoDbAsyncTable<FakeItem> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItem.getTableSchema(),
                                            "test_table");

        when(mockDynamoDbAsyncClient.createTable(any(CreateTableRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CreateTableResponse.builder().build()));

        dynamoDbMappedIndex.createTable().join();

        ArgumentCaptor<CreateTableRequest> requestCaptor = ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockDynamoDbAsyncClient).createTable(requestCaptor.capture());

        CreateTableRequest request = requestCaptor.getValue();

        assertThat(request.localSecondaryIndexes().size()).isEqualTo(0);
        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(0);
    }

    @Test
    public void createTable_groupsSecondaryIndexesExistingInTableSchema() {
        DefaultDynamoDbAsyncTable<FakeItemWithIndices> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithIndices.getTableSchema(),
                                            "test_table");

        when(mockDynamoDbAsyncClient.createTable(any(CreateTableRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CreateTableResponse.builder().build()));

        dynamoDbMappedIndex.createTable().join();

        ArgumentCaptor<CreateTableRequest> requestCaptor = ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockDynamoDbAsyncClient).createTable(requestCaptor.capture());

        CreateTableRequest request = requestCaptor.getValue();

        assertThat(request.localSecondaryIndexes().size()).isEqualTo(1);
        Iterator<LocalSecondaryIndex> lsiIterator = request.localSecondaryIndexes().iterator();
        assertThat(dynamoDbMappedIndex.tableName()).isEqualTo("test_table");
        assertThat(lsiIterator.next().indexName()).isEqualTo("lsi_1");

        assertThat(request.globalSecondaryIndexes().size()).isEqualTo(2);
        List<String> globalIndicesNames = request.globalSecondaryIndexes().stream()
                                                 .map(GlobalSecondaryIndex::indexName)
                                                 .collect(Collectors.toList());
        assertThat(globalIndicesNames).containsExactlyInAnyOrder("gsi_1", "gsi_2");
    }

    @Test
    public void vectorIndex_constructsCorrectMappedVectorIndex() {
        TableSchema<EnhancedDocument> tableSchema = DocumentTableSchema.builder()
                                                                       .vectorIndex(b -> b.indexName("embeddings-index")
                                                                                          .vectorAttributeName("embedding")
                                                                                          .dimensions(1536)
                                                                                          .distanceFunction(
                                                                                              DistanceFunction.COSINE))
                                                                       .build();
        DefaultDynamoDbAsyncTable<EnhancedDocument> table =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            tableSchema,
                                            TABLE_NAME);

        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = table.vectorIndex("embeddings-index");

        assertThat(vectorIndex.tableSchema()).isSameAs(tableSchema);
        assertThat(vectorIndex.tableName()).isEqualTo(TABLE_NAME);
        assertThat(vectorIndex.indexName()).isEqualTo("embeddings-index");
    }

    @Test
    public void vectorIndex_invalidIndex_throwsIllegalArgumentException() {
        TableSchema<EnhancedDocument> tableSchema = DocumentTableSchema.builder()
                                                                       .vectorIndex(b -> b.indexName("embeddings-index")
                                                                                          .vectorAttributeName("embedding")
                                                                                          .dimensions(1536)
                                                                                          .distanceFunction(
                                                                                              DistanceFunction.COSINE))
                                                                       .build();
        DefaultDynamoDbAsyncTable<EnhancedDocument> table =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            tableSchema,
                                            TABLE_NAME);

        assertThatThrownBy(() -> table.vectorIndex("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nonexistent");
    }

    @Test
    public void vectorIndex_returnsMapperExtension() {
        TableSchema<EnhancedDocument> tableSchema = DocumentTableSchema.builder()
                                                                       .vectorIndex(b -> b.indexName("embeddings-index")
                                                                                          .vectorAttributeName("embedding")
                                                                                          .dimensions(1536)
                                                                                          .distanceFunction(
                                                                                              DistanceFunction.COSINE))
                                                                       .build();
        DefaultDynamoDbAsyncTable<EnhancedDocument> table =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            tableSchema,
                                            TABLE_NAME);

        DefaultDynamoDbAsyncVectorIndex<EnhancedDocument> vectorIndex = table.vectorIndex("embeddings-index");

        assertThat(vectorIndex.mapperExtension()).isSameAs(mockDynamoDbEnhancedClientExtension);
    }

    @Test
    public void createTable_noArg_includesVectorIndexesFromMetadata() {
        TableSchema<EnhancedDocument> tableSchema = DocumentTableSchema.builder()
                                                                       .addIndexPartitionKey(
                                                                           TableMetadata.primaryIndexName(),
                                                                           "pk",
                                                                           AttributeValueType.S)
                                                                       .vectorIndex(b -> b.indexName("vec-index")
                                                                                          .vectorAttributeName("emb")
                                                                                          .dimensions(128)
                                                                                          .distanceFunction(
                                                                                              DistanceFunction.COSINE))
                                                                       .build();
        DefaultDynamoDbAsyncTable<EnhancedDocument> table =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            tableSchema,
                                            TABLE_NAME);

        when(mockDynamoDbAsyncClient.createTable(any(CreateTableRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CreateTableResponse.builder().build()));

        table.createTable().join();

        ArgumentCaptor<CreateTableRequest> captor = ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockDynamoDbAsyncClient).createTable(captor.capture());

        assertThat(captor.getValue().vectorIndexes()).hasSize(1);
        assertThat(captor.getValue().vectorIndexes().get(0).indexName()).isEqualTo("vec-index");
    }

    @Test
    public void createTable_noArg_noVectorMetadata_vectorIndexesEmpty() {
        DefaultDynamoDbAsyncTable<FakeItem> table =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItem.getTableSchema(),
                                            "test_table");

        when(mockDynamoDbAsyncClient.createTable(any(CreateTableRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CreateTableResponse.builder().build()));

        table.createTable().join();

        ArgumentCaptor<CreateTableRequest> captor = ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(mockDynamoDbAsyncClient).createTable(captor.capture());

        assertThat(captor.getValue().vectorIndexes()).isEmpty();
    }
}
