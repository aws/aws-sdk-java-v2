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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class PutItemOperation<T>
    implements BatchableWriteOperation<T>,
               TransactableWriteOperation<T>,
               TableOperation<T, PutItemRequest, PutItemResponse, PutItemEnhancedResponse<T>> {

    private final Either<PutItemEnhancedRequest<T>, TransactPutItemEnhancedRequest<T>> request;

    private PutItemOperation(PutItemEnhancedRequest<T> request) {
        this.request = Either.left(request);
    }

    private PutItemOperation(TransactPutItemEnhancedRequest<T> request) {
        this.request = Either.right(request);
    }

    public static <T> PutItemOperation<T> create(PutItemEnhancedRequest<T> request) {
        return new PutItemOperation<>(request);
    }

    public static <T> PutItemOperation<T> create(TransactPutItemEnhancedRequest<T> request) {
        return new PutItemOperation<>(request);
    }

    @Override
    public OperationName operationName() {
        return OperationName.PUT_ITEM;
    }

    @Override
    public PutItemRequest generateRequest(TableSchema<T> tableSchema,
                                          OperationContext operationContext,
                                          DynamoDbEnhancedClientExtension extension) {

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("PutItem cannot be executed against a secondary index.");
        }

        T item = request.map(PutItemEnhancedRequest::item, TransactPutItemEnhancedRequest::item);
        TableSchema<? extends T> subtypeTableSchema = tableSchema.subtypeTableSchema(item);
        TableMetadata tableMetadata = subtypeTableSchema.tableMetadata();

        // Fail fast if required primary partition key does not exist and avoid the call to DynamoDb
        tableMetadata.primaryPartitionKey();

        boolean alwaysIgnoreNulls = true;
        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(item, alwaysIgnoreNulls);

        WriteModification transformation =
            extension != null ? extension.beforeWrite(
                DefaultDynamoDbExtensionContext.builder()
                                               .items(itemMap)
                                               .operationContext(operationContext)
                                               .tableMetadata(tableMetadata)
                                               .tableSchema(subtypeTableSchema)
                                               .operationName(operationName())
                                               .build())
                : null;

        if (transformation != null && transformation.transformedItem() != null) {
            itemMap = transformation.transformedItem();
        }

        PutItemRequest.Builder requestBuilder = PutItemRequest.builder()
                                                              .tableName(operationContext.tableName())
                                                              .item(itemMap);

        if (request.left().isPresent()) {
            requestBuilder = addPlainPutItemParameters(requestBuilder, request.left().get());
        }

        requestBuilder = addExpressionsIfExist(requestBuilder, transformation);

        return requestBuilder.build();
    }

    @Override
    public PutItemEnhancedResponse<T> transformResponse(PutItemResponse response,
                                                        TableSchema<T> tableSchema,
                                                        OperationContext operationContext,
                                                        DynamoDbEnhancedClientExtension extension) {
        T attributes = null;
        if (response.hasAttributes()) {
            attributes = EnhancedClientUtils.readAndTransformSingleItem(response.attributes(), tableSchema, operationContext,
                                                                        extension);
        }

        return PutItemEnhancedResponse.<T>builder(null)
                                      .attributes(attributes)
                                      .consumedCapacity(response.consumedCapacity())
                                      .itemCollectionMetrics(response.itemCollectionMetrics())
                                      .build();
    }

    @Override
    public Function<PutItemRequest, PutItemResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::putItem;
    }

    @Override
    public Function<PutItemRequest, CompletableFuture<PutItemResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::putItem;
    }

    @Override
    public WriteRequest generateWriteRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             DynamoDbEnhancedClientExtension extension) {

        PutItemRequest putItemRequest = generateRequest(tableSchema, operationContext, extension);

        if (putItemRequest.conditionExpression() != null) {
            throw new IllegalArgumentException("A mapper extension inserted a conditionExpression in a PutItem "
                                               + "request as part of a BatchWriteItemRequest. This is not supported by "
                                               + "DynamoDb. An extension known to do this is the "
                                               + "VersionedRecordExtension which is loaded by default unless overridden. "
                                               + "To fix this use a table schema that does not "
                                               + "have a versioned attribute in it or do not load the offending extension.");
        }

        return WriteRequest.builder().putRequest(PutRequest.builder().item(putItemRequest.item()).build()).build();
    }

    @Override
    public TransactWriteItem generateTransactWriteItem(TableSchema<T> tableSchema,
                                                       OperationContext operationContext,
                                                       DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {
        PutItemRequest putItemRequest = generateRequest(tableSchema, operationContext, dynamoDbEnhancedClientExtension);

        Put.Builder builder = Put.builder()
                                 .item(putItemRequest.item())
                                 .tableName(putItemRequest.tableName())
                                 .conditionExpression(putItemRequest.conditionExpression())
                                 .expressionAttributeValues(putItemRequest.expressionAttributeValues())
                                 .expressionAttributeNames(putItemRequest.expressionAttributeNames());

        request.right()
               .map(TransactPutItemEnhancedRequest::returnValuesOnConditionCheckFailureAsString)
               .ifPresent(builder::returnValuesOnConditionCheckFailure);

        return TransactWriteItem.builder()
                                .put(builder.build())
                                .build();
    }

    private PutItemRequest.Builder addExpressionsIfExist(PutItemRequest.Builder requestBuilder,
                                                         WriteModification transformation) {

        Expression originalConditionExpression = request.map(r -> Optional.ofNullable(r.conditionExpression()),
                                                             r -> Optional.ofNullable(r.conditionExpression()))
                                                        .orElse(null);
        Expression mergedConditionExpression;

        if (transformation != null && transformation.additionalConditionalExpression() != null) {
            mergedConditionExpression = Expression.join(originalConditionExpression,
                                                        transformation.additionalConditionalExpression(), " AND ");
        } else {
            mergedConditionExpression = originalConditionExpression;
        }

        if (mergedConditionExpression != null) {
            requestBuilder = requestBuilder.conditionExpression(mergedConditionExpression.expression());

            // Avoiding adding empty collections that the low level SDK will propagate to DynamoDb where it causes error.
            if (mergedConditionExpression.expressionValues() != null && !mergedConditionExpression.expressionValues().isEmpty()) {
                requestBuilder = requestBuilder.expressionAttributeValues(mergedConditionExpression.expressionValues());

            }

            if (mergedConditionExpression.expressionNames() != null && !mergedConditionExpression.expressionNames().isEmpty()) {
                requestBuilder = requestBuilder.expressionAttributeNames(mergedConditionExpression.expressionNames());
            }
        }
        return requestBuilder;
    }

    private PutItemRequest.Builder addPlainPutItemParameters(PutItemRequest.Builder requestBuilder,
                                                             PutItemEnhancedRequest<?> enhancedRequest) {
        requestBuilder = requestBuilder.returnValues(enhancedRequest.returnValuesAsString());
        requestBuilder = requestBuilder.returnConsumedCapacity(enhancedRequest.returnConsumedCapacityAsString());
        requestBuilder = requestBuilder.returnItemCollectionMetrics(enhancedRequest.returnItemCollectionMetricsAsString());
        return requestBuilder;
    }
}
