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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult;

/**
 * Adapts a {@link Subscriber} to a {@link InputStream}.
 * <p>
 * Reads from the stream will block until data is published to this subscriber. The amount of data stored in memory by this
 * subscriber when the input stream is not being read is bounded.
 */
@SdkProtectedApi
public final class InputStreamSubscriber extends InputStream implements Subscriber<ByteBuffer>, SdkAutoCloseable {
    private static final int BUFFER_SIZE = 4 * 1024 * 1024; // 4 MB

    private final ByteBufferStoringSubscriber delegate;
    private final ByteBuffer singleByte = ByteBuffer.allocate(1);

    private final AtomicReference<State> inputStreamState = new AtomicReference<>(State.UNINITIALIZED);
    private final AtomicBoolean drainingCallQueue = new AtomicBoolean(false);
    private final Queue<QueueEntry> callQueue = new ConcurrentLinkedQueue<>();

    private Subscription subscription;

    private boolean done = false;

    public InputStreamSubscriber() {
        this.delegate = new ByteBufferStoringSubscriber(BUFFER_SIZE);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (!inputStreamState.compareAndSet(State.UNINITIALIZED, State.READABLE)) {
            close();
            return;
        }

        this.subscription = new CancelWatcher(s);
        delegate.onSubscribe(subscription);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        callQueue.add(new QueueEntry(false, () -> delegate.onNext(byteBuffer)));
        drainQueue();
    }

    @Override
    public void onError(Throwable t) {
        callQueue.add(new QueueEntry(true, () -> delegate.onError(t)));
        drainQueue();
    }

    @Override
    public void onComplete() {
        callQueue.add(new QueueEntry(true, delegate::onComplete));
        drainQueue();
    }

    @Override
    public int read() {
        singleByte.clear();
        TransferResult transferResult = delegate.blockingTransferTo(singleByte);

        if (singleByte.hasRemaining()) {
            assert transferResult == TransferResult.END_OF_STREAM;
            return -1;
        }

        return singleByte.get(0) & 0xFF;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] bytes, int off, int len) {
        if (len == 0) {
            return 0;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, off, len);
        TransferResult transferResult = delegate.blockingTransferTo(byteBuffer);
        int dataTransferred = byteBuffer.position() - off;

        if (dataTransferred == 0) {
            assert transferResult == TransferResult.END_OF_STREAM;
            return -1;
        }

        return dataTransferred;
    }

    @Override
    public void close() {
        if (inputStreamState.compareAndSet(State.UNINITIALIZED, State.CLOSED)) {
            delegate.onSubscribe(new NoOpSubscription());
            delegate.onError(new CancellationException());
        } else if (inputStreamState.compareAndSet(State.READABLE, State.CLOSED)) {
            subscription.cancel();
            onError(new CancellationException());
        }
    }

    private void drainQueue() {
        do {
            if (!drainingCallQueue.compareAndSet(false, true)) {
                break;
            }

            try {
                doDrainQueue();
            } finally {
                drainingCallQueue.set(false);
            }
        } while (!callQueue.isEmpty());
    }

    private void doDrainQueue() {
        while (true) {
            QueueEntry entry = callQueue.poll();
            if (done || entry == null) {
                return;
            }
            done = entry.terminal;
            entry.call.run();
        }
    }

    private static final class QueueEntry {
        private final boolean terminal;
        private final Runnable call;

        private QueueEntry(boolean terminal, Runnable call) {
            this.terminal = terminal;
            this.call = call;
        }
    }

    private enum State {
        UNINITIALIZED,
        READABLE,
        CLOSED
    }

    private final class CancelWatcher implements Subscription {
        private final Subscription s;

        private CancelWatcher(Subscription s) {
            this.s = s;
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
            close();
        }
    }

    private static final class NoOpSubscription implements Subscription {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    }
}
