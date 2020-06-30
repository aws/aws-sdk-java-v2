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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@SdkInternalApi
public class DeleteItemOperation<T>
    implements TableOperation<T, DeleteItemRequest, DeleteItemResponse, T>,
               TransactableWriteOperation<T>,
               BatchableWriteOperation<T> {

    private final DeleteItemEnhancedRequest request;

    private DeleteItemOperation(DeleteItemEnhancedRequest request) {
        this.request = request;
    }

    public static <T> DeleteItemOperation<T> create(DeleteItemEnhancedRequest request) {
        return new DeleteItemOperation<>(request);
    }

    @Override
    public DeleteItemRequest generateRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             DynamoDbEnhancedClientExtension extension) {

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("DeleteItem cannot be executed against a secondary index.");
        }

        DeleteItemRequest.Builder requestBuilder =
            DeleteItemRequest.builder()
                             .tableName(operationContext.tableName())
                             .key(this.request.key().keyMap(tableSchema, operationContext.indexName()))
                             .returnValues(ReturnValue.ALL_OLD);

        requestBuilder = addExpressionsIfExist(requestBuilder);

        return requestBuilder.build();
    }

    @Override
    public T transformResponse(DeleteItemResponse response,
                               TableSchema<T> tableSchema,
                               OperationContext operationContext,
                               DynamoDbEnhancedClientExtension extension) {
        return EnhancedClientUtils.readAndTransformSingleItem(response.attributes(), tableSchema, operationContext, extension);
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
                                             DynamoDbEnhancedClientExtension extension) {
        DeleteItemRequest deleteItemRequest = generateRequest(tableSchema, operationContext, extension);

        return WriteRequest.builder()
                           .deleteRequest(DeleteRequest.builder().key(deleteItemRequest.key()).build())
                           .build();
    }

    @Override
    public TransactWriteItem generateTransactWriteItem(TableSchema<T> tableSchema,
                                                       OperationContext operationContext,
                                                       DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {
        DeleteItemRequest deleteItemRequest = generateRequest(tableSchema, operationContext, dynamoDbEnhancedClientExtension);

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

    private DeleteItemRequest.Builder addExpressionsIfExist(DeleteItemRequest.Builder requestBuilder) {
        if (this.request.conditionExpression() != null) {
            requestBuilder = requestBuilder.conditionExpression(this.request.conditionExpression().expression());
            Map<String, String> expressionNames = this.request.conditionExpression().expressionNames();
            Map<String, AttributeValue> expressionValues = this.request.conditionExpression().expressionValues();

            // Avoiding adding empty collections that the low level SDK will propagate to DynamoDb where it causes error.
            if (expressionNames != null && !expressionNames.isEmpty()) {
                requestBuilder = requestBuilder.expressionAttributeNames(expressionNames);
            }

            if (expressionValues != null && !expressionValues.isEmpty()) {
                requestBuilder = requestBuilder.expressionAttributeValues(expressionValues);
            }
        }
        return requestBuilder;
    }

}
