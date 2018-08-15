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
import com.squareup.javapoet.WildcardTypeName;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.awscore.eventstream.EventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.eventstream.EventStreamTaggedUnionJsonUnmarshaller;
import software.amazon.awssdk.awscore.eventstream.RestEventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonProtocol;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.handler.AttachHttpMetadataResponseHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.protocol.json.VoidJsonUnmarshaller;
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
        ClassName baseException = baseExceptionClassName(model);

        Metadata metadata = model.getMetadata();
        ClassName protocolFactory = poetExtensions.getClientClass(metadata.getProtocolFactory());

        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("init")
                                                  .addParameter(TypeName.BOOLEAN, "supportsCbor")
                                                  .returns(protocolFactory)
                                                  .addModifiers(Modifier.PRIVATE)
                                                  .addCode(
                                                      "return new $T(new $T()\n" +
                                                      ".withSupportsCbor(supportsCbor)\n" +
                                                      ".withSupportsIon($L)" +
                                                      ".withBaseServiceExceptionClass($L.class)",
                                                      AwsJsonProtocolFactory.class,
                                                      JsonClientMetadata.class,
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
    public CodeBlock responseHandler(IntermediateModel model, OperationModel opModel) {
        ClassName unmarshaller = getUnmarshallerType(opModel);
        TypeName pojoResponseType = getPojoResponseType(opModel);

        String protocolFactory = protocolFactoryLiteral(opModel);
        CodeBlock.Builder builder = CodeBlock.builder();
        if (opModel.hasEventStreamOutput()) {
            responseHandlersForEventStreaming(opModel, unmarshaller, pojoResponseType, protocolFactory, builder);
        } else {
            builder.add("\n\n$T<$T> responseHandler = $L.createResponseHandler(new $T()" +
                        "                                   .withPayloadJson($L)" +
                        "                                   .withHasStreamingSuccessResponse($L), new $T());",
                        HttpResponseHandler.class,
                        pojoResponseType,
                        protocolFactory,
                        JsonOperationMetadata.class,
                        !opModel.getHasBlobMemberAsPayload(),
                        opModel.hasStreamingOutput(),
                        unmarshaller);
        }
        return builder.build();
    }

    @Override
    public CodeBlock errorResponseHandler(OperationModel opModel) {
        String protocolFactory = protocolFactoryLiteral(opModel);

        return CodeBlock
            .builder()
            .add("\n\n$T<$T> errorResponseHandler = createErrorResponseHandler($L);",
                 HttpResponseHandler.class, AwsServiceException.class, protocolFactory)
            .build();
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        TypeName responseType = getPojoResponseType(opModel);
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
            codeBlock.add(".withMarshaller(new $T(new $T(protocolFactory), requestBody))",
                          ParameterizedTypeName.get(ClassName.get(StreamingRequestMarshaller.class), requestType),
                          marshaller);
        } else {
            codeBlock.add(".withMarshaller(new $T(protocolFactory))", marshaller);
        }

        return codeBlock.add("$L);", opModel.hasStreamingOutput() ? ", responseTransformer" : "")
                        .build();
    }

    @Override
    public CodeBlock asyncExecutionHandler(IntermediateModel intermediateModel, OperationModel opModel) {
        final boolean isRestJson = isRestJson(intermediateModel);
        TypeName pojoResponseType = getPojoResponseType(opModel);
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        String asyncRequestBody = opModel.hasStreamingInput() ? ".withAsyncRequestBody(requestBody)"
                                                              : "";
        CodeBlock.Builder builder = CodeBlock.builder();
        if (opModel.hasEventStreamOutput()) {
            ShapeModel shapeModel = EventStreamUtils.getEventStreamInResponse(opModel.getOutputShape());
            ClassName eventStreamBaseClass = poetExtensions.getModelClassFromShape(shapeModel);
            ParameterizedTypeName transformerType = ParameterizedTypeName.get(
                ClassName.get(EventStreamAsyncResponseTransformer.class), pojoResponseType, eventStreamBaseClass);
            builder.addStatement("$1T<$2T> future = new $1T<>()",
                                 ClassName.get(CompletableFuture.class),
                                 ClassName.get(Void.class));
            builder.add("$T asyncResponseTransformer = $T.<$T, $T>builder()\n" +
                        "     .eventStreamResponseHandler(asyncResponseHandler)\n"
                        + "   .eventResponseHandler(eventResponseHandler)\n"
                        + "   .initialResponseHandler(responseHandler)\n"
                        + "   .exceptionResponseHandler(errorResponseHandler)\n"
                        + "   .future(future)\n"
                        + "   .executor(executor)\n"
                        + "   .serviceName(serviceName())\n"
                        + "   .build();",
                        transformerType,
                        ClassName.get(EventStreamAsyncResponseTransformer.class),
                        pojoResponseType,
                        eventStreamBaseClass);

            if (isRestJson) {
                builder.add(restAsyncResponseTransformer(pojoResponseType, eventStreamBaseClass));
            }
        }

        boolean isStreaming = opModel.hasStreamingOutput() || opModel.hasEventStreamOutput();
        String protocolFactory = protocolFactoryLiteral(opModel);
        String customerResponseHandler = opModel.hasEventStreamOutput() ? "asyncResponseHandler" : "asyncResponseTransformer";
        builder.add("\n\n$L clientHandler.execute(new $T<$T, $T>()\n" +
                    ".withMarshaller(new $T($L))\n" +
                    "$L" +
                    "$L" +
                    ".withResponseHandler($L)\n" +
                    ".withErrorResponseHandler(errorResponseHandler)\n" +
                    asyncRequestBody +
                    ".withInput($L)$L)$L;",
                    // If the operation has an event stream output we use a different future so we don't return the one
                    // from the client.
                    opModel.hasEventStreamOutput() ? "" : "return",
                    ClientExecutionParams.class,
                    requestType,
                    opModel.hasEventStreamOutput() && !isRestJson ? SdkResponse.class : pojoResponseType,
                    marshaller,
                    protocolFactory,
                    opModel.hasEventStreamInput() ? CodeBlock.builder()
                                                             .add(".withAsyncRequestBody($T.fromPublisher(adapted))",
                                                                  AsyncRequestBody.class)
                                                             .build()
                                                             .toString()
                                                  : "",
                    opModel.hasEventStreamInput() && opModel.hasEventStreamOutput() ? CodeBlock
                        .builder().add(".withFullDuplex(true)").build() : "",
                    opModel.hasEventStreamOutput() && !isRestJson ? "voidResponseHandler" : "responseHandler",
                    opModel.getInput().getVariableName(),
                    asyncResponseTransformerVariable(isStreaming, isRestJson, opModel),
                    whenCompleteBody(opModel, customerResponseHandler));
        if (opModel.hasEventStreamOutput()) {
            builder.addStatement("return future");
        }
        return builder.build();
    }

    private String asyncResponseTransformerVariable(boolean isStreaming, boolean isRestJson, OperationModel opModel) {
        if (isStreaming) {
            if (opModel.hasEventStreamOutput() && isRestJson) {
                return  ", restAsyncResponseTransformer";
            } else {
                return  ", asyncResponseTransformer";
            }
        }
        return "";
    }

    /**
     * For Rest services, we need to use the {@link RestEventStreamAsyncResponseTransformer} instead of
     * {@link EventStreamAsyncResponseTransformer} class. This method has the code to create a restAsyncResponseTransformer
     * variable.
     *
     * @param pojoResponseType Type of operation response shape
     * @param eventStreamBaseClass Class name for the base class of all events in the operation
     */
    private CodeBlock restAsyncResponseTransformer(TypeName pojoResponseType, ClassName eventStreamBaseClass) {
        ParameterizedTypeName restTransformerType = ParameterizedTypeName.get(
            ClassName.get(RestEventStreamAsyncResponseTransformer.class), pojoResponseType, eventStreamBaseClass);
        return CodeBlock.builder()
                        .add("$T restAsyncResponseTransformer = $T.<$T, $T>builder()\n"
                             + ".eventStreamAsyncResponseTransformer(asyncResponseTransformer)\n"
                             + ".eventStreamResponseHandler(asyncResponseHandler)\n"
                             + ".build();",
                             restTransformerType,
                             ClassName.get(RestEventStreamAsyncResponseTransformer.class),
                             pojoResponseType,
                             eventStreamBaseClass)
                        .build();
    }


    /**
     * For streaming operations we need to notify the response handler or response transformer on exception so
     * we add a .whenComplete to the future.
     *
     * @param operationModel Op model.
     * @param responseHandlerName Variable name of response handler customer passed in.
     * @return whenComplete to append to future.
     */
    private String whenCompleteBody(OperationModel operationModel, String responseHandlerName) {
        if (operationModel.hasEventStreamOutput()) {
            return eventStreamOutputWhenComplete(responseHandlerName);
        } else if (operationModel.hasStreamingOutput()) {
            return streamingOutputWhenComplete(responseHandlerName);
        } else {
            // Non streaming can just return the future as is
            return "";
        }
    }

    /**
     * Need to notify the response handler/response transformer if the future is completed exceptionally.
     *
     * @param responseHandlerName Variable name of response handler customer passed in.
     * @return whenComplete to append to future.
     */
    private String streamingOutputWhenComplete(String responseHandlerName) {
        return String.format(".whenComplete((r, e) -> {%n"
                             + "     if (e != null) {%n"
                             + "         %s.exceptionOccurred(e);%n"
                             + "     }%n"
                             + "})", responseHandlerName);
    }

    /**
     * For event streaming our future notification is a bit complicated. We create a different future that is not tied
     * to the lifecycle of the wire request. Successful completion of the future is signalled in
     * {@link EventStreamAsyncResponseTransformer}. Failure is notified via the normal future (the one returned by the client
     * handler).
     *
     * @param responseHandlerName Variable name of response handler customer passed in.
     * @return whenComplete to append to future.
     */
    private String eventStreamOutputWhenComplete(String responseHandlerName) {
        return String.format(".whenComplete((r, e) -> {%n"
                             + "     if (e != null) {%n"
                             + "         try {"
                             + "             %s.exceptionOccurred(e);%n"
                             + "         } finally {"
                             + "             future.completeExceptionally(e);"
                             + "         }"
                             + "     }%n"
                             + "})", responseHandlerName);
    }

    private ClassName getUnmarshallerType(OperationModel opModel) {
        return poetExtensions.getTransformClass(opModel.getReturnType().getReturnType() + "Unmarshaller");
    }

    /**
     * Gets the POJO response type for the operation.
     *
     * @param opModel Operation to get response type for.
     */
    private TypeName getPojoResponseType(OperationModel opModel) {
        return poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
    }

    @Override
    public Optional<MethodSpec> createErrorResponseHandler() {
        ClassName httpResponseHandler = ClassName.get(HttpResponseHandler.class);
        ClassName sdkBaseException = ClassName.get(AwsServiceException.class);
        TypeName responseHandlerOfException = ParameterizedTypeName.get(httpResponseHandler, sdkBaseException);

        return Optional.of(MethodSpec.methodBuilder("createErrorResponseHandler")
                                     .addParameter(AwsJsonProtocolFactory.class, "protocolFactory")
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

    private ClassName baseExceptionClassName(IntermediateModel model) {
        String exceptionPath = model.getSdkModeledExceptionBaseFqcn()
                                    .substring(0, model.getSdkModeledExceptionBaseFqcn().lastIndexOf("."));

        return ClassName.get(exceptionPath, model.getSdkModeledExceptionBaseClassName());
    }

    /**
     * Add responseHandlers for event streaming operations
     */
    private void responseHandlersForEventStreaming(OperationModel opModel, ClassName unmarshaller, TypeName pojoResponseType,
                                                   String protocolFactory, CodeBlock.Builder builder) {
        builder.add("\n\n$T<$T> responseHandler = new $T($L.createResponseHandler(new $T()" +
                    "                                    .withPayloadJson($L)" +
                    "                                    .withHasStreamingSuccessResponse($L), new $T()));",
                    HttpResponseHandler.class,
                    pojoResponseType,
                    AttachHttpMetadataResponseHandler.class,
                    protocolFactory,
                    JsonOperationMetadata.class,
                    !opModel.getHasBlobMemberAsPayload(),
                    opModel.hasStreamingOutput(),
                    unmarshaller);

        builder.add("\n\n$T<$T> voidResponseHandler = $L.createResponseHandler(new $T()" +
                    "                                   .withPayloadJson(false)" +
                    "                                   .withHasStreamingSuccessResponse(true), new $T());",
                    HttpResponseHandler.class,
                    SdkResponse.class,
                    protocolFactory,
                    JsonOperationMetadata.class,
                    VoidJsonUnmarshaller.class);

        ShapeModel eventStream = EventStreamUtils.getEventStreamInResponse(opModel.getOutputShape());
        ClassName eventStreamBaseClass = poetExtensions.getModelClassFromShape(eventStream);
        builder
            .add("\n\n$T<$T> eventResponseHandler = $L.createResponseHandler(new $T()" +
                 "                                   .withPayloadJson($L)" +
                 "                                   .withHasStreamingSuccessResponse($L), "
                 + "$T.builder()",
                 HttpResponseHandler.class,
                 WildcardTypeName.subtypeOf(eventStreamBaseClass),
                 protocolFactory,
                 JsonOperationMetadata.class,
                 true,
                 false,
                 ClassName.get(EventStreamTaggedUnionJsonUnmarshaller.class));
        EventStreamUtils.getEvents(eventStream)
                        .forEach(shape -> {
                            String unmarshallerClassName = shape.getVariable().getVariableType() + "Unmarshaller";
                            builder.add(".putUnmarshaller(\"$L\", $T.getInstance())\n",
                                        shape.getC2jName(),
                                        poetExtensions.getTransformClass(unmarshallerClassName));
                        });
        builder.add(".defaultUnmarshaller((in) -> $T.UNKNOWN)\n"
                    + ".build());\n", eventStreamBaseClass);
    }

    private String protocolFactoryLiteral(OperationModel opModel) {
        // TODO Fix once below kinesis TODO is done
        if (opModel.hasEventStreamInput() && opModel.hasEventStreamOutput()) {
            return "protocolFactory";

            // TODO remove this once kinesis supports CBOR for event streaming
        } else if (opModel.hasEventStreamOutput()) {
            return "jsonProtocolFactory";
        }

        return "protocolFactory";
    }

    private boolean isRestJson(IntermediateModel model) {
        return Protocol.REST_JSON.equals(model.getMetadata().getProtocol());
    }
}
