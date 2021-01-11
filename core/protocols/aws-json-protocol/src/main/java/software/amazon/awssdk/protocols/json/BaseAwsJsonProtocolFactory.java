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

package software.amazon.awssdk.protocols.json;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.MetricCollectingHttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.dom.JsonDomParser;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonErrorMessageParser;
import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonProtocolErrorUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.AwsJsonResponseHandler;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonProtocolUnmarshaller;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonResponseHandler;

@SdkProtectedApi
public abstract class BaseAwsJsonProtocolFactory {

    /**
     * Content type resolver implementation for plain text AWS_JSON services.
     */
    protected static final JsonContentTypeResolver AWS_JSON = new DefaultJsonContentTypeResolver("application/x-amz-json-");

    private final AwsJsonProtocolMetadata protocolMetadata;
    private final List<ExceptionMetadata> modeledExceptions;
    private final Supplier<SdkPojo> defaultServiceExceptionSupplier;
    private final String customErrorCodeFieldName;
    private final SdkClientConfiguration clientConfiguration;
    private final JsonProtocolUnmarshaller protocolUnmarshaller;

    protected BaseAwsJsonProtocolFactory(Builder<?> builder) {
        this.protocolMetadata = builder.protocolMetadata.build();
        this.modeledExceptions = unmodifiableList(builder.modeledExceptions);
        this.defaultServiceExceptionSupplier = builder.defaultServiceExceptionSupplier;
        this.customErrorCodeFieldName = builder.customErrorCodeFieldName;
        this.clientConfiguration = builder.clientConfiguration;
        this.protocolUnmarshaller = JsonProtocolUnmarshaller
            .builder()
            .parser(JsonDomParser.create(getSdkFactory().getJsonFactory()))
            .defaultTimestampFormats(getDefaultTimestampFormats())
            .build();
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
    public final <T extends SdkPojo> HttpResponseHandler<T> createResponseHandler(JsonOperationMetadata operationMetadata,
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
    public final <T extends SdkPojo> HttpResponseHandler<T> createResponseHandler(
        JsonOperationMetadata operationMetadata,
        Function<SdkHttpFullResponse, SdkPojo> pojoSupplier) {
        return timeUnmarshalling(
            new AwsJsonResponseHandler<>(
                new JsonResponseHandler<>(protocolUnmarshaller,
                                          pojoSupplier,
                                          operationMetadata.hasStreamingSuccessResponse(),
                                          operationMetadata.isPayloadJson())));
    }

    /**
     * Creates a response handler for handling a error response (non 2xx response).
     */
    public final HttpResponseHandler<AwsServiceException> createErrorResponseHandler(
        JsonOperationMetadata errorResponseMetadata) {
        return timeUnmarshalling(AwsJsonProtocolErrorUnmarshaller
            .builder()
            .jsonProtocolUnmarshaller(protocolUnmarshaller)
            .exceptions(modeledExceptions)
            .errorCodeParser(getSdkFactory().getErrorCodeParser(customErrorCodeFieldName))
            .errorMessageParser(AwsJsonErrorMessageParser.DEFAULT_ERROR_MESSAGE_PARSER)
            .jsonFactory(getSdkFactory().getJsonFactory())
            .defaultExceptionSupplier(defaultServiceExceptionSupplier)
            .build());
    }

    private <T> MetricCollectingHttpResponseHandler<T> timeUnmarshalling(HttpResponseHandler<T> delegate) {
        return MetricCollectingHttpResponseHandler.create(CoreMetric.UNMARSHALLING_DURATION, delegate);
    }

    private StructuredJsonGenerator createGenerator(OperationInfo operationInfo) {
        if (operationInfo.hasPayloadMembers() || protocolMetadata.protocol() == AwsJsonProtocol.AWS_JSON) {
            return createGenerator();
        } else {
            return StructuredJsonGenerator.NO_OP;
        }
    }

    @SdkTestInternalApi
    private StructuredJsonGenerator createGenerator() {
        return getSdkFactory().createWriter(getContentType());
    }

    @SdkTestInternalApi
    protected final String getContentType() {
        return getContentTypeResolver().resolveContentType(protocolMetadata);
    }

    /**
     * @return Content type resolver implementation to use.
     */
    protected JsonContentTypeResolver getContentTypeResolver() {
        return AWS_JSON;
    }

    /**
     * @return Instance of {@link StructuredJsonFactory} to use in creating handlers.
     */
    protected StructuredJsonFactory getSdkFactory() {
        return AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY;
    }

    /**
     * @return The default timestamp format for unmarshalling for each location in the response. This
     * can be overridden by subclasses to customize behavior.
     */
    protected Map<MarshallLocation, TimestampFormatTrait.Format> getDefaultTimestampFormats() {
        Map<MarshallLocation, TimestampFormatTrait.Format> formats = new HashMap<>();
        formats.put(MarshallLocation.HEADER, TimestampFormatTrait.Format.RFC_822);
        formats.put(MarshallLocation.PAYLOAD, TimestampFormatTrait.Format.UNIX_TIMESTAMP);
        return Collections.unmodifiableMap(formats);
    }

    public final ProtocolMarshaller<SdkHttpFullRequest> createProtocolMarshaller(OperationInfo operationInfo) {
        return JsonProtocolMarshallerBuilder.create()
                                            .endpoint(clientConfiguration.option(SdkClientOption.ENDPOINT))
                                            .jsonGenerator(createGenerator(operationInfo))
                                            .contentType(getContentType())
                                            .operationInfo(operationInfo)
                                            .sendExplicitNullForPayload(false)
                                            .build();
    }

    /**
     * Builder for {@link AwsJsonProtocolFactory}.
     */
    public abstract static class Builder<SubclassT extends Builder> {

        private final AwsJsonProtocolMetadata.Builder protocolMetadata = AwsJsonProtocolMetadata.builder();
        private final List<ExceptionMetadata> modeledExceptions = new ArrayList<>();
        private Supplier<SdkPojo> defaultServiceExceptionSupplier;
        private String customErrorCodeFieldName;
        private SdkClientConfiguration clientConfiguration;

        protected Builder() {
        }

        /**
         * Registers a new modeled exception by the error code.
         *
         * @param errorMetadata Metadata to unmarshall the modeled exception.
         * @return This builder for method chaining.
         */
        public final SubclassT registerModeledException(ExceptionMetadata errorMetadata) {
            modeledExceptions.add(errorMetadata);
            return getSubclass();
        }

        /**
         * A supplier for the services base exception builder. This is used when we can't identify any modeled
         * exception to unmarshall into.
         *
         * @param exceptionBuilderSupplier Suppplier of the base service exceptions Builder.
         * @return This builder for method chaining.
         */
        public final SubclassT defaultServiceExceptionSupplier(Supplier<SdkPojo> exceptionBuilderSupplier) {
            this.defaultServiceExceptionSupplier = exceptionBuilderSupplier;
            return getSubclass();
        }

        /**
         * @param protocol Protocol of the client (i.e. REST or RPC).
         * @return This builder for method chaining.
         */
        public final SubclassT protocol(AwsJsonProtocol protocol) {
            protocolMetadata.protocol(protocol);
            return getSubclass();
        }

        /**
         * Protocol version of the client (right now supports JSON 1.0 and JSON 1.1). Used to determine content type.
         *
         * @param protocolVersion JSON protocol version.
         * @return This builder for method chaining.
         */
        public final SubclassT protocolVersion(String protocolVersion) {
            protocolMetadata.protocolVersion(protocolVersion);
            return getSubclass();
        }

        /**
         * Custom field name containing the error code that identifies the exception. Currently only used by Glacier
         * which uses the "code" field instead of the traditional "__type".
         *
         * @param customErrorCodeFieldName Custom field name to look for error code.
         * @return This builder for method chaining.
         */
        public final SubclassT customErrorCodeFieldName(String customErrorCodeFieldName) {
            this.customErrorCodeFieldName = customErrorCodeFieldName;
            return getSubclass();
        }

        /**
         * Sets the {@link SdkClientConfiguration} which contains the service endpoint.
         *
         * @param clientConfiguration Configuration of the client.
         * @return This builder for method chaining.
         */
        public final SubclassT clientConfiguration(SdkClientConfiguration clientConfiguration) {
            this.clientConfiguration = clientConfiguration;
            return getSubclass();
        }

        @SuppressWarnings("unchecked")
        private SubclassT getSubclass() {
            return (SubclassT) this;
        }

    }
}
