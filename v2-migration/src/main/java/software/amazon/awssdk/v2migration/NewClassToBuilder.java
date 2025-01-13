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

import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isEligibleToConvertToBuilder;

import java.util.Collections;
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
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.v2migration.internal.utils.IdentifierUtils;
import software.amazon.awssdk.v2migration.internal.utils.NamingUtils;
import software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils;

/**
 * Internal recipe that converts new class creation to the builder pattern.
 *
 * @see NewClassToBuilderPattern
 */
@SdkInternalApi
public class NewClassToBuilder extends Recipe {
    @Override
    public String getDisplayName() {
        return "Transform 'new' expressions to builders";
    }

    @Override
    public String getDescription() {
        return "Transforms 'new' expression for generated model, client objects and client config related objects to the "
               + "equivalent builder()..build() expression in V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new NewV1ClassToBuilderVisitor();
    }

    // Change a new Foo() to Foo.builder().build()
    // Any withers called on new Foo() are moved to before .build()
    // Make any appropriate v1 -> v2 type changes
    private static class NewV1ClassToBuilderVisitor extends JavaVisitor<ExecutionContext> {
        // Rearrange a [...].build().with*() to [...].with*().build()
        @Override
        public J visitMethodInvocation(J.MethodInvocation previousMethod, ExecutionContext executionContext) {
            J.MethodInvocation method = super.visitMethodInvocation(previousMethod, executionContext).cast();

            // [...].with*()
            if (!NamingUtils.isWither(method.getSimpleName())) {
                return method;
            }

            Expression select = method.getSelect();

            if (!(select instanceof J.MethodInvocation)) {
                return method;
            }

            // [...]
            J.MethodInvocation selectInvoke = (J.MethodInvocation) select;

            // [...] == [...].build()
            if (!selectInvoke.getSimpleName().equals("build")) {
                return method;
            }

            // Do the reordering
            Expression selectInvokeSelect = selectInvoke.getSelect();

            J.MethodInvocation newWith = method.withSelect(selectInvokeSelect);

            return maybeAutoFormat(previousMethod, selectInvoke.withSelect(newWith), executionContext);
        }

        // new Foo() -> Foo.builder().build()
        @Override
        public J visitNewClass(J.NewClass previousNewClass, ExecutionContext executionContext) {
            J.NewClass newClass = super.visitNewClass(previousNewClass, executionContext).cast();

            if (!(newClass.getType() instanceof JavaType.FullyQualified)) {
                return newClass;
            }

            JavaType.FullyQualified classType = (JavaType.FullyQualified) newClass.getType();

            if (!isEligibleToConvertToBuilder(classType) ||
                !newClass.getConstructorType().getParameterNames().isEmpty()) {
                return newClass;
            }

            JavaType.FullyQualified builderType = SdkTypeUtils.v2Builder(classType);

            J.Identifier modelId = new J.Identifier(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                Collections.emptyList(),
                classType.getClassName(),
                classType,
                null
            );

            JavaType.Method methodType = new JavaType.Method(
                null,
                0L,
                classType,
                "builder",
                builderType,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );

            J.Identifier builderMethod = IdentifierUtils.makeId("builder", methodType);

            J.MethodInvocation builderInvoke = new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(modelId),
                null,
                builderMethod,
                JContainer.empty(),
                methodType
            );

            JavaType.Method buildMethodType = new JavaType.Method(
                null,
                0L,
                builderType,
                "build",
                classType,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );

            J.Identifier buildName = IdentifierUtils.makeId("build", buildMethodType);

            J.MethodInvocation buildInvoke = new J.MethodInvocation(
                Tree.randomId(),
                newClass.getPrefix(),
                Markers.EMPTY,
                new JRightPadded(builderInvoke, Space.format("\n"), Markers.EMPTY),
                null,
                buildName,
                JContainer.empty(),
                buildMethodType
            );

            return maybeAutoFormat(previousNewClass, buildInvoke, executionContext);
        }
    }
}
