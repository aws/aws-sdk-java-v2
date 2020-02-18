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

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.nullAttributeValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.cleanAttributeName;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
public abstract class QueryConditional {
    private static final UnaryOperator<String> EXPRESSION_KEY_MAPPER =
        key -> "#AMZN_MAPPED_" + cleanAttributeName(key);
    private static final UnaryOperator<String> EXPRESSION_VALUE_KEY_MAPPER =
        key -> ":AMZN_MAPPED_" + cleanAttributeName(key);
    private static final UnaryOperator<String> EXPRESSION_OTHER_VALUE_KEY_MAPPER =
        key -> ":AMZN_MAPPED_" + cleanAttributeName(key) + "2";

    public static QueryConditional equalTo(Key key) {
        return new EqualToConditional(key);
    }

    public static QueryConditional greaterThan(Key key) {
        return new SingleKeyItemConditional(key, ">");
    }

    public static QueryConditional greaterThanOrEqualTo(Key key) {
        return new SingleKeyItemConditional(key, ">=");
    }

    public static QueryConditional lessThan(Key key) {
        return new SingleKeyItemConditional(key, "<");
    }

    public static QueryConditional lessThanOrEqualTo(Key key) {
        return new SingleKeyItemConditional(key, "<=");
    }

    public static QueryConditional between(Key key1, Key key2) {
        return new BetweenConditional(key1, key2);
    }

    public static QueryConditional beginsWith(Key key) {
        return new BeginsWithConditional(key);
    }

    public abstract Expression expression(TableSchema<?> tableSchema, String indexName);

    private static class EqualToConditional extends QueryConditional {
        private final Key key;

        private EqualToConditional(Key key) {
            this.key = key;
        }

        @Override
        public Expression expression(TableSchema<?> tableSchema, String indexName) {
            String partitionKey = tableSchema.tableMetadata().indexPartitionKey(indexName);
            AttributeValue partitionValue = key.partitionKeyValue();

            if (partitionValue == null || partitionValue.equals(nullAttributeValue())) {
                throw new IllegalArgumentException("Partition key must be a valid scalar value to execute a query "
                    + "against. The provided partition key was set to null.");
            }

            Optional<AttributeValue> sortKeyValue = key.sortKeyValue();

            if (sortKeyValue.isPresent()) {
                Optional<String> sortKey = tableSchema.tableMetadata().indexSortKey(indexName);

                if (!sortKey.isPresent()) {
                    throw new IllegalArgumentException("A sort key was supplied as part of a query conditional "
                                                       + "against an index that does not support a sort key. Index: "
                                                       + indexName);
                }

                return partitionAndSortExpression(partitionKey,
                                                  sortKey.get(),
                                                  partitionValue,
                                                  sortKeyValue.get());
            } else {
                return partitionOnlyExpression(partitionKey, partitionValue);
            }
        }

        private Expression partitionOnlyExpression(String partitionKey,
                                                   AttributeValue partitionValue) {

            String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(partitionKey);
            String partitionKeyValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(partitionKey);
            String queryExpression = String.format("%s = %s", partitionKeyToken, partitionKeyValueToken);

            return Expression.builder()
                             .expression(queryExpression)
                             .expressionNames(Collections.singletonMap(partitionKeyToken, partitionKey))
                             .expressionValues(Collections.singletonMap(partitionKeyValueToken, partitionValue))
                             .build();
        }

