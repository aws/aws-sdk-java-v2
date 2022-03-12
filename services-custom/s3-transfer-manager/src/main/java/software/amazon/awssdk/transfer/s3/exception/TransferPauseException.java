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

package software.amazon.awssdk.transfer.s3.exception;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;

@SdkPublicApi
public final class TransferPauseException extends SdkClientException {
    private static final long serialVersionUID = 1L;
    private final ErrorCode errorCode;

    private TransferPauseException(Builder builder) {
        super(builder);
        this.errorCode = builder.errorCode();
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public static TransferPauseException create(ErrorCode error, String message) {
       return builder().errorCode(error).message(message).build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public enum ErrorCode {
        /**
         * Pause is not yet applicable since transfer has not started
         */
        NOT_STARTED,

        /**
         * Pause is not possible since transfer has finished
         */
        ALREADY_FINISHED,

        /**
         * Pause is not possible because a previous pause was requested
         */
        PAUSE_IN_PROGRESS
    }

    public interface Builder extends SdkClientException.Builder {

        Builder errorCode(ErrorCode errorCode);

        @Override
        Builder message(String message);

        ErrorCode errorCode();

        @Override
        TransferPauseException build();
    }

    protected static class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private ErrorCode errorCode;

        @Override
        public Builder errorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        @Override
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public ErrorCode errorCode() {
            return errorCode;
        }

        @Override
        public TransferPauseException build() {
            return new TransferPauseException(this);
        }

    }
}
