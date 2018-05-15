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

package software.amazon.awssdk.core.protocol.json;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.Protocol;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.runtime.http.response.JsonResponseHandler;
import software.amazon.awssdk.core.runtime.transform.JsonErrorUnmarshaller;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Factory to generate the various JSON protocol handlers and generators depending on the wire protocol to be used for
 * communicating with the AWS service.
 */
@ThreadSafe
@SdkProtectedApi
public class SdkJsonProtocolFactory {

    private final JsonClientMetadata metadata;

    private final List<JsonErrorUnmarshaller> errorUnmarshallers = new ArrayList<>();

    public SdkJsonProtocolFactory(JsonClientMetadata metadata) {
        this.metadata = metadata;
        createErrorUnmarshallers();
    }

    public <T extends SdkRequest> ProtocolRequestMarshaller<T> createProtocolMarshaller(
        OperationInfo operationInfo, T origRequest) {
        return JsonProtocolMarshallerBuilder.<T>standard()
            .jsonGenerator(createGenerator(operationInfo))
            .contentType(getContentType())
            .operationInfo(operationInfo)
            .originalRequest(origRequest)
            .sendExplicitNullForPayload(false)
            .build();
    }

    private StructuredJsonGenerator createGenerator(OperationInfo operationInfo) {
        if (operationInfo.hasPayloadMembers() || operationInfo.protocol() == Protocol.AWS_JSON) {
            return createGenerator();
        } else {
            return StructuredJsonGenerator.NO_OP;
        }
    }

    @SdkTestInternalApi
    StructuredJsonGenerator createGenerator() {
        return getSdkFactory().createWriter(getContentType());
    }

    @SdkTestInternalApi
    String getContentType() {
        return getContentTypeResolver().resolveContentType(metadata);
    }

    /**
     * Returns the response handler to be used for handling a successful response.
     *
     * @param operationMetadata Additional context information about an operation to create the appropriate response handler.
     */
    public <T> JsonResponseHandler<T> createResponseHandler(
        JsonOperationMetadata operationMetadata,
        Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller) {
        return getSdkFactory().createResponseHandler(operationMetadata, responseUnmarshaller);
    }

    public JsonUnmarshallerContext createJsonUnmarshallerContext(HttpResponse httpResponse) {
        return getSdkFactory().createJsonUnmarshallerContext(httpResponse);
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    public HttpResponseHandler<SdkServiceException> createErrorResponseHandler(
        JsonErrorResponseMetadata errorResponseMetadata) {
        return getSdkFactory().createErrorResponseHandler(errorUnmarshallers, errorResponseMetadata
            .getCustomErrorCodeFieldName());
    }

    @SuppressWarnings("unchecked")
    private void createErrorUnmarshallers() {
        for (JsonErrorShapeMetadata errorMetadata : metadata.getErrorShapeMetadata()) {
            errorUnmarshallers.add(new JsonErrorUnmarshaller(
                (Class<? extends SdkServiceException>) errorMetadata.getModeledClass(),
                errorMetadata.getErrorCode()));

        }
        errorUnmarshallers.add(new JsonErrorUnmarshaller(
            (Class<? extends SdkServiceException>) metadata.getBaseServiceExceptionClass(),
            null));
    }

    /**
     * @return Instance of {@link SdkStructuredJsonFactory} to use in creating handlers.
     */
    private SdkStructuredJsonFactory getSdkFactory() {
        if (isCborEnabled()) {
            return SdkStructuredCborFactory.SDK_CBOR_FACTORY;
        } else if (isIonEnabled()) {
            return isIonBinaryEnabled()
                   ? SdkStructuredIonFactory.SDK_ION_BINARY_FACTORY
                   : SdkStructuredIonFactory.SDK_ION_TEXT_FACTORY;
        } else {
            return SdkStructuredPlainJsonFactory.SDK_JSON_FACTORY;
        }
    }

    /**
     * @return Content type resolver implementation to use.
     */
    private JsonContentTypeResolver getContentTypeResolver() {
        if (isCborEnabled()) {
            return JsonContentTypeResolver.CBOR;
        } else if (isIonEnabled()) {
            return isIonBinaryEnabled()
                   ? JsonContentTypeResolver.ION_BINARY
                   : JsonContentTypeResolver.ION_TEXT;
        } else {
            return JsonContentTypeResolver.JSON;
        }
    }

    private boolean isCborEnabled() {
        return metadata.isSupportsCbor() && SdkSystemSetting.CBOR_ENABLED.getBooleanValueOrThrow();
    }

    private boolean isIonEnabled() {
        return metadata.isSupportsIon();
    }

    boolean isIonBinaryEnabled() {
        return SdkSystemSetting.BINARY_ION_ENABLED.getBooleanValueOrThrow();
    }
}
