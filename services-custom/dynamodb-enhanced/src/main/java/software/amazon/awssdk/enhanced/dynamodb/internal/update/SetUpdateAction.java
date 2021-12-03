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

package software.amazon.awssdk.enhanced.dynamodb.internal.update;

import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression.listKeyRef;
import static software.amazon.awssdk.enhanced.dynamodb.model.UpdateExpression.valueRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.LongAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.UpdateBehavior;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class SetUpdateAction implements UpdateAction {

    public static final String EQUALS = " = ";

    private final String actionExpression;
    private final Map<String, String> expressionNames;
    private final Map<String, AttributeValue> expressionValues;
    private final String attributeName;

    public SetUpdateAction(BuilderImpl builder) {
        this.actionExpression = builder.actionExpression;
        this.expressionNames = builder.expressionNames == null ? Collections.emptyMap() : builder.expressionNames;
        this.expressionValues = builder.expressionValues == null ? Collections.emptyMap() : builder.expressionValues;
        this.attributeName = builder.attributeName;
    }

    @Override
    public UpdateActionType type() {
        return UpdateActionType.SET;
    }

    public static BuilderImpl builder() {
        return new BuilderImpl();
    }

    private static String ifNotExists(String key, String initValue) {
        return "if_not_exists(" + keyRef(key) + ", " + valueRef(initValue) + ")";
    }

    private static String listAppend(String key, String values) {
        return "list_append(" + keyRef(key) + ", " + valueRef(values) + ")";
    }

    private static String listPrepend(String key, String values) {
        return "list_append(" + valueRef(values) + ", " +  keyRef(key) + ")";
    }

    private static String addValue(String value) {
        return " + " + valueRef(value);
    }

    private static Function<String, String> behaviorBasedValue(UpdateBehavior updateBehavior) {
        switch (updateBehavior) {
            case WRITE_ALWAYS:
                return v -> valueRef(v);
            case WRITE_IF_NOT_EXISTS:
                return k -> ifNotExists(k, k);
            default:
                throw new IllegalArgumentException("Unsupported update behavior '" + updateBehavior + "'");
        }
    }

    private static String setValueExpression(String attributeName, UpdateBehavior updateBehavior) {
        return keyRef(attributeName) +
               EQUALS +
               behaviorBasedValue(updateBehavior).apply(attributeName);
    }

    private static String appendToListExpression(String attributeName, String valuesName, boolean prepend) {
        return keyRef(attributeName) +
               EQUALS +
               (prepend ? listPrepend(attributeName, valuesName) : listAppend(attributeName, valuesName));
    }

    private static String setListValue(String attributeName, int index, String itemName) {
        return listKeyRef(attributeName, index) +
               EQUALS +
               valueRef(itemName);
    }

    private static String addValueExpression(String attributeName, String deltaName) {
        return keyRef(attributeName) + EQUALS + addValue(deltaName);
    }

    private static String addValueWithOptionalStartExpression(String attributeName, String startName, String deltaName) {
        return keyRef(attributeName) + EQUALS +
               ifNotExists(attributeName, startName) +
               addValue(deltaName);
    }

    public static SetUpdateAction setAttribute(String attributeName, AttributeValue value, UpdateBehavior updateBehavior) {
        String valueName = attributeName + "_AMZN_VALUE";
        return builder().attributeName(attributeName)
                        .expression(setValueExpression(attributeName, updateBehavior))
                        .expressionNames(UpdateExpressionUtils.expressionNamesFor(attributeName, valueName))
                        .expressionValues(Collections.singletonMap(valueRef(valueName), value))
                        .build();
    }

    public static UpdateAction addValue(String attributeName, AttributeValue deltaValue) {
        String deltaName = attributeName + "_AMZN_DELTA";
        return builder().attributeName(attributeName)
                        .expression(addValueExpression(attributeName, deltaName))
                        .expressionNames(Collections.singletonMap(keyRef(deltaName), deltaName))
                        .expressionValues(Collections.singletonMap(valueRef(deltaName), deltaValue))
                        .build();
    }

    public static UpdateAction addWithStartValue(String attributeName, AttributeValue startValue, AttributeValue deltaValue) {
        Map<String, String> expressionNames = new HashMap<>();
        String deltaName = attributeName + "_AMZN_DELTA";
        String startName = attributeName + "_AMZN_START";
        expressionNames.put(keyRef(startName), startName);
        expressionNames.put(keyRef(deltaName), deltaName);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(valueRef(startName), subtract(startValue, deltaValue));
        expressionValues.put(valueRef(deltaName), deltaValue);

        return builder().attributeName(attributeName)
                        .expression(addValueWithOptionalStartExpression(attributeName, startName, deltaName))
                        .expressionNames(expressionNames)
                        .expressionValues(expressionValues)
                        .build();
    }

    public static UpdateAction updateListItem(String attributeName, int index, AttributeValue value) {
        Map<String, String> expressionNames = new HashMap<>();
        String listItemName = attributeName + "_AMZN_LIST_ITEM";
        expressionNames.put(keyRef(listItemName), listItemName);
        expressionNames.put(keyRef(attributeName), attributeName);

        return builder().attributeName(attributeName)
                        .expression(setListValue(attributeName, index, listItemName))
                        .expressionNames(expressionNames)
                        .expressionValues(Collections.singletonMap(valueRef(listItemName), value))
                        .build();
    }

    public static UpdateAction appendToList(String attributeName, AttributeValue list, boolean prepend) {
        Map<String, String> expressionNames = new HashMap<>();
        String listName = attributeName + "_AMZN_LIST";
        expressionNames.put(keyRef(attributeName), attributeName);
        expressionNames.put(keyRef(listName), listName);

        return builder().attributeName(attributeName)
                        .expression(appendToListExpression(attributeName, listName, prepend))
                        .expressionNames(expressionNames)
                        .expressionValues(Collections.singletonMap(valueRef(listName), list))
                        .build();
    }

    private static AttributeValue subtract(AttributeValue val1, AttributeValue val2) {
        AttributeConverter<Long> converter = LongAttributeConverter.create();
        Long result = converter.transformTo(val1) - converter.transformTo(val2);
        return converter.transformFrom(result);
    }

    @Override
    public String expression() {
        return actionExpression;
    }

    @Override
    public Map<String, String> expressionNames() {
        return expressionNames;
    }

    @Override
    public Map<String, AttributeValue> expressionValues() {
        return expressionValues;
    }

    @Override
    public String attributeName() {
        return attributeName;
    }

    public static class BuilderImpl implements Builder {

        private String attributeName;
        private String actionExpression;
        private Map<String, String> expressionNames;
        private Map<String, AttributeValue> expressionValues;

        @Override
        public BuilderImpl type(UpdateActionType updateActionType) {
            return this;
        }

        @Override
        public BuilderImpl attributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        @Override
        public BuilderImpl expression(String actionExpression) {
            this.actionExpression = actionExpression;
            return this;
        }

        @Override
        public BuilderImpl expressionNames(Map<String, String> expressionNames) {
            this.expressionNames = expressionNames;
            return this;
        }

        @Override
        public BuilderImpl expressionValues(Map<String, AttributeValue> expressionValues) {
            this.expressionValues = expressionValues;
            return this;
        }

        @Override
        public SetUpdateAction build() {
            return new SetUpdateAction(this);
        }
    }
}
