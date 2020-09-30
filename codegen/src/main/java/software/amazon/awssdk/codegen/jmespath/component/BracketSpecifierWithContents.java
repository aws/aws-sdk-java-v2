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
 * A {@link BracketSpecifier} with some kind of content. Either:
 * <ul>
 *     <li>A number, as in [1]</li>
 *     <li>A star expression, as in [*]</li>
 *     <li>A slice expression, as in [1:2:3]</li>
 * </ul>
 */
public class BracketSpecifierWithContents {
    private Integer number;
    private WildcardExpression wildcardExpression;
    private SliceExpression sliceExpression;

    private BracketSpecifierWithContents() {
    }

    public static BracketSpecifierWithContents number(Integer number) {
        Validate.notNull(number, "number");
        BracketSpecifierWithContents result = new BracketSpecifierWithContents();
        result.number = number;
        return result;
    }

    public static BracketSpecifierWithContents wildcardExpression(WildcardExpression wildcardExpression) {
        Validate.notNull(wildcardExpression, "wildcardExpression");
        BracketSpecifierWithContents result = new BracketSpecifierWithContents();
        result.wildcardExpression = wildcardExpression;
        return result;
    }

    public static BracketSpecifierWithContents sliceExpression(SliceExpression sliceExpression) {
        Validate.notNull(sliceExpression, "sliceExpression");
        BracketSpecifierWithContents result = new BracketSpecifierWithContents();
        result.sliceExpression = sliceExpression;
        return result;
    }

    public boolean isNumber() {
        return number != null;
    }

    public boolean isWildcardExpression() {
        return wildcardExpression != null;
    }

    public boolean isSliceExpression() {
        return sliceExpression != null;
    }

    public int asNumber() {
        Validate.validState(isNumber(), "Not a Number");
        return number;
    }

    public WildcardExpression asWildcardExpression() {
        Validate.validState(isWildcardExpression(), "Not a WildcardExpression");
        return wildcardExpression;
    }

    public SliceExpression asSliceExpression() {
        Validate.validState(isSliceExpression(), "Not a SliceExpression");
        return sliceExpression;
    }

    public void visit(JmesPathVisitor visitor) {
        if (isNumber()) {
            visitor.visitNumber(asNumber());
        } else if (isWildcardExpression()) {
            visitor.visitWildcardExpression(asWildcardExpression());
        } else if (isSliceExpression()) {
            visitor.visitSliceExpression(asSliceExpression());
        } else {
            throw new IllegalStateException();
        }
    }
}
