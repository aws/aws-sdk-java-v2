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
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.BETWEEN_OPERATOR;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.SECOND_VALUE_TOKEN_MAPPER;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.addNonRightmostSortKeyConditions;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.addPartitionKeyConditions;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.buildExpression;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.resolveKeys;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.validatePartitionKeyConstraints;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.validateSortKeyConstraints;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.KeyResolution;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public class BetweenConditional implements QueryConditional {
    private static final String BETWEEN_NULL_SORT_KEY_ERROR =
        "Attempt to query using a 'between' condition operator where one of the keys has a null sort key. Index: %s";

    private final Key key1;
    private final Key key2;

    public BetweenConditional(Key key1, Key key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        KeyResolution keyResolution1 = resolveKeys(key1, tableSchema, indexName);
        KeyResolution keyResolution2 = resolveKeys(key2, tableSchema, indexName);

        validateBetweenConstraints(keyResolution1, keyResolution2, indexName);

        return buildBetweenExpression(keyResolution1, keyResolution2);
    }

    private void validateBetweenConstraints(KeyResolution keyResolution1, KeyResolution keyResolution2, String indexName) {
        validatePartitionKeyConstraints(keyResolution1, indexName);
        validateSortKeyConstraints(keyResolution1, indexName);

        if (!keyResolution1.hasSortValues() || !keyResolution2.hasSortValues() ||
            keyResolution2.sortValues.contains(nullAttributeValue())) {
            throw new IllegalArgumentException(String.format(BETWEEN_NULL_SORT_KEY_ERROR, indexName));
        }
    }

    private Expression buildBetweenExpression(KeyResolution keyResolution1, KeyResolution keyResolution2) {
        StringBuilder expression = new StringBuilder();
        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();

        addPartitionKeyConditions(expression, names, values,
                                  keyResolution1.partitionKeys, keyResolution1.partitionValues);

        addNonRightmostSortKeyConditions(expression, names, values,
                                         keyResolution1.sortKeys, keyResolution1.sortValues);

        addBetweenCondition(expression, names, values, keyResolution1, keyResolution2);

        return buildExpression(expression, names, values);
    }

    private void addBetweenCondition(StringBuilder expression, Map<String, String> names,
                                     Map<String, AttributeValue> values,
                                     KeyResolution keyResolution1, KeyResolution keyResolution2) {
        String rightmostSortKey = keyResolution1.getRightmostSortKey();
        AttributeValue rightmostSortValue1 = keyResolution1.getRightmostSortValue();
        AttributeValue rightmostSortValue2 = keyResolution2.getRightmostSortValue();

        String keyToken = EnhancedClientUtils.keyRef(rightmostSortKey);
        String valueToken1 = EnhancedClientUtils.valueRef(rightmostSortKey);
        String valueToken2 = SECOND_VALUE_TOKEN_MAPPER.apply(rightmostSortKey);

        expression.append(QueryConditionalUtils.AND_OPERATOR)
                  .append(keyToken)
                  .append(BETWEEN_OPERATOR)
                  .append(valueToken1)
                  .append(QueryConditionalUtils.AND_OPERATOR)
                  .append(valueToken2);

        names.put(keyToken, rightmostSortKey);
        values.put(valueToken1, rightmostSortValue1);
        values.put(valueToken2, rightmostSortValue2);
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

        if (key1 != null ? !key1.equals(that.key1) : that.key1 != null) {
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