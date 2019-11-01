/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Map;
import java.util.function.Function;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableWriteOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.WriteModification;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@SdkPublicApi
public class PutItem<T>
    implements BatchableWriteOperation<T>,
               TransactableWriteOperation<T>,
               TableOperation<T, PutItemRequest, PutItemResponse, Void> {

    private final T item;

    private PutItem(T item) {
        this.item = item;
    }

    public static <T> PutItem<T> of(T item) {
        return new PutItem<>(item);
    }

    public static GenericBuilder builder() {
        return new GenericBuilder();
    }

    public Builder<T> toBuilder() {
        return new Builder<T>().item(item);
    }

    @Override
    public PutItemRequest generateRequest(TableSchema<T> tableSchema,
                                          OperationContext operationContext,
                                          MapperExtension mapperExtension) {
        if (!TableMetadata.getPrimaryIndexName().equals(operationContext.getIndexName())) {
            throw new IllegalArgumentException("PutItem cannot be executed against a secondary index.");
        }

        // Redundant check for the existence of a partition key to avoid the call to DynamoDb and having it complain
        // instead
        tableSchema.getTableMetadata().getPrimaryPartitionKey();

        Map<String, AttributeValue> itemMap = tableSchema.itemToMap(item, true);     // always ignore nulls for putItem
        TableMetadata tableMetadata = tableSchema.getTableMetadata();

        // Allow a command mapperExtension to modify the attribute values of the item in the PutItemRequest and
        // add a conditional statement
        WriteModification transformation =
            mapperExtension != null ? mapperExtension.beforeWrite(itemMap, operationContext, tableMetadata) : null;

        if (transformation != null && transformation.getTransformedItem() != null) {
            itemMap = transformation.getTransformedItem();
        }

        PutItemRequest.Builder baseRequest = PutItemRequest.builder()
                             .tableName(operationContext.getTableName())
                             .item(itemMap);

        if (transformation != null && transformation.getAdditionalConditionalExpression() != null) {
            baseRequest = baseRequest.conditionExpression(
                transformation.getAdditionalConditionalExpression().getExpression())
                                     .expressionAttributeValues(
                    transformation.getAdditionalConditionalExpression().getExpressionValues())
                                     .expressionAttributeNames(
                    transformation.getAdditionalConditionalExpression().getExpressionNames());
        }

        return baseRequest.build();
    }

    @Override
    public Void transformResponse(PutItemResponse response,
                                  TableSchema<T> tableSchema,
                                  OperationContext operationContext,
                                  MapperExtension mapperExtension) {
        // No results are returned by this operation
        return null;
    }

    @Override
    public Function<PutItemRequest, PutItemResponse> getServiceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::putItem;
    }

    @Override
    public WriteRequest generateWriteRequest(TableSchema<T> tableSchema,
                                             OperationContext operationContext,
                                             MapperExtension mapperExtension) {
        PutItemRequest putItemRequest = generateRequest(tableSchema,
                                                        operationContext,
                                                        mapperExtension);

        if (putItemRequest.conditionExpression() != null) {
            throw new IllegalArgumentException("A mapper extension inserted a conditionExpression in a PutItem "
                                               + "request as part of a BatchWriteItemRequest. This is not supported by "
                                               + "DynamoDb. An extension known to do this is the "
                                               + "VersionedRecordExtension.");
        }

        return WriteRequest.builder().putRequest(PutRequest.builder().item(putItemRequest.item()).build()).build();
    }

    @Override
    public TransactWriteItem generateTransactWriteItem(TableSchema<T> tableSchema,
                                                       OperationContext operationContext,
                                                       MapperExtension mapperExtension) {
        PutItemRequest putItemRequest = generateRequest(tableSchema, operationContext, mapperExtension);

        Put put = Put.builder()
                     .item(putItemRequest.item())
                     .tableName(putItemRequest.tableName())
                     .conditionExpression(putItemRequest.conditionExpression())
                     .expressionAttributeValues(putItemRequest.expressionAttributeValues())
                     .expressionAttributeNames(putItemRequest.expressionAttributeNames())
                     .build();

        return TransactWriteItem.builder()
                                .put(put)
                                .build();
    }

    public T getItem() {
        return item;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PutItem<?> putItem = (PutItem<?>) o;

        return item != null ? item.equals(putItem.item) : putItem.item == null;
    }

    @Override
    public int hashCode() {
        return item != null ? item.hashCode() : 0;
    }

    public static class GenericBuilder {
        private GenericBuilder() {
        }

        public <T> Builder<T> item(T item) {
            return new Builder<T>().item(item);
        }

        public PutItem<?> build() {
            throw new UnsupportedOperationException("Cannot construct a PutItem operation without an item to put.");
        }
    }

    public static final class Builder<T> {
        private T item;

        private Builder() {
        }

        public Builder<T> item(T item) {
            this.item = item;
            return this;
        }

        public PutItem<T> build() {
            return new PutItem<>(item);
        }
    }
}
