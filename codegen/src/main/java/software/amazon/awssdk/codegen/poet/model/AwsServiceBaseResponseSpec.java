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
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class AwsServiceBaseResponseSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final PoetExtensions poetExtensions;
    private final ClassName responseMetadata;

    public AwsServiceBaseResponseSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        this.poetExtensions = new PoetExtensions(this.intermediateModel);
        this.responseMetadata = poetExtensions.getResponseMetadataClass();
    }

    @Override
    public TypeSpec poetSpec() {
        MethodSpec.Builder constructorBuilder =
            MethodSpec.constructorBuilder()
                      .addModifiers(Modifier.PROTECTED)
                      .addParameter(className().nestedClass("Builder"), "builder")
                      .addStatement("super(builder)");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className())
                                           .addAnnotation(PoetUtils.generatedAnnotation())
                                           .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                           .superclass(ClassName.get(AwsResponse.class))
                                           .addType(builderInterfaceSpec())
                                           .addType(builderImplSpec());

        addResponseMetadata(classBuilder, constructorBuilder);

        classBuilder.addMethod(constructorBuilder.build());
        return classBuilder.build();
    }

    @Override
    public ClassName className() {
        return poetExtensions.getModelClass(intermediateModel.getSdkResponseBaseClassName());
    }

    private TypeSpec builderInterfaceSpec() {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder("Builder")
                                           .addModifiers(Modifier.PUBLIC)
                                           .addSuperinterface(ClassName.get(AwsResponse.class).nestedClass("Builder"))
                                           .addMethod(MethodSpec.methodBuilder("build")
                                                                .addAnnotation(Override.class)
                                                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                                                .returns(className())
                                                                .build());

        addResponseMetadataToInterface(builder);
        return builder.build();
    }

    private ClassName builderInterfaceName() {
        return className().nestedClass("Builder");
    }

    private TypeSpec builderImplSpec() {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                                                          .addModifiers(Modifier.PROTECTED)
                                                          .addParameter(className(), "response")
                                                          .addStatement("super(response)")
                                                          .addStatement("this.responseMetadata = response.responseMetadata()");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("BuilderImpl")
                                           .addModifiers(Modifier.PROTECTED, Modifier.STATIC, Modifier.ABSTRACT)
                                           .addSuperinterface(className().nestedClass("Builder"))
                                           .superclass(ClassName.get(AwsResponse.class).nestedClass("BuilderImpl"))
                                           .addMethod(MethodSpec.constructorBuilder()
                                                                .addModifiers(Modifier.PROTECTED)
                                                                .build())
                                           .addMethod(constructorBuilder.build());

        addResponseMetadataToImpl(classBuilder);

        return classBuilder.build();
    }

    private void addResponseMetadata(TypeSpec.Builder classBuilder, MethodSpec.Builder constructorBuilder) {
        constructorBuilder.addStatement("this.responseMetadata = builder.responseMetadata()");

        classBuilder.addField(FieldSpec.builder(responseMetadata, "responseMetadata",
                                                Modifier.PRIVATE, Modifier.FINAL)
                                       .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("responseMetadata")
                                         .returns(responseMetadata)
                                         .addModifiers(Modifier.PUBLIC)
                                         .addAnnotation(Override.class)
                                         .addCode("return responseMetadata;")
                                         .build());

    }

    private void addResponseMetadataToInterface(TypeSpec.Builder builder) {
        builder.addMethod(MethodSpec.methodBuilder("responseMetadata")
                                    .returns(responseMetadata)
                                    .addAnnotation(Override.class)
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .build());

        builder.addMethod(MethodSpec.methodBuilder("responseMetadata")
                                    .addParameter(AwsResponseMetadata.class, "metadata")
                                    .returns(builderInterfaceName())
                                    .addAnnotation(Override.class)
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .build());
    }

    private void addResponseMetadataToImpl(TypeSpec.Builder classBuilder) {
        classBuilder.addField(FieldSpec.builder(responseMetadata, "responseMetadata", Modifier.PRIVATE).build());

        classBuilder.addMethod(MethodSpec.methodBuilder("responseMetadata")
                                         .returns(responseMetadata)
                                         .addAnnotation(Override.class)
                                         .addModifiers(Modifier.PUBLIC)
                                         .addStatement("return responseMetadata")
                                         .build());

        classBuilder.addMethod(MethodSpec.methodBuilder("responseMetadata")
                                         .addParameter(AwsResponseMetadata.class, "responseMetadata")
                                         .returns(builderInterfaceName())
                                         .addAnnotation(Override.class)
                                         .addModifiers(Modifier.PUBLIC)
                                         .addCode(CodeBlock.builder()
                                                           .add("this.responseMetadata = $T.create(responseMetadata);\n",
                                                                responseMetadata)
                                                           .add("return this;")
                                                           .build())
                                         .build());
    }
}
