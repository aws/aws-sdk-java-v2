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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.readAndTransformSingleItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@SdkPublicApi
public final class BatchWriteResult {
    private final Map<String, List<WriteRequest>> unprocessedRequests;

    private BatchWriteResult(Builder builder) {
        this.unprocessedRequests = Collections.unmodifiableMap(builder.unprocessedRequests);
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> List<T> unprocessedPutItemsForTable(DynamoDbTable<T> mappedTable) {
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

    public <T> List<T> unprocessedDeleteItemsForTable(DynamoDbTable<T> mappedTable) {
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

    public static final class Builder {
        private Map<String, List<WriteRequest>> unprocessedRequests;

        private Builder() {
        }

        public Builder unprocessedRequests(Map<String, List<WriteRequest>> unprocessedRequests) {
            this.unprocessedRequests =
                unprocessedRequests.entrySet()
                                   .stream()
                                   .collect(Collectors.toMap(
                                       Map.Entry::getKey,
                                       entry -> Collections.unmodifiableList(entry.getValue())));
            return this;
        }

        public BatchWriteResult build() {
            return new BatchWriteResult(this);
        }
    }
}
