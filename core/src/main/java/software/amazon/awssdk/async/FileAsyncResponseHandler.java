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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotation.SdkInternalApi;

/**
 * {@link AsyncResponseHandler} that writes the data to the specified file.
 *
 * @param <ResponseT> Response POJO type. Not used.
 */
@SdkInternalApi
class FileAsyncResponseHandler<ResponseT> implements AsyncResponseHandler<ResponseT, Void> {

    private final Path path;
    private AsynchronousFileChannel fileChannel;

    FileAsyncResponseHandler(Path path) {
        this.path = path;
    }

    private AsynchronousFileChannel createChannel(Path path) throws IOException {
        return AsynchronousFileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }

    @Override
    public void responseReceived(ResponseT response) {
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        // onStream may be called multiple times so reset the file channel every time
        this.fileChannel = invokeSafely(() -> createChannel(path));
        publisher.subscribe(new FileSubscriber());
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        try {
            invokeSafely(fileChannel::close);
        } catch (RuntimeException e) {
            path.toFile().delete();
            throw e;
        }
        if (!path.toFile().delete()) {
            throw new UncheckedIOException(new IOException(
                    String.format("Could not delete %s.", path.toFile().getAbsolutePath())));
        }
    }

    @Override
    public Void complete() {
        invokeSafely(fileChannel::close);
        return null;
    }

    /**
     * {@link Subscriber} implementation that writes chunks to a file.
     */
    private class FileSubscriber implements Subscriber<ByteBuffer> {

        private final AtomicLong position = new AtomicLong();
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            // Request the first chunk to start producing content
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            fileChannel.write(byteBuffer, position.get(), byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    if (result > 0) {
                        position.addAndGet(result);
                        // Request another chunk of data
                        subscription.request(1);
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    subscription.cancel();
                }
            });

        }

        @Override
        public void onError(Throwable t) {
            // Error handled by response handler
        }

        @Override
        public void onComplete() {
            // Completion handled by response handler
        }
    }
}
