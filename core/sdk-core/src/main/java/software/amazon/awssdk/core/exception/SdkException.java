/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.utils.builder.Buildable;

/**
 * Base class for all exceptions thrown by the SDK.
 *
 * @see SdkServiceException
 * @see SdkClientException
 */
@SdkPublicApi
public class SdkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected SdkException(Builder builder) {
        super(builder.message(), builder.cause());
    }

    public static SdkException create(String message, Throwable cause) {
        return SdkException.builder().message(message).cause(cause).build();
    }

    /**
     * Specifies whether or not an exception can be expected to succeed on a retry.
     */
    public boolean retryable() {
        return false;
    }

    /**
     * Create a {@link SdkException.Builder} initialized with the properties of this {@code SdkException}.
     *
     * @return A new builder initialized with this config's properties.
     */
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * @return {@link Builder} instance to construct a new {@link SdkException}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends Buildable {
        /**
         * Specifies the exception that caused this exception to occur.
         *
         * @param cause The exception that caused this exception to occur.
         * @return This object for method chaining.
         */
        Builder cause(Throwable cause);

        /**
         * The exception that caused this exception to occur.
         *
         * @return The exception that caused this exception to occur.
         */
        Throwable cause();

        /**
         * Specifies the details of this exception.
         *
         * @param message The details of this exception.
         * @return This method for object chaining
         */
        Builder message(String message);

        /**
         * The details of this exception.
         *
         * @return Details of this exception.
         */
        String message();

        /**
         * Creates a new {@link SdkException} with the specified properties.
         *
         * @return The new {@link SdkException}.
         */
        @Override
        SdkException build();
    }

    protected static class BuilderImpl implements Builder {

        protected Throwable cause;
        protected String message;

        protected BuilderImpl() {}

        protected BuilderImpl(SdkException ex) {
            this.cause = ex.getCause();
            this.message = ex.getMessage();
        }


        public Throwable getCause() {
            return cause;
        }

        public void setCause(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public Throwable cause() {
            return cause;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }

        public SdkException build() {
            return new SdkException(this);
        }

    }
}
