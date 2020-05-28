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

package software.amazon.awssdk.core.runtime.transform;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.transform.AbstractStreamingRequestMarshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Augments a {@link Marshaller} to add contents for an async streamed request.
 *
 * @param <T> Type of POJO being marshalled.
 */
@SdkProtectedApi
public final class AsyncStreamingRequestMarshaller<T> extends AbstractStreamingRequestMarshaller<T> {

    private final AsyncRequestBody asyncRequestBody;

    private AsyncStreamingRequestMarshaller(Builder builder) {
        super(builder);
        this.asyncRequestBody = builder.asyncRequestBody;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SdkHttpFullRequest marshall(T in) {
        SdkHttpFullRequest.Builder marshalled = delegateMarshaller.marshall(in).toBuilder();

        addHeaders(marshalled, asyncRequestBody.contentLength(), requiresLength, transferEncoding, useHttp2);

        return marshalled.build();
    }

    /**
     * Builder class to build {@link AsyncStreamingRequestMarshaller} object.
     */
    public static final class Builder extends AbstractStreamingRequestMarshaller.Builder<Builder> {

        private AsyncRequestBody asyncRequestBody;

        /**
         * @param asyncRequestBody {@link AsyncRequestBody} representing the HTTP payload
         * @return This object for method chaining
         */
        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
            return this;
        }

        public <T> AsyncStreamingRequestMarshaller<T> build() {
            return new AsyncStreamingRequestMarshaller<>(this);
        }
    }
}
