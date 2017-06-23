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

package software.amazon.awssdk.http;

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.http.exception.SdkInterruptedException;
import software.amazon.awssdk.http.pipeline.RequestPipelineBuilder;
import software.amazon.awssdk.http.pipeline.stages.AfterCallbackStage;
import software.amazon.awssdk.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.http.pipeline.stages.AttachRequestConfigStage;
import software.amazon.awssdk.http.pipeline.stages.BeforeRequestHandlersStage;
import software.amazon.awssdk.http.pipeline.stages.BeforeUnmarshallingCallbackStage;
import software.amazon.awssdk.http.pipeline.stages.ClientExecutionTimedStage;
import software.amazon.awssdk.http.pipeline.stages.ExceptionReportingStage;
import software.amazon.awssdk.http.pipeline.stages.HandleResponseStage;
import software.amazon.awssdk.http.pipeline.stages.HttpResponseAdaptingStage;
import software.amazon.awssdk.http.pipeline.stages.InstrumentHttpResponseContentStage;
import software.amazon.awssdk.http.pipeline.stages.MakeHttpRequestStage;
import software.amazon.awssdk.http.pipeline.stages.MakeRequestImmutable;
import software.amazon.awssdk.http.pipeline.stages.MakeRequestMutable;
import software.amazon.awssdk.http.pipeline.stages.MergeCustomHeadersStage;
import software.amazon.awssdk.http.pipeline.stages.MergeCustomQueryParamsStage;
import software.amazon.awssdk.http.pipeline.stages.MoveParametersToBodyStage;
import software.amazon.awssdk.http.pipeline.stages.ReportRequestContentLengthStage;
import software.amazon.awssdk.http.pipeline.stages.RetryableStage;
import software.amazon.awssdk.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.http.pipeline.stages.TimerExceptionHandlingStage;
import software.amazon.awssdk.http.pipeline.stages.UnwrapResponseContainer;
import software.amazon.awssdk.internal.AmazonWebServiceRequestAdapter;
import software.amazon.awssdk.internal.http.response.AwsErrorResponseHandler;
import software.amazon.awssdk.internal.http.response.AwsResponseHandlerAdapter;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicyAdapter;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.util.CapacityManager;

@ThreadSafe
@SdkProtectedApi
public class AmazonHttpClient implements AutoCloseable {

    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String HEADER_SDK_TRANSACTION_ID = "amz-sdk-invocation-id";

    /**
     * Logger providing detailed information on requests/responses. Users can enable this logger to
     * get access to AWS request IDs for responses, individual requests and parameters sent to AWS,
     * etc.
     */
    @SdkInternalApi
    public static final Log REQUEST_LOG = LogFactory.getLog("software.amazon.awssdk.request");

    /**
     * When throttled retries are enabled, each retry attempt will consume this much capacity.
     * Successful retry attempts will release this capacity back to the pool while failed retries
     * will not.  Successful initial (non-retry) requests will always release 1 capacity unit to the
     * pool.
     */
    public static final int THROTTLED_RETRY_COST = 5;

    /**
     * Used for testing via failure injection.
     */
    static UnreliableTestConfig unreliableTestConfig;

    /**
     * When throttled retries are enabled, this is the total number of subsequent failed retries
     * that may be attempted before retry capacity is fully drained.
     */
    static final int THROTTLED_RETRIES = 100;

    /**
     * A request metric collector used specifically for this httpClientSettings client; or null if
     * there is none. This collector, if specified, always takes precedence over the one specified
     * at the AWS SDK level.
     *
     * @see AwsSdkMetrics
     */
    private final RequestMetricCollector requestMetricCollector;

    private final HttpClientDependencies httpClientDependencies;

    private AmazonHttpClient(HttpClientDependencies httpClientDependencies, RequestMetricCollector requestMetricCollector) {
        this.httpClientDependencies = paramNotNull(httpClientDependencies, "HttpClientDependencies");
        this.requestMetricCollector = requestMetricCollector;
    }

