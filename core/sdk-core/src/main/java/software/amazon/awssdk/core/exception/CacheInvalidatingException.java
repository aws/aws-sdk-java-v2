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
import software.amazon.awssdk.utils.cache.CacheInvalidatingError;

/**
 * An exception that signals a non-recoverable credential refresh failure.
 * When thrown by a credential provider's refresh function, the caching layer
 * will propagate this exception immediately to the caller without applying
 * refresh backoff or extending cached credential expiration.
 *
 * <p>This is used for errors where the credential source has definitively
 * indicated that the current authentication state is invalid and requires
 * user intervention (e.g., expired SSO tokens, changed user credentials).</p>
 */
@SdkPublicApi
public final class CacheInvalidatingException extends SdkClientException implements CacheInvalidatingError {

    private CacheInvalidatingException(Builder builder) {
        super(builder);
    }

    public static CacheInvalidatingException create(String message) {
        return builder().message(message).build();
    }

    public static CacheInvalidatingException create(String message, Throwable cause) {
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
        Builder writableStackTrace(Boolean writableStackTrace);

        @Override
        Builder numAttempts(Integer numAttempts);

        @Override
        CacheInvalidatingException build();
    }

    protected static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {

        protected BuilderImpl() {
        }

        protected BuilderImpl(CacheInvalidatingException ex) {
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
        public Builder writableStackTrace(Boolean writableStackTrace) {
            this.writableStackTrace = writableStackTrace;
            return this;
        }

        @Override
        public Builder numAttempts(Integer numAttempts) {
            this.numAttempts = numAttempts;
            return this;
        }

        @Override
        public CacheInvalidatingException build() {
            return new CacheInvalidatingException(this);
        }
    }
}
