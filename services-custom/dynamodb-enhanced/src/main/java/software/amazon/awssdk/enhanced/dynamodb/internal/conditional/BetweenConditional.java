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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public class BetweenConditional implements QueryConditional {
    private static final UnaryOperator<String> EXPRESSION_KEY_MAPPER =
        k -> "#AMZN_MAPPED_" + cleanAttributeName(k);
    private static final UnaryOperator<String> EXPRESSION_VALUE_KEY_MAPPER =
        k -> ":AMZN_MAPPED_" + cleanAttributeName(k);
    private static final UnaryOperator<String> EXPRESSION_OTHER_VALUE_KEY_MAPPER =
        k -> ":AMZN_MAPPED_" + cleanAttributeName(k) + "2";

    private final Key key1;
    private final Key key2;

    public BetweenConditional(Key key1, Key key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        QueryConditionalKeyValues queryConditionalKeyValues1 = QueryConditionalKeyValues.from(key1, tableSchema, indexName);
        QueryConditionalKeyValues queryConditionalKeyValues2 = QueryConditionalKeyValues.from(key2, tableSchema, indexName);

        if (queryConditionalKeyValues1.sortValue().equals(nullAttributeValue()) ||
            queryConditionalKeyValues2.sortValue().equals(nullAttributeValue())) {
            throw new IllegalArgumentException("Attempt to query using a 'between' condition operator where one "
                                               + "of the items has a null sort key.");
        }

        String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(queryConditionalKeyValues1.partitionKey());
        String partitionValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(queryConditionalKeyValues1.partitionKey());
        String sortKeyToken = EXPRESSION_KEY_MAPPER.apply(queryConditionalKeyValues1.sortKey());
        String sortKeyValueToken1 = EXPRESSION_VALUE_KEY_MAPPER.apply(queryConditionalKeyValues1.sortKey());
        String sortKeyValueToken2 = EXPRESSION_OTHER_VALUE_KEY_MAPPER.apply(queryConditionalKeyValues2.sortKey());

        String queryExpression = String.format("%s = %s AND %s BETWEEN %s AND %s",
                                               partitionKeyToken,
                                               partitionValueToken,
                                               sortKeyToken,
                                               sortKeyValueToken1,
                                               sortKeyValueToken2);
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(partitionValueToken, queryConditionalKeyValues1.partitionValue());
        expressionAttributeValues.put(sortKeyValueToken1, queryConditionalKeyValues1.sortValue());
        expressionAttributeValues.put(sortKeyValueToken2, queryConditionalKeyValues2.sortValue());
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put(partitionKeyToken, queryConditionalKeyValues1.partitionKey());
        expressionAttributeNames.put(sortKeyToken, queryConditionalKeyValues1.sortKey());

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
