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
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;

@SdkPublicApi
public class TransactWriteItems
    implements DatabaseOperation<TransactWriteItemsRequest, TransactWriteItemsResponse, Void> {

    private final List<WriteTransaction> writeTransactions;

    private TransactWriteItems(List<WriteTransaction> writeTransactions) {
        this.writeTransactions = writeTransactions;
    }

    public static TransactWriteItems create(List<WriteTransaction> writeTransactions) {
        return new TransactWriteItems(writeTransactions);
    }

    public static TransactWriteItems create(WriteTransaction... writeTransactions) {
        return new TransactWriteItems(Arrays.asList(writeTransactions));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().writeTransactions(this.writeTransactions);
    }

    @Override
    public TransactWriteItemsRequest generateRequest(MapperExtension mapperExtension) {
        List<TransactWriteItem> requestItems = writeTransactions.stream()
                                                                .map(WriteTransaction::generateRequest)
                                                                .collect(Collectors.toList());

        return TransactWriteItemsRequest.builder()
                                        .transactItems(requestItems)
                                        .build();
    }

    @Override
    public Void transformResponse(TransactWriteItemsResponse response, MapperExtension mapperExtension) {
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

    public List<WriteTransaction> writeTransactions() {
        return writeTransactions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactWriteItems that = (TransactWriteItems) o;

        return writeTransactions != null ? writeTransactions.equals(that.writeTransactions) : that.writeTransactions == null;
    }

    @Override
    public int hashCode() {
        return writeTransactions != null ? writeTransactions.hashCode() : 0;
    }

    public static final class Builder {
        private List<WriteTransaction> writeTransactions;

        private Builder() {
        }

        public Builder writeTransactions(List<WriteTransaction> writeTransactions) {
            this.writeTransactions = writeTransactions;
            return this;
        }

        public TransactWriteItems build() {
            return new TransactWriteItems(this.writeTransactions);
        }
    }
}
