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

package software.amazon.awssdk.core.internal.async;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Implementation of {@link AsyncRequestBody} that reads data from a file.
 *
 * @see AsyncRequestBody#fromFile(Path)
 * @see AsyncRequestBody#fromFile(java.io.File)
 */
@SdkInternalApi
public final class FileAsyncRequestBody implements AsyncRequestBody {

    /**
     * Default size (in bytes) of ByteBuffer chunks read from the file and delivered to the subscriber.
     */
    private static final int DEFAULT_CHUNK_SIZE = 16 * 1024;

    /**
     * File to read.
     */
    private final Path path;

    /**
     * Size (in bytes) of ByteBuffer chunks read from the file and delivered to the subscriber.
     */
    private final int chunkSizeInBytes;

    private FileAsyncRequestBody(DefaultBuilder builder) {
        this.path = builder.path;
        this.chunkSizeInBytes = builder.chunkSizeInBytes == null ? DEFAULT_CHUNK_SIZE : builder.chunkSizeInBytes;
    }

    @Override
    public long contentLength() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        try {
            AsynchronousFileChannel channel = openInputChannel(this.path);

            // We need to synchronize here because the subscriber could call
            // request() from within onSubscribe which would potentially
            // trigger onNext before onSubscribe is finished.
            Subscription subscription = new FileSubscription(channel, s, chunkSizeInBytes);
            synchronized (subscription) {
                s.onSubscribe(subscription);
            }
        } catch (IOException e) {
            // subscribe() must return normally, so we need to signal the
            // failure to open via onError() once onSubscribe() is signaled.
            s.onSubscribe(new NoopSubscription(s));
            s.onError(e);
        }
    }

    /**
     * @return Builder instance to construct a {@link FileAsyncRequestBody}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * A builder for {@link FileAsyncRequestBody}.
     */
    public interface Builder extends SdkBuilder<Builder, FileAsyncRequestBody> {

        /**
         * Sets the file to send to the service.
         *
         * @param path Path to file to read.
         * @return This builder for method chaining.
         */
        Builder path(Path path);

        /**
         * Sets the size of chunks read from the file. Increasing this will cause more data to be buffered into memory but
         * may yield better latencies. Decreasing this will reduce memory usage but may cause reduced latency. Setting this value
         * is very dependent on upload speed and requires some performance testing to tune.
         *
         * <p>The default chunk size is {@value #DEFAULT_CHUNK_SIZE} bytes</p>
         *
         * @param chunkSize New chunk size in bytes.
         * @return This builder for method chaining.
         */
        Builder chunkSizeInBytes(Integer chunkSize);

    }

    private static final class DefaultBuilder implements Builder {

        private Path path;
        private Integer chunkSizeInBytes;

        @Override
        public Builder path(Path path) {
            this.path = path;
            return this;
        }

        public void setPath(Path path) {
            path(path);
        }

        @Override
        public Builder chunkSizeInBytes(Integer chunkSizeInBytes) {
            this.chunkSizeInBytes = chunkSizeInBytes;
            return this;
        }

        public void setChunkSizeInBytes(Integer chunkSizeInBytes) {
            chunkSizeInBytes(chunkSizeInBytes);
        }

        @Override
        public FileAsyncRequestBody build() {
            return new FileAsyncRequestBody(this);
        }
    }

    /**
     * Reads the file for one subscriber.
     */
    private static final class FileSubscription implements Subscription {
        private final AsynchronousFileChannel inputChannel;
        private final Subscriber<? super ByteBuffer> subscriber;
        private final int chunkSize;

        private long position = 0;
        private AtomicLong outstandingDemand = new AtomicLong(0);
        private boolean writeInProgress = false;
        private volatile boolean done = false;

        private FileSubscription(AsynchronousFileChannel inputChannel, Subscriber<? super ByteBuffer> subscriber, int chunkSize) {
            this.inputChannel = inputChannel;
            this.subscriber = subscriber;
            this.chunkSize = chunkSize;
        }

        @Override
        public void request(long n) {
            if (done) {
                return;
            }

            if (n < 1) {
                IllegalArgumentException ex =
                    new IllegalArgumentException(subscriber + " violated the Reactive Streams rule 3.9 by requesting a "
                            + "non-positive number of elements.");
                signalOnError(ex);
            } else {
                try {
                    // As governed by rule 3.17, when demand overflows `Long.MAX_VALUE` we treat the signalled demand as
                    // "effectively unbounded"
                    outstandingDemand.getAndUpdate(initialDemand -> {
                        if (Long.MAX_VALUE - initialDemand < n) {
                            return Long.MAX_VALUE;
                        } else {
                            return initialDemand + n;
                        }
                    });

                    synchronized (this) {
                        if (!writeInProgress) {
                            writeInProgress = true;
                            readData();
                        }
                    }
                } catch (Exception e) {
                    signalOnError(e);
                }
            }
        }

        @Override
        public void cancel() {
            synchronized (this) {
                if (!done) {
                    done = true;
                    closeFile();
                }
            }
        }

        private void readData() {
            // It's possible to have another request for data come in after we've closed the file.
            if (!inputChannel.isOpen()) {
                return;
            }

            final ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
            inputChannel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if (result > 0) {
                        attachment.flip();
                        position += attachment.remaining();
                        signalOnNext(attachment);
                        // If we have more permits, queue up another read.
                        if (outstandingDemand.decrementAndGet() > 0) {
                            readData();
                            return;
                        }
                    } else {
                        // Reached the end of the file, notify the subscriber and cleanup
                        signalOnComplete();
                        closeFile();
                    }

                    synchronized (FileSubscription.this) {
                        writeInProgress = false;
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    signalOnError(exc);
                    closeFile();
                }
            });
        }

        private void closeFile() {
            try {
                inputChannel.close();
            } catch (IOException e) {
                signalOnError(e);
            }
        }

        private void signalOnNext(ByteBuffer bb) {
            synchronized (this) {
                if (!done) {
                    subscriber.onNext(bb);
                }
            }
        }

        private void signalOnComplete() {
            synchronized (this) {
                if (!done) {
                    subscriber.onComplete();
                    done = true;
                }
            }
        }

        private void signalOnError(Throwable t) {
            synchronized (this) {
                if (!done) {
                    subscriber.onError(t);
                    done = true;
                }
            }
        }
    }

    private static AsynchronousFileChannel openInputChannel(Path path) throws IOException {
        return AsynchronousFileChannel.open(path, StandardOpenOption.READ);
    }
}
