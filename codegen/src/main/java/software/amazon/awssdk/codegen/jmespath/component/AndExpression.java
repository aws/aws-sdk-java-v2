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
 * An and expression will evaluate to either the left expression or the right expression. If the expression on the left hand side
 * is a truth-like value, then the value on the right hand side is returned. Otherwise the result of the expression on the left
 * hand side is returned.
 *
 * Examples:
 * <ul>
 * <li>True && False</li>
 * <li>Number && EmptyList</li>
 * <li>a == `1` && b == `2`</li>
 * </ul>
 *
 * https://jmespath.org/specification.html#and-expressions
 */
public class AndExpression {
    private final Expression leftExpression;
    private final Expression rightExpression;

    public AndExpression(Expression leftExpression, Expression rightExpression) {
        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    public Expression leftExpression() {
        return leftExpression;
    }

    public Expression rightExpression() {
        return rightExpression;
    }
}
