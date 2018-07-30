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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.StaticImport;
import software.amazon.awssdk.core.adapter.StandardMemberCopier;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

class MemberCopierSpec implements ClassSpec {
    private final MemberModel memberModel;
    private final ServiceModelCopiers serviceModelCopiers;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;

    MemberCopierSpec(MemberModel memberModel,
                     ServiceModelCopiers serviceModelCopiers,
                     TypeProvider typeProvider,
                     PoetExtensions poetExtensions) {
        this.memberModel = memberModel;
        this.serviceModelCopiers = serviceModelCopiers;
        this.typeProvider = typeProvider;
        this.poetExtensions = poetExtensions;
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(className())
                .addModifiers(Modifier.FINAL)
                .addAnnotation(PoetUtils.generatedAnnotation())
                .addMethod(copyMethod());

        if (serviceModelCopiers.requiresBuilderCopier(memberModel)) {
            builder.addMethod(builderCopyMethod());
        }

        if (Utils.isListWithEnumShape(memberModel) || Utils.isMapWithEnumShape(memberModel)) {
            builder.addMethod(enumToStringCopyMethod());
        }

        return builder.build();
    }

    @Override
    public ClassName className() {
        return serviceModelCopiers.copierClassFor(memberModel).get();
    }

    @Override
    public Iterable<StaticImport> staticImports() {
        if (memberModel.isList()) {
            return Collections.singletonList(StaticImport.staticMethodImport(Collectors.class, "toList"));
        }

        if (memberModel.isMap()) {
            return Collections.singletonList(StaticImport.staticMethodImport(Collectors.class, "toMap"));
        }

        return Collections.emptyList();
    }

    private MethodSpec copyMethod() {
        return copyMethodProto().addCode(copyMethodBody()).build();
    }

    private MethodSpec.Builder copyMethodProto() {
        return copyMethodProto(typeProvider.parameterType(memberModel), serviceModelCopiers.copyMethodName());
    }

    private MethodSpec.Builder copyMethodProto(TypeName parameterType, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                         .addModifiers(Modifier.STATIC)
                         .addParameter(parameterType, memberParamName())
                         .returns(typeProvider.fieldType(memberModel));
    }

    private MethodSpec enumToStringCopyMethod() {
        return copyMethodProto(paramaterTypeForEnumToStringCopyMethod(),
                               serviceModelCopiers.enumToStringCopyMethodName())
            .addCode(enumToStringCopyMethodBody()).build();
    }

    private TypeName paramaterTypeForEnumToStringCopyMethod() {
        TypeName typeName = null;

        if (memberModel.isList()) {
            typeName = typeProvider.listWithEnumParameterType(memberModel);
        } else if (memberModel.isMap()) {
            typeName = typeProvider.mapWithEnumParameterType(memberModel.getMapModel());
        }

        return typeName;
    }

    private CodeBlock enumToStringCopyMethodBody() {
        if (memberModel.isList()) {
            return listCopyBody(true);
        } else if (memberModel.isMap()) {
            return mapCopyBody(true);
        }

        return null;
    }

    private MethodSpec builderCopyMethod() {
        if (memberModel.isList()) {
            return builderCopyMethodForList();
        }
        if (memberModel.isMap()) {
            return builderCopyMethodForMap();
        }
        throw new UnsupportedOperationException();
    }

    private MethodSpec builderCopyMethodForMap() {
        TypeName keyType = typeProvider.getTypeNameForSimpleType(memberModel.getMapModel().getKeyModel()
                                                                            .getVariable().getVariableType());
        ClassName valueParameter = poetExtensions.getModelClass(memberModel.getMapModel().getValueModel().getC2jShape());
        ClassName builderForParameter = valueParameter.nestedClass("Builder");
        TypeName parameterType =
            ParameterizedTypeName.get(ClassName.get(Map.class), keyType, WildcardTypeName.subtypeOf(builderForParameter));

        CodeBlock code =
            CodeBlock.builder()
                     .beginControlFlow("if ($N == null)", memberParamName())
                     .addStatement("return null")
                     .endControlFlow()
                     .addStatement("return $N($N.entrySet().stream().collect(toMap($T::getKey, e -> e.getValue().build())))",
                                   serviceModelCopiers.copyMethodName(),
                                   memberParamName(),
                                   Map.Entry.class)
                     .build();

        return MethodSpec.methodBuilder(serviceModelCopiers.builderCopyMethodName())
                         .addModifiers(Modifier.STATIC)
                         .addParameter(parameterType, memberParamName())
                         .returns(typeProvider.fieldType(memberModel))
                         .addCode(code)
                         .build();
    }

    private MethodSpec builderCopyMethodForList() {
        ClassName listParameter = poetExtensions.getModelClass(memberModel.getListModel().getListMemberModel().getC2jShape());
        ClassName builderForParameter = listParameter.nestedClass("Builder");

        TypeName parameterType =
            ParameterizedTypeName.get(ClassName.get(Collection.class), WildcardTypeName.subtypeOf(builderForParameter));

        CodeBlock code = CodeBlock.builder()
                                  .beginControlFlow("if ($N == null)", memberParamName())
                                  .addStatement("return null")
                                  .endControlFlow()
                                  .addStatement("return $N($N.stream().map($T::$N).collect(toList()))",
                                                serviceModelCopiers.copyMethodName(),
                                                memberParamName(),
                                                builderForParameter,
                                                "build")
                                  .build();

        return MethodSpec.methodBuilder(serviceModelCopiers.builderCopyMethodName())
                         .addModifiers(Modifier.STATIC)
                         .addParameter(parameterType, memberParamName())
                         .returns(typeProvider.fieldType(memberModel))
                         .addCode(code)
                         .build();
    }

