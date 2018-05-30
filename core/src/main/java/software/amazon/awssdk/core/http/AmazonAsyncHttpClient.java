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

import static software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder.async;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.config.SdkAsyncClientConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpAsyncClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipelineBuilder;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AfterExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncExecutionFailureExceptionReportingStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncRetryableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.BeforeTransmissionExecutionInterceptorsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeAsyncHttpRequestStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeRequestImmutable;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeRequestMutable;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomHeadersStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomQueryParamsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MoveParametersToBodyStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.UnwrapResponseContainer;
import software.amazon.awssdk.core.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.core.retry.SdkDefaultRetrySettings;
import software.amazon.awssdk.core.util.CapacityManager;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@ThreadSafe
@SdkInternalApi
@ReviewBeforeRelease("come up with better name")
public class AmazonAsyncHttpClient implements SdkAutoCloseable {
    private final HttpAsyncClientDependencies httpClientDependencies;

    public AmazonAsyncHttpClient(SdkAsyncClientConfiguration configuration) {
        this.httpClientDependencies = HttpAsyncClientDependencies.builder()
                                                                 .clientExecutionTimer(new ClientExecutionTimer())
                                                                 .asyncClientConfiguration(configuration)
                                                                 .capacityManager(createCapacityManager())
                                                                 .build();
    }

    private CapacityManager createCapacityManager() {
        // When enabled, total retry capacity is computed based on retry cost and desired number of retries.
        // TODO: Allow customers to configure throttled retries (https://github.com/aws/aws-sdk-java-v2/issues/17)
        return new CapacityManager(SdkDefaultRetrySettings.RETRY_THROTTLING_COST * SdkDefaultRetrySettings.THROTTLED_RETRIES);
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
         * Fluent setter for {@link SdkHttpRequestProvider}
         *
         * @param requestProvider Request provider object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder requestProvider(SdkHttpRequestProvider requestProvider);

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
                SdkHttpResponseHandler<? extends SdkException> errorResponseHandler);

        /**
         * Fluent setter for the execution context
         *
         * @param executionContext Execution context
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder executionContext(ExecutionContext executionContext);

        /**
         * Fluent setter for {@link SdkRequest}
         *
         * @param originalRequest Request object
         * @return This builder for method chaining.
         */
        RequestExecutionBuilder originalRequest(SdkRequest originalRequest);

        /**
         * Executes the request with the given configuration.
         *
         * @param responseHandler Response handler that outputs the actual result type which is
         *                        preferred going forward.
         * @param <OutputT>       Result type
         * @return Unmarshalled result type.
         */
        <OutputT> CompletableFuture<OutputT> execute(SdkHttpResponseHandler<OutputT> responseHandler);
    }

    private class RequestExecutionBuilderImpl implements RequestExecutionBuilder {

        private SdkHttpRequestProvider requestProvider;
        private SdkHttpFullRequest request;
        private SdkHttpResponseHandler<? extends SdkException> errorResponseHandler;
        private SdkRequest originalRequest;
        private ExecutionContext executionContext;

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
                SdkHttpResponseHandler<? extends SdkException> errorResponseHandler) {
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
        public RequestExecutionBuilder originalRequest(SdkRequest originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }

        @Override
        public <OutputT> CompletableFuture<OutputT> execute(SdkHttpResponseHandler<OutputT> responseHandler) {
            try {
                return RequestPipelineBuilder
                        .firstAsync(RequestPipelineBuilder
                                .firstAsync(MakeRequestMutable::new)
                                .then(ApplyTransactionIdStage::new)
                                .then(ApplyUserAgentStage::new)
                                .then(MergeCustomHeadersStage::new)
                                .then(MergeCustomQueryParamsStage::new)
                                .then(MoveParametersToBodyStage::new)
                                .then(MakeRequestImmutable::new)
                                .then(RequestPipelineBuilder
                                      .firstAsync(SigningStage::new)
                                      .then(BeforeTransmissionExecutionInterceptorsStage::new)
                                      .then(d -> new MakeAsyncHttpRequestStage<>(responseHandler, errorResponseHandler, d))
                                      .wrap(AsyncRetryableStage::new)
                                      ::build)
                                .then(async(() -> new UnwrapResponseContainer<>()))
                                .then(async(() -> new AfterExecutionInterceptorsStage<>()))::build)
                        .wrap(AsyncExecutionFailureExceptionReportingStage::new)
                        .build(httpClientDependencies)
                        .execute(request, createRequestExecutionDependencies());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SdkClientException(e);
            }
        }

        private RequestExecutionContext createRequestExecutionDependencies() {
            return RequestExecutionContext.builder()
                                          .requestProvider(requestProvider)
                                          .originalRequest(originalRequest)
                                          .executionContext(executionContext)
                                          .build();
        }

    }

}
