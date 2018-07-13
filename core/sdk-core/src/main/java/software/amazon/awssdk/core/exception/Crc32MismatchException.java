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
 * Extension of {@link SdkClientException} that is thrown whenever the
 * client-side computed CRC32 does not match the server-side computed CRC32.
 *
 * This exception will not be retried by the SDK but may be retryable by the client.
 * Retrying may succeed if the mismatch occurred during the transmission of the response
 * over the network or during the write to disk. The SDK does not retry this exception
 * as this may result in additional calls being made that may contain large payloads.
 */
@SdkPublicApi
public final class Crc32MismatchException extends SdkClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CRC32MismatchException with the specified message.
     */
    protected Crc32MismatchException(Builder b) {
        super(b);
    }

    public static Crc32MismatchException create(String message, Throwable cause) {
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
        Crc32MismatchException build();
    }

    protected static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {

        protected BuilderImpl() {}

        protected BuilderImpl(Crc32MismatchException ex) {
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
        public Crc32MismatchException build() {
            return new Crc32MismatchException(this);
        }
    }
}
