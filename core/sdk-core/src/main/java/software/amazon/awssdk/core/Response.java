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

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Response wrapper that indicates success or failure with the associated unmarshalled response object or exception
 * object. This object is used by the core request/response pipeline to pass response metadata alongside the actual
 * deserialized response object through different stages of the pipeline.
 *
 * @param <T> the modelled SDK response type.
 */
@SdkProtectedApi
public final class Response<T> {
    private final Boolean isSuccess;
    private final T response;
    private final SdkException exception;
    private final SdkHttpFullResponse httpResponse;

    private Response(Builder<T> builder) {
        this.isSuccess = builder.isSuccess;
        this.response = builder.response;
        this.exception = builder.exception;
        this.httpResponse = builder.httpResponse;
    }

    /**
     * Returns a newly initialized builder object for a {@link Response}
     * @param <T> Modelled response type.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new builder with initial values pulled from the current object.
     */
    public Builder<T> toBuilder() {
        return new Builder<T>().isSuccess(this.isSuccess)
                               .response(this.response)
                               .exception(this.exception)
                               .httpResponse(this.httpResponse);
    }

    /**
     * The modelled response object returned by the service. If the response was a failure, this value is likely to
     * be null.
     */
    public T response() {
        return response;
    }

    /**
     * The modelled exception returned by the service. If the response was not a failure, this value is likely to
     * be null.
     */
    public SdkException exception() {
        return exception;
    }

    /**
     * The HTTP response that was received by the SDK prior to determining the result.
     */
    public SdkHttpFullResponse httpResponse() {
        return httpResponse;
    }

    /**
     * Indicates whether the result indicates success or failure of the original request. A true value indicates
     * success; a false value indicates failure.
     */
    public Boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Response<?> response1 = (Response<?>) o;

        if (isSuccess != null ? ! isSuccess.equals(response1.isSuccess) : response1.isSuccess != null) {
            return false;
        }
        if (response != null ? ! response.equals(response1.response) : response1.response != null) {
            return false;
        }
        if (exception != null ? ! exception.equals(response1.exception) : response1.exception != null) {
            return false;
        }
        return httpResponse != null ? httpResponse.equals(response1.httpResponse) : response1.httpResponse == null;
    }

    @Override
    public int hashCode() {
        int result = isSuccess != null ? isSuccess.hashCode() : 0;
        result = 31 * result + (response != null ? response.hashCode() : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        result = 31 * result + (httpResponse != null ? httpResponse.hashCode() : 0);
        return result;
    }

    public static final class Builder<T> {
        private Boolean isSuccess;
        private T response;
        private SdkException exception;
        private SdkHttpFullResponse httpResponse;

        private Builder() {
        }

        /**
         * Indicates whether the result indicates success or failure of the original request. A true value indicates
         * success; a false value indicates failure.
         */
        public Builder<T> isSuccess(Boolean success) {
            isSuccess = success;
            return this;
        }

        /**
         * The modelled response object returned by the service. If the response was a failure, this value is likely to
         * be null.
         */
        public Builder<T> response(T response) {
            this.response = response;
            return this;
        }

        /**
         * The modelled exception returned by the service. If the response was not a failure, this value is likely to
         * be null.
         */
        public Builder<T> exception(SdkException exception) {
            this.exception = exception;
            return this;
        }

        /**
         * The HTTP response that was received by the SDK prior to determining the result.
         */
        public Builder<T> httpResponse(SdkHttpFullResponse httpResponse) {
            this.httpResponse = httpResponse;
            return this;
        }

        /**
         * Builds a {@link Response} object based on the values held by this builder.
         */
        public Response<T> build() {
            return new Response<>(this);
        }
    }
}
