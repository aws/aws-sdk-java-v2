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
import java.util.concurrent.CompletableFuture;
import java.util.zip.Checksum;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.async.DelegatingSubscriber;

/**
 * A subscriber that takes a collection of checksums, and updates each checksum when it receives data.
 */
@SdkInternalApi
public final class ChecksumSubscriber extends DelegatingSubscriber<ByteBuffer, ByteBuffer> {
    private final CompletableFuture<Void> signal;
    private final Collection<Checksum> checksums = new ArrayList<>();
    private byte[] copyBuffer;

    public ChecksumSubscriber(Subscriber<? super ByteBuffer> subscriber,
                              Collection<? extends Checksum> consumers,
                              CompletableFuture<Void> signal) {

        super(subscriber);
        this.checksums.addAll(consumers);
        this.signal = signal;
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        updateChecksumsAndReset(byteBuffer);
        subscriber.onNext(byteBuffer);
    }

    private void updateChecksumsAndReset(ByteBuffer buffer) {
        int position = buffer.position();
        int limit = buffer.limit();
        int remaining = limit - position;
        if (remaining <= 0) {
            return;
        }
        if (buffer.hasArray()) {
            checksums.forEach(c -> c.update(buffer.array(), position + buffer.arrayOffset(), remaining));
        } else {
            if (copyBuffer == null) {
                copyBuffer = new byte[4096];
            }
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), copyBuffer.length);
                buffer.get(copyBuffer, 0, length);
                checksums.forEach(c -> c.update(copyBuffer, 0, length));
            }
        }
        buffer.position(position);
        buffer.limit(limit);
    }

    @Override
    public void onError(Throwable t) {
        super.onError(t);
        signal.completeExceptionally(t);
    }

    @Override
    public void onComplete() {
        super.onComplete();
        signal.complete(null);
    }
}
