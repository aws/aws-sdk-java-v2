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


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * A {@link SubAsyncRequestBody} implementation that supports resubscribe/retry once all data has been published to the first
 * subscriber
 */
@SdkInternalApi
public final class RetryableSubAsyncRequestBody implements SubAsyncRequestBody {
    private static final Logger log = Logger.loggerFor(RetryableSubAsyncRequestBody.class);
    /**
     * The maximum length of the content this AsyncRequestBody can hold. If the upstream content length is known, this is
     * the same as totalLength
     */
    private final SubAsyncRequestBodyConfiguration configuration;
    private final int partNumber;
    private final boolean contentLengthKnown;
    private final String sourceBodyName;

    private volatile long bufferedLength = 0;
    private volatile ByteBuffersAsyncRequestBody bufferedAsyncRequestBody;
    private List<ByteBuffer> buffers = new ArrayList<>();
    private final AtomicBoolean subscribeCalled = new AtomicBoolean(false);
    private final SimplePublisher<ByteBuffer> delegate = new SimplePublisher<>();
    private final Consumer<Long> onNumBytesReceived;
    private final Consumer<Long> onNumBytesConsumed;
    private final Object buffersLock = new Object();

    /**
     * Creates a new RetryableSubAsyncRequestBody with the given configuration.
     */
    public RetryableSubAsyncRequestBody(SubAsyncRequestBodyConfiguration configuration) {
        this.configuration = Validate.paramNotNull(configuration, "configuration");
        this.partNumber = configuration.partNumber();
        this.contentLengthKnown = configuration.contentLengthKnown();
        this.sourceBodyName = configuration.sourceBodyName();
        this.onNumBytesReceived = configuration.onNumBytesReceived();
        this.onNumBytesConsumed = configuration.onNumBytesConsumed();
    }

    @Override
    public Optional<Long> contentLength() {
        return contentLengthKnown ? Optional.of(configuration.maxLength()) : Optional.of(bufferedLength);
    }

    @Override
    public void send(ByteBuffer data) {
        log.trace(() -> String.format("Sending bytebuffer %s to part number %d", data, partNumber));
        long length = data.remaining();
        bufferedLength += length;

        onNumBytesReceived.accept(length);
        delegate.send(data.asReadOnlyBuffer()).whenComplete((r, t) -> {
            if (t != null) {
                delegate.error(t);
            }
        });
        synchronized (buffersLock) {
            buffers.add(data.asReadOnlyBuffer());
        }
    }

    @Override
    public void complete() {
        log.debug(() -> "Received complete() for part number: " + partNumber);
        // ByteBuffersAsyncRequestBody MUST be created before we complete the current
        // request because retry may happen right after
        synchronized (buffersLock) {
            bufferedAsyncRequestBody = ByteBuffersAsyncRequestBody.of(buffers, bufferedLength);
        }
        delegate.complete().exceptionally(e -> {
            delegate.error(e);
            return null;
        });
    }

    @Override
    public long maxLength() {
        return configuration.maxLength();
    }

    @Override
    public long receivedBytesLength() {
        return bufferedLength;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        log.debug(() -> "Subscribe for part number: " + partNumber);
        if (subscribeCalled.compareAndSet(false, true)) {
            delegate.subscribe(s);
        } else {
            log.debug(() -> "Resubscribe for part number " + partNumber);
            if (bufferedAsyncRequestBody == null) {
                s.onSubscribe(new NoopSubscription(s));
                s.onError(NonRetryableException.create(
                    "A retry was attempted, but data is not buffered successfully for retry for partNumber: " + partNumber));
                return;
            }
            bufferedAsyncRequestBody.subscribe(s);
        }
    }

    @Override
    public void close() {
        try {
            log.debug(() -> "Closing current body " + partNumber);
            onNumBytesConsumed.accept(bufferedLength);
            if (bufferedAsyncRequestBody != null) {
                synchronized (buffersLock) {
                    buffers.clear();
                    buffers = null;
                }
                bufferedAsyncRequestBody.close();
                bufferedAsyncRequestBody = null;
            }
        } catch (Throwable e) {
            log.warn(() -> String.format("Unexpected error thrown from cleaning up AsyncRequestBody for part number %d, "
                                         + "resource may be leaked", partNumber));
        }
    }

    @Override
    public int partNumber() {
        return partNumber;
    }

    @Override
    public String body() {
        return sourceBodyName;
    }

}
