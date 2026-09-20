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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncSearchVectorsIntegrationTest extends SearchVectorsIntegrationTestBase {

    private static final String TABLE_NAME = SHARED_ASYNC_TABLE_NAME;
    private DynamoDbAsyncClient asyncClient;
    private DynamoDbEnhancedAsyncClient enhancedAsyncClient;
    private DynamoDbAsyncTable<VectorRecord> mappedTable;

    @BeforeAll
    void setup() {
        asyncClient = createAsyncSearchVectorsClient();
        enhancedAsyncClient = DynamoDbEnhancedAsyncClient
            .builder().dynamoDbClient(asyncClient).build();
        mappedTable = enhancedAsyncClient.table(
            TABLE_NAME, VECTOR_TABLE_SCHEMA);

        prepareAsyncSharedTable(
            asyncClient,
            TABLE_NAME,
            () -> mappedTable.createTable(createTableRequest()).join(),
            () -> testRecords().forEach(r -> mappedTable.putItem(r).join()),
            () -> mappedTable.vectorIndex(COSINE_INDEX)
                             .searchVectorsWithResponse(sharedTableSearchReadyRequest()).join()
                             .results());
        prepareDedicatedIntegrationTablesOnce();
    }

    @AfterAll
    void teardown() {
        asyncClient.close();
    }

    @Override
    protected String sharedTableName() {
        return TABLE_NAME;
    }

    @Override
    protected DescribeTableResponse describeTable(String tableName) {
        return asyncClient.describeTable(r -> r.tableName(tableName)).join();
    }

    @Override
    protected List<SearchResultItem<VectorRecord>> executeSearch(String index, SearchVectorsEnhancedRequest request) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(request).join().results();
    }

    @Override
    protected void executePut(VectorRecord record) {
        mappedTable.putItem(record).join();
    }

    @Override
    protected void executeDelete(Key key) {
        mappedTable.deleteItem(key).join();
    }

    @Override
    protected List<SearchResultItem<EnhancedDocument>> executeSearchDocument(String index,
                                                                             SearchVectorsEnhancedRequest request,
                                                                             DocumentTableSchema schema) {
        DynamoDbAsyncTable<EnhancedDocument> docTable = enhancedAsyncClient.table(TABLE_NAME, schema);
        return docTable.vectorIndex(index).searchVectorsWithResponse(request).join().results();
    }

    @Override
    protected SearchVectorsEnhancedResponse<VectorRecord> executeSearchWithResponse(String index,
                                                                                    SearchVectorsEnhancedRequest request) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(request).join();
    }

    @Override
    protected List<SearchResultItem<VectorRecord>> executeSearchWithBuilder(
        String index, Consumer<SearchVectorsEnhancedRequest.Builder> builder) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(builder).join().results();
    }

    @Override
    protected SearchVectorsEnhancedResponse<VectorRecord> executeSearchWithResponseBuilder(
        String index, Consumer<SearchVectorsEnhancedRequest.Builder> builder) {
        return mappedTable.vectorIndex(index).searchVectorsWithResponse(builder).join();
    }

    @Override
    protected void executeTempTableCreate(String tempTableName,
                                          TableSchema<?> schema,
                                          CreateTableEnhancedRequest request) {
        enhancedAsyncClient.table(tempTableName, schema).createTable(request).join();
        asyncClient.waiter().waitUntilTableExists(r -> r.tableName(tempTableName)).join();
    }

    @Override
    protected void createAndWaitForDedicatedTable(String tableName,
                                                  StaticTableSchema<VectorRecord> schema,
                                                  List<EnhancedVectorIndex> indexes) {
        DynamoDbAsyncTable<VectorRecord> table = enhancedAsyncClient.table(tableName, schema);
        table.createTable(CreateTableEnhancedRequest.builder().vectorIndexes(indexes).build()).join();
        asyncClient.waiter().waitUntilTableExists(r -> r.tableName(tableName)).join();
    }

    @Override
    protected void deleteDedicatedTable(String tableName) {
        asyncClient.deleteTable(r -> r.tableName(tableName)).join();
    }

    @Override
    protected void dedicatedPutItem(String tableName,
                                    StaticTableSchema<VectorRecord> schema,
                                    VectorRecord record) {
        enhancedAsyncClient.table(tableName, schema).putItem(record).join();
    }

    @Override
    protected VectorRecord dedicatedGetItem(String tableName,
                                            StaticTableSchema<VectorRecord> schema,
                                            Key key) {
        return enhancedAsyncClient.table(tableName, schema).getItem(key).join();
    }

    @Override
    protected List<SearchResultItem<VectorRecord>> dedicatedSearch(String tableName,
                                                                   StaticTableSchema<VectorRecord> schema,
                                                                   String indexName,
                                                                   SearchVectorsEnhancedRequest request) {
        return enhancedAsyncClient.table(tableName, schema).vectorIndex(indexName)
                                  .searchVectorsWithResponse(request).join().results();
    }

    @Override
    protected SearchVectorsResponse executeLowLevelSearchVectors(SearchVectorsRequest request) {
        return asyncClient.searchVectors(request).join();
    }
}
