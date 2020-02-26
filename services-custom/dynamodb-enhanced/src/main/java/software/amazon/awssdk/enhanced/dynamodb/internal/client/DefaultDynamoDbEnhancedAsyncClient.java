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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.BatchGetItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.BatchWriteItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TransactGetItemsOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TransactWriteItemsOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@SdkInternalApi
public final class DefaultDynamoDbEnhancedAsyncClient implements DynamoDbEnhancedAsyncClient {
    private final DynamoDbAsyncClient dynamoDbClient;
    private final DynamoDbEnhancedClientExtension extension;

    private DefaultDynamoDbEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbClient,
                                               DynamoDbEnhancedClientExtension extension) {
        this.dynamoDbClient = dynamoDbClient;
        this.extension = extension;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> DefaultDynamoDbAsyncTable<T> table(String tableName, TableSchema<T> tableSchema) {
        return new DefaultDynamoDbAsyncTable<>(dynamoDbClient, extension, tableSchema, tableName);
    }

    @Override
    public SdkPublisher<BatchGetResultPage> batchGetItem(BatchGetItemEnhancedRequest request) {
        BatchGetItemOperation operation = BatchGetItemOperation.create(request);
        return operation.executeAsync(dynamoDbClient, extension);
    }

    @Override
    public SdkPublisher<BatchGetResultPage> batchGetItem(
        Consumer<BatchGetItemEnhancedRequest.Builder> requestConsumer) {

        BatchGetItemEnhancedRequest.Builder builder = BatchGetItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return batchGetItem(builder.build());
    }

    @Override
    public CompletableFuture<BatchWriteResult> batchWriteItem(BatchWriteItemEnhancedRequest request) {
        BatchWriteItemOperation operation = BatchWriteItemOperation.create(request);
        return operation.executeAsync(dynamoDbClient, extension);
    }

    @Override
    public CompletableFuture<BatchWriteResult> batchWriteItem(
        Consumer<BatchWriteItemEnhancedRequest.Builder> requestConsumer) {

        BatchWriteItemEnhancedRequest.Builder builder = BatchWriteItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return batchWriteItem(builder.build());
    }

    @Override
    public CompletableFuture<List<TransactGetResultPage>> transactGetItems(TransactGetItemsEnhancedRequest request) {
        TransactGetItemsOperation operation = TransactGetItemsOperation.create(request);
        return operation.executeAsync(dynamoDbClient, extension);
    }

    @Override
    public CompletableFuture<List<TransactGetResultPage>> transactGetItems(
        Consumer<TransactGetItemsEnhancedRequest.Builder> requestConsumer) {
        TransactGetItemsEnhancedRequest.Builder builder = TransactGetItemsEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return transactGetItems(builder.build());
    }

    @Override
    public CompletableFuture<Void> transactWriteItems(TransactWriteItemsEnhancedRequest request) {
        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(request);
        return operation.executeAsync(dynamoDbClient, extension);
    }

    @Override
    public CompletableFuture<Void> transactWriteItems(
        Consumer<TransactWriteItemsEnhancedRequest.Builder> requestConsumer) {

        TransactWriteItemsEnhancedRequest.Builder builder = TransactWriteItemsEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return transactWriteItems(builder.build());
    }

    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return dynamoDbClient;
    }

    public DynamoDbEnhancedClientExtension mapperExtension() {
        return extension;
    }

    public Builder toBuilder() {
        return builder().dynamoDbClient(this.dynamoDbClient).extendWith(this.extension);
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
        return extension != null ? extension.equals(that.extension) : that.extension == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (extension != null ? extension.hashCode() : 0);
        return result;
    }

    public static final class Builder implements DynamoDbEnhancedAsyncClient.Builder {
        private DynamoDbAsyncClient dynamoDbClient;
        private DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension;

        private Builder() {
        }

        public DefaultDynamoDbEnhancedAsyncClient build() {
            if (dynamoDbClient == null) {
                throw new IllegalArgumentException("You must provide a DynamoDbClient to build a "
                                                   + "DefaultDynamoDbEnhancedClient.");
            }

            return new DefaultDynamoDbEnhancedAsyncClient(dynamoDbClient, dynamoDbEnhancedClientExtension);
        }

        public Builder dynamoDbClient(DynamoDbAsyncClient dynamoDbAsyncClient) {
            this.dynamoDbClient = dynamoDbAsyncClient;
            return this;
        }

        public Builder extendWith(DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {
            if (dynamoDbEnhancedClientExtension != null && this.dynamoDbEnhancedClientExtension != null) {
                throw new IllegalArgumentException("You may only extend a DefaultDynamoDbEnhancedClient with a single "
                                                   + "extension. To combine multiple extensions, use the "
                                                   + "ChainMapperExtension.");
            }

            this.dynamoDbEnhancedClientExtension = dynamoDbEnhancedClientExtension;
            return this;
        }
    }
}
