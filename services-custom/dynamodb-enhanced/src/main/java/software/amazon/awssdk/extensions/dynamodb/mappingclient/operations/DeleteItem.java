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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.readAndTransformSingleItem;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@SdkPublicApi
public class DeleteItem<T>
    implements TableOperation<T, DeleteItemRequest, DeleteItemResponse, T>,
               TransactableWriteOperation<T>,
               BatchableWriteOperation<T> {

    private final Key key;
    private final Expression conditionExpression;

    private DeleteItem(Builder b) {
        this.key = b.key;
        this.conditionExpression = b.conditionExpression;
    }

    public static <T> DeleteItem<T> of(Key key) {
        return DeleteItem.builder().key(key).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().key(key);
    }

    @Override
    public DeleteItemRequest generateRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             MapperExtension mapperExtension) {

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("DeleteItem cannot be executed against a secondary index.");
        }

        DeleteItemRequest.Builder requestBuilder =
            DeleteItemRequest.builder()
                             .tableName(operationContext.tableName())
                             .key(key.keyMap(tableSchema, operationContext.indexName()))
                             .returnValues(ReturnValue.ALL_OLD);

        if (conditionExpression != null) {
            requestBuilder = requestBuilder.conditionExpression(conditionExpression.expression());

            // Avoid adding empty collections
            if (!conditionExpression.expressionNames().isEmpty()) {
                requestBuilder = requestBuilder.expressionAttributeNames(conditionExpression.expressionNames());
            }

            if (!conditionExpression.expressionValues().isEmpty()) {
                requestBuilder = requestBuilder.expressionAttributeValues(conditionExpression.expressionValues());
            }
        }

        return requestBuilder.build();
    }

    @Override
    public T transformResponse(DeleteItemResponse response,
                               TableSchema<T> tableSchema,
                               OperationContext operationContext,
                               MapperExtension mapperExtension) {
        return readAndTransformSingleItem(response.attributes(), tableSchema, operationContext, mapperExtension);
    }

    @Override
    public Function<DeleteItemRequest, DeleteItemResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::deleteItem;
    }

    @Override
    public Function<DeleteItemRequest, CompletableFuture<DeleteItemResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::deleteItem;
    }

    @Override
    public WriteRequest generateWriteRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             MapperExtension mapperExtension) {
        DeleteItemRequest deleteItemRequest = generateRequest(tableSchema, operationContext, mapperExtension);

        return WriteRequest.builder()
                           .deleteRequest(DeleteRequest.builder().key(deleteItemRequest.key()).build())
                           .build();
    }

    @Override
    public TransactWriteItem generateTransactWriteItem(TableSchema<T> tableSchema,
                                                       OperationContext operationContext,
                                                       MapperExtension mapperExtension) {
        DeleteItemRequest deleteItemRequest = generateRequest(tableSchema, operationContext, mapperExtension);

        Delete delete = Delete.builder()
                              .key(deleteItemRequest.key())
                              .tableName(deleteItemRequest.tableName())
                              .conditionExpression(deleteItemRequest.conditionExpression())
                              .expressionAttributeValues(deleteItemRequest.expressionAttributeValues())
                              .expressionAttributeNames(deleteItemRequest.expressionAttributeNames())
                              .build();

        return TransactWriteItem.builder()
                                .delete(delete)
                                .build();
    }

    public Key key() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeleteItem<?> that = (DeleteItem<?>) o;

        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }

    public static final class Builder {
        private Key key;
        private Expression conditionExpression;

        private Builder() {
        }

        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        public Builder conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        public <T> DeleteItem<T> build() {
            return new DeleteItem<>(this);
        }
    }
}
