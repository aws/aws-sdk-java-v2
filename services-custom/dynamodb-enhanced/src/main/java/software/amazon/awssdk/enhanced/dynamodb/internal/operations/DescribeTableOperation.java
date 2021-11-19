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
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTableEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;

@SdkInternalApi
public class DescribeTableOperation<T> implements TableOperation<T, DescribeTableRequest, DescribeTableResponse,
    DescribeTableEnhancedResponse> {

    public static <T> DescribeTableOperation<T> create() {
        return new DescribeTableOperation<>();
    }

    @Override
    public OperationName operationName() {
        return OperationName.DESCRIBE_TABLE;
    }

    @Override
    public DescribeTableRequest generateRequest(TableSchema<T> tableSchema,
                                                OperationContext operationContext,
                                                DynamoDbEnhancedClientExtension extension) {

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            throw new IllegalArgumentException("DescribeTable cannot be executed against a secondary index.");
        }
        return DescribeTableRequest.builder()
                                   .tableName(operationContext.tableName())
                                   .build();
    }

    @Override
    public Function<DescribeTableRequest, DescribeTableResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::describeTable;
    }

    @Override
    public Function<DescribeTableRequest, CompletableFuture<DescribeTableResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::describeTable;
    }

    @Override
    public DescribeTableEnhancedResponse transformResponse(DescribeTableResponse response,
                                                           TableSchema<T> tableSchema,
                                                           OperationContext operationContext,
                                                           DynamoDbEnhancedClientExtension extension) {
        return DescribeTableEnhancedResponse.builder()
                                            .response(response)
                                            .build();
    }
}
