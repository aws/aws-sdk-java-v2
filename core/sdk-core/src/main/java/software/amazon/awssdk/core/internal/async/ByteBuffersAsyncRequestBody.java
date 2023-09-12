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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.utils.Logger;

/**
 * An implementation of {@link AsyncRequestBody} for providing data from the supplied {@link ByteBuffer} array. This is created
 * using static methods on {@link AsyncRequestBody}
 *
 * @see AsyncRequestBody#fromBytes(byte[])
 * @see AsyncRequestBody#fromBytesUnsafe(byte[])
 * @see AsyncRequestBody#fromByteBuffer(ByteBuffer)
 * @see AsyncRequestBody#fromByteBufferUnsafe(ByteBuffer)
 * @see AsyncRequestBody#fromByteBuffers(ByteBuffer...)
 * @see AsyncRequestBody#fromByteBuffersUnsafe(ByteBuffer...)
 * @see AsyncRequestBody#fromString(String)
 */
@SdkInternalApi
public final class ByteBuffersAsyncRequestBody implements AsyncRequestBody {
    private static final Logger log = Logger.loggerFor(ByteBuffersAsyncRequestBody.class);

    private final String mimetype;
    private final Long length;
    private final ByteBuffer[] buffers;

    private ByteBuffersAsyncRequestBody(String mimetype, Long length, ByteBuffer... buffers) {
        this.mimetype = mimetype;
        this.length = length;
        this.buffers = buffers;
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.ofNullable(length);
    }

    @Override
    public String contentType() {
        return mimetype;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        // As per rule 1.9 we must throw NullPointerException if the subscriber parameter is null
        if (s == null) {
            throw new NullPointerException("Subscription MUST NOT be null.");
        }

        // As per 2.13, this method must return normally (i.e. not throw).
        try {
            s.onSubscribe(
                new Subscription() {
                    private final AtomicInteger index = new AtomicInteger(0);
                    private final AtomicBoolean completed = new AtomicBoolean(false);

                    @Override
                    public void request(long n) {
                        if (completed.get()) {
                            return;
                        }

                        if (n > 0) {
                            int i = index.getAndIncrement();

                            if (i >= buffers.length) {
                                return;
                            }

                            long remaining = n;

                            do {
                                ByteBuffer buffer = buffers[i];

                                s.onNext(buffer.asReadOnlyBuffer());
                                remaining--;
                            } while (remaining > 0 && (i = index.getAndIncrement()) < buffers.length);

                            if (i >= buffers.length - 1 && completed.compareAndSet(false, true)) {
                                s.onComplete();
                            }
                        } else {
                            s.onError(new IllegalArgumentException("§3.9: non-positive requests are not allowed!"));
                        }
                    }

                    @Override
                    public void cancel() {
                        completed.set(true);
                    }
                }
            );
        } catch (Throwable ex) {
            log.error(() -> s + " violated the Reactive Streams rule 2.13 by throwing an exception from onSubscribe.", ex);
        }
    }

    public static ByteBuffersAsyncRequestBody of(ByteBuffer... buffers) {
        long length = Arrays.stream(buffers)
                         .mapToLong(ByteBuffer::remaining)
                         .sum();
        return new ByteBuffersAsyncRequestBody(Mimetype.MIMETYPE_OCTET_STREAM, length, buffers);
    }

    public static ByteBuffersAsyncRequestBody of(Long length, ByteBuffer... buffers) {
        return new ByteBuffersAsyncRequestBody(Mimetype.MIMETYPE_OCTET_STREAM, length, buffers);
    }

    public static ByteBuffersAsyncRequestBody of(String mimetype, ByteBuffer... buffers) {
        long length = Arrays.stream(buffers)
                            .mapToLong(ByteBuffer::remaining)
                            .sum();
        return new ByteBuffersAsyncRequestBody(mimetype, length, buffers);
    }

    public static ByteBuffersAsyncRequestBody of(String mimetype, Long length, ByteBuffer... buffers) {
        return new ByteBuffersAsyncRequestBody(mimetype, length, buffers);
    }

    public static ByteBuffersAsyncRequestBody from(byte[] bytes) {
        return new ByteBuffersAsyncRequestBody(Mimetype.MIMETYPE_OCTET_STREAM, (long) bytes.length,
                                               ByteBuffer.wrap(bytes));
    }

    public static ByteBuffersAsyncRequestBody from(String mimetype, byte[] bytes) {
        return new ByteBuffersAsyncRequestBody(mimetype, (long) bytes.length, ByteBuffer.wrap(bytes));
    }
}
