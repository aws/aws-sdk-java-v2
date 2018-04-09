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

package software.amazon.awssdk.core.interceptor;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link Context.AfterExecution} and its parent interfaces.
 */
@SdkInternalApi
public final class InterceptorContext
        implements Context.AfterExecution,
                   ToCopyableBuilder<InterceptorContext.Builder, InterceptorContext> {
    private final SdkRequest request;
    private final SdkHttpFullRequest httpRequest;
    private final SdkHttpFullResponse httpResponse;
    private final SdkResponse response;

    private InterceptorContext(Builder builder) {
        this.request = Validate.paramNotNull(builder.request, "request");
        this.httpRequest = builder.httpRequest;
        this.httpResponse = builder.httpResponse;
        this.response = builder.response;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public SdkRequest request() {
        return request;
    }

    @Override
    public SdkHttpFullRequest httpRequest() {
        return httpRequest;
    }

    @Override
    public SdkHttpFullResponse httpResponse() {
        return httpResponse;
    }

    @Override
    public SdkResponse response() {
        return response;
    }

    @NotThreadSafe
    @SdkPublicApi
    public static final class Builder implements CopyableBuilder<Builder, InterceptorContext> {
        private SdkRequest request;
        private SdkHttpFullRequest httpRequest;
        private SdkHttpFullResponse httpResponse;
        private SdkResponse response;

        private Builder() {
            super();
        }

        private Builder(InterceptorContext context) {
            this.request = context.request;
            this.httpRequest = context.httpRequest;
            this.httpResponse = context.httpResponse;
            this.response = context.response;
        }

        public Builder request(SdkRequest request) {
            this.request = request;
            return this;
        }

        public Builder httpRequest(SdkHttpFullRequest httpRequest) {
            this.httpRequest = httpRequest;
            return this;
        }

        public Builder httpResponse(SdkHttpFullResponse httpResponse) {
            this.httpResponse = httpResponse;
            return this;
        }

        public Builder response(SdkResponse response) {
            this.response = response;
            return this;
        }

        @Override
        public InterceptorContext build() {
            return new InterceptorContext(this);
        }
    }
}
