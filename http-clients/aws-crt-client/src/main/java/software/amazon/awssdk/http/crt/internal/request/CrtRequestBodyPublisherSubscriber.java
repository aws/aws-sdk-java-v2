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

package software.amazon.awssdk.http.crt.internal.request;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.crt.internal.CrtStreamHandler;
import software.amazon.awssdk.http.crt.internal.ResponseHandlerErrorNotifier;
import software.amazon.awssdk.utils.Validate;

/**
 * Subscriber that consumes the SDK request body publisher and pushes each chunk to the CRT stream
 * via {@link CrtStreamHandler#writeData(byte[], boolean)}. Signals end-of-stream on {@code onComplete},
 * and tears down the stream on errors from either the publisher or CRT.
 */
@SdkInternalApi
public final class CrtRequestBodyPublisherSubscriber implements Subscriber<ByteBuffer> {

    private final CrtStreamHandler streamHandler;
    private final CompletableFuture<Void> executeFuture;
    private final ResponseHandlerErrorNotifier errorNotifier;
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    // Per Reactive Streams rule 2.7, calls to subscription.request and subscription.cancel must
    // be performed serially. This lock provides the happens-before edge between calls made on
    // different threads (publisher thread for onSubscribe; CRT event loop thread for whenComplete
    // callbacks; either for handleError).
    private final Object subscriptionLock = new Object();

    private Subscription subscription;

    public CrtRequestBodyPublisherSubscriber(CrtStreamHandler streamHandler,
                                             CompletableFuture<Void> executeFuture,
                                             ResponseHandlerErrorNotifier errorNotifier) {
        this.streamHandler = streamHandler;
        this.executeFuture = executeFuture;
        this.errorNotifier = errorNotifier;
    }

    @Override
    public void onSubscribe(Subscription s) {
        Validate.paramNotNull(s, "s");
        synchronized (subscriptionLock) {
            if (this.subscription != null) {
                s.cancel();
                return;
            }
            this.subscription = s;
            s.request(1);
        }
    }

    @Override
    public void onNext(ByteBuffer buf) {
        Validate.paramNotNull(buf, "buf");
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        CompletableFuture<Void> writeFuture = streamHandler.writeData(bytes, false);
        writeFuture.whenComplete((v, err) -> {
            if (err != null) {
                handleError(err, true);
            } else {
                requestNext();
            }
        });
    }

    @Override
    public void onComplete() {
        streamHandler.writeData(null, true).whenComplete((v, err) -> {
            if (err != null) {
                handleError(err, false);
            }
        });
    }

    @Override
    public void onError(Throwable t) {
        Validate.paramNotNull(t, "t");
        handleError(t, false);
    }

    private void requestNext() {
        synchronized (subscriptionLock) {
            subscription.request(1);
        }
    }

    private void handleError(Throwable t, boolean cancelSubscription) {
        if (!terminated.compareAndSet(false, true)) {
            return;
        }
        streamHandler.closeConnection();
        errorNotifier.tryNotifyError(t);
        executeFuture.completeExceptionally(t);
        if (cancelSubscription) {
            synchronized (subscriptionLock) {
                if (subscription != null) {
                    subscription.cancel();
                }
            }
        }
    }
}
