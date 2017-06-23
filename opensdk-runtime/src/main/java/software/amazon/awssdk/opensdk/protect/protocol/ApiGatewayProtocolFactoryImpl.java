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

package software.amazon.awssdk.opensdk.protect.protocol;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.opensdk.BaseResult;
import software.amazon.awssdk.opensdk.internal.BaseException;
import software.amazon.awssdk.opensdk.internal.protocol.ApiGatewayErrorResponseHandler;
import software.amazon.awssdk.opensdk.internal.protocol.ApiGatewayErrorUnmarshaller;
import software.amazon.awssdk.opensdk.internal.protocol.ApiGatewayResponseHandler;
import software.amazon.awssdk.protocol.OperationInfo;
import software.amazon.awssdk.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.protocol.json.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocol.json.SdkStructuredJsonFactory;
import software.amazon.awssdk.protocol.json.SdkStructuredPlainJsonFactory;
import software.amazon.awssdk.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.runtime.http.response.JsonResponseHandler;
import software.amazon.awssdk.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.runtime.transform.Unmarshaller;

/**
 * Protocol factory implementation for API Gateway clients.
 */
public final class ApiGatewayProtocolFactoryImpl {

    private static final SdkStructuredJsonFactory JSON_FACTORY = SdkStructuredPlainJsonFactory.SDK_JSON_FACTORY;
    private static final String CONTENT_TYPE = "application/json";

    private final JsonClientMetadata metadata;

    public ApiGatewayProtocolFactoryImpl(JsonClientMetadata metadata) {
        this.metadata = metadata;
    }

    public <T> ProtocolRequestMarshaller<T> createProtocolMarshaller(OperationInfo operationInfo, T origRequest) {
        return JsonProtocolMarshallerBuilder.<T>standard()
                .jsonGenerator(operationInfo.hasPayloadMembers() ? createGenerator() : StructuredJsonGenerator.NO_OP)
                .contentType(getContentType())
                .operationInfo(operationInfo)
                .originalRequest(origRequest)
                .sendExplicitNullForPayload(true)
                .build();
    }

    private StructuredJsonGenerator createGenerator() {
        return JSON_FACTORY.createWriter(CONTENT_TYPE);
    }

    public String getContentType() {
        return CONTENT_TYPE;
    }

    /**
     * Creates a response handler to be used for handling a successful response.
     *
     * @param operationMetadata Additional context information about an operation to create the appropriate response handler.
     */
    public <T extends BaseResult> HttpResponseHandler<T> createResponseHandler(
            JsonOperationMetadata operationMetadata,
            Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller) {
        final JsonResponseHandler<T> responseHandler = JSON_FACTORY
                .createResponseHandler(operationMetadata, responseUnmarshaller);
        return new ApiGatewayResponseHandler<>(responseHandler);
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    public HttpResponseHandler<SdkBaseException> createErrorResponseHandler(
            JsonErrorResponseMetadata errorResponsMetadata) {
        return new ApiGatewayErrorResponseHandler(
                createErrorUnmarshallers(errorResponsMetadata.getErrorShapes().stream()),
                SdkStructuredPlainJsonFactory.JSON_FACTORY);
    }

    @SuppressWarnings("unchecked")
    private List<ApiGatewayErrorUnmarshaller> createErrorUnmarshallers(
            Stream<JsonErrorShapeMetadata> errorShapeMetadata) {
        final List<ApiGatewayErrorUnmarshaller> errorUnmarshallers = errorShapeMetadata
                .map(this::createErrorUnmarshaller).collect(Collectors.toList());
        // All unmodeled/unknown exceptions are unmarshalled into the service specific base
        // exception class.
        errorUnmarshallers.add(new ApiGatewayErrorUnmarshaller(
                (Class<? extends BaseException>) metadata.getBaseServiceExceptionClass(),
                Optional.empty()));
        return errorUnmarshallers;
    }

    @SuppressWarnings("unchecked")
    private ApiGatewayErrorUnmarshaller createErrorUnmarshaller(JsonErrorShapeMetadata errorShape) {
        return new ApiGatewayErrorUnmarshaller(
                (Class<? extends BaseException>) errorShape.getModeledClass(),
                Optional.of(errorShape.getHttpStatusCode()));
    }
}
