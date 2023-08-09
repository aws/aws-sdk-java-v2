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

import static software.amazon.awssdk.core.HttpChecksumConstant.DEFAULT_ASYNC_CHUNK_SIZE;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.LAST_BYTE_LEN;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.calculateChecksumTrailerLength;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.calculateChunkLength;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.createChecksumTrailer;
import static software.amazon.awssdk.core.internal.util.ChunkContentUtils.createChunk;

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Wrapper class to wrap an AsyncRequestBody.
 * This will read the data in chunk format and append Checksum as trailer at the end.
 */
@SdkInternalApi
public class ChecksumCalculatingAsyncRequestBody implements AsyncRequestBody {

    private static final byte[] FINAL_BYTE = new byte[0];
    private final AsyncRequestBody wrapped;
    private final SdkChecksum sdkChecksum;
    private final Algorithm algorithm;
    private final String trailerHeader;
    private final long totalBytes;

    private ChecksumCalculatingAsyncRequestBody(DefaultBuilder builder) {
        Validate.notNull(builder.asyncRequestBody, "wrapped AsyncRequestBody cannot be null");
        Validate.notNull(builder.algorithm, "algorithm cannot be null");
        Validate.notNull(builder.trailerHeader, "trailerHeader cannot be null");
        this.wrapped = builder.asyncRequestBody;
        this.algorithm = builder.algorithm;
        this.sdkChecksum = builder.algorithm != null ? SdkChecksum.forAlgorithm(algorithm) : null;
        this.trailerHeader = builder.trailerHeader;
        this.totalBytes = wrapped.contentLength()
                                 .orElseThrow(() -> new UnsupportedOperationException("Content length must be supplied."));
    }

    /**
     * @return Builder instance to construct a {@link FileAsyncRequestBody}.
     */
    public static ChecksumCalculatingAsyncRequestBody.Builder builder() {
        return new DefaultBuilder();
    }

    public interface Builder extends SdkBuilder<ChecksumCalculatingAsyncRequestBody.Builder,
            ChecksumCalculatingAsyncRequestBody> {

        /**
         * Sets the AsyncRequestBody that will be wrapped.
         * @param asyncRequestBody AsyncRequestBody.
         * @return This builder for method chaining.
         */
        ChecksumCalculatingAsyncRequestBody.Builder asyncRequestBody(AsyncRequestBody asyncRequestBody);


        /**
         * Sets the checksum algorithm.
         * @param algorithm algorithm that is used to compute the checksum.
         * @return  This builder for method chaining.
         */
        ChecksumCalculatingAsyncRequestBody.Builder algorithm(Algorithm algorithm);

        /**
         * Sets the Trailer header where computed SdkChecksum will be updated.
         * @param trailerHeader Trailer header name which will be appended at the end of the string.
         * @return This builder for method chaining.
         */
        ChecksumCalculatingAsyncRequestBody.Builder trailerHeader(String trailerHeader);

    }

    private static final class DefaultBuilder implements ChecksumCalculatingAsyncRequestBody.Builder {

        private AsyncRequestBody asyncRequestBody;
        private Algorithm algorithm;
        private String trailerHeader;


        @Override
        public ChecksumCalculatingAsyncRequestBody build() {
            return new ChecksumCalculatingAsyncRequestBody(this);
        }

        @Override
        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
            return this;
        }

        @Override
        public ChecksumCalculatingAsyncRequestBody.Builder algorithm(Algorithm algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        @Override
        public ChecksumCalculatingAsyncRequestBody.Builder trailerHeader(String trailerHeader) {
            this.trailerHeader = trailerHeader;
            return this;
        }
    }

    @Override
    public Optional<Long> contentLength() {
        if (wrapped.contentLength().isPresent() && algorithm != null) {
            return Optional.of(calculateChunkLength(wrapped.contentLength().get())
                               + LAST_BYTE_LEN
                               + calculateChecksumTrailerLength(algorithm, trailerHeader));
        }
        return wrapped.contentLength();
    }

    @Override
    public String contentType() {
        return wrapped.contentType();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        Validate.notNull(s, "Subscription MUST NOT be null.");
        if (sdkChecksum != null) {
            sdkChecksum.reset();
        }

        SynchronousChunkBuffer synchronousChunkBuffer = new SynchronousChunkBuffer(totalBytes);
        wrapped.flatMapIterable(synchronousChunkBuffer::buffer)
               .subscribe(new ChecksumCalculatingSubscriber(s, sdkChecksum, trailerHeader));
    }

    private static final class ChecksumCalculatingSubscriber implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> wrapped;
        private final SdkChecksum checksum;
        private final String trailerHeader;
        private Subscription subscription;

        ChecksumCalculatingSubscriber(Subscriber<? super ByteBuffer> wrapped,
                                      SdkChecksum checksum,
                                      String trailerHeader) {
            this.wrapped = wrapped;
            this.checksum = checksum;
            this.trailerHeader = trailerHeader;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            wrapped.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            try {
                if (byteBuffer.remaining() < 1) {
                    return;
                }
                byteBuffer.mark();
                checksum.update(byteBuffer);
                byteBuffer.reset();
                ByteBuffer allocatedBuffer = createChunk(byteBuffer, false);
                wrapped.onNext(allocatedBuffer);
            } catch (SdkException sdkException) {
                this.subscription.cancel();
                onError(sdkException);
            }
        }

        @Override
        public void onError(Throwable t) {
            wrapped.onError(t);
        }

        @Override
        public void onComplete() {
            wrapped.onNext(finalChunkWithAppendedChecksumTrailer());
            wrapped.onComplete();
        }

        private ByteBuffer finalChunkWithAppendedChecksumTrailer() {
            ByteBuffer finalChunkedByteBuffer = createChunk(ByteBuffer.wrap(FINAL_BYTE), true);
            ByteBuffer checksumTrailerByteBuffer = createChecksumTrailer(BinaryUtils.toBase64(checksum.getChecksumBytes()),
                                                                         trailerHeader);
            ByteBuffer checksumAppendedBuffer = ByteBuffer.allocate(finalChunkedByteBuffer.remaining() +
                                                                    checksumTrailerByteBuffer.remaining());
            checksumAppendedBuffer.put(finalChunkedByteBuffer)
                                  .put(checksumTrailerByteBuffer);
            checksumAppendedBuffer.flip();
            return checksumAppendedBuffer;
        }
    }

    private static final class SynchronousChunkBuffer {
        private final ChunkBuffer chunkBuffer;

        SynchronousChunkBuffer(long totalBytes) {
            this.chunkBuffer = ChunkBuffer.builder().bufferSize(DEFAULT_ASYNC_CHUNK_SIZE).totalBytes(totalBytes).build();
        }

        private Iterable<ByteBuffer> buffer(ByteBuffer bytes) {
            return chunkBuffer.split(bytes);
        }
    }

}