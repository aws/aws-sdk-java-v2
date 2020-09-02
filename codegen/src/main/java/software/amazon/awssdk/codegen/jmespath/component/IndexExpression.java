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

import java.util.Optional;
import software.amazon.awssdk.utils.Validate;

/**
 * An index expression is used to access elements in a list. Indexing is 0 based, the index of 0 refers to the first element
 * of the list. A negative number is a valid index. A negative number indicates that indexing is relative to the end of the
 * list.
 *
 * https://jmespath.org/specification.html#index-expressions
 */
public class IndexExpression {
    private final Expression expression;
    private final BracketSpecifier bracketSpecifier;

    private IndexExpression(Expression expression, BracketSpecifier bracketSpecifier) {
        this.expression = expression;
        this.bracketSpecifier = bracketSpecifier;
    }

    public static IndexExpression indexExpression(Expression expression, BracketSpecifier bracketSpecifier) {
        Validate.notNull(bracketSpecifier, "bracketSpecifier");
        return new IndexExpression(expression, bracketSpecifier);
    }

    public Optional<Expression> expression() {
        return Optional.ofNullable(expression);
    }

    public BracketSpecifier bracketSpecifier() {
        return bracketSpecifier;
    }
}
