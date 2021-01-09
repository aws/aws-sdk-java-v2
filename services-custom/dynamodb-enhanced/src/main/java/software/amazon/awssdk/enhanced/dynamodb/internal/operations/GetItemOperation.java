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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;

@SdkInternalApi
public class GetItemOperation<T> implements TableOperation<T, GetItemRequest, GetItemResponse, T>,
                                            BatchableReadOperation,
                                            TransactableReadOperation<T> {

    private final GetItemEnhancedRequest request;

    private GetItemOperation(GetItemEnhancedRequest request) {
        this.request = request;
    }

    public static <T> GetItemOperation<T> create(GetItemEnhancedRequest request) {
        return new GetItemOperation<>(request);
    }

    @Override
    public Boolean consistentRead() {
        return this.request.consistentRead();
    }

    @Override
    public Key key() {
        return this.request.key();
    }

    @Override
    public GetItemRequest generateRequest(TableSchema<T> tableSchema,
                                          OperationContext context,
                                          DynamoDbEnhancedClientExtension extension) {
        if (!TableMetadata.primaryIndexName().equals(context.indexName())) {
            throw new IllegalArgumentException("GetItem cannot be executed against a secondary index.");
        }

        return GetItemRequest.builder()
                             .tableName(context.tableName())
                             .key(this.request.key().keyMap(tableSchema, context.indexName()))
                             .consistentRead(this.request.consistentRead())
                             .build();
    }

    @Override
    public T transformResponse(GetItemResponse response,
                               TableSchema<T> tableSchema,
                               OperationContext context,
                               DynamoDbEnhancedClientExtension extension) {
        return EnhancedClientUtils.readAndTransformSingleItem(response.item(), tableSchema, context, extension);
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
                                                   DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {
        return TransactGetItem.builder()
                              .get(Get.builder()
                                      .tableName(operationContext.tableName())
                                      .key(this.request.key().keyMap(tableSchema, operationContext.indexName()))
                                      .build())
                              .build();
    }

}