    public static Builder builder() {
        return new Builder();
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
    public void close() throws Exception {
        httpClientDependencies.close();
    }

    /**
     * Package protected for unit-testing.
     */
    @SdkTestInternalApi
    public ClientExecutionTimer getClientExecutionTimer() {
        return this.httpClientDependencies.clientExecutionTimer();
    }

    /**
     * Returns the httpClientSettings client specific request metric collector; or null if there is
     * none.
     */
    public RequestMetricCollector getRequestMetricCollector() {
        return requestMetricCollector;
    }

    /**
     * Check if the thread has been interrupted. If so throw an {@link InterruptedException}.
     * Long running tasks should be periodically checked if the current thread has been
     * interrupted and handle it appropriately
     *
     * @throws InterruptedException If thread has been interrupted
     */
    // TODO address
    public static void checkInterrupted() throws InterruptedException {
        checkInterrupted(null);
    }

    /**
     * Check if the thread has been interrupted. If so throw an {@link InterruptedException}.
     * Long running tasks should be periodically checked if the current thread has been
     * interrupted and handle it appropriately
     *
     * @param response Response to be closed before returning control to the caller to avoid
     *                 leaking the connection.
     * @throws InterruptedException If thread has been interrupted
     */
    // TODO address
    public static void checkInterrupted(Response<?> response) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new SdkInterruptedException(response);
        }
    }

    /**
     * Executes the request and returns the result.
     *
     * @param request              The AmazonWebServices request to send to the remote server
     * @param responseHandler      A response handler to accept a successful response from the
     *                             remote server
     * @param errorResponseHandler A response handler to accept an unsuccessful response from the
     *                             remote server
     * @param executionContext     Additional information about the context of this web service
     *                             call
     * @deprecated Use {@link #requestExecutionBuilder()} to configure and execute a HTTP request.
     */
    @Deprecated
    public <T> T execute(Request<?> request,
                         HttpResponseHandler<AmazonWebServiceResponse<T>> responseHandler,
                         HttpResponseHandler<AmazonServiceException> errorResponseHandler,
                         ExecutionContext executionContext) {
        HttpResponseHandler<T> adaptedRespHandler = new AwsResponseHandlerAdapter<>(
                getNonNullResponseHandler(responseHandler),
                executionContext.getAwsRequestMetrics());
        return requestExecutionBuilder()
                .request(request)
                .requestConfig(new AmazonWebServiceRequestAdapter(request.getOriginalRequest()))
                .errorResponseHandler(new AwsErrorResponseHandler(errorResponseHandler, executionContext.getAwsRequestMetrics()))
                .executionContext(executionContext)
                .execute(adaptedRespHandler);
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
         * @deprecated by {@link #request(SdkHttpFullRequest)}.
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

        /**
         * Fluent setter for the error response handler
         *
         * @param errorResponseHandler Error response handler
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder errorResponseHandler(
                HttpResponseHandler<? extends SdkBaseException> errorResponseHandler);

        /**
         * Fluent setter for the execution context
         *
         * @param executionContext Execution context
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder executionContext(ExecutionContext executionContext);

        /**
         * Fluent setter for {@link RequestConfig}
         *
         * @param requestConfig Request config object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder requestConfig(RequestConfig requestConfig);

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

    public static class Builder {

        private LegacyClientConfiguration clientConfig;
        private RetryPolicy retryPolicy;
        private RequestMetricCollector requestMetricCollector;
        private boolean calculateCrc32FromCompressedData;
        private SdkHttpClient sdkHttpClient;

        private Builder() {
        }

        public Builder clientConfiguration(LegacyClientConfiguration clientConfig) {
            this.clientConfig = clientConfig;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder requestMetricCollector(RequestMetricCollector requestMetricCollector) {
            this.requestMetricCollector = requestMetricCollector;
            return this;
        }

        public Builder calculateCrc32FromCompressedData(boolean calculateCrc32FromCompressedData) {
            this.calculateCrc32FromCompressedData = calculateCrc32FromCompressedData;
            return this;
        }

        public Builder sdkHttpClient(SdkHttpClient sdkHttpClient) {
            this.sdkHttpClient = sdkHttpClient;
            return this;
        }

        public AmazonHttpClient build() {
            return new AmazonHttpClient(
                    HttpClientDependencies.builder()
                                          .clientExecutionTimer(new ClientExecutionTimer())
                                          .config(clientConfig)
                                          .retryCapacity(createCapacityManager())
                                          .retryPolicy(resolveRetryPolicy())
                                          .calculateCrc32FromCompressedData(calculateCrc32FromCompressedData)
                                          .sdkHttpClient(sdkHttpClient)
                                          .build(),
                    requestMetricCollector);
        }

        private CapacityManager createCapacityManager() {
            // When enabled, total retry capacity is computed based on retry cost
            // and desired number of retries.
            int throttledRetryMaxCapacity = clientConfig.useThrottledRetries()
                    ? THROTTLED_RETRY_COST * THROTTLED_RETRIES : -1;
            return new CapacityManager(throttledRetryMaxCapacity);
        }

        private RetryPolicy resolveRetryPolicy() {
            return retryPolicy == null ? new RetryPolicyAdapter(clientConfig.getRetryPolicy(), clientConfig) : retryPolicy;
        }
    }

    private static class NoOpResponseHandler<T> implements HttpResponseHandler<T> {
        @Override
        public T handle(HttpResponse response) throws Exception {
            return null;
        }

        @Override
        public boolean needsConnectionLeftOpen() {
            return false;
        }
    }

    private class RequestExecutionBuilderImpl implements RequestExecutionBuilder {

        private SdkHttpFullRequest request;
        private RequestConfig requestConfig;
        private HttpResponseHandler<? extends SdkBaseException> errorResponseHandler;
        private ExecutionContext executionContext = new ExecutionContext();

        @Override
        public RequestExecutionBuilder request(Request<?> request) {
            this.request = SdkHttpFullRequestAdapter.toHttpFullRequest(request);
            return this;
        }

        @Override
        public RequestExecutionBuilder request(SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public RequestExecutionBuilder errorResponseHandler(
                HttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
            this.errorResponseHandler = errorResponseHandler;
            return this;
        }

        @Override
        public RequestExecutionBuilder executionContext(
                ExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        @Override
        public RequestExecutionBuilder requestConfig(RequestConfig requestConfig) {
            this.requestConfig = requestConfig;
            return this;
        }

        @Override
        public <OutputT> OutputT execute(HttpResponseHandler<OutputT> responseHandler) {
            try {
                return RequestPipelineBuilder
                        .first(AttachRequestConfigStage::new) // TODO Remove this after merge with generated s3 client
                        .then(BeforeRequestHandlersStage::new)
                        // Start of mutating request
                        .then(MakeRequestMutable::new)
                        .then(ApplyTransactionIdStage::new)
                        .then(ApplyUserAgentStage::new)
                        .then(MergeCustomHeadersStage::new)
                        .then(MergeCustomQueryParamsStage::new)
                        .then(MoveParametersToBodyStage::new)
                        .then(MakeRequestImmutable::new)
                        // End of mutating request
                        .then(ReportRequestContentLengthStage::new)
                        .then(RequestPipelineBuilder
                                      .first(SigningStage::new)
                                      .then(MakeHttpRequestStage::new)
                                      .then(HttpResponseAdaptingStage::new)
                                      .then(InstrumentHttpResponseContentStage::new)
                                      .then(BeforeUnmarshallingCallbackStage::new)
                                      .then(() -> new HandleResponseStage<>(getNonNullResponseHandler(responseHandler),
                                                                            getNonNullResponseHandler(errorResponseHandler)))
                                      .wrap(ExceptionReportingStage::new)
                                      .wrap(TimerExceptionHandlingStage::new)
                                      .wrap(RetryableStage::new)::build)
                        .wrap(StreamManagingStage::new)
                        .wrap(AfterCallbackStage::new)
                        .wrap(ClientExecutionTimedStage::new)
                        .then(() -> new UnwrapResponseContainer<>())
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
                                          .requestConfig(requestConfig == null ? RequestConfig.NO_OP : requestConfig)
                                          .executionContext(executionContext)
                                          .build();
        }

    }

}
