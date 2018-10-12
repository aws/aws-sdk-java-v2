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

import static software.amazon.awssdk.core.SdkSystemSetting.BINARY_ION_ENABLED;
import static software.amazon.awssdk.core.SdkSystemSetting.CBOR_ENABLED;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.http.response.AwsJsonResponseHandler;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonErrorUnmarshaller;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredCborFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredIonFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredJsonFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.JsonContentResolverFactory;
import software.amazon.awssdk.awscore.internal.protocol.json.JsonContentTypeResolver;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.JsonResponseHandler;
import software.amazon.awssdk.core.internal.protocol.json.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.core.protocol.OperationInfo;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.SdkPojo;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorResponseMetadata;
import software.amazon.awssdk.core.protocol.json.JsonErrorShapeMetadata;
import software.amazon.awssdk.core.protocol.json.JsonOperationMetadata;
import software.amazon.awssdk.core.protocol.json.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.core.protocol.json.StructuredJsonGenerator;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Factory to generate the various JSON protocol handlers and generators depending on the wire protocol to be used for
 * communicating with the service.
 */
@ThreadSafe
@SdkProtectedApi
public final class AwsJsonProtocolFactory {

    private final JsonClientMetadata jsonClientMetadata;
    private final AwsJsonProtocolMetadata protocolMetadata;
    private final List<AwsJsonErrorUnmarshaller> errorUnmarshallers = new ArrayList<>();

    private AwsJsonProtocolFactory(Builder builder) {
        this.jsonClientMetadata = builder.metadata;
        this.protocolMetadata = builder.protocolMetadata.build();
        createErrorUnmarshallers();
    }

    /**
     * Creates a new response handler with the given {@link JsonOperationMetadata} and a supplier of the POJO response
     * type.
     *
     * @param operationMetadata Metadata about operation being unmarshalled.
     * @param pojoSupplier {@link Supplier} of the POJO response type.
     * @param <T> Type being unmarshalled.
     * @return HttpResponseHandler that will handle the HTTP response and unmarshall into a POJO.
     */
    public <T extends SdkPojo> HttpResponseHandler<T> createResponseHandler(JsonOperationMetadata operationMetadata,
                                                                            Supplier<SdkPojo> pojoSupplier) {
        return createResponseHandler(operationMetadata, r -> pojoSupplier.get());
    }

    /**
     * Creates a new response handler with the given {@link JsonOperationMetadata} and a supplier of the POJO response
     * type.
     *
     * @param operationMetadata Metadata about operation being unmarshalled.
     * @param pojoSupplier {@link Supplier} of the POJO response type. Has access to the HTTP response, primarily for polymorphic
     * deserialization as seen in event stream (i.e. unmarshalled event depends on ':event-type' header).
     * @param <T> Type being unmarshalled.
     * @return HttpResponseHandler that will handle the HTTP response and unmarshall into a POJO.
     */
    public <T extends SdkPojo> HttpResponseHandler<T> createResponseHandler(JsonOperationMetadata operationMetadata,
                                                                            Function<SdkHttpFullResponse, SdkPojo> pojoSupplier) {
        JsonProtocolUnmarshaller<T> unmarshaller = new JsonProtocolUnmarshaller<>(getSdkFactory().createObjectMapper());
        return new AwsJsonResponseHandler<>(
            new JsonResponseHandler<>(unmarshaller,
                                      pojoSupplier,
                                      operationMetadata.isHasStreamingSuccessResponse(),
                                      operationMetadata.isPayloadJson()));
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    public HttpResponseHandler<AwsServiceException> createErrorResponseHandler(
        JsonErrorResponseMetadata errorResponseMetadata) {
        return getSdkFactory().createErrorResponseHandler(errorUnmarshallers, errorResponseMetadata
            .getCustomErrorCodeFieldName());
    }

    private StructuredJsonGenerator createGenerator(OperationInfo operationInfo) {
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

    public <T> ProtocolMarshaller<Request<T>> createProtocolMarshaller(
        OperationInfo operationInfo, T origRequest) {
        return JsonProtocolMarshallerBuilder.<T>standard()
            .jsonGenerator(createGenerator(operationInfo))
            .contentType(getContentType())
            .operationInfo(operationInfo)
            .originalRequest(origRequest)
            .sendExplicitNullForPayload(false)
            .build();
    }

    private boolean isCborEnabled() {
        return jsonClientMetadata.isSupportsCbor() && CBOR_ENABLED.getBooleanValueOrThrow();
    }

    private boolean isIonEnabled() {
        return jsonClientMetadata.isSupportsIon();
    }

    private boolean isIonBinaryEnabled() {
        return BINARY_ION_ENABLED.getBooleanValueOrThrow();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsJsonProtocolFactory}.
     */
    public static final class Builder {

        private final JsonClientMetadata metadata = new JsonClientMetadata();
        private final AwsJsonProtocolMetadata.Builder protocolMetadata = AwsJsonProtocolMetadata.builder();

        private Builder() {
        }

        public Builder addErrorMetadata(JsonErrorShapeMetadata errorShapeMetadata) {
            metadata.addErrorMetadata(errorShapeMetadata);
            return this;
        }

        public Builder contentTypeOverride(String contentType) {
            metadata.withContentTypeOverride(contentType);
            return this;
        }

        public Builder supportsCbor(boolean supportsCbor) {
            metadata.withSupportsCbor(supportsCbor);
            return this;
        }

        public Builder supportsIon(boolean supportsIon) {
            metadata.withSupportsIon(supportsIon);
            return this;
        }

        public Builder baseServiceExceptionClass(Class<? extends RuntimeException> baseServiceExceptionClass) {
            metadata.withBaseServiceExceptionClass(baseServiceExceptionClass);
            return this;
        }

        public Builder protocol(AwsJsonProtocol protocol) {
            protocolMetadata.protocol(protocol);
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            protocolMetadata.protocolVersion(protocolVersion);
            return this;
        }

        public AwsJsonProtocolFactory build() {
            return new AwsJsonProtocolFactory(this);
        }

    }
}