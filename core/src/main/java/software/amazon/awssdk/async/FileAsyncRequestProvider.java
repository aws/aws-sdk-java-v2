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

package software.amazon.awssdk.async;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * Implementation of {@link AsyncRequestProvider} that reads data from a file.
 */
@SdkInternalApi
class FileAsyncRequestProvider implements AsyncRequestProvider {

    /**
     * Size of ByteBuffer chunks delivered to subscriber
     */
    private static final int CHUNK_SIZE = 1024;

    /**
     * File to read.
     */
    private final File file;

    FileAsyncRequestProvider(Path path) {
        this.file = path.toFile();
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        s.onSubscribe(new FileSubscription(file, s));
    }

    /**
     * Reads the file for one subscriber.
     */
    private static class FileSubscription implements Subscription {

        private final AsynchronousFileChannel inputChannel;
        private final Subscriber<? super ByteBuffer> subscriber;

        private long position = 0;
        private AtomicLong outstandingRequests = new AtomicLong(0);
        private boolean writeInProgress = false;

        private FileSubscription(File file, Subscriber<? super ByteBuffer> subscriber) {
            this.inputChannel = openInputChannel(file);
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            outstandingRequests.addAndGet(n);

            synchronized (this) {
                if (!writeInProgress) {
                    writeInProgress = true;
                    readData();
                }
            }
        }

        @Override
        public void cancel() {
            closeFile();
        }

        private void readData() {
            final ByteBuffer buffer = ByteBuffer.allocate(CHUNK_SIZE);
            inputChannel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if (result > 0) {
                        attachment.flip();
                        position += attachment.remaining();
                        subscriber.onNext(attachment);
                        // If we have more permits, queue up another read.
                        if (outstandingRequests.decrementAndGet() > 0) {
                            readData();
                            return;
                        }
                    } else {
                        // Reached the end of the file, notify the subscriber and cleanup
                        subscriber.onComplete();
                        closeFile();
                    }

                    synchronized (FileSubscription.this) {
                        writeInProgress = false;
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    subscriber.onError(exc);
                    closeFile();
                }
            });
        }

        private void closeFile() {
            try {
                inputChannel.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

    private static AsynchronousFileChannel openInputChannel(File file) {
        try {
            final Path path = Paths.get(file.getAbsolutePath());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            return AsynchronousFileChannel.open(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}
