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

import static software.amazon.awssdk.enhanced.dynamodb.Expression.joinExpressions;
import static software.amazon.awssdk.enhanced.dynamodb.Expression.joinNames;
import static software.amazon.awssdk.enhanced.dynamodb.Expression.joinValues;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteCompleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteCompleteItemEnhancedRequest;
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
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class DeleteItemOperation<T>
    implements TableOperation<T, DeleteItemRequest, DeleteItemResponse, DeleteItemEnhancedResponse<T>>,
               TransactableWriteOperation<T>,
               BatchableWriteOperation<T> {

    private final Either<DeleteCompleteItemEnhancedRequest<T>, TransactDeleteCompleteItemEnhancedRequest<T>> request;

    private DeleteItemOperation(DeleteCompleteItemEnhancedRequest<T> request) {
        this.request = Either.left(request);
    }

    private DeleteItemOperation(TransactDeleteCompleteItemEnhancedRequest<T> request) {
        this.request = Either.right(request);
    }

    public static <T> DeleteItemOperation<T> create(DeleteItemEnhancedRequest request) {
        return new DeleteItemOperation<>(adapt(request));
    }

    public static <T> DeleteItemOperation<T> create(TransactDeleteItemEnhancedRequest request) {
        return new DeleteItemOperation<>(adapt(request));
    }

    public static <T> DeleteItemOperation<T> create(DeleteCompleteItemEnhancedRequest<T> request) {
        return new DeleteItemOperation<>(request);
    }

    public static <T> DeleteItemOperation<T> create(TransactDeleteCompleteItemEnhancedRequest<T> request) {
        return new DeleteItemOperation<>(request);
    }

    private static DeleteCompleteItemEnhancedRequest adapt(DeleteItemEnhancedRequest request) {
        return DeleteCompleteItemEnhancedRequest.builder()
                                                .key(request.key())
                                                .conditionExpression(request.conditionExpression())
                                                .returnConsumedCapacity(request.returnConsumedCapacityAsString())
                                                .returnItemCollectionMetrics(request.returnItemCollectionMetricsAsString())
                                                .returnValuesOnConditionCheckFailure(
                                                    request.returnValuesOnConditionCheckFailureAsString())
                                                .build();
    }

    private static TransactDeleteCompleteItemEnhancedRequest adapt(TransactDeleteItemEnhancedRequest request) {
        return TransactDeleteCompleteItemEnhancedRequest.builder()
                                                        .key(request.key())
                                                        .conditionExpression(request.conditionExpression())
                                                        .returnValuesOnConditionCheckFailure(
                                                            request.returnValuesOnConditionCheckFailureAsString())
                                                        .build();
    }

    @Override
    public OperationName operationName() {
        return OperationName.DELETE_ITEM;
    }

    /**
     * Builds the DeleteItemRequest, including optimistic delete check logic, if applicable.
     */
    @Override
    public DeleteItemRequest generateRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             DynamoDbEnhancedClientExtension extension) {

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("DeleteItem cannot be executed against a secondary index.");
        }

        Key key = request.map(DeleteCompleteItemEnhancedRequest::key, TransactDeleteCompleteItemEnhancedRequest::key);

        DeleteItemRequest.Builder requestBuilder =
            DeleteItemRequest.builder()
                             .tableName(operationContext.tableName())
                             .key(key.keyMap(tableSchema, operationContext.indexName()))
                             .returnValues(ReturnValue.ALL_OLD);

        if (request.left().isPresent()) {
            requestBuilder = addPlainDeleteItemParameters(requestBuilder, request.left().get());
        }

        requestBuilder = addExpressionsIfExist(requestBuilder);
        performBeforeDeleteChecks(tableSchema, operationContext, extension, requestBuilder);

        return requestBuilder.build();
    }

    @Override
    public DeleteItemEnhancedResponse<T> transformResponse(DeleteItemResponse response,
                                                     TableSchema<T> tableSchema,
                                                     OperationContext operationContext,
                                                     DynamoDbEnhancedClientExtension extension) {
        T attributes = EnhancedClientUtils.readAndTransformSingleItem(response.attributes(), tableSchema, operationContext,
                                                                      extension);
        return DeleteItemEnhancedResponse.<T>builder(null)
                                         .attributes(attributes)
                                         .consumedCapacity(response.consumedCapacity())
                                         .itemCollectionMetrics(response.itemCollectionMetrics())
                                         .build();
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

        Delete.Builder builder = Delete.builder()
                                       .key(deleteItemRequest.key())
                                       .tableName(deleteItemRequest.tableName())
                                       .conditionExpression(deleteItemRequest.conditionExpression())
                                       .expressionAttributeValues(deleteItemRequest.expressionAttributeValues())
                                       .expressionAttributeNames(deleteItemRequest.expressionAttributeNames());

        request.right()
               .map(TransactDeleteCompleteItemEnhancedRequest::returnValuesOnConditionCheckFailureAsString)
               .ifPresent(builder::returnValuesOnConditionCheckFailure);

        return TransactWriteItem.builder()
                                .delete(builder.build())
                                .build();
    }

    private DeleteItemRequest.Builder addExpressionsIfExist(DeleteItemRequest.Builder requestBuilder) {
        Expression conditionExpression = request.map(r -> Optional.ofNullable(r.conditionExpression()),
                                                     r -> Optional.ofNullable(r.conditionExpression()))
                                                .orElse(null);

        if (conditionExpression != null) {
            requestBuilder = requestBuilder.conditionExpression(conditionExpression.expression());
            Map<String, String> expressionNames = conditionExpression.expressionNames();
            Map<String, AttributeValue> expressionValues = conditionExpression.expressionValues();

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

    private DeleteItemRequest.Builder addPlainDeleteItemParameters(DeleteItemRequest.Builder requestBuilder,
                                                                   DeleteCompleteItemEnhancedRequest<T> enhancedRequest) {
        requestBuilder = requestBuilder.returnConsumedCapacity(enhancedRequest.returnConsumedCapacityAsString());
        requestBuilder = requestBuilder.returnItemCollectionMetrics(enhancedRequest.returnItemCollectionMetricsAsString());
        requestBuilder =
            requestBuilder.returnValuesOnConditionCheckFailure(enhancedRequest.returnValuesOnConditionCheckFailureAsString());
        return requestBuilder;
    }

    private void performBeforeDeleteChecks(TableSchema<T> tableSchema,
                                           OperationContext operationContext,
                                           DynamoDbEnhancedClientExtension extension,
                                           DeleteItemRequest.Builder requestBuilder) {

        Key key = request.map(DeleteCompleteItemEnhancedRequest::key, TransactDeleteCompleteItemEnhancedRequest::key);
        Map<String, AttributeValue> keyAttributes = key.keyMap(tableSchema, operationContext.indexName());

        // Create item map for extension processing
        Map<String, AttributeValue> itemForExtensions = new HashMap<>(keyAttributes);

        // If an item is provided (plain or transact), use full item attributes for extension processing (includes version)
        T item = request.left()
                        .map(r -> (T) r.item())
                        .orElseGet(() -> request.right()
                                                .map(r -> (T) r.item())
                                                .orElse(null));

        if (item != null) {
            itemForExtensions = tableSchema.itemToMap(item, false);
        }

        WriteModification beforeDeleteConditionExpression =
            extension != null ? extension.beforeWrite(
                DefaultDynamoDbExtensionContext.builder()
                                               .items(itemForExtensions)
                                               .operationContext(operationContext)
                                               .tableMetadata(tableSchema.tableMetadata())
                                               .tableSchema(tableSchema)
                                               .operationName(operationName())
                                               .build())
                              : null;

        if (beforeDeleteConditionExpression != null) {
            Expression expression = beforeDeleteConditionExpression.additionalConditionalExpression();
            if (expression != null) {
                requestBuilder.conditionExpression(joinExpressions(requestBuilder.build().conditionExpression(),
                                                                   expression.expression(), " AND "))
                              .expressionAttributeNames(joinNames(requestBuilder.build().expressionAttributeNames(),
                                                                  expression.expressionNames()))
                              .expressionAttributeValues(joinValues(requestBuilder.build().expressionAttributeValues(),
                                                                    expression.expressionValues()));
            }
        }
    }
}
