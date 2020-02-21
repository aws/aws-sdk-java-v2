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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.core;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.createKeyFromItem;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbAsyncTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.PaginatedTableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.GetItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.PutItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.ScanEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.CreateTableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.DeleteItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.GetItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.PutItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.QueryOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.ScanOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.UpdateItemOperation;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@SdkInternalApi
public final class DefaultDynamoDbAsyncTable<T> implements DynamoDbAsyncTable<T> {
    private final DynamoDbAsyncClient dynamoDbClient;
    private final MapperExtension mapperExtension;
    private final TableSchema<T> tableSchema;
    private final String tableName;

    DefaultDynamoDbAsyncTable(DynamoDbAsyncClient dynamoDbClient,
                              MapperExtension mapperExtension,
                              TableSchema<T> tableSchema,
                              String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.mapperExtension = mapperExtension;
        this.tableSchema = tableSchema;
        this.tableName = tableName;
    }

    @Override
    public MapperExtension mapperExtension() {
        return this.mapperExtension;
    }

    @Override
    public TableSchema<T> tableSchema() {
        return this.tableSchema;
    }

    public DynamoDbAsyncClient dynamoDbClient() {
        return dynamoDbClient;
    }

    public String tableName() {
        return tableName;
    }

    @Override
    public DefaultDynamoDbAsyncIndex<T> index(String indexName) {
        // Force a check for the existence of the index
        tableSchema.tableMetadata().indexPartitionKey(indexName);

        return new DefaultDynamoDbAsyncIndex<>(dynamoDbClient, mapperExtension, tableSchema, tableName, indexName);
    }

    @Override
    public CompletableFuture<Void> createTable(CreateTableEnhancedRequest request) {
        TableOperation<T, ?, ?, Void> operation = CreateTableOperation.create(request);
        return operation.executeOnPrimaryIndexAsync(tableSchema, tableName, mapperExtension, dynamoDbClient);
    }

    @Override
    public CompletableFuture<T> deleteItem(DeleteItemEnhancedRequest request) {
        TableOperation<T, ?, ?, T> operation = DeleteItemOperation.create(request);
        return operation.executeOnPrimaryIndexAsync(tableSchema, tableName, mapperExtension, dynamoDbClient);
    }

    @Override
    public CompletableFuture<T> getItem(GetItemEnhancedRequest request) {
        TableOperation<T, ?, ?, T> operation = GetItemOperation.create(request);
        return operation.executeOnPrimaryIndexAsync(tableSchema, tableName, mapperExtension, dynamoDbClient);
    }

    @Override
    public SdkPublisher<Page<T>> query(QueryEnhancedRequest request) {
        PaginatedTableOperation<T, ?, ?, Page<T>> operation = QueryOperation.create(request);
        return operation.executeOnPrimaryIndexAsync(tableSchema, tableName, mapperExtension, dynamoDbClient);
    }

    @Override
    public CompletableFuture<Void> putItem(PutItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, Void> operation = PutItemOperation.create(request);
        return operation.executeOnPrimaryIndexAsync(tableSchema, tableName, mapperExtension, dynamoDbClient);
    }

    @Override
    public SdkPublisher<Page<T>> scan(ScanEnhancedRequest request) {
        PaginatedTableOperation<T, ?, ?, Page<T>> operation = ScanOperation.create(request);
        return operation.executeOnPrimaryIndexAsync(tableSchema, tableName, mapperExtension, dynamoDbClient);
    }

    @Override
    public CompletableFuture<T> updateItem(UpdateItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, T> operation = UpdateItemOperation.create(request);
        return operation.executeOnPrimaryIndexAsync(tableSchema, tableName, mapperExtension, dynamoDbClient);
    }

    @Override
    public Key keyFrom(T item) {
        return createKeyFromItem(item, tableSchema, TableMetadata.primaryIndexName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultDynamoDbAsyncTable<?> that = (DefaultDynamoDbAsyncTable<?>) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient)
            : that.dynamoDbClient != null) {

            return false;
        }
        if (mapperExtension != null ? ! mapperExtension.equals(that.mapperExtension) : that.mapperExtension != null) {
            return false;
        }
        if (tableSchema != null ? ! tableSchema.equals(that.tableSchema) : that.tableSchema != null) {
            return false;
        }
        return tableName != null ? tableName.equals(that.tableName) : that.tableName == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (mapperExtension != null ? mapperExtension.hashCode() : 0);
        result = 31 * result + (tableSchema != null ? tableSchema.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }
}
