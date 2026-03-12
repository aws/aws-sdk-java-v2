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

import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.V2_CORE_CLASSES_WITH_STATIC_FACTORY;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isEligibleToConvertToStaticFactory;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Internal recipe that converts new class creation to static factory method
 *
 */
@SdkInternalApi
public class NewClassToStaticFactory extends Recipe {
    @Override
    public String getDisplayName() {
        return "Transform 'new' expressions to static factory methods";
    }

    @Override
    public String getDescription() {
        return "Transforms 'new' expression for client config related objects to the "
               + "equivalent .create() expression in V2.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new NewClassToStaticFactoryVisitor();
    }

    // Change a new Foo() to Foo.create()
    private static class NewClassToStaticFactoryVisitor extends JavaVisitor<ExecutionContext> {
        // new Foo() -> Foo.create()
        @Override
        public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
            newClass = super.visitNewClass(newClass, executionContext).cast();

            if (!(newClass.getType() instanceof JavaType.FullyQualified)) {
                return newClass;
            }

            JavaType.FullyQualified classType = (JavaType.FullyQualified) newClass.getType();

            if (!isEligibleToConvertToStaticFactory(classType)) {
                return newClass;
            }

            String fullyQualifiedName = classType.getFullyQualifiedName();
            int numOfParams = V2_CORE_CLASSES_WITH_STATIC_FACTORY.get(fullyQualifiedName);

            switch (numOfParams) {
                case 0:
                    return JavaTemplate.builder(classType.getClassName() + ".create()")
                                       .build()
                                       .apply(getCursor(), newClass.getCoordinates().replace());
                case 1:
                    return JavaTemplate.builder(String.format("%s.create(#{any()})", classType.getClassName()))
                                       .build()
                                       .apply(getCursor(), newClass.getCoordinates().replace(), newClass.getArguments().get(0));
                case 2:
                    return JavaTemplate.builder(String.format("%s.create(#{any()}, #{any()})", classType.getClassName()))
                                       .build()
                                       .apply(getCursor(), newClass.getCoordinates().replace(), newClass.getArguments().get(0),
                                              newClass.getArguments().get(1));
                case 3:
                    return JavaTemplate.builder(String.format("%s.create(#{any()}, #{any()}, #{any()})",
                                                              classType.getClassName()))
                                       .build()
                                       .apply(getCursor(), newClass.getCoordinates().replace(),
                                              newClass.getArguments().get(0),
                                              newClass.getArguments().get(1),
                                              newClass.getArguments().get(2));
                default:
                    throw new UnsupportedOperationException("Unsupported number of parameters: " + numOfParams);
            }
        }
    }
}
