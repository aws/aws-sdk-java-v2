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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AsyncPathNonBlockingCheck extends AbstractCheck {

    private List<String> illegalMethods = new ArrayList<>();

    public void setIllegalMethods(String... illegalMethods) {
        this.illegalMethods.clear();
        this.illegalMethods.addAll(Arrays.asList(illegalMethods));
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
        return new int[]{
            TokenTypes.METHOD_CALL
        };
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (ast.getType() != TokenTypes.METHOD_CALL) {
            return;
        }

        // find method calls that look like `someVar.join()`
        DetailAST dot = ast.findFirstToken(TokenTypes.DOT);
        if (dot == null) {
            return;
        }

        DetailAST methodCall = dot.getLastChild();
        if (illegalMethods.contains(methodCall.getText())) {
            // allow String.join(...);
            if ("String".equals(dot.getFirstChild().getText())) {
                return;
            }
            log(ast, methodCall + " is not allowed to be called.");
        }
    }
}
