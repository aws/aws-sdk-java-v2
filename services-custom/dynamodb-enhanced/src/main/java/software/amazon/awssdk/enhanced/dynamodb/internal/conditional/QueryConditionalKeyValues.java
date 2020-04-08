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

package software.amazon.awssdk.enhanced.dynamodb.internal.conditional;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Internal helper class to act as a struct to store specific key values that are used throughout various
 * {@link software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional} implementations.
 */
@SdkInternalApi
class QueryConditionalKeyValues {
    private final String partitionKey;
    private final AttributeValue partitionValue;
    private final String sortKey;
    private final AttributeValue sortValue;

    private QueryConditionalKeyValues(String partitionKey,
                                      AttributeValue partitionValue,
                                      String sortKey,
                                      AttributeValue sortValue) {
        this.partitionKey = partitionKey;
        this.partitionValue = partitionValue;
        this.sortKey = sortKey;
        this.sortValue = sortValue;
    }

    static QueryConditionalKeyValues from(Key key, TableSchema<?> tableSchema, String indexName) {
        String partitionKey = tableSchema.tableMetadata().indexPartitionKey(indexName);
        AttributeValue partitionValue = key.partitionKeyValue();
        String sortKey = tableSchema.tableMetadata().indexSortKey(indexName).orElseThrow(
            () -> new IllegalArgumentException("A query conditional requires a sort key to be present on the table "
                                               + "or index being queried, yet none have been defined in the "
                                               + "model"));
        AttributeValue sortValue =
            key.sortKeyValue().orElseThrow(
                () -> new IllegalArgumentException("A query conditional requires a sort key to compare with, "
                                                   + "however one was not provided."));

        return new QueryConditionalKeyValues(partitionKey, partitionValue, sortKey, sortValue);
    }

    String partitionKey() {
        return partitionKey;
    }

    AttributeValue partitionValue() {
        return partitionValue;
    }

    String sortKey() {
        return sortKey;
    }

    AttributeValue sortValue() {
        return sortValue;
    }
}
