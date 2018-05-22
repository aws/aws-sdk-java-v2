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
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.utils.CollectionUtils;

public final class BeanGetterHelper {
    private final PoetExtensions poetExtensions;
    private final TypeProvider typeProvider;

    public BeanGetterHelper(PoetExtensions poetExtensions, TypeProvider typeProvider) {
        this.poetExtensions = poetExtensions;
        this.typeProvider = typeProvider;
    }

    public MethodSpec beanStyleGetter(MemberModel memberModel) {
        if (memberModel.hasBuilder()) {
            return builderGetter(memberModel);
        }
        if (memberModel.isCollectionWithBuilderMember()) {
            return memberModel.isList() ? listOfBuildersGetter(memberModel) : mapOfBuildersGetter(memberModel);
        }
        return regularGetter(memberModel);
    }

    private MethodSpec regularGetter(MemberModel memberModel) {
        return basicGetter(memberModel,
                           typeProvider.parameterType(memberModel),
                           CodeBlock.builder().add("return $N", memberModel.getVariable().getVariableName())
                                    .build());
    }

    private MethodSpec builderGetter(MemberModel memberModel) {
        return basicGetter(memberModel,
                           poetExtensions.getModelClass(memberModel.getC2jShape()).nestedClass("Builder"),
                           CodeBlock.builder().add("return $1N != null ? $1N.toBuilder() : null",
                                                   memberModel.getVariable().getVariableName())
                                    .build());
    }

    private MethodSpec mapOfBuildersGetter(MemberModel memberModel) {
        TypeName keyType = typeProvider.getTypeNameForSimpleType(memberModel.getMapModel().getKeyModel()
                                                                            .getVariable().getVariableType());
        ClassName valueType = poetExtensions.getModelClass(memberModel.getMapModel().getValueModel().getC2jShape());
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType.nestedClass("Builder"));

        return basicGetter(memberModel,
                           returnType,
                           CodeBlock.builder().add("return $1N != null ? $2T.mapValues($1N, $3T::toBuilder) : null",
                                                   memberModel.getVariable().getVariableName(),
                                                   CollectionUtils.class,
                                                   valueType)
                                    .build());
    }

    private MethodSpec listOfBuildersGetter(MemberModel memberModel) {
        ClassName memberType = poetExtensions.getModelClass(memberModel.getListModel().getListMemberModel().getC2jShape());
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(Collection.class), memberType.nestedClass("Builder"));

        return basicGetter(memberModel,
                           returnType,
                           CodeBlock.builder().add(
                               "return $1N != null ? $1N.stream().map($2T::toBuilder).collect($3T.toList()) : null",
                               memberModel.getVariable().getVariableName(),
                               memberType,
                               Collectors.class)
                               .build());
    }

    private MethodSpec basicGetter(MemberModel memberModel, TypeName returnType, CodeBlock statement) {
        return MethodSpec.methodBuilder(memberModel.getBeanStyleGetterMethodName())
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                         .returns(returnType)
                         .addStatement(statement)
                         .build();
    }
}
