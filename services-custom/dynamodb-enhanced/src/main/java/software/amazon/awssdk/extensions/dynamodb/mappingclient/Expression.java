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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SdkPublicApi
public final class Expression {
    private final String expression;
    private final Map<String, AttributeValue> expressionValues;
    private final Map<String, String> expressionNames;

    private Expression(String expression,
                       Map<String, AttributeValue> expressionValues,
                       Map<String, String> expressionNames) {
        this.expression = expression;
        this.expressionValues = expressionValues;
        this.expressionNames = expressionNames;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Expression coalesce(Expression condition1, Expression condition2, String joinToken) {
        if (condition1 == null) {
            return condition2;
        }

        if (condition2 == null) {
            return condition1;
        }

        return Expression.builder()
                         .expression(coalesceExpressions(condition1.expression, condition2.expression, joinToken))
                         .expressionValues(coalesceValues(condition1.expressionValues(),
                                                          condition2.expressionValues()))
                         .expressionNames(coalesceNames(condition1.expressionNames(),
                                                        condition2.expressionNames()))
                         .build();
    }

    public static String coalesceExpressions(String expression1, String expression2, String joinToken) {
        if (expression1 == null) {
            return expression2;
        }

        if (expression2 == null) {
            return expression1;
        }

        return "(" + expression1 + ")" + joinToken + "(" + expression2 + ")";
    }

    public static Map<String, AttributeValue> coalesceValues(Map<String, AttributeValue> expressionValues1,
                                                             Map<String, AttributeValue> expressionValues2) {
        if (expressionValues1 == null) {
            return expressionValues2;
        }

        if (expressionValues2 == null) {
            return expressionValues1;
        }

        Map<String, AttributeValue> result = new HashMap<>(expressionValues1);
        expressionValues2.forEach((key, value) -> {
            AttributeValue oldValue = result.put(key, value);

            if (oldValue != null && !oldValue.equals(value)) {
                throw new IllegalArgumentException(
                    String.format("Attempt to coalesce two expressions with conflicting expression values. "
                                  + "Expression value key = '%s'", key));
            }
        });

        return Collections.unmodifiableMap(result);
    }

    public static Map<String, String> coalesceNames(Map<String, String> expressionValues1,
                                                    Map<String, String> expressionValues2) {
        if (expressionValues1 == null) {
            return expressionValues2;
        }

        if (expressionValues2 == null) {
            return expressionValues1;
        }

        Map<String, String> result = new HashMap<>(expressionValues1);
        expressionValues2.forEach((key, value) -> {
            String oldValue = result.put(key, value);

            if (oldValue != null && !oldValue.equals(value)) {
                throw new IllegalArgumentException(
                    String.format("Attempt to coalesce two expressions with conflicting expression names. "
                                  + "Expression name key = '%s'", key));
            }
        });

        return Collections.unmodifiableMap(result);
    }

    public String expression() {
        return expression;
    }

    public Map<String, AttributeValue> expressionValues() {
        return expressionValues;
    }

    public Map<String, String> expressionNames() {
        return expressionNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Expression that = (Expression) o;

        if (expression != null ? ! expression.equals(that.expression) : that.expression != null) {
            return false;
        }
        if (expressionValues != null ? ! expressionValues.equals(that.expressionValues) :
            that.expressionValues != null) {
            return false;
        }
        return expressionNames != null ? expressionNames.equals(that.expressionNames) : that.expressionNames == null;
    }

    @Override
    public int hashCode() {
        int result = expression != null ? expression.hashCode() : 0;
        result = 31 * result + (expressionValues != null ? expressionValues.hashCode() : 0);
        result = 31 * result + (expressionNames != null ? expressionNames.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private String expression;
        private Map<String, AttributeValue> expressionValues;
        private Map<String, String> expressionNames;

        private Builder() {
        }

        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public Builder expressionValues(Map<String, AttributeValue> expressionValues) {
            this.expressionValues = expressionValues == null ? null : new HashMap<>(expressionValues);
            return this;
        }

        public Builder putExpressionValue(String key, AttributeValue value) {
            if (this.expressionValues == null) {
                this.expressionValues = new HashMap<>();
            }

            this.expressionValues.put(key, value);
            return this;
        }

        public Builder expressionNames(Map<String, String> expressionNames) {
            this.expressionNames = expressionNames == null ? null : new HashMap<>(expressionNames);
            return this;
        }

        public Builder putExpressionName(String key, String value) {
            if (this.expressionNames == null) {
                this.expressionNames = new HashMap<>();
            }

            this.expressionNames.put(key, value);
            return this;
        }

        public Expression build() {
            return new Expression(expression,
                                  expressionValues == null ? null : Collections.unmodifiableMap(expressionValues),
                                  expressionNames == null ? null : Collections.unmodifiableMap(expressionNames));
        }
    }
}
