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
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.InputStreamConsumingPublisher;

/**
 * An implementation of {@link AsyncRequestBody} that allows performing a blocking write of an input stream to a downstream
 * service.
 *
 * <p>See {@link AsyncRequestBody#forBlockingInputStream(Long)}.
 */
@SdkPublicApi
public final class BlockingInputStreamAsyncRequestBody implements AsyncRequestBody {
    private static final Duration DEFAULT_SUBSCRIBE_TIMEOUT = Duration.ofSeconds(10);
    private static final String DEFAULT_CONTENT_TYPE = Mimetype.MIMETYPE_OCTET_STREAM;
    private final InputStreamConsumingPublisher delegate = new InputStreamConsumingPublisher();
    private final CountDownLatch subscribedLatch = new CountDownLatch(1);
    private final AtomicBoolean subscribeCalled = new AtomicBoolean(false);
    private final Long contentLength;
    private final String contentType;
    private final Duration subscribeTimeout;

    BlockingInputStreamAsyncRequestBody(Builder builder) {
        this.contentLength = builder.contentLength;
        this.contentType = builder.contentType != null ? builder.contentType : DEFAULT_CONTENT_TYPE;
        this.subscribeTimeout = Validate.isPositiveOrNull(builder.subscribeTimeout, "subscribeTimeout") != null ?
                                builder.subscribeTimeout :
                                DEFAULT_SUBSCRIBE_TIMEOUT;
    }

    /**
     * Creates a default builder for {@link BlockingInputStreamAsyncRequestBody}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.ofNullable(contentLength);
    }

    @Override
    public String contentType() {
        return contentType;
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

    @Override
    public String bodyName() {
        return "Stream";
    }

    private void waitForSubscriptionIfNeeded() throws InterruptedException {
        long timeoutSeconds = subscribeTimeout.getSeconds();
        if (!subscribedLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The service request was not made within " + timeoutSeconds + " seconds of "
                                            + "doBlockingWrite being invoked. Make sure to invoke the service request "
                                            + "BEFORE invoking doBlockingWrite if your caller is single-threaded.");
        }
    }

    public static final class Builder {
        private Duration subscribeTimeout;
        private Long contentLength;
        private String contentType;

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

        /**
         * The content type of the output stream.
         *
         * @param contentType the content type
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public BlockingInputStreamAsyncRequestBody build() {
            return new BlockingInputStreamAsyncRequestBody(this);
        }
    }
}
