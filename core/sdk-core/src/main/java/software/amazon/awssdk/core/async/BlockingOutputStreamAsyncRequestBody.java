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

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.CancellableOutputStream;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.OutputStreamPublisher;

/**
 * An implementation of {@link AsyncRequestBody} that allows performing a blocking write of an output stream to a downstream
 * service.
 *
 * <p>The caller is responsible for calling {@link OutputStream#close()} on the {@link #outputStream()} when writing is
 * complete.
 *
 * @see AsyncRequestBody#forBlockingOutputStream(Long)
 */
@SdkPublicApi
public final class BlockingOutputStreamAsyncRequestBody implements AsyncRequestBody {
    private final OutputStreamPublisher delegate = new OutputStreamPublisher();
    private final CountDownLatch subscribedLatch = new CountDownLatch(1);
    private final AtomicBoolean subscribeCalled = new AtomicBoolean(false);
    private final Long contentLength;
    private final Duration subscribeTimeout;

    private BlockingOutputStreamAsyncRequestBody(Builder builder) {
        this.contentLength = builder.contentLength;
        this.subscribeTimeout = Validate.isPositiveOrNull(builder.subscribeTimeout, "subscribeTimeout") != null ?
                                builder.subscribeTimeout :
                                Duration.ofSeconds(10);
    }

    /**
     * Return an output stream to which blocking writes can be made to the downstream service.
     *
     * <p>This method will block the calling thread until the SDK is connected to the service. This means that this request body
     * should usually be passed to the SDK before this method is called.
     *
     * <p>You can invoke {@link CancellableOutputStream#cancel()} to cancel any blocked write calls to the downstream service
     * (and mark the stream as failed).
     */
    public CancellableOutputStream outputStream() {
        waitForSubscriptionIfNeeded();
        return delegate;
    }

    /**
     * Creates a default builder for {@link BlockingOutputStreamAsyncRequestBody}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.ofNullable(contentLength);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        if (subscribeCalled.compareAndSet(false, true)) {
            delegate.subscribe(s);
            subscribedLatch.countDown();
        } else {
            s.onSubscribe(new NoopSubscription(s));
            s.onError(NonRetryableException.create("A retry was attempted, but AsyncRequestBody.forBlockingOutputStream does not "
                                                   + "support retries."));
        }
    }

    @Override
    public String body() {
        return "Stream";
    }

    private void waitForSubscriptionIfNeeded() {
        try {
            long timeoutSeconds = subscribeTimeout.getSeconds();
            if (!subscribedLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The service request was not made within " + timeoutSeconds + " seconds of "
                                                + "outputStream being invoked. Make sure to invoke the service request "
                                                + "BEFORE invoking outputStream if your caller is single-threaded.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for subscription.", e);
        }
    }

    public static final class Builder {
        private Duration subscribeTimeout;
        private Long contentLength;

        private Builder() {
        }

        /**
         * Defines how long it should wait for this AsyncRequestBody to be subscribed (to start streaming) before timing out.
         * By default, it's 10 seconds.
         *
         * <p>You may want to increase it if the request may not be executed right away.
         *
         * @param subscribeTimeout the timeout
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder subscribeTimeout(Duration subscribeTimeout) {
            this.subscribeTimeout = subscribeTimeout;
            return this;
        }

        /**
         * The content length of the output stream.
         *
         * @param contentLength the content length
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder contentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public BlockingOutputStreamAsyncRequestBody build() {
            return new BlockingOutputStreamAsyncRequestBody(this);
        }
    }
}
