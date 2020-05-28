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
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.isNullAttributeValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public class EqualToConditional implements QueryConditional {
    private static final UnaryOperator<String> EXPRESSION_KEY_MAPPER =
        k -> "#AMZN_MAPPED_" + cleanAttributeName(k);
    private static final UnaryOperator<String> EXPRESSION_VALUE_KEY_MAPPER =
        k -> ":AMZN_MAPPED_" + cleanAttributeName(k);

    private final Key key;

    public EqualToConditional(Key key) {
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
        if (isNullAttributeValue(sortKeyValue)) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EqualToConditional that = (EqualToConditional) o;

        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
