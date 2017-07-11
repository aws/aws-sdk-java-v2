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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;

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

        fluentDeclarations.add(fluentSetterDeclaration(memberAsParameter(), returnType)
                .addJavadoc("$L", setterDocumentation)
                .build());

        fluentDeclarations.add(fluentSetterDeclaration(ParameterSpec.builder(asArray(), fieldName()).build(), returnType)
                .addJavadoc("$L", setterDocumentation)
                .varargs(true)
                .build());

        if (memberModel().getEnumType() != null) {
            fluentDeclarations.add(fluentSetterDeclaration(ParameterSpec.builder(
                    asArrayOfModeledEnum(), fieldName()).build(), returnType)
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

        if (memberModel().getEnumType() != null) {
            fluent.add(fluentEnumVarargToListSetter(returnType));
        }

        return fluent;
    }

    @Override
    public List<MethodSpec> beanStyle() {
        List<MethodSpec> beanStyle = new ArrayList<>();

        beanStyle.add(beanStyleCopySetter());
        beanStyle.add(beanStyleVarargToListSetter());

        if (memberModel().getEnumType() != null) {
            beanStyle.add(beanStyleEnumVarargToListSetter());
        }

        return beanStyle;
    }

    private MethodSpec fluentCopySetter(TypeName returnType) {
        return fluentSetterBuilder(returnType)
                .addCode(copySetterBody()
                        .toBuilder()
                        .addStatement("return this").build())
                .build();
    }

    private MethodSpec beanStyleCopySetter() {
        MethodSpec.Builder builder = beanStyleSetterBuilder()
                .addCode(copySetterBody());

        if (annotateJsonProperty()) {
            builder.addAnnotation(
                    AnnotationSpec.builder(JsonProperty.class)
                            .addMember("value", "$S", memberModel().getHttp().getMarshallLocationName()).build());
        }

        return builder.build();

    }

    private MethodSpec fluentVarargToListSetter(TypeName returnType) {
        return fluentSetterBuilder(ParameterSpec.builder(asArray(), fieldName()).build(), returnType)
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(varargToListSetterBody())
                .addStatement("return this")
                .build();
    }

    private MethodSpec beanStyleVarargToListSetter() {
        return beanStyleSetterBuilder(ParameterSpec.builder(asArray(), fieldName()).build())
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(varargToListSetterBody())
                .build();
    }

    private MethodSpec fluentEnumVarargToListSetter(TypeName returnType) {
        return fluentSetterBuilder(ParameterSpec.builder(asArrayOfModeledEnum(), fieldName()).build(), returnType)
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(enumVarargToListSetterBody())
                .addStatement("return this")
                .build();
    }

    private MethodSpec beanStyleEnumVarargToListSetter() {
        return beanStyleSetterBuilder(ParameterSpec.builder(asArrayOfModeledEnum(), fieldName()).build())
                .varargs(true)
                .addAnnotation(SafeVarargs.class)
                .addCode(enumVarargToListSetterBody())
                .build();
    }

    private CodeBlock varargToListSetterBody() {
        return CodeBlock.of("$1L($2T.asList($1L));", fieldName(), Arrays.class);
    }

    private CodeBlock enumVarargToListSetterBody() {
        return CodeBlock.of("$1L($2T.asList($1L).stream().map($3T::toString).collect($4T.toList()));",
                            fieldName(), Arrays.class, Object.class, Collectors.class);
    }

    private MemberModel elementModel() {
        return memberModel().getListModel().getListMemberModel();
    }

    private TypeName modeledEnumElement() {
        return poetExtensions.getModelClass(memberModel().getEnumType());
    }

    private TypeName listElementType() {
        return typeProvider.parameterType(elementModel());
    }

    private ArrayTypeName asArray() {
        return ArrayTypeName.of(listElementType());
    }

    private ArrayTypeName asArrayOfModeledEnum() {
        return ArrayTypeName.of(modeledEnumElement());
    }
}
