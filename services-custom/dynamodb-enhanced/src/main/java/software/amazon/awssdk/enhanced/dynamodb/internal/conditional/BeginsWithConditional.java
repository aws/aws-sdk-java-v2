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

import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.AND_OPERATOR;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.BEGINS_WITH_FUNCTION;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.FUNCTION_CLOSE;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.MISSING_SORT_VALUE_ERROR;
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
public class BeginsWithConditional implements QueryConditional {
    private static final String BEGINS_WITH_NUMERIC_SORT_KEY_ERROR = 
        "Attempt to query using a 'beginsWith' condition operator against a numeric sort key. Index: %s, Attribute: %s";

    private final Key key;

    public BeginsWithConditional(Key key) {
        this.key = key;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        KeyResolution keyResolution = resolveKeys(key, tableSchema, indexName);

        validateBeginsWithConstraints(keyResolution, indexName);

        return buildBeginsWithExpression(keyResolution);
    }

    private void validateBeginsWithConstraints(KeyResolution keyResolution, String indexName) {
        validatePartitionKeyConstraints(keyResolution, indexName);
        validateSortKeyConstraints(keyResolution, indexName);

        if (keyResolution.sortValues.isEmpty()) {
            throw new IllegalArgumentException(String.format(MISSING_SORT_VALUE_ERROR, indexName));
        }

        AttributeValue rightmostSortValue = keyResolution.getRightmostSortValue();

        if (rightmostSortValue.n() != null) {
            throw new IllegalArgumentException(String.format(BEGINS_WITH_NUMERIC_SORT_KEY_ERROR, indexName,
                                                             keyResolution.getRightmostSortKey()));
        }
    }

    private Expression buildBeginsWithExpression(KeyResolution keyResolution) {
        StringBuilder expression = new StringBuilder();
        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();

        addPartitionKeyConditions(expression, names, values,
                                  keyResolution.partitionKeys, keyResolution.partitionValues);

        addNonRightmostSortKeyConditions(expression, names, values,
                                         keyResolution.sortKeys, keyResolution.sortValues);

        addBeginsWithCondition(expression, names, values, keyResolution);

        return buildExpression(expression, names, values);
    }

    private void addBeginsWithCondition(StringBuilder expression, Map<String, String> names,
                                        Map<String, AttributeValue> values, KeyResolution keyResolution) {
        String rightmostSortKey = keyResolution.getRightmostSortKey();
        AttributeValue rightmostSortValue = keyResolution.getRightmostSortValue();

        String keyToken = EnhancedClientUtils.keyRef(rightmostSortKey);
        String valueToken = EnhancedClientUtils.valueRef(rightmostSortKey);

        expression.append(AND_OPERATOR)
                  .append(BEGINS_WITH_FUNCTION)
                  .append(keyToken)
                  .append(", ")
                  .append(valueToken)
                  .append(FUNCTION_CLOSE);

        names.put(keyToken, rightmostSortKey);
        values.put(valueToken, rightmostSortValue);
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