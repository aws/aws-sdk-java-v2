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

package software.amazon.awssdk.awscore.exception;

import java.util.Optional;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.internal.AwsErrorCode;
import software.amazon.awssdk.core.exception.SdkServiceException;

/**
 * Extension of {@link SdkServiceException} that represents an error response returned
 * by an Amazon web service.
 * <p>
 * <p>
 * AmazonServiceException provides callers several pieces of
 * information that can be used to obtain more information about the error and
 * why it occurred. In particular, the errorType field can be used to determine
 * if the caller's request was invalid, or the service encountered an error on
 * the server side while processing it.
 *
 * @see SdkServiceException
 */
@SdkPublicApi
public class AwsServiceException extends SdkServiceException {

    private transient AwsErrorDetails awsErrorDetails;

    protected AwsServiceException(Builder b) {
        super(b);
        this.awsErrorDetails = b.awsErrorDetails();
    }

    /**
     * Additional details pertaining to an exception thrown by an AWS service.
     *
     * @return {@link AwsErrorDetails}.
     */
    public AwsErrorDetails awsErrorDetails() {
        return awsErrorDetails;
    }

    @Override
    public String getMessage() {
        if (awsErrorDetails != null) {
            return awsErrorDetails().errorMessage() +
                    " (Service: " + awsErrorDetails().serviceName() +
                    ", Status Code: " + statusCode() +
                    ", Request ID: " + requestId() + ")";
        }

        return super.getMessage();
    }

    @Override
    public boolean isClockSkewException() {
        return Optional.ofNullable(awsErrorDetails)
                .map(a -> AwsErrorCode.CLOCK_SKEW_ERROR_CODES.contains(a.errorCode()))
                .orElse(false);
    }

    @Override
    public boolean isThrottlingException() {
        return super.isThrottlingException() ||
                Optional.ofNullable(awsErrorDetails)
                        .map(a -> AwsErrorCode.THROTTLING_ERROR_CODES.contains(a.errorCode()))
                        .orElse(false);
    }

    /**
     * @return {@link Builder} instance to construct a new {@link AwsServiceException}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create a {@link AwsServiceException.Builder} initialized with the properties of this {@code AwsServiceException}.
     *
     * @return A new builder initialized with this config's properties.
     */
    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    public interface Builder extends SdkServiceException.Builder {

        /**
         * Specifies the additional awsErrorDetails from the service response.
         * @param awsErrorDetails Object containing additional details from the response.
         * @return This object for method chaining.
         */
        Builder awsErrorDetails(AwsErrorDetails awsErrorDetails);

        /**
         * The {@link AwsErrorDetails} from the service response.
         *
         * @return {@link AwsErrorDetails}.
         */
        AwsErrorDetails awsErrorDetails();

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable t);

        @Override
        Builder requestId(String requestId);

        @Override
        Builder statusCode(int statusCode);

        @Override
        AwsServiceException build();
    }

    protected static class BuilderImpl extends SdkServiceException.BuilderImpl implements Builder {

        protected AwsErrorDetails awsErrorDetails;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsServiceException ex) {
            super(ex);
            this.awsErrorDetails = ex.awsErrorDetails();
        }

        @Override
        public Builder awsErrorDetails(AwsErrorDetails awsErrorDetails) {
            this.awsErrorDetails = awsErrorDetails;
            return this;
        }

        @Override
        public AwsErrorDetails awsErrorDetails() {
            return awsErrorDetails;
        }

        public AwsErrorDetails getAwsErrorDetails() {
            return awsErrorDetails;
        }

        public void setAwsErrorDetails(AwsErrorDetails awsErrorDetails) {
            this.awsErrorDetails = awsErrorDetails;
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
        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public AwsServiceException build() {
            return new AwsServiceException(this);
        }
    }
}
