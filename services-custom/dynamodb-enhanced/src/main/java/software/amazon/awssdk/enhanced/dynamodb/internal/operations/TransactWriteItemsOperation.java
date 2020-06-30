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
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

@SdkInternalApi
public class TransactWriteItemsOperation
    implements DatabaseOperation<TransactWriteItemsRequest, TransactWriteItemsResponse, Void> {

    private TransactWriteItemsEnhancedRequest request;

    private TransactWriteItemsOperation(TransactWriteItemsEnhancedRequest request) {
        this.request = request;
    }

    public static TransactWriteItemsOperation create(TransactWriteItemsEnhancedRequest request) {
        return new TransactWriteItemsOperation(request);
    }

    @Override
    public TransactWriteItemsRequest generateRequest(DynamoDbEnhancedClientExtension extension) {
        return TransactWriteItemsRequest.builder()
                                        .transactItems(this.request.transactWriteItems())
                                        .clientRequestToken(this.request.clientRequestToken())
                                        .build();
    }

    @Override
    public Void transformResponse(TransactWriteItemsResponse response, DynamoDbEnhancedClientExtension extension) {
        return null;        // this operation does not return results
    }

    @Override
    public Function<TransactWriteItemsRequest, TransactWriteItemsResponse> serviceCall(
        DynamoDbClient dynamoDbClient) {

        return dynamoDbClient::transactWriteItems;
    }

    @Override
    public Function<TransactWriteItemsRequest, CompletableFuture<TransactWriteItemsResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::transactWriteItems;
    }

}
