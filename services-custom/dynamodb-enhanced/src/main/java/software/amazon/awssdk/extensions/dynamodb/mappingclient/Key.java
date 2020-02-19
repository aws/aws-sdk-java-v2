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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An object that represents a key that can be used to either identify a specific record or form part of a query
 * conditional. Keys are literal and hence not typed, and can be re-used in commands for different modelled types if
 * the literal values are to be the same.
 * <p>
 * A key will always have a single partition key value associated with it, and optionally will have a sort key value.
 * The names of the keys themselves are not part of this object.
 */
@SdkPublicApi
public class Key {
    private final AttributeValue partitionKeyValue;
    private final AttributeValue sortKeyValue;

    private Key(AttributeValue partitionKeyValue, AttributeValue sortKeyValue) {
        this.partitionKeyValue = partitionKeyValue;
        this.sortKeyValue = sortKeyValue;
    }

    /**
     * Construct a literal key with just a partition key value.
     * @param partitionKeyValue A DynamoDb {@link AttributeValue} that is the literal value of the partition key.
     * @return A key.
     */
    public static Key create(AttributeValue partitionKeyValue) {
        return new Key(partitionKeyValue, null);
    }

    /**
     * Construct a literal key with both a partition key value and a sort key value.
     * @param partitionKeyValue A DynamoDb {@link AttributeValue} that is the literal value of the partition key.
     * @param sortKeyValue A DynamoDb {@link AttributeValue} that is the literal value of the sort key.
     * @return A key.
     */
    public static Key create(AttributeValue partitionKeyValue, AttributeValue sortKeyValue) {
        return new Key(partitionKeyValue, sortKeyValue);
    }

    /**
     * Return a map of the key elements that can be passed directly to DynamoDb.
     * @param tableSchema A tableschema to determine the key attribute names from.
     * @param index The name of the index to use when determining the key attribute names.
     * @return A map of attribute names to {@link AttributeValue}.
     */
    public Map<String, AttributeValue> keyMap(TableSchema<?> tableSchema, String index) {
        Map<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put(tableSchema.tableMetadata().indexPartitionKey(index), partitionKeyValue);

        if (sortKeyValue != null) {
            keyMap.put(tableSchema.tableMetadata().indexSortKey(index).orElseThrow(
                () -> new IllegalArgumentException("A sort key value was supplied for an index that does not support "
                                                   + "one. Index: " + index)), sortKeyValue);
        }

        return Collections.unmodifiableMap(keyMap);
    }

    /**
     * Get the literal value of the partition key stored in this object.
     * @return An {@link AttributeValue} representing the literal value of the partition key.
     */
    public AttributeValue partitionKeyValue() {
        return partitionKeyValue;
    }

    /**
     * Get the literal value of the sort key stored in this object if available.
     * @return An optional {@link AttributeValue} representing the literal value of the sort key, or empty if there
     * is no sort key value in this Key.
     */
    public Optional<AttributeValue> sortKeyValue() {
        return Optional.ofNullable(sortKeyValue);
    }

    /**
     * Return a map of the key elements that form the primary key of a table that can be passed directly to DynamoDb.
     * @param tableSchema A tableschema to determine the key attribute names from.
     * @return A map of attribute names to {@link AttributeValue}.
     */
    public Map<String, AttributeValue> primaryKeyMap(TableSchema<?> tableSchema) {
        return keyMap(tableSchema, TableMetadata.primaryIndexName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Key key = (Key) o;

        if (partitionKeyValue != null ? ! partitionKeyValue.equals(key.partitionKeyValue) :
            key.partitionKeyValue != null) {
            return false;
        }
        return sortKeyValue != null ? sortKeyValue.equals(key.sortKeyValue) : key.sortKeyValue == null;
    }

    @Override
    public int hashCode() {
        int result = partitionKeyValue != null ? partitionKeyValue.hashCode() : 0;
        result = 31 * result + (sortKeyValue != null ? sortKeyValue.hashCode() : 0);
        return result;
    }
}
