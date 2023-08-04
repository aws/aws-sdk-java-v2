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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Class that will buffer incoming BufferBytes with unknown total length to chunks of bufferSize
 */
@SdkInternalApi
public final class ChunkBufferWithUnknownLength {
    private ByteBuffer currentBuffer;

    private ChunkBufferWithUnknownLength(Integer bufferSize) {
        int chunkSize = bufferSize != null ? bufferSize : DEFAULT_ASYNC_CHUNK_SIZE;
        this.currentBuffer = ByteBuffer.allocate(chunkSize);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public synchronized Iterable<ByteBuffer> bufferAndCreateChunks(ByteBuffer buffer) {
        List<ByteBuffer> bufferedList = new ArrayList<>();
        while (buffer.hasRemaining()) {
            int bytesToCopy = Math.min(buffer.remaining(), currentBuffer.remaining());
            byte[] bytes = new byte[bytesToCopy];
            buffer.get(bytes);
            currentBuffer.put(bytes);

            if (!currentBuffer.hasRemaining() || !buffer.hasRemaining()) {
                currentBuffer.flip();
                ByteBuffer bufferToSend = ByteBuffer.allocate(currentBuffer.limit());
                bufferToSend.put(currentBuffer);
                bufferToSend.flip();
                bufferedList.add(bufferToSend);
                currentBuffer.clear();
            }
        }
        return bufferedList;
    }

    public interface Builder extends SdkBuilder<Builder, ChunkBufferWithUnknownLength> {
        Builder bufferSize(int bufferSize);
    }

    private static final class DefaultBuilder implements Builder {
        private Integer bufferSize;

        @Override
        public ChunkBufferWithUnknownLength build() {
            return new ChunkBufferWithUnknownLength(bufferSize);
        }

        @Override
        public Builder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }
    }
}