        private Expression partitionAndSortExpression(String partitionKey,
                                                      String sortKey,
                                                      AttributeValue partitionValue,
                                                      AttributeValue sortKeyValue) {


            // When a sort key is explicitly provided as null treat as partition only expression
            if (sortKeyValue.equals(nullAttributeValue())) {
                return partitionOnlyExpression(partitionKey, partitionValue);
            }

            String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(partitionKey);
            String partitionKeyValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(partitionKey);
            String sortKeyToken = EXPRESSION_KEY_MAPPER.apply(sortKey);
            String sortKeyValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(sortKey);

            String queryExpression = String.format("%s = %s AND %s = %s",
                                                   partitionKeyToken,
                                                   partitionKeyValueToken,
                                                   sortKeyToken,
                                                   sortKeyValueToken);
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(partitionKeyValueToken, partitionValue);
            expressionAttributeValues.put(sortKeyValueToken, sortKeyValue);
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put(partitionKeyToken, partitionKey);
            expressionAttributeNames.put(sortKeyToken, sortKey);

            return Expression.builder()
                             .expression(queryExpression)
                             .expressionValues(expressionAttributeValues)
                             .expressionNames(expressionAttributeNames)
                             .build();
        }
    }

    private static class ExpressionParameters {
        private final String partitionKey;
        private final AttributeValue partitionValue;
        private final String sortKey;
        private final AttributeValue sortValue;

        private ExpressionParameters(String partitionKey,
                                     AttributeValue partitionValue,
                                     String sortKey,
                                     AttributeValue sortValue) {
            this.partitionKey = partitionKey;
            this.partitionValue = partitionValue;
            this.sortKey = sortKey;
            this.sortValue = sortValue;
        }

