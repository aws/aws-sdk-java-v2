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
 * A comparator expression is two expressions separated by a {@link Comparator}.
 *
 * Examples:
 * <ul>
 *     <li>Foo == Bar</li>
 *     <li>Bar <= `101</li>
 * </ul>
 */
public class ComparatorExpression {
    private final Expression leftExpression;
    private final Comparator comparator;
    private final Expression rightExpression;

    public ComparatorExpression(Expression leftExpression, Comparator comparator, Expression rightExpression) {
        this.leftExpression = leftExpression;
        this.comparator = comparator;
        this.rightExpression = rightExpression;
    }

    public Expression leftExpression() {
        return leftExpression;
    }

    public Comparator comparator() {
        return comparator;
    }

    public Expression rightExpression() {
        return rightExpression;
    }
}
