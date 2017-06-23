/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

/**
 * Creates the method specs for common method overrides for service models.
 */
public class ModelMethodOverrides {
    private final PoetExtensions poetExtensions;

    public ModelMethodOverrides(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    public MethodSpec equalsMethod(ShapeModel shapeModel) {
        ClassName className = poetExtensions.getModelClass(shapeModel.getShapeName());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("equals")
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

        shapeModel.getNonStreamingMembers().forEach(m -> {
            String getterName = m.getFluentGetterMethodName();
            methodBuilder.beginControlFlow("if (other.$N() == null ^ this.$N() == null)", getterName, getterName)
                         .addStatement("return false")
                         .endControlFlow()

                         .beginControlFlow("if (other.$N() != null && !other.$N().equals(this.$N()))", getterName, getterName,
                                           getterName)
                         .addStatement("return false")
                         .endControlFlow();
        });

        methodBuilder.addStatement("return true");

        return methodBuilder.build();
    }

    public MethodSpec toStringMethod(ShapeModel shapeModel) {
        MethodSpec.Builder toStringMethodBuilder = MethodSpec.methodBuilder("toString")
                                                             .returns(String.class)
                                                             .addAnnotation(Override.class)
                                                             .addModifiers(Modifier.PUBLIC)
                                                             .addStatement("$T sb = new $T()", StringBuilder.class,
                                                                           StringBuilder.class)
                                                             .addStatement("sb.append(\"{\")");

        shapeModel.getNonStreamingMembers()
                  .forEach(m -> {
                      String getterName = m.getFluentGetterMethodName();
                      toStringMethodBuilder.beginControlFlow("if ($N() != null)", getterName)
                                           .addStatement("sb.append(\"$N: \").append($N()).append(\",\")", m.getName(),
                                                         getterName)
                                           .endControlFlow();
                  });

        toStringMethodBuilder.addStatement("sb.append(\"}\")");
        toStringMethodBuilder.addStatement("return sb.toString()");

        return toStringMethodBuilder.build();
    }

    public MethodSpec hashCodeMethod(ShapeModel shapeModel) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("hashCode")
                                                     .returns(int.class)
                                                     .addAnnotation(Override.class)
                                                     .addModifiers(Modifier.PUBLIC)
                                                     .addStatement("int hashCode = 1");

        shapeModel.getNonStreamingMembers()
                  .forEach(m -> methodBuilder.addStatement(
                          "hashCode = 31 * hashCode + (($N() == null)? 0 : $N().hashCode())",
                          m.getFluentGetterMethodName(),
                          m.getFluentGetterMethodName()));

        methodBuilder.addStatement("return hashCode");

        return methodBuilder.build();
    }
}
