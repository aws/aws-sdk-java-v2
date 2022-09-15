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
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Class that will buffer incoming BufferBytes of totalBytes length to chunks of bufferSize*
 */
@SdkInternalApi
public final class ChunkBuffer {
    private final AtomicLong remainingBytes;
    private final ByteBuffer currentBuffer;
    private final int bufferSize;

    private ChunkBuffer(Long totalBytes, Integer bufferSize) {
        Validate.notNull(totalBytes, "The totalBytes must not be null");

        int chunkSize = bufferSize != null ? bufferSize : DEFAULT_ASYNC_CHUNK_SIZE;
        this.bufferSize = chunkSize;
        this.currentBuffer = ByteBuffer.allocate(chunkSize);
        this.remainingBytes = new AtomicLong(totalBytes);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }


    // currentBuffer and bufferedList can get over written if concurrent Threads calls this method at the same time.
    public synchronized Iterable<ByteBuffer> bufferAndCreateChunks(ByteBuffer buffer) {
        int startPosition = 0;
        List<ByteBuffer> bufferedList = new ArrayList<>();
        int currentBytesRead = buffer.remaining();
        do {
            int bufferedBytes = currentBuffer.position();
            int availableToRead = bufferSize - bufferedBytes;
            int bytesToMove = Math.min(availableToRead, currentBytesRead - startPosition);

            if (bufferedBytes == 0) {
                currentBuffer.put(buffer.array(), startPosition, bytesToMove);
            } else {
                currentBuffer.put(buffer.array(), 0, bytesToMove);
            }

            startPosition = startPosition + bytesToMove;

            // Send the data once the buffer is full
            if (currentBuffer.position() == bufferSize) {
                currentBuffer.position(0);
                ByteBuffer bufferToSend = ByteBuffer.allocate(bufferSize);
                bufferToSend.put(currentBuffer.array(), 0, bufferSize);
                bufferToSend.clear();
                currentBuffer.clear();
                bufferedList.add(bufferToSend);
                remainingBytes.addAndGet(-bufferSize);
            }
        } while (startPosition < currentBytesRead);

        int remainingBytesInBuffer = currentBuffer.position();

        // Send the remaining buffer when
        // 1. remainingBytes in buffer are same as the last few bytes to be read.
        // 2. If it is a zero byte and the last byte to be read.
        if (remainingBytes.get() == remainingBytesInBuffer &&
            (buffer.remaining() == 0 || remainingBytesInBuffer > 0)) {
            currentBuffer.clear();
            ByteBuffer trimmedBuffer = ByteBuffer.allocate(remainingBytesInBuffer);
            trimmedBuffer.put(currentBuffer.array(), 0, remainingBytesInBuffer);
            trimmedBuffer.clear();
            bufferedList.add(trimmedBuffer);
            remainingBytes.addAndGet(-remainingBytesInBuffer);
        }
        return bufferedList;
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
