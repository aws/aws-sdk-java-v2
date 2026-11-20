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

package software.amazon.awssdk.auth.credentials;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Signals that the {@link CredentialDaemonCredentialsProvider} could not get credentials from the local credential daemon.
 *
 * <p>
 * {@link #errorCode()} returns one of the error code constants of this class, so a caller can tell the failures apart without
 * parsing the message.
 */
@SdkPublicApi
public final class CredentialDaemonException extends SdkClientException {
    /**
     * The credential daemon could not be reached, or it did not answer in time.
     */
    public static final String NOT_RUNNING = "NOT_RUNNING";

    /**
     * The credential daemon holds the credential, but the user must sign in again.
     */
    public static final String REAUTH_REQUIRED = "REAUTH_REQUIRED";

    /**
     * The credential daemon has no credential with the requested name for this user.
     */
    public static final String CREDENTIAL_NOT_FOUND = "CREDENTIAL_NOT_FOUND";

    /**
     * The credential daemon rejected the request for another reason.
     */
    public static final String REQUEST_REJECTED = "REQUEST_REJECTED";

    /**
     * The credential daemon returned a type of credential material that this version of the SDK does not support.
     */
    public static final String UNSUPPORTED_MATERIAL = "UNSUPPORTED_MATERIAL";

    /**
     * The credential daemon returned a response that does not follow the protocol.
     */
    public static final String PROTOCOL_ERROR = "PROTOCOL_ERROR";

    /**
     * The configuration of the credential daemon provider, or of the profile that uses it, is not valid.
     */
    public static final String INVALID_CONFIG = "INVALID_CONFIG";

    /**
     * The platform cannot reach the credential daemon. It needs Linux or macOS and Java 16 or later.
     */
    public static final String UNSUPPORTED_PLATFORM = "UNSUPPORTED_PLATFORM";

    private final String errorCode;

    private CredentialDaemonException(BuilderImpl builder) {
        super(builder);
        this.errorCode = builder.errorCode;
    }

    /**
     * Returns the code that identifies the failure, one of the error code constants of this class.
     */
    public String errorCode() {
        return errorCode;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Returns a new builder for a {@link CredentialDaemonException}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * A builder for a {@link CredentialDaemonException}.
     */
    public interface Builder extends SdkClientException.Builder {
        /**
         * Sets the code that identifies the failure, one of the error code constants of {@link CredentialDaemonException}.
         */
        Builder errorCode(String errorCode);

        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        CredentialDaemonException build();
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private String errorCode;

        private BuilderImpl() {
        }

        private BuilderImpl(CredentialDaemonException ex) {
            super(ex);
            this.errorCode = ex.errorCode;
        }

        @Override
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        @Override
        public Builder message(String message) {
            super.message(message);
            return this;
        }

        @Override
        public Builder cause(Throwable cause) {
            super.cause(cause);
            return this;
        }

        @Override
        public Builder writableStackTrace(Boolean writableStackTrace) {
            super.writableStackTrace(writableStackTrace);
            return this;
        }

        @Override
        public Builder numAttempts(Integer numAttempts) {
            super.numAttempts(numAttempts);
            return this;
        }

        @Override
        public CredentialDaemonException build() {
            return new CredentialDaemonException(this);
        }
    }
}
