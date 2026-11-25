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

package software.amazon.awssdk.core.internal.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.async.AwsChunkedResponseDecodingPublisher;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import utils.ValidSdkObjects;

/**
 * Verifies the order and effect of the async response interceptor chain built by {@link BaseAsyncClientHandler}.
 */
class BaseAsyncClientHandlerResponseInterceptorChainTest {

    private static final Publisher<ByteBuffer> ORIGINAL_BODY = Flowable.just(utf8("original"));
    private static final Publisher<ByteBuffer> MODIFIED_BODY = Flowable.just(utf8("modified"));

    @Test
    void responseChain_whenInterceptorModifiesResponse_afterTransmissionSeesOriginalAndDelegateSeesModified() {
        RecordingInterceptor interceptor = new RecordingInterceptor(true);
        CapturingHandler delegate = new CapturingHandler();

        run(interceptor, delegate);

        assertThat(interceptor.calls)
            .containsExactly("afterTransmission", "modifyHttpResponse", "beforeUnmarshalling", "modifyAsyncHttpResponse");
        assertThat(interceptor.afterTransmissionResponse.firstMatchingHeader("x-test")).hasValue("original");
        assertThat(interceptor.beforeUnmarshallingResponse.firstMatchingHeader("x-test")).hasValue("modified");
        assertThat(interceptor.modifyAsyncHttpResponsePublisher).isSameAs(ORIGINAL_BODY);
        assertThat(delegate.headers.firstMatchingHeader("x-test")).hasValue("modified");
        assertThat(delegate.stream).isSameAs(MODIFIED_BODY);
    }

    @Test
    void responseChain_whenInterceptorDoesNotModifyResponse_delegateSeesOriginal() {
        CapturingHandler delegate = new CapturingHandler();

        run(new RecordingInterceptor(false), delegate);

        assertThat(delegate.headers.firstMatchingHeader("x-test")).hasValue("original");
        assertThat(delegate.stream).isSameAs(ORIGINAL_BODY);
    }

    @Test
    void responseChain_whenAwsChunkedDecodingEnabled_afterTransmissionSeesRawAndModifyingInterceptorsSeeDecoded() {
        RecordingInterceptor interceptor = new RecordingInterceptor(false);
        CapturingHandler delegate = new CapturingHandler();
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_DECODING, true);
        SdkHttpFullResponse response = SdkHttpFullResponse.builder()
                                                          .statusCode(200)
                                                          .putHeader("Content-Encoding", "aws-chunked")
                                                          .putHeader("x-amz-decoded-content-length", "11")
                                                          .putHeader("x-amz-trailer", "x-amz-checksum-crc32")
                                                          .build();
        String encoded = "5\r\nhello\r\n6\r\n world\r\n0\r\nx-amz-checksum-crc32:abc==\r\n\r\n";

        TransformingAsyncResponseHandler<Void> chain =
            BaseAsyncClientHandler.interceptorCallingResponseHandler(delegate, context(interceptor, attributes));
        chain.onHeaders(response);
        chain.onStream(Flowable.just(utf8(encoded)));

        assertThat(interceptor.afterTransmissionResponse.firstMatchingHeader("Content-Encoding")).hasValue("aws-chunked");
        assertThat(interceptor.beforeUnmarshallingResponse.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(interceptor.beforeUnmarshallingResponse.firstMatchingHeader("Content-Length")).hasValue("11");
        assertThat(interceptor.modifyAsyncHttpResponsePublisher).isInstanceOf(AwsChunkedResponseDecodingPublisher.class);
        assertThat(delegate.headers.firstMatchingHeader("Content-Encoding")).isEmpty();
        assertThat(readAll(delegate.stream)).isEqualTo("hello world");
    }

