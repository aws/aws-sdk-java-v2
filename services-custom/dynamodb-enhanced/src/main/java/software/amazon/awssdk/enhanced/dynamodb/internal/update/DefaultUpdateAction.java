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

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DefaultUpdateAction implements UpdateAction {

    public static final String EQUALS = " = ";

    private final UpdateActionType type;
    private final String actionExpression;
    private final Map<String, String> expressionNames;
    private final Map<String, AttributeValue> expressionValues;
    private final String attributeName;

    public DefaultUpdateAction(BuilderImpl builder) {
        this.type = builder.updateActionType;
        this.attributeName = builder.attributeName;
        this.actionExpression = builder.expression;
        this.expressionNames = builder.expressionNames == null ? Collections.emptyMap() : builder.expressionNames;
        this.expressionValues = builder.expressionValues == null ? Collections.emptyMap() : builder.expressionValues;
    }

    @Override
    public UpdateActionType type() {
        return type;
    }

    public static BuilderImpl builder() {
        return new BuilderImpl();
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

        private UpdateActionType updateActionType;
        private String attributeName;
        private String expression;
        private Map<String, String> expressionNames;
        private Map<String, AttributeValue> expressionValues;

        @Override
        public BuilderImpl type(UpdateActionType updateActionType) {
            this.updateActionType = updateActionType;
            return this;
        }

        @Override
        public BuilderImpl attributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        @Override
        public BuilderImpl expression(String actionExpression) {
            this.expression = actionExpression;
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
        public UpdateAction build() {
            return new DefaultUpdateAction(this);
        }
    }
}
