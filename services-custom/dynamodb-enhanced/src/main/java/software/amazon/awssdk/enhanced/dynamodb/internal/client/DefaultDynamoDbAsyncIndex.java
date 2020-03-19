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
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.PaginatedIndexOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.QueryOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.ScanOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@SdkInternalApi
public final class DefaultDynamoDbAsyncIndex<T> implements DynamoDbAsyncIndex<T> {
    private final DynamoDbAsyncClient dynamoDbClient;
    private final DynamoDbEnhancedClientExtension extension;
    private final TableSchema<T> tableSchema;
    private final String tableName;
    private final String indexName;

    DefaultDynamoDbAsyncIndex(DynamoDbAsyncClient dynamoDbClient,
                              DynamoDbEnhancedClientExtension extension,
                              TableSchema<T> tableSchema,
                              String tableName,
                              String indexName) {
        this.dynamoDbClient = dynamoDbClient;
        this.extension = extension;
        this.tableSchema = tableSchema;
        this.tableName = tableName;
        this.indexName = indexName;
    }

    @Override
    public SdkPublisher<Page<T>> query(QueryEnhancedRequest request) {
        PaginatedIndexOperation<T, ?, ?> operation = QueryOperation.create(request);
        return operation.executeOnSecondaryIndexAsync(tableSchema, tableName, indexName, extension, dynamoDbClient);
    }

    @Override
    public SdkPublisher<Page<T>> query(Consumer<QueryEnhancedRequest.Builder> requestConsumer) {
        QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return query(builder.build());
    }

    @Override
    public SdkPublisher<Page<T>> query(QueryConditional queryConditional) {
        return query(r -> r.queryConditional(queryConditional));
    }

    @Override
    public SdkPublisher<Page<T>> scan(ScanEnhancedRequest request) {
        PaginatedIndexOperation<T, ?, ?> operation = ScanOperation.create(request);
        return operation.executeOnSecondaryIndexAsync(tableSchema, tableName, indexName, extension, dynamoDbClient);
    }

    @Override
    public SdkPublisher<Page<T>> scan(Consumer<ScanEnhancedRequest.Builder> requestConsumer) {
        ScanEnhancedRequest.Builder builder = ScanEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return scan(builder.build());
    }

    @Override
    public SdkPublisher<Page<T>> scan() {
        return scan(ScanEnhancedRequest.builder().build());
    }

    @Override
    public DynamoDbEnhancedClientExtension mapperExtension() {
        return this.extension;
    }

    @Override
    public TableSchema<T> tableSchema() {
        return tableSchema;
    }

    public DynamoDbAsyncClient dynamoDbClient() {
        return dynamoDbClient;
    }

    public String tableName() {
        return tableName;
    }

    public String indexName() {
        return indexName;
    }

    @Override
    public Key keyFrom(T item) {
        return createKeyFromItem(item, tableSchema, indexName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultDynamoDbAsyncIndex<?> that = (DefaultDynamoDbAsyncIndex<?>) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient)
            : that.dynamoDbClient != null) {

            return false;
        }
        if (extension != null ? ! extension.equals(that.extension) : that.extension != null) {
            return false;
        }
        if (tableSchema != null ? ! tableSchema.equals(that.tableSchema) : that.tableSchema != null) {
            return false;
        }
        if (tableName != null ? ! tableName.equals(that.tableName) : that.tableName != null) {
            return false;
        }
        return indexName != null ? indexName.equals(that.indexName) : that.indexName == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        result = 31 * result + (tableSchema != null ? tableSchema.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        return result;
    }
}
