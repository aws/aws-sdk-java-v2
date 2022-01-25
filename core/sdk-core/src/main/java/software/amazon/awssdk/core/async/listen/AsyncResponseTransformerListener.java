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

package software.amazon.awssdk.core.async.listen;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;
import software.amazon.awssdk.utils.Validate;

/**
 * Listener interface that invokes callbacks associated with this {@link AsyncResponseTransformer} and any resulting {@link
 * SdkPublisher} and {@link Subscriber}.
 *
 * @see PublisherListener
 * @see SubscriberListener
 */
public interface AsyncResponseTransformerListener<ResponseT> extends PublisherListener<ByteBuffer> {

    /**
     * Invoked after {@link AsyncResponseTransformer#onResponse(Object)}
     */
    default void transformerOnResponse(ResponseT response) {
    }

    /**
     * Invoked after {@link AsyncResponseTransformer#onStream(SdkPublisher)}
     */
    default void transformerOnStream(SdkPublisher<ByteBuffer> publisher) {
    }

    /**
     * Invoked after {@link AsyncResponseTransformer#exceptionOccurred(Throwable)}
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

    @SdkProtectedApi
    final class NotifyingAsyncResponseTransformer<ResponseT, ResultT> implements AsyncResponseTransformer<ResponseT, ResultT> {
        private static final Logger log = LoggerFactory.getLogger(NotifyingAsyncResponseTransformer.class);

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
            delegate.onResponse(response);
            invoke(() -> listener.transformerOnResponse(response), "transformerOnResponse");
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(PublisherListener.wrap(publisher, listener));
            invoke(() -> listener.transformerOnStream(publisher), "transformerOnStream");
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            delegate.exceptionOccurred(error);
            invoke(() -> listener.transformerExceptionOccurred(error), "transformerExceptionOccurred");
        }

        static void invoke(UnsafeRunnable runnable, String callbackName) {
            runAndLogError(log, callbackName + " callback failed. This exception will be dropped.", runnable);
        }
    }
}
