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

package software.amazon.awssdk.protocols.json;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.protocols.json.SdkByteArrayOutputStream.CHUNK_SIZE;
import static software.amazon.awssdk.protocols.json.SdkByteArrayOutputStream.MAX_BUFFER_SIZE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * Unit tests for {@link SdkByteArrayOutputStream}, covering both the small-payload (base buffer)
 * and large-payload (overflow) paths.
 */
class SdkByteArrayOutputStreamTest {

    private static final Random RANDOM = new Random(42);

    @Test
    void write_smallPayload_behavesLikeByteArrayOutputStream() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        byte[] data = randomBytes(1000);
        stream.write(data, 0, data.length);

        assertThat(stream.size()).isEqualTo(1000);
        assertThat(stream.toByteArray()).isEqualTo(data);
    }

    @Test
    void write_singleBytes_smallPayload_behavesCorrectly() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(16);
        for (int i = 0; i < 256; i++) {
            stream.write(i);
        }

        assertThat(stream.size()).isEqualTo(256);
        byte[] result = stream.toByteArray();
        for (int i = 0; i < 256; i++) {
            assertThat(result[i]).isEqualTo((byte) i);
        }
    }

    @Test
    void size_emptyStream_returnsZero() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        assertThat(stream.size()).isEqualTo(0);
        assertThat(stream.toByteArray()).isEmpty();
    }

    @Test
    void write_exactlyMaxBufferSize_doesNotOverflow() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        byte[] data = randomBytes(MAX_BUFFER_SIZE);
        stream.write(data, 0, data.length);

        assertThat(stream.size()).isEqualTo(MAX_BUFFER_SIZE);
        assertThat(stream.toByteArray()).isEqualTo(data);
    }

    @Test
    void write_oneBytePastMaxBufferSize_triggersOverflow() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        byte[] data = randomBytes(MAX_BUFFER_SIZE + 1);
        stream.write(data, 0, data.length);

        assertThat(stream.size()).isEqualTo(MAX_BUFFER_SIZE + 1);
        assertThat(stream.toByteArray()).isEqualTo(data);
    }

    @Test
    void write_singleByte_triggersOverflow() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        byte[] base = randomBytes(MAX_BUFFER_SIZE);
        stream.write(base, 0, base.length);

        // This single byte should trigger overflow
        stream.write(0xFF);

        assertThat(stream.size()).isEqualTo(MAX_BUFFER_SIZE + 1);
        byte[] result = stream.toByteArray();
        assertThat(Arrays.copyOf(result, MAX_BUFFER_SIZE)).isEqualTo(base);
        assertThat(result[MAX_BUFFER_SIZE]).isEqualTo((byte) 0xFF);
    }

    @Test
    void write_largePayload_multipleChunks_producesCorrectOutput() {
        // Write enough to span the base buffer + multiple overflow chunks
        int totalSize = MAX_BUFFER_SIZE + (CHUNK_SIZE * 3) + 100;
        byte[] data = randomBytes(totalSize);

        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(data, 0, data.length);

        assertThat(stream.size()).isEqualTo(totalSize);
        assertThat(stream.toByteArray()).isEqualTo(data);
    }

    @Test
    void write_largePayload_incrementalWrites_producesCorrectOutput() {
        // Write in small increments that cross chunk boundaries
        int totalSize = MAX_BUFFER_SIZE + (CHUNK_SIZE * 2) + 500;
        byte[] data = randomBytes(totalSize);

        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        int offset = 0;
        int chunkSize = 1337; // Deliberately not aligned to CHUNK_SIZE
        while (offset < data.length) {
            int len = Math.min(chunkSize, data.length - offset);
            stream.write(data, offset, len);
            offset += len;
        }

        assertThat(stream.size()).isEqualTo(totalSize);
        assertThat(stream.toByteArray()).isEqualTo(data);
    }

    @Test
    void write_singleBytes_intoOverflow_producesCorrectOutput() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        byte[] base = randomBytes(MAX_BUFFER_SIZE);
        stream.write(base, 0, base.length);

        // Write 200 single bytes into overflow
        byte[] overflow = new byte[200];
        for (int i = 0; i < 200; i++) {
            overflow[i] = (byte) (i & 0xFF);
            stream.write(overflow[i]);
        }

        assertThat(stream.size()).isEqualTo(MAX_BUFFER_SIZE + 200);
        byte[] result = stream.toByteArray();
        assertThat(Arrays.copyOf(result, MAX_BUFFER_SIZE)).isEqualTo(base);
        assertThat(Arrays.copyOfRange(result, MAX_BUFFER_SIZE, result.length)).isEqualTo(overflow);
    }

    @Test
    void contentSize_smallPayload_matchesSize() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        byte[] data = randomBytes(500);
        stream.write(data, 0, data.length);

        assertThat(stream.contentSize()).isEqualTo(500);
        assertThat(stream.contentSize()).isEqualTo(stream.size());
    }

    @Test
    void contentSize_largePayload_matchesSize() {
        int totalSize = MAX_BUFFER_SIZE + CHUNK_SIZE + 100;
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(randomBytes(totalSize), 0, totalSize);

        assertThat(stream.contentSize()).isEqualTo(totalSize);
        assertThat(stream.contentSize()).isEqualTo(stream.size());
    }

    @Test
    void contentStreamProvider_smallPayload_producesSameBytesAsToByteArray() throws IOException {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        byte[] data = randomBytes(5000);
        stream.write(data, 0, data.length);

        ContentStreamProvider provider = stream.contentStreamProvider();
        byte[] streamed = readAllBytes(provider.newStream());

        assertThat(streamed).isEqualTo(data);
    }

    @Test
    void contentStreamProvider_largePayload_producesSameBytesAsToByteArray() throws IOException {
        int totalSize = MAX_BUFFER_SIZE + (CHUNK_SIZE * 2) + 500;
        byte[] data = randomBytes(totalSize);

        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(data, 0, data.length);

        byte[] expected = stream.toByteArray();
        ContentStreamProvider provider = stream.contentStreamProvider();
        byte[] streamed = readAllBytes(provider.newStream());

        assertThat(streamed).isEqualTo(expected);
    }

    @Test
    void contentStreamProvider_isResettable_smallPayload() throws IOException {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        byte[] data = randomBytes(100);
        stream.write(data, 0, data.length);

        ContentStreamProvider provider = stream.contentStreamProvider();
        byte[] first = readAllBytes(provider.newStream());
        byte[] second = readAllBytes(provider.newStream());

        assertThat(first).isEqualTo(data);
        assertThat(second).isEqualTo(data);
    }

    @Test
    void contentStreamProvider_isResettable_largePayload() throws IOException {
        int totalSize = MAX_BUFFER_SIZE + CHUNK_SIZE + 100;
        byte[] data = randomBytes(totalSize);

        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(data, 0, data.length);

        ContentStreamProvider provider = stream.contentStreamProvider();
        byte[] first = readAllBytes(provider.newStream());
        byte[] second = readAllBytes(provider.newStream());

        assertThat(first).isEqualTo(data);
        assertThat(second).isEqualTo(data);
    }

    @Test
    void contentStreamProvider_emptyStream_producesEmptyContent() throws IOException {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        ContentStreamProvider provider = stream.contentStreamProvider();
        byte[] content = readAllBytes(provider.newStream());

        assertThat(content).isEmpty();
    }

    @Test
    void reset_smallPayload_clearsAllData() {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        stream.write(new byte[]{1, 2, 3}, 0, 3);

        stream.reset();

        assertThat(stream.size()).isEqualTo(0);
        assertThat(stream.toByteArray()).isEmpty();
    }

    @Test
    void reset_afterOverflow_clearsAllState() {
        int totalSize = MAX_BUFFER_SIZE + CHUNK_SIZE + 100;
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(randomBytes(totalSize), 0, totalSize);

        assertThat(stream.size()).isEqualTo(totalSize);

        stream.reset();

        assertThat(stream.size()).isEqualTo(0);
        assertThat(stream.toByteArray()).isEmpty();
        assertThat(stream.contentSize()).isEqualTo(0);
    }

    @Test
    void reset_afterOverflow_allowsReuse() {
        int totalSize = MAX_BUFFER_SIZE + CHUNK_SIZE + 100;
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(randomBytes(totalSize), 0, totalSize);

        stream.reset();

        // Write new data after reset
        byte[] newData = randomBytes(500);
        stream.write(newData, 0, newData.length);

        assertThat(stream.size()).isEqualTo(500);
        assertThat(stream.toByteArray()).isEqualTo(newData);
    }

    @Test
    void reset_afterOverflow_thenWriteLargeAgain_producesCorrectOutput() {
        int totalSize = MAX_BUFFER_SIZE + 500;
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(randomBytes(totalSize), 0, totalSize);

        stream.reset();

        // Write a different large payload
        byte[] newData = randomBytes(MAX_BUFFER_SIZE + 1000);
        stream.write(newData, 0, newData.length);

        assertThat(stream.size()).isEqualTo(newData.length);
        assertThat(stream.toByteArray()).isEqualTo(newData);
    }

    @Test
    void writeTo_smallPayload_writesAllData() throws IOException {
        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(64);
        byte[] data = randomBytes(500);
        stream.write(data, 0, data.length);

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);

        assertThat(target.toByteArray()).isEqualTo(data);
    }

    @Test
    void writeTo_largePayload_writesAllData() throws IOException {
        int totalSize = MAX_BUFFER_SIZE + (CHUNK_SIZE * 2) + 500;
        byte[] data = randomBytes(totalSize);

        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(data, 0, data.length);

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);

        assertThat(target.toByteArray()).isEqualTo(data);
    }

    @Test
    void writeTo_afterOverflow_matchesToByteArray() throws IOException {
        int totalSize = MAX_BUFFER_SIZE + CHUNK_SIZE + 100;
        byte[] data = randomBytes(totalSize);

        SdkByteArrayOutputStream stream = new SdkByteArrayOutputStream(1024);
        stream.write(data, 0, data.length);

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        stream.writeTo(target);

        assertThat(target.toByteArray()).isEqualTo(stream.toByteArray());
    }

    private static byte[] randomBytes(int length) {
        byte[] data = new byte[length];
        RANDOM.nextBytes(data);
        return data;
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
