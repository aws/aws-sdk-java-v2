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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.v2migration.internal.utils.IdentifierUtils;

@SdkInternalApi
public class S3StreamingResponseToV2 extends Recipe {
    private static final JavaType.FullyQualified S3_OBJECT_TYPE =
        TypeUtils.asFullyQualified(JavaType.buildType("com.amazonaws.services.s3.model.S3Object"));
    private static final JavaType.FullyQualified S3_GET_OBJECT_RESPONSE_TYPE =
        TypeUtils.asFullyQualified(JavaType.buildType("software.amazon.awssdk.services.s3.model.GetObjectResponse"));

    private static final JavaType.FullyQualified RESPONSE_INPUT_STREAM_TYPE =
        TypeUtils.asFullyQualified(JavaType.buildType("software.amazon.awssdk.core.ResponseInputStream"));

    private static final MethodMatcher GET_OBJECT_CONTENT =
        new MethodMatcher("com.amazonaws.services.s3.model.S3Object getObjectContent()");

    private static final MethodMatcher OBJECT_INPUT_STREAM_METHOD =
        new MethodMatcher("com.amazonaws.services.s3.model.S3ObjectInputStream *(..)");

    @Override
    public String getDisplayName() {
        return "V1 S3Object to V2";
    }

    @Override
    public String getDescription() {
        return "Transform usage of V1 S3Object to V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Visitor();
    }

    private static class Visitor extends JavaVisitor<ExecutionContext> {

        // Transform an S3Object myObject = ...
        // to
        // ResponseInputStream<GetObjectResponse> myObject = ...
        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                                ExecutionContext executionContext) {
            if (TypeUtils.isAssignableTo(S3_OBJECT_TYPE, multiVariable.getType())) {
                JavaType.Parameterized newType =
                    new JavaType.Parameterized(null, RESPONSE_INPUT_STREAM_TYPE,
                                               Collections.singletonList(S3_GET_OBJECT_RESPONSE_TYPE));

                maybeAddS3ResponseImports();

                multiVariable = multiVariable.withType(newType)
                                             .withTypeExpression(IdentifierUtils.makeId(IdentifierUtils.simpleName(newType),
                                                                                        newType));

                List<J.VariableDeclarations.NamedVariable> variables = multiVariable.getVariables().stream()
                                                                                  .map(nv -> nv.withType(newType))
                                                                                  .collect(Collectors.toList());

                multiVariable = multiVariable.withVariables(variables);
            }

            multiVariable = super.visitVariableDeclarations(multiVariable, executionContext).cast();

            return multiVariable;
        }

        private void maybeAddS3ResponseImports() {
            maybeAddImport(RESPONSE_INPUT_STREAM_TYPE);
            // Note: 'onlyIfReferenced' set to false because OpenRewrite does not seem to consider just having the type as a type
            // parameter as being in use, so will not add the import if set to true.
            maybeAddImport(S3_GET_OBJECT_RESPONSE_TYPE.getFullyQualifiedName(), null, false);
        }

        // In v2, the request is inverted. Whereas S3Object contains the response stream, in V2, a response stream object
        // wraps the response object. so the InputStream methods are directly on the "response" object now, and to access the
        // non-streaming members, we need to insert a call to response().
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            method = super.visitMethodInvocation(method, executionContext).cast();

            Expression select = method.getSelect();

            if (select == null) {
                return method;
            }

            if (GET_OBJECT_CONTENT.matches(method)) {
                return select.withPrefix(method.getPrefix());
            }

            JavaType.Method methodType = method.getMethodType();

            if (methodType == null) {
                return method;
            }

            if (!TypeUtils.isAssignableTo(S3_OBJECT_TYPE, methodType.getDeclaringType())) {
                return method;
            }

            // Calling a method on the stream, just change the declaring type
            if (isObjectContentMethod(method)) {
                method = method.withMethodType(methodType.withDeclaringType(RESPONSE_INPUT_STREAM_TYPE));
                return method;
            }

            JavaType.Method responseGetterType = new JavaType.Method(
                null,
                0L,
                RESPONSE_INPUT_STREAM_TYPE,
                "response",
                S3_GET_OBJECT_RESPONSE_TYPE,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );

            // Calling a method on the response, insert a response() getter
            J.Identifier responseGetterId = IdentifierUtils.makeId("response", responseGetterType);

            J.MethodInvocation getResponse = new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(select),
                null,
                responseGetterId,
                JContainer.empty(),
                responseGetterType
            );

            methodType = methodType.withDeclaringType(S3_GET_OBJECT_RESPONSE_TYPE);
            method = method.withSelect(getResponse)
                           .withName(method.getName().withType(methodType))
                           .withMethodType(methodType);

            return method;
        }

        private static boolean isObjectContentMethod(J.MethodInvocation method) {
            Expression select = method.getSelect();
            if (select == null || !TypeUtils.isAssignableTo(S3_OBJECT_TYPE, select.getType())) {
                return false;
            }
            return OBJECT_INPUT_STREAM_METHOD.matches(method);
        }
    }
}
