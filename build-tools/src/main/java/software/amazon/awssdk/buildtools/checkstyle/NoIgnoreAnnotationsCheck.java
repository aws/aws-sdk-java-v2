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
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtil;

/**
 * Checks if a class uses the @Ignore annotation. Avoid disabling tests and work to
 * resolve issues with the test instead.
 *
 * For manual tests and exceptional circumstances, use the commentation feature CHECKSTYLE: OFF
 * to mark a test as ignored.
 */
public class NoIgnoreAnnotationsCheck extends AbstractCheck {

    private static final String IGNORE_ANNOTATION = "Ignore";

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
        return new int[] {TokenTypes.CLASS_DEF, TokenTypes.METHOD_DEF};
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (!AnnotationUtil.containsAnnotation(ast, IGNORE_ANNOTATION)) {
            return;
        }

        log(ast, "@Ignore annotation is not allowed");
    }
}
