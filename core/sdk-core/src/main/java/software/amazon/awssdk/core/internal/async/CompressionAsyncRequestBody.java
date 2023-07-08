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
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.compression.Compressor;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Wrapper class to wrap an AsyncRequestBody.
 * This will compress the payload with the provided {@link Compressor}.
 */
@SdkInternalApi
public class CompressionAsyncRequestBody implements AsyncRequestBody {

    private static final int COMPRESSION_CHUNK_SIZE = 128 * 1024;
    private final AsyncRequestBody wrapped;
    private final Compressor compressor;
    private final long totalBytes;

    private CompressionAsyncRequestBody(DefaultBuilder builder) {
        Validate.notNull(builder.asyncRequestBody, "wrapped AsyncRequestBody cannot be null");
        Validate.notNull(builder.compressor, "compressor cannot be null");
        this.wrapped = builder.asyncRequestBody;
        this.compressor = builder.compressor;
        this.totalBytes = wrapped.contentLength()
                                 .orElseThrow(() -> new UnsupportedOperationException("Content length must be supplied."));
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
    }

    private static final class DefaultBuilder implements Builder {

        private AsyncRequestBody asyncRequestBody;
        private Compressor compressor;

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
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.empty();
    }

    @Override
    public String contentType() {
        return wrapped.contentType();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        Validate.notNull(s, "Subscription MUST NOT be null.");

        SynchronousChunkBuffer synchronousChunkBuffer = new SynchronousChunkBuffer(totalBytes);
        wrapped.flatMapIterable(synchronousChunkBuffer::buffer)
               .subscribe(new CompressionSubscriber(s, compressor, totalBytes));
    }

    private static final class CompressionSubscriber implements Subscriber<ByteBuffer> {

        private final Subscriber<? super ByteBuffer> wrapped;
        private final Compressor compressor;
        private final AtomicLong remainingBytes;

        CompressionSubscriber(Subscriber<? super ByteBuffer> wrapped, Compressor compressor, long totalBytes) {
            this.wrapped = wrapped;
            this.compressor = compressor;
            this.remainingBytes = new AtomicLong(totalBytes);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            wrapped.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            this.remainingBytes.addAndGet(-byteBuffer.remaining());
            ByteBuffer compressedBuffer = compressor.compress(byteBuffer);
            wrapped.onNext(compressedBuffer);
        }

        @Override
        public void onError(Throwable t) {
            wrapped.onError(t);
        }

        @Override
        public void onComplete() {
            wrapped.onComplete();
        }
    }

    private static final class SynchronousChunkBuffer {
        private final ChunkBuffer chunkBuffer;

        SynchronousChunkBuffer(long totalBytes) {
            this.chunkBuffer = ChunkBuffer.builder().bufferSize(COMPRESSION_CHUNK_SIZE).totalBytes(totalBytes).build();
        }

        private Iterable<ByteBuffer> buffer(ByteBuffer bytes) {
            return chunkBuffer.bufferAndCreateChunks(bytes);
        }
    }
}
