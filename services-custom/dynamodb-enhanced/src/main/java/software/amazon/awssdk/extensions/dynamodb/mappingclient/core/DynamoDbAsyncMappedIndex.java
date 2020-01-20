/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.AsyncMappedIndex;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.IndexOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.PaginatedIndexOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@SdkPublicApi
@ThreadSafe
public final class DynamoDbAsyncMappedIndex<T> implements AsyncMappedIndex<T> {
    private final DynamoDbAsyncClient dynamoDbClient;
    private final MapperExtension mapperExtension;
    private final TableSchema<T> tableSchema;
    private final String tableName;
    private final String indexName;

    DynamoDbAsyncMappedIndex(DynamoDbAsyncClient dynamoDbClient,
                             MapperExtension mapperExtension,
                             TableSchema<T> tableSchema,
                             String tableName,
                             String indexName) {
        this.dynamoDbClient = dynamoDbClient;
        this.mapperExtension = mapperExtension;
        this.tableSchema = tableSchema;
        this.tableName = tableName;
        this.indexName = indexName;
    }

    @Override
    public <R> CompletableFuture<R> execute(IndexOperation<T, ?, ?, R> operationToPerform) {
        return operationToPerform.executeOnSecondaryIndexAsync(tableSchema,
                                                               tableName,
                                                               indexName,
                                                               mapperExtension,
                                                               dynamoDbClient);
    }

    @Override
    public <R> SdkPublisher<R> execute(PaginatedIndexOperation<T, ?, ?, R> operationToPerform) {
        return operationToPerform.executeOnSecondaryIndexAsync(tableSchema,
                                                               tableName,
                                                               indexName,
                                                               mapperExtension,
                                                               dynamoDbClient);
    }

    @Override
    public MapperExtension mapperExtension() {
        return this.mapperExtension;
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

        DynamoDbAsyncMappedIndex<?> that = (DynamoDbAsyncMappedIndex<?>) o;

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
        if (tableName != null ? ! tableName.equals(that.tableName) : that.tableName != null) {
            return false;
        }
        return indexName != null ? indexName.equals(that.indexName) : that.indexName == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (mapperExtension != null ? mapperExtension.hashCode() : 0);
        result = 31 * result + (tableSchema != null ? tableSchema.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        result = 31 * result + (indexName != null ? indexName.hashCode() : 0);
        return result;
    }
}
