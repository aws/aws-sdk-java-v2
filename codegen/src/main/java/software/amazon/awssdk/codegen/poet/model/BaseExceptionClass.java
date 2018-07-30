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

package software.amazon.awssdk.codegen.poet.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import javax.lang.model.element.Modifier;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class BaseExceptionClass implements ClassSpec {

    private final ClassName baseExceptionClassName;

    public BaseExceptionClass(IntermediateModel model) {
        final String basePackage = model.getMetadata().getFullModelPackageName();
        this.baseExceptionClassName = ClassName.get(basePackage, model.getSdkModeledExceptionBaseClassName());
    }


    @Override
    public TypeSpec poetSpec() {
        return PoetUtils.createClassBuilder(baseExceptionClassName)
                .superclass(AwsServiceException.class)
                .addMethod(constructor())
                .addMethod(builderMethod())
                .addMethod(toBuilderMethod())
                .addMethod(serializableBuilderClass())
                .addModifiers(Modifier.PUBLIC)
                .addType(builderInterface())
                .addType(builderImplClass())
                .build();
    }

    public MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(baseExceptionClassName.nestedClass("Builder"), "builder")
                .addStatement("super(builder)")
                .build();
    }

    public TypeSpec builderInterface() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(baseExceptionClassName.nestedClass("Builder"))
                .addSuperinterface(ClassName.get(AwsServiceException.class).nestedClass("Builder"))
                .addModifiers(Modifier.PUBLIC)
                .addMethods(ExceptionProperties.builderInterfaceMethods(className().nestedClass("Builder")));

        return builder.build();
    }

    public TypeSpec builderImplClass() {
        return TypeSpec.classBuilder(baseExceptionClassName.nestedClass("BuilderImpl"))
                .addSuperinterface(className().nestedClass("Builder"))
                .superclass(ClassName.get(AwsServiceException.class).nestedClass("BuilderImpl"))
                .addModifiers(Modifier.STATIC, Modifier.PROTECTED)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PROTECTED).build())
                .addMethod(copyModelConstructor())
                .addMethods(ExceptionProperties.builderImplMethods(className().nestedClass("BuilderImpl")))
                .addMethod(MethodSpec.methodBuilder("build")
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return new $T(this)", className())
                        .returns(className())
                        .build())
                .build();
    }

    private MethodSpec copyModelConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PROTECTED)
                .addParameter(className(), "ex")
                .addStatement("super(ex)")
                .build();
    }

    private MethodSpec builderMethod() {
        return MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(className().nestedClass("Builder"))
                .addStatement("return new $T()", className().nestedClass("BuilderImpl"))
                .build();
    }

    private MethodSpec toBuilderMethod() {
        return MethodSpec.methodBuilder("toBuilder")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(className().nestedClass("Builder"))
                .addStatement("return new $T(this)", className().nestedClass("BuilderImpl"))
                .build();
    }

    private MethodSpec serializableBuilderClass() {
        return MethodSpec.methodBuilder("serializableBuilderClass")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(className().nestedClass("Builder"))))
                .addStatement("return $T.class", className().nestedClass("BuilderImpl"))
                .build();
    }

    @Override
    public ClassName className() {
        return baseExceptionClassName;
    }
}