        private static ExpressionParameters from(Key key, TableSchema tableSchema, String indexName) {
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

            return new ExpressionParameters(partitionKey, partitionValue, sortKey, sortValue);
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

    private static class SingleKeyItemConditional extends QueryConditional {
        private final Key key;
        private final String operator;

        private SingleKeyItemConditional(Key key, String operator) {
            this.key = key;
            this.operator = operator;
        }

        @Override
        public Expression expression(TableSchema<?> tableSchema, String indexName) {
            ExpressionParameters expressionParameters = ExpressionParameters.from(key, tableSchema, indexName);

            if (expressionParameters.sortValue().equals(nullAttributeValue())) {
                throw new IllegalArgumentException("Attempt to query using a relative condition operator against a "
                                                   + "null sort key.");
            }

            String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(expressionParameters.partitionKey());
            String partitionValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(expressionParameters.partitionKey());
            String sortKeyToken = EXPRESSION_KEY_MAPPER.apply(expressionParameters.sortKey());
            String sortValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(expressionParameters.sortKey());

            String queryExpression = String.format("%s = %s AND %s %s %s",
                                                   partitionKeyToken,
                                                   partitionValueToken,
                                                   sortKeyToken,
                                                   operator,
                                                   sortValueToken);
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(partitionValueToken, expressionParameters.partitionValue());
            expressionAttributeValues.put(sortValueToken, expressionParameters.sortValue());
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put(partitionKeyToken, expressionParameters.partitionKey());
            expressionAttributeNames.put(sortKeyToken, expressionParameters.sortKey());

            return Expression.builder()
                             .expression(queryExpression)
                             .expressionValues(expressionAttributeValues)
                             .expressionNames(expressionAttributeNames)
                             .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SingleKeyItemConditional that = (SingleKeyItemConditional) o;

            if (key != null ? ! key.equals(that.key) : that.key != null) {
                return false;
            }
            return operator != null ? operator.equals(that.operator) : that.operator == null;
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (operator != null ? operator.hashCode() : 0);
            return result;
        }
    }

    private static class BeginsWithConditional extends QueryConditional {
        private final Key key;

        private BeginsWithConditional(Key key) {
            this.key = key;
        }

        @Override
        public Expression expression(TableSchema<?> tableSchema, String indexName) {
            ExpressionParameters expressionParameters = ExpressionParameters.from(key, tableSchema, indexName);

            if (expressionParameters.sortValue().equals(nullAttributeValue())) {
                throw new IllegalArgumentException("Attempt to query using a 'beginsWith' condition operator against a "
                                                   + "null sort key.");
            }

            if (expressionParameters.sortValue().n() != null) {
                throw new IllegalArgumentException("Attempt to query using a 'beginsWith' condition operator against "
                                                   + "a numeric sort key.");
            }

            String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(expressionParameters.partitionKey());
            String partitionValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(expressionParameters.partitionKey());
            String sortKeyToken = EXPRESSION_KEY_MAPPER.apply(expressionParameters.sortKey());
            String sortValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(expressionParameters.sortKey());

            String queryExpression = String.format("%s = %s AND begins_with ( %s, %s )",
                                                   partitionKeyToken,
                                                   partitionValueToken,
                                                   sortKeyToken,
                                                   sortValueToken);
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(partitionValueToken, expressionParameters.partitionValue());
            expressionAttributeValues.put(sortValueToken, expressionParameters.sortValue());
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put(partitionKeyToken, expressionParameters.partitionKey());
            expressionAttributeNames.put(sortKeyToken, expressionParameters.sortKey());

            return Expression.builder()
                             .expression(queryExpression)
                             .expressionValues(Collections.unmodifiableMap(expressionAttributeValues))
                             .expressionNames(expressionAttributeNames)
                             .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BeginsWithConditional that = (BeginsWithConditional) o;

            return key != null ? key.equals(that.key) : that.key == null;
        }

        @Override
        public int hashCode() {
            return key != null ? key.hashCode() : 0;
        }
    }

    private static class BetweenConditional extends QueryConditional {
        private final Key key1;
        private final Key key2;

        private BetweenConditional(Key key1, Key key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        @Override
        public Expression expression(TableSchema<?> tableSchema, String indexName) {
            ExpressionParameters expressionParameters1 = ExpressionParameters.from(key1, tableSchema, indexName);
            ExpressionParameters expressionParameters2 = ExpressionParameters.from(key2, tableSchema, indexName);

            if (expressionParameters1.sortValue().equals(nullAttributeValue()) ||
                expressionParameters2.sortValue().equals(nullAttributeValue())) {
                throw new IllegalArgumentException("Attempt to query using a 'between' condition operator where one "
                                                   + "of the items has a null sort key.");
            }

            String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(expressionParameters1.partitionKey());
            String partitionValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(expressionParameters1.partitionKey());
            String sortKeyToken = EXPRESSION_KEY_MAPPER.apply(expressionParameters1.sortKey());
            String sortKeyValueToken1 = EXPRESSION_VALUE_KEY_MAPPER.apply(expressionParameters1.sortKey());
            String sortKeyValueToken2 = EXPRESSION_OTHER_VALUE_KEY_MAPPER.apply(expressionParameters2.sortKey());

            String queryExpression = String.format("%s = %s AND %s BETWEEN %s AND %s",
                                                   partitionKeyToken,
                                                   partitionValueToken,
                                                   sortKeyToken,
                                                   sortKeyValueToken1,
                                                   sortKeyValueToken2);
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(partitionValueToken, expressionParameters1.partitionValue());
            expressionAttributeValues.put(sortKeyValueToken1, expressionParameters1.sortValue());
            expressionAttributeValues.put(sortKeyValueToken2, expressionParameters2.sortValue());
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put(partitionKeyToken, expressionParameters1.partitionKey());
            expressionAttributeNames.put(sortKeyToken, expressionParameters1.sortKey());

            return Expression.builder()
                             .expression(queryExpression)
                             .expressionValues(Collections.unmodifiableMap(expressionAttributeValues))
                             .expressionNames(expressionAttributeNames)
                             .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BetweenConditional that = (BetweenConditional) o;

            if (key1 != null ? ! key1.equals(that.key1) : that.key1 != null) {
                return false;
            }
            return key2 != null ? key2.equals(that.key2) : that.key2 == null;
        }

        @Override
        public int hashCode() {
            int result = key1 != null ? key1.hashCode() : 0;
            result = 31 * result + (key2 != null ? key2.hashCode() : 0);
            return result;
        }
    }
}
