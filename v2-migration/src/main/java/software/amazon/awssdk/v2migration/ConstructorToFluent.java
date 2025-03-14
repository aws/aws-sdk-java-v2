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

import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.fullyQualified;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class ConstructorToFluent extends Recipe {
    private final String clzzFqcn;
    private final List<String> parameterTypes;
    private final List<String> fluentNames;

    @JsonCreator
    public ConstructorToFluent(@JsonProperty("clzzFqcn") String clzzFqcn,
                               @JsonProperty("parameterTypes") List<String> parameterTypes,
                               @JsonProperty("fluentNames") List<String> fluentNames) {
        this.clzzFqcn = clzzFqcn;
        this.parameterTypes = parameterTypes;
        this.fluentNames = fluentNames;

        if (fluentNames.size() != parameterTypes.size()) {
            throw new IllegalArgumentException("parameterTypes and fluentNames must be the same length.");
        }
    }

    @Override
    public String getDisplayName() {
        return "Moves constructor arguments to fluent setters";
    }

    @Override
    public String getDescription() {
        return "A recipe that takes constructor arguments and moves them to the specified fluent setters on the object.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<JavaType> paramJavaTypes = parameterTypes.stream()
            .map(JavaType::buildType)
            .collect(Collectors.toList());
        return new Visitor(clzzFqcn, paramJavaTypes, fluentNames);
    }

    private static class Visitor extends JavaVisitor<ExecutionContext> {
        private final JavaType.FullyQualified clzz;
        private final List<JavaType> parameterTypes;
        private final List<String> fluentNames;

        Visitor(String clzz, List<JavaType> parameterTypes, List<String> fluentNames) {
            this.clzz = fullyQualified(clzz);
            this.parameterTypes = parameterTypes;
            this.fluentNames = fluentNames;
        }

        @Override
        public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
            JavaType.Method ctorType = newClass.getMethodType();

            if (ctorType == null) {
                return newClass;
            }

            if (!clzz.isAssignableFrom(ctorType.getDeclaringType())) {
                return newClass;
            }

            List<JavaType> paramTypes = ctorType.getParameterTypes();

            if (paramTypes.size() != this.parameterTypes.size()) {
                return newClass;
            }

            for (int i = 0; i < parameterTypes.size(); ++i) {
                JavaType expected = this.parameterTypes.get(i);
                if (!TypeUtils.isAssignableTo(expected, paramTypes.get(i))) {
                    return newClass;
                }
            }

            // Remove all the arguments from the constructor
            List<Expression> arguments = newClass.getArguments();

            ctorType = ctorType.withParameterTypes(Collections.emptyList())
                               .withParameterNames(Collections.emptyList());

            newClass = newClass.withArguments(Collections.emptyList())
                               .withMethodType(ctorType);

            JavaType.FullyQualified declaringType = ctorType.getDeclaringType();
            Expression select = newClass;
            for (int i = 0; i < parameterTypes.size(); ++i) {
                JavaType paramType = parameterTypes.get(i);
                String name = fluentNames.get(i);
                // Note we don't preserve prefix so we don't end up with
                // extra spaces after the opening paren like 'withFoo(  arg)'
                Expression argExpr = arguments.get(i).withPrefix(Space.EMPTY);

                select = addWither(select, name, paramType, argExpr, declaringType);
            }

            return select;
        }

        private static J.MethodInvocation addWither(Expression select, String simpleName,
                                                    JavaType parameterType,
                                                    Expression paramExpr,
                                                    JavaType.FullyQualified declaringType) {
            JavaType.Method methodType = new JavaType.Method(
                null,
                0L,
                declaringType,
                simpleName,
                declaringType,
                Collections.singletonList(simpleName),
                Collections.singletonList(parameterType),
                null,
                null
            );

            J.Identifier witherId = new J.Identifier(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Collections.emptyList(),
                simpleName,
                methodType,
                null
            );

            return new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(select),
                null,
                witherId,
                JContainer.build(Collections.singletonList(JRightPadded.build(paramExpr))),
                methodType
            );
        }
    }
}
