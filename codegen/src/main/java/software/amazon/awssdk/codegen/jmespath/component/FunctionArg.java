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

package software.amazon.awssdk.codegen.jmespath.component;


import software.amazon.awssdk.codegen.jmespath.parser.JmesPathVisitor;
import software.amazon.awssdk.utils.Validate;

/**
 * An argument to a {@link FunctionExpression}. Either a {@link Expression} that is evaluated and passed to the function or a
 * {@link ExpressionType} that is passed to the function as-is and is evaluated by the function.
 */
public class FunctionArg {
    private Expression expression;
    private ExpressionType expressionType;

    private FunctionArg() {
    }

    public static FunctionArg expression(Expression expression) {
        Validate.notNull(expression, "expression");
        FunctionArg result = new FunctionArg();
        result.expression = expression;
        return result;
    }

    public static FunctionArg expressionType(ExpressionType expressionType) {
        Validate.notNull(expressionType, "expressionType");
        FunctionArg result = new FunctionArg();
        result.expressionType = expressionType;
        return result;
    }

    public boolean isExpression() {
        return expression != null;
    }

    public boolean isExpressionType() {
        return expressionType != null;
    }


    public Expression asExpression() {
        Validate.validState(isExpression(), "Not a Expression");
        return expression;
    }

    public ExpressionType asExpressionType() {
        Validate.validState(isExpressionType(), "Not a ExpressionType");
        return expressionType;
    }

    public void visit(JmesPathVisitor visitor) {
        if (isExpression()) {
            visitor.visitExpression(asExpression());
        } else if (isExpressionType()) {
            visitor.visitExpressionType(asExpressionType());
        } else {
            throw new IllegalStateException();
        }
    }
}
