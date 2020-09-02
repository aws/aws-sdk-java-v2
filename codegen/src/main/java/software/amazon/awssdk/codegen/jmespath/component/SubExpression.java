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
 * A subexpression is a combination of two expressions separated by the ‘.’ char. A subexpression is evaluated as follows:
 * <ol>
 *     <li>Evaluate the expression on the left with the original JSON document.</li>
 *     <li>Evaluate the expression on the right with the result of the left expression evaluation.</li>
 * </ol>
 *
 * https://jmespath.org/specification.html#subexpressions
 */
public class SubExpression {
    private final Expression leftExpression;
    private final SubExpressionRight rightSide;

    public SubExpression(Expression leftExpression, SubExpressionRight rightSide) {
        this.leftExpression = leftExpression;
        this.rightSide = rightSide;
    }

    public Expression leftExpression() {
        return leftExpression;
    }

    public SubExpressionRight rightSubExpression() {
        return rightSide;
    }
}
