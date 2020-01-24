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
import software.amazon.awssdk.extensions.dynamodb.mappingclient.BatchableReadOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TransactableReadOperation;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;

@SdkPublicApi
public class GetItem<T> implements TableOperation<T, GetItemRequest, GetItemResponse, T>,
                                   BatchableReadOperation,
                                   TransactableReadOperation<T> {

    private final Key key;
    private final Boolean consistentRead;

    private GetItem(Key key, Boolean consistentRead) {
        this.key = key;
        this.consistentRead = consistentRead;
    }

    public static <T> GetItem<T> create(Key key) {
        return new GetItem<>(key, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().key(key).consistentRead(consistentRead);
    }

    @Override
    public Boolean consistentRead() {
        return this.consistentRead;
    }

    @Override
    public Key key() {
        return this.key;
    }

    @Override
    public GetItemRequest generateRequest(TableSchema<T> tableSchema,
                                          OperationContext context,
                                          MapperExtension mapperExtension) {
        if (!TableMetadata.primaryIndexName().equals(context.indexName())) {
            throw new IllegalArgumentException("GetItem cannot be executed against a secondary index.");
        }

        return GetItemRequest.builder()
                             .tableName(context.tableName())
                             .key(key.keyMap(tableSchema, context.indexName()))
                             .consistentRead(consistentRead)
                             .build();
    }

    @Override
    public T transformResponse(GetItemResponse response,
                               TableSchema<T> tableSchema,
                               OperationContext context,
                               MapperExtension mapperExtension) {
        return readAndTransformSingleItem(response.item(), tableSchema, context, mapperExtension);
    }

    @Override
    public Function<GetItemRequest, GetItemResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::getItem;
    }

    @Override
    public Function<GetItemRequest, CompletableFuture<GetItemResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::getItem;
    }

    @Override
    public TransactGetItem generateTransactGetItem(TableSchema<T> tableSchema,
                                                   OperationContext operationContext,
                                                   MapperExtension mapperExtension) {
        return TransactGetItem.builder()
                              .get(Get.builder()
                                      .tableName(operationContext.tableName())
                                      .key(key.keyMap(tableSchema, operationContext.indexName()))
                                      .build())
                              .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GetItem<?> getItem = (GetItem<?>) o;

        if (key != null ? ! key.equals(getItem.key) : getItem.key != null) {
            return false;
        }
        return consistentRead != null ? consistentRead.equals(getItem.consistentRead) : getItem.consistentRead == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (consistentRead != null ? consistentRead.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private Key key;
        private Boolean consistentRead;

        private Builder() {
        }

        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        public <T> GetItem<T> build() {
            return new GetItem<>(this.key, this.consistentRead);
        }
    }
}
