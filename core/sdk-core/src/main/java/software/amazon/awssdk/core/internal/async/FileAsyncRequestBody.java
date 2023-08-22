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
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Implementation of {@link AsyncRequestBody} that reads data from a file.
 *
 * @see AsyncRequestBody#fromFile(Path)
 * @see AsyncRequestBody#fromFile(java.io.File)
 */
@SdkInternalApi
public final class FileAsyncRequestBody implements AsyncRequestBody {
    private static final Logger log = Logger.loggerFor(FileAsyncRequestBody.class);

    /**
     * Default size (in bytes) of ByteBuffer chunks read from the file and delivered to the subscriber.
     */
    private static final int DEFAULT_CHUNK_SIZE = 16 * 1024;

    /**
     * File to read.
     */
    private final Path path;

    private final long fileLength;

    /**
     * Size (in bytes) of ByteBuffer chunks read from the file and delivered to the subscriber.
     */
    private final int chunkSizeInBytes;
    private final long position;
    private final long numBytesToRead;

    private FileAsyncRequestBody(DefaultBuilder builder) {
        this.path = builder.path;
        this.chunkSizeInBytes = builder.chunkSizeInBytes == null ? DEFAULT_CHUNK_SIZE : builder.chunkSizeInBytes;
        this.fileLength = invokeSafely(() -> Files.size(path));
        this.position = builder.position == null ? 0 : Validate.isNotNegative(builder.position, "position");
        this.numBytesToRead = builder.numBytesToRead == null ? fileLength - this.position :
                              Validate.isNotNegative(builder.numBytesToRead, "numBytesToRead");
    }

    @Override
    public SdkPublisher<AsyncRequestBody> split(AsyncRequestBodySplitConfiguration splitConfiguration) {
        Validate.notNull(splitConfiguration, "splitConfiguration");
        return new FileAsyncRequestBodySplitHelper(this, splitConfiguration).split();
    }

    public Path path() {
        return path;
    }

    public long fileLength() {
        return fileLength;
    }

    public int chunkSizeInBytes() {
        return chunkSizeInBytes;
    }

    public long position() {
        return position;
    }

    public long numBytesToRead() {
        return numBytesToRead;
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of(numBytesToRead);
    }

