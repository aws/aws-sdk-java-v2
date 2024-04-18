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

package software.amazon.awssdk.migration.internal.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.migration.internal.utils.NamingUtils;
import software.amazon.awssdk.migration.internal.utils.SdkTypeUtils;
import software.amazon.awssdk.migration.recipe.NewV1ModelClassToV2;

/**
 * Internal recipe that renames fluent V1 setters (withers), to V2 equivalents.
 *
 * @see NewV1ModelClassToV2
 */
@SdkInternalApi
public class V1SetterToV2 extends Recipe {
    @Override
    public String getDisplayName() {
        return "V1 Setter to V2";
    }

    @Override
    public String getDescription() {
        return "Transforms a setter on a V1 model object to the equivalent in V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new V1SetterToV2Visitor();
    }

    private static class V1SetterToV2Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            method = super.visitMethodInvocation(method, executionContext);

            JavaType selectType = null;

            Expression select = method.getSelect();
            if (select != null) {
                selectType = select.getType();
            }

            if (selectType == null) {
                return method;
            }

            if (SdkTypeUtils.isV2ModelBuilder(selectType) && !SdkTypeUtils.isV2ModelBuilder(method.getType())) {
                String methodName = method.getSimpleName();

                if (NamingUtils.isWither(methodName)) {
                    methodName = NamingUtils.removeWith(methodName);
                } else if (NamingUtils.isSetter(methodName)) {
                    methodName = NamingUtils.removeSet(methodName);
                }

                JavaType.Method mt = method.getMethodType();

                if (mt != null) {
                    mt = mt.withName(methodName)
                           .withReturnType(selectType);

                    JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(selectType);

                    if (fullyQualified != null) {
                        mt = mt.withDeclaringType(fullyQualified);
                    }

                    method = method.withName(method.getName()
                                                   .withSimpleName(methodName)
                                                   .withType(mt))
                                   .withMethodType(mt);
                }
            }

            return method;
        }
    }
}
