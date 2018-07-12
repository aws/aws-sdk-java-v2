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

package software.amazon.awssdk.core.eventstream;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Exception thrown during the processing of an event stream.
 */
@SdkPublicApi
public final class EventStreamException extends SdkException {
    private final String errorCode;

    private EventStreamException(Builder builder) {
        super(builder);
        this.errorCode = builder.errorCode();
    }

    /**
     * Error code returned by the service.
     */
    public String errorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "; Error Code: " + errorCode;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Builder interface for {@link EventStreamException}.
     */
    public interface Builder extends SdkException.Builder {

        /**
         * Set the error code for this exception.
         *
         * @param errorCode The error code.
         * @return This object for method chaining.
         */
        Builder errorCode(String errorCode);

        /**
         * @return The exception error code set on this builder.
         */
        String errorCode();

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable t);

        @Override
        EventStreamException build();
    }

    private static class BuilderImpl extends SdkException.BuilderImpl implements Builder {
        private String errorCode;

        @Override
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        @Override
        public String errorCode() {
            return errorCode;
        }

        @Override
        public Builder cause(Throwable t) {
            super.cause(t);
            return this;
        }

        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public EventStreamException build() {
            return new EventStreamException(this);
        }
    }
}
