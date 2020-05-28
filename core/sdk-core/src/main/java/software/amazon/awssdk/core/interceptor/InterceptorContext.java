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

package software.amazon.awssdk.core.interceptor;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link Context.AfterExecution} and its parent interfaces.
 */
@SdkProtectedApi
public final class InterceptorContext
        implements Context.AfterExecution,
                   Context.ModifyHttpRequest,
                   ToCopyableBuilder<InterceptorContext.Builder, InterceptorContext> {
    private final SdkRequest request;
    private final SdkHttpRequest httpRequest;
    private final Optional<RequestBody> requestBody;
    private final SdkHttpResponse httpResponse;
    private final Optional<InputStream> responseBody;
    private final SdkResponse response;
    private final Optional<AsyncRequestBody> asyncRequestBody;
    private final Optional<Publisher<ByteBuffer>> responsePublisher;

    private InterceptorContext(Builder builder) {
        this.request = Validate.paramNotNull(builder.request, "request");
        this.httpRequest = builder.httpRequest;
        this.requestBody = builder.requestBody;
        this.httpResponse = builder.httpResponse;
        this.responseBody = builder.responseBody;
        this.response = builder.response;
        this.asyncRequestBody = builder.asyncRequestBody;
        this.responsePublisher = builder.responsePublisher;
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
    public Optional<RequestBody> requestBody() {
        return requestBody;
    }

    @Override
    public Optional<AsyncRequestBody> asyncRequestBody() {
        return asyncRequestBody;
    }

    @Override
    public Optional<Publisher<ByteBuffer>> responsePublisher() {
        return responsePublisher;
    }

    @Override
    public SdkHttpRequest httpRequest() {
        return httpRequest;
    }

    @Override
    public SdkHttpResponse httpResponse() {
        return httpResponse;
    }

    @Override
    public Optional<InputStream> responseBody() {
        return responseBody;
    }

    @Override
    public SdkResponse response() {
        return response;
    }

    @NotThreadSafe
    @SdkPublicApi
    public static final class Builder implements CopyableBuilder<Builder, InterceptorContext> {
        private SdkRequest request;
        private SdkHttpRequest httpRequest;
        private Optional<RequestBody> requestBody = Optional.empty();
        private SdkHttpResponse httpResponse;
        private Optional<InputStream> responseBody = Optional.empty();
        private SdkResponse response;
        private Optional<AsyncRequestBody> asyncRequestBody = Optional.empty();
        private Optional<Publisher<ByteBuffer>> responsePublisher = Optional.empty();

        private Builder() {
            super();
        }

        private Builder(InterceptorContext context) {
            this.request = context.request;
            this.httpRequest = context.httpRequest;
            this.requestBody = context.requestBody;
            this.httpResponse = context.httpResponse;
            this.responseBody = context.responseBody;
            this.response = context.response;
            this.asyncRequestBody = context.asyncRequestBody;
            this.responsePublisher = context.responsePublisher;
        }

        public Builder request(SdkRequest request) {
            this.request = request;
            return this;
        }

        public Builder httpRequest(SdkHttpRequest httpRequest) {
            this.httpRequest = httpRequest;
            return this;
        }

        public Builder requestBody(RequestBody requestBody) {
            this.requestBody = Optional.ofNullable(requestBody);
            return this;
        }

        public Builder httpResponse(SdkHttpResponse httpResponse) {
            this.httpResponse = httpResponse;
            return this;
        }

        public Builder responseBody(InputStream responseBody) {
            this.responseBody = Optional.ofNullable(responseBody);
            return this;
        }

        public Builder response(SdkResponse response) {
            this.response = response;
            return this;
        }

        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = Optional.ofNullable(asyncRequestBody);
            return this;
        }

        public Builder responsePublisher(Publisher<ByteBuffer> responsePublisher) {
            this.responsePublisher = Optional.ofNullable(responsePublisher);
            return this;
        }

        @Override
        public InterceptorContext build() {
            return new InterceptorContext(this);
        }
    }
}
