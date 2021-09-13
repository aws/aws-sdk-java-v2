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

import static software.amazon.awssdk.codegen.poet.model.TypeProvider.ShapeTransformation.USE_BUILDER;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.model.TypeProvider.TypeNameOptions;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

public final class BeanGetterHelper {
    private final PoetExtensions poetExtensions;
    private final ServiceModelCopiers copiers;
    private final TypeProvider typeProvider;

    public BeanGetterHelper(PoetExtensions poetExtensions, ServiceModelCopiers copiers, TypeProvider typeProvider) {
        this.poetExtensions = poetExtensions;
        this.copiers = copiers;
        this.typeProvider = typeProvider;
    }

    public MethodSpec beanStyleGetter(MemberModel memberModel) {
        if (memberModel.hasBuilder()) {
            return builderGetter(memberModel);
        }
        if (memberModel.containsBuildable()) {
            return buildableContainerGetter(memberModel);
        }
        if (memberModel.isSdkBytesType()) {
            return byteBufferGetter(memberModel);
        }
        if (memberModel.isList() && memberModel.getListModel().getListMemberModel().isSdkBytesType()) {
            return listByteBufferGetter(memberModel);
        }
        if (memberModel.isMap() && memberModel.getMapModel().getValueModel().isSdkBytesType()) {
            return mapByteBufferGetter(memberModel);
        }
        return regularGetter(memberModel);
    }

    private MethodSpec buildableContainerGetter(MemberModel memberModel) {
        TypeName returnType = typeProvider.typeName(memberModel, new TypeNameOptions().shapeTransformation(USE_BUILDER)
                                                                                      .useByteBufferTypes(true));

        ClassName copierClass = copiers.copierClassFor(memberModel)
                                       .orElseThrow(() -> new IllegalStateException("Copier class not found for " + memberModel));

        String variableName = memberModel.getVariable().getVariableName();

        CodeBlock.Builder code = CodeBlock.builder();

        code.add("$T result = $T.$N(this.$N);", returnType, copierClass, copiers.copyToBuilderMethodName(), variableName);

        if (memberModel.isList()) {
            code.add("if (result instanceof $T) {", SdkAutoConstructList.class)
                .add("return null;")
                .add("}");
        }

        if (memberModel.isMap()) {
            code.add("if (result instanceof $T) {", SdkAutoConstructMap.class)
                .add("return null;")
                .add("}");
        }

        code.add("return result;");

        return MethodSpec.methodBuilder(memberModel.getBeanStyleGetterMethodName())
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                         .returns(returnType)
                         .addCode(code.build())
                         .build();
    }

    private MethodSpec byteBufferGetter(MemberModel memberModel) {
        return basicGetter(memberModel,
                           ClassName.get(ByteBuffer.class),
                           CodeBlock.of("return $1N == null ? null : $1N.asByteBuffer();",
                                        memberModel.getVariable().getVariableName()));
    }

    private MethodSpec listByteBufferGetter(MemberModel memberModel) {
        return basicGetter(memberModel,
                           ParameterizedTypeName.get(List.class, ByteBuffer.class),
                           CodeBlock.of("return $1N == null ? null : $1N.stream().map($2T::asByteBuffer).collect($3T.toList());",
                                        memberModel.getVariable().getVariableName(), SdkBytes.class, Collectors.class));
    }

    private MethodSpec mapByteBufferGetter(MemberModel memberModel) {
        String body = "return $1N == null ? null : " +
                      "$1N.entrySet().stream().collect($2T.toMap(e -> e.getKey(), e -> e.getValue().asByteBuffer()));";
        String keyType = memberModel.getMapModel().getKeyModel().getVariable().getVariableType();
        return basicGetter(memberModel,
                           PoetUtils.createParameterizedTypeName(Map.class, keyType, ByteBuffer.class.getSimpleName()),
                           CodeBlock.of(body, memberModel.getVariable().getVariableName(), Collectors.class));
    }

    private MethodSpec regularGetter(MemberModel memberModel) {
        return basicGetter(memberModel,
                           typeProvider.parameterType(memberModel),
                           CodeBlock.of("return $N;", memberModel.getVariable().getVariableName()));
    }

    private MethodSpec builderGetter(MemberModel memberModel) {
        return basicGetter(memberModel,
                           poetExtensions.getModelClass(memberModel.getC2jShape()).nestedClass("Builder"),
                           CodeBlock.of("return $1N != null ? $1N.toBuilder() : null;",
                                        memberModel.getVariable().getVariableName()));
    }

    private MethodSpec basicGetter(MemberModel memberModel, TypeName returnType, CodeBlock body) {
        CodeBlock.Builder getterBody = CodeBlock.builder();

        memberModel.getAutoConstructClassIfExists().ifPresent(autoConstructClass -> {
            getterBody.add("if ($N instanceof $T) {", memberModel.getVariable().getVariableName(), autoConstructClass)
                      .add("return null;")
                      .add("}");
        });

        getterBody.add(body);

        return MethodSpec.methodBuilder(memberModel.getBeanStyleGetterMethodName())
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                         .returns(returnType)
                         .addCode(getterBody.build())
                         .build();
    }
}
