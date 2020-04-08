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

package software.amazon.awssdk.core.retry;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Contains useful information about a failed request that can be used to make retry and backoff decisions. See {@link
 * RetryPolicy}.
 */
@Immutable
@SdkPublicApi
public final class RetryPolicyContext implements ToCopyableBuilder<RetryPolicyContext.Builder, RetryPolicyContext> {

    private final SdkRequest originalRequest;
    private final SdkHttpFullRequest request;
    private final SdkException exception;
    private final ExecutionAttributes executionAttributes;
    private final int retriesAttempted;
    private final Integer httpStatusCode;

    private RetryPolicyContext(Builder builder) {
        this.originalRequest = builder.originalRequest;
        this.request = builder.request;
        this.exception = builder.exception;
        this.executionAttributes = builder.executionAttributes;
        this.retriesAttempted = builder.retriesAttempted;
        this.httpStatusCode = builder.httpStatusCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The original request passed to the client method for an operation.
     */
    public SdkRequest originalRequest() {
        return this.originalRequest;
    }

    /**
     * @return The marshalled request.
     */
    public SdkHttpFullRequest request() {
        return this.request;
    }

    /**
     * @return The last seen exception for the request.
     */
    public SdkException exception() {
        return this.exception;
    }

    /**
     * @return Mutable execution context.
     */
    public ExecutionAttributes executionAttributes() {
        return this.executionAttributes;
    }

    /**
     * @return Number of retries attempted thus far.
     */
    public int retriesAttempted() {
        return this.retriesAttempted;
    }

    /**
     * @return The total number of requests made thus far.
     */
    public int totalRequests() {
        return this.retriesAttempted + 1;
    }

    /**
     * @return HTTP status code of response. May be null if no response was received from the service.
     */
    public Integer httpStatusCode() {
        return this.httpStatusCode;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @SdkPublicApi
    public static final class Builder implements CopyableBuilder<Builder, RetryPolicyContext> {
        private SdkRequest originalRequest;
        private SdkHttpFullRequest request;
        private SdkException exception;
        private ExecutionAttributes executionAttributes;
        private int retriesAttempted;
        private Integer httpStatusCode;

        private Builder() {
        }

        private Builder(RetryPolicyContext copy) {
            this.originalRequest = copy.originalRequest;
            this.request = copy.request;
            this.exception = copy.exception;
            this.executionAttributes = copy.executionAttributes;
            this.retriesAttempted = copy.retriesAttempted;
            this.httpStatusCode = copy.httpStatusCode;
        }

        public Builder originalRequest(SdkRequest originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }

        public Builder request(SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        public Builder exception(SdkException exception) {
            this.exception = exception;
            return this;
        }

        public Builder executionAttributes(ExecutionAttributes executionAttributes) {
            this.executionAttributes = executionAttributes;
            return this;
        }

        public Builder retriesAttempted(int retriesAttempted) {
            this.retriesAttempted = retriesAttempted;
            return this;
        }

        public Builder httpStatusCode(Integer httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        public RetryPolicyContext build() {
            return new RetryPolicyContext(this);
        }

    }

}
