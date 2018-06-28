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

package software.amazon.awssdk.regions.internal.util;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Parameters that are used in {@link ResourcesEndpointRetryPolicy}.
 */
@SdkInternalApi
public class ResourcesEndpointRetryParameters {

    private final Integer statusCode;

    private final Exception exception;

    private ResourcesEndpointRetryParameters(Builder builder) {
        this.statusCode = builder.statusCode;
        this.exception = builder.exception;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Exception getException() {
        return exception;
    }

    public static class Builder {

        private final Integer statusCode;

        private final Exception exception;

        private Builder() {
            this.statusCode = null;
            this.exception = null;
        }

        private Builder(Integer statusCode, Exception exception) {
            this.statusCode = statusCode;
            this.exception = exception;
        }

        /**
         * @param statusCode The status code from Http response.
         *
         * @return This object for method chaining.
         */
        public Builder withStatusCode(Integer statusCode) {
            return new Builder(statusCode, this.exception);
        }

        /**
         *
         * @param exception The exception that was thrown.
         * @return This object for method chaining.
         */
        public Builder withException(Exception exception) {
            return new Builder(this.statusCode, exception);
        }

        public ResourcesEndpointRetryParameters build() {
            return new ResourcesEndpointRetryParameters(this);
        }

    }
}
