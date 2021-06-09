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
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableResponse;


@SdkInternalApi
public class DeleteTableOperation<T> implements TableOperation<T, DeleteTableRequest, DeleteTableResponse, Void> {

    public static <T> DeleteTableOperation<T> create() {
        return new DeleteTableOperation<>();
    }

    @Override
    public DeleteTableRequest generateRequest(TableSchema<T> tableSchema,
                                              OperationContext operationContext,
                                              DynamoDbEnhancedClientExtension extension) {

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("DeleteTable cannot be executed against a secondary index.");
        }
        return DeleteTableRequest.builder()
                .tableName(operationContext.tableName())
                .build();
    }

    @Override
    public Function<DeleteTableRequest, DeleteTableResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::deleteTable;
    }

    @Override
    public Function<DeleteTableRequest, CompletableFuture<DeleteTableResponse>> asyncServiceCall(
            DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::deleteTable;
    }

    @Override
    public Void transformResponse(DeleteTableResponse response,
                                  TableSchema<T> tableSchema,
                                  OperationContext operationContext,
                                  DynamoDbEnhancedClientExtension extension) {
        // This operation does not return results
        return null;
    }


}
