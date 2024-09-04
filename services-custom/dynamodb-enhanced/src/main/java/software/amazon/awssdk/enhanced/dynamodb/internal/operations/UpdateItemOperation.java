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
import static software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionUtils.operationExpression;
import static software.amazon.awssdk.utils.CollectionUtils.filterMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.internal.update.UpdateExpressionConverter;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.update.UpdateExpression;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Either;

@SdkInternalApi
public class UpdateItemOperation<T>
    implements TableOperation<T, UpdateItemRequest, UpdateItemResponse, UpdateItemEnhancedResponse<T>>,
               TransactableWriteOperation<T> {
    
    public static final String NESTED_OBJECT_UPDATE = "_NESTED_ATTR_UPDATE_";
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
        
        Map<String, AttributeValue> itemMapImmutable = tableSchema.itemToMap(item, Boolean.TRUE.equals(ignoreNulls));
        
        // If ignoreNulls is set to true, check for nested params to be updated
        // If needed, Transform itemMap for it to be able to handle them.
        Map<String, AttributeValue> itemMap = Boolean.TRUE.equals(ignoreNulls) ?
                                             transformItemToMapForUpdateExpression(itemMapImmutable) : itemMapImmutable;
        
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

        Map<String, AttributeValue> keyAttributes = filterMap(itemMap, entry -> primaryKeys.contains(entry.getKey()));
        Map<String, AttributeValue> nonKeyAttributes = filterMap(itemMap, entry -> !primaryKeys.contains(entry.getKey()));

        Expression updateExpression = generateUpdateExpressionIfExist(tableMetadata, transformation, nonKeyAttributes);
        Expression conditionExpression = generateConditionExpressionIfExist(transformation, request);

        Map<String, String> expressionNames = coalesceExpressionNames(updateExpression, conditionExpression);
        Map<String, AttributeValue> expressionValues = coalesceExpressionValues(updateExpression, conditionExpression);

        UpdateItemRequest.Builder requestBuilder = UpdateItemRequest.builder()
            .tableName(operationContext.tableName())
            .key(keyAttributes);

        if (request.left().isPresent()) {
            addPlainUpdateItemParameters(requestBuilder, request.left().get());
        }
        if (updateExpression != null) {
            requestBuilder.updateExpression(updateExpression.expression());
        }
        if (conditionExpression != null) {
            requestBuilder.conditionExpression(conditionExpression.expression());
        }
        if (CollectionUtils.isNotEmpty(expressionNames)) {
            requestBuilder = requestBuilder.expressionAttributeNames(expressionNames);
        }
        if (CollectionUtils.isNotEmpty(expressionValues)) {
            requestBuilder = requestBuilder.expressionAttributeValues(expressionValues);
        }

        return requestBuilder.build();
    }
    
    /**
     * Method checks if a nested object parameter requires an update
     * If so flattens out nested params separated by "_NESTED_ATTR_UPDATE_"
     * this is consumed by @link EnhancedClientUtils to form the appropriate UpdateExpression
     */
    public Map<String, AttributeValue> transformItemToMapForUpdateExpression(Map<String, AttributeValue> itemToMap) {
        
        Map<String, AttributeValue> nestedAttributes = new HashMap<>();
        
        itemToMap.forEach((key, value) -> {
            if (value.hasM() && isNotEmptyMap(value.m())) {
                nestedAttributes.put(key, value);
            }
        });
        
        if (!nestedAttributes.isEmpty()) {
            Map<String, AttributeValue> itemToMapMutable = new HashMap<>(itemToMap);
            nestedAttributes.forEach((key, value) -> {
                itemToMapMutable.remove(key);
                nestedItemToMap(itemToMapMutable, key, value);
            });
            return itemToMapMutable;
        }
        
        return itemToMap;
    }
    
    private Map<String, AttributeValue> nestedItemToMap(Map<String, AttributeValue> itemToMap,
                                 String key,
                                 AttributeValue attributeValue) {
        attributeValue.m().forEach((mapKey, mapValue) -> {
            String nestedAttributeKey = key + NESTED_OBJECT_UPDATE + mapKey;
            if (attributeValueNonNullOrShouldWriteNull(mapValue)) {
                if (mapValue.hasM()) {
                    nestedItemToMap(itemToMap, nestedAttributeKey, mapValue);
                } else {
                    itemToMap.put(nestedAttributeKey, mapValue);
                }
            }
        });
        return itemToMap;
    }

    private boolean isNotEmptyMap(Map<String, AttributeValue> map) {
        return !map.isEmpty() && map.values().stream()
                                    .anyMatch(this::attributeValueNonNullOrShouldWriteNull);
    }
    
    private boolean attributeValueNonNullOrShouldWriteNull(AttributeValue attributeValue) {
        return !isNullAttributeValue(attributeValue);
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

    /**
     * Retrieves the UpdateExpression from extensions if existing, and then creates an UpdateExpression for the request POJO
     * if there are attributes to be updated (most likely). If both exist, they are merged and the code generates a final
     * Expression that represent the result.
     */
    private Expression generateUpdateExpressionIfExist(TableMetadata tableMetadata,
                                                       WriteModification transformation,
                                                       Map<String, AttributeValue> attributes) {
        UpdateExpression updateExpression = null;
        if (transformation != null && transformation.updateExpression() != null) {
            updateExpression = transformation.updateExpression();
        }
        if (!attributes.isEmpty()) {
            List<String> nonRemoveAttributes = UpdateExpressionConverter.findAttributeNames(updateExpression);
            UpdateExpression operationUpdateExpression = operationExpression(attributes, tableMetadata, nonRemoveAttributes);
            if (updateExpression == null) {
                updateExpression = operationUpdateExpression;
            } else {
                updateExpression = UpdateExpression.mergeExpressions(updateExpression, operationUpdateExpression);
            }
        }
        return UpdateExpressionConverter.toExpression(updateExpression);
    }

    /**
     * Retrieves the ConditionExpression from extensions if existing, and retrieves the ConditionExpression from the request
     * if existing. If both exist, they are merged.
     */
    private Expression generateConditionExpressionIfExist(
            WriteModification transformation,
            Either<UpdateItemEnhancedRequest<T>, TransactUpdateItemEnhancedRequest<T>> request) {

        Expression conditionExpression = null;

        if (transformation != null && transformation.additionalConditionalExpression() != null) {
            conditionExpression = transformation.additionalConditionalExpression();
        }

        Expression operationConditionExpression = request.map(r -> Optional.ofNullable(r.conditionExpression()),
                                                              r -> Optional.ofNullable(r.conditionExpression()))
                                                         .orElse(null);
        if (operationConditionExpression != null) {
            conditionExpression = operationConditionExpression.and(conditionExpression);
        }
        return conditionExpression;
    }

    private UpdateItemRequest.Builder addPlainUpdateItemParameters(UpdateItemRequest.Builder requestBuilder,
                                                                   UpdateItemEnhancedRequest<?> enhancedRequest) {
        requestBuilder = requestBuilder.returnConsumedCapacity(enhancedRequest.returnConsumedCapacityAsString());
        requestBuilder = requestBuilder.returnItemCollectionMetrics(enhancedRequest.returnItemCollectionMetricsAsString());
        requestBuilder =
            requestBuilder.returnValuesOnConditionCheckFailure(enhancedRequest.returnValuesOnConditionCheckFailureAsString());

        // Set the default returnValue to ReturnValue.ALL_NEW if not specified
        ReturnValue returnValue = enhancedRequest.returnValuesAsString() == null
                                  ? ReturnValue.ALL_NEW
                                  : ReturnValue.fromValue(enhancedRequest.returnValuesAsString());

        requestBuilder.returnValues(returnValue.toString());

        return requestBuilder;
    }

    private static Map<String, String> coalesceExpressionNames(Expression firstExpression, Expression secondExpression) {
        Map<String, String> expressionNames = null;
        if (firstExpression != null && !CollectionUtils.isNullOrEmpty(firstExpression.expressionNames())) {
            expressionNames = firstExpression.expressionNames();
        }
        if (secondExpression != null && !CollectionUtils.isNullOrEmpty(secondExpression.expressionNames())) {
            expressionNames = Expression.joinNames(expressionNames, secondExpression.expressionNames());
        }
        return expressionNames;
    }

    private static Map<String, AttributeValue> coalesceExpressionValues(Expression firstExpression, Expression secondExpression) {
        Map<String, AttributeValue> expressionValues = null;
        if (firstExpression != null && !CollectionUtils.isNullOrEmpty(firstExpression.expressionValues())) {
            expressionValues = firstExpression.expressionValues();
        }
        if (secondExpression != null && !CollectionUtils.isNullOrEmpty(secondExpression.expressionValues())) {
            expressionValues = Expression.joinValues(expressionValues, secondExpression.expressionValues());
        }
        return expressionValues;
    }
}
