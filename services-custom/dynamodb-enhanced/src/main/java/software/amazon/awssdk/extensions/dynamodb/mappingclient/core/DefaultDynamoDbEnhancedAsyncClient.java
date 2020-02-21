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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchGetResultPage;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchWriteResult;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactGetResultPage;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.BatchGetItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.BatchWriteItemOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.TransactGetItemsOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.TransactWriteItemsOperation;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@SdkInternalApi
public final class DefaultDynamoDbEnhancedAsyncClient implements DynamoDbEnhancedAsyncClient {
    private final DynamoDbAsyncClient dynamoDbClient;
    private final MapperExtension mapperExtension;

    private DefaultDynamoDbEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbClient, MapperExtension mapperExtension) {
        this.dynamoDbClient = dynamoDbClient;
        this.mapperExtension = mapperExtension;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> DefaultDynamoDbAsyncTable<T> table(String tableName, TableSchema<T> tableSchema) {
        return new DefaultDynamoDbAsyncTable<>(dynamoDbClient, mapperExtension, tableSchema, tableName);
    }

    @Override
    public SdkPublisher<BatchGetResultPage> batchGetItem(BatchGetItemEnhancedRequest request) {
        BatchGetItemOperation operation = BatchGetItemOperation.create(request);
        return operation.executeAsync(dynamoDbClient, mapperExtension);
    }

    @Override
    public CompletableFuture<BatchWriteResult> batchWriteItem(BatchWriteItemEnhancedRequest request) {
        BatchWriteItemOperation operation = BatchWriteItemOperation.create(request);
        return operation.executeAsync(dynamoDbClient, mapperExtension);
    }

    @Override
    public CompletableFuture<List<TransactGetResultPage>> transactGetItems(TransactGetItemsEnhancedRequest request) {
        TransactGetItemsOperation operation = TransactGetItemsOperation.create(request);
        return operation.executeAsync(dynamoDbClient, mapperExtension);
    }

    @Override
    public CompletableFuture<Void> transactWriteItems(TransactWriteItemsEnhancedRequest request) {
        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(request);
        return operation.executeAsync(dynamoDbClient, mapperExtension);
    }

    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return dynamoDbClient;
    }

    public MapperExtension mapperExtension() {
        return mapperExtension;
    }

    public Builder toBuilder() {
        return builder().dynamoDbClient(this.dynamoDbClient).extendWith(this.mapperExtension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultDynamoDbEnhancedAsyncClient that = (DefaultDynamoDbEnhancedAsyncClient) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient)
            : that.dynamoDbClient != null) {

            return false;
        }
        return mapperExtension != null ? mapperExtension.equals(that.mapperExtension) : that.mapperExtension == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (mapperExtension != null ? mapperExtension.hashCode() : 0);
        return result;
    }

    public static final class Builder implements DynamoDbEnhancedAsyncClient.Builder {
        private DynamoDbAsyncClient dynamoDbClient;
        private MapperExtension mapperExtension;

        private Builder() {
        }

        public DefaultDynamoDbEnhancedAsyncClient build() {
            if (dynamoDbClient == null) {
                throw new IllegalArgumentException("You must provide a DynamoDbClient to build a "
                                                   + "DefaultDynamoDbEnhancedClient.");
            }

            return new DefaultDynamoDbEnhancedAsyncClient(dynamoDbClient, mapperExtension);
        }

        public Builder dynamoDbClient(DynamoDbAsyncClient dynamoDbAsyncClient) {
            this.dynamoDbClient = dynamoDbAsyncClient;
            return this;
        }

        public Builder extendWith(MapperExtension mapperExtension) {
            if (mapperExtension != null && this.mapperExtension != null) {
                throw new IllegalArgumentException("You may only extend a DefaultDynamoDbEnhancedClient with a single "
                                                   + "extension. To combine multiple extensions, use the "
                                                   + "ChainMapperExtension.");
            }

            this.mapperExtension = mapperExtension;
            return this;
        }
    }
}
