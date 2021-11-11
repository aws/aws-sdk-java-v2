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

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.readAndTransformSingleItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.WriteModification;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionBuilder;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class UpdateItemOperation<T>
    implements TableOperation<T, UpdateItemRequest, UpdateItemResponse, UpdateItemEnhancedResponse<T>>,
               TransactableWriteOperation<T> {

    private static final Function<String, String> EXPRESSION_VALUE_KEY_MAPPER =
        key -> ":AMZN_MAPPED_" + EnhancedClientUtils.cleanAttributeName(key);

    private static final Function<String, String> EXPRESSION_KEY_MAPPER =
        key -> "#AMZN_MAPPED_" + EnhancedClientUtils.cleanAttributeName(key);

    private static final Function<String, String> CONDITIONAL_UPDATE_MAPPER =
        key -> "if_not_exists(" + EXPRESSION_KEY_MAPPER.apply(key) + ", " +
                EXPRESSION_VALUE_KEY_MAPPER.apply(key) + ")";

    private final Either<UpdateItemEnhancedRequest<T>, TransactUpdateItemEnhancedRequest<T>> request;

    private UpdateItemOperation(UpdateItemEnhancedRequest<T> request) {
        this.request = Either.left(request);
    }

    private UpdateItemOperation(TransactUpdateItemEnhancedRequest<T> request) {
        this.request = Either.right(request);
    }

    public static <T> UpdateItemOperation<T> create(UpdateItemEnhancedRequest<T> request) {
        return new UpdateItemOperation<>(request);
    }

    public static <T> UpdateItemOperation<T> create(TransactUpdateItemEnhancedRequest<T> request) {
        return new UpdateItemOperation<>(request);
    }

    @Override
    public OperationName operationName() {
        return OperationName.UPDATE_ITEM;
    }

    @Override
    public UpdateItemRequest generateRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             DynamoDbEnhancedClientExtension extension) {
        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("UpdateItem cannot be executed against a secondary index.");
        }

        T item = request.map(UpdateItemEnhancedRequest::item, TransactUpdateItemEnhancedRequest::item);
        Boolean ignoreNulls = request.map(r -> Optional.ofNullable(r.ignoreNulls()),
                                          r -> Optional.ofNullable(r.ignoreNulls()))
                                     .orElse(null);

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(item, Boolean.TRUE.equals(ignoreNulls));
        TableMetadata tableMetadata = tableSchema.tableMetadata();

        WriteModification transformation =
            extension != null
            ? extension.beforeWrite(DefaultDynamoDbExtensionContext.builder()
                                                                   .items(itemMap)
                                                                   .operationContext(operationContext)
                                                                   .tableMetadata(tableMetadata)
                                                                   .tableSchema(tableSchema)
                                                                   .operationName(operationName())
                                                                   .build())
            : null;

        if (transformation != null && transformation.transformedItem() != null) {
            itemMap = transformation.transformedItem();
        }

        Collection<String> primaryKeys = tableSchema.tableMetadata().primaryKeys();

        Map<String, AttributeValue> keyAttributeValues = itemMap.entrySet().stream()
            .filter(entry -> primaryKeys.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        UpdateItemRequest.Builder requestBuilder = UpdateItemRequest.builder()
            .tableName(operationContext.tableName())
            .key(keyAttributeValues)
            .returnValues(ReturnValue.ALL_NEW);

        Map<String, AttributeValue> filteredAttributeValues = itemMap.entrySet().stream()
            .filter(entry -> !primaryKeys.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (request.left().isPresent()) {
            addPlainUpdateItemParameters(requestBuilder, request.left().get());
        }

        requestBuilder = addExpressionsIfExist(transformation, filteredAttributeValues, requestBuilder, tableMetadata);

        return requestBuilder.build();
    }

    @Override
    public UpdateItemEnhancedResponse<T> transformResponse(UpdateItemResponse response,
                               TableSchema<T> tableSchema,
                               OperationContext operationContext,
                               DynamoDbEnhancedClientExtension extension) {
        try {
            T attributes = readAndTransformSingleItem(response.attributes(), tableSchema, operationContext, extension);

            return UpdateItemEnhancedResponse.<T>builder(null)
                .attributes(attributes)
                .consumedCapacity(response.consumedCapacity())
                .itemCollectionMetrics(response.itemCollectionMetrics())
                .build();
        } catch (RuntimeException e) {
            // With a partial update it's possible to update the record into a state that the mapper can no longer
            // read or validate. This is more likely to happen with signed and encrypted records that undergo partial
            // updates (that practice is discouraged for this reason).
            throw new IllegalStateException("Unable to read the new item returned by UpdateItem after the update "
                                            + "occurred. Rollbacks are not supported by this operation, therefore the "
                                            + "record may no longer be readable using this model.", e);
        }
    }

    @Override
    public Function<UpdateItemRequest, UpdateItemResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::updateItem;
    }

    @Override
    public Function<UpdateItemRequest, CompletableFuture<UpdateItemResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::updateItem;
    }

    @Override
    public TransactWriteItem generateTransactWriteItem(TableSchema<T> tableSchema, OperationContext operationContext,
                                                       DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {
        UpdateItemRequest updateItemRequest = generateRequest(tableSchema, operationContext, dynamoDbEnhancedClientExtension);

        Update.Builder builder = Update.builder()
                                       .key(updateItemRequest.key())
                                       .tableName(updateItemRequest.tableName())
                                       .updateExpression(updateItemRequest.updateExpression())
                                       .conditionExpression(updateItemRequest.conditionExpression())
                                       .expressionAttributeValues(updateItemRequest.expressionAttributeValues())
                                       .expressionAttributeNames(updateItemRequest.expressionAttributeNames());

        request.right()
               .map(TransactUpdateItemEnhancedRequest::returnValuesOnConditionCheckFailureAsString)
               .ifPresent(builder::returnValuesOnConditionCheckFailure);

        return TransactWriteItem.builder()
                                .update(builder.build())
                                .build();
    }

    private UpdateItemRequest.Builder addExpressionsIfExist(WriteModification transformation,
                                                            Map<String, AttributeValue> filteredAttributeValues,
                                                            UpdateItemRequest.Builder requestBuilder,
                                                            TableMetadata tableMetadata) {
        Map<String, String> expressionNames = null;
        Map<String, AttributeValue> expressionValues = null;
        String conditionExpressionString = null;

        /* Add update expression for transformed non-key attributes if applicable */
        if (!filteredAttributeValues.isEmpty()) {
            Expression fullUpdateExpression = UpdateExpressionBuilder.generateUpdateExpression(filteredAttributeValues,
                                                                                               tableMetadata);
            expressionNames = fullUpdateExpression.expressionNames();
            expressionValues = fullUpdateExpression.expressionValues();
            requestBuilder = requestBuilder.updateExpression(fullUpdateExpression.expression());
        }

        /* Merge in conditional expression from extension WriteModification if applicable */
        if (transformation != null && transformation.additionalConditionalExpression() != null) {
            expressionNames =
                Expression.joinNames(expressionNames,
                                     transformation.additionalConditionalExpression().expressionNames());
            expressionValues =
                Expression.joinValues(expressionValues,
                                      transformation.additionalConditionalExpression().expressionValues());
            conditionExpressionString = transformation.additionalConditionalExpression().expression();
        }

        /* Merge in conditional expression from specified 'conditionExpression' if applicable */
        Expression conditionExpression = request.map(r -> Optional.ofNullable(r.conditionExpression()),
                                                     r -> Optional.ofNullable(r.conditionExpression()))
                                                .orElse(null);
        if (conditionExpression != null) {
            expressionNames = Expression.joinNames(expressionNames, conditionExpression.expressionNames());
            expressionValues = Expression.joinValues(expressionValues, conditionExpression.expressionValues());
            conditionExpressionString = Expression.joinExpressions(conditionExpressionString,
                                                                   conditionExpression.expression(), " AND ");
        }

        // Avoiding adding empty collections that the low level SDK will propagate to DynamoDb where it causes error.
        if (expressionNames != null && !expressionNames.isEmpty()) {
            requestBuilder = requestBuilder.expressionAttributeNames(expressionNames);
        }

        if (expressionValues != null && !expressionValues.isEmpty()) {
            requestBuilder = requestBuilder.expressionAttributeValues(expressionValues);
        }

        return requestBuilder.conditionExpression(conditionExpressionString);
    }

    private UpdateItemRequest.Builder addPlainUpdateItemParameters(UpdateItemRequest.Builder requestBuilder,
                                                                   UpdateItemEnhancedRequest<?> enhancedRequest) {
        requestBuilder = requestBuilder.returnConsumedCapacity(enhancedRequest.returnConsumedCapacityAsString());
        requestBuilder = requestBuilder.returnItemCollectionMetrics(enhancedRequest.returnItemCollectionMetricsAsString());
        return requestBuilder;
    }
}
