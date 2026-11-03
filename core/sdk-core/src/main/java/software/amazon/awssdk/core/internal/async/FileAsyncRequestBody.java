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
import java.nio.channels.Channel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
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

    /**
     * If set, {@link #path} is resolved below this directory without following a symbolic link in any path component.
     */
    private final Path confineToRoot;

    private final long fileLength;

    /**
     * Size (in bytes) of ByteBuffer chunks read from the file and delivered to the subscriber.
     */
    private final int chunkSizeInBytes;
    private final long position;
    private final long numBytesToRead;
    private FileTime modifiedTimeAtStart;
    private Long sizeAtStart;

    private FileAsyncRequestBody(DefaultBuilder builder) {
        this.path = builder.path;
        this.confineToRoot = builder.confineToRoot;
        this.chunkSizeInBytes = builder.chunkSizeInBytes == null ? DEFAULT_CHUNK_SIZE : builder.chunkSizeInBytes;
        // When confined, the length, the size at start and the modified time at start all come from one attribute read.
        BasicFileAttributes confinedAttributes =
            confineToRoot == null ? null : invokeSafely(() -> ConfinedFileAccess.readAttributes(confineToRoot, path));
        this.fileLength = confinedAttributes != null ? confinedAttributes.size() : invokeSafely(() -> Files.size(path));
        this.position = builder.position == null ? 0 : Validate.isNotNegative(builder.position, "position");
        this.numBytesToRead = builder.numBytesToRead == null ? fileLength - this.position :
                              Validate.isNotNegative(builder.numBytesToRead, "numBytesToRead");
        if (confinedAttributes != null) {
            this.modifiedTimeAtStart = builder.modifiedTimeAtStart != null ? builder.modifiedTimeAtStart
                                                                           : confinedAttributes.lastModifiedTime();
            this.sizeAtStart = builder.sizeAtStart != null ? builder.sizeAtStart : confinedAttributes.size();
        } else {
            initUnconfinedStartAttributes(builder);
        }
    }

    private void initUnconfinedStartAttributes(DefaultBuilder builder) {
        if (builder.modifiedTimeAtStart != null) {
            this.modifiedTimeAtStart = builder.modifiedTimeAtStart;
        } else {
            try {
                this.modifiedTimeAtStart = Files.getLastModifiedTime(path);
            } catch (IOException e) {
                log.debug(() -> "Failed to get last modified time for path " + path, e);
                this.modifiedTimeAtStart = null;
            }
        }

        if (builder.sizeAtStart != null) {
            this.sizeAtStart = builder.sizeAtStart;
        } else {
            try {
                this.sizeAtStart = Files.size(path);
            } catch (IOException e) {
                log.debug(() -> "Failed to get file size for path " + path, e);
                this.sizeAtStart = null;
            }
        }
    }

    @Override
    public SdkPublisher<AsyncRequestBody> split(AsyncRequestBodySplitConfiguration splitConfiguration) {
        Validate.notNull(splitConfiguration, "splitConfiguration");
        return new FileAsyncRequestBodySplitHelper(this, splitConfiguration).split();
    }

    @Override
    public SdkPublisher<CloseableAsyncRequestBody> splitCloseable(AsyncRequestBodySplitConfiguration splitConfiguration) {
        return split(splitConfiguration).map(body -> new ClosableAsyncRequestBodyWrapper(body));
    }

    public Path path() {
        return path;
    }

    /**
     * @return The directory below which {@link #path()} is resolved without following symbolic links, or {@code null} if
     * the path is opened normally.
     */
    Path confineToRoot() {
        return confineToRoot;
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

    public FileTime modifiedTimeAtStart() {
        return modifiedTimeAtStart;
    }

    public Long sizeAtStart() {
        return sizeAtStart;
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
        Channel channel = null;
        try {
            channel = confineToRoot == null ? openInputChannel(this.path)
                                            : ConfinedFileAccess.openForRead(confineToRoot, this.path);

            // We need to synchronize here because the subscriber could call
            // request() from within onSubscribe which would potentially
            // trigger onNext before onSubscribe is finished.
            Subscription subscription = new FileSubscription(channel, s, modifiedTimeAtStart, sizeAtStart);

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

    @Override
    public String body() {
        return BodyType.FILE.getName();
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

        /**
         * Optional - sets the file modified time at the start of the request.
         * @param modifiedTimeAtStart initial file modification time
         * @return The builder for method chaining.
         */
        Builder modifiedTimeAtStart(FileTime modifiedTimeAtStart);

        /**
         * Optional - sets the file size in bytes at the start of the request.
         * @param sizeAtStart initial file size at start.
         * @return The builder for method chaining.
         */
        Builder sizeAtStart(Long sizeAtStart);

        /**
         * Sets a directory that contains the file. When set, the file is resolved below this directory without following a
         * symbolic link in any path component below it, every time its attributes are read or it is opened. A symbolic link
         * in any of those components makes the read fail. The directory itself is trusted.
         *
         * <p>By default, it's null, and the file is read normally, following symbolic links.
         *
         * @param confineToRoot the directory below which the file must be resolved
         * @return The builder for method chaining.
         */
        Builder confineToRoot(Path confineToRoot);
    }

    private static final class DefaultBuilder implements Builder {

        private Long position;
        private Path path;
        private Integer chunkSizeInBytes;
        private Long numBytesToRead;
        private FileTime modifiedTimeAtStart;
        private Long sizeAtStart;
        private Path confineToRoot;

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

        @Override
        public Builder modifiedTimeAtStart(FileTime modifiedTimeAtStart) {
            this.modifiedTimeAtStart = modifiedTimeAtStart;
            return this;
        }

        @Override
        public Builder sizeAtStart(Long sizeAtStart) {
            this.sizeAtStart = sizeAtStart;
            return this;
        }

        @Override
        public Builder confineToRoot(Path confineToRoot) {
            this.confineToRoot = confineToRoot;
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
        /**
         * An {@link AsynchronousFileChannel}, or a {@link FileChannel} when the body is confined to a root.
         */
        private final Channel inputChannel;
        private final Subscriber<? super ByteBuffer> subscriber;

        private final AtomicLong currentPosition;
        private final AtomicLong remainingBytes;
        private final long sizeAtStart;
        private final FileTime modifiedTimeAtStart;
        private long outstandingDemand = 0;
        private boolean readInProgress = false;
        private volatile boolean done = false;
        private final Object lock = new Object();

        private FileSubscription(Channel inputChannel,
                                 Subscriber<? super ByteBuffer> subscriber,
                                 FileTime modifiedTimeAtStart, Long sizeAtStart) throws IOException {
            this.inputChannel = inputChannel;
            this.subscriber = subscriber;
            this.remainingBytes = new AtomicLong(numBytesToRead);
            this.currentPosition = new AtomicLong(position);
            BasicFileAttributes confinedAttributes = null;
            if (confineToRoot != null && (sizeAtStart == null || modifiedTimeAtStart == null)) {
                confinedAttributes = ConfinedFileAccess.readAttributes(confineToRoot, path);
            }

            if (sizeAtStart != null) {
                this.sizeAtStart = sizeAtStart;
            } else if (confinedAttributes != null) {
                this.sizeAtStart = confinedAttributes.size();
            } else {
                this.sizeAtStart = Files.size(path);
            }

            if (modifiedTimeAtStart != null) {
                this.modifiedTimeAtStart = modifiedTimeAtStart;
            } else if (confinedAttributes != null) {
                this.modifiedTimeAtStart = confinedAttributes.lastModifiedTime();
            } else {
                this.modifiedTimeAtStart = Files.getLastModifiedTime(path);
            }
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
            CompletionHandler<Integer, ByteBuffer> handler = new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    try {
                        if (result > 0) {
                            attachment.flip();

                            int readBytes = attachment.remaining();
                            currentPosition.addAndGet(readBytes);
                            long remaining = remainingBytes.addAndGet(-readBytes);

                            // we need to validate the file is unchanged before providing the last bytes to subscriber
                            // the subscriber (eg: NettyRequestExecutor) may cancel subscription once all expected bytes have
                            // been received.  Validating here ensures errors are correctly signaled.
                            if (remaining == 0) {
                                closeFile();
                                if (!validateFileUnchangedAndSignalErrors()) {
                                    return;
                                }
                            }

                            signalOnNext(attachment);

                            if (remaining == 0) {
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
            };

            if (inputChannel instanceof FileChannel) {
                readOnConfinedReadPool((FileChannel) inputChannel, buffer, currentPosition.get(), handler);
            } else {
                ((AsynchronousFileChannel) inputChannel).read(buffer, currentPosition.get(), buffer, handler);
            }
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
            if (!validateFileUnchangedAndSignalErrors()) {
                return;
            }

            synchronized (this) {
                if (!done) {
                    done = true;
                    subscriber.onComplete();
                }
            }
        }

        private boolean validateFileUnchangedAndSignalErrors() {
            try {
                BasicFileAttributes confinedAttributes =
                    confineToRoot == null ? null : ConfinedFileAccess.readAttributes(confineToRoot, path);
                long sizeAtEnd = confinedAttributes != null ? confinedAttributes.size() : Files.size(path);
                if (sizeAtStart != sizeAtEnd) {
                    signalOnError(SdkClientException.create("File size changed after reading started. Initial size: "
                                                         + sizeAtStart + ". Current size: " + sizeAtEnd));
                    return false;
                }

                if (remainingBytes.get() > 0) {
                    signalOnError(SdkClientException.create("Fewer bytes were read than were expected, was the file modified "
                                                           + "after reading started?"));
                    return false;
                }

                FileTime modifiedTimeAtEnd = confinedAttributes != null ? confinedAttributes.lastModifiedTime()
                                                                        : Files.getLastModifiedTime(path);
                if (modifiedTimeAtStart.compareTo(modifiedTimeAtEnd) != 0) {
                    signalOnError(SdkClientException.create("File last-modified time changed after reading started. "
                                                            + "Initial modification time: " + modifiedTimeAtStart
                                                            + ". Current modification time: " + modifiedTimeAtEnd));
                    return false;
                }
            } catch (NoSuchFileException e) {
                signalOnError(SdkClientException.create("Unable to check file status after read. Was the file deleted"
                                                        + " or were its permissions changed?", e));
                return false;
            } catch (IOException e) {
                signalOnError(SdkClientException.create("Unable to check file status after read.", e));
                return false;
            }
            return true;
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

    /**
     * Reads from {@code channel} at {@code position} into {@code buffer} on {@link ConfinedReadPool} and completes
     * {@code handler} from the pool thread. A confined file is opened through a {@link java.nio.file.SecureDirectoryStream},
     * which returns a blocking {@link FileChannel}, so its reads run on the pool to keep them off the subscriber's thread.
     */
    private static void readOnConfinedReadPool(FileChannel channel, ByteBuffer buffer, long position,
                                               CompletionHandler<Integer, ByteBuffer> handler) {
        try {
            ConfinedReadPool.EXECUTOR.execute(() -> {
                int result;
                try {
                    result = channel.read(buffer, position);
                } catch (Throwable t) {
                    handler.failed(t, buffer);
                    return;
                }
                handler.completed(result, buffer);
            });
        } catch (Throwable t) {
            handler.failed(t, buffer);
        }
    }

    /**
     * Holds the pool that runs reads of confined files, created on first use. It has at most one daemon thread per
     * available processor, and more reads wait in its queue. Each read is one positional read of at most one chunk. Idle
     * threads end after 60 seconds, so the pool holds no threads while no confined upload is running.
     */
    private static final class ConfinedReadPool {
        private static final int MAX_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors());
        private static final ThreadPoolExecutor EXECUTOR = createExecutor();

        private ConfinedReadPool() {
        }

        private static ThreadPoolExecutor createExecutor() {
            ThreadPoolExecutor executor =
                new ThreadPoolExecutor(MAX_THREADS, MAX_THREADS, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                                       new ThreadFactoryBuilder().threadNamePrefix("sdk-confined-file-read")
                                                                 .daemonThreads(true)
                                                                 .build());
            executor.allowCoreThreadTimeOut(true);
            return executor;
        }
    }

    private static class ClosableAsyncRequestBodyWrapper implements CloseableAsyncRequestBody {
        private final AsyncRequestBody delegate;

        ClosableAsyncRequestBodyWrapper(AsyncRequestBody body) {
            this.delegate = body;
        }

        @Override
        public Optional<Long> contentLength() {
            return delegate.contentLength();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            delegate.subscribe(s);
        }

        @Override
        public void close() {
            // no op
        }

        @Override
        public String body() {
            return delegate.body();
        }
    }
}