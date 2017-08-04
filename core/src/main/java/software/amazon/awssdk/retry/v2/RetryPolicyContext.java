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

package software.amazon.awssdk.retry.v2;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Contains useful information about a failed request that can be used to make retry and backoff decisions. See {@link
 * RetryPolicy}.
 */
@Immutable
public class RetryPolicyContext {

    private final Object originalRequest;
    private final SdkHttpFullRequest request;
    private final SdkBaseException exception;
    private final int retriesAttempted;
    private final Integer httpStatusCode;

    private RetryPolicyContext(Object originalRequest,
                               SdkHttpFullRequest request,
                               SdkBaseException exception,
                               int retriesAttempted,
                               Integer httpStatusCode) {
        this.originalRequest = originalRequest;
        this.request = request;
        this.exception = exception;
        this.retriesAttempted = retriesAttempted;
        this.httpStatusCode = httpStatusCode;
    }

    @SdkInternalApi
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The original request passed to the client method for an operation.
     */
    public Object originalRequest() {
        return this.originalRequest;
    }

    /**
     * @return The marshalled request. See {@link Request#addHandlerContext(HandlerContextKey, Object)} for a mechanism to store
     *     request level state across invocations of the retry policy.
     */
    public SdkHttpFullRequest request() {
        return this.request;
    }

    /**
     * @return The previous exception (may be a client or a service exception).
     */
    public SdkBaseException exception() {
        return this.exception;
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
        return retriesAttempted() + 1;
    }

    /**
     * @return HTTP status code of response. May be null if no response was received from the service.
     */
    public Integer httpStatusCode() {
        return this.httpStatusCode;
    }

    @SdkInternalApi
    public static class Builder {

        private Object originalRequest;
        private SdkHttpFullRequest request;
        private SdkBaseException exception;
        private int retriesAttempted;
        private Integer httpStatusCode;

        private Builder() {
        }

        public Builder originalRequest(Object originalRequest) {
            this.originalRequest = originalRequest;
            return this;
        }

        public Builder request(SdkHttpFullRequest request) {
            this.request = request;
            return this;
        }

        public Builder exception(SdkBaseException exception) {
            this.exception = exception;
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
            return new RetryPolicyContext(originalRequest, request, exception, retriesAttempted, httpStatusCode);
        }

    }
}
