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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Class that will buffer incoming BufferBytes of totalBytes length to chunks of bufferSize*
 */
@SdkInternalApi
public final class ChunkBuffer {
    private static final Logger log = Logger.loggerFor(ChunkBuffer.class);
    private final AtomicLong transferredBytes;
    private final ByteBuffer currentBuffer;
    private final int chunkSize;
    private final long totalBytes;

    private ChunkBuffer(Long totalBytes, Integer bufferSize) {
        Validate.notNull(totalBytes, "The totalBytes must not be null");

        int chunkSize = bufferSize != null ? bufferSize : DEFAULT_ASYNC_CHUNK_SIZE;
        this.chunkSize = chunkSize;
        this.currentBuffer = ByteBuffer.allocate(chunkSize);
        this.totalBytes = totalBytes;
        this.transferredBytes = new AtomicLong(0);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }


    /**
     * Split the input {@link ByteBuffer} into multiple smaller {@link ByteBuffer}s, each of which contains {@link #chunkSize}
     * worth of bytes. If the last chunk of the input ByteBuffer contains less than {@link #chunkSize} data, the last chunk will
     * be buffered.
     */
    public synchronized Iterable<ByteBuffer> split(ByteBuffer inputByteBuffer) {

        if (!inputByteBuffer.hasRemaining()) {
            return Collections.singletonList(inputByteBuffer);
        }

        List<ByteBuffer> byteBuffers = new ArrayList<>();

        // If current buffer is not empty, fill the buffer first.
        if (currentBuffer.position() != 0) {
            fillCurrentBuffer(inputByteBuffer);

            if (isCurrentBufferFull()) {
                addCurrentBufferToIterable(byteBuffers, chunkSize);
            }
        }

        // If the input buffer is not empty, split the input buffer
        if (inputByteBuffer.hasRemaining()) {
            splitRemainingInputByteBuffer(inputByteBuffer, byteBuffers);
        }

        // If this is the last chunk, add data buffered to the iterable
        if (isLastChunk()) {
            int remainingBytesInBuffer = currentBuffer.position();
            addCurrentBufferToIterable(byteBuffers, remainingBytesInBuffer);
        }
        return byteBuffers;
    }

    private boolean isCurrentBufferFull() {
        return currentBuffer.position() == chunkSize;
    }

    /**
     * Splits the input ByteBuffer to multiple chunks and add them to the iterable.
     */
    private void splitRemainingInputByteBuffer(ByteBuffer inputByteBuffer, List<ByteBuffer> byteBuffers) {
        while (inputByteBuffer.hasRemaining()) {
            ByteBuffer inputByteBufferCopy = inputByteBuffer.asReadOnlyBuffer();
            if (inputByteBuffer.remaining() < chunkSize) {
                currentBuffer.put(inputByteBuffer);
                break;
            }

            int newLimit = inputByteBufferCopy.position() + chunkSize;
            inputByteBufferCopy.limit(newLimit);
            inputByteBuffer.position(newLimit);
            byteBuffers.add(inputByteBufferCopy);
            transferredBytes.addAndGet(chunkSize);
        }
    }

    private boolean isLastChunk() {
        long remainingBytes = totalBytes - transferredBytes.get();
        return remainingBytes != 0 && remainingBytes == currentBuffer.position();
    }

    private void addCurrentBufferToIterable(List<ByteBuffer> byteBuffers, int capacity) {
        ByteBuffer bufferedChunk = ByteBuffer.allocate(capacity);
        currentBuffer.flip();
        bufferedChunk.put(currentBuffer);
        bufferedChunk.flip();
        byteBuffers.add(bufferedChunk);
        transferredBytes.addAndGet(bufferedChunk.remaining());
        currentBuffer.clear();
    }

    private void fillCurrentBuffer(ByteBuffer inputByteBuffer) {
        while (currentBuffer.position() < chunkSize) {
            if (!inputByteBuffer.hasRemaining()) {
                break;
            }

            int remainingCapacity = chunkSize - currentBuffer.position();

            if (inputByteBuffer.remaining() < remainingCapacity) {
                currentBuffer.put(inputByteBuffer);
            } else {
                ByteBuffer remainingChunk = inputByteBuffer.asReadOnlyBuffer();
                int newLimit = inputByteBuffer.position() + remainingCapacity;
                remainingChunk.limit(newLimit);
                inputByteBuffer.position(newLimit);
                currentBuffer.put(remainingChunk);
            }
        }
    }

    public interface Builder extends SdkBuilder<Builder, ChunkBuffer> {

        Builder bufferSize(int bufferSize);

        Builder totalBytes(long totalBytes);


    }

    private static final class DefaultBuilder implements Builder {

        private Integer bufferSize;
        private Long totalBytes;

        @Override
        public ChunkBuffer build() {
            return new ChunkBuffer(totalBytes, bufferSize);
        }

        @Override
        public Builder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        @Override
        public Builder totalBytes(long totalBytes) {
            this.totalBytes = totalBytes;
            return this;
        }
    }
}
