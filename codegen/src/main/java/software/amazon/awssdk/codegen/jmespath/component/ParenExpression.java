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

/**
 * A paren-expression allows a user to override the precedence order of an expression, e.g. (a || b) && c.
 *
 * https://jmespath.org/specification.html#paren-expressions
 */
public class ParenExpression {
    private final Expression expression;

    public ParenExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression expression() {
        return expression;
    }
}
