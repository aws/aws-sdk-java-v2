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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.zip.Checksum;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A subscriber that takes a collection of checksums, and updates each checksum when it receives data.
 */
@SdkInternalApi
public final class ChecksumSubscriber implements Subscriber<ByteBuffer> {
    private final CompletableFuture<Publisher<ByteBuffer>> checksumming = new CompletableFuture<>();
    private final Collection<Checksum> checksums = new ArrayList<>();
    private volatile boolean canceled = false;
    private volatile Subscription subscription;

    private final List<ByteBuffer> bufferedPayload = new ArrayList<>();

    public ChecksumSubscriber(Collection<? extends Checksum> consumers) {
        this.checksums.addAll(consumers);

        checksumming.whenComplete((r, t) -> {
            if (t instanceof CancellationException) {
                synchronized (this) {
                    canceled = true;
                    if (subscription != null) {
                        subscription.cancel();
                    }
                }
            }
        });
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized (this) {
            if (!canceled && this.subscription == null) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        if (!canceled) {
            updateChecksumsAndBuffer(byteBuffer);
        }
    }

    private void updateChecksumsAndBuffer(ByteBuffer buffer) {
        int remaining = buffer.remaining();
        if (remaining <= 0) {
            return;
        }

        byte[] copyBuffer = new byte[remaining];
        buffer.get(copyBuffer);
        checksums.forEach(c -> c.update(copyBuffer, 0, remaining));
        bufferedPayload.add(ByteBuffer.wrap(copyBuffer));
    }


    @Override
    public void onError(Throwable throwable) {
        checksumming.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        checksumming.complete(new InMemoryPublisher(bufferedPayload));
    }

    public CompletableFuture<Publisher<ByteBuffer>> completeFuture() {
        return checksumming;
    }
}
