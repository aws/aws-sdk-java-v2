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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchResultItem;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.VectorRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchVectorsIntegrationTest extends SearchVectorsIntegrationTestBase {

    private static final String TABLE_NAME = SHARED_SYNC_TABLE_NAME;
    private DynamoDbClient dynamoDbClient;
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<VectorRecord> mappedTable;

    @BeforeAll
    void setup() {
        dynamoDbClient = createSearchVectorsClient();
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(dynamoDbClient).build();
        mappedTable = enhancedClient.table(
            TABLE_NAME, VECTOR_TABLE_SCHEMA);

        prepareSyncSharedTable(
            dynamoDbClient,
            TABLE_NAME,
            () -> mappedTable.createTable(createTableRequest()),
            () -> testRecords().forEach(mappedTable::putItem),
            () -> mappedTable.vectorIndex(COSINE_INDEX)
                             .searchVectorsWithResponse(sharedTableSearchReadyRequest())
                             .results());
        prepareDedicatedIntegrationTablesOnce();
    }

    @AfterAll
    void teardown() {
        dynamoDbClient.close();
    }

    @Override
    protected String sharedTableName() {
        return TABLE_NAME;
    }

    @Override
    protected DescribeTableResponse describeTable(String tableName) {
        return dynamoDbClient.describeTable(r -> r.tableName(tableName));
    }

    @Override
    protected List<SearchResultItem<VectorRecord>> executeSearch(String index, SearchVectorsEnhancedRequest request) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(request).results();
    }

    @Override
    protected void executePut(VectorRecord record) {
        mappedTable.putItem(record);
    }

    @Override
    protected void executeDelete(Key key) {
        mappedTable.deleteItem(key);
    }

    @Override
    protected List<SearchResultItem<EnhancedDocument>> executeSearchDocument(String index,
                                                                             SearchVectorsEnhancedRequest request,
                                                                             DocumentTableSchema schema) {
        DynamoDbTable<EnhancedDocument> docTable = enhancedClient.table(TABLE_NAME, schema);
        return docTable.vectorIndex(index).searchVectorsWithResponse(request).results();
    }

    @Override
    protected SearchVectorsEnhancedResponse<VectorRecord> executeSearchWithResponse(String index,
                                                                                    SearchVectorsEnhancedRequest request) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(request);
    }

    @Override
    protected List<SearchResultItem<VectorRecord>> executeSearchWithBuilder(
        String index, Consumer<SearchVectorsEnhancedRequest.Builder> builder) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(builder).results();
    }

    @Override
    protected SearchVectorsEnhancedResponse<VectorRecord> executeSearchWithResponseBuilder(
        String index, Consumer<SearchVectorsEnhancedRequest.Builder> builder) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(builder);
    }

    @Override
    protected void executeTempTableCreate(String tempTableName,
                                          TableSchema<?> schema,
                                          CreateTableEnhancedRequest request) {
        enhancedClient.table(tempTableName, schema).createTable(request);
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(tempTableName));
    }

    @Override
    protected void createAndWaitForDedicatedTable(String tableName,
                                                  StaticTableSchema<VectorRecord> schema,
                                                  List<EnhancedVectorIndex> indexes) {
        DynamoDbTable<VectorRecord> table = enhancedClient.table(tableName, schema);
        table.createTable(CreateTableEnhancedRequest.builder().vectorIndexes(indexes).build());
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(tableName));
    }

    @Override
    protected void deleteDedicatedTable(String tableName) {
        dynamoDbClient.deleteTable(r -> r.tableName(tableName));
    }

    @Override
    protected void dedicatedPutItem(String tableName,
                                    StaticTableSchema<VectorRecord> schema,
                                    VectorRecord record) {
        enhancedClient.table(tableName, schema).putItem(record);
    }

    @Override
    protected VectorRecord dedicatedGetItem(String tableName,
                                            StaticTableSchema<VectorRecord> schema,
                                            Key key) {
        return enhancedClient.table(tableName, schema).getItem(key);
    }

    @Override
    protected List<SearchResultItem<VectorRecord>> dedicatedSearch(String tableName,
                                                                   StaticTableSchema<VectorRecord> schema,
                                                                   String indexName,
                                                                   SearchVectorsEnhancedRequest request) {
        return enhancedClient.table(tableName, schema).vectorIndex(indexName).searchVectorsWithResponse(request).results();
    }

    @Override
    protected SearchVectorsResponse executeLowLevelSearchVectors(SearchVectorsRequest request) {
        return dynamoDbClient.searchVectors(request);
    }
}
