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

package software.amazon.awssdk.services.sso.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * <p>
 * The session token that was passed is expired or is not valid.
 * </p>
 */
@SdkPublicApi
public final class ExpiredTokenException extends SdkClientException {

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList());

    private ExpiredTokenException(Builder b) {
        super(b);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends SdkPojo, SdkClientException.Builder {
        @Override
        Builder message(String message);

        @Override
        Builder cause(Throwable cause);

        @Override
        ExpiredTokenException build();
    }

    static final class BuilderImpl extends SdkClientException.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(ExpiredTokenException model) {
            super(model);
        }

        @Override
        public BuilderImpl message(String message) {
            this.message = message;
            return this;
        }

        @Override
        public BuilderImpl cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public ExpiredTokenException build() {
            return new ExpiredTokenException(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
