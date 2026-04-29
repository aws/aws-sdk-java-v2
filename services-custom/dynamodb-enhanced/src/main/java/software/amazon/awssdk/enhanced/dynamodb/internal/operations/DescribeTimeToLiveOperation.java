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
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DescribeTimeToLiveEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTimeToLiveResponse;

@SdkInternalApi
public class DescribeTimeToLiveOperation<T> implements TableOperation<T, DescribeTimeToLiveRequest, DescribeTimeToLiveResponse,
    DescribeTimeToLiveEnhancedResponse> {

    public static <T> DescribeTimeToLiveOperation<T> create() {
        return new DescribeTimeToLiveOperation<>();
    }

    @Override
    public OperationName operationName() {
        return OperationName.DESCRIBE_TIME_TO_LIVE;
    }

    @Override
    public DescribeTimeToLiveRequest generateRequest(TableSchema<T> tableSchema,
                                                     OperationContext operationContext,
                                                     DynamoDbEnhancedClientExtension extension) {
        return DescribeTimeToLiveRequest.builder()
                                        .tableName(operationContext.tableName())
                                        .build();
    }

    @Override
    public Function<DescribeTimeToLiveRequest, DescribeTimeToLiveResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::describeTimeToLive;
    }

    @Override
    public Function<DescribeTimeToLiveRequest, CompletableFuture<DescribeTimeToLiveResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::describeTimeToLive;
    }

    @Override
    public DescribeTimeToLiveEnhancedResponse transformResponse(DescribeTimeToLiveResponse response,
                                                                TableSchema<T> tableSchema,
                                                                OperationContext operationContext,
                                                                DynamoDbEnhancedClientExtension extension) {
        return DescribeTimeToLiveEnhancedResponse.builder()
                                                 .response(response)
                                                 .build();
    }
}
