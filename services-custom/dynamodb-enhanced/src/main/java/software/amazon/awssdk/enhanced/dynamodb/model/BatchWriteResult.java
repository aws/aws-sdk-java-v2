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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.createKeyFromMap;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.readAndTransformSingleItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

/**
 * Defines the result of the batchWriteItem() operation, such as
 * {@link DynamoDbEnhancedClient#batchWriteItem(BatchWriteItemEnhancedRequest)}. The result describes any unprocessed items
 * after the operation completes.
 * <ul>
 *     <li>Use the {@link #unprocessedPutItemsForTable(MappedTableResource)} method once for each table present in the request
 *  to get any unprocessed items from a put action on that table.</li>
 *     <li>Use the {@link #unprocessedDeleteItemsForTable(MappedTableResource)} method once for each table present in the request
 *  to get any unprocessed items from a delete action on that table.</li>
 * </ul>
 *
 */
@SdkPublicApi
public final class BatchWriteResult {
    private final Map<String, List<WriteRequest>> unprocessedRequests;

    private BatchWriteResult(Builder builder) {
        this.unprocessedRequests = Collections.unmodifiableMap(builder.unprocessedRequests);
    }

    /**
     * Creates a newly initialized builder for a request object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieve any unprocessed put action items belonging to the supplied table from the result .
     * Call this method once for each table present in the batch request.
     *
     * @param mappedTable the table to retrieve unprocessed items for
     * @param <T> the type of the table items
     * @return a list of items
     */
    public <T> List<T> unprocessedPutItemsForTable(MappedTableResource<T> mappedTable) {
        List<WriteRequest> writeRequests =
            unprocessedRequests.getOrDefault(mappedTable.tableName(),
                                             Collections.emptyList());

        return writeRequests.stream()
                            .filter(writeRequest -> writeRequest.putRequest() != null)
                            .map(WriteRequest::putRequest)
                            .map(PutRequest::item)
                            .map(item -> readAndTransformSingleItem(item,
                                                                    mappedTable.tableSchema(),
                                                                    DefaultOperationContext.create(mappedTable.tableName()),
                                                                    mappedTable.mapperExtension()))
                            .collect(Collectors.toList());
    }

    /**
     * Retrieve any unprocessed delete action keys belonging to the supplied table from the result.
     * Call this method once for each table present in the batch request.
     *
     * @param mappedTable the table to retrieve unprocessed items for.
     * @return a list of keys that were not processed as part of the batch request.
     */
    public List<Key> unprocessedDeleteItemsForTable(MappedTableResource<?> mappedTable) {
        List<WriteRequest> writeRequests =
            unprocessedRequests.getOrDefault(mappedTable.tableName(),
                                             Collections.emptyList());

        return writeRequests.stream()
                            .filter(writeRequest -> writeRequest.deleteRequest() != null)
                            .map(WriteRequest::deleteRequest)
                            .map(DeleteRequest::key)
                            .map(itemMap -> createKeyFromMap(itemMap,
                                                             mappedTable.tableSchema(),
                                                             TableMetadata.primaryIndexName()))
                            .collect(Collectors.toList());
    }

    /**
     * A builder that is used to create a result with the desired parameters.
     */
    public static final class Builder {
        private Map<String, List<WriteRequest>> unprocessedRequests;

        private Builder() {
        }

        /**
         * Add a map of unprocessed requests to this result object.
         *
         * @param unprocessedRequests the map of table to write request representing the unprocessed requests
         * @return a builder of this type
         */
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
