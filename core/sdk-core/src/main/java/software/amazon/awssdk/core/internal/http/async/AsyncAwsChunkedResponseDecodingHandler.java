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

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.async.AwsChunkedResponseDecodingPublisher;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedResponseDecodingUtils;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Async response handler decorator that decodes an aws-chunked response body and rewrites its framing headers. Sits
 * between the read-only {@code afterTransmission} interceptors and any interceptor that can modify the response, so those
 * only see the decoded body and headers.
 *
 * @param <T> the type of the result
 */
@SdkInternalApi
public final class AsyncAwsChunkedResponseDecodingHandler<T> implements TransformingAsyncResponseHandler<T> {
    private final TransformingAsyncResponseHandler<T> delegate;
    private final ExecutionContext context;

    /**
     * Set in onHeaders when the body must be decoded, and applied to the body in onStream. Volatile because the two
     * callbacks may run on different threads.
     */
    private volatile UnaryOperator<Publisher<ByteBuffer>> bodyDecoder;

    public AsyncAwsChunkedResponseDecodingHandler(TransformingAsyncResponseHandler<T> delegate, ExecutionContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public void onHeaders(SdkHttpResponse response) {
        bodyDecoder = null;
        ExecutionAttributes executionAttributes = context.executionAttributes();
        // Clear trailers from an earlier attempt, so they are never read for this response
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS, null);
        if (!AwsChunkedResponseDecodingUtils.shouldDecode(response, executionAttributes)) {
            delegate.onHeaders(response);
            return;
        }

        long decodedContentLength = AwsChunkedResponseDecodingUtils.decodedContentLength(response);
        Set<String> declaredTrailers = AwsChunkedResponseDecodingUtils.declaredTrailers(response);
        bodyDecoder = body -> {
            AwsChunkedResponseDecodingPublisher decoded =
                new AwsChunkedResponseDecodingPublisher(body, decodedContentLength, declaredTrailers);
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS,
                                             () -> AwsChunkedResponseDecodingUtils.completedTrailers(decoded.trailersFuture()));
            return decoded;
        };

        SdkHttpResponse rewritten = AwsChunkedResponseDecodingUtils.rewriteHeaders(response, decodedContentLength);
        context.interceptorContext(context.interceptorContext().copy(b -> b.httpResponse(rewritten)));
        delegate.onHeaders(rewritten);
    }

    @Override
    public void onError(Throwable error) {
        delegate.onError(error);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        UnaryOperator<Publisher<ByteBuffer>> decoder = bodyDecoder;
        delegate.onStream(decoder == null ? publisher : decoder.apply(publisher));
    }

    @Override
    public CompletableFuture<T> prepare() {
        return delegate.prepare();
    }
}
