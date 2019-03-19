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

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.AnnotationUtil;
import com.puppycrawl.tools.checkstyle.utils.ScopeUtil;
import java.util.Arrays;
import java.util.List;

/**
 * A rule that checks if sdk annotation is missing on any non-test classes. Inner classes are ignored.
 */
public class MissingSdkAnnotationCheck extends AbstractCheck {

    private static final List<String> SDK_ANNOTATIONS = Arrays.asList("SdkPublicApi", "SdkInternalApi", "SdkProtectedApi");

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
            TokenTypes.CLASS_DEF,
            TokenTypes.INTERFACE_DEF
        };
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (!ScopeUtil.isOuterMostType(ast) || SDK_ANNOTATIONS.stream().anyMatch(a -> AnnotationUtil.containsAnnotation
            (ast, a))) {
            return;
        }

        log(ast, "SDK annotation is missing on this class. eg: @SdkProtectedApi");
    }
}
