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

/**
 * A {@link QueryConditional} implementation that matches values from a specific key using a supplied operator for the
 * sort key value comparison. The partition key value will always have an equivalence comparison applied.
 * <p>
 * This class is used by higher-level (more specific) {@link QueryConditional} implementations such as
 * {@link QueryConditional#sortGreaterThan(Key)} to reduce code duplication.
 */
@SdkInternalApi
public class SingleKeyItemConditional implements QueryConditional {

    private final Key key;
    private final String operator;

    public SingleKeyItemConditional(Key key, String operator) {
        this.key = key;
        this.operator = operator;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        KeyResolution keyResolution = resolveKeys(key, tableSchema, indexName);

        validateSingleKeyConstraints(keyResolution, indexName);

        return buildSingleKeyExpression(keyResolution);
    }

    private void validateSingleKeyConstraints(KeyResolution keyResolution, String indexName) {
        validatePartitionKeyConstraints(keyResolution, indexName);
        validateSortKeyConstraints(keyResolution, indexName);
        if (keyResolution.sortValues.isEmpty()) {
            throw new IllegalArgumentException(String.format(MISSING_SORT_VALUE_ERROR, indexName));
        }
    }

    private Expression buildSingleKeyExpression(KeyResolution keyResolution) {
        StringBuilder expression = new StringBuilder();
        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();

        addPartitionKeyConditions(expression, names, values,
                                  keyResolution.partitionKeys, keyResolution.partitionValues);

        addNonRightmostSortKeyConditions(expression, names, values,
                                         keyResolution.sortKeys, keyResolution.sortValues);

        addOperatorCondition(expression, names, values, keyResolution);

        return buildExpression(expression, names, values);
    }

    private void addOperatorCondition(StringBuilder expression, Map<String, String> names,
                                      Map<String, AttributeValue> values, KeyResolution keyResolution) {
        String rightmostSortKey = keyResolution.getRightmostSortKey();
        AttributeValue rightmostSortValue = keyResolution.getRightmostSortValue();

        String keyToken = EnhancedClientUtils.keyRef(rightmostSortKey);
        String valueToken = EnhancedClientUtils.valueRef(rightmostSortKey);

        expression.append(QueryConditionalUtils.AND_OPERATOR)
                  .append(keyToken)
                  .append(" ")
                  .append(operator)
                  .append(" ")
                  .append(valueToken);

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

        SingleKeyItemConditional that = (SingleKeyItemConditional) o;

        if (key != null ? !key.equals(that.key) : that.key != null) {
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