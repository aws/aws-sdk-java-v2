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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

class MemberCopierSpec implements ClassSpec {
    private final MemberModel memberModel;
    private final ServiceModelCopiers serviceModelCopiers;
    private final TypeProvider typeProvider;
    private final PoetExtensions poetExtensions;

    private enum EnumTransform {
        /** Copy enums as strings */
        STRING_TO_ENUM,
        /** Copy strings as enums */
        ENUM_TO_STRING,
        /** Copy without a transformation */
        NONE
    }

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

        // If this is a collection, and it contains enums, or recursively
        // contains enums, add extra methods for copying the elements from an
        // enum to string and vice versa
        if (isEnumCopyAvailable(memberModel)) {
            builder.addMethod(enumToStringCopyMethod());
            builder.addMethod(stringToEnumCopyMethod());
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

    public static boolean isEnumCopyAvailable(MemberModel memberModel) {
        if (!(memberModel.isMap() || memberModel.isList())) {
            return false;
        }

        if (memberModel.isMap()) {
            MapModel mapModel = memberModel.getMapModel();
            MemberModel keyModel = mapModel.getKeyModel();
            MemberModel valueModel = mapModel.getValueModel();
            if (keyModel.getEnumType() != null || valueModel.getEnumType() != null) {
                return true;
            }

            if (valueModel.isList() || valueModel.isMap()) {
                return isEnumCopyAvailable(valueModel);
            }
            // Keys are always simple, don't need to check
            return false;
        } else {
            MemberModel element = memberModel.getListModel().getListMemberModel();
            if (element.getEnumType() != null) {
                return true;
            }
            if (element.isList() || element.isMap()) {
                return isEnumCopyAvailable(element);
            }
            return false;
        }
    }

    private MethodSpec copyMethod() {
        return copyMethodProto().addCode(copyMethodBody()).build();
    }

    private MethodSpec.Builder copyMethodProto() {
        return copyMethodProto(typeProvider.parameterType(memberModel),
                               typeProvider.fieldType(memberModel),
                               serviceModelCopiers.copyMethodName());
    }

    private MethodSpec.Builder copyMethodProto(TypeName parameterType, TypeName returnType, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                         .addModifiers(Modifier.STATIC)
                         .addParameter(parameterType, memberParamName())
                         .returns(returnType);
    }

    private MethodSpec enumToStringCopyMethod() {
        return copyMethodProto(parameterTypeForEnumToStringCopyMethod(),
                               typeProvider.fieldType(memberModel),
                               serviceModelCopiers.enumToStringCopyMethodName())
            .addCode(enumToStringCopyMethodBody()).build();
    }

    private MethodSpec stringToEnumCopyMethod() {
        return copyMethodProto(typeProvider.parameterType(memberModel),
                               typeProvider.enumReturnType(memberModel),
                               serviceModelCopiers.stringToEnumCopyMethodName())
                .addCode(stringToEnumCopyMethodBody()).build();
    }

    private TypeName parameterTypeForEnumToStringCopyMethod() {
        return typeProvider.parameterType(memberModel, true);
    }

    private CodeBlock enumToStringCopyMethodBody() {
        if (memberModel.isList()) {
            return listCopyBody(EnumTransform.ENUM_TO_STRING);
        } else if (memberModel.isMap()) {
            return mapCopyBody(EnumTransform.ENUM_TO_STRING);
        }

        return null;
    }

    private CodeBlock stringToEnumCopyMethodBody() {
        if (memberModel.isList()) {
            return listCopyBody(EnumTransform.STRING_TO_ENUM);
        } else {
            return mapCopyBody(EnumTransform.STRING_TO_ENUM);
        }
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
                     .beginControlFlow("if ($1N == null || $1N instanceof $2T)",
                                       memberParamName(), DefaultSdkAutoConstructMap.class)
                     .addStatement("return $T.getInstance()", DefaultSdkAutoConstructMap.class)
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
                                  .beginControlFlow("if ($1N == null || $1N instanceof $2T)",
                                                    memberParamName(), DefaultSdkAutoConstructList.class)
                                  .addStatement("return $T.getInstance()", DefaultSdkAutoConstructList.class)
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
            return mapCopyBody(EnumTransform.NONE);
        }

        if (memberModel.isList()) {
            return listCopyBody(EnumTransform.NONE);
        }

        return modelCopyBody();
    }

