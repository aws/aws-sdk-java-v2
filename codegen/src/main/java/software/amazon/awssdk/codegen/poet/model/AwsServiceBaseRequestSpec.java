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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.AwsRequest;
import software.amazon.awssdk.AwsRequestOverrideConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class AwsServiceBaseRequestSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final PoetExtensions poetExtensions;

    public AwsServiceBaseRequestSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtensions(intermediateModel);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className())
                .addJavadoc("Base request class for all requests to " + intermediateModel.getMetadata().getServiceFullName())
                .addAnnotation(PoetUtils.GENERATED)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(PoetUtils.createBoundedTypeVariableName("B", className().nestedClass("Builder"), "B, R"))
                .addTypeVariable(PoetUtils.createBoundedTypeVariableName("R", className(), "B", "R"))
                .superclass(ParameterizedTypeName.get(ClassName.get(AwsRequest.class), TypeVariableName.get("B"),
                        TypeVariableName.get("R"), ClassName.get(AwsRequestOverrideConfig.class)))
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(TypeVariableName.get("B"), "builder")
                        .addStatement("super(builder)")
                        .build())
                .addType(builderInterfaceSpec())
                .addType(builderImplSpec());
        return builder.build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getModelClass(intermediateModel.getMetadata().getServiceName().replace(" ", "") + "Request");
    }

    private TypeSpec builderInterfaceSpec() {
        return TypeSpec.interfaceBuilder("Builder")
                .addTypeVariable(PoetUtils.createBoundedTypeVariableName("B", className().nestedClass("Builder"), "B, R"))
                .addTypeVariable(PoetUtils.createBoundedTypeVariableName("R", className(), "B", "R"))
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(AwsRequest.Builder.class), TypeVariableName.get("B"),
                        TypeVariableName.get("R"), ClassName.get(AwsRequestOverrideConfig.class)))
                .build();
    }

    private TypeSpec builderImplSpec() {
        return TypeSpec.classBuilder("BuilderImpl")
                .addModifiers(Modifier.PROTECTED, Modifier.STATIC, Modifier.ABSTRACT)
                .addTypeVariable(PoetUtils.createBoundedTypeVariableName("B", className().nestedClass("Builder"), "B, R"))
                .addTypeVariable(PoetUtils.createBoundedTypeVariableName("R", className(), "B", "R"))
                .addSuperinterface(ParameterizedTypeName.get(className().nestedClass("Builder"), TypeVariableName.get("B"),
                        TypeVariableName.get("R")))
                .superclass(ParameterizedTypeName.get(ClassName.get(AwsRequest.class).nestedClass("BuilderImpl"),
                        TypeVariableName.get("B"), TypeVariableName.get("R"), ClassName.get(AwsRequestOverrideConfig.class)))
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("B")),
                                "concrete")
                        .addStatement("super(concrete)")
                        .build())
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PROTECTED)
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("B")),
                                "concrete")
                        .addParameter(TypeVariableName.get("R"), "request")
                        .addStatement("super(concrete, request)")
                        .build())
                .build();
    }
}
