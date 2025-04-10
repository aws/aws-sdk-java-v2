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

package software.amazon.awssdk.v2migration;

import static software.amazon.awssdk.v2migration.internal.utils.S3TransformUtils.changeBucketNameToBucket;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isV2ModelClass;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.v2migration.internal.utils.NamingUtils;

@SdkInternalApi
public class V1GetterToV2 extends Recipe {
    @Override
    public String getDisplayName() {
        return "V1 Getter to V2";
    }

    @Override
    public String getDescription() {
        return "Transforms V1 getter to fluent getter in V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new V1GetterToV2Visitor();
    }

    private static class V1GetterToV2Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            method = super.visitMethodInvocation(method, executionContext);

            JavaType.Method methodType = method.getMethodType();

            if (methodType == null) {
                return method;
            }

            String methodName = method.getSimpleName();
            JavaType.FullyQualified fullyQualified = methodType.getDeclaringType();

            if (!shouldChangeGetter(fullyQualified)) {
                return method;
            }

            methodName = changeBucketNameToBucket(methodName);

            if (NamingUtils.isGetter(methodName)) {
                methodName = NamingUtils.removeGet(methodName);
            }

            methodType = methodType.withName(methodName);
            method = method.withName(method.getName()
                                           .withSimpleName(methodName)
                                           .withType(methodType))
                           .withMethodType(methodType);

            return method;
        }

        private static boolean shouldChangeGetter(JavaType.FullyQualified selectType) {
            return isV2ModelClass(selectType);
        }
    }
}
