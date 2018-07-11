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

package software.amazon.awssdk.core.exception;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Extension of {@link SdkException} that can be used by clients to
 * explicitly have an exception not retried. This exception will never be
 * thrown by the SDK unless explicitly used by the client.
 *
 * See {@link RetryableException} for marking retryable exceptions.
 */
@SdkPublicApi
public final class NonRetryableException extends SdkException {

    protected NonRetryableException(Builder b) {
        super(b);
    }

    @Override
    public boolean retryable() {
        return false;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkException.Builder {
        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        NonRetryableException build();
    }

    protected static final class BuilderImpl extends SdkException.BuilderImpl implements Builder {

        protected BuilderImpl() {}

        protected BuilderImpl(NonRetryableException ex) {
            super(ex);
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public String message() {
            return message;
        }

        @Override
        public Throwable cause() {
            return cause;
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public NonRetryableException build() {
            return new NonRetryableException(this);
        }
    }
}
