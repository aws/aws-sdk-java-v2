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

package software.amazon.awssdk.utils.async;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;


@SdkProtectedApi
public class ByteBufferingSubscriber<T extends ByteBuffer> extends DelegatingSubscriber<T, T> {
    private final int bufferSize;
    private final AtomicInteger bufferedBytes = new AtomicInteger(0);
    private final ByteBuffer currentBuffer;
    private Subscription subscription;


    public ByteBufferingSubscriber(Subscriber<ByteBuffer> subscriber, int bufferSize) {
        super(subscriber);
        this.bufferSize = bufferSize;
        currentBuffer = ByteBuffer.allocate(bufferSize);

    }


    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        super.onSubscribe(subscription);
    }

    @Override
    public void onNext(T t) {
        int startPosition = 0;
        int currentBytesRead = t.remaining();
        do {
            int availableToRead = bufferSize - bufferedBytes.get();
            int bytesToMove = availableToRead > (currentBytesRead - startPosition)
                              ? (currentBytesRead - startPosition)
                              : availableToRead;

            if (bufferedBytes.get() == 0) {
                currentBuffer.put(t.array(), startPosition, bytesToMove);

            } else {
                currentBuffer.put(t.array(), 0, bytesToMove);
            }
            startPosition = startPosition + bytesToMove;
            if (bufferedBytes.addAndGet(bytesToMove) == bufferSize) {
                currentBuffer.position(0);
                subscriber.onNext((T) currentBuffer);
                currentBuffer.clear();
                bufferedBytes.addAndGet(-bufferSize);
            }
        } while (startPosition < currentBytesRead);

        if (bufferedBytes.get() > 0) {
            subscription.request(1);
        }
    }

    @Override
    public void onComplete() {
        // Deliver any remaining items before calling on complete

        if (bufferedBytes.get() > 0) {
            currentBuffer.position(0);
            if (bufferedBytes.get() < bufferSize) {
                // Create a ByteBuffer with capacity equal to remaining bytes in the currentBuffer.
                ByteBuffer trimmedBuffer = ByteBuffer.allocate(bufferedBytes.get());
                trimmedBuffer.put(currentBuffer.array(), 0, bufferedBytes.get());
                trimmedBuffer.position(0);
                subscriber.onNext((T) trimmedBuffer);
            } else {
                subscriber.onNext((T) currentBuffer);
            }
            currentBuffer.clear();
        }
        super.onComplete();
    }
}
