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

package software.amazon.awssdk.core.internal.async;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.utils.Logger;

/**
 * {@link Subscriber} implementation that writes chunks to a file.
 */
public class FileSubscriber implements Subscriber<ByteBuffer> {
    private static final Logger log = Logger.loggerFor(FileSubscriber.class);
    private final AtomicLong position;
    private final AsynchronousFileChannel fileChannel;
    private final Path path;
    private final CompletableFuture<Void> future;
    private final Consumer<Throwable> onErrorMethod;

    private volatile boolean writeInProgress = false;
    private volatile boolean closeOnLastWrite = false;
    private Subscription subscription;

    public FileSubscriber(AsynchronousFileChannel fileChannel, Path path, CompletableFuture<Void> future,
                   Consumer<Throwable> onErrorMethod, long startingPosition) {
        this.fileChannel = fileChannel;
        this.path = path;
        this.future = future;
        this.onErrorMethod = onErrorMethod;
        this.position = new AtomicLong(startingPosition);
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (this.subscription != null) {
            s.cancel();
            return;
        }
        this.subscription = s;
        // Request the first chunk to start producing content
        s.request(1);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            throw new NullPointerException("Element must not be null");
        }

        performWrite(byteBuffer);
    }

    private void performWrite(ByteBuffer byteBuffer) {
        writeInProgress = true;

        fileChannel.write(byteBuffer, position.get(), byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                position.addAndGet(result);

                if (byteBuffer.hasRemaining()) {
                    performWrite(byteBuffer);
                } else {
                    synchronized (FileSubscriber.this) {
                        writeInProgress = false;
                        if (closeOnLastWrite) {
                            close();
                        } else {
                            subscription.request(1);
                        }
                    }
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                subscription.cancel();
                future.completeExceptionally(exc);
            }
        });
    }

    @Override
    public void onError(Throwable t) {
        onErrorMethod.accept(t);
    }

    @Override
    public void onComplete() {
        log.trace(() -> "onComplete");
        // if write in progress, tell write to close on finish.
        synchronized (this) {
            if (writeInProgress) {
                log.trace(() -> "writeInProgress = true, not closing");
                closeOnLastWrite = true;
            } else {
                log.trace(() -> "writeInProgress = false, closing");
                close();
            }
        }
    }

    private void close() {
        try {
            if (fileChannel != null) {
                invokeSafely(fileChannel::close);
            }
            log.trace(() -> "Completing File async transformer future future");
            future.complete(null);
        } catch (RuntimeException exception) {
            future.completeExceptionally(exception);
        }
    }

    @Override
    public String toString() {
        return getClass() + ":" + path.toString();
    }
}
