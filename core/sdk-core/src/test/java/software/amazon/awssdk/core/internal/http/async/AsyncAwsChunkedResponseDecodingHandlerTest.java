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

package software.amazon.awssdk.core.internal.http.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import utils.ValidSdkObjects;

class AsyncAwsChunkedResponseDecodingHandlerTest {

    private static final String ENCODED = "5\r\nhello\r\n6\r\n world\r\n0\r\nx-amz-checksum-crc32:abc==\r\n\r\n";

    private final CapturingHandler delegate = new CapturingHandler();
    private final ExecutionAttributes attributes = new ExecutionAttributes();
    private final ExecutionContext context =
        ExecutionContext.builder()
                        .interceptorContext(InterceptorContext.builder().request(ValidSdkObjects.sdkRequest()).build())
                        .executionAttributes(attributes)
                        .build();
    private final AsyncAwsChunkedResponseDecodingHandler<Void> handler =
        new AsyncAwsChunkedResponseDecodingHandler<>(delegate, context);

    @Test
    void onStream_whenAwsChunkedResponse_delegateReceivesDecodedBodyAndRewrittenHeaders() {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);

        handler.onHeaders(awsChunkedResponse().build());
        handler.onStream(Flowable.just(utf8(ENCODED)));

        assertThat(delegate.headers.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(delegate.headers.firstMatchingHeader("Content-Length")).hasValue("11");
        assertThat(delegate.headers.firstMatchingHeader("x-amz-trailer")).isEmpty();
        assertThat(context.interceptorContext().httpResponse()).isSameAs(delegate.headers);

        assertThat(readAll(delegate.stream)).isEqualTo("hello world");
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS).get())
            .containsEntry("x-amz-checksum-crc32", "abc==");
    }

    @Test
    void onStream_whenDecodingNotEnabled_passesHeadersAndPublisherThrough() {
        SdkHttpFullResponse response = awsChunkedResponse().build();
        Publisher<ByteBuffer> raw = Flowable.just(utf8(ENCODED));

        handler.onHeaders(response);
        handler.onStream(raw);

        assertThat(delegate.headers).isSameAs(response);
        assertThat(delegate.stream).isSameAs(raw);
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS)).isNull();
    }

    @Test
    void trailers_whenBodyNotYetConsumed_throws() {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);

        handler.onHeaders(awsChunkedResponse().build());
        handler.onStream(Flowable.just(utf8(ENCODED)));

        Supplier<?> trailers = attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS);
        assertThatThrownBy(trailers::get)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("only available after");
    }

    @Test
    void onHeaders_whenAwsChunkedIsNotLastEncoding_throws() {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);
        SdkHttpFullResponse response = awsChunkedResponse().putHeader("Content-Encoding", "aws-chunked,gzip").build();

        assertThatThrownBy(() -> handler.onHeaders(response))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("as the last Content-Encoding value");
        assertThat(delegate.headers).isNull();
    }

    @Test
    void onHeaders_whenEarlierAttemptLeftTrailers_clearsThem() {
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS, Collections::emptyMap);

        handler.onHeaders(SdkHttpFullResponse.builder().statusCode(200).build());

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS)).isNull();
    }

    static SdkHttpFullResponse.Builder awsChunkedResponse() {
        return SdkHttpFullResponse.builder()
                                  .statusCode(200)
                                  .putHeader("Content-Encoding", "aws-chunked")
                                  .putHeader("x-amz-decoded-content-length", "11")
                                  .putHeader("x-amz-trailer", "x-amz-checksum-crc32");
    }

    static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    static String readAll(Publisher<ByteBuffer> publisher) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (ByteBuffer buffer : Flowable.fromPublisher(publisher).blockingIterable()) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            out.write(bytes, 0, bytes.length);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    static final class CapturingHandler implements TransformingAsyncResponseHandler<Void> {
        SdkHttpResponse headers;
        Publisher<ByteBuffer> stream;

        @Override
        public CompletableFuture<Void> prepare() {
            return new CompletableFuture<>();
        }

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            this.headers = headers;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            this.stream = stream;
        }

        @Override
        public void onError(Throwable error) {
        }
    }
}
