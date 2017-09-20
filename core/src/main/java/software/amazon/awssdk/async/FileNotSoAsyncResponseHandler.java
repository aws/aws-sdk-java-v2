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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * {@link AsyncResponseHandler} that writes the data to the specified file.
 *
 * @param <ResponseT> Response POJO type. Returned on {@link #complete()}.
 */
@SdkInternalApi
public class FileNotSoAsyncResponseHandler<ResponseT> implements AsyncResponseHandler<ResponseT, ResponseT> {

    private final Path path;
    private FileOutputStream fos;
    private volatile ResponseT response;

    public FileNotSoAsyncResponseHandler(Path path) {
        this.path = path;
    }

    @Override
    public void responseReceived(ResponseT response) {
        this.response = response;
    }

    @Override
    public void onStream(Publisher<ByteBuffer> publisher) {
        this.fos = invokeSafely(() -> new FileOutputStream(path.toFile()));
        publisher.subscribe(new FileSubscriber());
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        try {
            invokeSafely(fos::close);
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
    public ResponseT complete() {
        invokeSafely(fos::close);
        return response;
    }

    /**
     * {@link Subscriber} implementation that writes chunks to a file.
     */
    private class FileSubscriber implements Subscriber<ByteBuffer> {

        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            // Request the first chunk to start producing content
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            invokeSafely(() -> fos.write(BinaryUtils.copyBytesFrom(byteBuffer)));
            subscription.request(1);
        }

        @Override
        public void onError(Throwable t) {
            // Error handled by response handler
        }

        @Override
        public void onComplete() {
        }

    }
}