    private CodeBlock copyMethodBody() {
        if (memberModel.isMap()) {
            return mapCopyBody(false);
        }

        if (memberModel.isList()) {
            return listCopyBody(false);
        }

        return modelCopyBody();
    }

    private CodeBlock listCopyBody(boolean checkForModeledEnum) {
        String paramName = memberParamName();
        MemberModel listMember = memberModel.getListModel().getListMemberModel();
        String copyName = paramName + "Copy";

        CodeBlock.Builder builder = CodeBlock.builder();

        if (typeProvider.useAutoConstructLists()) {
            builder.beginControlFlow("if ($1N == null || $1N instanceof $2T)", memberParamName(), SdkAutoConstructList.class)
                   .addStatement("return $T.getInstance()", DefaultSdkAutoConstructList.class)
                   .endControlFlow();

        } else {
            builder.beginControlFlow("if ($N == null)", memberParamName())
                   .addStatement("return null")
                   .endControlFlow();
        }

        Optional<ClassName> elementCopier = serviceModelCopiers.copierClassFor(listMember);

        // If list member is enum type, generate the body to convert collection of enums into collection of strings
        // checkForModeledEnum is set to true for generating copyEnumToString method
        if (checkForModeledEnum && listMember.getEnumType() != null) {
            builder.addStatement("$T $N = $N.stream().map(Object::toString).collect(toList())",
                                 typeProvider.fieldType(memberModel),
                                 copyName,
                                 paramName);

        } else if (!elementCopier.isPresent()) { // Just use constructor copy if there's no copier for the element
            builder.addStatement("$T $N = new $T<>($N)",
                                 typeProvider.fieldType(memberModel),
                                 copyName,
                                 typeProvider.listImplClassName(),
                                 paramName);
        } else {
            ClassName copier = elementCopier.get();
            builder.addStatement("$T $N = $N.stream().map($T::$N).collect(toList())",
                                 typeProvider.fieldType(memberModel),
                                 copyName,
                                 paramName,
                                 copier,
                                 serviceModelCopiers.copyMethodName());
        }

        builder.addStatement("return $T.unmodifiableList($N)", Collections.class, copyName);

        return builder.build();
    }

    private CodeBlock mapCopyBody(boolean checkForModeledEnum) {
        MapModel mapModel = memberModel.getMapModel();
        String copyMethod = serviceModelCopiers.copyMethodName();
        String paramName = memberParamName();
        String copyName = paramName + "Copy";

        CodeBlock keyCopyExpr =
            Optional.ofNullable(mapModel.getKeyModel())
                    .map(model -> serviceModelCopiers.copierClassFor(model)
                                                     .map(copier -> CodeBlock.of("e -> $T.$N(e.getKey())",
                                                                                 copier,
                                                                                 copyMethod))
                                                     .orElseGet(() -> checkForModeledEnum && model.getEnumType() != null
                                                                      ? CodeBlock.of("e -> e.getKey().toString()")
                                                                      : CodeBlock.of("$T::getKey", Map.Entry.class)))
                    .orElseGet(() -> CodeBlock.of("e -> $T.$N(e.getKey())",
                                                  StandardMemberCopier.class,
                                                  copyMethod));

        CodeBlock valueCopyExpr =
            Optional.ofNullable(mapModel.getValueModel())
                    .map(model -> serviceModelCopiers.copierClassFor(model)
                                                     .map(copier -> CodeBlock.of("e -> $T.$N(e.getValue())",
                                                                                 copier,
                                                                                 copyMethod))
                                                     .orElseGet(() -> checkForModeledEnum && model.getEnumType() != null
                                                                      ? CodeBlock.of("e -> e.getValue().toString()")
                                                                      : CodeBlock.of("$T::getValue", Map.Entry.class)))
                    .orElseGet(() -> CodeBlock.of("e -> $T.$N(e.getValue())",
                                                  StandardMemberCopier.class,
                                                  copyMethod));

        CodeBlock.Builder builder = CodeBlock.builder();
        if (typeProvider.useAutoConstructMaps()) {
            builder.beginControlFlow("if ($1N == null || $1N instanceof $2T)", memberParamName(), SdkAutoConstructMap.class)
                    .addStatement("return $T.getInstance()", DefaultSdkAutoConstructMap.class)
                    .endControlFlow();
        } else {
            builder.beginControlFlow("if ($1N == null)", memberParamName())
                    .addStatement("return null")
                    .endControlFlow();
        }

        builder.addStatement("$T $N = $N.entrySet().stream().collect(toMap($L, $L))", typeProvider.fieldType(memberModel),
                        copyName, memberParamName(), keyCopyExpr, valueCopyExpr);

        return builder.addStatement("return $T.unmodifiableMap($N)", Collections.class, copyName).build();
    }

    private CodeBlock modelCopyBody() {
        // These are immutable so just return the instance
        return CodeBlock.builder()
                .addStatement("return $N", memberParamName())
                .build();
    }

    private String memberParamName() {
        if (memberModel.isSimple()) {
            return Utils.unCapitalize(memberModel.getVariable().getSimpleType()) + "Param";
        }
        return Utils.unCapitalize(memberModel.getC2jShape()) + "Param";
    }
}
