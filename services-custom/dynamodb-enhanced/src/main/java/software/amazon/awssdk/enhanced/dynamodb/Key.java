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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * An object that represents a key that can be used to either identify a specific record or form part of a query
 * conditional. Keys are literal and hence not typed, and can be re-used in commands for different modelled types if
 * the literal values are to be the same.
 * <p>
 * A key will always have partition key values associated with it, and optionally will have sort key values.
 * Supports up to {@value #MAX_KEYS} partition keys and {@value #MAX_KEYS} sort keys.
 * The names of the keys themselves are not part of this object.
 */
@SdkPublicApi
@ThreadSafe
public final class Key {
    public static final int MAX_KEYS = 4;

    private final List<AttributeValue> partitionValues;
    private final List<AttributeValue> sortValues;

    private Key(Builder builder) {
        if (builder.partitionValues == null || builder.partitionValues.isEmpty()) {
            throw new IllegalArgumentException("partitionValues should not be null or empty");
        }
        Validate.isTrue(builder.partitionValues.size() <= MAX_KEYS,
                        String.format("Maximum %s partition keys supported", MAX_KEYS));
        Validate.isTrue(builder.sortValues == null || builder.sortValues.size() <= MAX_KEYS,
                        String.format("Maximum %s sort keys supported", MAX_KEYS));

        this.partitionValues = Collections.unmodifiableList(new ArrayList<>(builder.partitionValues));
        this.sortValues = builder.sortValues != null ?
                          Collections.unmodifiableList(new ArrayList<>(builder.sortValues)) : Collections.emptyList();
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
     * @param tableSchema A table schema to determine the key attribute names from.
     * @param index The name of the index to use when determining the key attribute names.
     * @return A map of attribute names to {@link AttributeValue}.
     */
    public Map<String, AttributeValue> keyMap(TableSchema<?> tableSchema, String index) {
        Validate.notNull(tableSchema, "tableSchema must not be null");
        Map<String, AttributeValue> keyMap = new HashMap<>();

        List<String> partitionKeys = tableSchema.tableMetadata().indexPartitionKeys(index);
        for (int i = 0; i < partitionKeys.size() && i < partitionValues.size(); i++) {
            keyMap.put(partitionKeys.get(i), partitionValues.get(i));
        }

        if (!sortValues.isEmpty()) {
            List<String> sortKeys = tableSchema.tableMetadata().indexSortKeys(index);
            if (sortKeys.isEmpty()) {
                throw new IllegalArgumentException("Sort key values were supplied for an index that does not support "
                                                   + "sort keys. Index: " + index);
            }
            for (int i = 0; i < sortKeys.size() && i < sortValues.size(); i++) {
                keyMap.put(sortKeys.get(i), sortValues.get(i));
            }
        }

        return Collections.unmodifiableMap(keyMap);
    }

    /**
     * Get the literal values of all composite partition keys stored in this object.
     *
     * @return A list of {@link AttributeValue} representing the literal values of the partition keys.
     */
    public List<AttributeValue> partitionKeyValues() {
        return partitionValues;
    }

    /**
     * Get the literal values of all composite sort keys stored in this object.
     *
     * @return A list of {@link AttributeValue} representing the literal values of the sort keys.
     */
    public List<AttributeValue> sortKeyValues() {
        return sortValues;
    }

    /**
     * Get the literal value of the single partition key stored in this object.
     * <p>
     * Use {@link #partitionKeyValues()} for composite key support
     * @return An {@link AttributeValue} representing the literal value of the first partition key.
     */
    public AttributeValue partitionKeyValue() {
        return this.partitionValues.isEmpty() ? null : this.partitionValues.get(0);
    }

    /**
     * Get the literal value of the single sort key stored in this object if available.
     * <p>
     * Use {@link #sortKeyValues()} for composite key support
     * @return An optional {@link AttributeValue} representing the literal value of the first sort key, or empty if there is no
     * sort key value in this Key.
     */
    public Optional<AttributeValue> sortKeyValue() {
        return Optional.ofNullable(this.sortValues.isEmpty() ? null : this.sortValues.get(0));
    }

    /**
     * Return a map of the key elements that form the primary key of a table that can be passed directly to DynamoDb.
     * @param tableSchema A table schema to determine the key attribute names from.
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
        return new Builder().partitionValues(this.partitionValues).sortValues(this.sortValues);
    }

    /**
     * Builder for {@link Key}
     */
    @NotThreadSafe
    public static final class Builder {

        private static final DefaultAttributeConverterProvider DEFAULT_CONVERTER_PROVIDER =
            DefaultAttributeConverterProvider.create();

        private List<AttributeValue> partitionValues = new ArrayList<>();
        private List<AttributeValue> sortValues = new ArrayList<>();

        private Builder() {
        }

        /**
         * Values to be used for composite partition keys
         *
         * @param partitionValues list of partition key values (max {@value #MAX_KEYS})
         */
        public Builder partitionValues(List<AttributeValue> partitionValues) {
            this.partitionValues = new ArrayList<>(partitionValues != null ? partitionValues : Collections.emptyList());
            return this;
        }

        /**
         * Values to be used for composite sort keys
         *
         * @param sortValues list of sort key values (max {@value #MAX_KEYS})
         */
        public Builder sortValues(List<AttributeValue> sortValues) {
            this.sortValues = new ArrayList<>(sortValues != null ? sortValues : Collections.emptyList());
            return this;
        }

        /**
         * Adds a partition key value to this key builder for composite partition keys.
         * <p>
         * The value will be automatically converted to a DynamoDB {@link AttributeValue} using the default
         * attribute converter. Supported types include primitives, strings, numbers, binary data, and other
         * standard Java types that can be mapped to DynamoDB attribute values.
         * <p>
         * For composite partition keys, values are added in order (0, 1, 2, 3) and must match the order
         * defined in the table schema.
         *
         * @param value the partition key value to add. Must not be null.
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if value is null or the value type cannot be converted to an AttributeValue
         */
        public Builder addPartitionValue(Object value) {
            if (value == null) {
                throw new IllegalArgumentException("Partition key value cannot be null");
            }
            partitionValues.add(convertToAttributeValue(value));
            return this;
        }

        /**
         * Adds a sort key value to this key builder for composite sort keys.
         * <p>
         * The value will be automatically converted to a DynamoDB {@link AttributeValue} using the default
         * attribute converter. Supported types include primitives, strings, numbers, binary data, and other
         * standard Java types that can be mapped to DynamoDB attribute values.
         * <p>
         * For composite sort keys, values are added in order (0, 1, 2, 3) and must match the order
         * defined in the table schema.
         *
         * @param value the sort key value to add. Must not be null.
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if value is null or the value type cannot be converted to an AttributeValue
         */
        public Builder addSortValue(Object value) {
            if (value == null) {
                throw new IllegalArgumentException("Sort key value cannot be null");
            }
            sortValues.add(convertToAttributeValue(value));
            return this;
        }

        @SuppressWarnings("unchecked")
        private AttributeValue convertToAttributeValue(Object value) {
            try {
                EnhancedType<Object> type = (EnhancedType<Object>) EnhancedType.of(value.getClass());
                AttributeConverter<Object> converter = DEFAULT_CONVERTER_PROVIDER.converterFor(type);
                return converter.transformFrom(value);
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName(), e);
            }
        }

        /**
         * Value to be used for the single partition key
         * <p>
         * Use {@link #partitionValues(List)} or {@link #addPartitionValue(Object)} for composite key support
         * @param partitionValue partition key value
         */
        public Builder partitionValue(AttributeValue partitionValue) {
            if (partitionValue == null || partitionValue.nul() != null && partitionValue.nul()) {
                throw new IllegalArgumentException("partitionValue should not be null");
            }
            this.partitionValues = Collections.singletonList(partitionValue);
            return this;
        }

        /**
         * String value to be used for the single partition key. The string will be converted into an AttributeValue of type S.
         * <p>
         * Use {@link #partitionValues(List)} or {@link #addPartitionValue(Object)} for composite key support
         * @param partitionValue partition key value
         */
        public Builder partitionValue(String partitionValue) {
            if (partitionValue == null) {
                throw new IllegalArgumentException("partitionValue should not be null");
            }
            this.partitionValues = Collections.singletonList(AttributeValues.stringValue(partitionValue));
            return this;
        }

        /**
         * Numeric value to be used for the single partition key. The number will be converted into an AttributeValue of type N.
         * <p>
         * Use {@link #partitionValues(List)} or {@link #addPartitionValue(Object)} for composite key support
         * @param partitionValue partition key value
         */
        public Builder partitionValue(Number partitionValue) {
            if (partitionValue == null) {
                throw new IllegalArgumentException("partitionValue should not be null");
            }
            this.partitionValues = Collections.singletonList(AttributeValues.numberValue(partitionValue));
            return this;
        }

        /**
         * Binary value to be used for the single partition key. The input will be converted into an AttributeValue of type B.
         * <p>
         * Use {@link #partitionValues(List)} or {@link #addPartitionValue(Object)} for composite key support
         * @param partitionValue the bytes to be used for the binary key value.
         */
        public Builder partitionValue(SdkBytes partitionValue) {
            if (partitionValue == null) {
                throw new IllegalArgumentException("partitionValue should not be null");
            }
            this.partitionValues = Collections.singletonList(AttributeValues.binaryValue(partitionValue));
            return this;
        }

        /**
         * Value to be used for the single sort key
         * <p>
         * Use {@link #sortValues(List)} or {@link #addSortValue(Object)} for composite key support
         * @param sortValue sort key value
         */
        public Builder sortValue(AttributeValue sortValue) {
            this.sortValues = sortValue != null ?
                              Collections.singletonList(sortValue) : Collections.emptyList();
            return this;
        }

        /**
         * String value to be used for the single sort key. The string will be converted into an AttributeValue of type S.
         * <p>
         * Use {@link #sortValues(List)} or {@link #addSortValue(Object)} for composite key support
         * @param sortValue sort key value
         */
        public Builder sortValue(String sortValue) {
            this.sortValues = sortValue != null ?
                              Collections.singletonList(AttributeValues.stringValue(sortValue)) : Collections.emptyList();
            return this;
        }

        /**
         * Numeric value to be used for the single sort key. The number will be converted into an AttributeValue of type N.
         * <p>
         * Use {@link #sortValues(List)} or {@link #addSortValue(Object)} for composite key support
         * @param sortValue sort key value
         */
        public Builder sortValue(Number sortValue) {
            this.sortValues = sortValue != null ?
                              Collections.singletonList(AttributeValues.numberValue(sortValue)) : Collections.emptyList();
            return this;
        }

        /**
         * Binary value to be used for the single sort key. The input will be converted into an AttributeValue of type B.
         * <p>
         * Use {@link #sortValues(List)} or {@link #addSortValue(Object)} for composite key support
         * @param sortValue the bytes to be used for the binary key value.
         */
        public Builder sortValue(SdkBytes sortValue) {
            this.sortValues = sortValue != null ?
                              Collections.singletonList(AttributeValues.binaryValue(sortValue)) : Collections.emptyList();
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

        return Objects.equals(partitionValues, key.partitionValues) &&
               Objects.equals(sortValues, key.sortValues);
    }

    @Override
    public int hashCode() {
        int result = partitionValues == null || partitionValues.isEmpty() ? 0
                     : listHashCode(partitionValues, 0);

        result = sortValues == null || sortValues.isEmpty() ? 31 * result
                 : listHashCode(sortValues, result);

        return result;
    }

    private static int listHashCode(List<AttributeValue> list, int hash) {
        int result = hash;
        for (AttributeValue value : list) {
            result = 31 * result + value.hashCode();
        }
        return result;
    }
}
