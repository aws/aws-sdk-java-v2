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

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A {@link QueryConditional} implementation that matches values from a specific key using a supplied operator for the
 * sort key value comparison. The partition key value will always have an equivalence comparison applied.
 * <p>
 * This class is used by higher-level (more specific) {@link QueryConditional} implementations such as
 * {@link QueryConditional#sortGreaterThan(Key)} to reduce code duplication.
 */
@SdkInternalApi
public class SingleKeyItemConditional implements QueryConditional {
    private static final UnaryOperator<String> EXPRESSION_KEY_MAPPER =
        k -> "#AMZN_MAPPED_" + cleanAttributeName(k);
    private static final UnaryOperator<String> EXPRESSION_VALUE_KEY_MAPPER =
        k -> ":AMZN_MAPPED_" + cleanAttributeName(k);

    private final Key key;
    private final String operator;

    public SingleKeyItemConditional(Key key, String operator) {
        this.key = key;
        this.operator = operator;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        QueryConditionalKeyValues queryConditionalKeyValues = QueryConditionalKeyValues.from(key, tableSchema, indexName);

        if (queryConditionalKeyValues.sortValue().equals(nullAttributeValue())) {
            throw new IllegalArgumentException("Attempt to query using a relative condition operator against a "
                                               + "null sort key.");
        }

        String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(queryConditionalKeyValues.partitionKey());
        String partitionValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(queryConditionalKeyValues.partitionKey());
        String sortKeyToken = EXPRESSION_KEY_MAPPER.apply(queryConditionalKeyValues.sortKey());
        String sortValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(queryConditionalKeyValues.sortKey());

        String queryExpression = String.format("%s = %s AND %s %s %s",
                                               partitionKeyToken,
                                               partitionValueToken,
                                               sortKeyToken,
                                               operator,
                                               sortValueToken);
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(partitionValueToken, queryConditionalKeyValues.partitionValue());
        expressionAttributeValues.put(sortValueToken, queryConditionalKeyValues.sortValue());
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put(partitionKeyToken, queryConditionalKeyValues.partitionKey());
        expressionAttributeNames.put(sortKeyToken, queryConditionalKeyValues.sortKey());

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
