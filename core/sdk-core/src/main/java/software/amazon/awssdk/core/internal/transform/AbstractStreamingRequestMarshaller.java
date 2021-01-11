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

package software.amazon.awssdk.core.internal.transform;

import static software.amazon.awssdk.http.Header.CHUNKED;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public abstract class AbstractStreamingRequestMarshaller<T> implements Marshaller<T> {
    protected final Marshaller<T> delegateMarshaller;
    protected final boolean requiresLength;
    protected final boolean transferEncoding;
    protected final boolean useHttp2;

    protected AbstractStreamingRequestMarshaller(Builder builder) {
        this.delegateMarshaller = builder.delegateMarshaller;
        this.requiresLength = builder.requiresLength;
        this.transferEncoding = builder.transferEncoding;
        this.useHttp2 = builder.useHttp2;
    }

    /**
     * This method will run certain validations for content-length and add
     * additional headers (like Transfer-Encoding) if necessary.
     *
     * If requiresLength and transferEncoding is not set to true and Content Length is missing,
     * SDK is not required to calculate the Content-Length and delegate that behavior to the underlying http client.
     *
     * @param marshalled A mutable builder for {@link SdkHttpFullRequest} representing a HTTP request.
     * @param contentLength Optional of content length
     * @param requiresLength True if Content-Length header is required on the request
     * @param transferEncoding True if "Transfer-Encoding: chunked" header should be set on request
     * @param useHttp2 True if the operation uses http2
     */
    protected final void addHeaders(SdkHttpFullRequest.Builder marshalled,
                                    Optional<Long> contentLength,
                                    boolean requiresLength,
                                    boolean transferEncoding,
                                    boolean useHttp2) {

        if (marshalled.firstMatchingHeader(CONTENT_LENGTH).isPresent()) {
            return;
        }

        if (contentLength.isPresent()) {
            marshalled.putHeader(CONTENT_LENGTH, Long.toString(contentLength.get()));
            return;
        }

        if (requiresLength) {
            throw SdkClientException.create("This API requires Content-Length header to be set. "
                                            + "Please set the content length on the RequestBody.");
        } else if (transferEncoding && !useHttp2) {
            marshalled.putHeader(TRANSFER_ENCODING, CHUNKED);
        }
    }

    protected abstract static class Builder<BuilderT extends Builder> {
        private Marshaller delegateMarshaller;
        private boolean requiresLength = Boolean.FALSE;
        private boolean transferEncoding = Boolean.FALSE;
        private boolean useHttp2 = Boolean.FALSE;

        protected Builder() {
        }

        /**
         * @param delegateMarshaller POJO marshaller (for path/query/header members)
         * @return This object for method chaining
         */
        public BuilderT delegateMarshaller(Marshaller delegateMarshaller) {
            this.delegateMarshaller = delegateMarshaller;
            return (BuilderT) this;
        }


        /**
         * @param requiresLength boolean value indicating if Content-Length header is required in the request
         * @return This object for method chaining
         */
        public BuilderT requiresLength(boolean requiresLength) {
            this.requiresLength = requiresLength;
            return (BuilderT) this;
        }

        /**
         * @param transferEncoding boolean value indicating if Transfer-Encoding: chunked header is required in the request
         * @return This object for method chaining
         */
        public BuilderT transferEncoding(boolean transferEncoding) {
            this.transferEncoding = transferEncoding;
            return (BuilderT) this;
        }

        /**
         * @param useHttp2 boolean value indicating if request uses HTTP 2 protocol
         * @return This object for method chaining
         */
        public BuilderT useHttp2(boolean useHttp2) {
            this.useHttp2 = useHttp2;
            return (BuilderT) this;
        }
    }
}
