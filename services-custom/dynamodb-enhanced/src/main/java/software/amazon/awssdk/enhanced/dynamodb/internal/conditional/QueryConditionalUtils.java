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

import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.cleanAttributeName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public final class QueryConditionalUtils {

    public static final String AND_OPERATOR = " AND ";
    public static final String EQUALITY_OPERATOR = " = ";
    public static final String BETWEEN_OPERATOR = " BETWEEN ";
    public static final String BEGINS_WITH_FUNCTION = "begins_with(";
    public static final String FUNCTION_CLOSE = ")";

    public static final String MISSING_SORT_VALUE_ERROR =
        "A query conditional requires a sort key to compare with, however one was not provided. Index: %s";
    public static final UnaryOperator<String> SECOND_VALUE_TOKEN_MAPPER =
        k -> ":AMZN_MAPPED_" + cleanAttributeName(k) + "2";
    private static final String NULL_PARTITION_KEY_ERROR =
        "Partition key must be a valid scalar value to execute a query against. The provided partition key was set to null. "
        + "Index: %s";
    private static final String NULL_PARTITION_KEYS_ERROR =
        "Partition key must be a valid scalar value to execute a query against. The provided composite partition keys contains "
        + "null. Index: %s";
    private static final String PARTITION_KEY_SIZE_MISMATCH_ERROR =
        "All partition key attributes must be provided for composite key query. Expected: %d, provided: %d. Index: %s";
    private static final String NULL_SORT_KEY_ERROR = "Attempt to query using a condition operator against a null sort key. "
                                                     + "Index: %s";
    private static final String SORT_KEY_SIZE_MISMATCH_ERROR =
        "Cannot provide more sort key values than defined in schema. Index: %s";
    private static final String SORT_KEY_VALUES_WITHOUT_SCHEMA_ERROR = "Sort key values were supplied for an index that does"
                                                                       + " not support sort keys. Index: %s";

    private QueryConditionalUtils() {
    }

    public static KeyResolution resolveKeys(Key key, TableSchema<?> tableSchema, String indexName) {
        List<String> partitionKeys = resolvePartitionKeys(tableSchema, indexName);
        List<AttributeValue> partitionValues = resolvePartitionValues(key);

        List<String> sortKeys = resolveSortKeys(tableSchema, indexName);
        List<AttributeValue> sortValues = resolveSortValues(key);

        return new KeyResolution(partitionKeys, partitionValues, sortKeys, sortValues);
    }

    public static void addPartitionKeyConditions(StringBuilder expression, Map<String, String> names,
                                                 Map<String, AttributeValue> values,
                                                 List<String> partitionKeys, List<AttributeValue> partitionValues) {
        for (int i = 0; i < partitionKeys.size(); i++) {
            if (i > 0) {
                expression.append(AND_OPERATOR);
            }
            addEqualityCondition(expression, names, values, partitionKeys.get(i), partitionValues.get(i));
        }
    }

    public static void addNonRightmostSortKeyConditions(StringBuilder expression, Map<String, String> names,
                                                        Map<String, AttributeValue> values,
                                                        List<String> sortKeys, List<AttributeValue> sortValues) {
        int rightmostIndex = sortValues.size() - 1;
        for (int i = 0; i < rightmostIndex; i++) {
            expression.append(AND_OPERATOR);
            addEqualityCondition(expression, names, values, sortKeys.get(i), sortValues.get(i));
        }
    }

    public static void addEqualityCondition(StringBuilder expression, Map<String, String> names,
                                            Map<String, AttributeValue> values, String key, AttributeValue value) {
        String keyToken = EnhancedClientUtils.keyRef(key);
        String valueToken = EnhancedClientUtils.valueRef(key);

        expression.append(keyToken).append(EQUALITY_OPERATOR).append(valueToken);
        names.put(keyToken, key);
        values.put(valueToken, value);
    }

    public static Expression buildExpression(StringBuilder expression, Map<String, String> names,
                                             Map<String, AttributeValue> values) {
        return Expression.builder()
                         .expression(expression.toString())
                         .expressionNames(Collections.unmodifiableMap(names))
                         .expressionValues(Collections.unmodifiableMap(values))
                         .build();
    }

    public static void validatePartitionKeyConstraints(KeyResolution keyResolution, String indexName) {
        if (keyResolution.isCompositePartition()) {
            if (keyResolution.partitionValues.contains(nullAttributeValue())) {
                throw new IllegalArgumentException(String.format(NULL_PARTITION_KEYS_ERROR, indexName));
            }
            if (keyResolution.partitionValues.size() != keyResolution.partitionKeys.size()) {
                throw new IllegalArgumentException(String.format(PARTITION_KEY_SIZE_MISMATCH_ERROR,
                                                                 keyResolution.partitionKeys.size(),
                                                                 keyResolution.partitionValues.size(), indexName));
            }
        } else {
            if (!keyResolution.hasPartitionValues() || keyResolution.partitionValues.get(0) == null ||
                keyResolution.partitionValues.get(0).equals(nullAttributeValue())) {
                throw new IllegalArgumentException(String.format(NULL_PARTITION_KEY_ERROR, indexName));
            }
        }
    }

    public static void validateSortKeyConstraints(KeyResolution keyResolution, String indexName) {
        if (keyResolution.hasSortKeys() || keyResolution.hasSortValues()) {
            if (keyResolution.sortKeys.isEmpty()) {
                throw new IllegalArgumentException(String.format(SORT_KEY_VALUES_WITHOUT_SCHEMA_ERROR, indexName));
            }

            if (keyResolution.sortValues.size() > keyResolution.sortKeys.size()) {
                throw new IllegalArgumentException(String.format(SORT_KEY_SIZE_MISMATCH_ERROR, indexName));
            }
            if (keyResolution.sortValues.contains(nullAttributeValue())) {
                throw new IllegalArgumentException(String.format(NULL_SORT_KEY_ERROR, indexName));
            }
        }
    }

    private static List<String> resolvePartitionKeys(TableSchema<?> tableSchema, String indexName) {
        List<String> partitionKeys = tableSchema.tableMetadata().indexPartitionKeys(indexName);
        if (!partitionKeys.isEmpty()) {
            return partitionKeys;
        }

        String partitionKey = tableSchema.tableMetadata().indexPartitionKey(indexName);
        return Collections.singletonList(partitionKey);
    }

    private static List<AttributeValue> resolvePartitionValues(Key key) {
        List<AttributeValue> partitionValues = key.partitionKeyValues();
        if (!partitionValues.isEmpty()) {
            return partitionValues;
        }

        AttributeValue partitionValue = key.partitionKeyValue();
        return Collections.singletonList(partitionValue);
    }

    private static List<String> resolveSortKeys(TableSchema<?> tableSchema, String indexName) {
        List<String> sortKeys = tableSchema.tableMetadata().indexSortKeys(indexName);
        if (!sortKeys.isEmpty()) {
            return sortKeys;
        }

        return tableSchema.tableMetadata().indexSortKey(indexName)
                          .map(Collections::singletonList)
                          .orElse(Collections.emptyList());
    }

    private static List<AttributeValue> resolveSortValues(Key key) {
        List<AttributeValue> sortValues = key.sortKeyValues();
        if (!sortValues.isEmpty()) {
            return sortValues;
        }

        return key.sortKeyValue()
                  .map(Collections::singletonList)
                  .orElse(Collections.emptyList());
    }

    public static class KeyResolution {
        public final List<String> partitionKeys;
        public final List<AttributeValue> partitionValues;
        public final List<String> sortKeys;
        public final List<AttributeValue> sortValues;

        public KeyResolution(List<String> partitionKeys, List<AttributeValue> partitionValues,
                             List<String> sortKeys, List<AttributeValue> sortValues) {
            this.partitionKeys = Collections.unmodifiableList(new ArrayList<>(partitionKeys));
            this.partitionValues = Collections.unmodifiableList(new ArrayList<>(partitionValues));
            this.sortKeys = Collections.unmodifiableList(new ArrayList<>(sortKeys));
            this.sortValues = Collections.unmodifiableList(new ArrayList<>(sortValues));
        }

        public boolean hasPartitionValues() {
            return !partitionValues.isEmpty();
        }

        public boolean hasSortKeys() {
            return !sortKeys.isEmpty();
        }

        public boolean hasSortValues() {
            return !sortValues.isEmpty();
        }

        public boolean isCompositePartition() {
            return partitionKeys.size() > 1;
        }

        public boolean isCompositeSort() {
            return sortKeys.size() > 1;
        }

        /**
        * Returns the rightmost provided sort value.
        */
        public AttributeValue getRightmostSortValue() {
            return sortValues.get(sortValues.size() - 1);
        }

        /**
        * Returns the sort-key name corresponding to the rightmost provided sort value.
        */
        public String getRightmostSortKey() {
            return sortKeys.get(sortValues.size() - 1);
        }
    }
}