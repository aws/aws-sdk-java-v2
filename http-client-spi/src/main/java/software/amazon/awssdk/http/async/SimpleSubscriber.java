/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.async;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Simple subscriber that does no backpressure and doesn't care about errors or completion.
 */
@SdkProtectedApi
public class SimpleSubscriber implements Subscriber<ByteBuffer> {

    private final Consumer<ByteBuffer> consumer;
    private final AtomicReference<Subscription> subscription = new AtomicReference<>();

    public SimpleSubscriber(Consumer<ByteBuffer> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onSubscribe(Subscription s) {
        // As per rule 1.9 we must throw NullPointerException if the subscriber parameter is null
        if (s == null) {
            throw new NullPointerException("Subscription MUST NOT be null.");
        }

        if (subscription.get() == null) {
            if (subscription.compareAndSet(null, s)) {
                s.request(Long.MAX_VALUE);
            } else {
                onSubscribe(s); // lost race, retry (will cancel in the else branch below)
            }
        } else {
            try {
                s.cancel(); // Cancel the additional subscription
            } catch (final Throwable t) {
                // Subscription.cancel is not allowed to throw an exception, according to rule 3.15
                (new IllegalStateException(s + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.",
                        t))
                    .printStackTrace(System.err);
            }
        }
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        // Rule 2.13, null arguments must be failed on eagerly
        if (byteBuffer == null) {
            throw new NullPointerException("Element passed to onNext MUST NOT be null.");
        }

        consumer.accept(byteBuffer);
    }

    @Override
    public void onError(Throwable t) {
        if (t == null) {
            throw new NullPointerException("Throwable passed to onError MUST NOT be null.");
        }
        // else, ignore
    }

    @Override
    public void onComplete() {
        // ignore
    }
}
