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

package software.amazon.awssdk.buildtools.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.Arrays;
import java.util.List;

/**
 * Caps the length of the {@code guidance} and {@code saferAlternative} member values on any
 * {@code @SdkAdvancedApi} usage. The values are written as multi-line string-literal concatenations, so this operates
 * on the source AST and sums the decoded content of every concatenated string literal in the member's expression before
 * comparing to {@code max}.
 */
public class SdkAdvancedApiMemberLengthCheck extends AbstractCheck {

    private static final String ANNOTATION_NAME = "SdkAdvancedApi";
    private static final List<String> CHECKED_MEMBERS = Arrays.asList("guidance", "saferAlternative");

    private int max = 1000;

    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {TokenTypes.ANNOTATION};
    }

    @Override
    public void visitToken(DetailAST annotation) {
        if (!isSdkAdvancedApi(annotation)) {
            return;
        }

        for (DetailAST child = annotation.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() != TokenTypes.ANNOTATION_MEMBER_VALUE_PAIR) {
                continue;
            }

            DetailAST memberName = child.findFirstToken(TokenTypes.IDENT);
            if (memberName == null || !CHECKED_MEMBERS.contains(memberName.getText())) {
                continue;
            }

            DetailAST expr = child.findFirstToken(TokenTypes.EXPR);
            if (expr == null) {
                continue;
            }

            int length = concatenatedStringLength(expr);
            if (length > max) {
                log(memberName, String.format(
                    "@%s member '%s' is %d characters, which exceeds the maximum of %d. Tighten the wording.",
                    ANNOTATION_NAME, memberName.getText(), length, max));
            }
        }
    }

    private boolean isSdkAdvancedApi(DetailAST annotation) {
        DetailAST nameNode = annotation.findFirstToken(TokenTypes.IDENT);
        if (nameNode == null) {
            nameNode = annotation.findFirstToken(TokenTypes.DOT);
        }
        if (nameNode == null) {
            return false;
        }
        String name = FullIdent.createFullIdent(nameNode).getText();
        return name.equals(ANNOTATION_NAME) || name.endsWith("." + ANNOTATION_NAME);
    }

    private int concatenatedStringLength(DetailAST node) {
        int total = 0;
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.STRING_LITERAL) {
                total += decodedLength(child.getText());
            } else {
                total += concatenatedStringLength(child);
            }
        }
        return total;
    }

    /**
     * Counts the logical characters in a string literal token (its text includes the surrounding quotes), treating each
     * escape sequence as a single character so the count matches the text a consumer of the annotation would see.
     */
    private int decodedLength(String literalWithQuotes) {
        String body = literalWithQuotes;
        if (body.length() >= 2 && body.charAt(0) == '"' && body.charAt(body.length() - 1) == '"') {
            body = body.substring(1, body.length() - 1);
        }

        int count = 0;
        int i = 0;
        while (i < body.length()) {
            if (body.charAt(i) == '\\' && i + 1 < body.length()) {
                char next = body.charAt(i + 1);
                if (next == 'u') {
                    i += 6;
                } else if (next >= '0' && next <= '7') {
                    int j = i + 1;
                    int digits = 0;
                    while (j < body.length() && digits < 3 && body.charAt(j) >= '0' && body.charAt(j) <= '7') {
                        j++;
                        digits++;
                    }
                    i = j;
                } else {
                    i += 2;
                }
            } else {
                i++;
            }
            count++;
        }
        return count;
    }
}