    private CodeBlock listCopyBody(EnumTransform enumTransform) {
        String paramName = memberParamName();
        String copyName = paramName + "Copy";

        CodeBlock.Builder builder = CodeBlock.builder();

        builder.beginControlFlow("if ($1N == null || $1N instanceof $2T)", memberParamName(), SdkAutoConstructList.class)
               .addStatement("return $T.getInstance()", DefaultSdkAutoConstructList.class)
               .endControlFlow();

        Optional<ClassName> copierClass = serviceModelCopiers.copierClassFor(memberModel.getListModel().getListMemberModel());
        boolean hasCopier = copierClass.isPresent();

        TypeName copyType;
        if (enumTransform == EnumTransform.STRING_TO_ENUM) {
            copyType = typeProvider.enumReturnType(memberModel);
        } else {
            copyType = typeProvider.fieldType(memberModel);
        }

        MemberModel elementModel = memberModel.getListModel().getListMemberModel();

        switch (enumTransform) {
            case STRING_TO_ENUM:
                if (hasCopier) {
                    builder.addStatement("$T $N = $N.stream().map($T::$N).collect(toList())",
                                         copyType,
                                         copyName,
                                         paramName, copierClass.get(),
                                         serviceModelCopiers.stringToEnumCopyMethodName());
                } else {
                    builder.addStatement("$T $N = $N.stream().map($T::fromValue).collect(toList())",
                                         copyType,
                                         copyName,
                                         paramName,
                                         poetExtensions.getModelClass(elementModel.getEnumType()));
                }
                break;
            case ENUM_TO_STRING:
                if (hasCopier) {
                    builder.addStatement("$T $N = $N.stream().map($T::$N).collect(toList())",
                                         copyType,
                                         copyName,
                                         paramName,
                                         copierClass.get(),
                                         serviceModelCopiers.enumToStringCopyMethodName());
                } else {
                    builder.addStatement("$T $N = $N.stream().map(Object::toString).collect(toList())",
                                         copyType,
                                         copyName,
                                         paramName);
                }
                break;
            case NONE:
                if (hasCopier) {
                    builder.addStatement("$T $N = $N.stream().map($T::$N).collect(toList())",
                                         copyType,
                                         copyName,
                                         paramName,
                                         copierClass.get(),
                                         serviceModelCopiers.copyMethodName());
                } else {
                    builder.addStatement("$T $N = new $T<>($N)",
                                         copyType,
                                         copyName,
                                         typeProvider.listImplClassName(),
                                         paramName);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown enum transform: " + enumTransform);
        }

        builder.addStatement("return $T.unmodifiableList($N)", Collections.class, copyName);

        return builder.build();
    }

    private CodeBlock mapCopyBody(EnumTransform enumTransform) {
        MapModel mapModel = memberModel.getMapModel();
        String paramName = memberParamName();
        String copyName = paramName + "Copy";

        MemberModel keyModel = mapModel.getKeyModel();
        MemberModel valueModel = mapModel.getValueModel();

        // We need to know if we're applying the transform to the key, value, or both
        boolean keyHasEnum = keyModel.getEnumType() != null || isEnumCopyAvailable(keyModel);
        boolean valueHasEnum = valueModel.getEnumType() != null || isEnumCopyAvailable(valueModel);
        EnumTransform keyTransform = keyHasEnum ? enumTransform : EnumTransform.NONE;
        EnumTransform valueTransform = valueHasEnum ? enumTransform : EnumTransform.NONE;

        CodeBlock keyCopyExpr = mapKeyValCopyExpr(keyModel, "getKey", keyTransform);
        CodeBlock valueCopyExpr = mapKeyValCopyExpr(valueModel, "getValue", valueTransform);

        CodeBlock.Builder builder = CodeBlock.builder();
        builder.beginControlFlow("if ($1N == null || $1N instanceof $2T)", memberParamName(), SdkAutoConstructMap.class)
                .addStatement("return $T.getInstance()", DefaultSdkAutoConstructMap.class)
                .endControlFlow();

        TypeName copyType;
        if (enumTransform == EnumTransform.STRING_TO_ENUM) {
            copyType = typeProvider.enumReturnType(memberModel);
        } else {
            copyType = typeProvider.fieldType(memberModel);
        }

        // If we're transforming from string to enum, Don't include UNKNOWN_TO_SDK_VERSION values as keys in the map
        if (keyTransform == EnumTransform.STRING_TO_ENUM) {
            ClassName enumClassName = poetExtensions.getModelClass(keyModel.getEnumType());
            CodeBlock putLambda = CodeBlock.builder()
                    .beginControlFlow("(m, e) ->")
                    .add("$T keyAsEnum = $L;", enumClassName, keyCopyExpr)
                    .beginControlFlow("if (keyAsEnum != $T.UNKNOWN_TO_SDK_VERSION)", enumClassName)
                    .add("m.put(keyAsEnum, $L);", valueCopyExpr)
                    .endControlFlow()
                    .endControlFlow()
                    .build();
            builder.addStatement("$T $N = $N.entrySet().stream().collect($T::new, $L, $T::putAll)",
                    copyType, copyName, memberParamName(), HashMap.class, putLambda, HashMap.class);
        } else {
            builder.addStatement("$T $N = $N.entrySet().stream().collect($T::new, (m, e) -> m.put($L, $L), $T::putAll)",
                    copyType, copyName, memberParamName(), HashMap.class, keyCopyExpr, valueCopyExpr,
                    HashMap.class);
        }

        return builder.addStatement("return $T.unmodifiableMap($N)", Collections.class, copyName).build();
    }

    private CodeBlock mapKeyValCopyExpr(MemberModel keyValModel, String getterName, EnumTransform enumTransform) {
        Optional<ClassName> keyCopier = serviceModelCopiers.copierClassFor(keyValModel);
        boolean hasCopier = keyCopier.isPresent();
        switch (enumTransform) {
            case STRING_TO_ENUM:
                if (hasCopier) {
                    return CodeBlock.of("$T.$N(e.$N())",
                                        keyCopier.get(),
                                        serviceModelCopiers.stringToEnumCopyMethodName(),
                                        getterName);
                } else {
                    return CodeBlock.of("$T.fromValue(e.$N())",
                                        poetExtensions.getModelClass(keyValModel.getEnumType()),
                                        getterName);
                }
            case ENUM_TO_STRING:
                if (hasCopier) {
                    return CodeBlock.of("$T.$N(e.$N())",
                                        keyCopier.get(),
                                        serviceModelCopiers.enumToStringCopyMethodName(),
                                        getterName);
                } else {
                    return CodeBlock.of("e.$N().toString()", getterName);
                }
            case NONE:
                if (hasCopier) {
                    return CodeBlock.of("$T.$N(e.$N())",
                                        keyCopier.get(),
                                        serviceModelCopiers.copyMethodName(),
                                        getterName);
                } else {
                    return CodeBlock.of("e.$N()", getterName);
                }
            default:
                throw new IllegalArgumentException("Unknown enum transform: " + enumTransform);
        }
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
