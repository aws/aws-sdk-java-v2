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

package software.amazon.awssdk.core.async;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.io.SdkLengthAwareInputStream;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.async.InputStreamConsumingPublisher;

/**
 * An implementation of {@link AsyncRequestBody} that allows performing a blocking write of an input stream to a downstream
 * service.
 *
 * <p>See {@link AsyncRequestBody#forBlockingInputStream(Long)}.
 */
@SdkPublicApi
public final class BlockingInputStreamAsyncRequestBody implements AsyncRequestBody {
    private final InputStreamConsumingPublisher delegate = new InputStreamConsumingPublisher();
    private final CountDownLatch subscribedLatch = new CountDownLatch(1);
    private final AtomicBoolean subscribeCalled = new AtomicBoolean(false);
    private final Long contentLength;
    private final Duration subscribeTimeout;

    BlockingInputStreamAsyncRequestBody(Long contentLength) {
        this(contentLength, Duration.ofSeconds(10));
    }

    BlockingInputStreamAsyncRequestBody(Long contentLength, Duration subscribeTimeout) {
        this.contentLength = contentLength;
        this.subscribeTimeout = subscribeTimeout;
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.ofNullable(contentLength);
    }

    /**
     * Block the calling thread and write the provided input stream to the downstream service.
     *
     * <p>This method will block the calling thread immediately. This means that this request body should usually be passed to
     * the SDK before this method is called.
     *
     * <p>This method will return the amount of data written when the entire input stream has been written. This will throw an
     * exception if writing the input stream has failed.
     *
     * <p>You can invoke {@link #cancel()} to cancel any blocked write calls to the downstream service (and mark the stream as
     * failed).
     */
    public long writeInputStream(InputStream inputStream) {
        try {
            waitForSubscriptionIfNeeded();
            if (contentLength != null) {
                return delegate.doBlockingWrite(new SdkLengthAwareInputStream(inputStream, contentLength));
            }

            return delegate.doBlockingWrite(inputStream);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            delegate.cancel();
            throw new RuntimeException(e);
        }
    }

    /**
     * Cancel any running write (and mark the stream as failed).
     */
    public void cancel() {
        delegate.cancel();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        if (subscribeCalled.compareAndSet(false, true)) {
            delegate.subscribe(s);
            subscribedLatch.countDown();
        } else {
            s.onSubscribe(new NoopSubscription(s));
            s.onError(NonRetryableException.create("A retry was attempted, but AsyncRequestBody.forBlockingInputStream does not "
                                                   + "support retries. Consider using AsyncRequestBody.fromInputStream with an "
                                                   + "input stream that supports mark/reset to get retry support."));
        }
    }

    private void waitForSubscriptionIfNeeded() throws InterruptedException {
        long timeoutSeconds = subscribeTimeout.getSeconds();
        if (!subscribedLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The service request was not made within " + timeoutSeconds + " seconds of "
                                            + "doBlockingWrite being invoked. Make sure to invoke the service request "
                                            + "BEFORE invoking doBlockingWrite if your caller is single-threaded.");
        }
    }
}
