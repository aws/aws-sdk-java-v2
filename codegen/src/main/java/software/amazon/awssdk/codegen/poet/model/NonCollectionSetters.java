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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.config.customization.ConvenienceTypeOverload;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetUtils;

class NonCollectionSetters extends AbstractMemberSetters {
    NonCollectionSetters(IntermediateModel intermediateModel,
                         ShapeModel shapeModel,
                         MemberModel memberModel,
                         TypeProvider typeProvider) {
        super(intermediateModel, shapeModel, memberModel, typeProvider);
    }

    public List<MethodSpec> fluentDeclarations(TypeName returnType) {
        List<MethodSpec> fluentDeclarations = new ArrayList<>();
        fluentDeclarations.add(fluentAbstractSetterDeclaration(memberAsParameter(), returnType)
                                   .addJavadoc("$L", memberModel().getFluentSetterDocumentation())
                                   .build());

        if (memberModel().getEnumType() != null) {
            fluentDeclarations.add(fluentAbstractSetterDeclaration(modeledParam(), returnType)
                                       .addJavadoc("$L", memberModel().getFluentSetterDocumentation())
                                       .build());
        }

        if (memberModel().hasBuilder()) {
            fluentDeclarations.add(fluentConsumerFluentSetter(returnType));
        }

        return fluentDeclarations;
    }

    @Override
    public List<MethodSpec> fluent(TypeName returnType) {
        List<MethodSpec> fluentSetters = new ArrayList<>();

        fluentSetters.add(fluentAssignmentSetter(returnType));

        if (memberModel().getEnumType() != null) {
            fluentSetters.add(fluentEnumToStringSetter(returnType));
        }

        return fluentSetters;
    }

    public MethodSpec convenienceDeclaration(TypeName returnType, ConvenienceTypeOverload overload) {
        return MethodSpec.methodBuilder(memberModel().getFluentSetterMethodName())
                         .addParameter(PoetUtils.classNameFromFqcn(overload.getConvenienceType()), memberAsParameter().name)
                         .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                         .returns(returnType)
                         .build();
    }

    public MethodSpec fluentConvenience(TypeName returnType, ConvenienceTypeOverload overload) {
        return MethodSpec.methodBuilder(memberModel().getFluentSetterMethodName())
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(PoetUtils.classNameFromFqcn(overload.getConvenienceType()), memberAsParameter().name)
                         .addStatement("$L(new $T().adapt($L))",
                                       memberModel().getFluentSetterMethodName(),
                                       PoetUtils.classNameFromFqcn(overload.getTypeAdapterFqcn()),
                                       memberAsParameter().name)
                         .addStatement("return this")
                         .returns(returnType)
                         .build();
    }

    @Override
    public MethodSpec beanStyle() {
        MethodSpec.Builder builder = beanStyleSetterBuilder()
            .addCode(copySetterBuilderBody());

        if (annotateJsonProperty()) {
            builder.addAnnotation(
                AnnotationSpec.builder(JsonProperty.class)
                              .addMember("value", "$S", memberModel().getHttp().getMarshallLocationName()).build());
        }

        return builder.build();
    }

    public MethodSpec beanStyleConvenience(ConvenienceTypeOverload overload) {
        return MethodSpec.methodBuilder(memberModel().getBeanStyleSetterMethodName())
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(PoetUtils.classNameFromFqcn(overload.getConvenienceType()), memberAsParameter().name)
                         .addStatement("this.$L = new $T().adapt($L)",
                                       memberAsParameter().name,
                                       PoetUtils.classNameFromFqcn(overload.getTypeAdapterFqcn()),
                                       memberAsParameter().name)
                         .build();
    }

    private MethodSpec fluentAssignmentSetter(TypeName returnType) {
        return fluentSetterBuilder(returnType)
            .addCode(copySetterBody().toBuilder().addStatement("return this").build())
            .build();
    }

    private MethodSpec fluentEnumToStringSetter(TypeName returnType) {
        return fluentSetterBuilder(modeledParam(), returnType)
            .addCode(enumToStringAssignmentBody().toBuilder().addStatement("return this").build())
            .build();
    }

    private MethodSpec fluentConsumerFluentSetter(TypeName returnType) {
        ClassName memberClass = poetExtensions.getModelClass(memberModel().getShape().getC2jName());
        ClassName builderClass = memberClass.nestedClass("Builder");
        return fluentDefaultSetterDeclaration(builderConsumerParam(builderClass), returnType)
            .addModifiers(Modifier.DEFAULT)
            .addStatement("return $N($T.builder().apply($N).build())",
                          memberModel().getFluentSetterMethodName(),
                          memberClass,
                          fieldName())
            .addJavadoc("$L", memberModel().getDefaultConsumerFluentSetterDocumentation())
            .build();
    }

    private CodeBlock enumToStringAssignmentBody() {
        return CodeBlock.builder()
                        .addStatement("this.$N($N.toString())", fieldName(), fieldName())
                        .build();
    }

    private ParameterSpec modeledParam() {
        return ParameterSpec.builder(poetExtensions.getModelClass(memberModel().getShape().getShapeName()), fieldName()).build();
    }

    private ParameterSpec builderConsumerParam(ClassName builderClass) {
        return ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Consumer.class), builderClass), fieldName()).build();
    }
}
