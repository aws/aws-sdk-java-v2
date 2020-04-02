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

import static software.amazon.awssdk.http.Header.CONTENT_TYPE;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.transform.AbstractStreamingRequestMarshaller;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Augments a {@link Marshaller} to add contents for a streamed request.
 *
 * @param <T> Type of POJO being marshalled.
 */
@SdkProtectedApi
public final class StreamingRequestMarshaller<T> extends AbstractStreamingRequestMarshaller<T> {

    private final RequestBody requestBody;

    private StreamingRequestMarshaller(Builder builder) {
        super(builder);
        this.requestBody = builder.requestBody;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public SdkHttpFullRequest marshall(T in) {
        SdkHttpFullRequest.Builder marshalled = delegateMarshaller.marshall(in).toBuilder();
        marshalled.contentStreamProvider(requestBody.contentStreamProvider());
        String contentType = marshalled.firstMatchingHeader(CONTENT_TYPE)
                                       .orElse(null);
        if (StringUtils.isEmpty(contentType)) {
            marshalled.putHeader(CONTENT_TYPE, requestBody.contentType());
        }

        // Currently, SDK always require content length in RequestBody. So we always
        // send Content-Length header for sync APIs
        // This change will be useful if SDK relaxes the content-length requirement in RequestBody
        addHeaders(marshalled, Optional.of(requestBody.contentLength()), requiresLength, transferEncoding, useHttp2);

        return marshalled.build();
    }

    /**
     * Builder class to build {@link StreamingRequestMarshaller} object.
     */
    public static final class Builder extends AbstractStreamingRequestMarshaller.Builder<Builder> {
        private RequestBody requestBody;

        /**
         * @param requestBody {@link RequestBody} representing the HTTP payload
         * @return This object for method chaining
         */
        public Builder requestBody(RequestBody requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public <T> StreamingRequestMarshaller<T> build() {
            return new StreamingRequestMarshaller<>(this);
        }
    }
}
