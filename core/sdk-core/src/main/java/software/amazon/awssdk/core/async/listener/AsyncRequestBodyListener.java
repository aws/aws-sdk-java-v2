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
import java.util.Optional;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Listener interface that invokes callbacks associated with a {@link AsyncRequestBody} and any resulting {@link Subscriber}.
 *
 * @see PublisherListener
 * @see SubscriberListener
 */
@SdkProtectedApi
public interface AsyncRequestBodyListener extends PublisherListener<ByteBuffer> {

    /**
     * Wrap a {@link AsyncRequestBody} with a new one that will notify a {@link AsyncRequestBodyListener} of important events
     * occurring.
     */
    static AsyncRequestBody wrap(AsyncRequestBody delegate, AsyncRequestBodyListener listener) {
        return new NotifyingAsyncRequestBody(delegate, listener);
    }

    @SdkInternalApi
    final class NotifyingAsyncRequestBody implements AsyncRequestBody {
        private static final Logger log = Logger.loggerFor(NotifyingAsyncRequestBody.class);

        private final AsyncRequestBody delegate;
        private final AsyncRequestBodyListener listener;

        NotifyingAsyncRequestBody(AsyncRequestBody delegate, AsyncRequestBodyListener listener) {
            this.delegate = Validate.notNull(delegate, "delegate");
            this.listener = Validate.notNull(listener, "listener");
        }

        @Override
        public Optional<Long> contentLength() {
            return delegate.contentLength();
        }

        @Override
        public String contentType() {
            return delegate.contentType();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            invoke(() -> listener.publisherSubscribe(s), "publisherSubscribe");
            delegate.subscribe(SubscriberListener.wrap(s, listener));
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
