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

import static software.amazon.awssdk.migration.internal.utils.SdkTypeUtils.isV2ClientClass;
import static software.amazon.awssdk.migration.internal.utils.SdkTypeUtils.isV2ModelClass;

import java.util.Map;
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
import software.amazon.awssdk.migration.recipe.NewClassToBuilderPattern;
import software.amazon.awssdk.utils.ImmutableMap;

/**
 * Internal recipe that renames fluent V1 setters (withers), to V2 equivalents
 * for generated model classes and client classes.
 *
 * @see NewClassToBuilderPattern
 */
@SdkInternalApi
public class V1SetterToV2 extends Recipe {
    private static final Map<String, String> CLIENT_CONFIG_NAMING_MAPPING =
        ImmutableMap.<String, String>builder()
                    .put("credentials", "credentialsProvider")
                    .put("clientConfiguration", "overrideConfiguration")
                    .put("endpointConfiguration", "endpointOverride")
                    .build();

    @Override
    public String getDisplayName() {
        return "V1 Setter to V2";
    }

    @Override
    public String getDescription() {
        return "Transforms V1 setter to fluent setter in V2.";
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

            if (selectType == null || !shouldChangeSetter(selectType)) {
                return method;
            }

            String methodName = method.getSimpleName();

            if (NamingUtils.isWither(methodName)) {
                methodName = NamingUtils.removeWith(methodName);
            } else if (NamingUtils.isSetter(methodName)) {
                methodName = NamingUtils.removeSet(methodName);
            }

            if (isV2ClientClass(selectType)) {
                methodName = CLIENT_CONFIG_NAMING_MAPPING.getOrDefault(methodName, methodName);
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

            return method;
        }

        private static boolean shouldChangeSetter(JavaType selectType) {
            return isV2ModelClass(selectType) || isV2ClientClass(selectType);
        }
    }
}
