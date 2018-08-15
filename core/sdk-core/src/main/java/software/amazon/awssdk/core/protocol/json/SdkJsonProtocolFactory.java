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

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.JsonResponseHandler;
import software.amazon.awssdk.core.internal.protocol.json.SdkJsonErrorUnmarshaller;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Factory to generate the JSON protocol handlers and generators to be used for
 * communicating with the service.
 */
@ThreadSafe
@SdkProtectedApi
public class SdkJsonProtocolFactory extends BaseJsonProtocolFactory<SdkRequest, SdkServiceException> {

    private static final SdkStructuredJsonFactory JSON_FACTORY = SdkStructuredPlainJsonFactory.SDK_JSON_FACTORY;
    private static final String CONTENT_TYPE = "application/json";

    SdkJsonProtocolFactory(JsonClientMetadata metadata) {
        super(metadata);
    }

    protected StructuredJsonGenerator createGenerator(OperationInfo operationInfo) {
        if (operationInfo.hasPayloadMembers()) {
            return createGenerator();
        } else {
            return StructuredJsonGenerator.NO_OP;
        }
    }

    private StructuredJsonGenerator createGenerator() {
        return JSON_FACTORY.createWriter(CONTENT_TYPE);
    }

    @SdkTestInternalApi
    protected String getContentType() {
        return CONTENT_TYPE;
    }

    /**
     * Returns the response handler to be used for handling a successful response.
     *
     * @param operationMetadata Additional context information about an operation to create the appropriate response handler.
     */
    public <T> JsonResponseHandler<T> createResponseHandler(JsonOperationMetadata operationMetadata,
                                                            Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller) {
        return JSON_FACTORY.createResponseHandler(operationMetadata, responseUnmarshaller);
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    @Override
    public HttpResponseHandler<SdkServiceException> createErrorResponseHandler(
        JsonErrorResponseMetadata errorResponseMetadata) {
        return JSON_FACTORY.createErrorResponseHandler(createErrorUnmarshallers());
    }

    @SuppressWarnings("unchecked")
    private List<SdkJsonErrorUnmarshaller> createErrorUnmarshallers() {

        List<SdkJsonErrorUnmarshaller> errorUnmarshallers = jsonClientMetadata.getErrorShapeMetadata().stream()
                                                                              .map(this::createErrorUnmarshaller)
                                                                              .collect(Collectors.toList());
        // All unmodeled/unknown exceptions are unmarshalled into the service specific base
        // exception class.
        errorUnmarshallers.add(new SdkJsonErrorUnmarshaller(
            (Class<? extends SdkServiceException>) jsonClientMetadata.getBaseServiceExceptionClass(),
            null));
        return errorUnmarshallers;
    }

    @SuppressWarnings("unchecked")
    private SdkJsonErrorUnmarshaller createErrorUnmarshaller(JsonErrorShapeMetadata errorShape) {
        return new SdkJsonErrorUnmarshaller(
            errorShape.getModeledClass(),
            errorShape.getHttpStatusCode());
    }
}
