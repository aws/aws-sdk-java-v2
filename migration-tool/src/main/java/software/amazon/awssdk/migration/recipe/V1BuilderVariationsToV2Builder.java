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

package software.amazon.awssdk.migration.recipe;

import static software.amazon.awssdk.migration.internal.utils.SdkTypeUtils.isV2AsyncClientClass;
import static software.amazon.awssdk.migration.internal.utils.SdkTypeUtils.isV2ClientBuilder;

import java.util.Collections;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.migration.internal.utils.SdkTypeUtils;

/**
 * Internal recipe that renames V1 client builder variations. For example: {@code SqsClientBuilder.standard().build()} to
 * {@code SqsClient.builder().build()} and {@code SqsAsyncClient.asyncBuilder().build()} to
 * {@code SqsAsyncClient.builder().build()}.
 */
@SdkInternalApi
public class V1BuilderVariationsToV2Builder extends Recipe {

    @Override
    public String getDisplayName() {
        return "V1 client builder variations to builder()";
    }

    @Override
    public String getDescription() {
        return "Transforms V1 builder variations to builder()";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static class Visitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            method = super.visitMethodInvocation(method, executionContext);

            JavaType selectType = null;

            Expression select = method.getSelect();
            if (select != null) {
                selectType = select.getType();
            }

            if (selectType == null || !shouldChangeMethod(selectType)) {
                return method;
            }

            if (isV2AsyncClientClass(selectType)) {
                return renameAsyncBuilderToBuilder(method, selectType);
            }

            if (isV2ClientBuilder(selectType)) {
                return renameStandardToBuilder(method, selectType);
            }

            return method;
        }

        private static boolean shouldChangeMethod(JavaType selectType) {
            return isV2ClientBuilder(selectType) || isV2AsyncClientClass(selectType);
        }

        private J.MethodInvocation renameStandardToBuilder(J.MethodInvocation method, JavaType selectType) {
            String methodName = method.getSimpleName();
            JavaType.Method mt = method.getMethodType();
            JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(selectType);

            if (mt == null || !"standard".equals(methodName) || fullyQualified == null) {
                return method;
            }

            methodName = "builder";

            JavaType.FullyQualified v2Client = SdkTypeUtils.v2ClientFromClientBuilder(fullyQualified);
            J.Identifier id = new J.Identifier(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Collections.emptyList(),
                v2Client.getClassName(),
                v2Client,
                null
            );

            J.Identifier builderMethod = new J.Identifier(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Collections.emptyList(),
                methodName,
                null,
                null
            );

            JavaType.Method methodType = new JavaType.Method(
                null,
                0L,
                v2Client,
                methodName,
                v2Client,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );

            J.MethodInvocation builderInvoke = new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(id),
                null,
                builderMethod,
                JContainer.empty(),
                methodType
            );

            maybeRemoveImport(fullyQualified);
            maybeAddImport(v2Client);
            return builderInvoke;
        }

        private J.MethodInvocation renameAsyncBuilderToBuilder(J.MethodInvocation method, JavaType selectType) {
            String methodName = method.getSimpleName();
            JavaType.Method mt = method.getMethodType();
            JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(selectType);

            if (mt == null || fullyQualified == null) {
                return method;
            }

            if ("asyncBuilder".equals(methodName)) {
                methodName = "builder";
            }

            mt = mt.withName(methodName)
                   .withReturnType(selectType)
                   .withDeclaringType(fullyQualified);

            method = method.withName(method.getName()
                                           .withSimpleName(methodName)
                                           .withType(mt))
                           .withMethodType(mt);
            return method;
        }
    }

}
