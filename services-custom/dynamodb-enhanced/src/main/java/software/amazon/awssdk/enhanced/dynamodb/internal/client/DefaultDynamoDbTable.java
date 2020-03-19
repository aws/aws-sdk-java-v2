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

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.createKeyFromItem;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.CreateTableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DeleteItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.GetItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.PaginatedTableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.PutItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.QueryOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.ScanOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TableOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.UpdateItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SdkInternalApi
public class DefaultDynamoDbTable<T> implements DynamoDbTable<T> {
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClientExtension extension;
    private final TableSchema<T> tableSchema;
    private final String tableName;

    DefaultDynamoDbTable(DynamoDbClient dynamoDbClient,
                         DynamoDbEnhancedClientExtension extension,
                         TableSchema<T> tableSchema,
                         String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.extension = extension;
        this.tableSchema = tableSchema;
        this.tableName = tableName;
    }

    @Override
    public DynamoDbEnhancedClientExtension mapperExtension() {
        return this.extension;
    }

    @Override
    public TableSchema<T> tableSchema() {
        return this.tableSchema;
    }

    public DynamoDbClient dynamoDbClient() {
        return dynamoDbClient;
    }

    public String tableName() {
        return tableName;
    }

    @Override
    public DefaultDynamoDbIndex<T> index(String indexName) {
        // Force a check for the existence of the index
        tableSchema.tableMetadata().indexPartitionKey(indexName);

        return new DefaultDynamoDbIndex<>(dynamoDbClient,
                                          extension,
                                          tableSchema,
                                          tableName,
                                          indexName);
    }

    @Override
    public void createTable(CreateTableEnhancedRequest request) {
        TableOperation<T, ?, ?, Void> operation = CreateTableOperation.create(request);
        operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public void createTable(Consumer<CreateTableEnhancedRequest.Builder> requestConsumer) {
        CreateTableEnhancedRequest.Builder builder = CreateTableEnhancedRequest.builder();
        requestConsumer.accept(builder);
        createTable(builder.build());
    }

    @Override
    public void createTable() {
        createTable(CreateTableEnhancedRequest.builder().build());
    }

    @Override
    public T deleteItem(DeleteItemEnhancedRequest request) {
        TableOperation<T, ?, ?, T> operation = DeleteItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public T deleteItem(Consumer<DeleteItemEnhancedRequest.Builder> requestConsumer) {
        DeleteItemEnhancedRequest.Builder builder = DeleteItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return deleteItem(builder.build());
    }

    @Override
    public T deleteItem(Key key) {
        return deleteItem(r -> r.key(key));
    }

    @Override
    public T deleteItem(T keyItem) {
        return deleteItem(keyFrom(keyItem));
    }

    @Override
    public T getItem(GetItemEnhancedRequest request) {
        TableOperation<T, ?, ?, T> operation = GetItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public T getItem(Consumer<GetItemEnhancedRequest.Builder> requestConsumer) {
        GetItemEnhancedRequest.Builder builder = GetItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return getItem(builder.build());
    }

    @Override
    public T getItem(Key key) {
        return getItem(r -> r.key(key));
    }

    @Override
    public T getItem(T keyItem) {
        return getItem(keyFrom(keyItem));
    }

    @Override
    public PageIterable<T> query(QueryEnhancedRequest request) {
        PaginatedTableOperation<T, ?, ?> operation = QueryOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public PageIterable<T> query(Consumer<QueryEnhancedRequest.Builder> requestConsumer) {
        QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return query(builder.build());
    }

    @Override
    public PageIterable<T> query(QueryConditional queryConditional) {
        return query(r -> r.queryConditional(queryConditional));
    }

    @Override
    public void putItem(PutItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, Void> operation = PutItemOperation.create(request);
        operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public void putItem(Consumer<PutItemEnhancedRequest.Builder<T>> requestConsumer) {
        PutItemEnhancedRequest.Builder<T> builder =
            PutItemEnhancedRequest.builder(this.tableSchema.itemType().rawClass());
        requestConsumer.accept(builder);
        putItem(builder.build());
    }

    @Override
    public void putItem(T item) {
        putItem(r -> r.item(item));
    }

    @Override
    public PageIterable<T> scan(ScanEnhancedRequest request) {
        PaginatedTableOperation<T, ?, ?> operation = ScanOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public PageIterable<T> scan(Consumer<ScanEnhancedRequest.Builder> requestConsumer) {
        ScanEnhancedRequest.Builder builder = ScanEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return scan(builder.build());
    }

    @Override
    public PageIterable<T> scan() {
        return scan(ScanEnhancedRequest.builder().build());
    }

    @Override
    public T updateItem(UpdateItemEnhancedRequest<T> request) {
        TableOperation<T, ?, ?, T> operation = UpdateItemOperation.create(request);
        return operation.executeOnPrimaryIndex(tableSchema, tableName, extension, dynamoDbClient);
    }

    @Override
    public T updateItem(Consumer<UpdateItemEnhancedRequest.Builder<T>> requestConsumer) {
        UpdateItemEnhancedRequest.Builder<T> builder =
            UpdateItemEnhancedRequest.builder(this.tableSchema.itemType().rawClass());
        requestConsumer.accept(builder);
        return updateItem(builder.build());
    }

    @Override
    public T updateItem(T item) {
        return updateItem(r -> r.item(item));
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

        DefaultDynamoDbTable<?> that = (DefaultDynamoDbTable<?>) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient) : that.dynamoDbClient != null) {
            return false;
        }
        if (extension != null ?
            !extension.equals(that.extension) :
            that.extension != null) {

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
        result = 31 * result + (extension != null ?
            extension.hashCode() : 0);
        result = 31 * result + (tableSchema != null ? tableSchema.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }
}
