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
import com.squareup.javapoet.WildcardTypeName;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.awscore.eventstream.EventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.eventstream.EventStreamTaggedUnionPojoSupplier;
import software.amazon.awssdk.awscore.eventstream.RestEventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.codegen.model.config.customization.S3ArnableFieldConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.client.traits.HttpChecksumRequiredTrait;
import software.amazon.awssdk.codegen.poet.client.traits.HttpChecksumTrait;
import software.amazon.awssdk.codegen.poet.client.traits.NoneAuthTypeRequestTrait;
import software.amazon.awssdk.codegen.poet.client.traits.RequestCompressionTrait;
import software.amazon.awssdk.codegen.poet.eventstream.EventStreamUtils;
import software.amazon.awssdk.codegen.poet.model.EventStreamSpecHelper;
import software.amazon.awssdk.core.SdkPojoBuilder;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.protocols.xml.XmlOperationMetadata;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public final class XmlProtocolSpec extends QueryProtocolSpec {

    private final IntermediateModel model;

    public XmlProtocolSpec(IntermediateModel model,
                           PoetExtension poetExtensions) {
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

        if (opModel.hasEventStreamOutput()) {
            return CodeBlock.builder()
                            .add(eventStreamResponseHandlers(opModel, responseType))
                            .build();
        }

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
                                               .add(credentialType(opModel, model))
                                               .add(".withInput($L)", opModel.getInput().getVariableName())
                                               .add(HttpChecksumRequiredTrait.putHttpChecksumAttribute(opModel))
                                               .add(HttpChecksumTrait.create(opModel))
                                               .add(NoneAuthTypeRequestTrait.create(opModel))
                                               .add(RequestCompressionTrait.create(opModel, model));


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
        String eventStreamTransformFutureName = "eventStreamTransformFuture";

        CodeBlock.Builder builder = CodeBlock.builder();

        if (opModel.hasEventStreamOutput()) {
            builder.add(eventStreamResponseTransformers(opModel, eventStreamTransformFutureName));
        }

        TypeName executeFutureValueType = executeFutureValueType(opModel, poetExtensions);
        String executionResponseTransformerName = "asyncResponseTransformer";

        if (opModel.hasEventStreamOutput()) {
            executionResponseTransformerName = "restAsyncResponseTransformer";
        }

        builder.add("\n\n$T<$T> executeFuture = clientHandler.execute(new $T<$T, $T>()\n",
                    CompletableFuture.class, executeFutureValueType,
                    ClientExecutionParams.class, requestType, pojoResponseType)
               .add(".withOperationName(\"$N\")\n", opModel.getOperationName())
               .add(".withMarshaller($L)\n", asyncMarshaller(intermediateModel, opModel, marshaller, "protocolFactory"));

        if (opModel.hasEventStreamOutput()) {
            builder.add(".withResponseHandler(responseHandler)")
                   .add(".withErrorResponseHandler(errorResponseHandler)");
        } else {
            builder.add(".withCombinedResponseHandler(responseHandler)");
        }

        builder.add(hostPrefixExpression(opModel))
               .add(credentialType(opModel, model))
               .add(".withMetricCollector(apiCallMetricCollector)\n")
               .add(asyncRequestBody(opModel))
               .add(HttpChecksumRequiredTrait.putHttpChecksumAttribute(opModel))
               .add(HttpChecksumTrait.create(opModel))
               .add(NoneAuthTypeRequestTrait.create(opModel))
               .add(RequestCompressionTrait.create(opModel, model));

        s3ArnableFields(opModel, model).ifPresent(builder::add);

        builder.add(".withInput($L)", opModel.getInput().getVariableName());
        if (opModel.hasEventStreamOutput()) {
            builder.add(", $N", executionResponseTransformerName);
        }
        builder.addStatement(")");

        String whenCompleteFutureName = "whenCompleteFuture";
        builder.addStatement("$T $N = null", ParameterizedTypeName.get(ClassName.get(CompletableFuture.class),
                executeFutureValueType), whenCompleteFutureName);

        if (opModel.hasEventStreamOutput()) {
            builder.addStatement("$N = executeFuture$L", whenCompleteFutureName,
                    whenCompleteBlock(opModel, "asyncResponseHandler",
                                      eventStreamTransformFutureName));
        } else {
            builder.addStatement("$N = executeFuture$L", whenCompleteFutureName, publishMetricsWhenComplete());
        }

        builder.addStatement("$T.forwardExceptionTo($N, executeFuture)", CompletableFutureUtils.class,
                whenCompleteFutureName);

        if (opModel.hasEventStreamOutput()) {
            builder.addStatement("return $T.forwardExceptionTo($N, executeFuture)", CompletableFutureUtils.class,
                                 eventStreamTransformFutureName);
        } else {
            builder.addStatement("return $N", whenCompleteFutureName);
        }

        return builder.build();
    }

    private String asyncRequestBody(OperationModel opModel) {
        return opModel.hasStreamingInput() ? ".withAsyncRequestBody(requestBody)" : "";
    }

    private CodeBlock asyncStreamingExecutionHandler(IntermediateModel intermediateModel, OperationModel opModel) {
        return super.asyncExecutionHandler(intermediateModel, opModel);
    }

    private CodeBlock eventStreamResponseHandlers(OperationModel opModel, TypeName pojoResponseType) {
        CodeBlock streamResponseOpMd = CodeBlock.builder()
                                                .add("$T.builder()", XmlOperationMetadata.class)
                                                .add(".hasStreamingSuccessResponse(true)")
                                                .add(".build()")
                                                .build();


        CodeBlock.Builder builder = CodeBlock.builder();

        // Response handler for handling the initial response from the operation. Note, this does not handle the event stream
        // messages, that is the job of "eventResponseHandler" below
        builder.addStatement("$T<$T> responseHandler = protocolFactory.createResponseHandler($T::builder, $L)",
                             HttpResponseHandler.class,
                             pojoResponseType,
                             pojoResponseType,
                             streamResponseOpMd);

        // Response handler responsible for errors for the API call itself, as well as errors sent over the event stream
        builder.addStatement("$T errorResponseHandler = protocolFactory"
                             + ".createErrorResponseHandler()", ParameterizedTypeName.get(HttpResponseHandler.class,
                                                                                          AwsServiceException.class));


        ShapeModel eventStreamShape = EventStreamUtils.getEventStreamInResponse(opModel.getOutputShape());
        ClassName eventStream = poetExtensions.getModelClassFromShape(eventStreamShape);
        EventStreamSpecHelper eventStreamSpecHelper = new EventStreamSpecHelper(eventStreamShape, intermediateModel);

        CodeBlock.Builder supplierBuilder = CodeBlock.builder()
                                                     .add("$T.builder()", EventStreamTaggedUnionPojoSupplier.class);
        EventStreamUtils.getEvents(eventStreamShape).forEach(m -> {
            String builderName = eventStreamSpecHelper.eventBuilderMethodName(m);
            supplierBuilder.add(".putSdkPojoSupplier($S, $T::$N)", m.getName(), eventStream, builderName);
        });
        supplierBuilder.add(".defaultSdkPojoSupplier(() -> new $T($T.UNKNOWN))", SdkPojoBuilder.class, eventStream);
        CodeBlock supplierCodeBlock = supplierBuilder.add(".build()").build();

        CodeBlock nonStreamingOpMd = CodeBlock.builder()
                                              .add("$T.builder()", XmlOperationMetadata.class)
                                              .add(".hasStreamingSuccessResponse(false)")
                                              .add(".build()")
                                              .build();

        // The response handler responsible for unmarshalling each event
        builder.addStatement("$T eventResponseHandler = protocolFactory.createResponseHandler($L, $L)",
                             ParameterizedTypeName.get(ClassName.get(HttpResponseHandler.class),
                                                       WildcardTypeName.subtypeOf(eventStream)),
                             supplierCodeBlock,
                             nonStreamingOpMd);


        return builder.build();
    }

    private CodeBlock eventStreamResponseTransformers(OperationModel opModel, String eventTransformerFutureName) {
        ShapeModel shapeModel = EventStreamUtils.getEventStreamInResponse(opModel.getOutputShape());
        ClassName pojoResponseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
        ClassName eventStreamBaseClass = poetExtensions.getModelClassFromShape(shapeModel);

        CodeBlock.Builder builder = CodeBlock.builder();

        ParameterizedTypeName transformerType = ParameterizedTypeName.get(
            ClassName.get(EventStreamAsyncResponseTransformer.class),
            pojoResponseType,
            eventStreamBaseClass);

        builder.addStatement("$1T<$2T> $3N = new $1T<>()", ClassName.get(CompletableFuture.class),
                             ClassName.get(Void.class), eventTransformerFutureName)
               .add("$T asyncResponseTransformer = $T.<$T, $T>builder()\n",
                    transformerType, ClassName.get(EventStreamAsyncResponseTransformer.class), pojoResponseType,
                    eventStreamBaseClass)
               .add(".eventStreamResponseHandler(asyncResponseHandler)\n")
               .add(".eventResponseHandler(eventResponseHandler)\n")
               .add(".initialResponseHandler(responseHandler)\n")
               .add(".exceptionResponseHandler(errorResponseHandler)\n")
               .add(".future($N)\n", eventTransformerFutureName)
               .add(".executor(executor)\n")
               .add(".serviceName(serviceName())\n")
               .addStatement(".build()");

        ParameterizedTypeName restTransformType =
            ParameterizedTypeName.get(ClassName.get(RestEventStreamAsyncResponseTransformer.class), pojoResponseType,
                                      eventStreamBaseClass);

        // Wrap the event transformer with this so that the caller's response handler's onResponse() method is invoked. See
        // docs for RestEventStreamAsyncResponseTransformer for more info on why it's needed
        builder.addStatement("$T restAsyncResponseTransformer = $T.<$T, $T>builder()\n"
                             + ".eventStreamAsyncResponseTransformer(asyncResponseTransformer)\n"
                             + ".eventStreamResponseHandler(asyncResponseHandler)\n"
                             + ".build()", restTransformType, RestEventStreamAsyncResponseTransformer.class,
                             pojoResponseType, eventStreamBaseClass);

        return builder.build();
    }

    private CodeBlock whenCompleteBlock(OperationModel operationModel, String responseHandlerName,
                                        String eventTransformerFutureName) {
        CodeBlock.Builder whenComplete = CodeBlock.builder()
                                                  .add(".whenComplete((r, e) -> ")
                                                  .beginControlFlow("")
                                                  .beginControlFlow("if (e != null)")
                                                  .add("runAndLogError(log, $S, () -> $N.exceptionOccurred(e));",
                                                       "Exception thrown in exceptionOccurred callback, ignoring",
                                                       responseHandlerName);

        if (operationModel.hasEventStreamOutput()) {
            whenComplete.add("$N.completeExceptionally(e);", eventTransformerFutureName);
        }

        whenComplete.endControlFlow()
                    .add(publishMetrics())
                    .endControlFlow()
                    .add(")")
                    .build();

        return whenComplete.build();
    }
}
