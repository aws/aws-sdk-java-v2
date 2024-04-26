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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.migration.internal.utils.IdentifierUtils;

public class S3StreamingResponseToV2 extends Recipe {
    private static final JavaType S3_OBJECT_TYPE = JavaType.buildType("com.amazonaws.services.s3.model.S3Object");
    private static final JavaType.FullyQualified S3_GET_OBJECT_RESPONSE_TYPE =
        TypeUtils.asFullyQualified(JavaType.buildType("software.amazon.awssdk.services.s3.model.GetObjectResponse"));
    private static final JavaType.FullyQualified RESPONSE_INPUT_STREAM_TYPE =
        TypeUtils.asFullyQualified(JavaType.buildType("software.amazon.awssdk.core.ResponseInputStream"));
    @Override
    public String getDisplayName() {
        return "Streaming v1 response";
    }

    @Override
    public String getDescription() {
        return "Streaming v1 response.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static class Visitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            if (TypeUtils.isAssignableTo(S3_OBJECT_TYPE, multiVariable.getType())) {
                JavaType.Parameterized newType = new JavaType.Parameterized(null, RESPONSE_INPUT_STREAM_TYPE,
                                                                            Collections.singletonList(S3_GET_OBJECT_RESPONSE_TYPE));

                maybeAddImport(RESPONSE_INPUT_STREAM_TYPE);
                maybeAddImport(S3_GET_OBJECT_RESPONSE_TYPE);

                multiVariable = multiVariable.withType(newType)
                                             .withTypeExpression(new J.Identifier(
                                                 Tree.randomId(),
                                                 Space.EMPTY,
                                                 Markers.EMPTY,
                                                 Collections.emptyList(),
                                                 IdentifierUtils.simpleName(newType),
                                                 newType,
                                                 null
                                             ));

                List<J.VariableDeclarations.NamedVariable> variables = multiVariable.getVariables().stream()
                                                                                  .map(nv -> nv.withType(newType))
                                                                                  .collect(Collectors.toList());

                multiVariable = multiVariable.withVariables(variables);
            }
            multiVariable = super.visitVariableDeclarations(multiVariable, executionContext).cast();

            return multiVariable;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            method = super.visitMethodInvocation(method, executionContext).cast();

            JavaType.Method methodType = method.getMethodType();

            if (methodType == null) {
                return method;
            }

            if (!TypeUtils.isAssignableTo(S3_OBJECT_TYPE, methodType.getDeclaringType())) {
                return method;
            }

            if ("getObjectContent".equals(method.getSimpleName())) {
                return method.getSelect();
            }

            return method;
        }
    }
}
