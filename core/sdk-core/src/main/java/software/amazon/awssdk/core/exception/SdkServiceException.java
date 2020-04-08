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

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.HttpStatusCode;

/**
 * Extension of SdkException that represents an error response returned by
 * the requested downstream service. Receiving an exception of this type indicates that
 * the caller's request was correctly transmitted to the service, but for some
 * reason, the service was not able to process it, and returned an error
 * response instead.
 * <p>
 * Exceptions that extend {@link SdkServiceException} are assumed to be able to be
 * successfully retried.
 * <p>
 * SdkServiceException provides callers several pieces of information that can
 * be used to obtain more information about the error and why it occurred.
 *
 * @see SdkClientException
 */
@SdkPublicApi
public class SdkServiceException extends SdkException implements SdkPojo {

    private final String requestId;
    private final int statusCode;

    protected SdkServiceException(Builder b) {
        super(b);
        this.requestId = b.requestId();
        this.statusCode = b.statusCode();
    }

    /**
     * The requestId that was returned by the called service.
     * @return String containing the requestId
     */
    public String requestId() {
        return requestId;
    }

    /**
     * The status code that was returned by the called service.
     * @return int containing the status code.
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Specifies whether or not an exception may have been caused by clock skew.
     */
    public boolean isClockSkewException() {
        return false;
    }

    /**
     * Specifies whether or not an exception is caused by throttling.
     *
     * @return true if the status code is 429, otherwise false.
     */
    public boolean isThrottlingException() {
        return statusCode == HttpStatusCode.THROTTLING;
    }

    /**
     * @return {@link Builder} instance to construct a new {@link SdkServiceException}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create a {@link SdkServiceException.Builder} initialized with the properties of this {@code SdkServiceException}.
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

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    public interface Builder extends SdkException.Builder, SdkPojo {
        Builder message(String message);

        Builder cause(Throwable cause);

        /**
         * Specifies the requestId returned by the called service.
         *
         * @param requestId A string that identifies the request made to a service.
         * @return This object for method chaining.
         */
        Builder requestId(String requestId);

        /**
         * The requestId returned by the called service.
         *
         * @return String containing the requestId
         */
        String requestId();

        /**
         * Specifies the status code returned by the service.
         *
         * @param statusCode an int containing the status code returned by the service.
         * @return This method for object chaining.
         */
        Builder statusCode(int statusCode);

        /**
         * The status code returned by the service.
         * @return int containing the status code
         */
        int statusCode();

        /**
         * Creates a new {@link SdkServiceException} with the specified properties.
         *
         * @return The new {@link SdkServiceException}.
         */
        @Override
        SdkServiceException build();
    }

    protected static class BuilderImpl extends SdkException.BuilderImpl implements Builder {

        protected String requestId;
        protected int statusCode;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkServiceException ex) {
            super(ex);
            this.requestId = ex.requestId();
            this.statusCode = ex.statusCode();
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
        public String requestId() {
            return requestId;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public SdkServiceException build() {
            return new SdkServiceException(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }
    }
}
