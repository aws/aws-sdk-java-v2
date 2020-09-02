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
 * A pipe expression combines two expressions, separated by the | character. It is similar to a sub-expression with two
 * important distinctions:
 *
 * <ol>
 *     <li>Any expression can be used on the right hand side. A sub-expression restricts the type of expression that can be used
 *     on the right hand side.</li>
 *     <li>A pipe-expression stops projections on the left hand side from propagating to the right hand side. If the left
 *     expression creates a projection, it does not apply to the right hand side.</li>
 * </ol>
 *
 * https://jmespath.org/specification.html#pipe-expressions
 */
public class PipeExpression {
    private final Expression leftExpression;
    private final Expression rightExpression;

    public PipeExpression(Expression leftExpression, Expression rightExpression) {
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
