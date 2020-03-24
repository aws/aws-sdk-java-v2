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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Document;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.internal.DefaultDocument;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsResponse;

@SdkInternalApi
public class TransactGetItemsOperation
    implements DatabaseOperation<TransactGetItemsRequest, TransactGetItemsResponse, List<Document>> {

    private TransactGetItemsEnhancedRequest request;

    private TransactGetItemsOperation(TransactGetItemsEnhancedRequest request) {
        this.request = request;
    }

    public static TransactGetItemsOperation create(TransactGetItemsEnhancedRequest request) {
        return new TransactGetItemsOperation(request);
    }

    @Override
    public TransactGetItemsRequest generateRequest(DynamoDbEnhancedClientExtension extension) {
        return TransactGetItemsRequest.builder()
                                      .transactItems(this.request.transactGetItems())
                                      .build();
    }

    @Override
    public Function<TransactGetItemsRequest, TransactGetItemsResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::transactGetItems;
    }

    @Override
    public Function<TransactGetItemsRequest, CompletableFuture<TransactGetItemsResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::transactGetItems;
    }

    @Override
    public List<Document> transformResponse(TransactGetItemsResponse response,
                                            DynamoDbEnhancedClientExtension extension) {
        return response.responses()
                       .stream()
                       .map(r -> r == null ? null : DefaultDocument.create(r.item()))
                       .collect(Collectors.toList());
    }

}
