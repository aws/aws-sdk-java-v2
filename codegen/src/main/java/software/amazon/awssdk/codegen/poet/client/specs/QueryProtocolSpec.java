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

package software.amazon.awssdk.codegen.poet.client.specs;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.protocols.query.AwsQueryProtocolFactory;

public class QueryProtocolSpec implements ProtocolSpec {

    protected final PoetExtensions poetExtensions;

    public QueryProtocolSpec(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    @Override
    public FieldSpec protocolFactory(IntermediateModel model) {
        return FieldSpec.builder(protocolFactoryClass(), "protocolFactory")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();
    }

    protected Class<?> protocolFactoryClass() {
        return AwsQueryProtocolFactory.class;
    }

    @Override
    public MethodSpec initProtocolFactory(IntermediateModel model) {
        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("init")
                                                  .returns(protocolFactoryClass())
                                                  .addModifiers(Modifier.PRIVATE);

        methodSpec.addCode("return $T.builder()\n", protocolFactoryClass());

        registerModeledExceptions(model, poetExtensions).forEach(methodSpec::addCode);

        methodSpec.addCode(".clientConfiguration(clientConfiguration)\n"
                           + ".defaultServiceExceptionSupplier($T::builder)\n",
                           poetExtensions.getModelClass(model.getSdkModeledExceptionBaseClassName()));
        methodSpec.addCode(".build();");

        return methodSpec.build();
    }

    @Override
    public CodeBlock responseHandler(IntermediateModel model,
                                     OperationModel opModel) {
        ClassName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        return CodeBlock.builder()
                        .addStatement("\n\n$T<$T> responseHandler = protocolFactory.createResponseHandler($T::builder)",
                                      HttpResponseHandler.class,
                                      responseType,
                                      responseType)
                        .build();
    }

    @Override
    public CodeBlock errorResponseHandler(OperationModel opModel) {
        return CodeBlock.builder().add("\n\n$T errorResponseHandler = protocolFactory.createErrorResponseHandler();",
                                       ParameterizedTypeName.get(HttpResponseHandler.class, AwsServiceException.class))
                        .build();
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        TypeName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");
        CodeBlock.Builder codeBlock = CodeBlock
            .builder()
            .add("\n\nreturn clientHandler.execute(new $T<$T, $T>()" +
                 ".withResponseHandler($N)" +
                 ".withErrorResponseHandler($N)" +
                 ".withInput($L)",
                 ClientExecutionParams.class,
                 requestType,
                 responseType,
                 "responseHandler",
                 "errorResponseHandler",
                 opModel.getInput().getVariableName());
        if (opModel.hasStreamingInput()) {
            return codeBlock.add(".withRequestBody(requestBody)")
                            .add(".withMarshaller(new $T(new $T(protocolFactory), requestBody)));",
                                 ParameterizedTypeName.get(ClassName.get(StreamingRequestMarshaller.class), requestType),
                                 marshaller)
                            .build();
        }
        return codeBlock.add(".withMarshaller(new $T(protocolFactory)) $L);", marshaller,
                             opModel.hasStreamingOutput() ? ", responseTransformer" : "").build();
    }

    @Override
    public CodeBlock asyncExecutionHandler(IntermediateModel intermediateModel, OperationModel opModel) {
        ClassName pojoResponseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        String asyncRequestBody = opModel.hasStreamingInput() ? ".withAsyncRequestBody(requestBody)"
                                                              : "";
        return CodeBlock.builder().add("\n\nreturn clientHandler.execute(new $T<$T, $T>()\n" +
                                       ".withMarshaller(new $T(protocolFactory))" +
                                       ".withResponseHandler(responseHandler)" +
                                       ".withErrorResponseHandler($N)\n" +
                                       asyncRequestBody +
                                       ".withInput($L) $L);",
                                       ClientExecutionParams.class,
                                       requestType,
                                       pojoResponseType,
                                       marshaller,
                                       "errorResponseHandler",
                                       opModel.getInput().getVariableName(),
                                       opModel.hasStreamingOutput() ? ", asyncResponseTransformer" : "")
                        .build();
    }

    @Override
    public Optional<MethodSpec> createErrorResponseHandler() {
        return Optional.empty();
    }
}