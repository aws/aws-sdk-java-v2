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

import static software.amazon.awssdk.http.pipeline.RequestPipelineBuilder.async;
import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.pipeline.RequestPipelineBuilder;
import software.amazon.awssdk.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.http.pipeline.stages.AsyncRetryableStage;
import software.amazon.awssdk.http.pipeline.stages.BeforeRequestHandlersStage;
import software.amazon.awssdk.http.pipeline.stages.MakeAsyncHttpRequestStage;
import software.amazon.awssdk.http.pipeline.stages.MakeRequestImmutable;
import software.amazon.awssdk.http.pipeline.stages.MakeRequestMutable;
import software.amazon.awssdk.http.pipeline.stages.MergeCustomHeadersStage;
import software.amazon.awssdk.http.pipeline.stages.MergeCustomQueryParamsStage;
import software.amazon.awssdk.http.pipeline.stages.MoveParametersToBodyStage;
import software.amazon.awssdk.http.pipeline.stages.ReportRequestContentLengthStage;
import software.amazon.awssdk.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.http.pipeline.stages.UnwrapResponseContainer;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicyAdapter;
import software.amazon.awssdk.retry.v2.RetryPolicy;
import software.amazon.awssdk.util.CapacityManager;

@ThreadSafe
@SdkProtectedApi
public class AmazonAsyncHttpClient implements AutoCloseable {

    /**
     * A request metric collector used specifically for this httpClientSettings client; or null if
     * there is none. This collector, if specified, always takes precedence over the one specified
     * at the AWS SDK level.
     *
     * @see AwsSdkMetrics
     */
    private final RequestMetricCollector requestMetricCollector;

    private final HttpClientDependencies httpClientDependencies;

    private AmazonAsyncHttpClient(HttpClientDependencies httpClientDependencies, RequestMetricCollector requestMetricCollector) {
        this.httpClientDependencies = paramNotNull(httpClientDependencies, "HttpClientDependencies");
        this.requestMetricCollector = requestMetricCollector;
    }

    public static Builder builder() {
        return new Builder();
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
     * Returns the httpClientSettings client specific request metric collector; or null if there is
     * none.
     */
    public RequestMetricCollector getRequestMetricCollector() {
        return requestMetricCollector;
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
         * @param requestProvider Request provider object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder requestProvider(SdkHttpRequestProvider requestProvider);

        /**
         * Fluent setter for {@link Request}
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
                SdkHttpResponseHandler<? extends SdkBaseException> errorResponseHandler);

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
        <OutputT> CompletableFuture<OutputT> execute(SdkHttpResponseHandler<OutputT> responseHandler);

        /**
         * Executes the request with the given configuration; not handling response.
         *
         * @return Void response
         */
        CompletableFuture<Response<Void>> execute();

    }

    public static class Builder {

        private LegacyClientConfiguration clientConfig;
        private RetryPolicy retryPolicy;
        private RequestMetricCollector requestMetricCollector;
        private boolean calculateCrc32FromCompressedData;
        private SdkAsyncHttpClient sdkAsyncHttpClient;
        private ScheduledExecutorService executorService;

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

        public Builder sdkAsyncHttpClient(SdkAsyncHttpClient sdkAsyncHttpClient) {
            this.sdkAsyncHttpClient = sdkAsyncHttpClient;
            return this;
        }

        public Builder asyncExecutor(ScheduledExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public AmazonAsyncHttpClient build() {
            return new AmazonAsyncHttpClient(
                    HttpClientDependencies.builder()
                                          .clientExecutionTimer(new ClientExecutionTimer())
                                          .config(clientConfig)
                                          .retryCapacity(createCapacityManager())
                                          .retryPolicy(resolveRetryPolicy())
                                          .calculateCrc32FromCompressedData(calculateCrc32FromCompressedData)
                                          .sdkAsyncHttpClient(sdkAsyncHttpClient)
                                          .asyncExecutorService(executorService)
                                          .build(),
                    requestMetricCollector);
        }

        private CapacityManager createCapacityManager() {
            // When enabled, total retry capacity is computed based on retry cost
            // and desired number of retries.
            int throttledRetryMaxCapacity = clientConfig.useThrottledRetries()
                    ? AmazonHttpClient.THROTTLED_RETRY_COST * AmazonHttpClient.THROTTLED_RETRIES : -1;
            return new CapacityManager(throttledRetryMaxCapacity);
        }

        private RetryPolicy resolveRetryPolicy() {
            return retryPolicy == null ? new RetryPolicyAdapter(clientConfig.getRetryPolicy(), clientConfig) : retryPolicy;
        }
    }

    private class RequestExecutionBuilderImpl implements RequestExecutionBuilder {

        private SdkHttpRequestProvider requestProvider;
        private SdkHttpFullRequest request;
        private RequestConfig requestConfig;
        private SdkHttpResponseHandler<? extends SdkBaseException> errorResponseHandler;
        private ExecutionContext executionContext = new ExecutionContext();

        @Override
        public RequestExecutionBuilder requestProvider(SdkHttpRequestProvider requestProvider) {
            this.requestProvider = requestProvider;
            return this;
        }

        @Override
        public RequestExecutionBuilder request(SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public RequestExecutionBuilder errorResponseHandler(
                SdkHttpResponseHandler<? extends SdkBaseException> errorResponseHandler) {
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
        public <OutputT> CompletableFuture<OutputT> execute(SdkHttpResponseHandler<OutputT> responseHandler) {
            try {
                return RequestPipelineBuilder
                        .first(BeforeRequestHandlersStage::new)
                        .then(MakeRequestMutable::new)
                        .then(ApplyTransactionIdStage::new)
                        .then(ApplyUserAgentStage::new)
                        .then(MergeCustomHeadersStage::new)
                        .then(MergeCustomQueryParamsStage::new)
                        .then(MoveParametersToBodyStage::new)
                        .then(MakeRequestImmutable::new)
                        .then(ReportRequestContentLengthStage::new)
                        .then(RequestPipelineBuilder
                                      .first(SigningStage::new)
                                      .then(d -> new MakeAsyncHttpRequestStage<>(responseHandler, errorResponseHandler, d))
                                      // TODO BeforeUnmarshallingStage
                                      .wrap(AsyncRetryableStage::new)
                                      ::build)
                        .then(async(() -> new UnwrapResponseContainer<>()))
                        .build(httpClientDependencies)
                        .execute(request, createRequestExecutionDependencies());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SdkClientException(e);
            }
        }

        @Override
        public CompletableFuture<Response<Void>> execute() {
            return execute(null);
        }

        private RequestExecutionContext createRequestExecutionDependencies() {
            return RequestExecutionContext.builder()
                                          .requestProvider(requestProvider)
                                          .requestConfig(requestConfig)
                                          .executionContext(executionContext)
                                          .build();
        }

    }

}
