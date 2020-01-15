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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DatabaseOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItemsResponse;

@SdkPublicApi
public class TransactGetItems
    implements DatabaseOperation<TransactGetItemsRequest, TransactGetItemsResponse, List<UnmappedItem>> {

    private final List<ReadTransaction> readTransactions;

    private TransactGetItems(List<ReadTransaction> readTransactions) {
        this.readTransactions = readTransactions;
    }

    public static TransactGetItems create(List<ReadTransaction> transactGetRequests) {
        return new TransactGetItems(transactGetRequests);
    }

    public static TransactGetItems create(ReadTransaction... readTransactions) {
        return new TransactGetItems(Arrays.asList(readTransactions));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().readTransactions(readTransactions);
    }

    @Override
    public TransactGetItemsRequest generateRequest(MapperExtension mapperExtension) {
        return TransactGetItemsRequest.builder()
                                      .transactItems(readTransactions.stream()
                                                                     .map(ReadTransaction::generateTransactGetItem)
                                                                     .collect(Collectors.toList()))
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
    public List<UnmappedItem> transformResponse(TransactGetItemsResponse response, MapperExtension mapperExtension) {
        return response.responses()
                       .stream()
                       .map(r -> r == null ? null : UnmappedItem.create(r.item()))
                       .collect(Collectors.toList());
    }

    public List<ReadTransaction> readTransactions() {
        return readTransactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactGetItems that = (TransactGetItems) o;

        return readTransactions != null ? readTransactions.equals(that.readTransactions) : that.readTransactions == null;
    }

    @Override
    public int hashCode() {
        return readTransactions != null ? readTransactions.hashCode() : 0;
    }

    public static final class Builder {
        private List<ReadTransaction> readTransactions;

        private Builder() {
        }

        public Builder readTransactions(List<ReadTransaction> readTransactions) {
            this.readTransactions = readTransactions;
            return this;
        }

        public TransactGetItems build() {
            return new TransactGetItems(readTransactions);
        }
    }
}
