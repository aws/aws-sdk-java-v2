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

import static java.util.Collections.emptyList;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.readAndTransformSingleItem;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;
import software.amazon.awssdk.enhanced.dynamodb.internal.operations.DefaultOperationContext;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;

/**
 * Defines one result page with retrieved items in the result of a batchGetItem() operation, such as
 * {@link DynamoDbEnhancedClient#batchGetItem(BatchGetItemEnhancedRequest)}.
 * <p>
 * Use the {@link #resultsForTable(MappedTableResource)} method once for each table present in the request
 * to retrieve items from that table in the page.
 */
@SdkPublicApi
public final class BatchGetResultPage {
    private final BatchGetItemResponse batchGetItemResponse;
    private final DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension;

    private BatchGetResultPage(Builder builder) {
        this.batchGetItemResponse = builder.batchGetItemResponse;
        this.dynamoDbEnhancedClientExtension = builder.dynamoDbEnhancedClientExtension;
    }

    /**
     * Creates a newly initialized builder for a result object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieve all items on this result page belonging to the supplied table. Call this method once for each table present in the
     * batch request.
     *
     * @param mappedTable the table to retrieve items for
     * @param <T> the type of the table items
     * @return a list of items
     */
    public <T> List<T> resultsForTable(MappedTableResource<T> mappedTable) {
        List<Map<String, AttributeValue>> results =
            batchGetItemResponse.responses()
                                .getOrDefault(mappedTable.tableName(), emptyList());

        return results.stream()
                      .map(itemMap -> readAndTransformSingleItem(itemMap,
                                                                 mappedTable.tableSchema(),
                                                                 DefaultOperationContext.create(mappedTable.tableName()),
                                                                 dynamoDbEnhancedClientExtension))
                      .collect(Collectors.toList());
    }

    /**
     * A builder that is used to create a result object with the desired parameters.
     */
    public static final class Builder {

        private BatchGetItemResponse batchGetItemResponse;
        private DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension;

        private Builder() {
        }

        /**
         * Adds a response to the result object. Required.
         *
         * @param batchGetItemResponse
         * @return a builder of this type
         */
        public Builder batchGetItemResponse(BatchGetItemResponse batchGetItemResponse) {
            this.batchGetItemResponse = batchGetItemResponse;
            return this;
        }

        /**
         * Adds a mapper extension that can be used to modify the values read from the database.
         * @see DynamoDbEnhancedClientExtension
         *
         * @param dynamoDbEnhancedClientExtension the supplied mapper extension
         * @return a builder of this type
         */
        public Builder mapperExtension(DynamoDbEnhancedClientExtension dynamoDbEnhancedClientExtension) {
            this.dynamoDbEnhancedClientExtension = dynamoDbEnhancedClientExtension;
            return this;
        }

        public BatchGetResultPage build() {
            return new BatchGetResultPage(this);
        }
    }
}
