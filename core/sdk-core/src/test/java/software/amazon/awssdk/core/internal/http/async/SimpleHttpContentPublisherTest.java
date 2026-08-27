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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

class SimpleHttpContentPublisherTest {

    private static final byte[] BODY = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    @Test
    void contentLength_whenNoContentStreamProvider_isZero() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestBuilder().build());

        assertThat(publisher.contentLength()).hasValue(0L);
    }

    @Test
    void subscribe_whenNoContentStreamProvider_publishesEmptyBuffer() {
        CollectingSubscriber subscriber = drain(new SimpleHttpContentPublisher(requestBuilder().build()));

        assertThat(subscriber.content()).isEmpty();
        assertThat(subscriber.bufferCount()).isEqualTo(1);
    }

    @Test
    void subscribe_whenContentLengthMatchesBody_publishesFullBody() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length));

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    @Test
    void subscribe_whenContentLengthHeaderAbsent_publishesFullBody() {
        SdkHttpFullRequest request = requestBuilder().contentStreamProvider(() -> new ByteArrayInputStream(BODY)).build();

        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(request);

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    /**
     * An unusable Content-Length must fall back to reading the stream rather than failing or truncating.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "not-a-number", "-1", "12.5", "99999999999999999999999999"})
    void subscribe_whenContentLengthHeaderIsUnusable_publishesFullBody(String contentLength) {
        SdkHttpFullRequest request = requestBuilder().contentStreamProvider(() -> new ByteArrayInputStream(BODY))
                                                    .putHeader(Header.CONTENT_LENGTH, contentLength)
                                                    .build();

        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(request);

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    @Test
    void subscribe_whenContentLengthIsZeroAndBodyEmpty_publishesEmptyBuffer() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(new byte[0], 0));

        assertThat(publisher.contentLength()).hasValue(0L);
        assertThat(drain(publisher).content()).isEmpty();
    }

    /**
     * The pre-sized read has to loop, because an {@link InputStream} may return fewer bytes than asked for.
     */
    @Test
    void subscribe_whenStreamReturnsPartialReads_publishesFullBody() {
        SdkHttpFullRequest request =
            requestBuilder().contentStreamProvider(() -> new OneByteAtATimeInputStream(BODY))
                            .putHeader(Header.CONTENT_LENGTH, Integer.toString(BODY.length))
                            .build();

        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(request);

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    /**
     * Content-Length larger than the stream: publish what is actually there rather than padding with trailing zeros.
     */
    @Test
    void subscribe_whenContentLengthLongerThanStream_publishesOnlyActualBytes() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length + 100));

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    /**
     * Content-Length smaller than the stream: read the remainder rather than truncating the body.
     */
    @Test
    void subscribe_whenContentLengthShorterThanStream_publishesFullBody() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, 5));

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    /**
     * A body larger than the pre-size ceiling still has to be published in full.
     */
    @Test
    void subscribe_whenBodyLargerThanPresizeCeiling_publishesFullBody() {
        SimpleHttpContentPublisher publisher =
            new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length), 8);

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    /**
     * The ceiling exists so that a bogus Content-Length cannot drive a huge allocation. The body still has to be
     * published in full, and the reported length has to come from the body rather than the header.
     */
    @Test
    void subscribe_whenContentLengthFarExceedsPresizeCeiling_publishesFullBody() {
        SimpleHttpContentPublisher publisher =
            new SimpleHttpContentPublisher(requestWithBody(BODY, Integer.MAX_VALUE), 8);

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    /**
     * Same as above but through the production ceiling, so a {@code Content-Length} of ~2 GiB must not be allocated.
     */
    @Test
    void subscribe_whenContentLengthIsIntMaxValue_publishesFullBodyWithoutExhaustingMemory() {
        SimpleHttpContentPublisher publisher =
            new SimpleHttpContentPublisher(requestWithBody(BODY, Integer.MAX_VALUE));

        assertThat(publisher.contentLength()).hasValue((long) BODY.length);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    @Test
    void subscribe_whenSubscribedTwice_eachSubscriberReceivesFullBody() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length));

        assertThat(drain(publisher).content()).isEqualTo(BODY);
        assertThat(drain(publisher).content()).isEqualTo(BODY);
    }

    /**
     * A retry constructs a new publisher from the same request. Both attempts must see identical bytes.
     *
     * <p>Uses a stream that fails reads after {@code close()} and is exposed through
     * {@link ContentStreamProvider#fromInputStream(InputStream)}, which hands back the <em>same</em> stream on every
     * call and relies on mark/reset. Closing the stream while buffering would break the second attempt.
     */
    @Test
    void construct_whenProviderReusesStreamAcrossAttempts_bothAttemptsSeeSameBody() {
        CloseAwareInputStream stream = new CloseAwareInputStream(BODY);
        SdkHttpFullRequest request = requestBuilder().contentStreamProvider(ContentStreamProvider.fromInputStream(stream))
                                                    .putHeader(Header.CONTENT_LENGTH, Integer.toString(BODY.length))
                                                    .build();

        byte[] firstAttempt = drain(new SimpleHttpContentPublisher(request)).content();
        byte[] secondAttempt = drain(new SimpleHttpContentPublisher(request)).content();

        assertThat(firstAttempt).isEqualTo(BODY);
        assertThat(secondAttempt).isEqualTo(BODY);
        assertThat(stream.closeCount()).isZero();
    }

    @Test
    void construct_whenContentLengthHeaderAbsent_doesNotCloseProviderStream() {
        CloseAwareInputStream stream = new CloseAwareInputStream(BODY);
        SdkHttpFullRequest request = requestBuilder().contentStreamProvider(() -> stream).build();

        assertThat(drain(new SimpleHttpContentPublisher(request)).content()).isEqualTo(BODY);
        assertThat(stream.closeCount()).isZero();
    }

    @Test
    void request_whenDemandIsNotPositive_signalsError() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length));
        CollectingSubscriber subscriber = new CollectingSubscriber(0);
        publisher.subscribe(subscriber);

        subscriber.subscription().request(0);

        assertThat(subscriber.error()).isInstanceOf(IllegalArgumentException.class)
                                      .hasMessageContaining("Demand must be positive");
        assertThat(subscriber.bufferCount()).isZero();
        assertThat(subscriber.completed()).isFalse();
    }

    @Test
    void request_whenCalledRepeatedly_publishesBodyOnce() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length));
        CollectingSubscriber subscriber = new CollectingSubscriber(0);
        publisher.subscribe(subscriber);

        subscriber.subscription().request(1);
        subscriber.subscription().request(1);
        subscriber.subscription().request(Long.MAX_VALUE);

        assertThat(subscriber.bufferCount()).isEqualTo(1);
        assertThat(subscriber.content()).isEqualTo(BODY);
        assertThat(subscriber.completeCount()).isEqualTo(1);
    }

    @Test
    void cancel_whenCancelledBeforeRequest_publishesNothing() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length));
        CollectingSubscriber subscriber = new CollectingSubscriber(0);
        publisher.subscribe(subscriber);

        subscriber.subscription().cancel();
        subscriber.subscription().request(Long.MAX_VALUE);

        assertThat(subscriber.bufferCount()).isZero();
        assertThat(subscriber.completed()).isFalse();
        assertThat(subscriber.error()).isNull();
    }

    /**
     * The published buffer has to line up with what {@link SimpleHttpContentPublisher#contentLength()} advertises, since
     * the HTTP clients use the two together to frame the request.
     */
    @Test
    void subscribe_whenBodyPublished_bufferRemainingMatchesContentLength() {
        SimpleHttpContentPublisher publisher = new SimpleHttpContentPublisher(requestWithBody(BODY, BODY.length));

        CollectingSubscriber subscriber = drain(publisher);

        assertThat(subscriber.totalRemaining()).isEqualTo(publisher.contentLength().get());
    }

    private static SdkHttpFullRequest.Builder requestBuilder() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("https://aws.amazon.com"))
                                 .method(SdkHttpMethod.POST);
    }

    private static SdkHttpFullRequest requestWithBody(byte[] body, long advertisedContentLength) {
        return requestBuilder().contentStreamProvider(() -> new ByteArrayInputStream(body))
                               .putHeader(Header.CONTENT_LENGTH, Long.toString(advertisedContentLength))
                               .build();
    }

    private static CollectingSubscriber drain(SimpleHttpContentPublisher publisher) {
        CollectingSubscriber subscriber = new CollectingSubscriber(Long.MAX_VALUE);
        publisher.subscribe(subscriber);
        assertThat(subscriber.error()).isNull();
        assertThat(subscriber.completed()).isTrue();
        return subscriber;
    }

    private static final class CollectingSubscriber implements Subscriber<ByteBuffer> {
        private final long initialDemand;
        private final List<byte[]> buffers = new ArrayList<>();
        private Subscription subscription;
        private Throwable error;
        private int completeCount;
        private long totalRemaining;

        private CollectingSubscriber(long initialDemand) {
            this.initialDemand = initialDemand;
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            if (initialDemand > 0) {
                s.request(initialDemand);
            }
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            totalRemaining += byteBuffer.remaining();
            byte[] buffer = new byte[byteBuffer.remaining()];
            byteBuffer.get(buffer);
            buffers.add(buffer);
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onComplete() {
            completeCount++;
        }

        private Subscription subscription() {
            return subscription;
        }

        private Throwable error() {
            return error;
        }

        private boolean completed() {
            return completeCount > 0;
        }

        private int completeCount() {
            return completeCount;
        }

        private int bufferCount() {
            return buffers.size();
        }

        private long totalRemaining() {
            return totalRemaining;
        }

        private byte[] content() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            buffers.forEach(b -> out.write(b, 0, b.length));
            return out.toByteArray();
        }
    }

    /**
     * Returns at most one byte per read, to exercise the pre-sized read loop.
     */
    private static final class OneByteAtATimeInputStream extends FilterInputStream {
        private OneByteAtATimeInputStream(byte[] content) {
            super(new ByteArrayInputStream(content));
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, Math.min(len, 1));
        }
    }

    /**
     * Models a real stream: reads fail once it has been closed. {@link ByteArrayInputStream} alone cannot catch an
     * unwanted {@code close()} because its {@code close()} is a no-op.
     */
    private static final class CloseAwareInputStream extends FilterInputStream {
        private boolean closed;
        private int closeCount;

        private CloseAwareInputStream(byte[] content) {
            super(new ByteArrayInputStream(content));
        }

        @Override
        public int read() throws IOException {
            throwIfClosed();
            return super.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throwIfClosed();
            return super.read(b, off, len);
        }

        @Override
        public synchronized void reset() throws IOException {
            throwIfClosed();
            super.reset();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            closeCount++;
            super.close();
        }

        private int closeCount() {
            return closeCount;
        }

        private void throwIfClosed() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }
    }
}
