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
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.addEqualityCondition;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.addPartitionKeyConditions;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.buildExpression;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.resolveKeys;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.validatePartitionKeyConstraints;
import static software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.validateSortKeyConstraints;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.conditional.QueryConditionalUtils.KeyResolution;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkInternalApi
public class EqualToConditional implements QueryConditional {

    private final Key key;

    public EqualToConditional(Key key) {
        this.key = key;
    }

    @Override
    public Expression expression(TableSchema<?> tableSchema, String indexName) {
        KeyResolution keyResolution = resolveKeys(key, tableSchema, indexName);

        validateKeyConstraints(keyResolution, indexName);

        return buildEqualityExpression(keyResolution);
    }

    private void validateKeyConstraints(KeyResolution keyResolution, String indexName) {
        validatePartitionKeyConstraints(keyResolution, indexName);
        validateSortKeyConstraints(keyResolution, indexName);
    }

    private Expression buildEqualityExpression(KeyResolution keyResolution) {
        StringBuilder expression = new StringBuilder();
        Map<String, String> names = new HashMap<>();
        Map<String, AttributeValue> values = new HashMap<>();

        addPartitionKeyConditions(expression, names, values, keyResolution.partitionKeys, keyResolution.partitionValues);

        addSortKeyEqualityConditions(expression, names, values, keyResolution);

        return buildExpression(expression, names, values);
    }

    private void addSortKeyEqualityConditions(StringBuilder expression, Map<String, String> names,
                                              Map<String, AttributeValue> values, KeyResolution keyResolution) {
        List<String> sortKeys = keyResolution.sortKeys;
        List<AttributeValue> sortValues = keyResolution.sortValues;

        for (int i = 0; i < sortValues.size(); i++) {
            expression.append(AND_OPERATOR);
            addEqualityCondition(expression, names, values, sortKeys.get(i), sortValues.get(i));
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

        EqualToConditional that = (EqualToConditional) o;

        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}