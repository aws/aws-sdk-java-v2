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

package software.amazon.awssdk.codegen.poet.client.specs;

import static software.amazon.awssdk.codegen.poet.client.AsyncClientInterface.STREAMING_TYPE_VARIABLE;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.w3c.dom.Node;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.http.DefaultErrorResponseHandler;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.StaxResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.runtime.transform.StandardErrorUnmarshaller;
import software.amazon.awssdk.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.utils.StringUtils;

public class QueryXmlProtocolSpec implements ProtocolSpec {

    private final PoetExtensions poetExtensions;
    private final TypeName unmarshallerType = ParameterizedTypeName.get(Unmarshaller.class,
                                                                        AmazonServiceException.class,
                                                                        Node.class);
    private final TypeName listOfUnmarshallersType = ParameterizedTypeName.get(ClassName.get("java.util", "List"),
                                                                               unmarshallerType);

    public QueryXmlProtocolSpec(PoetExtensions poetExtensions) {
        this.poetExtensions = poetExtensions;
    }

    @Override
    public FieldSpec protocolFactory(IntermediateModel model) {

        return FieldSpec.builder(listOfUnmarshallersType, "exceptionUnmarshallers")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build();
    }

    @Override
    public MethodSpec initProtocolFactory(IntermediateModel model) {
        MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("init")
                                                  .returns(listOfUnmarshallersType)
                                                  .addModifiers(Modifier.PRIVATE);

        methodSpec.addStatement("$T<$T> unmarshallers = new $T<>()", List.class, unmarshallerType, ArrayList.class);
        errorUnmarshallers(model).forEach(methodSpec::addCode);
        methodSpec.addCode(CodeBlock.builder().add("unmarshallers.add(new $T($T.class));",
                                                   getErrorUnmarshallerClass(model),
                                                   poetExtensions.getModelClass(model.getSdkModeledExceptionBaseClassName()))
                                    .build());
        methodSpec.addStatement("return $N", "unmarshallers");

        return methodSpec.build();
    }

    private Class<?> getErrorUnmarshallerClass(IntermediateModel model) {
        try {
            return StringUtils.isNotBlank(model.getExceptionUnmarshallerImpl()) ?
                    Class.forName(model.getExceptionUnmarshallerImpl()) :
                    StandardErrorUnmarshaller.class;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CodeBlock responseHandler(OperationModel opModel) {
        ClassName unmarshaller = poetExtensions.getTransformClass(opModel.getReturnType().getReturnType() + "Unmarshaller");
        ClassName returnType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());


        if (opModel.hasStreamingOutput()) {
            final ParameterizedTypeName responseHandlerType = ParameterizedTypeName
                    .get(ClassName.get(HttpResponseHandler.class), STREAMING_TYPE_VARIABLE);
            return CodeBlock.builder()
                            .addStatement("\n\n$T responseHandler = $T.createStreamingResponseHandler(" +
                                          "new $T(), streamingHandler)",
                                          responseHandlerType,
                                          StaxResponseHandler.class,
                                          unmarshaller)
                            .build();
        }
        return CodeBlock.builder()
                        .addStatement("\n\n$T<$T> responseHandler = new $T<$T>(new $T())",
                                      StaxResponseHandler.class,
                                      returnType,
                                      StaxResponseHandler.class,
                                      returnType,
                                      unmarshaller)
                        .build();
    }

    @Override
    public CodeBlock asyncResponseHandler(OperationModel opModel) {
        if (opModel.hasStreamingOutput()) {
            ClassName unmarshaller = poetExtensions.getTransformClass(opModel.getReturnType().getReturnType() + "Unmarshaller");
            return CodeBlock.builder()
                            .addStatement("$T<$T> responseHandler = $T.createStreamingAsyncResponseHandler(" +
                                          "new $T(), asyncResponseHandler)",
                                          SdkHttpResponseHandler.class,
                                          STREAMING_TYPE_VARIABLE,
                                          StaxResponseHandler.class,
                                          unmarshaller)
                            .build();
        } else {
            return responseHandler(opModel);
        }
    }

    @Override
    public CodeBlock errorResponseHandler(OperationModel opModel) {
        return CodeBlock.builder().add("\n\n$T errorResponseHandler = new $T($N);",
                                       DefaultErrorResponseHandler.class,
                                       DefaultErrorResponseHandler.class,
                                       "exceptionUnmarshallers")
                        .build();
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        TypeName returnType = opModel.hasStreamingOutput() ? STREAMING_TYPE_VARIABLE :
                poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
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
                     returnType,
                     "responseHandler",
                     "errorResponseHandler",
                     opModel.getInput().getVariableName());
        if (opModel.hasStreamingInput()) {
            return codeBlock.add(".withMarshaller(new $T(new $T(), requestBody)));",
                                 ParameterizedTypeName.get(ClassName.get(StreamingRequestMarshaller.class), requestType),
                                 marshaller)
                            .build();
        }
        return codeBlock.add(".withMarshaller(new $T()));", marshaller).build();
    }

    @Override
    public CodeBlock asyncExecutionHandler(OperationModel opModel) {
        ClassName pojoResponseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        String asyncRequestProvider = opModel.hasStreamingInput() ? ".withAsyncRequestProvider(requestProvider)"
                : "";
        TypeName returnType = opModel.hasStreamingOutput() ? TypeVariableName.get("ReturnT") : pojoResponseType;
        String responseHandler = opModel.hasStreamingOutput() ? ".withAsyncResponseHandler(responseHandler)"
                : ".withResponseHandler(responseHandler)";
        return CodeBlock.builder().add("\n\nreturn clientHandler.execute(new $T<$T, $T>()\n" +
                                       ".withMarshaller(new $T())" +
                                       responseHandler +
                                       ".withErrorResponseHandler($N)\n" +
                                       asyncRequestProvider +
                                       ".withInput($L));",
                                       ClientExecutionParams.class,
                                       requestType,
                                       returnType,
                                       marshaller,
                                       "errorResponseHandler",
                                       opModel.getInput().getVariableName())
                        .build();
    }

    @Override
    public Optional<MethodSpec> createErrorResponseHandler() {
        return Optional.empty();
    }

    @Override
    public List<CodeBlock> errorUnmarshallers(IntermediateModel model) {
        List<ShapeModel> exceptions = model.getShapes()
                                           .values()
                                           .stream()
                                           .filter(s -> s.getType().equals("Exception"))
                                           .collect(Collectors.toList());

        return exceptions.stream().map(s -> {
            ClassName exceptionClass = poetExtensions.getTransformClass(s.getShapeName() + "Unmarshaller");
            return CodeBlock.builder()
                            .add("unmarshallers.add(new $T());", exceptionClass).build();
        }).collect(Collectors.toList());
    }
}
