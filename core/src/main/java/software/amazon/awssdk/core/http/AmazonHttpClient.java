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

package software.amazon.awssdk.core.http;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.config.SyncClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.pipeline.RequestPipelineBuilder;
import software.amazon.awssdk.core.http.pipeline.stages.AfterExecutionInterceptorsStage;
import software.amazon.awssdk.core.http.pipeline.stages.AfterTransmissionExecutionInterceptorsStage;
import software.amazon.awssdk.core.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.core.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.core.http.pipeline.stages.BeforeTransmissionExecutionInterceptorsStage;
import software.amazon.awssdk.core.http.pipeline.stages.BeforeUnmarshallingExecutionInterceptorsStage;
import software.amazon.awssdk.core.http.pipeline.stages.ClientExecutionTimedStage;
import software.amazon.awssdk.core.http.pipeline.stages.ExecutionFailureExceptionReportingStage;
import software.amazon.awssdk.core.http.pipeline.stages.HandleResponseStage;
import software.amazon.awssdk.core.http.pipeline.stages.HttpResponseAdaptingStage;
import software.amazon.awssdk.core.http.pipeline.stages.MakeHttpRequestStage;
import software.amazon.awssdk.core.http.pipeline.stages.MakeRequestImmutable;
import software.amazon.awssdk.core.http.pipeline.stages.MakeRequestMutable;
import software.amazon.awssdk.core.http.pipeline.stages.MergeCustomHeadersStage;
import software.amazon.awssdk.core.http.pipeline.stages.MergeCustomQueryParamsStage;
import software.amazon.awssdk.core.http.pipeline.stages.MoveParametersToBodyStage;
import software.amazon.awssdk.core.http.pipeline.stages.RetryableStage;
import software.amazon.awssdk.core.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.core.http.pipeline.stages.TimerExceptionHandlingStage;
import software.amazon.awssdk.core.http.pipeline.stages.UnwrapResponseContainer;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.core.retry.SdkDefaultRetrySettings;
import software.amazon.awssdk.core.util.CapacityManager;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@ThreadSafe
@SdkInternalApi
public class AmazonHttpClient implements SdkAutoCloseable {
    /**
     * Used for testing via failure injection.
     */
    static UnreliableTestConfig unreliableTestConfig;

    private final HttpSyncClientDependencies httpClientDependencies;

    public AmazonHttpClient(SyncClientConfiguration syncClientConfiguration) {
        this.httpClientDependencies = HttpSyncClientDependencies.builder()
                                                                .clientExecutionTimer(new ClientExecutionTimer())
                                                                .syncClientConfiguration(syncClientConfiguration)
                                                                .capacityManager(createCapacityManager())
                                                                .build();
    }

    private CapacityManager createCapacityManager() {
        // When enabled, total retry capacity is computed based on retry cost and desired number of retries.
        // TODO: Allow customers to configure throttled retries (https://github.com/aws/aws-sdk-java-v2/issues/17)
        return new CapacityManager(SdkDefaultRetrySettings.RETRY_THROTTLING_COST * SdkDefaultRetrySettings.THROTTLED_RETRIES);
    }

    /**
     * Used to configure the test conditions for injecting intermittent failures to the content
     * input stream.
     *
     * @param config unreliable test configuration for failure injection; or null to disable such
     *               test.
     */
    static void configUnreliableTestConditions(UnreliableTestConfig config) {
        unreliableTestConfig = config;
    }

    /**
     * Shuts down this HTTP client object, releasing any resources that might be held open. This is
     * an optional method, and callers are not expected to call it, but can if they want to
     * explicitly release any open resources. Once a client has been shutdown, it cannot be used to
     * make more requests.
     */
    @Override
    public void close() {
        httpClientDependencies.close();
    }

    /**
     * For unit testing only.
     */
    @SdkTestInternalApi
    public ClientExecutionTimer getClientExecutionTimer() {
        return this.httpClientDependencies.clientExecutionTimer();
    }

    /**
     * Ensures the response handler is not null. If it is this method returns a dummy response
     * handler.
     *
     * @return Either original response handler or dummy response handler.
     */
    private <T> HttpResponseHandler<T> getNonNullResponseHandler(
            HttpResponseHandler<T> responseHandler) {
        if (responseHandler != null) {
            return responseHandler;
        } else {
            return new NoOpResponseHandler<>();
        }
    }

    /**
     * @return A builder used to configure and execute a HTTP request.
     */
    public RequestExecutionBuilder requestExecutionBuilder() {
        return new RequestExecutionBuilderImpl();
    }

    /**
     * Interface to configure a request execution and execute the request.
     */
    public interface RequestExecutionBuilder {

