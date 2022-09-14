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

package software.amazon.awssdk.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.internal.async.ChunkBuffer;
import software.amazon.awssdk.utils.StringUtils;

class ChunkBufferTest {

    @Test
    void builderWithNoTotalSize() {
        assertThatThrownBy(() -> ChunkBuffer.builder().build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void numberOfChunkMultipleOfTotalBytes() {
        String inputString = StringUtils.repeat("*", 25);

        ChunkBuffer chunkBuffer =
            ChunkBuffer.builder().bufferSize(5).totalBytes(inputString.getBytes(StandardCharsets.UTF_8).length).build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.bufferAndCreateChunks(ByteBuffer.wrap(inputString.getBytes(StandardCharsets.UTF_8)));

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
            assertThat(r.array()).isEqualTo(StringUtils.repeat("*", 5).getBytes(StandardCharsets.UTF_8));
        });
        assertThat(iteratedCounts.get()).isEqualTo(5);
    }

    @Test
    void numberOfChunk_Not_MultipleOfTotalBytes() {
        int totalBytes = 23;
        int bufferSize = 5;

        String inputString = StringUtils.repeat("*", totalBytes);
        ChunkBuffer chunkBuffer =
            ChunkBuffer.builder().bufferSize(bufferSize).totalBytes(inputString.getBytes(StandardCharsets.UTF_8).length).build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.bufferAndCreateChunks(ByteBuffer.wrap(inputString.getBytes(StandardCharsets.UTF_8)));

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
            if (iteratedCounts.get() * bufferSize < totalBytes) {
                assertThat(r.array()).isEqualTo(StringUtils.repeat("*", bufferSize).getBytes(StandardCharsets.UTF_8));
            } else {
                assertThat(r.array()).isEqualTo(StringUtils.repeat("*", 3).getBytes(StandardCharsets.UTF_8));

            }
        });
    }

    @Test
    void zeroTotalBytesAsInput_returnsZeroByte() {
        byte[] zeroByte = new byte[0];
        ChunkBuffer chunkBuffer =
            ChunkBuffer.builder().bufferSize(5).totalBytes(zeroByte.length).build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.bufferAndCreateChunks(ByteBuffer.wrap(zeroByte));

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
        });
        assertThat(iteratedCounts.get()).isEqualTo(1);
    }

    @Test
    void emptyAllocatedBytes_returnSameNumberOfEmptyBytes() {

        int totalBytes = 17;
        int bufferSize = 5;
        ByteBuffer wrap = ByteBuffer.allocate(totalBytes);
        ChunkBuffer chunkBuffer =
            ChunkBuffer.builder().bufferSize(bufferSize).totalBytes(wrap.remaining()).build();
        Iterable<ByteBuffer> byteBuffers =
            chunkBuffer.bufferAndCreateChunks(wrap);

        AtomicInteger iteratedCounts = new AtomicInteger();
        byteBuffers.forEach(r -> {
            iteratedCounts.getAndIncrement();
            if (iteratedCounts.get() * bufferSize < totalBytes) {
                // array of empty bytes
                assertThat(r.array()).isEqualTo(ByteBuffer.allocate(bufferSize).array());
            } else {
                assertThat(r.array()).isEqualTo(ByteBuffer.allocate(totalBytes % bufferSize).array());
            }
        });
        assertThat(iteratedCounts.get()).isEqualTo(4);
    }
}


