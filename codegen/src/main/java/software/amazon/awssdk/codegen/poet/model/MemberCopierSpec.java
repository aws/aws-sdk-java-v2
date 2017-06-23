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
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.MapModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.runtime.StandardMemberCopier;

class MemberCopierSpec implements ClassSpec {
    private final MemberModel memberModel;
    private final ServiceModelCopiers serviceModelCopiers;
    private final TypeProvider typeProvider;

    MemberCopierSpec(MemberModel memberModel, ServiceModelCopiers serviceModelCopiers, TypeProvider typeProvider) {
        this.memberModel = memberModel;
        this.serviceModelCopiers = serviceModelCopiers;
        this.typeProvider = typeProvider;
    }

    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className())
                .addModifiers(Modifier.FINAL)
                .addAnnotation(PoetUtils.GENERATED)
                .addMethod(copyMethod())
                .build();
    }

    @Override
    public ClassName className() {
        return serviceModelCopiers.copierClassFor(memberModel).get();
    }

    private MethodSpec copyMethod() {
        return copyMethodProto().addCode(copyMethodBody()).build();
    }

    private MethodSpec.Builder copyMethodProto() {
        TypeName parameterType = typeProvider.parameterType(memberModel);
        return MethodSpec.methodBuilder(serviceModelCopiers.copyMethodName())
                .addModifiers(Modifier.STATIC)
                .addParameter(ParameterSpec.builder(parameterType, memberParamName()).build())
                .returns(typeProvider.fieldType(memberModel));
    }

    private CodeBlock copyMethodBody() {
        if (memberModel.isMap()) {
            return mapCopyBody();
        }

        if (memberModel.isList()) {
            return listCopyBody();
        }

        return modelCopyBody();
    }

    private CodeBlock listCopyBody() {
        String paramName = memberParamName();
        MemberModel listMember = memberModel.getListModel().getListMemberModel();
        String copyName = paramName + "Copy";
        CodeBlock.Builder builder = CodeBlock.builder()
                .beginControlFlow("if ($N == null)", memberParamName())
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T $N = new $T<>($N.size())", typeProvider.fieldType(memberModel), copyName,
                        typeProvider.listImplClassName(), paramName)
                .beginControlFlow("for ($T e : $N)", typeProvider.parameterType(listMember), paramName);

        CodeBlock.Builder elementCopyExprBuilder = CodeBlock.builder();
        serviceModelCopiers.copierClassFor(listMember)
                .map(copyClass -> elementCopyExprBuilder.add("$T.$N(e)", copyClass, serviceModelCopiers.copyMethodName()))
                .orElseGet(() -> elementCopyExprBuilder.add("e"));

        return builder.addStatement("$N.add($L)", copyName, elementCopyExprBuilder.build())
                .endControlFlow()
                .addStatement("return $N", copyName).build();
    }

    private CodeBlock mapCopyBody() {
        MapModel mapModel = memberModel.getMapModel();
        String paramName = memberParamName();
        String copyName = paramName + "Copy";
        CodeBlock.Builder builder = CodeBlock.builder()
                .beginControlFlow("if ($N == null)", memberParamName())
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$T $N = new $T<>($N.size())", typeProvider.fieldType(memberModel),
                        copyName, typeProvider.mapImplClassName(), memberParamName())
                .beginControlFlow("for ($T e : $N.entrySet())", typeProvider.mapEntryType(mapModel), paramName);


        CodeBlock keyCopyExpr = CodeBlock.of("$T.$N(e.getKey())",
                ClassName.get(StandardMemberCopier.class), serviceModelCopiers.copyMethodName());

        CodeBlock.Builder valueCopyExprBuilder = CodeBlock.builder();
        serviceModelCopiers.copierClassFor(mapModel.getValueModel())
                .map(copyClass -> valueCopyExprBuilder.add("$T.$N(e.getValue())", copyClass,
                        serviceModelCopiers.copyMethodName()))
                .orElseGet(() -> valueCopyExprBuilder.add("e.getValue()"));

        return builder.addStatement("$N.put($L, $L)", copyName, keyCopyExpr, valueCopyExprBuilder.build())
                .endControlFlow()
                .addStatement("return $N", copyName)
                .build();
    }

    private CodeBlock modelCopyBody() {
        // These are immutable so just return the instance
        return CodeBlock.builder()
                .addStatement("return $N", memberParamName())
                .build();
    }

    private String memberParamName() {
        if (memberModel.isSimple()) {
            return Utils.unCapitialize(memberModel.getVariable().getSimpleType()) + "Param";
        }
        return Utils.unCapitialize(memberModel.getC2jShape()) + "Param";
    }
}
