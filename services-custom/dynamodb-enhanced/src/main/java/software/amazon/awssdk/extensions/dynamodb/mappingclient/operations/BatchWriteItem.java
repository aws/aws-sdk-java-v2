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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DatabaseOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@SdkPublicApi
public class BatchWriteItem
    implements DatabaseOperation<BatchWriteItemRequest,
                                 BatchWriteItemResponse,
                                 BatchWriteItem.BatchWriteItemResults> {

    private final Collection<WriteBatch> writeBatches;

    private BatchWriteItem(Collection<WriteBatch> writeBatches) {
        this.writeBatches = writeBatches;
    }

    public static BatchWriteItem create(Collection<WriteBatch> writeBatches) {
        return new BatchWriteItem(writeBatches);
    }

    public static BatchWriteItem create(WriteBatch... writeBatches) {
        return new BatchWriteItem(Arrays.asList(writeBatches));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().writeBatches(writeBatches);
    }

    @Override
    public BatchWriteItemRequest generateRequest(MapperExtension mapperExtension) {
        Map<String, Collection<WriteRequest>> requestItems = new HashMap<>();
        writeBatches.forEach(writeBatch -> writeBatch.addWriteRequestsToMap(requestItems));
        return BatchWriteItemRequest.builder()
                                    .requestItems(Collections.unmodifiableMap(requestItems))
                                    .build();
    }

    @Override
    public BatchWriteItemResults transformResponse(BatchWriteItemResponse response, MapperExtension mapperExtension) {
        return new BatchWriteItemResults(response.unprocessedItems());
    }

    @Override
    public Function<BatchWriteItemRequest, BatchWriteItemResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::batchWriteItem;
    }

    @Override
    public Function<BatchWriteItemRequest, CompletableFuture<BatchWriteItemResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {

        return dynamoDbAsyncClient::batchWriteItem;
    }

    public Collection<WriteBatch> writeBatches() {
        return writeBatches;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchWriteItem that = (BatchWriteItem) o;

        return writeBatches != null ? writeBatches.equals(that.writeBatches) : that.writeBatches == null;
    }

    @Override
    public int hashCode() {
        return writeBatches != null ? writeBatches.hashCode() : 0;
    }

    public static final class Builder {
        private Collection<WriteBatch> writeBatches;

        private Builder() {
        }

        public Builder writeBatches(Collection<WriteBatch> writeBatches) {
            this.writeBatches = writeBatches;
            return this;
        }

        public BatchWriteItem build() {
            return new BatchWriteItem(writeBatches);
        }
    }

    public static class BatchWriteItemResults {
        private final Map<String, List<WriteRequest>> unprocessedRequests;

        private BatchWriteItemResults(Map<String, List<WriteRequest>> unprocessedRequests) {
            this.unprocessedRequests = unprocessedRequests;
        }

        public <T> List<T> unprocessedPutItemsForTable(MappedTable<T> mappedTable) {
            List<WriteRequest> writeRequests =
                unprocessedRequests.getOrDefault(mappedTable.tableName(),
                                                 Collections.emptyList());

            return writeRequests.stream()
                                .filter(writeRequest -> writeRequest.putRequest() != null)
                                .map(WriteRequest::putRequest)
                                .map(PutRequest::item)
                                .map(item -> readAndTransformSingleItem(item,
                                                                        mappedTable.tableSchema(),
                                                                        OperationContext.create(mappedTable.tableName()),
                                                                        mappedTable.mapperExtension()))
                                .collect(Collectors.toList());
        }

        public <T> List<T> unprocessedDeleteItemsForTable(MappedTable<T> mappedTable) {
            List<WriteRequest> writeRequests =
                unprocessedRequests.getOrDefault(mappedTable.tableName(),
                                                 Collections.emptyList());

            return writeRequests.stream()
                                .filter(writeRequest -> writeRequest.deleteRequest() != null)
                                .map(WriteRequest::deleteRequest)
                                .map(DeleteRequest::key)
                                .map(itemMap -> mappedTable.tableSchema().mapToItem(itemMap))
                                .collect(Collectors.toList());
        }
    }
}
