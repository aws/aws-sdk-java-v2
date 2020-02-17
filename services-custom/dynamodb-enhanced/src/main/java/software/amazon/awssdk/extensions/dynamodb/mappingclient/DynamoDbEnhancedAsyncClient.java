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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.core.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchGetResultPage;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.BatchWriteResult;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactGetResultPage;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Asynchronous interface for running commands against a DynamoDb database.
 */
@SdkPublicApi
public interface DynamoDbEnhancedAsyncClient {

    /**
     * Returns a mapped table that can be used to execute commands that work with mapped items against that table.
     *
     * @param tableName The name of the physical table persisted by DynamoDb.
     * @param tableSchema A {@link TableSchema} that maps the table to a modelled object.
     * @return A {@link DynamoDbAsyncTable} object that can be used to execute table operations against.
     * @param <T> THe modelled object type being mapped to this table.
     */
    <T> DynamoDbAsyncTable<T> table(String tableName, TableSchema<T> tableSchema);

    default SdkPublisher<BatchGetResultPage> batchGetItem(BatchGetItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default SdkPublisher<BatchGetResultPage> batchGetItem(Consumer<BatchGetItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<BatchWriteResult> batchWriteItem(BatchWriteItemEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<BatchWriteResult> batchWriteItem(Consumer<BatchWriteItemEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<List<TransactGetResultPage>> transactGetItems(TransactGetItemsEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<List<TransactGetResultPage>> transactGetItems(
        Consumer<TransactGetItemsEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<Void> transactWriteItems(TransactWriteItemsEnhancedRequest request) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<Void> transactWriteItems(Consumer<TransactWriteItemsEnhancedRequest.Builder> requestConsumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a default builder for {@link DynamoDbEnhancedAsyncClient}.
     */
    static DynamoDbEnhancedAsyncClient.Builder builder() {
        return DefaultDynamoDbEnhancedAsyncClient.builder();
    }

    /**
     * The builder definition for a {@link DynamoDbEnhancedAsyncClient}.
     */
    interface Builder {
        Builder dynamoDbClient(DynamoDbAsyncClient dynamoDbAsyncClient);

        Builder extendWith(MapperExtension mapperExtension);

        DynamoDbEnhancedAsyncClient build();
    }
}
