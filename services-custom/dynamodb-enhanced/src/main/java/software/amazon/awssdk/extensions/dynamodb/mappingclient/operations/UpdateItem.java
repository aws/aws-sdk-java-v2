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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.WriteModification;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

@SdkPublicApi
public class UpdateItem<T>
    implements TableOperation<T, UpdateItemRequest, UpdateItemResponse, T>,
               TransactableWriteOperation<T> {

    private static final Function<String, String> EXPRESSION_VALUE_KEY_MAPPER =
        key -> ":AMZN_MAPPED_" + cleanAttributeName(key);

    private static final Function<String, String> EXPRESSION_KEY_MAPPER =
        key -> "#AMZN_MAPPED_" + cleanAttributeName(key);

    private final T item;
    private final Boolean ignoreNulls;
    private final Expression conditionExpression;

    private UpdateItem(Builder<T> b) {
        this.item = b.item;
        this.ignoreNulls = b.ignoreNulls;
        this.conditionExpression = b.conditionExpression;
    }

    public static <T> UpdateItem<T> of(T item) {
        return UpdateItem.builder().item(item).build();
    }

    public static GenericBuilder builder() {
        return new GenericBuilder();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>().item(item).ignoreNulls(ignoreNulls);
    }

    @Override
    public UpdateItemRequest generateRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             MapperExtension mapperExtension) {
        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("UpdateItem cannot be executed against a secondary index.");
        }

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(item,  Boolean.TRUE.equals(ignoreNulls));
        TableMetadata tableMetadata = tableSchema.tableMetadata();

        // Allow a command mapperExtension to modify the attribute values of the item in the PutItemRequest and add
        // a conditional statement
        WriteModification transformation =
            mapperExtension != null ? mapperExtension.beforeWrite(itemMap, operationContext, tableMetadata) : null;

        if (transformation != null && transformation.transformedItem() != null) {
            itemMap = transformation.transformedItem();
        }

        Collection<String> primaryKeys = tableSchema.tableMetadata().primaryKeys();
        Map<String, AttributeValue> filteredAttributeValues = itemMap.entrySet().stream()
            .filter(entry -> !primaryKeys.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, AttributeValue> keyAttributeValues = itemMap.entrySet().stream()
            .filter(entry -> primaryKeys.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        UpdateItemRequest.Builder baseUpdateItemRequest = UpdateItemRequest.builder()
            .tableName(operationContext.tableName())
            .key(keyAttributeValues)
            .returnValues(ReturnValue.ALL_NEW);

        Map<String, String> expressionNames = null;
        Map<String, AttributeValue> expressionValues = null;
        String conditionExpressionString = null;

        /* Add update expression for transformed non-key attributes if applicable */
        if (!filteredAttributeValues.isEmpty()) {
            Expression fullUpdateExpression = generateUpdateExpression(filteredAttributeValues);
            expressionNames = fullUpdateExpression.expressionNames();
            expressionValues = fullUpdateExpression.expressionValues();
            baseUpdateItemRequest = baseUpdateItemRequest.updateExpression(fullUpdateExpression.expression());
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
        if (conditionExpression != null) {
            expressionNames = Expression.coalesceNames(expressionNames, conditionExpression.expressionNames());
            expressionValues = Expression.coalesceValues(expressionValues, conditionExpression.expressionValues());
            conditionExpressionString = Expression.coalesceExpressions(conditionExpressionString,
                                                                       conditionExpression.expression(), " AND ");
        }

        // The SDK handles collections a little weirdly. Avoiding adding empty collections
        if (expressionNames != null && !expressionNames.isEmpty()) {
            baseUpdateItemRequest = baseUpdateItemRequest.expressionAttributeNames(expressionNames);
        }

        if (expressionValues != null && !expressionValues.isEmpty()) {
            baseUpdateItemRequest = baseUpdateItemRequest.expressionAttributeValues(expressionValues);
        }

        return baseUpdateItemRequest.conditionExpression(conditionExpressionString)
                                    .build();
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

    public T item() {
        return item;
    }

    public Boolean ignoreNulls() {
        return ignoreNulls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpdateItem<?> that = (UpdateItem<?>) o;

        if (item != null ? ! item.equals(that.item) : that.item != null) {
            return false;
        }
        return ignoreNulls != null ? ignoreNulls.equals(that.ignoreNulls) : that.ignoreNulls == null;
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + (ignoreNulls != null ? ignoreNulls.hashCode() : 0);
        return result;
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

    public static class GenericBuilder {
        private Boolean ignoreNulls;
        private Expression conditionExpression;

        private GenericBuilder() {
        }

        public GenericBuilder ignoreNulls(Boolean ignoreNulls) {
            this.ignoreNulls = ignoreNulls;
            return this;
        }

        public GenericBuilder conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        public <T> Builder<T> item(T item) {
            return new Builder<T>().item(item).ignoreNulls(ignoreNulls).conditionExpression(conditionExpression);
        }

        public UpdateItem<?> build() {
            throw new UnsupportedOperationException("Cannot construct a UpdateItem operation without an item to put.");
        }
    }

    public static final class Builder<T> {
        private T item;
        private Boolean ignoreNulls;
        private Expression conditionExpression;

        private Builder() {
        }

        public Builder<T> ignoreNulls(Boolean ignoreNulls) {
            this.ignoreNulls = ignoreNulls;
            return this;
        }

        public Builder<T> conditionExpression(Expression conditionExpression) {
            this.conditionExpression = conditionExpression;
            return this;
        }

        public Builder<T> item(T item) {
            this.item = item;
            return this;
        }

        public UpdateItem<T> build() {
            return new UpdateItem<>(this);
        }
    }
}
