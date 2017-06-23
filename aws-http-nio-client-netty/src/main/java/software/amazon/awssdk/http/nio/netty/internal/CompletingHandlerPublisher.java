/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty.internal;

import com.typesafe.netty.HandlerPublisher;
import io.netty.util.concurrent.EventExecutor;
import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

/**
 * Extends {@link HandlerPublisher} to call complete on an {@link SdkHttpResponseHandler} once all the data has been sent to the
 * {@link Subscriber} and {@link Subscriber#onComplete()} has been called.
 */
class CompletingHandlerPublisher extends HandlerPublisher<ByteBuffer> {

    private final SdkHttpResponseHandler responseHandler;

    /**
     * Create a handler publisher.
     *
     * The supplied executor must be the same event loop as the event loop that this handler is eventually registered
     * with, if not, an exception will be thrown when the handler is registered.
     *
     * @param executor        The executor to execute asynchronous events from the subscriber on.
     * @param responseHandler Response handler to invoke callbacks on.
     */
    CompletingHandlerPublisher(EventExecutor executor, SdkHttpResponseHandler responseHandler) {
        super(executor, ByteBuffer.class);
        this.responseHandler = responseHandler;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        // Intercept subscribe to wrap with a subscriber that calls out to the response handler
        super.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriber.onSubscribe(s);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                subscriber.onNext(byteBuffer);
            }

            @Override
            public void onError(Throwable t) {
                subscriber.onError(t);
                responseHandler.exceptionOccurred(t);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
                responseHandler.complete();
            }
        });
    }
}
