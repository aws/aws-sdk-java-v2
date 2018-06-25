/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet;

import static software.amazon.awssdk.utils.StringUtils.isNotBlank;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.codegen.model.intermediate.DocumentationModel;
import software.amazon.awssdk.codegen.model.intermediate.HasDeprecation;

public final class PoetUtils {
    private static final AnnotationSpec GENERATED = AnnotationSpec.builder(Generated.class)
                                                                  .addMember("value", "$S", "software.amazon.awssdk:codegen")
                                                                  .build();

    private PoetUtils() {
    }

    public static AnnotationSpec generatedAnnotation() {
        return GENERATED;
    }

    public static MethodSpec.Builder toStringBuilder() {
        return MethodSpec.methodBuilder("toString")
                         .returns(String.class)
                         .addModifiers(Modifier.PUBLIC)
                         .addAnnotation(Override.class);
    }

    public static void addDeprecated(Consumer<Class<?>> builder, HasDeprecation deprecation) {
        if (deprecation.isDeprecated()) {
            addDeprecated(builder);
        }
    }

    public static void addDeprecated(Consumer<Class<?>> builder) {
        builder.accept(Deprecated.class);
    }

    public static void addJavadoc(Consumer<String> builder, String javadoc) {
        if (isNotBlank(javadoc)) {
            builder.accept(javadoc.replace("$", "$$") + (javadoc.endsWith("\n") ? "" : "\n"));
        }
    }

    public static void addJavadoc(Consumer<String> builder, DocumentationModel docModel) {
        addJavadoc(builder, docModel.getDocumentation());
    }

    public static TypeSpec.Builder createEnumBuilder(ClassName name) {
        return TypeSpec.enumBuilder(name).addAnnotation(PoetUtils.generatedAnnotation()).addModifiers(Modifier.PUBLIC);
    }

    public static TypeSpec.Builder createInterfaceBuilder(ClassName name) {
        return TypeSpec.interfaceBuilder(name).addAnnotation(PoetUtils.generatedAnnotation()).addModifiers(Modifier.PUBLIC);
    }

    public static TypeSpec.Builder createClassBuilder(ClassName name) {
        return TypeSpec.classBuilder(name).addAnnotation(PoetUtils.generatedAnnotation());
    }

    public static ParameterizedTypeName createParameterizedTypeName(ClassName className, String... typeVariables) {
        TypeName[] typeParameters = Arrays.stream(typeVariables).map(TypeVariableName::get).toArray(TypeName[]::new);
        return ParameterizedTypeName.get(className, typeParameters);
    }

    public static ParameterizedTypeName createParameterizedTypeName(Class<?> clazz, String... typeVariables) {
        return createParameterizedTypeName(ClassName.get(clazz), typeVariables);
    }

    public static TypeVariableName createBoundedTypeVariableName(String parameterName, ClassName upperBound,
                                                                 String... typeVariables) {
        return TypeVariableName.get(parameterName, createParameterizedTypeName(upperBound, typeVariables));
    }

    public static ClassName classNameFromFqcn(String fqcn) {
        String basePath = fqcn.substring(0, fqcn.lastIndexOf("."));
        String className = fqcn.substring(fqcn.lastIndexOf(".") + 1);
        return ClassName.get(basePath, className);
    }

    public static JavaFile buildJavaFile(ClassSpec spec) {
        JavaFile.Builder builder = JavaFile.builder(spec.className().packageName(), spec.poetSpec()).skipJavaLangImports(true);
        spec.staticImports().forEach(i -> i.memberNames().forEach(m -> builder.addStaticImport(i.className(), m)));
        return builder.build();
    }

}
