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

package software.amazon.awssdk.protocol.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.async.AsyncResponseHandler;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponseAdapter;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.UnmarshallingAsyncResponseHandler;
import software.amazon.awssdk.protocol.OperationInfo;
import software.amazon.awssdk.protocol.Protocol;
import software.amazon.awssdk.protocol.ProtocolRequestMarshaller;
import software.amazon.awssdk.runtime.http.response.JsonResponseHandler;
import software.amazon.awssdk.runtime.transform.JsonErrorUnmarshaller;
import software.amazon.awssdk.runtime.transform.JsonUnmarshallerContext;
import software.amazon.awssdk.runtime.transform.JsonUnmarshallerContextImpl;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.runtime.transform.UnmarshallingStreamingResponseHandler;
import software.amazon.awssdk.sync.StreamingResponseHandler;

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

    public <T> ProtocolRequestMarshaller<T> createProtocolMarshaller(OperationInfo operationInfo, T origRequest) {
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

    /**
     * Returns the response handler to be used for handling a successful response.
     */
    public <ResponseT, ReturnT> SdkHttpResponseHandler<ReturnT> createAsyncStreamingResponseHandler(
            Unmarshaller<ResponseT, JsonUnmarshallerContext> responseUnmarshaller,
            AsyncResponseHandler<ResponseT, ReturnT> asyncResponseHandler) {

        return new UnmarshallingAsyncResponseHandler<>(
                asyncResponseHandler,
            sdkHttpResponse -> unmarshall(responseUnmarshaller, (SdkHttpFullResponse) sdkHttpResponse));
    }

    public <ResponseT, ReturnT> HttpResponseHandler<ReturnT> createStreamingResponseHandler(
            Unmarshaller<ResponseT, JsonUnmarshallerContext> responseUnmarshaller,
            StreamingResponseHandler<ResponseT, ReturnT> streamingResponseHandler) {
        return new UnmarshallingStreamingResponseHandler<>(streamingResponseHandler, httpResponse ->
                unmarshall(responseUnmarshaller, httpResponse));
    }

    private <ResponseT> ResponseT unmarshall(Unmarshaller<ResponseT, JsonUnmarshallerContext> responseUnmarshaller,
                                             SdkHttpFullResponse sdkHttpResponse) throws Exception {
        return unmarshall(responseUnmarshaller, SdkHttpResponseAdapter.adapt(false, null, sdkHttpResponse));
    }

    private <ResponseT> ResponseT unmarshall(Unmarshaller<ResponseT, JsonUnmarshallerContext> responseUnmarshaller,
                                             HttpResponse httpResponse) throws Exception {
        return responseUnmarshaller.unmarshall(new JsonUnmarshallerContextImpl(null, Collections.emptyMap(), httpResponse));
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    public HttpResponseHandler<AmazonServiceException> createErrorResponseHandler(
            JsonErrorResponseMetadata errorResponsMetadata) {
        return getSdkFactory().createErrorResponseHandler(errorUnmarshallers, errorResponsMetadata
                .getCustomErrorCodeFieldName());
    }

    @SuppressWarnings("unchecked")
    private void createErrorUnmarshallers() {
        for (JsonErrorShapeMetadata errorMetadata : metadata.getErrorShapeMetadata()) {
            errorUnmarshallers.add(new JsonErrorUnmarshaller(
                    (Class<? extends AmazonServiceException>) errorMetadata.getModeledClass(),
                    errorMetadata.getErrorCode()));

        }
        errorUnmarshallers.add(new JsonErrorUnmarshaller(
                (Class<? extends AmazonServiceException>) metadata.getBaseServiceExceptionClass(),
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
        return metadata.isSupportsCbor() && AwsSystemSetting.AWS_CBOR_ENABLED.getBooleanValueOrThrow();
    }

    private boolean isIonEnabled() {
        return metadata.isSupportsIon();
    }

    boolean isIonBinaryEnabled() {
        return AwsSystemSetting.AWS_BINARY_ION_ENABLED.getBooleanValueOrThrow();
    }
}
