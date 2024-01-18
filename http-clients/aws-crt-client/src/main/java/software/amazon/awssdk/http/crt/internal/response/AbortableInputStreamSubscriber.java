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

package software.amazon.awssdk.http.crt.internal.response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.async.InputStreamSubscriber;

/**
 * Wrapper of {@link InputStreamSubscriber} that also implements {@link Abortable} and closes the underlying connections when
 * {@link #close()} or {@link #abort()} is invoked.
 */
@SdkInternalApi
public final class AbortableInputStreamSubscriber extends InputStream implements Subscriber<ByteBuffer>, Abortable {

    private final InputStreamSubscriber delegate;
    private final Runnable closeConnection;

    public AbortableInputStreamSubscriber(Runnable onClose, InputStreamSubscriber inputStreamSubscriber) {
        this.delegate = inputStreamSubscriber;
        this.closeConnection = onClose;
    }

    @Override
    public void abort() {
        close();
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public void onSubscribe(Subscription s) {
        delegate.onSubscribe(s);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        delegate.onNext(byteBuffer);
    }

    @Override
    public void onError(Throwable t) {
        delegate.onError(t);
    }

    @Override
    public void onComplete() {
        delegate.onComplete();
    }

    @Override
    public void close() {
        closeConnection.run();
        delegate.close();
    }
}
