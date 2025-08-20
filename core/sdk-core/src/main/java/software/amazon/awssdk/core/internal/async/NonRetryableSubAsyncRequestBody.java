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
 * A {@link SubAsyncRequestBody} implementation that doesn't support resubscribe/retry
 */
@SdkInternalApi
public final class NonRetryableSubAsyncRequestBody implements SubAsyncRequestBody {
    private static final Logger log = Logger.loggerFor(NonRetryableSubAsyncRequestBody.class);
    private final SubAsyncRequestBodyConfiguration configuration;
    private final int partNumber;
    private final boolean contentLengthKnown;
    private final String sourceBodyName;
    private final SimplePublisher<ByteBuffer> delegate = new SimplePublisher<>();
    private final AtomicBoolean subscribeCalled = new AtomicBoolean(false);
    private volatile long bufferedLength = 0;
    private final Consumer<Long> onNumBytesReceived;
    private final Consumer<Long> onNumBytesConsumed;

    /**
     * Creates a new NonRetryableSubAsyncRequestBody with the given configuration.
     */
    public NonRetryableSubAsyncRequestBody(SubAsyncRequestBodyConfiguration configuration) {
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

    public void send(ByteBuffer data) {
        log.debug(() -> String.format("Sending bytebuffer %s to part %d", data, partNumber));
        int length = data.remaining();
        bufferedLength += length;
        onNumBytesReceived.accept((long) length);
        delegate.send(data).whenComplete((r, t) -> {
            onNumBytesConsumed.accept((long) length);
            if (t != null) {
                error(t);
            }
        });
    }

    public void complete() {
        log.debug(() -> "Received complete() for part number: " + partNumber);
        delegate.complete().whenComplete((r, t) -> {
            if (t != null) {
                error(t);
            }
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
    public boolean contentLengthKnown() {
        return contentLengthKnown;
    }

    @Override
    public int partNumber() {
        return partNumber;
    }

    public void error(Throwable error) {
        delegate.error(error);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        if (subscribeCalled.compareAndSet(false, true)) {
            delegate.subscribe(s);
        } else {
            s.onSubscribe(new NoopSubscription(s));
            s.onError(NonRetryableException.create(
                "A retry was attempted, but the provided source AsyncRequestBody does not "
                + "support splitting to retryable AsyncRequestBody. Consider using BufferedSplittableAsyncRequestBody."));
        }
    }

    @Override
    public String body() {
        return sourceBodyName;
    }
}
