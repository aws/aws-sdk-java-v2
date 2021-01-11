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
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.SdkBytes;

/**
 * Abstract implementation of {@link MemberSetters} to share common functionality.
 */
abstract class AbstractMemberSetters implements MemberSetters {
    protected final PoetExtensions poetExtensions;
    private final ShapeModel shapeModel;
    private final MemberModel memberModel;
    private final TypeProvider typeProvider;
    private final ServiceModelCopiers serviceModelCopiers;

    AbstractMemberSetters(IntermediateModel intermediateModel,
                          ShapeModel shapeModel,
                          MemberModel memberModel,
                          TypeProvider typeProvider) {
        this.shapeModel = shapeModel;
        this.memberModel = memberModel;
        this.typeProvider = typeProvider;
        this.serviceModelCopiers = new ServiceModelCopiers(intermediateModel);
        this.poetExtensions = new PoetExtensions(intermediateModel);
    }

    protected MethodSpec.Builder fluentAbstractSetterDeclaration(ParameterSpec parameter, TypeName returnType) {
        return fluentSetterDeclaration(parameter, returnType).addModifiers(Modifier.ABSTRACT);
    }

    protected MethodSpec.Builder fluentAbstractSetterDeclaration(String methodName,
                                                                 ParameterSpec parameter,
                                                                 TypeName returnType) {
        return setterDeclaration(methodName, parameter, returnType).addModifiers(Modifier.ABSTRACT);
    }


    protected MethodSpec.Builder fluentDefaultSetterDeclaration(ParameterSpec parameter, TypeName returnType) {
        return fluentSetterDeclaration(parameter, returnType).addModifiers(Modifier.DEFAULT);
    }

    protected MethodSpec.Builder fluentSetterBuilder(TypeName returnType) {
        return fluentSetterBuilder(memberAsParameter(), returnType);
    }

    protected MethodSpec.Builder fluentSetterBuilder(String methodName, TypeName returnType) {
        return fluentSetterBuilder(methodName, memberAsParameter(), returnType);
    }

    protected MethodSpec.Builder fluentSetterBuilder(ParameterSpec setterParam, TypeName returnType) {
        return fluentSetterBuilder(memberModel().getFluentSetterMethodName(), setterParam, returnType);
    }

