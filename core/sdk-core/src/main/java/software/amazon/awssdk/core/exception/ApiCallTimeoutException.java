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

package software.amazon.awssdk.core.exception;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

/**
 * Signals that an api call could not complete within the specified timeout.
 *
 * @see ClientOverrideConfiguration#apiCallTimeout()
 */
@SdkPublicApi
public final class ApiCallTimeoutException extends SdkClientException {

    private static final long serialVersionUID = 1L;

    private ApiCallTimeoutException(Builder b) {
        super(b);
    }

    public static ApiCallTimeoutException create(long timeout) {
        return builder().message(String.format("Client execution did not complete before the specified timeout configuration: "
                                               + "%s millis", timeout))
                        .build();
    }

    public static ApiCallTimeoutException create(String message, Throwable cause) {
        return builder().message(message).cause(cause).build();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkClientException.Builder {
        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        ApiCallTimeoutException build();
    }

    protected static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {

        protected BuilderImpl() {
        }

        protected BuilderImpl(ApiCallTimeoutException ex) {
            super(ex);
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public ApiCallTimeoutException build() {
            return new ApiCallTimeoutException(this);
        }
    }
}