    @Override
    public String contentType() {
        return Mimetype.getInstance().getMimetype(path);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        AsynchronousFileChannel channel = null;
        try {
            channel = openInputChannel(this.path);

            // We need to synchronize here because the subscriber could call
            // request() from within onSubscribe which would potentially
            // trigger onNext before onSubscribe is finished.
            Subscription subscription = new FileSubscription(channel, s);

            synchronized (subscription) {
                s.onSubscribe(subscription);
            }
        } catch (IOException | RuntimeException e) {
            if (channel != null) {
                runAndLogError(log.logger(), "Unable to close file channel", channel::close);
            }
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
         * Sets the size of chunks to read from the file. Increasing this will cause more data to be buffered into memory but
         * may yield better latencies. Decreasing this will reduce memory usage but may cause reduced latency. Setting this value
         * is very dependent on upload speed and requires some performance testing to tune.
         *
         * <p>The default chunk size is {@value #DEFAULT_CHUNK_SIZE} bytes</p>
         *
         * @param chunkSize New chunk size in bytes.
         * @return This builder for method chaining.
         */
        Builder chunkSizeInBytes(Integer chunkSize);

        /**
         * Sets the file position at which the request body begins.
         *
         * <p>By default, it's 0, i.e., reading from the beginning.
         *
         * @param position the position of the file
         * @return The builder for method chaining.
         */
        Builder position(Long position);

        /**
         * Sets the number of bytes to read from this file.
         * 
         * <p>By default, it's same as the file length.
         *
         * @param numBytesToRead number of bytes to read
         * @return The builder for method chaining.
         */
        Builder numBytesToRead(Long numBytesToRead);
    }

    private static final class DefaultBuilder implements Builder {

        private Long position;
        private Path path;
        private Integer chunkSizeInBytes;
        private Long numBytesToRead;

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

        @Override
        public Builder position(Long position) {
            this.position = position;
            return this;
        }

        @Override
        public Builder numBytesToRead(Long numBytesToRead) {
            this.numBytesToRead = numBytesToRead;
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
    private final class FileSubscription implements Subscription {
        private final AsynchronousFileChannel inputChannel;
        private final Subscriber<? super ByteBuffer> subscriber;

        private final AtomicLong currentPosition;
        private final AtomicLong remainingBytes;
        private final long sizeAtStart;
        private final FileTime modifiedTimeAtStart;
        private long outstandingDemand = 0;
        private boolean readInProgress = false;
        private volatile boolean done = false;
        private final Object lock = new Object();

        private FileSubscription(AsynchronousFileChannel inputChannel,
                                 Subscriber<? super ByteBuffer> subscriber) throws IOException {
            this.inputChannel = inputChannel;
            this.subscriber = subscriber;
            this.sizeAtStart = inputChannel.size();
            this.modifiedTimeAtStart = Files.getLastModifiedTime(path);
            this.remainingBytes = new AtomicLong(numBytesToRead);
            this.currentPosition = new AtomicLong(position);
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
                    // We need to synchronize here because of the race condition
                    // where readData finishes reading at the same time request
                    // demand comes in
                    synchronized (lock) {
                        // As governed by rule 3.17, when demand overflows `Long.MAX_VALUE` we treat the signalled demand as
                        // "effectively unbounded"
                        if (Long.MAX_VALUE -  outstandingDemand < n) {
                            outstandingDemand = Long.MAX_VALUE;
                        } else {
                            outstandingDemand += n;
                        }

                        if (!readInProgress) {
                            readInProgress = true;
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
            if (!inputChannel.isOpen() || done) {
                return;
            }

            ByteBuffer buffer = ByteBuffer.allocate(Math.min(chunkSizeInBytes, NumericUtils.saturatedCast(remainingBytes.get())));
            inputChannel.read(buffer, currentPosition.get(), buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    try {
                        if (result > 0) {
                            attachment.flip();

                            int readBytes = attachment.remaining();
                            currentPosition.addAndGet(readBytes);
                            remainingBytes.addAndGet(-readBytes);

                            signalOnNext(attachment);

                            if (remainingBytes.get() == 0) {
                                closeFile();
                                signalOnComplete();
                            }

                            synchronized (lock) {
                                // If we have more permits, queue up another read.
                                if (--outstandingDemand > 0) {
                                    readData();
                                } else {
                                    readInProgress = false;
                                }
                            }
                        } else {
                            // Reached the end of the file, notify the subscriber and cleanup
                            closeFile();
                            signalOnComplete();
                        }
                    } catch (Throwable throwable) {
                        closeFile();
                        signalOnError(throwable);
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
                log.warn(() -> "Failed to close the file", e);
            }
        }

        private void signalOnNext(ByteBuffer attachment) {
            synchronized (this) {
                if (!done) {
                    subscriber.onNext(attachment);
                }
            }
        }

        private void signalOnComplete() {
            try {
                long sizeAtEnd = Files.size(path);
                if (sizeAtStart != sizeAtEnd) {
                    signalOnError(new IOException("File size changed after reading started. Initial size: " + sizeAtStart + ". "
                                                  + "Current size: " + sizeAtEnd));
                    return;
                }

                if (remainingBytes.get() > 0) {
                    signalOnError(new IOException("Fewer bytes were read than were expected, was the file modified after "
                                                  + "reading started?"));
                    return;
                }

                FileTime modifiedTimeAtEnd = Files.getLastModifiedTime(path);
                if (modifiedTimeAtStart.compareTo(modifiedTimeAtEnd) != 0) {
                    signalOnError(new IOException("File last-modified time changed after reading started. Initial modification "
                                                  + "time: " + modifiedTimeAtStart + ". Current modification time: " +
                                                  modifiedTimeAtEnd));
                    return;
                }
            } catch (NoSuchFileException e) {
                signalOnError(new IOException("Unable to check file status after read. Was the file deleted or were its "
                                              + "permissions changed?", e));
                return;
            } catch (IOException e) {
                signalOnError(new IOException("Unable to check file status after read.", e));
                return;
            }

            synchronized (this) {
                if (!done) {
                    done = true;
                    subscriber.onComplete();
                }
            }
        }

        private void signalOnError(Throwable t) {
            synchronized (this) {
                if (!done) {
                    done = true;
                    subscriber.onError(t);
                }
            }
        }
    }

    private static AsynchronousFileChannel openInputChannel(Path path) throws IOException {
        return AsynchronousFileChannel.open(path, StandardOpenOption.READ);
    }
}