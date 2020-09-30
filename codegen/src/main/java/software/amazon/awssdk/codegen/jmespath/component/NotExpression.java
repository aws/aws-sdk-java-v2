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
 * A not-expression negates the result of an expression. If the expression results in a truth-like value, a
 * not-expression will change this value to false. If the expression results in a false-like value, a not-expression will
 * change this value to true.
 *
 * https://jmespath.org/specification.html#not-expressions
 */
public class NotExpression {
    private final Expression expression;

    public NotExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression expression() {
        return expression;
    }
}
