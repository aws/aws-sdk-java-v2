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

package software.amazon.awssdk.enhanced.dynamodb;

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.nullAttributeValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * An object that represents a key that can be used to either identify a specific record or form part of a query
 * conditional. Keys are literal and hence not typed, and can be re-used in commands for different modelled types if
 * the literal values are to be the same.
 * <p>
 * A key will always have a single partition key value associated with it, and optionally will have a sort key value.
 * The names of the keys themselves are not part of this object.
 */
@SdkPublicApi
public final class Key {
    private final AttributeValue partitionValue;
    private final AttributeValue sortValue;

    private Key(Builder builder) {
        Validate.isTrue(builder.partitionValue != null && !builder.partitionValue.equals(nullAttributeValue()),
                        "partitionValue should not be null");
        this.partitionValue = builder.partitionValue;
        this.sortValue = builder.sortValue;
    }

    /**
     * Returns a new builder that can be used to construct an instance of this class.
     * @return A newly initialized {@link Builder} object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Return a map of the key elements that can be passed directly to DynamoDb.
     * @param tableSchema A tableschema to determine the key attribute names from.
     * @param index The name of the index to use when determining the key attribute names.
     * @return A map of attribute names to {@link AttributeValue}.
     */
    public Map<String, AttributeValue> keyMap(TableSchema<?> tableSchema, String index) {
        Map<String, AttributeValue> keyMap = new HashMap<>();
        keyMap.put(tableSchema.tableMetadata().indexPartitionKey(index), partitionValue);

        if (sortValue != null) {
            keyMap.put(tableSchema.tableMetadata().indexSortKey(index).orElseThrow(
                () -> new IllegalArgumentException("A sort key value was supplied for an index that does not support "
                                                   + "one. Index: " + index)), sortValue);
        }

        return Collections.unmodifiableMap(keyMap);
    }

    /**
     * Get the literal value of the partition key stored in this object.
     * @return An {@link AttributeValue} representing the literal value of the partition key.
     */
    public AttributeValue partitionKeyValue() {
        return partitionValue;
    }

    /**
     * Get the literal value of the sort key stored in this object if available.
     * @return An optional {@link AttributeValue} representing the literal value of the sort key, or empty if there
     * is no sort key value in this Key.
     */
    public Optional<AttributeValue> sortKeyValue() {
        return Optional.ofNullable(sortValue);
    }

    /**
     * Return a map of the key elements that form the primary key of a table that can be passed directly to DynamoDb.
     * @param tableSchema A tableschema to determine the key attribute names from.
     * @return A map of attribute names to {@link AttributeValue}.
     */
    public Map<String, AttributeValue> primaryKeyMap(TableSchema<?> tableSchema) {
        return keyMap(tableSchema, TableMetadata.primaryIndexName());
    }

    /**
     * Converts an existing key into a builder object that can be used to modify its values and then create a new key.
     * @return A {@link Builder} initialized with the values of this key.
     */
    public Builder toBuilder() {
        return new Builder().partitionValue(this.partitionValue).sortValue(this.sortValue);
    }

    /**
     * Builder for {@link Key}
     */
    public static final class Builder {
        private AttributeValue partitionValue;
        private AttributeValue sortValue;

        private Builder() {
        }

        /**
         * Value to be used for the partition key
         * @param partitionValue partition key value
         */
        public Builder partitionValue(AttributeValue partitionValue) {
            this.partitionValue = partitionValue;
            return this;
        }

        /**
         * String value to be used for the partition key. The string will be converted into an AttributeValue of type S.
         * @param partitionValue partition key value
         */
        public Builder partitionValue(String partitionValue) {
            this.partitionValue = AttributeValues.stringValue(partitionValue);
            return this;
        }

        /**
         * Numeric value to be used for the partition key. The number will be converted into an AttributeValue of type N.
         * @param partitionValue partition key value
         */
        public Builder partitionValue(Number partitionValue) {
            this.partitionValue = AttributeValues.numberValue(partitionValue);
            return this;
        }

        /**
         * Binary value to be used for the partition key. The input will be converted into an AttributeValue of type B.
         * @param partitionValue the bytes to be used for the binary key value.
         */
        public Builder partitionValue(SdkBytes partitionValue) {
            this.partitionValue = AttributeValues.binaryValue(partitionValue);
            return this;
        }

        /**
         * Value to be used for the sort key
         * @param sortValue sort key value
         */
        public Builder sortValue(AttributeValue sortValue) {
            this.sortValue = sortValue;
            return this;
        }

        /**
         * String value to be used for the sort key. The string will be converted into an AttributeValue of type S.
         * @param sortValue sort key value
         */
        public Builder sortValue(String sortValue) {
            this.sortValue = AttributeValues.stringValue(sortValue);
            return this;
        }

        /**
         * Numeric value to be used for the sort key. The number will be converted into an AttributeValue of type N.
         * @param sortValue sort key value
         */
        public Builder sortValue(Number sortValue) {
            this.sortValue = AttributeValues.numberValue(sortValue);
            return this;
        }

        /**
         * Binary value to be used for the sort key. The input will be converted into an AttributeValue of type B.
         * @param sortValue the bytes to be used for the binary key value.
         */
        public Builder sortValue(SdkBytes sortValue) {
            this.sortValue = AttributeValues.binaryValue(sortValue);
            return this;
        }

        /**
         * Construct a {@link Key} from this builder.
         */
        public Key build() {
            return new Key(this);
        }
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

        if (partitionValue != null ? ! partitionValue.equals(key.partitionValue) :
            key.partitionValue != null) {
            return false;
        }
        return sortValue != null ? sortValue.equals(key.sortValue) : key.sortValue == null;
    }

    @Override
    public int hashCode() {
        int result = partitionValue != null ? partitionValue.hashCode() : 0;
        result = 31 * result + (sortValue != null ? sortValue.hashCode() : 0);
        return result;
    }
}
