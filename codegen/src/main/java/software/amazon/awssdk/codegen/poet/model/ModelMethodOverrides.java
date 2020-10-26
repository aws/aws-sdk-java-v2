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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetCollectors;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.utils.ToString;

/**
 * Creates the method specs for common method overrides for service models.
 */
public class ModelMethodOverrides {
    private final PoetExtensions poetExtensions;

    public ModelMethodOverrides(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    public MethodSpec equalsBySdkFieldsMethod(ShapeModel shapeModel) {
        ClassName className = poetExtensions.getModelClass(shapeModel.getShapeName());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("equalsBySdkFields")
                                                     .returns(boolean.class)
                                                     .addAnnotation(Override.class)
                                                     .addModifiers(Modifier.PUBLIC)
                                                     .addParameter(Object.class, "obj")

                                                     .beginControlFlow("if (this == obj)")
                                                     .addStatement("return true")
                                                     .endControlFlow()

                                                     .beginControlFlow("if (obj == null)")
                                                     .addStatement("return false")
                                                     .endControlFlow()

                                                     .beginControlFlow("if (!(obj instanceof $T))", className)
                                                     .addStatement("return false")
                                                     .endControlFlow();

        if (!shapeModel.getNonStreamingMembers().isEmpty()) {
            methodBuilder.addStatement("$T other = ($T) obj", className, className);
        }

        List<MemberModel> memberModels = shapeModel.getNonStreamingMembers();
        CodeBlock.Builder memberEqualsStmt = CodeBlock.builder();
        if (memberModels.isEmpty()) {
            memberEqualsStmt.addStatement("return true");
        } else {
            memberEqualsStmt.add("return ");
            memberEqualsStmt.add(memberModels.stream().map(m -> {
                String getterName = m.getFluentGetterMethodName();

                CodeBlock.Builder result = CodeBlock.builder();
                if (m.getAutoConstructClassIfExists().isPresent()) {
                    String existenceCheckMethodName = m.getExistenceCheckMethodName();
                    result.add("$1N() == other.$1N() && ", existenceCheckMethodName);
                }

                return result.add("$T.equals($N(), other.$N())", Objects.class, getterName, getterName)
                             .build();
            }).collect(PoetCollectors.toDelimitedCodeBlock("&&")));
            memberEqualsStmt.add(";");
        }

        return methodBuilder.addCode(memberEqualsStmt.build()).build();
    }

    public MethodSpec equalsMethod(ShapeModel shapeModel) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("equals")
                                                     .returns(boolean.class)
                                                     .addAnnotation(Override.class)
                                                     .addModifiers(Modifier.PUBLIC)
                                                     .addParameter(Object.class, "obj");


        CodeBlock.Builder memberEqualsStmt = CodeBlock.builder();
        memberEqualsStmt.add("return ");

        if (poetExtensions.isRequest(shapeModel) || poetExtensions.isResponse(shapeModel)) {
            memberEqualsStmt.add("super.equals(obj) && ");
        }

        memberEqualsStmt.add("equalsBySdkFields(obj);");
        return methodBuilder.addCode(memberEqualsStmt.build()).build();
    }

    public MethodSpec toStringMethod(ShapeModel shapeModel) {
        String javadoc = "Returns a string representation of this object. This is useful for testing and " +
                         "debugging. Sensitive data will be redacted from this string using a placeholder " +
                         "value. ";

        MethodSpec.Builder toStringMethod = MethodSpec.methodBuilder("toString")
                                                      .returns(String.class)
                                                      .addAnnotation(Override.class)
                                                      .addModifiers(Modifier.PUBLIC)
                                                      .addJavadoc(javadoc);

        toStringMethod.addCode("return $T.builder($S)", ToString.class, shapeModel.getShapeName());
        shapeModel.getNonStreamingMembers()
                  .forEach(m -> toStringMethod.addCode(".add($S, ", m.getName())
                                              .addCode(toStringValue(m))
                                              .addCode(")"));
        toStringMethod.addCode(".build();");

        return toStringMethod.build();
    }

    public CodeBlock toStringValue(MemberModel member) {
        if (member.isSensitive()) {
            return CodeBlock.of("$L() == null ? null : $S",
                                member.getFluentGetterMethodName(),
                                "*** Sensitive Data Redacted ***");
        }

        if (member.getAutoConstructClassIfExists().isPresent()) {
            return CodeBlock.of("$N() ? $N() : null",
                                member.getExistenceCheckMethodName(),
                                member.getFluentGetterMethodName());
        }

        return CodeBlock.of("$L()", member.getFluentGetterMethodName());
    }

    public MethodSpec hashCodeMethod(ShapeModel shapeModel) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("hashCode")
                                                     .returns(int.class)
                                                     .addAnnotation(Override.class)
                                                     .addModifiers(Modifier.PUBLIC)
                                                     .addStatement("int hashCode = 1");


        if (poetExtensions.isRequest(shapeModel) || poetExtensions.isResponse(shapeModel)) {
            methodBuilder.addStatement("hashCode = 31 * hashCode + super.hashCode()");
        }

        shapeModel.getNonStreamingMembers()
                  .forEach(m -> methodBuilder.addCode("hashCode = 31 * hashCode + $T.hashCode(", Objects.class)
                                             .addCode(hashCodeValue(m))
                                             .addCode(");\n"));

        methodBuilder.addStatement("return hashCode");

        return methodBuilder.build();
    }

    public CodeBlock hashCodeValue(MemberModel member) {
        if (member.getAutoConstructClassIfExists().isPresent()) {
            return CodeBlock.of("$N() ? $N() : null",
                                member.getExistenceCheckMethodName(),
                                member.getFluentGetterMethodName());
        }

        return CodeBlock.of("$N()", member.getFluentGetterMethodName());
    }
}
