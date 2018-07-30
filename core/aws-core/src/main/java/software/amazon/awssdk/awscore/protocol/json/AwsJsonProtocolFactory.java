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

package software.amazon.awssdk.awscore.protocol.json;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonErrorUnmarshaller;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonProtocol;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredCborFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredIonFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredJsonFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.JsonContentResolverFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.JsonContentTypeResolver;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.JsonResponseHandler;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.json.BaseJsonProtocolFactory;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.core.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.core.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

/**
 * Factory to generate the various JSON protocol handlers and generators depending on the wire protocol to be used for
 * communicating with the service.
 */
@ThreadSafe
@SdkProtectedApi
public final class AwsJsonProtocolFactory extends BaseJsonProtocolFactory<AwsRequest, AwsServiceException> {

    private final AwsJsonProtocolMetadata protocolMetadata;
    private final List<AwsJsonErrorUnmarshaller> errorUnmarshallers = new ArrayList<>();

    public AwsJsonProtocolFactory(JsonClientMetadata metadata, AwsJsonProtocolMetadata protocolMetadata) {
        super(metadata);
        this.protocolMetadata = protocolMetadata;
        createErrorUnmarshallers();
    }

    /**
     * Returns the response handler to be used for handling a successful response.
     *
     * @param operationMetadata Additional context information about an operation to create the appropriate response handler.
     */
    @Override
    public <T> JsonResponseHandler<T> createResponseHandler(
        JsonOperationMetadata operationMetadata,
        Unmarshaller<T, JsonUnmarshallerContext> responseUnmarshaller) {
        return getSdkFactory().createResponseHandler(operationMetadata, responseUnmarshaller);
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    @Override
    public HttpResponseHandler<AwsServiceException> createErrorResponseHandler(
        JsonErrorResponseMetadata errorResponseMetadata) {
        return getSdkFactory().createErrorResponseHandler(errorUnmarshallers, errorResponseMetadata
            .getCustomErrorCodeFieldName());
    }

    protected StructuredJsonGenerator createGenerator(OperationInfo operationInfo) {
        if (operationInfo.hasPayloadMembers() || protocolMetadata.protocol() == AwsJsonProtocol.AWS_JSON) {
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
    protected String getContentType() {
        return getContentTypeResolver().resolveContentType(jsonClientMetadata, protocolMetadata);
    }

    @SuppressWarnings("unchecked")
    private void createErrorUnmarshallers() {
        for (JsonErrorShapeMetadata errorMetadata : jsonClientMetadata.getErrorShapeMetadata()) {
            errorUnmarshallers.add(new AwsJsonErrorUnmarshaller(
                (Class<? extends AwsServiceException>) errorMetadata.getModeledClass(),
                errorMetadata.getErrorCode()));
        }

        errorUnmarshallers.add(new AwsJsonErrorUnmarshaller(
            (Class<? extends AwsServiceException>) jsonClientMetadata.getBaseServiceExceptionClass(),
            null));
    }

    /**
     * @return Content type resolver implementation to use.
     */
    private JsonContentTypeResolver getContentTypeResolver() {
        if (isCborEnabled()) {
            return JsonContentResolverFactory.AWS_CBOR;
        } else if (isIonEnabled()) {
            return isIonBinaryEnabled()
                   ? JsonContentResolverFactory.ION_BINARY
                   : JsonContentResolverFactory.ION_TEXT;
        } else {
            return JsonContentResolverFactory.AWS_JSON;
        }
    }

    /**
     * @return Instance of {@link AwsStructuredJsonFactory} to use in creating handlers.
     */
    private AwsStructuredJsonFactory getSdkFactory() {
        if (isCborEnabled()) {
            return AwsStructuredCborFactory.SDK_CBOR_FACTORY;
        } else if (isIonEnabled()) {
            return isIonBinaryEnabled()
                   ? AwsStructuredIonFactory.SDK_ION_BINARY_FACTORY
                   : AwsStructuredIonFactory.SDK_ION_TEXT_FACTORY;
        } else {
            return AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY;
        }
    }
}