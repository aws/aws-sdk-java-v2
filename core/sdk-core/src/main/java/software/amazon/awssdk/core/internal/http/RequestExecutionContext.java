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

package software.amazon.awssdk.core.internal.http;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.core.internal.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.Validate;

/**
 * Request scoped dependencies and context for an execution of a request by {@link AmazonSyncHttpClient} or
 * {@link AmazonAsyncHttpClient}.
 * Provided to the {@link RequestPipeline#execute(Object, RequestExecutionContext)} method.
 */
@SdkInternalApi
public final class RequestExecutionContext {
    private static final RequestOverrideConfiguration EMPTY_CONFIG = SdkRequestOverrideConfiguration.builder().build();
    private final SdkHttpContentPublisher requestProvider;
    private final SdkRequest originalRequest;
    private final ExecutionContext executionContext;
    private TimeoutTracker apiCallTimeoutTracker;

    private RequestExecutionContext(Builder builder) {
        this.requestProvider = builder.requestProvider;
        this.originalRequest = Validate.paramNotNull(builder.originalRequest, "originalRequest");
        this.executionContext = Validate.paramNotNull(builder.executionContext, "executionContext");
    }

    /**
     * Create a {@link Builder}, used to create a {@link RequestExecutionContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public SdkHttpContentPublisher requestProvider() {
        return requestProvider;
    }

    /**
     * @return Execution interceptors to hook into execution lifecycle.
     */
    public ExecutionInterceptorChain interceptorChain() {
        return executionContext.interceptorChain();
    }

    public ExecutionAttributes executionAttributes() {
        return executionContext.executionAttributes();
    }

    @ReviewBeforeRelease("We should combine RequestExecutionContext and ExecutionContext. There's no benefit to both of "
                         + "these. Once that's done, this won't be needed.")
    public ExecutionContext executionContext() {
        return executionContext;
    }

    public SdkRequest originalRequest() {
        return originalRequest;
    }

    public RequestOverrideConfiguration requestConfig() {
        return originalRequest.overrideConfiguration()
                              // ugly but needed to avoid capture of capture and creating a type mismatch
                              .map(c -> (RequestOverrideConfiguration) c)
                              .orElse(EMPTY_CONFIG);
    }

    /**
     * @return SignerProvider used to obtain an instance of a {@link Signer}.
     */
    public Signer signer() {
        return executionContext.signer();
    }

    /**
     * @return Tracker task for the {@link TimeoutTracker}.
     */
    public TimeoutTracker apiCallTimeoutTracker() {
        return apiCallTimeoutTracker;
    }

    /**
     * Sets the tracker task for the . Should
     * be called once per request lifecycle.
     */
    public void apiCallTimeoutTracker(TimeoutTracker timeoutTracker) {
        this.apiCallTimeoutTracker = timeoutTracker;
    }


    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    public static final class Builder {

        private SdkHttpContentPublisher requestProvider;
        private SdkRequest originalRequest;
        private ExecutionContext executionContext;

        public Builder requestProvider(SdkHttpContentPublisher requestProvider) {
            this.requestProvider = requestProvider;
            return this;
        }

        public Builder originalRequest(SdkRequest originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }

        public Builder executionContext(ExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        public RequestExecutionContext build() {
            return new RequestExecutionContext(this);
        }
    }
}
