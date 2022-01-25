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

package software.amazon.awssdk.http.async;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.Validate;

/**
 * Request object containing the parameters necessary to make an asynchronous HTTP request.
 *
 * @see SdkAsyncHttpClient
 */
@SdkPublicApi
public final class AsyncExecuteRequest {
    private final SdkHttpRequest request;
    private final SdkHttpContentPublisher requestContentPublisher;
    private final SdkAsyncHttpResponseHandler responseHandler;
    private final MetricCollector metricCollector;
    private final boolean isFullDuplex;
    private final SdkHttpExecutionAttributes sdkHttpExecutionAttributes;

    private AsyncExecuteRequest(BuilderImpl builder) {
        this.request = builder.request;
        this.requestContentPublisher = builder.requestContentPublisher;
        this.responseHandler = builder.responseHandler;
        this.metricCollector = builder.metricCollector;
        this.isFullDuplex = builder.isFullDuplex;
        this.sdkHttpExecutionAttributes = builder.executionAttributesBuilder.build();
    }

    /**
     * @return The HTTP request.
     */
    public SdkHttpRequest request() {
        return request;
    }

    /**
     * @return The publisher of request body.
     */
    public SdkHttpContentPublisher requestContentPublisher() {
        return requestContentPublisher;
    }

    /**
     * @return The response handler.
     */
    public SdkAsyncHttpResponseHandler responseHandler() {
        return responseHandler;
    }

    /**
     * @return The {@link MetricCollector}.
     */
    public Optional<MetricCollector> metricCollector() {
        return Optional.ofNullable(metricCollector);
    }

    /**
     * @return True if the operation this request belongs to is full duplex. Otherwise false.
     */
    public boolean fullDuplex() {
        return isFullDuplex;
    }

    /**
     * @return the SDK HTTP execution attributes associated with this request
     */
    public SdkHttpExecutionAttributes httpExecutionAttributes() {
        return sdkHttpExecutionAttributes;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Set the HTTP request to be executed by the client.
         *
         * @param request The request.
         * @return This builder for method chaining.
         */
        Builder request(SdkHttpRequest request);

        /**
         * Set the publisher of the request content.
         *
         * @param requestContentPublisher The publisher.
         * @return This builder for method chaining.
         */
        Builder requestContentPublisher(SdkHttpContentPublisher requestContentPublisher);

        /**
         * Set the response handler for the resposne.
         *
         * @param responseHandler The response handler.
         * @return This builder for method chaining.
         */
        Builder responseHandler(SdkAsyncHttpResponseHandler responseHandler);

        /**
         * Set the {@link MetricCollector} to be used by the HTTP client to
         * report metrics collected for this request.
         *
         * @param metricCollector The metric collector.
         * @return This builder for method chaining.
         */
        Builder metricCollector(MetricCollector metricCollector);

        /**
         * Option to indicate if the request is for a full duplex operation ie., request and response are sent/received at
         * the same time.
         * <p>
         * This can be used to set http configuration like ReadTimeouts as soon as request has begin sending data instead of
         * waiting for the entire request to be sent.
         *
         * @return True if the operation this request belongs to is full duplex. Otherwise false.
         */
        Builder fullDuplex(boolean fullDuplex);

        /**
         * Put an HTTP execution attribute into to the collection of HTTP execution attributes for this request
         *
         * @param attribute The execution attribute object
         * @param value     The value of the execution attribute.
         */
        <T> Builder putHttpExecutionAttribute(SdkHttpExecutionAttribute<T> attribute, T value);

        /**
         * Sets the additional HTTP execution attributes collection for this request.
         * <p>
         * This will override the attributes configured through
         * {@link #putHttpExecutionAttribute(SdkHttpExecutionAttribute, Object)}
         *
         * @param executionAttributes Execution attributes map for this request.
         * @return This object for method chaining.
         */
        Builder httpExecutionAttributes(SdkHttpExecutionAttributes executionAttributes);

        AsyncExecuteRequest build();
    }

    private static class BuilderImpl implements Builder {
        private SdkHttpRequest request;
        private SdkHttpContentPublisher requestContentPublisher;
        private SdkAsyncHttpResponseHandler responseHandler;
        private MetricCollector metricCollector;
        private boolean isFullDuplex;
        private SdkHttpExecutionAttributes.Builder executionAttributesBuilder = SdkHttpExecutionAttributes.builder();

        @Override
        public Builder request(SdkHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public Builder requestContentPublisher(SdkHttpContentPublisher requestContentPublisher) {
            this.requestContentPublisher = requestContentPublisher;
            return this;
        }

        @Override
        public Builder responseHandler(SdkAsyncHttpResponseHandler responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }

        @Override
        public Builder metricCollector(MetricCollector metricCollector) {
            this.metricCollector = metricCollector;
            return this;
        }

        @Override
        public Builder fullDuplex(boolean fullDuplex) {
            isFullDuplex = fullDuplex;
            return this;
        }

        @Override
        public <T> Builder putHttpExecutionAttribute(SdkHttpExecutionAttribute<T> attribute, T value) {
            this.executionAttributesBuilder.put(attribute, value);
            return this;
        }

        @Override
        public Builder httpExecutionAttributes(SdkHttpExecutionAttributes executionAttributes) {
            Validate.paramNotNull(executionAttributes, "executionAttributes");
            this.executionAttributesBuilder = executionAttributes.toBuilder();
            return this;
        }


        @Override
        public AsyncExecuteRequest build() {
            return new AsyncExecuteRequest(this);
        }
    }
}