    @Test
    void responseChain_whenModifyHttpResponseThrows_afterTransmissionRanAndExceptionPropagates() {
        RuntimeException failure = new RuntimeException("modifyHttpResponse boom");
        ThrowingInterceptor interceptor = new ThrowingInterceptor(failure);
        CapturingHandler delegate = new CapturingHandler();

        ExecutionContext context = contextFor(interceptor);
        TransformingAsyncResponseHandler<Void> chain =
            BaseAsyncClientHandler.interceptorCallingResponseHandler(delegate, context);

        // afterTransmission (outer handler) runs before modifyHttpResponse (inner handler) throws on the same onHeaders call.
        assertThatThrownBy(() -> chain.onHeaders(httpResponse()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("modifyHttpResponse boom");
        assertThat(interceptor.afterTransmissionRan).isTrue();
        assertThat(delegate.headers).isNull();
        // The context stored by afterTransmission, which failure handling reads, holds the received response
        assertThat(context.interceptorContext().httpResponse().firstMatchingHeader("x-test")).hasValue("original");
    }

    @Test
    void responseChain_whenOnError_propagatesThroughBothWrappersToDelegate() {
        RuntimeException error = new RuntimeException("stream failure");
        CapturingHandler delegate = new CapturingHandler();

        TransformingAsyncResponseHandler<Void> chain =
            BaseAsyncClientHandler.interceptorCallingResponseHandler(delegate, contextFor(new RecordingInterceptor(false)));
        chain.onError(error);

        assertThat(delegate.error).isSameAs(error);
    }

    private static void run(ExecutionInterceptor interceptor, CapturingHandler delegate) {
        TransformingAsyncResponseHandler<Void> chain =
            BaseAsyncClientHandler.interceptorCallingResponseHandler(delegate, contextFor(interceptor));
        chain.onHeaders(httpResponse());
        chain.onStream(ORIGINAL_BODY);
    }

    private static ExecutionContext contextFor(ExecutionInterceptor interceptor) {
        return context(interceptor, new ExecutionAttributes());
    }

    private static SdkHttpFullResponse httpResponse() {
        return SdkHttpFullResponse.builder().statusCode(200).putHeader("x-test", "original").build();
    }

    private static ExecutionContext context(ExecutionInterceptor interceptor, ExecutionAttributes attributes) {
        return ExecutionContext.builder()
                               .interceptorChain(new ExecutionInterceptorChain(Collections.singletonList(interceptor)))
                               .interceptorContext(InterceptorContext.builder().request(ValidSdkObjects.sdkRequest()).build())
                               .executionAttributes(attributes)
                               .build();
    }

    private static String readAll(Publisher<ByteBuffer> publisher) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (ByteBuffer buffer : Flowable.fromPublisher(publisher).blockingIterable()) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            out.write(bytes, 0, bytes.length);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private static final class CapturingHandler implements TransformingAsyncResponseHandler<Void> {
        private SdkHttpResponse headers;
        private Publisher<ByteBuffer> stream;
        private Throwable error;

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
            this.error = error;
        }
    }

    private static final class RecordingInterceptor implements ExecutionInterceptor {
        private final boolean modify;
        private final List<String> calls = new ArrayList<>();
        private SdkHttpResponse afterTransmissionResponse;
        private SdkHttpResponse beforeUnmarshallingResponse;
        private Publisher<ByteBuffer> modifyAsyncHttpResponsePublisher;

        private RecordingInterceptor(boolean modify) {
            this.modify = modify;
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            calls.add("afterTransmission");
            afterTransmissionResponse = context.httpResponse();
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                  ExecutionAttributes executionAttributes) {
            calls.add("modifyHttpResponse");
            return modify ? context.httpResponse().toBuilder().putHeader("x-test", "modified").build()
                          : context.httpResponse();
        }

        @Override
        public void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {
            calls.add("beforeUnmarshalling");
            beforeUnmarshallingResponse = context.httpResponse();
        }

        @Override
        public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                             ExecutionAttributes executionAttributes) {
            calls.add("modifyAsyncHttpResponse");
            modifyAsyncHttpResponsePublisher = context.responsePublisher().orElse(null);
            return modify ? Optional.of(MODIFIED_BODY) : context.responsePublisher();
        }
    }

    /**
     * Runs {@code afterTransmission} normally, then throws from {@code modifyHttpResponse}.
     */
    private static final class ThrowingInterceptor implements ExecutionInterceptor {
        private final RuntimeException failure;
        private boolean afterTransmissionRan;

        private ThrowingInterceptor(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            afterTransmissionRan = true;
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                  ExecutionAttributes executionAttributes) {
            throw failure;
        }
    }
}
