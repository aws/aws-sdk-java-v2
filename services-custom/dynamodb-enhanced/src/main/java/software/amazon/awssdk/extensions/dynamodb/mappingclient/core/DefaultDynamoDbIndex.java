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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbIndex;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.PaginatedIndexOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.ScanEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.QueryOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.ScanOperation;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SdkPublicApi
@ThreadSafe
public class DefaultDynamoDbIndex<T> implements DynamoDbIndex<T> {
    private final DynamoDbClient dynamoDbClient;
    private final MapperExtension mapperExtension;
    private final TableSchema<T> tableSchema;
    private final String tableName;
    private final String indexName;

    DefaultDynamoDbIndex(DynamoDbClient dynamoDbClient,
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
    public SdkIterable<Page<T>> query(QueryEnhancedRequest request) {
        PaginatedIndexOperation<T, ?, ?, Page<T>> operation = QueryOperation.create(request);
        return operation.executeOnSecondaryIndex(tableSchema, tableName, indexName, mapperExtension, dynamoDbClient);
    }

    @Override
    public SdkIterable<Page<T>> scan(ScanEnhancedRequest request) {
        PaginatedIndexOperation<T, ?, ?, Page<T>> operation = ScanOperation.create(request);
        return operation.executeOnSecondaryIndex(tableSchema, tableName, indexName, mapperExtension, dynamoDbClient);
    }

    @Override
    public MapperExtension mapperExtension() {
        return this.mapperExtension;
    }

    @Override
    public TableSchema<T> tableSchema() {
        return tableSchema;
    }

    public DynamoDbClient dynamoDbClient() {
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

        DefaultDynamoDbIndex<?> that = (DefaultDynamoDbIndex<?>) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient) : that.dynamoDbClient != null) {
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
