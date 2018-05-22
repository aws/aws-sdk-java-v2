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

import static software.amazon.awssdk.codegen.model.intermediate.Protocol.AWS_JSON;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocol;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.client.ClientExecutionParams;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.core.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;

public class JsonProtocolSpec implements ProtocolSpec {

    private final PoetExtensions poetExtensions;

    public JsonProtocolSpec(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    @Override
    public FieldSpec protocolFactory(IntermediateModel model) {
        return FieldSpec.builder(AwsJsonProtocolFactory.class, "protocolFactory")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();
    }

    @Override
    public MethodSpec initProtocolFactory(IntermediateModel model) {
        String exceptionPath = model.getSdkModeledExceptionBaseFqcn()
                                    .substring(0, model.getSdkModeledExceptionBaseFqcn().lastIndexOf("."));

        ClassName baseException = ClassName.get(exceptionPath, model.getSdkModeledExceptionBaseClassName());

        Metadata metadata = model.getMetadata();
        ClassName protocolFactory = poetExtensions.getClientClass(metadata.getProtocolFactory());

        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("init")
                                                  .returns(protocolFactory)
                                                  .addModifiers(Modifier.PRIVATE)
                                                  .addCode(
                                                          "return new $T(new $T()\n" +
                                                          ".withSupportsCbor($L)\n" +
                                                          ".withSupportsIon($L)" +
                                                          ".withBaseServiceExceptionClass($L.class)",
                                                          AwsJsonProtocolFactory.class,
                                                          JsonClientMetadata.class,
                                                          metadata.isCborProtocol(),
                                                          metadata.isIonProtocol(), baseException);

        if (metadata.getContentType() != null) {
            methodSpec.addCode(".withContentTypeOverride($S)", metadata.getContentType());
        }

        errorUnmarshallers(model).forEach(methodSpec::addCode);

        methodSpec.addCode(",\n");
        methodSpec.addCode("$T.builder().protocolVersion($S)\n" +
                           ".protocol($T.$L).build()", AwsJsonProtocolMetadata.class,
                           metadata.getJsonVersion(), AwsJsonProtocol.class, protocolEnumName(metadata.getProtocol()));

        methodSpec.addCode(");");

        return methodSpec.build();
    }

    @Override
    public CodeBlock responseHandler(OperationModel opModel) {
        ClassName unmarshaller = poetExtensions.getTransformClass(opModel.getReturnType().getReturnType() + "Unmarshaller");
        ClassName returnType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        return CodeBlock
                .builder()
                .add("\n\n$T<$T> responseHandler = $L.createResponseHandler(new $T()" +
                     "                                   .withPayloadJson($L)" +
                     "                                   .withHasStreamingSuccessResponse($L), new $T());",
                     HttpResponseHandler.class,
                     returnType,
                     "protocolFactory",
                     JsonOperationMetadata.class,
                     !opModel.getHasBlobMemberAsPayload(),
                     opModel.hasStreamingOutput(),
                     unmarshaller)
                .build();
    }

    @Override
    public CodeBlock errorResponseHandler(OperationModel opModel) {
        return CodeBlock
                .builder()
                .add("\n\n$T<$T> errorResponseHandler = createErrorResponseHandler();",
                     HttpResponseHandler.class, SdkServiceException.class)
                .build();
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        TypeName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        final CodeBlock.Builder codeBlock = CodeBlock
                .builder()
                .add("\n\nreturn clientHandler.execute(new $T<$T, $T>()\n" +
                     ".withResponseHandler($N)\n" +
                     ".withErrorResponseHandler($N)\n" +
                     ".withInput($L)\n",
                     ClientExecutionParams.class,
                     requestType,
                     responseType,
                     "responseHandler",
                     "errorResponseHandler",
                     opModel.getInput().getVariableName());

        if (opModel.hasStreamingInput()) {
            return codeBlock.add(".withMarshaller(new $T(new $T(protocolFactory), requestBody)));",
                                 ParameterizedTypeName.get(ClassName.get(StreamingRequestMarshaller.class), requestType),
                                 marshaller)
                            .build();
        }

        return codeBlock.add(".withMarshaller(new $T(protocolFactory))$L);", marshaller,
                             opModel.hasStreamingOutput() ? ", responseTransformer" : "")
                        .build();
    }

    @Override
    public CodeBlock asyncExecutionHandler(OperationModel opModel) {
        ClassName pojoResponseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        String asyncRequestBody = opModel.hasStreamingInput() ? ".withAsyncRequestBody(requestBody)"
                : "";
        return CodeBlock.builder().add("\n\nreturn clientHandler.execute(new $T<$T, $T>()\n" +
                                       ".withMarshaller(new $T(protocolFactory))" +
                                       ".withResponseHandler(responseHandler)" +
                                       ".withErrorResponseHandler(errorResponseHandler)\n" +
                                       asyncRequestBody +
                                       ".withInput($L)$L);",
                                       ClientExecutionParams.class,
                                       requestType,
                                       pojoResponseType,
                                       marshaller,
                                       opModel.getInput().getVariableName(),
                                       opModel.hasStreamingOutput() ? ", asyncResponseTransformer" : "")
                        .build();
    }

    @Override
    public Optional<MethodSpec> createErrorResponseHandler() {
        ClassName httpResponseHandler = ClassName.get(HttpResponseHandler.class);
        ClassName sdkBaseException = ClassName.get(SdkServiceException.class);
        TypeName responseHandlerOfException = ParameterizedTypeName.get(httpResponseHandler, sdkBaseException);

        return Optional.of(MethodSpec.methodBuilder("createErrorResponseHandler")
                                     .returns(responseHandlerOfException)
                                     .addModifiers(Modifier.PRIVATE)
                                     .addStatement("return protocolFactory.createErrorResponseHandler(new $T())",
                                                   JsonErrorResponseMetadata.class)
                                     .build());
    }

    @Override
    public List<CodeBlock> errorUnmarshallers(IntermediateModel model) {
        List<ShapeModel> exceptions = model.getShapes().values().stream()
                                           .filter(s -> s.getShapeType().equals(ShapeType.Exception))
                                           .collect(Collectors.toList());

        return exceptions.stream().map(s -> {
            ClassName exceptionClass = poetExtensions.getModelClass(s.getShapeName());
            return CodeBlock.builder().add(".addErrorMetadata(new $T().withErrorCode($S).withModeledClass($T.class))",
                                           JsonErrorShapeMetadata.class,
                                           s.getErrorCode(),
                                           exceptionClass)
                            .build();
        }).collect(Collectors.toList());
    }

    private String protocolEnumName(software.amazon.awssdk.codegen.model.intermediate.Protocol protocol) {
        switch (protocol) {
            case CBOR:
            case ION:
            case AWS_JSON:
                return AWS_JSON.name();
            default:
                return protocol.name();
        }
    }
}
