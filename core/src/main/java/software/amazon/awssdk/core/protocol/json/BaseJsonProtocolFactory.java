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

import static software.amazon.awssdk.core.SdkSystemSetting.BINARY_ION_ENABLED;
import static software.amazon.awssdk.core.SdkSystemSetting.CBOR_ENABLED;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.core.runtime.http.response.JsonResponseHandler;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Factory to generate the various JSON protocol handlers and generators depending on the wire protocol to be used for
 * communicating with the service.
 *
 * @param <RequestT> The type of the request
 * @param <ExceptionT> the type of the exception
 */
@ThreadSafe
@SdkProtectedApi
public abstract class BaseJsonProtocolFactory<RequestT extends SdkRequest, ExceptionT extends SdkServiceException> {

    protected final JsonClientMetadata jsonClientMetadata;

    public BaseJsonProtocolFactory(JsonClientMetadata metadata) {
        this.jsonClientMetadata = metadata;
    }

    public <T extends RequestT> ProtocolRequestMarshaller<T> createProtocolMarshaller(
        OperationInfo operationInfo, T origRequest) {
        return JsonProtocolMarshallerBuilder.<T>standard()
            .jsonGenerator(createGenerator(operationInfo))
            .contentType(getContentType())
            .operationInfo(operationInfo)
            .originalRequest(origRequest)
            .sendExplicitNullForPayload(false)
            .build();
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    public abstract HttpResponseHandler<ExceptionT> createErrorResponseHandler(
        JsonErrorResponseMetadata errorResponseMetadata);

    /**
     * Returns the response handler to be used for handling a successful response.
     *
     * @param operationMetadata Additional context information about an operation to create the appropriate response handler.
     */
    public abstract <T> JsonResponseHandler<T> createResponseHandler(
        JsonOperationMetadata operationMetadata, Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller);

    protected abstract String getContentType();

    protected abstract StructuredJsonGenerator createGenerator(OperationInfo operationInfo);

    protected final boolean isCborEnabled() {
        return jsonClientMetadata.isSupportsCbor() && CBOR_ENABLED.getBooleanValueOrThrow();
    }

    protected final boolean isIonEnabled() {
        return jsonClientMetadata.isSupportsIon();
    }

    protected final boolean isIonBinaryEnabled() {
        return BINARY_ION_ENABLED.getBooleanValueOrThrow();
    }
}
