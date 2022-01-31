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

package software.amazon.awssdk.core.async.listener;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Listener interface that invokes callbacks associated with a {@link AsyncResponseTransformer} and any resulting {@link
 * SdkPublisher} and {@link Subscriber}.
 *
 * @see PublisherListener
 * @see SubscriberListener
 */
@SdkProtectedApi
public interface AsyncResponseTransformerListener<ResponseT> extends PublisherListener<ByteBuffer> {

    /**
     * Invoked before {@link AsyncResponseTransformer#onResponse(Object)}
     */
    default void transformerOnResponse(ResponseT response) {
    }

    /**
     * Invoked before {@link AsyncResponseTransformer#onStream(SdkPublisher)}
     */
    default void transformerOnStream(SdkPublisher<ByteBuffer> publisher) {
    }

    /**
     * Invoked before {@link AsyncResponseTransformer#exceptionOccurred(Throwable)}
     */
    default void transformerExceptionOccurred(Throwable t) {
    }

    /**
     * Wrap a {@link AsyncResponseTransformer} with a new one that will notify a {@link AsyncResponseTransformerListener} of
     * important events occurring.
     */
    static <ResponseT, ResultT> AsyncResponseTransformer<ResponseT, ResultT> wrap(
        AsyncResponseTransformer<ResponseT, ResultT> delegate,
        AsyncResponseTransformerListener<ResponseT> listener) {
        return new NotifyingAsyncResponseTransformer<>(delegate, listener);
    }

    @SdkInternalApi
    final class NotifyingAsyncResponseTransformer<ResponseT, ResultT> implements AsyncResponseTransformer<ResponseT, ResultT> {
        private static final Logger log = Logger.loggerFor(NotifyingAsyncResponseTransformer.class);

        private final AsyncResponseTransformer<ResponseT, ResultT> delegate;
        private final AsyncResponseTransformerListener<ResponseT> listener;

        NotifyingAsyncResponseTransformer(AsyncResponseTransformer<ResponseT, ResultT> delegate,
                                          AsyncResponseTransformerListener<ResponseT> listener) {
            this.delegate = Validate.notNull(delegate, "delegate");
            this.listener = Validate.notNull(listener, "listener");
        }

        @Override
        public CompletableFuture<ResultT> prepare() {
            return delegate.prepare();
        }

        @Override
        public void onResponse(ResponseT response) {
            invoke(() -> listener.transformerOnResponse(response), "transformerOnResponse");
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            invoke(() -> listener.transformerOnStream(publisher), "transformerOnStream");
            delegate.onStream(PublisherListener.wrap(publisher, listener));
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            invoke(() -> listener.transformerExceptionOccurred(error), "transformerExceptionOccurred");
            delegate.exceptionOccurred(error);
        }

        static void invoke(Runnable runnable, String callbackName) {
            try {
                runnable.run();
            } catch (Exception e) {
                log.error(() -> callbackName + " callback failed. This exception will be dropped.", e);
            }
        }
    }
}
