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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.w3c.dom.Node;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.http.response.DefaultErrorResponseHandler;
import software.amazon.awssdk.awscore.http.response.StaxResponseHandler;
import software.amazon.awssdk.awscore.protocol.xml.StandardErrorUnmarshaller;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.utils.StringUtils;

public class QueryXmlProtocolSpec implements ProtocolSpec {

    private final PoetExtensions poetExtensions;
    private final TypeName unmarshallerType = ParameterizedTypeName.get(Unmarshaller.class,
                                                                        AwsServiceException.class,
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

    private ClassName getErrorUnmarshallerClass(IntermediateModel model) {
        return StringUtils.isNotBlank(model.getExceptionUnmarshallerImpl()) ?
               PoetUtils.classNameFromFqcn(model.getExceptionUnmarshallerImpl()) :
               ClassName.get(StandardErrorUnmarshaller.class);
    }

    @Override
    public CodeBlock responseHandler(OperationModel opModel) {
        ClassName unmarshaller = poetExtensions.getTransformClass(opModel.getReturnType().getReturnType() + "Unmarshaller");
        ClassName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        if (opModel.hasStreamingOutput()) {
            return CodeBlock.builder()
                            .addStatement("\n\n$T<$T> responseHandler = $T.createStreamingResponseHandler(new $T())",
                                          HttpResponseHandler.class,
                                          responseType,
                                          StaxResponseHandler.class,
                                          unmarshaller)
                            .build();
        }
        return CodeBlock.builder()
                        .addStatement("\n\n$T<$T> responseHandler = new $T<$T>(new $T())",
                                      StaxResponseHandler.class,
                                      responseType,
                                      StaxResponseHandler.class,
                                      responseType,
                                      unmarshaller)
                        .build();
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
            return codeBlock.add(".withMarshaller(new $T(new $T(), requestBody)));",
                                 ParameterizedTypeName.get(ClassName.get(StreamingRequestMarshaller.class), requestType),
                                 marshaller)
                            .build();
        }
        return codeBlock.add(".withMarshaller(new $T()) $L);", marshaller,
                             opModel.hasStreamingOutput() ? ", responseTransformer" : "").build();
    }

    @Override
    public CodeBlock asyncExecutionHandler(OperationModel opModel) {
        ClassName pojoResponseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        String asyncRequestBody = opModel.hasStreamingInput() ? ".withAsyncRequestBody(requestBody)"
                : "";
        return CodeBlock.builder().add("\n\nreturn clientHandler.execute(new $T<$T, $T>()\n" +
                                       ".withMarshaller(new $T())" +
                                       ".withResponseHandler(responseHandler)" +
                                       ".withErrorResponseHandler($N)\n" +
                                       asyncRequestBody +
                                       ".withInput($L) $L)$L;",
                                       ClientExecutionParams.class,
                                       requestType,
                                       pojoResponseType,
                                       marshaller,
                                       "errorResponseHandler",
                                       opModel.getInput().getVariableName(),
                                       opModel.hasStreamingOutput() ? ", asyncResponseTransformer" : "",
                                       opModel.hasStreamingOutput() ? ".whenComplete((r, e) -> {\n"
                                                                      + "    if (e != null) {\n"
                                                                      + "        asyncResponseTransformer.exceptionOccurred(e);\n"
                                                                      + "    }\n"
                                                                      + "})" : "")
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
