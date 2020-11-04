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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.runtime.transform.AsyncStreamingRequestMarshaller;
import software.amazon.awssdk.core.runtime.transform.StreamingRequestMarshaller;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.utils.StringUtils;

public interface ProtocolSpec {

    FieldSpec protocolFactory(IntermediateModel model);

    MethodSpec initProtocolFactory(IntermediateModel model);

    CodeBlock responseHandler(IntermediateModel model, OperationModel opModel);

    Optional<CodeBlock> errorResponseHandler(OperationModel opModel);

    CodeBlock executionHandler(OperationModel opModel);

    /**
     * Execution handler invocation only differs for protocols that support streaming outputs (REST-JSON, REST-XML).
     */
    default CodeBlock asyncExecutionHandler(IntermediateModel intermediateModel, OperationModel opModel) {
        return executionHandler(opModel);
    }

    default Class<? extends SyncClientHandler> getClientHandlerClass() {
        return AwsSyncClientHandler.class;
    }

    Optional<MethodSpec> createErrorResponseHandler();

    default List<MethodSpec> additionalMethods() {
        return new ArrayList<>();
    }

    default List<CodeBlock> registerModeledExceptions(IntermediateModel model, PoetExtensions poetExtensions) {
        return model.getShapes().values().stream()
                    .filter(s -> s.getShapeType() == ShapeType.Exception)
                    .map(e -> CodeBlock.builder()
                                       .add(".registerModeledException($T.builder().errorCode($S)"
                                            + ".exceptionBuilderSupplier($T::builder)$L.build())",
                                            ExceptionMetadata.class,
                                            e.getErrorCode(),
                                            poetExtensions.getModelClass(e.getShapeName()),
                                            populateHttpStatusCode(e))
                                       .build())
                    .collect(Collectors.toList());
    }

    default String populateHttpStatusCode(ShapeModel shapeModel) {
        return shapeModel.getHttpStatusCode() != null
               ? String.format(".httpStatusCode(%d)", shapeModel.getHttpStatusCode()) : "";
    }

    default String hostPrefixExpression(OperationModel opModel) {
        return opModel.getEndpointTrait() != null && !StringUtils.isEmpty(opModel.getEndpointTrait().getHostPrefix())
               ? ".hostPrefixExpression(resolvedHostExpression)\n"
               : "";
    }

    default String discoveredEndpoint(OperationModel opModel) {
        return opModel.getEndpointDiscovery() != null
               ? ".discoveredEndpoint(cachedEndpoint)\n"
               : "";
    }

    /**
     * For sync streaming operations, wrap request marshaller in {@link StreamingRequestMarshaller} class.
     */
    default CodeBlock syncStreamingMarshaller(IntermediateModel model, OperationModel opModel, ClassName marshaller) {
        return streamingMarshallerCode(model, opModel, marshaller, "protocolFactory", false);
    }

    default CodeBlock asyncMarshaller(IntermediateModel model, OperationModel opModel, ClassName marshaller,
                                     String protocolFactory) {
        if (opModel.hasStreamingInput()) {
            return streamingMarshallerCode(model, opModel, marshaller, protocolFactory, true);
        } else {
            return CodeBlock.builder().add("new $T($L)", marshaller, protocolFactory).build();
        }
    }

    default CodeBlock streamingMarshallerCode(IntermediateModel model, OperationModel opModel, ClassName marshaller,
                                              String protocolFactory, boolean isAsync) {
        CodeBlock.Builder builder = CodeBlock
            .builder()
            .add("$T.builder().delegateMarshaller(new $T($L))",
                 isAsync ? AsyncStreamingRequestMarshaller.class : StreamingRequestMarshaller.class,
                 marshaller,
                 protocolFactory)
            .add(".$L(requestBody)", isAsync ? "asyncRequestBody" : "requestBody");

        if (opModel.hasRequiresLengthInInput()) {
            builder.add(".requiresLength(true)");
        }

        if (AuthType.V4_UNSIGNED_BODY.equals(opModel.getAuthType())) {
            builder.add(".transferEncoding(true)");
        }

        if (model.getMetadata().supportsH2()) {
            builder.add(".useHttp2(true)");
        }

        builder.add(".build()");

        return builder.build();
    }


    /**
     * Need to notify the response handler/response transformer if the future is completed exceptionally.
     *
     * @param responseHandlerName Variable name of response handler customer passed in.
     * @return whenComplete to append to future.
     */
    default String streamingOutputWhenComplete(String responseHandlerName) {
        return String.format(".whenComplete((r, e) -> {%n"
                             + "     if (e != null) {%n"
                             + "         runAndLogError(log, \"Exception thrown in exceptionOccurred callback, ignoring\", () "
                             + "-> %s.exceptionOccurred(e));%n"
                             + "     }%n"
                             + "%s"
                             + "})", responseHandlerName, publishMetrics());

    }

    default TypeName executeFutureValueType(OperationModel opModel, PoetExtensions poetExtensions) {
        if (opModel.hasEventStreamOutput()) {
            return ClassName.get(Void.class);
        } else if (opModel.hasStreamingOutput()) {
            return TypeVariableName.get("ReturnT");
        } else {
            return getPojoResponseType(opModel, poetExtensions);
        }
    }

    /**
     * Gets the POJO response type for the operation.
     *
     * @param opModel Operation to get response type for.
     */
    default TypeName getPojoResponseType(OperationModel opModel, PoetExtensions poetExtensions) {
        return poetExtensions.getModelClass(opModel.getReturnType().getReturnType());
    }

    default String publishMetricsWhenComplete() {
        return String.format(".whenComplete((r, e) -> {%n"
                             + "%s%n"
                             + "})", publishMetrics());
    }

    default String publishMetrics() {
        return "metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));";
    }
}
