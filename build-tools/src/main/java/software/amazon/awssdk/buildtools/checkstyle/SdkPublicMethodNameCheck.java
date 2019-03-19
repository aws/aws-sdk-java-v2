/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.naming.MethodNameCheck;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtil;

/**
 * Sdk Method Name check to check only public methods in the classes with {@code @SdkPublicApi} annotation.
 */
public final class SdkPublicMethodNameCheck extends MethodNameCheck {

    /**
     * {@link Override Override} annotation name.
     */
    private static final String OVERRIDE = "Override";

    private static final String SDK_PUBLIC_API = "SdkPublicApi";

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
        return new int[] {
            TokenTypes.METHOD_DEF
        };
    }

    @Override
    public void visitToken(DetailAST ast) {
        // Get class classDef
        DetailAST classDef = ast.getParent().getParent();

        try {
            if (!AnnotationUtil.containsAnnotation(ast, OVERRIDE)
                && AnnotationUtil.containsAnnotation(classDef, SDK_PUBLIC_API)) {
                super.visitToken(ast);
            }
        } catch (NullPointerException ex) {
            //If that method is in an anonymous class, it will throw NPE, ignoring those.
        }

    }

    @Override
    protected boolean mustCheckName(DetailAST ast) {
        return ast.findFirstToken(TokenTypes.MODIFIERS)
                  .findFirstToken(TokenTypes.LITERAL_PUBLIC) != null;
    }
}