    protected MethodSpec.Builder fluentSetterBuilder(String methodName, ParameterSpec setterParam, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                         .addParameter(setterParam)
                         .addAnnotation(Override.class)
                         .returns(returnType)
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    protected MethodSpec.Builder beanStyleSetterBuilder() {
        return beanStyleSetterBuilder(memberAsBeanStyleParameter(), memberModel().getBeanStyleSetterMethodName());
    }

    protected MethodSpec.Builder deprecatedBeanStyleSetterBuilder() {
        return beanStyleSetterBuilder(memberAsBeanStyleParameter(), memberModel().getDeprecatedBeanStyleSetterMethodName());
    }

    protected MethodSpec.Builder beanStyleSetterBuilder(ParameterSpec setterParam, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addParameter(setterParam)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    protected CodeBlock copySetterBody() {
        return copySetterBody("this.$1N = $2T.$3N($1N)", "this.$1N = $1N", serviceModelCopiers.copyMethodName());
    }

    protected CodeBlock fluentSetterWithEnumCollectionsParameterMethodBody() {
        return copySetterBody("this.$1N = $2T.$3N($1N)", "this.$1N = $1N",
                              serviceModelCopiers.enumToStringCopyMethodName());
    }


    protected CodeBlock copySetterBodyWithModeledEnumParameter() {
        return copySetterBody("this.$1N = $2T.$3N($1N)", "this.$1N = $1N",
                              serviceModelCopiers.enumToStringCopyMethodName());
    }

    protected CodeBlock copySetterBuilderBody() {
        if (memberModel.hasBuilder()) {
            return copySetterBody("this.$1N = $1N != null ? $2T.$3N($1N.build()) : null",
                                  "this.$1N = $1N != null ? $1N.build() : null",
                                  serviceModelCopiers.copyMethodName());
        }
        if (memberModel.isCollectionWithBuilderMember()) {
            return copySetterBody("this.$1N = $2T.$3N($1N)", null, serviceModelCopiers.builderCopyMethodName());
        }
        return copySetterBody();
    }

    protected CodeBlock beanCopySetterBody() {
        if (memberModel.isSdkBytesType()) {
            return sdkBytesSetter();
        }
        if (memberModel.isList() && memberModel.getListModel().getListMemberModel().isSdkBytesType()) {
            return sdkBytesListSetter();
        }
        if (memberModel.isMap() && memberModel.getMapModel().getValueModel().isSdkBytesType()) {
            return sdkBytesMapValueSetter();
        }

        return copySetterBuilderBody();
    }

    private CodeBlock sdkBytesSetter() {
        return CodeBlock.of("$1N($2N == null ? null : $3T.fromByteBuffer($2N));",
                            memberModel.getFluentSetterMethodName(), fieldName(),
                            SdkBytes.class);
    }

    private CodeBlock sdkBytesListSetter() {
        return CodeBlock.of("$1N($2N == null ? null : $2N.stream().map($3T::fromByteBuffer).collect($4T.toList()));",
                            memberModel.getFluentSetterMethodName(), fieldName(),
                            SdkBytes.class, Collectors.class);
    }

    private CodeBlock sdkBytesMapValueSetter() {
        return CodeBlock.of("$1N($2N == null ? null : " +
                            "$2N.entrySet().stream()" +
                            ".collect($4T.toMap(e -> e.getKey(), e -> $3T.fromByteBuffer(e.getValue()))));",
                            memberModel.getFluentSetterMethodName(), fieldName(),
                            SdkBytes.class, Collectors.class);
    }

    protected ParameterSpec memberAsParameter() {
        return ParameterSpec.builder(typeProvider.parameterType(memberModel), fieldName()).build();
    }

    protected ParameterSpec memberAsBeanStyleParameter() {
        if (memberModel.hasBuilder()) {
            TypeName builderName = poetExtensions.getModelClass(memberModel.getC2jShape()).nestedClass("BuilderImpl");
            return ParameterSpec.builder(builderName, fieldName()).build();
        }

        if (memberModel.isList()) {
            MemberModel listMember = memberModel.getListModel().getListMemberModel();

            if (hasBuilder(listMember)) {
                TypeName memberName = poetExtensions.getModelClass(listMember.getC2jShape()).nestedClass("BuilderImpl");
                TypeName listType = ParameterizedTypeName.get(ClassName.get(Collection.class), memberName);
                return ParameterSpec.builder(listType, fieldName()).build();
            } else if (listMember.isSdkBytesType()) {
                TypeName listType = ParameterizedTypeName.get(Collection.class, ByteBuffer.class);
                return ParameterSpec.builder(listType, fieldName()).build();
            }
        }

        if (memberModel.isMap()) {
            MemberModel keyModel = memberModel.getMapModel().getKeyModel();
            TypeName keyType = typeProvider.getTypeNameForSimpleType(keyModel.getVariable().getVariableType());
            MemberModel valueModel = memberModel.getMapModel().getValueModel();
            TypeName valueType = null;

            if (hasBuilder(valueModel)) {
                valueType = poetExtensions.getModelClass(valueModel.getC2jShape()).nestedClass("BuilderImpl");
            } else if (valueModel.isSdkBytesType()) {
                valueType = TypeName.get(ByteBuffer.class);
            }

            if (valueType != null) {
                TypeName mapType = ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType);
                return ParameterSpec.builder(mapType, fieldName()).build();
            }
        }

        if (memberModel.isSdkBytesType()) {
            return ParameterSpec.builder(ByteBuffer.class, fieldName()).build();
        }

        return memberAsParameter();
    }

    protected ShapeModel shapeModel() {
        return shapeModel;
    }

    protected MemberModel memberModel() {
        return memberModel;
    }

    protected String fieldName() {
        return memberModel.getVariable().getVariableName();
    }

    private MethodSpec.Builder fluentSetterDeclaration(ParameterSpec parameter, TypeName returnType) {
        return setterDeclaration(memberModel().getFluentSetterMethodName(), parameter, returnType);
    }

    private MethodSpec.Builder setterDeclaration(String methodName, ParameterSpec parameter, TypeName returnType) {
        return MethodSpec.methodBuilder(methodName)
                         .addModifiers(Modifier.PUBLIC)
                         .addParameter(parameter)
                         .returns(returnType);
    }

    private CodeBlock copySetterBody(String copyAssignment, String regularAssignment, String copyMethodName) {
        Optional<ClassName> copierClass = serviceModelCopiers.copierClassFor(memberModel);

        return copierClass.map(className -> CodeBlock.builder().addStatement(copyAssignment,
                                                                             fieldName(),
                                                                             className,
                                                                             copyMethodName)
                                                     .build())
                          .orElseGet(() -> CodeBlock.builder().addStatement(regularAssignment, fieldName()).build());
    }

    private boolean hasBuilder(MemberModel model) {
        return model != null && model.hasBuilder();
    }
}
