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
 * An or expression will evaluate to either the left expression or the right expression. If the evaluation of the left
 * expression is not false it is used as the return value. If the evaluation of the right expression is not false it is
 * used as the return value. If neither the left or right expression are non-null, then a value of null is returned.
 *
 * Examples:
 * <ul>
 * <li>True || False</li>
 * <li>Number || EmptyList</li>
 * <li>a == `1` || b == `2`</li>
 * </ul>
 *
 * https://jmespath.org/specification.html#or-expressions
 */
public class OrExpression {
    private final Expression leftExpression;
    private final Expression rightExpression;

    public OrExpression(Expression leftExpression, Expression rightExpression) {
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
