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
import org.reactivestreams.Subscriber;
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
    private final ChunkBuffer chunkBuffer;

    private CompressionAsyncRequestBody(DefaultBuilder builder) {
        this.wrapped = Validate.paramNotNull(builder.asyncRequestBody, "asyncRequestBody");
        this.compressor = Validate.paramNotNull(builder.compressor, "compressor");
        int chunkSize = builder.chunkSize != null ? builder.chunkSize : DEFAULT_CHUNK_SIZE;
        this.chunkBuffer = ChunkBuffer.builder()
                                      .bufferSize(chunkSize)
                                      .build();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
        Validate.notNull(s, "Subscription MUST NOT be null.");

        SdkPublisher<Iterable<ByteBuffer>> split =
            split(wrapped).addTrailingData(() -> Collections.singleton(getBufferedDataIfPresent()));
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
        return subscriber -> source.subscribe(new SplittingSubscriber(subscriber));
    }

    private Iterable<ByteBuffer> getBufferedDataIfPresent() {
        return chunkBuffer.getBufferedData()
                          .map(Collections::singletonList)
                          .orElse(Collections.emptyList());
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

    private final class SplittingSubscriber extends DelegatingSubscriber<ByteBuffer, Iterable<ByteBuffer>> {

        protected SplittingSubscriber(Subscriber<? super Iterable<ByteBuffer>> subscriber) {
            super(subscriber);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            Iterable<ByteBuffer> buffers = chunkBuffer.split(byteBuffer);
            subscriber.onNext(buffers);
        }
    }
}
