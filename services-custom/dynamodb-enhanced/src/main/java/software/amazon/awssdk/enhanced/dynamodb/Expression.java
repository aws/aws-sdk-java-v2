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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * High-level representation of a DynamoDB 'expression' that can be used in various situations where the API requires
 * or accepts an expression. In addition various convenience methods are provided to help manipulate expressions.
 * <p>
 * At a minimum, an expression must contain a string that is the expression itself.
 * <p>
 * Optionally, attribute names can be substituted with tokens using the '#name_token' syntax; also attribute values can
 * be substituted with tokens using the ':value_token' syntax. If tokens are used in the expression then the values or
 * names associated with those tokens must be explicitly added to the expressionValues and expressionNames maps
 * respectively that are also stored on this object.
 * <p>
 * Example:-
 * {@code
 * Expression myExpression = Expression.builder()
 *                                     .expression("#a = :b")
 *                                     .putExpressionName("#a", "myAttribute")
 *                                     .putExpressionValue(":b", myAttributeValue)
 *                                     .build();
 * }
 */
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

    /**
     * Constructs a new expression builder.
     * @return a new expression builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Coalesces two complete expressions into a single expression. The expression string will be joined using the 
     * supplied join token, and the ExpressionNames and ExpressionValues maps will be merged.
     * @param expression1 The first expression to coalesce
     * @param expression2 The second expression to coalesce
     * @param joinToken The join token to be used to join the expression strings (e.g.: 'AND', 'OR')
     * @return The coalesced expression
     * @throws IllegalArgumentException if a conflict occurs when merging ExpressionNames or ExpressionValues
     */
    public static Expression join(Expression expression1, Expression expression2, String joinToken) {
        if (expression1 == null) {
            return expression2;
        }

        if (expression2 == null) {
            return expression1;
        }

        return Expression.builder()
                         .expression(joinExpressions(expression1.expression, expression2.expression, joinToken))
                         .expressionValues(joinValues(expression1.expressionValues(),
                                                      expression2.expressionValues()))
                         .expressionNames(joinNames(expression1.expressionNames(),
                                                    expression2.expressionNames()))
                         .build();
    }

    /**
     * Coalesces two expression strings into a single expression string. The expression string will be joined using the
     * supplied join token.
     * @param expression1 The first expression string to coalesce
     * @param expression2 The second expression string to coalesce
     * @param joinToken The join token to be used to join the expression strings (e.g.: 'AND', 'OR)
     * @return The coalesced expression
     */
    public static String joinExpressions(String expression1, String expression2, String joinToken) {
        if (expression1 == null) {
            return expression2;
        }

        if (expression2 == null) {
            return expression1;
        }

        return "(" + expression1 + ")" + joinToken + "(" + expression2 + ")";
    }

    /**
     * Coalesces two ExpressionValues maps into a single ExpressionValues map. The ExpressionValues map is an optional
     * component of an expression.
     * @param expressionValues1 The first ExpressionValues map
     * @param expressionValues2 The second ExpressionValues map
     * @return The coalesced ExpressionValues map
     * @throws IllegalArgumentException if a conflict occurs when merging ExpressionValues
     */
    public static Map<String, AttributeValue> joinValues(Map<String, AttributeValue> expressionValues1,
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

    /**
     * Coalesces two ExpressionNames maps into a single ExpressionNames map. The ExpressionNames map is an optional
     * component of an expression.
     * @param expressionNames1 The first ExpressionNames map
     * @param expressionNames2 The second ExpressionNames map
     * @return The coalesced ExpressionNames map
     * @throws IllegalArgumentException if a conflict occurs when merging ExpressionNames
     */
    public static Map<String, String> joinNames(Map<String, String> expressionNames1,
                                                Map<String, String> expressionNames2) {
        if (expressionNames1 == null) {
            return expressionNames2;
        }

        if (expressionNames2 == null) {
            return expressionNames1;
        }

        Map<String, String> result = new HashMap<>(expressionNames1);
        expressionNames2.forEach((key, value) -> {
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

    /**
     * A builder for {@link Expression}
     */
    public static final class Builder {
        private String expression;
        private Map<String, AttributeValue> expressionValues;
        private Map<String, String> expressionNames;

        private Builder() {
        }

        /**
         * The expression string
         */
        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        /**
         * The optional 'expression values' token map
         */
        public Builder expressionValues(Map<String, AttributeValue> expressionValues) {
            this.expressionValues = expressionValues == null ? null : new HashMap<>(expressionValues);
            return this;
        }

        /**
         * Adds a single element to the optional 'expression values' token map
         */
        public Builder putExpressionValue(String key, AttributeValue value) {
            if (this.expressionValues == null) {
                this.expressionValues = new HashMap<>();
            }

            this.expressionValues.put(key, value);
            return this;
        }

        /**
         * The optional 'expression names' token map
         */
        public Builder expressionNames(Map<String, String> expressionNames) {
            this.expressionNames = expressionNames == null ? null : new HashMap<>(expressionNames);
            return this;
        }

        /**
         * Adds a single element to the optional 'expression names' token map
         */
        public Builder putExpressionName(String key, String value) {
            if (this.expressionNames == null) {
                this.expressionNames = new HashMap<>();
            }

            this.expressionNames.put(key, value);
            return this;
        }

        /**
         * Builds an {@link Expression} based on the values stored in this builder
         */
        public Expression build() {
            return new Expression(expression,
                                  expressionValues == null ? null : Collections.unmodifiableMap(expressionValues),
                                  expressionNames == null ? null : Collections.unmodifiableMap(expressionNames));
        }
    }
}
