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

package software.amazon.awssdk.v2migration.openrewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * This class contains source imported from https://github
 * .com/openrewrite/rewrite/blob/main/rewrite-java/src/main/java/org/openrewrite/java/ChangeMethodInvocationReturnType.java,
 * licensed under the Apache License 2.0, available at the time of the fork (4/11/2025) here:
 * https://github.com/openrewrite/rewrite/blob/main/LICENSE
 * <p>
 * All original source licensed under the Apache License 2.0. All modifications are licensed under the Apache
 * License 2.0 by Amazon Web Services.
 */
@SdkInternalApi
public class ChangeMethodInvocationReturnType extends Recipe {

    @Option(displayName = "Method pattern",
        description = "A method pattern is used to find matching method invocations.",
        example = "org.mockito.Matchers anyVararg()")
    private final String methodPattern;

    @Option(displayName = "New method invocation return type",
        description = "The fully qualified new return type of method invocation.",
        example = "long")
    private final String newReturnType;

    @JsonCreator
    public ChangeMethodInvocationReturnType(@JsonProperty("methodPattern") String methodPattern,
                                            @JsonProperty("newReturnType") String newReturnType) {
        this.methodPattern = methodPattern;
        this.newReturnType = newReturnType;
    }

    @Override
    public String getDisplayName() {
        return "Change method invocation return type";
    }

    @Override
    public String getDescription() {
        return "Changes the return type of a method invocation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

            private boolean methodUpdated;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (methodMatcher.matches(method) && type != null && !newReturnType.equals(type.getReturnType().toString())) {
                    type = type.withReturnType(JavaType.buildType(newReturnType));
                    m = m.withMethodType(type);
                    if (m.getName().getType() != null) {
                        m = m.withName(m.getName().withType(type));
                    }
                    methodUpdated = true;
                }
                return m;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                methodUpdated = false;
                JavaType.FullyQualified originalType = multiVariable.getTypeAsFullyQualified();
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                if (methodUpdated) {
                    JavaType newType = JavaType.buildType(newReturnType);
                    JavaType.FullyQualified newFieldType = TypeUtils.asFullyQualified(newType);

                    maybeAddImport(newFieldType);
                    maybeRemoveImport(originalType);

                    mv = mv.withTypeExpression(mv.getTypeExpression() == null ?
                                               null :
                                               new J.Identifier(mv.getTypeExpression().getId(),
                                                                mv.getTypeExpression().getPrefix(),
                                                                Markers.EMPTY,
                                                                Collections.emptyList(),
                                                                newReturnType.substring(newReturnType.lastIndexOf('.') + 1),
                                                                newType,
                                                                null
                                               )
                    );

                    mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                        JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                        if (varType != null && !varType.equals(newType)) {
                            return var.withType(newType).withName(var.getName().withType(newType));
                        }
                        return var;
                    }));
                }

                return mv;
            }
        };
    }
}