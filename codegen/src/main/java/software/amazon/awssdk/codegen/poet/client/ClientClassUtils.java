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

package software.amazon.awssdk.codegen.poet.client;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.Optional;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.utils.Validate;

final class ClientClassUtils {
    private static final String PAGINATOR_USER_AGENT = "PAGINATED";

    private ClientClassUtils() {
    }

    static Optional<CodeBlock> getCustomResponseHandler(OperationModel operationModel, ClassName returnType) {
        Optional<String> customUnmarshaller = Optional.ofNullable(operationModel.getOutputShape())
                                                      .map(ShapeModel::getCustomization)
                                                      .flatMap(c -> Optional.ofNullable(c.getCustomUnmarshallerFqcn()));
        return customUnmarshaller.map(unmarshaller -> {
            if (operationModel.hasStreamingOutput()) {
                throw new UnsupportedOperationException("Custom unmarshallers cannot be applied to streaming operations yet.");
            }

            return CodeBlock.builder().add("$T<$T> responseHandler = (response, __) -> new $T().unmarshall(response);",
                                           HttpResponseHandler.class,
                                           returnType,
                                           ClassName.bestGuess(unmarshaller)).build();
        });
    }

    static MethodSpec consumerBuilderVariant(MethodSpec spec, String javadoc) {
        Validate.validState(spec.parameters.size() > 0, "A first parameter is required to generate a consumer-builder method.");
        Validate.validState(spec.parameters.get(0).type instanceof ClassName, "The first parameter must be a class.");

        ParameterSpec firstParameter = spec.parameters.get(0);
        ClassName firstParameterClass = (ClassName) firstParameter.type;
        TypeName consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class), firstParameterClass.nestedClass("Builder"));

        MethodSpec.Builder result = MethodSpec.methodBuilder(spec.name)
                                              .returns(spec.returnType)
                                              .addExceptions(spec.exceptions)
                                              .addJavadoc(javadoc)
                                              .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                                              .addTypeVariables(spec.typeVariables)
                                              .addParameter(ParameterSpec.builder(consumer, firstParameter.name).build());


        // Parameters
        StringBuilder methodBody = new StringBuilder("return $L($T.builder().applyMutation($L).build()");
        for (int i = 1; i < spec.parameters.size(); i++) {
            ParameterSpec parameter = spec.parameters.get(i);
            methodBody.append(", ").append(parameter.name);
            result.addParameter(parameter);
        }
        methodBody.append(")");

        result.addStatement(methodBody.toString(), spec.name, firstParameterClass, firstParameter.name);

        return result.build();
    }

    static MethodSpec applyPaginatorUserAgentMethod(PoetExtensions poetExtensions, IntermediateModel model) {

        TypeVariableName typeVariableName =
            TypeVariableName.get("T", poetExtensions.getModelClass(model.getSdkRequestBaseClassName()));

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName
            .get(ClassName.get(Consumer.class), ClassName.get(AwsRequestOverrideConfiguration.Builder.class));

        CodeBlock codeBlock = CodeBlock.builder()
                                       .addStatement("$T userAgentApplier = b -> b.addApiName($T.builder().version"
                                                     + "($T.SDK_VERSION).name($S).build())",
                                                     parameterizedTypeName, ApiName.class,
                                                     VersionInfo.class,
                                                     PAGINATOR_USER_AGENT)
                                       .addStatement("$T overrideConfiguration =\n"
                                                     + "            request.overrideConfiguration().map(c -> c.toBuilder()"
                                                     + ".applyMutation"
                                                     + "(userAgentApplier).build())\n"
                                                     + "            .orElse((AwsRequestOverrideConfiguration.builder()"
                                                     + ".applyMutation"
                                                     + "(userAgentApplier).build()))", AwsRequestOverrideConfiguration.class)
                                       .addStatement("return (T) request.toBuilder().overrideConfiguration"
                                                     + "(overrideConfiguration).build()")
                                       .build();

        return MethodSpec.methodBuilder("applyPaginatorUserAgent")
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(typeVariableName, "request")
                         .addTypeVariable(typeVariableName)
                         .addCode(codeBlock)
                         .returns(typeVariableName)
                         .build();
    }

    static MethodSpec applySignerOverrideMethod(PoetExtensions poetExtensions, IntermediateModel model) {
        final String signerOverrideVariable = "signerOverride";

        TypeVariableName typeVariableName =
            TypeVariableName.get("T", poetExtensions.getModelClass(model.getSdkRequestBaseClassName()));

        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName
            .get(ClassName.get(Consumer.class), ClassName.get(AwsRequestOverrideConfiguration.Builder.class));

        CodeBlock codeBlock = CodeBlock.builder()
                                       .beginControlFlow("if (request.overrideConfiguration().flatMap(c -> c.signer())"
                                                         + ".isPresent())")
                                       .addStatement("return request")
                                       .endControlFlow()
                                       .addStatement("$T $L = b -> b.signer(signer).build()",
                                                     parameterizedTypeName,
                                                     signerOverrideVariable)
                                       .addStatement("$1T overrideConfiguration =\n"
                                                     + "            request.overrideConfiguration().map(c -> c.toBuilder()"
                                                     + ".applyMutation($2L).build())\n"
                                                     + "            .orElse((AwsRequestOverrideConfiguration.builder()"
                                                     + ".applyMutation($2L).build()))",
                                                     AwsRequestOverrideConfiguration.class,
                                                     signerOverrideVariable)
                                       .addStatement("return (T) request.toBuilder().overrideConfiguration"
                                                     + "(overrideConfiguration).build()")
                                       .build();

        return MethodSpec.methodBuilder("applySignerOverride")
                         .addModifiers(Modifier.PRIVATE)
                         .addParameter(typeVariableName, "request")
                         .addParameter(Signer.class, "signer")
                         .addTypeVariable(typeVariableName)
                         .addCode(codeBlock)
                         .returns(typeVariableName)
                         .build();
    }

    static CodeBlock callApplySignerOverrideMethod(OperationModel opModel) {
        CodeBlock.Builder code = CodeBlock.builder();
        ShapeModel inputShape = opModel.getInputShape();

        if (inputShape.getRequestSignerClassFqcn() != null) {
            code.addStatement("$1L = applySignerOverride($1L, $2T.create())",
                              opModel.getInput().getVariableName(),
                              PoetUtils.classNameFromFqcn(inputShape.getRequestSignerClassFqcn()));
        }

        return code.build();
    }
}
