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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Async response handler decorator to run interceptors after response is received.
 *
 * @param <T> the type of the result
 */
@SdkInternalApi
public final class AsyncAfterTransmissionInterceptorCallingResponseHandler<T> implements TransformingAsyncResponseHandler<T> {
    private final TransformingAsyncResponseHandler<T> delegate;
    private final ExecutionContext context;

    public AsyncAfterTransmissionInterceptorCallingResponseHandler(TransformingAsyncResponseHandler<T> delegate,
                                                                   ExecutionContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    private SdkHttpResponse beforeUnmarshalling(SdkHttpFullResponse response, ExecutionContext context) {
        // Update interceptor context to include response
        InterceptorContext interceptorContext =
            context.interceptorContext().copy(b -> b.httpResponse(response));

        // interceptors.afterTransmission
        context.interceptorChain().afterTransmission(interceptorContext, context.executionAttributes());

        // interceptors.modifyHttpResponse
        interceptorContext = context.interceptorChain().modifyHttpResponse(interceptorContext, context.executionAttributes());

        // interceptors.beforeUnmarshalling
        context.interceptorChain().beforeUnmarshalling(interceptorContext, context.executionAttributes());

        // Store updated context
        context.interceptorContext(interceptorContext);

        return interceptorContext.httpResponse();
    }

    @Override
    public void onHeaders(SdkHttpResponse response) {
        delegate.onHeaders(beforeUnmarshalling((SdkHttpFullResponse) response, context)); // TODO: Ew
    }

    @Override
    public void onError(Throwable error) {
        delegate.onError(error);
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        Optional<Publisher<ByteBuffer>> newPublisher = context.interceptorChain()
                                                              .modifyAsyncHttpResponse(context.interceptorContext()
                                                                                              .toBuilder()
                                                                                              .responsePublisher(publisher)
                                                                                              .build(),
                                                                                       context.executionAttributes())
                                                              .responsePublisher();

        if (newPublisher.isPresent()) {
            delegate.onStream(newPublisher.get());
        } else {
            delegate.onStream(publisher);
        }
    }

    @Override
    public CompletableFuture<T> prepare() {
        return delegate.prepare();
    }
}