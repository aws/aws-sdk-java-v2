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
public class BeginsWithConditional implements QueryConditional {
    private static final UnaryOperator<String> EXPRESSION_KEY_MAPPER =
        k -> "#AMZN_MAPPED_" + cleanAttributeName(k);
    private static final UnaryOperator<String> EXPRESSION_VALUE_KEY_MAPPER =
        k -> ":AMZN_MAPPED_" + cleanAttributeName(k);

    private final Key key;

    public BeginsWithConditional(Key key) {
        this.key = key;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        QueryConditionalKeyValues queryConditionalKeyValues = QueryConditionalKeyValues.from(key, tableSchema, indexName);

        if (queryConditionalKeyValues.sortValue().equals(nullAttributeValue())) {
            throw new IllegalArgumentException("Attempt to query using a 'beginsWith' condition operator against a "
                                               + "null sort key.");
        }

        if (queryConditionalKeyValues.sortValue().n() != null) {
            throw new IllegalArgumentException("Attempt to query using a 'beginsWith' condition operator against "
                                               + "a numeric sort key.");
        }

        String partitionKeyToken = EXPRESSION_KEY_MAPPER.apply(queryConditionalKeyValues.partitionKey());
        String partitionValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(queryConditionalKeyValues.partitionKey());
        String sortKeyToken = EXPRESSION_KEY_MAPPER.apply(queryConditionalKeyValues.sortKey());
        String sortValueToken = EXPRESSION_VALUE_KEY_MAPPER.apply(queryConditionalKeyValues.sortKey());

        String queryExpression = String.format("%s = %s AND begins_with ( %s, %s )",
                                               partitionKeyToken,
                                               partitionValueToken,
                                               sortKeyToken,
                                               sortValueToken);
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(partitionValueToken, queryConditionalKeyValues.partitionValue());
        expressionAttributeValues.put(sortValueToken, queryConditionalKeyValues.sortValue());
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put(partitionKeyToken, queryConditionalKeyValues.partitionKey());
        expressionAttributeNames.put(sortKeyToken, queryConditionalKeyValues.sortKey());

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
