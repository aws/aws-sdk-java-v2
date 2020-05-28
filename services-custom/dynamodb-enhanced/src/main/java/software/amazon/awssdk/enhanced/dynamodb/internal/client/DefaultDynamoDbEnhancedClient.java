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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Document;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.BatchGetItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.BatchWriteItemOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TransactGetItemsOperation;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.TransactWriteItemsOperation;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@SdkInternalApi
public final class DefaultDynamoDbEnhancedClient implements DynamoDbEnhancedClient {
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClientExtension extension;

    private DefaultDynamoDbEnhancedClient(Builder builder) {
        this.dynamoDbClient = builder.dynamoDbClient == null ? DynamoDbClient.create() : builder.dynamoDbClient;
        this.extension = ExtensionResolver.resolveExtensions(builder.dynamoDbEnhancedClientExtensions);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> DefaultDynamoDbTable<T> table(String tableName, TableSchema<T> tableSchema) {
        return new DefaultDynamoDbTable<>(dynamoDbClient, extension, tableSchema, tableName);
    }

    @Override
    public BatchGetResultPageIterable batchGetItem(BatchGetItemEnhancedRequest request) {
        BatchGetItemOperation operation = BatchGetItemOperation.create(request);
        return BatchGetResultPageIterable.create(operation.execute(dynamoDbClient, extension));
    }

    @Override
    public BatchGetResultPageIterable batchGetItem(Consumer<BatchGetItemEnhancedRequest.Builder> requestConsumer) {
        BatchGetItemEnhancedRequest.Builder builder = BatchGetItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return batchGetItem(builder.build());
    }

    @Override
    public BatchWriteResult batchWriteItem(BatchWriteItemEnhancedRequest request) {
        BatchWriteItemOperation operation = BatchWriteItemOperation.create(request);
        return operation.execute(dynamoDbClient, extension);
    }

    @Override
    public BatchWriteResult batchWriteItem(Consumer<BatchWriteItemEnhancedRequest.Builder> requestConsumer) {
        BatchWriteItemEnhancedRequest.Builder builder = BatchWriteItemEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return batchWriteItem(builder.build());
    }

    @Override
    public List<Document> transactGetItems(TransactGetItemsEnhancedRequest request) {
        TransactGetItemsOperation operation = TransactGetItemsOperation.create(request);
        return operation.execute(dynamoDbClient, extension);
    }

    @Override
    public List<Document> transactGetItems(
        Consumer<TransactGetItemsEnhancedRequest.Builder> requestConsumer) {

        TransactGetItemsEnhancedRequest.Builder builder = TransactGetItemsEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return transactGetItems(builder.build());
    }

    @Override
    public Void transactWriteItems(TransactWriteItemsEnhancedRequest request) {
        TransactWriteItemsOperation operation = TransactWriteItemsOperation.create(request);
        return operation.execute(dynamoDbClient, extension);
    }

    @Override
    public Void transactWriteItems(Consumer<TransactWriteItemsEnhancedRequest.Builder> requestConsumer) {
        TransactWriteItemsEnhancedRequest.Builder builder = TransactWriteItemsEnhancedRequest.builder();
        requestConsumer.accept(builder);
        return transactWriteItems(builder.build());
    }

    public DynamoDbClient dynamoDbClient() {
        return dynamoDbClient;
    }

    public DynamoDbEnhancedClientExtension mapperExtension() {
        return extension;
    }

    public Builder toBuilder() {
        return builder().dynamoDbClient(this.dynamoDbClient).extensions(this.extension);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultDynamoDbEnhancedClient that = (DefaultDynamoDbEnhancedClient) o;

        if (dynamoDbClient != null ? ! dynamoDbClient.equals(that.dynamoDbClient) : that.dynamoDbClient != null) {
            return false;
        }
        return extension != null ?
            extension.equals(that.extension) :
            that.extension == null;
    }

    @Override
    public int hashCode() {
        int result = dynamoDbClient != null ? dynamoDbClient.hashCode() : 0;
        result = 31 * result + (extension != null ?
            extension.hashCode() : 0);
        return result;
    }

    public static final class Builder implements DynamoDbEnhancedClient.Builder {
        private DynamoDbClient dynamoDbClient;
        private List<DynamoDbEnhancedClientExtension> dynamoDbEnhancedClientExtensions =
            new ArrayList<>(ExtensionResolver.defaultExtensions());

        @Override
        public DefaultDynamoDbEnhancedClient build() {
            return new DefaultDynamoDbEnhancedClient(this);
        }

        @Override
        public Builder dynamoDbClient(DynamoDbClient dynamoDbClient) {
            this.dynamoDbClient = dynamoDbClient;
            return this;
        }

        @Override
        public Builder extensions(DynamoDbEnhancedClientExtension... dynamoDbEnhancedClientExtensions) {
            this.dynamoDbEnhancedClientExtensions = Arrays.asList(dynamoDbEnhancedClientExtensions);
            return this;
        }

        @Override
        public Builder extensions(List<DynamoDbEnhancedClientExtension> dynamoDbEnhancedClientExtensions) {
            this.dynamoDbEnhancedClientExtensions = new ArrayList<>(dynamoDbEnhancedClientExtensions);
            return this;
        }
    }
}
