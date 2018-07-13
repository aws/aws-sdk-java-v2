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
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.utils.Validate;

class ListSetters extends AbstractMemberSetters {
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;

    ListSetters(IntermediateModel intermediateModel,
                       ShapeModel shapeModel,
                       MemberModel memberModel,
                       TypeProvider typeProvider) {
        super(intermediateModel, shapeModel, memberModel, typeProvider);
        this.typeProvider = typeProvider;
        this.poetExtensions = new PoetExtensions(intermediateModel);
    }

    public List<MethodSpec> fluentDeclarations(TypeName returnType) {
        List<MethodSpec> fluentDeclarations = new ArrayList<>();

        String setterDocumentation = memberModel().getFluentSetterDocumentation();

        fluentDeclarations.add(fluentAbstractSetterDeclaration(memberAsParameter(), returnType)
                .addJavadoc("$L", setterDocumentation)
                .build());

        fluentDeclarations.add(fluentAbstractSetterDeclaration(ParameterSpec.builder(asArray(), fieldName()).build(), returnType)
                .addJavadoc("$L", setterDocumentation)
                .varargs(true)
                .build());

        if (elementModel().hasBuilder()) {
            fluentDeclarations.add(fluentAbstractSetterDeclaration(ParameterSpec.builder(asConsumerBuilderArray(),
                                                                                         fieldName())
                                                                                .build(), returnType)
                                           .varargs(true)
                                           .addJavadoc("$L", memberModel().getDefaultConsumerFluentSetterDocumentation())
                                           .build());
        }

        if (enumTypeInListMemberModel() != null) {
            // Generate setter with modeled Collections parameter
            fluentDeclarations.add(fluentAbstractSetterDeclaration(memberModel().getFluentEnumSetterMethodName(),
                                                                   collectionOfModeledEnumAsParameter(),
                                                                   returnType)
                                       .addJavadoc("$L", setterDocumentation)
                                       .build());

            // Generate setter with modeled var args parameter
            fluentDeclarations.add(fluentAbstractSetterDeclaration(memberModel().getFluentEnumSetterMethodName(),
                                                                   arrayOfModeledEnumAsParameter(),
                                                                   returnType)
                                       .varargs(true)
                                       .addJavadoc("$L", setterDocumentation)
                                       .build());
        }

        return fluentDeclarations;
    }

    @Override
    public List<MethodSpec> fluent(TypeName returnType) {
        List<MethodSpec> fluent = new ArrayList<>();

        fluent.add(fluentCopySetter(returnType));
        fluent.add(fluentVarargToListSetter(returnType));

        if (elementModel().hasBuilder()) {
            fluent.add(fluentVarargConsumerBuilderSetter(returnType));
        }

        if (enumTypeInListMemberModel() != null) {
            // Generate setter with modeled Collections parameter in BuilderImpl
            fluent.add(fluentEnumCollectionsSetter(memberModel().getFluentEnumSetterMethodName(),
                                                   collectionOfModeledEnumAsParameter(),
                                                   returnType));

            // Generate setter with modeled var args parameter in BuilderImpl
            fluent.add(fluentEnumVarargToListSetter(memberModel().getFluentEnumSetterMethodName(),
                                                    arrayOfModeledEnumAsParameter(),
                                                    returnType));
        }

        return fluent;
    }

    @Override
    public MethodSpec beanStyle() {
        MethodSpec.Builder builder = beanStyleSetterBuilder()
            .addCode(memberModel().isCollectionWithBuilderMember() ? copySetterBuilderBody() : beanCopySetterBody());

        if (annotateJsonProperty()) {
            builder.addAnnotation(
                AnnotationSpec.builder(JsonProperty.class)
                              .addMember("value", "$S", memberModel().getHttp().getMarshallLocationName()).build());
        }

        return builder.build();

    }

