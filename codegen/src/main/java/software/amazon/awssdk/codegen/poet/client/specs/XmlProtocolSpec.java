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

package software.amazon.awssdk.codegen.poet.client.specs;

import static software.amazon.awssdk.codegen.poet.PoetUtils.classNameFromFqcn;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.codegen.model.config.customization.S3ArnableFieldConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.codegen.poet.client.traits.HttpChecksumRequiredTrait;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.protocols.xml.XmlOperationMetadata;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public final class XmlProtocolSpec extends QueryProtocolSpec {

    private final IntermediateModel model;

    public XmlProtocolSpec(IntermediateModel model,
                           PoetExtensions poetExtensions) {
        super(model, poetExtensions);
        this.model = model;
    }

    @Override
    protected Class<?> protocolFactoryClass() {
        if (model.getCustomizationConfig().getCustomProtocolFactoryFqcn() != null) {
            try {
                return Class.forName(model.getCustomizationConfig().getCustomProtocolFactoryFqcn());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not find custom protocol factory class", e);
            }
        }
        return AwsXmlProtocolFactory.class;
    }

    @Override
    public CodeBlock responseHandler(IntermediateModel model,
                                     OperationModel opModel) {

        if (opModel.hasStreamingOutput()) {
            return streamingResponseHandler(opModel);
        }

        ClassName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        TypeName handlerType = ParameterizedTypeName.get(
            ClassName.get(HttpResponseHandler.class),
            ParameterizedTypeName.get(ClassName.get(software.amazon.awssdk.core.Response.class), responseType));

        return CodeBlock.builder()
                        .addStatement("\n\n$T responseHandler = protocolFactory.createCombinedResponseHandler($T::builder, "
                                      + "new $T().withHasStreamingSuccessResponse($L))",
                                      handlerType, responseType, XmlOperationMetadata.class, opModel.hasStreamingOutput())
                        .build();
    }

    private CodeBlock streamingResponseHandler(OperationModel opModel) {
        ClassName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        return CodeBlock.builder()
                        .addStatement("\n\n$T<$T> responseHandler = protocolFactory.createResponseHandler($T::builder, "
                                      + "new $T().withHasStreamingSuccessResponse($L))",
                                      HttpResponseHandler.class, responseType, responseType, XmlOperationMetadata.class,
                                      opModel.hasStreamingOutput())
                        .build();
    }

    @Override
    public Optional<CodeBlock> errorResponseHandler(OperationModel opModel) {
        return opModel.hasStreamingOutput() ? streamingErrorResponseHandler(opModel) : Optional.empty();
    }

    private Optional<CodeBlock> streamingErrorResponseHandler(OperationModel opModel) {
        return super.errorResponseHandler(opModel);
    }

    @Override
    public CodeBlock executionHandler(OperationModel opModel) {
        if (opModel.hasStreamingOutput()) {
            return streamingExecutionHandler(opModel);
        }

        TypeName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                                               .add("\n\nreturn clientHandler.execute(new $T<$T, $T>()\n",
                                                    ClientExecutionParams.class, requestType, responseType)
                                               .add(".withOperationName($S)\n", opModel.getOperationName())
                                               .add(".withCombinedResponseHandler(responseHandler)\n")
                                               .add(".withMetricCollector(apiCallMetricCollector)\n" +
                                                    hostPrefixExpression(opModel) +
                                                    discoveredEndpoint(opModel))
                                               .add(".withInput($L)", opModel.getInput().getVariableName())
                                               .add(HttpChecksumRequiredTrait.putHttpChecksumAttribute(opModel));

        s3ArnableFields(opModel, model).ifPresent(codeBlock::add);

        if (opModel.hasStreamingInput()) {
            return codeBlock.add(".withRequestBody(requestBody)")
                            .add(".withMarshaller($L));", syncStreamingMarshaller(intermediateModel, opModel, marshaller))
                            .build();
        }
        return codeBlock.add(".withMarshaller(new $T(protocolFactory)) $L);", marshaller,
                             opModel.hasStreamingOutput() ? ", responseTransformer" : "").build();
    }

    private Optional<CodeBlock> s3ArnableFields(OperationModel opModel, IntermediateModel model) {
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        Map<String, S3ArnableFieldConfig> s3ArnableFields = model.getCustomizationConfig().getS3ArnableFields();
        String shapeName = opModel.getInputShape().getShapeName();
        if (s3ArnableFields != null && s3ArnableFields.containsKey(shapeName)) {
            S3ArnableFieldConfig s3ArnableField = s3ArnableFields.get(shapeName);
            codeBlock.add(".putExecutionAttribute($T.$N, $T.builder().arn(arn).build())",
                          classNameFromFqcn(s3ArnableField.getExecutionAttributeKeyFqcn()),
                          "S3_ARNABLE_FIELD",
                          classNameFromFqcn(s3ArnableField.getExecutionAttributeValueFqcn()));

            return Optional.of(codeBlock.build());
        }

        return Optional.empty();
    }

    private CodeBlock streamingExecutionHandler(OperationModel opModel) {
        return super.executionHandler(opModel);
    }

    @Override
    public CodeBlock asyncExecutionHandler(IntermediateModel intermediateModel, OperationModel opModel) {
        if (opModel.hasStreamingOutput()) {
            return asyncStreamingExecutionHandler(intermediateModel, opModel);
        }

        ClassName pojoResponseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName requestType = poetExtensions.getModelClass(opModel.getInput().getVariableType());
        ClassName marshaller = poetExtensions.getRequestTransformClass(opModel.getInputShape().getShapeName() + "Marshaller");

        TypeName executeFutureValueType = executeFutureValueType(opModel, poetExtensions);
        CodeBlock.Builder builder =
            CodeBlock.builder()
                     .add("\n\n$T<$T> executeFuture = clientHandler.execute(new $T<$T, $T>()\n",
                          CompletableFuture.class, executeFutureValueType,
                          ClientExecutionParams.class, requestType, pojoResponseType)
                     .add(".withOperationName(\"$N\")\n", opModel.getOperationName())
                     .add(".withMarshaller($L)\n", asyncMarshaller(intermediateModel, opModel, marshaller, "protocolFactory"))
                     .add(".withCombinedResponseHandler(responseHandler)\n")
                     .add(hostPrefixExpression(opModel))
                     .add(".withMetricCollector(apiCallMetricCollector)\n")
                     .add(asyncRequestBody(opModel))
                     .add(HttpChecksumRequiredTrait.putHttpChecksumAttribute(opModel));

        s3ArnableFields(opModel, model).ifPresent(builder::add);
        builder.add(".withInput($L) $L);", opModel.getInput().getVariableName(), opModel.hasStreamingOutput() ?
                                                                                 ", asyncResponseTransformer" : "");
        builder.addStatement("$T requestOverrideConfig = $L.overrideConfiguration().orElse(null)",
                             AwsRequestOverrideConfiguration.class, opModel.getInput().getVariableName());

        String whenCompleteFutureName = "whenCompleteFuture";
        builder.addStatement("$T $N = null", ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                executeFutureValueType), whenCompleteFutureName);
        if (opModel.hasStreamingOutput()) {
            builder.addStatement("$N = executeFuture$L", whenCompleteFutureName,
                    streamingOutputWhenComplete("asyncResponseTransformer"));
        } else {
            builder.addStatement("$N = executeFuture$L", whenCompleteFutureName, publishMetricsWhenComplete());
        }
        builder.addStatement("return $T.forwardExceptionTo($N, executeFuture)", CompletableFutureUtils.class,
                whenCompleteFutureName);
        return builder.build();
    }

    private String asyncRequestBody(OperationModel opModel) {
        return opModel.hasStreamingInput() ? ".withAsyncRequestBody(requestBody)" : "";
    }

    private CodeBlock asyncStreamingExecutionHandler(IntermediateModel intermediateModel, OperationModel opModel) {
        return super.asyncExecutionHandler(intermediateModel, opModel);
    }
}
