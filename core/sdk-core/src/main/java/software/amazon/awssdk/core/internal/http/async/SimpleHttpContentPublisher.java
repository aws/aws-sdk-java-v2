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

package software.amazon.awssdk.core.internal.http.async;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Implementation of {@link SdkHttpContentPublisher} that provides all it's data at once. Useful for
 * non streaming operations that are already marshalled into memory.
 */
@SdkInternalApi
public final class SimpleHttpContentPublisher implements SdkHttpContentPublisher {

    private static final byte[] EMPTY_CONTENT = new byte[0];

    /**
     * Ceiling on how much is allocated up front from the {@code Content-Length} header. The header is normally exact,
     * but it can be set by a request interceptor, and trusting an absurd value would turn a request that merely fails
     * into an {@link OutOfMemoryError}. Bodies larger than this still read correctly, just via the growing path below.
     */
    private static final int MAX_PRESIZED_ALLOCATION = 8 * 1024 * 1024;

    private static final int GROWING_READ_BUFFER_SIZE = 4 * 1024;

    private final byte[] content;
    private final int length;

    public SimpleHttpContentPublisher(SdkHttpFullRequest request) {
        this(request, MAX_PRESIZED_ALLOCATION);
    }

    @SdkTestInternalApi
    SimpleHttpContentPublisher(SdkHttpFullRequest request, int maxPresizedAllocation) {
        this.content = readContent(request, maxPresizedAllocation);
        this.length = content.length;
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of((long) length);
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        s.onSubscribe(new SubscriptionImpl(s));
    }

    private static byte[] readContent(SdkHttpFullRequest request, int maxPresizedAllocation) {
        ContentStreamProvider provider = request.contentStreamProvider().orElse(null);
        if (provider == null) {
            return EMPTY_CONTENT;
        }
        return invokeSafely(() -> readFully(provider.newStream(), presizeHint(request, maxPresizedAllocation)));
    }

    /**
     * Reads {@code stream} into an exactly-sized array.
     *
     * <p>When the length is known up front this allocates the result array once and reads straight into it. That avoids
     * the staging buffer, the doubling reallocations and the final defensive copy that
     * {@link IoUtils#toByteArray(InputStream)} needs in order to handle an unknown length.
     *
     * <p>The stream is deliberately not closed, matching the previous behavior: some
     * {@link ContentStreamProvider} implementations (notably {@link ContentStreamProvider#fromInputStream(InputStream)})
     * hand back the same stream on every call and rely on mark/reset, so closing it here would break the next retry
     * attempt.
     *
     * @param presizeHint expected length, or a negative number if unknown.
     */
    private static byte[] readFully(InputStream stream, int presizeHint) throws IOException {
        if (presizeHint < 0) {
            return IoUtils.toByteArray(stream);
        }

        byte[] buffer = new byte[presizeHint];
        int read = readUpTo(stream, buffer);

        if (read < presizeHint) {
            // Stream was shorter than advertised. Trim rather than pad the body with trailing zeros.
            byte[] trimmed = new byte[read];
            System.arraycopy(buffer, 0, trimmed, 0, read);
            return trimmed;
        }

        // Either the stream is longer than advertised, or it is longer than MAX_PRESIZED_ALLOCATION. Read the remainder
        // instead of silently truncating the body.
        int next = stream.read();
        if (next < 0) {
            return buffer;
        }
        return readRemainder(stream, buffer, next);
    }

    /**
     * Fills {@code buffer} as far as the stream allows, returning the number of bytes read.
     */
    private static int readUpTo(InputStream stream, byte[] buffer) throws IOException {
        int read = 0;
        while (read < buffer.length) {
            int n = stream.read(buffer, read, buffer.length - read);
            if (n < 0) {
                break;
            }
            read += n;
        }
        return read;
    }

    private static byte[] readRemainder(InputStream stream, byte[] alreadyRead, int nextByte) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(alreadyRead.length * 2);
        out.write(alreadyRead, 0, alreadyRead.length);
        out.write(nextByte);

        byte[] chunk = new byte[GROWING_READ_BUFFER_SIZE];
        int n;
        while ((n = stream.read(chunk)) != -1) {
            out.write(chunk, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * The marshallers and the body-rewriting pipeline stages set {@code Content-Length} for in-memory bodies, so this is
     * normally present and exact.
     *
     * @return the length to pre-allocate, or a negative number if it could not be determined.
     */
    private static int presizeHint(SdkHttpFullRequest request, int maxPresizedAllocation) {
        return request.firstMatchingHeader(Header.CONTENT_LENGTH)
                      .map(contentLength -> parsePresizeHint(contentLength, maxPresizedAllocation))
                      .orElse(-1);
    }

    private static int parsePresizeHint(String contentLength, int maxPresizedAllocation) {
        long parsed;
        try {
            parsed = Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            return -1;
        }

        if (parsed < 0) {
            return -1;
        }
        return (int) Math.min(parsed, maxPresizedAllocation);
    }

    private class SubscriptionImpl implements Subscription {
        private boolean running = true;
        private final Subscriber<? super ByteBuffer> s;

        private SubscriptionImpl(Subscriber<? super ByteBuffer> s) {
            this.s = s;
        }

        @Override
        public void request(long n) {
            if (running) {
                running = false;
                if (n <= 0) {
                    s.onError(new IllegalArgumentException("Demand must be positive"));
                } else {
                    s.onNext(ByteBuffer.wrap(content));
                    s.onComplete();
                }
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
}