    private MethodSpec fluentCopySetter(TypeName returnType) {
        return fluentSetterBuilder(returnType)
                .addCode(copySetterBody()
                        .toBuilder()
                        .addStatement("return this").build())
                .build();
    }

    private MethodSpec fluentVarargToListSetter(TypeName returnType) {
        return fluentSetterBuilder(ParameterSpec.builder(asArray(), fieldName()).build(), returnType)
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(varargToListSetterBody())
                .addStatement("return this")
                .build();
    }

    private MethodSpec fluentVarargConsumerBuilderSetter(TypeName returnType) {
        return fluentSetterBuilder(ParameterSpec.builder(asConsumerBuilderArray(), fieldName()).build(), returnType)
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(consumerBuilderVarargSetterBody())
                .addStatement("return this")
                .build();
    }

    /**
     * {@link MethodSpec} to generate fluent setter method implementation that takes a collection of modeled enums as parameter
     */
    private MethodSpec fluentEnumCollectionsSetter(String methodName,
                                                   ParameterSpec parameter,
                                                   TypeName returnType) {
        return fluentSetterBuilder(methodName, parameter, returnType)
            .addCode(fluentSetterWithEnumCollectionsParameterMethodBody())
            .addStatement("return this")
            .build();
    }

    /**
     * {@link MethodSpec} to generate fluent setter method implementation that takes var args of modeled enums as parameter
     */
    private MethodSpec fluentEnumVarargToListSetter(String methodName,
                                                    ParameterSpec parameter,
                                                    TypeName returnType) {
        return fluentSetterBuilder(methodName, parameter, returnType)
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(enumVarargToListSetterBody())
                .addStatement("return this")
                .build();
    }


    private CodeBlock varargToListSetterBody() {
        return CodeBlock.of("$1L($2T.asList($3L));", memberModel().getFluentSetterMethodName(), Arrays.class, fieldName());
    }

    private CodeBlock consumerBuilderVarargSetterBody() {
        return CodeBlock.of("$1L($2T.of($1L).map(c -> $3T.builder().applyMutation(c).build()).collect($4T.toList()));",
                            fieldName(), Stream.class, listElementType(), Collectors.class);
    }

    private CodeBlock enumVarargToListSetterBody() {
        return CodeBlock.of("$1L($2T.asList($1L));", fieldName(), Arrays.class);
    }

    private MemberModel elementModel() {
        return memberModel().getListModel().getListMemberModel();
    }

    private TypeName modeledEnumElement() {
        return poetExtensions.getModelClass(enumTypeInListMemberModel());
    }

    private String enumTypeInListMemberModel() {
        return memberModel().getListModel().getListMemberModel().getEnumType();
    }

    private TypeName listElementType() {
        return typeProvider.parameterType(elementModel());
    }

    private TypeName listElementConsumerBuilderType() {
        TypeName listElementType = listElementType();
        ClassName classType = Validate.isInstanceOf(ClassName.class, listElementType,
                                                    "List element type must be of type class, but was %s", listElementType);
        return classType.nestedClass("Builder");
    }

    private TypeName asConsumerBuilderArray() {
        ParameterizedTypeName consumerBuilder = ParameterizedTypeName.get(ClassName.get(Consumer.class),
                                                                          listElementConsumerBuilderType());
        return ArrayTypeName.of(consumerBuilder);
    }

    private ArrayTypeName asArray() {
        return ArrayTypeName.of(listElementType());
    }

    private ArrayTypeName asArrayOfModeledEnum() {
        return ArrayTypeName.of(modeledEnumElement());
    }

    private ParameterSpec arrayOfModeledEnumAsParameter() {
        return ParameterSpec.builder(asArrayOfModeledEnum(), fieldName()).build();
    }

    private ParameterSpec collectionOfModeledEnumAsParameter() {
        return ParameterSpec.builder(typeProvider.listWithEnumParameterType(memberModel()), fieldName())
                            .build();
    }
}
