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
 * A bracket specifier within an {@link IndexExpression}. Either:
 * <ul>
 *     <li>With content, as in [1], [*] or [1:2:3]: {@link BracketSpecifierWithContents}</li>
 *     <li>Without content, as in []: {@link BracketSpecifierWithContents}</li>
 *     <li>With question-mark content, as in [?foo]: {@link BracketSpecifierWithQuestionMark}</li>
 * </ul>
 */
public class BracketSpecifier {
    private BracketSpecifierWithContents bracketSpecifierWithContents;
    private BracketSpecifierWithoutContents bracketSpecifierWithoutContents;
    private BracketSpecifierWithQuestionMark bracketSpecifierWithQuestionMark;

    public static BracketSpecifier withContents(BracketSpecifierWithContents bracketSpecifierWithContents) {
        Validate.notNull(bracketSpecifierWithContents, "bracketSpecifierWithContents");
        BracketSpecifier result = new BracketSpecifier();
        result.bracketSpecifierWithContents = bracketSpecifierWithContents;
        return result;
    }

    public static BracketSpecifier withNumberContents(int numberContents) {
        return withContents(BracketSpecifierWithContents.number(numberContents));
    }

    public static BracketSpecifier withSliceExpressionContents(SliceExpression sliceExpression) {
        return withContents(BracketSpecifierWithContents.sliceExpression(sliceExpression));
    }

    public static BracketSpecifier withWildcardExpressionContents(WildcardExpression wildcardExpression) {
        return withContents(BracketSpecifierWithContents.wildcardExpression(wildcardExpression));
    }

    public static BracketSpecifier withoutContents() {
        BracketSpecifier result = new BracketSpecifier();
        result.bracketSpecifierWithoutContents = new BracketSpecifierWithoutContents();
        return result;
    }

    public static BracketSpecifier withQuestionMark(BracketSpecifierWithQuestionMark bracketSpecifierWithQuestionMark) {
        Validate.notNull(bracketSpecifierWithQuestionMark, "bracketSpecifierWithQuestionMark");
        BracketSpecifier result = new BracketSpecifier();
        result.bracketSpecifierWithQuestionMark = bracketSpecifierWithQuestionMark;
        return result;
    }

    public boolean isBracketSpecifierWithContents() {
        return bracketSpecifierWithContents != null;
    }

    public boolean isBracketSpecifierWithoutContents() {
        return bracketSpecifierWithoutContents != null;
    }

    public boolean isBracketSpecifierWithQuestionMark() {
        return bracketSpecifierWithQuestionMark != null;
    }

    public BracketSpecifierWithContents asBracketSpecifierWithContents() {
        Validate.validState(isBracketSpecifierWithContents(), "Not a BracketSpecifierWithContents");
        return bracketSpecifierWithContents;
    }

    public BracketSpecifierWithoutContents asBracketSpecifierWithoutContents() {
        Validate.validState(isBracketSpecifierWithoutContents(), "Not a BracketSpecifierWithoutContents");
        return bracketSpecifierWithoutContents;
    }

    public BracketSpecifierWithQuestionMark asBracketSpecifierWithQuestionMark() {
        Validate.validState(isBracketSpecifierWithQuestionMark(), "Not a BracketSpecifierWithQuestionMark");
        return bracketSpecifierWithQuestionMark;
    }

    public void visit(JmesPathVisitor visitor) {
        if (isBracketSpecifierWithContents()) {
            visitor.visitBracketSpecifierWithContents(asBracketSpecifierWithContents());
        } else if (isBracketSpecifierWithoutContents()) {
            visitor.visitBracketSpecifierWithoutContents(asBracketSpecifierWithoutContents());
        } else if (isBracketSpecifierWithQuestionMark()) {
            visitor.visitBracketSpecifierWithQuestionMark(asBracketSpecifierWithQuestionMark());
        } else {
            throw new IllegalStateException();
        }
    }
}