        /**
         * Fluent setter for {@link Request}
         *
         * @param request Request object
         * @return This builder for method chaining.
         * @deprecated Use {@link #request(SdkHttpFullRequest)}
         */
        @Deprecated
        RequestExecutionBuilder request(Request<?> request);

        /**
         * Fluent setter for {@link SdkHttpFullRequest}
         *
         * @param request Request object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder request(SdkHttpFullRequest request);

        RequestExecutionBuilder originalRequest(SdkRequest originalRequest);

        /**
         * Fluent setter for the error response handler
         *
         * @param errorResponseHandler Error response handler
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder errorResponseHandler(
                HttpResponseHandler<? extends SdkException> errorResponseHandler);

        /**
         * Fluent setter for the execution context
         *
         * @param executionContext Execution context
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder executionContext(ExecutionContext executionContext);

        /**
         * Executes the request with the given configuration.
         *
         * @param responseHandler Response handler that outputs the actual result type which is
         *                        preferred going forward.
         * @param <OutputT>       Result type
         * @return Unmarshalled result type.
         */
        <OutputT> OutputT execute(HttpResponseHandler<OutputT> responseHandler);

        /**
         * Executes the request with the given configuration; not handling response.
         */
        void execute();

    }

    private static class NoOpResponseHandler<T> implements HttpResponseHandler<T> {
        @Override
        public T handle(HttpResponse response, ExecutionAttributes executionAttributes) throws Exception {
            return null;
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return false;
        }
    }

    private class RequestExecutionBuilderImpl implements RequestExecutionBuilder {

        private SdkHttpFullRequest request;
        private HttpResponseHandler<? extends SdkException> errorResponseHandler;
        private SdkRequest originalRequest;
        private ExecutionContext executionContext;

        @Override
        public RequestExecutionBuilder request(Request<?> request) {
            this.request = SdkHttpFullRequestAdapter.toHttpFullRequest(request);
            return this;
        }

        @Override
        @ReviewBeforeRelease("This is duplicating information in the interceptor context. Can they be consolidated?")
        public RequestExecutionBuilder request(SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public RequestExecutionBuilder originalRequest(SdkRequest originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }

        @Override
        public RequestExecutionBuilder errorResponseHandler(
                HttpResponseHandler<? extends SdkException> errorResponseHandler) {
            this.errorResponseHandler = errorResponseHandler;
            return this;
        }

        @Override
        public RequestExecutionBuilder executionContext(ExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        @Override
        public <OutputT> OutputT execute(HttpResponseHandler<OutputT> responseHandler) {
            // TODO: We currently have two ways of passing messages to the HTTP client: through the request or through the
            // execution interceptor context. We should combine these two methods when we refactor the way request execution
            // contexts work.
            if (request != null && executionContext != null) {
                executionContext.interceptorContext(
                        executionContext.interceptorContext().copy(ib -> ib.httpRequest(request)));
            }

            try {
                return RequestPipelineBuilder
                        // Start of mutating request
                        .firstSync(RequestPipelineBuilder
                                .firstSync(MakeRequestMutable::new)
                                .then(ApplyTransactionIdStage::new)
                                .then(ApplyUserAgentStage::new)
                                .then(MergeCustomHeadersStage::new)
                                .then(MergeCustomQueryParamsStage::new)
                                .then(MoveParametersToBodyStage::new)
                                .then(MakeRequestImmutable::new)
                                // End of mutating request
                                .then(RequestPipelineBuilder
                                          .firstSync(SigningStage::new)
                                          .then(BeforeTransmissionExecutionInterceptorsStage::new)
                                          .then(MakeHttpRequestStage::new)
                                          .then(AfterTransmissionExecutionInterceptorsStage::new)
                                          .then(HttpResponseAdaptingStage::new)
                                          .then(BeforeUnmarshallingExecutionInterceptorsStage::new)
                                          .then(() -> new HandleResponseStage<>(getNonNullResponseHandler(responseHandler),
                                                                                getNonNullResponseHandler(errorResponseHandler)))
                                          .wrap(TimerExceptionHandlingStage::new)
                                          .wrap(RetryableStage::new)::build)
                                .wrap(StreamManagingStage::new)
                                .wrap(ClientExecutionTimedStage::new)::build)
                        .then(() -> new UnwrapResponseContainer<>())
                        .then(() -> new AfterExecutionInterceptorsStage<>())
                        .wrap(ExecutionFailureExceptionReportingStage::new)
                        .build(httpClientDependencies)
                        .execute(request, createRequestExecutionDependencies());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SdkClientException(e);
            }
        }

        @Override
        public void execute() {
            execute(null);
        }

        private RequestExecutionContext createRequestExecutionDependencies() {
            return RequestExecutionContext.builder()
                                          .originalRequest(originalRequest)
                                          .executionContext(executionContext)
                                          .build();
        }

    }

}
