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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.isNullAttributeValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.cleanAttributeName;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.readAndTransformSingleItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.WriteModification;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@SdkInternalApi
public class UpdateItemOperation<T>
    implements TableOperation<T, UpdateItemRequest, UpdateItemResponse, T>,
               TransactableWriteOperation<T> {

    private static final Function<String, String> EXPRESSION_VALUE_KEY_MAPPER =
        key -> ":AMZN_MAPPED_" + cleanAttributeName(key);

    private static final Function<String, String> EXPRESSION_KEY_MAPPER =
        key -> "#AMZN_MAPPED_" + cleanAttributeName(key);

    private final UpdateItemEnhancedRequest<T> request;

    private UpdateItemOperation(UpdateItemEnhancedRequest<T> request) {
        this.request = request;
    }

    public static <T> UpdateItemOperation<T> create(UpdateItemEnhancedRequest<T> request) {
        return new UpdateItemOperation<>(request);
    }

    @Override
    public UpdateItemRequest generateRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             MapperExtension mapperExtension) {
        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("UpdateItem cannot be executed against a secondary index.");
        }

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(this.request.item(),
                                                                    Boolean.TRUE.equals(this.request.ignoreNulls()));
        TableMetadata tableMetadata = tableSchema.tableMetadata();

        WriteModification transformation =
            mapperExtension != null ? mapperExtension.beforeWrite(itemMap, operationContext, tableMetadata) : null;

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

        requestBuilder = addExpressionsIfExist(transformation, filteredAttributeValues, requestBuilder);

        return requestBuilder.build();
    }

    @Override
    public T transformResponse(UpdateItemResponse response,
                               TableSchema<T> tableSchema,
                               OperationContext operationContext,
                               MapperExtension mapperExtension) {
        try {
            return readAndTransformSingleItem(response.attributes(), tableSchema, operationContext, mapperExtension);
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
                                                       MapperExtension mapperExtension) {
        UpdateItemRequest updateItemRequest = generateRequest(tableSchema, operationContext, mapperExtension);

        Update update = Update.builder()
                              .key(updateItemRequest.key())
                              .tableName(updateItemRequest.tableName())
                              .updateExpression(updateItemRequest.updateExpression())
                              .conditionExpression(updateItemRequest.conditionExpression())
                              .expressionAttributeValues(updateItemRequest.expressionAttributeValues())
                              .expressionAttributeNames(updateItemRequest.expressionAttributeNames())
                              .build();

        return TransactWriteItem.builder()
                                .update(update)
                                .build();
    }

    private static Expression generateUpdateExpression(Map<String, AttributeValue> attributeValuesToUpdate) {
        // Sort the updates into 'SET' or 'REMOVE' based on null value
        List<String> updateSetActions = new ArrayList<>();
        List<String> updateRemoveActions = new ArrayList<>();

        attributeValuesToUpdate.forEach((key, value) -> {
            if (!isNullAttributeValue(value)) {
                updateSetActions.add(EXPRESSION_KEY_MAPPER.apply(key) + " = " + EXPRESSION_VALUE_KEY_MAPPER.apply(key));
            } else {
                updateRemoveActions.add(EXPRESSION_KEY_MAPPER.apply(key));
            }
        });

        // Combine the expressions
        List<String> updateActions = new ArrayList<>();

        if (!updateSetActions.isEmpty()) {
            updateActions.add("SET " + String.join(", ", updateSetActions));
        }

        if (!updateRemoveActions.isEmpty()) {
            updateActions.add("REMOVE " + String.join(", ", updateRemoveActions));
        }

        String updateExpression = String.join(" ", updateActions);

        Map<String, AttributeValue> expressionAttributeValues =
            attributeValuesToUpdate.entrySet()
                                   .stream()
                                   .filter(entry -> !isNullAttributeValue(entry.getValue()))
                                   .collect(Collectors.toMap(
                                       entry -> EXPRESSION_VALUE_KEY_MAPPER.apply(entry.getKey()),
                                       Map.Entry::getValue));

        Map<String, String> expressionAttributeNames =
            attributeValuesToUpdate.keySet()
                                   .stream()
                                   .collect(Collectors.toMap(EXPRESSION_KEY_MAPPER, key -> key));

        return Expression.builder()
                         .expression(updateExpression)
                         .expressionValues(Collections.unmodifiableMap(expressionAttributeValues))
                         .expressionNames(expressionAttributeNames)
                         .build();
    }

    private UpdateItemRequest.Builder addExpressionsIfExist(WriteModification transformation,
                                                            Map<String, AttributeValue> filteredAttributeValues,
                                                            UpdateItemRequest.Builder requestBuilder) {
        Map<String, String> expressionNames = null;
        Map<String, AttributeValue> expressionValues = null;
        String conditionExpressionString = null;

        /* Add update expression for transformed non-key attributes if applicable */
        if (!filteredAttributeValues.isEmpty()) {
            Expression fullUpdateExpression = generateUpdateExpression(filteredAttributeValues);
            expressionNames = fullUpdateExpression.expressionNames();
            expressionValues = fullUpdateExpression.expressionValues();
            requestBuilder = requestBuilder.updateExpression(fullUpdateExpression.expression());
        }

        /* Merge in conditional expression from extension WriteModification if applicable */
        if (transformation != null && transformation.additionalConditionalExpression() != null) {
            expressionNames =
                Expression.coalesceNames(expressionNames,
                                         transformation.additionalConditionalExpression().expressionNames());
            expressionValues =
                Expression.coalesceValues(expressionValues,
                                          transformation.additionalConditionalExpression().expressionValues());
            conditionExpressionString = transformation.additionalConditionalExpression().expression();
        }

        /* Merge in conditional expression from specified 'conditionExpression' if applicable */
        if (this.request.conditionExpression() != null) {
            expressionNames = Expression.coalesceNames(expressionNames, this.request.conditionExpression().expressionNames());
            expressionValues = Expression.coalesceValues(expressionValues, this.request.conditionExpression().expressionValues());
            conditionExpressionString = Expression.coalesceExpressions(conditionExpressionString,
                                                                       this.request.conditionExpression().expression(), " AND ");
        }

        // Avoiding adding empty collections that the low level SDK will propagate to DynamoDB where it causes error.
        if (expressionNames != null && !expressionNames.isEmpty()) {
            requestBuilder = requestBuilder.expressionAttributeNames(expressionNames);
        }

        if (expressionValues != null && !expressionValues.isEmpty()) {
            requestBuilder = requestBuilder.expressionAttributeValues(expressionValues);
        }

        return requestBuilder.conditionExpression(conditionExpressionString);
    }

}
