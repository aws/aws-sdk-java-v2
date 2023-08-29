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

import static software.amazon.awssdk.core.internal.io.AwsChunkedInputStream.DEFAULT_CHUNK_SIZE;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DelegatingSubscriber;
import software.amazon.awssdk.utils.async.FlatteningSubscriber;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Wrapper class to wrap an AsyncRequestBody.
 * This will chunk and compress the payload with the provided {@link Compressor}.
 */
@SdkInternalApi
public class CompressionAsyncRequestBody implements AsyncRequestBody {

    private final AsyncRequestBody wrapped;
    private final Compressor compressor;
    private final int chunkSize;

    private CompressionAsyncRequestBody(DefaultBuilder builder) {
        this.wrapped = Validate.paramNotNull(builder.asyncRequestBody, "asyncRequestBody");
        this.compressor = Validate.paramNotNull(builder.compressor, "compressor");
        this.chunkSize = builder.chunkSize != null ? builder.chunkSize : DEFAULT_CHUNK_SIZE;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        Validate.notNull(s, "Subscription MUST NOT be null.");

        SdkPublisher<Iterable<ByteBuffer>> split = split(wrapped);
        SdkPublisher<ByteBuffer> flattening = flattening(split);
        flattening.map(compressor::compress).subscribe(s);
    }

    @Override
    public Optional<Long> contentLength() {
        return wrapped.contentLength();
    }

    @Override
    public String contentType() {
        return wrapped.contentType();
    }

    private SdkPublisher<Iterable<ByteBuffer>> split(SdkPublisher<ByteBuffer> source) {
        return subscriber -> source.subscribe(new SplittingSubscriber(subscriber, chunkSize));
    }

    private SdkPublisher<ByteBuffer> flattening(SdkPublisher<Iterable<ByteBuffer>> source) {
        return subscriber -> source.subscribe(new FlatteningSubscriber<>(subscriber));
    }

    /**
     * @return Builder instance to construct a {@link CompressionAsyncRequestBody}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder extends SdkBuilder<CompressionAsyncRequestBody.Builder, CompressionAsyncRequestBody> {

        /**
         * Sets the AsyncRequestBody that will be wrapped.
         * @param asyncRequestBody
         * @return This builder for method chaining.
         */
        Builder asyncRequestBody(AsyncRequestBody asyncRequestBody);

        /**
         * Sets the compressor to compress the request.
         * @param compressor
         * @return This builder for method chaining.
         */
        Builder compressor(Compressor compressor);

        /**
         * Sets the chunk size. Default size is 128 * 1024.
         * @param chunkSize
         * @return This builder for method chaining.
         */
        Builder chunkSize(Integer chunkSize);
    }

    private static final class DefaultBuilder implements Builder {

        private AsyncRequestBody asyncRequestBody;
        private Compressor compressor;
        private Integer chunkSize;

        @Override
        public CompressionAsyncRequestBody build() {
            return new CompressionAsyncRequestBody(this);
        }

        @Override
        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
            return this;
        }

        @Override
        public Builder compressor(Compressor compressor) {
            this.compressor = compressor;
            return this;
        }

        @Override
        public Builder chunkSize(Integer chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }
    }

    private static final class SplittingSubscriber extends DelegatingSubscriber<ByteBuffer, Iterable<ByteBuffer>> {
        private final ChunkBuffer chunkBuffer;
        private final AtomicBoolean upstreamDone = new AtomicBoolean(false);
        private final AtomicLong downstreamDemand = new AtomicLong();
        private final Object lock = new Object();
        private volatile boolean sentFinalChunk = false;

        protected SplittingSubscriber(Subscriber<? super Iterable<ByteBuffer>> subscriber, int chunkSize) {
            super(subscriber);
            this.chunkBuffer = ChunkBuffer.builder()
                                          .bufferSize(chunkSize)
                                          .build();
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        throw new IllegalArgumentException("n > 0 required but it was " + n);
                    }

                    downstreamDemand.getAndAdd(n);

                    if (upstreamDone.get()) {
                        sendFinalChunk();
                    } else {
                        s.request(n);
                    }
                }

                @Override
                public void cancel() {
                    s.cancel();
                }
            });
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            downstreamDemand.decrementAndGet();
            Iterable<ByteBuffer> buffers = chunkBuffer.split(byteBuffer);
            subscriber.onNext(buffers);
        }

        @Override
        public void onComplete() {
            upstreamDone.compareAndSet(false, true);
            if (downstreamDemand.get() > 0) {
                sendFinalChunk();
            }
        }

        @Override
        public void onError(Throwable t) {
            upstreamDone.compareAndSet(false, true);
            super.onError(t);
        }

        private void sendFinalChunk() {
            synchronized (lock) {
                if (!sentFinalChunk) {
                    sentFinalChunk = true;
                    Optional<ByteBuffer> byteBuffer = chunkBuffer.getBufferedData();
                    byteBuffer.ifPresent(buffer -> subscriber.onNext(Collections.singletonList(buffer)));
                    subscriber.onComplete();
                }
            }
        }
    }
}
