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

package software.amazon.awssdk.protocols.query;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.http.MetricCollectingHttpResponseHandler;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.query.internal.marshall.QueryProtocolMarshaller;
import software.amazon.awssdk.protocols.query.internal.unmarshall.AwsQueryResponseHandler;
import software.amazon.awssdk.protocols.query.internal.unmarshall.QueryProtocolUnmarshaller;
import software.amazon.awssdk.protocols.query.unmarshall.AwsXmlErrorProtocolUnmarshaller;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

/**
 * Protocol factory for the AWS/Query protocol.
 */
@SdkProtectedApi
public class AwsQueryProtocolFactory {

    private final SdkClientConfiguration clientConfiguration;
    private final List<ExceptionMetadata> modeledExceptions;
    private final Supplier<SdkPojo> defaultServiceExceptionSupplier;
    private final MetricCollectingHttpResponseHandler<AwsServiceException> errorUnmarshaller;

    AwsQueryProtocolFactory(Builder<?> builder) {
        this.clientConfiguration = builder.clientConfiguration;
        this.modeledExceptions = unmodifiableList(builder.modeledExceptions);
        this.defaultServiceExceptionSupplier = builder.defaultServiceExceptionSupplier;
        this.errorUnmarshaller = timeUnmarshalling(AwsXmlErrorProtocolUnmarshaller
            .builder()
            .defaultExceptionSupplier(defaultServiceExceptionSupplier)
            .exceptions(modeledExceptions)
            // We don't set result wrapper since that's handled by the errorRootExtractor
            .errorUnmarshaller(QueryProtocolUnmarshaller.builder().build())
            .errorRootExtractor(this::getErrorRoot)
            .build());
    }

    /**
     * Creates a new marshaller for the given request.
     *
     * @param operationInfo Object containing metadata about the operation.
     * @return New {@link ProtocolMarshaller}.
     */
    public final ProtocolMarshaller<SdkHttpFullRequest> createProtocolMarshaller(
        OperationInfo operationInfo) {
        return QueryProtocolMarshaller.builder()
                                      .endpoint(clientConfiguration.option(SdkClientOption.ENDPOINT))
                                      .operationInfo(operationInfo)
                                      .isEc2(isEc2())
                                      .build();
    }

    /**
     * Creates the success response handler to unmarshall the response into a POJO.
     *
     * @param pojoSupplier Supplier of the POJO builder we are unmarshalling into.
     * @param <T> Type being unmarshalled.
     * @return New {@link HttpResponseHandler} for success responses.
     */
    public final <T extends AwsResponse> HttpResponseHandler<T> createResponseHandler(Supplier<SdkPojo> pojoSupplier) {
        return timeUnmarshalling(new AwsQueryResponseHandler<>(QueryProtocolUnmarshaller.builder()
                                                                                        .hasResultWrapper(!isEc2())
                                                                                        .build(), r -> pojoSupplier.get()));
    }

    /**
     * @return A {@link HttpResponseHandler} that will unmarshall the service exceptional response into
     * a modeled exception or the service base exception.
     */
    public final HttpResponseHandler<AwsServiceException> createErrorResponseHandler() {
        return errorUnmarshaller;
    }

    private <T> MetricCollectingHttpResponseHandler<T> timeUnmarshalling(HttpResponseHandler<T> delegate) {
        return MetricCollectingHttpResponseHandler.create(CoreMetric.UNMARSHALLING_DURATION, delegate);
    }

    /**
     * Extracts the <Error/> element from the root XML document. Method is protected as EC2 has a slightly
     * different location.
     *
     * @param document Root XML document.
     * @return If error root is found than a fulfilled {@link Optional}, otherwise an empty one.
     */
    Optional<XmlElement> getErrorRoot(XmlElement document) {
        return document.getOptionalElementByName("Error");
    }

    /**
     * EC2 has a few distinct differences from query so we wire things up a bit differently.
     */
    boolean isEc2() {
        return false;
    }

    /**
     * @return New Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link AwsQueryProtocolFactory}.
     *
     * @param <SubclassT> Subclass of Builder for fluent method chaining.
     */
    public static class Builder<SubclassT extends Builder> {

        private final List<ExceptionMetadata> modeledExceptions = new ArrayList<>();
        private SdkClientConfiguration clientConfiguration;
        private Supplier<SdkPojo> defaultServiceExceptionSupplier;

        Builder() {
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

        /**
         * Registers a new modeled exception by the error code.
         *
         * @param errorMetadata metadata for unmarshalling the exceptions
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

        @SuppressWarnings("unchecked")
        private SubclassT getSubclass() {
            return (SubclassT) this;
        }

        /**
         * @return New instance of {@link AwsQueryProtocolFactory}.
         */
        public AwsQueryProtocolFactory build() {
            return new AwsQueryProtocolFactory(this);
        }
    }
}
