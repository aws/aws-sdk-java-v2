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

package software.amazon.awssdk.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.internal.Base16Lower;

public class BinaryUtilsTest {

    @Test
    public void testHex() {
        {
            String hex = BinaryUtils.toHex(new byte[] {0});
            String hex2 = Base16Lower.encodeAsString(new byte[] {0});
            assertEquals(hex, hex2);
        }
        {
            String hex = BinaryUtils.toHex(new byte[] {-1});
            String hex2 = Base16Lower.encodeAsString(new byte[] {-1});
            assertEquals(hex, hex2);
        }
    }

    @Test
    public void testCopyBytes_Nulls() {
        assertNull(BinaryUtils.copyAllBytesFrom(null));
        assertNull(BinaryUtils.copyBytesFrom(null));
    }

    @Test
    public void testCopyBytesFromByteBuffer() {
        byte[] ba = {1, 2, 3, 4, 5};
        // capacity: 100
        final ByteBuffer b = ByteBuffer.allocate(100);
        b.put(ba);
        // limit: 5
        b.limit(5);
        assertTrue(b.capacity() > b.limit());
        b.rewind();
        assertTrue(b.position() == 0);
        b.get();
        assertTrue(b.position() == 1);
        // backing array
        byte[] array = b.array();
        assertTrue(array.length == 100);
        // actual data length
        byte[] allData = BinaryUtils.copyAllBytesFrom(b);
        assertTrue(allData.length == 5);
        // copy, not reference
        assertFalse(ba == allData);
        // partial data length
        byte[] partialData = BinaryUtils.copyBytesFrom(b);
        assertTrue(partialData.length == 4);
    }

    @Test
    public void testCopyBytesFrom_DirectByteBuffer() {
        byte[] ba = {1, 2, 3, 4, 5};
        // capacity: 100
        final ByteBuffer b = ByteBuffer.allocateDirect(100);
        b.put(ba);
        // limit: 5
        b.limit(5);
        assertTrue(b.capacity() > b.limit());
        b.rewind();
        assertTrue(b.position() == 0);
        b.get();
        assertTrue(b.position() == 1);
        // backing array
        assertFalse(b.hasArray());
        assertTrue(b.capacity() == 100);
        // actual data length
        byte[] allData = BinaryUtils.copyAllBytesFrom(b);
        assertTrue(allData.length == 5);
        // copy, not reference
        assertFalse(ba == allData);
        // partial data length
        byte[] partialData = BinaryUtils.copyBytesFrom(b);
        assertTrue(partialData.length == 4);
    }

    @Test
    public void testCopyBytesFromByteBuffer_Idempotent() {
        byte[] ba = {1, 2, 3, 4, 5};
        final ByteBuffer b = ByteBuffer.wrap(ba);
        b.limit(4);
        assertTrue(b.limit() == 4);
        b.rewind();
        assertTrue(b.position() == 0);
        b.get();
        assertTrue(b.position() == 1);
        // copy all bytes should be idempotent
        byte[] allData1 = BinaryUtils.copyAllBytesFrom(b);
        assertTrue(b.position() == 1);
        byte[] allData2 = BinaryUtils.copyAllBytesFrom(b);
        assertTrue(b.position() == 1);
        assertFalse(allData1 == allData2);
        assertTrue(allData1.length == 4);
        assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4}, allData1));

        // copy partial bytes should be idempotent
        byte[] partial1 = BinaryUtils.copyBytesFrom(b);
        assertTrue(b.position() == 1);
        byte[] partial2 = BinaryUtils.copyBytesFrom(b);
        assertTrue(b.position() == 1);
        assertFalse(partial1 == partial2);
        assertTrue(partial1.length == 3);
        assertTrue(Arrays.equals(new byte[] {2, 3, 4}, partial1));
    }

    @Test
    public void testCopyBytesFrom_DirectByteBuffer_Idempotent() {
        byte[] ba = {1, 2, 3, 4, 5};
        final ByteBuffer b = ByteBuffer.allocateDirect(ba.length);
        b.put(ba).rewind();
        b.limit(4);
        assertTrue(b.limit() == 4);
        b.rewind();
        assertTrue(b.position() == 0);
        b.get();
        assertTrue(b.position() == 1);
        // copy all bytes should be idempotent
        byte[] allData1 = BinaryUtils.copyAllBytesFrom(b);
        assertTrue(b.position() == 1);
        byte[] allData2 = BinaryUtils.copyAllBytesFrom(b);
        assertTrue(b.position() == 1);
        assertFalse(allData1 == allData2);
        assertTrue(allData1.length == 4);
        assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4}, allData1));

        // copy partial bytes should be idempotent
        byte[] partial1 = BinaryUtils.copyBytesFrom(b);
        assertTrue(b.position() == 1);
        byte[] partial2 = BinaryUtils.copyBytesFrom(b);
        assertTrue(b.position() == 1);
        assertFalse(partial1 == partial2);
        assertTrue(partial1.length == 3);
        assertTrue(Arrays.equals(new byte[] {2, 3, 4}, partial1));
    }

    @Test
    public void testCopyRemainingBytesFrom_nullBuffer() {
        assertThat(BinaryUtils.copyRemainingBytesFrom(null)).isNull();
    }

    @Test
    public void testCopyRemainingBytesFrom_noRemainingBytes() {
        ByteBuffer bb = ByteBuffer.allocate(1);
        bb.put(new byte[] {1});
        bb.flip();

        bb.get();

        assertThat(BinaryUtils.copyRemainingBytesFrom(bb)).hasSize(0);
    }

    @Test
    public void testCopyRemainingBytesFrom_fullBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(new byte[] {1, 2, 3, 4});
        bb.flip();

        byte[] copy = BinaryUtils.copyRemainingBytesFrom(bb);
        assertThat(bb).isEqualTo(ByteBuffer.wrap(copy));
        assertThat(copy).hasSize(4);
    }

    @Test
    public void testCopyRemainingBytesFrom_partiallyReadBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(new byte[] {1, 2, 3, 4});
        bb.flip();

        bb.get();
        bb.get();

        byte[] copy = BinaryUtils.copyRemainingBytesFrom(bb);
        assertThat(bb).isEqualTo(ByteBuffer.wrap(copy));
        assertThat(copy).hasSize(2);
    }

    @Test
    public void testImmutableCopyOfByteBuffer() {
        ByteBuffer sourceBuffer = ByteBuffer.allocate(4);
        byte[] originalBytesInSource = {1, 2, 3, 4};
        sourceBuffer.put(originalBytesInSource);
        sourceBuffer.flip();

        ByteBuffer immutableCopy = BinaryUtils.immutableCopyOf(sourceBuffer);

        byte[] bytesInSourceAfterCopy = {-1, -2, -3, -4};
        sourceBuffer.put(bytesInSourceAfterCopy);
        sourceBuffer.flip();

        assertTrue(immutableCopy.isReadOnly());
        byte[] fromImmutableCopy = new byte[originalBytesInSource.length];
        immutableCopy.get(fromImmutableCopy);
        assertArrayEquals(originalBytesInSource, fromImmutableCopy);

        assertEquals(0, sourceBuffer.position());
        byte[] fromSource = new byte[bytesInSourceAfterCopy.length];
        sourceBuffer.get(fromSource);
        assertArrayEquals(bytesInSourceAfterCopy, fromSource);
    }

    @Test
    public void immutableCopyOf_retainsOriginalLimit() {
        ByteBuffer sourceBuffer = ByteBuffer.allocate(10);
        byte[] bytes = {1, 2, 3, 4};
        sourceBuffer.put(bytes);
        sourceBuffer.rewind();
        sourceBuffer.limit(bytes.length);
        ByteBuffer copy = BinaryUtils.immutableCopyOf(sourceBuffer);
        assertThat(copy.limit()).isEqualTo(sourceBuffer.limit());
    }

    @Test
    public void testImmutableCopyOfByteBuffer_nullBuffer() {
        assertNull(BinaryUtils.immutableCopyOf(null));
    }

    @Test
    public void testImmutableCopyOfByteBuffer_partiallyReadBuffer() {
        ByteBuffer sourceBuffer = ByteBuffer.allocate(4);
        byte[] bytes = {1, 2, 3, 4};
        sourceBuffer.put(bytes);
        sourceBuffer.position(2);

        ByteBuffer immutableCopy = BinaryUtils.immutableCopyOf(sourceBuffer);

        assertEquals(sourceBuffer.position(), immutableCopy.position());
        immutableCopy.rewind();
        byte[] fromImmutableCopy = new byte[bytes.length];
        immutableCopy.get(fromImmutableCopy);
        assertArrayEquals(bytes, fromImmutableCopy);
    }

    @Test
    public void testImmutableCopyOfRemainingByteBuffer() {
        ByteBuffer sourceBuffer = ByteBuffer.allocate(4);
        byte[] originalBytesInSource = {1, 2, 3, 4};
        sourceBuffer.put(originalBytesInSource);
        sourceBuffer.flip();

        ByteBuffer immutableCopy = BinaryUtils.immutableCopyOfRemaining(sourceBuffer);

        byte[] bytesInSourceAfterCopy = {-1, -2, -3, -4};
        sourceBuffer.put(bytesInSourceAfterCopy);
        sourceBuffer.flip();

        assertTrue(immutableCopy.isReadOnly());
        byte[] fromImmutableCopy = new byte[originalBytesInSource.length];
        immutableCopy.get(fromImmutableCopy);
        assertArrayEquals(originalBytesInSource, fromImmutableCopy);

        assertEquals(0, sourceBuffer.position());
        byte[] fromSource = new byte[bytesInSourceAfterCopy.length];
        sourceBuffer.get(fromSource);
        assertArrayEquals(bytesInSourceAfterCopy, fromSource);
    }

    @Test
    public void testImmutableCopyOfByteBufferRemaining_nullBuffer() {
        assertNull(BinaryUtils.immutableCopyOfRemaining(null));
    }

    @Test
    public void testImmutableCopyOfByteBufferRemaining_partiallyReadBuffer() {
        ByteBuffer sourceBuffer = ByteBuffer.allocate(4);
        byte[] bytes = {1, 2, 3, 4};
        sourceBuffer.put(bytes);
        sourceBuffer.position(2);

        ByteBuffer immutableCopy = BinaryUtils.immutableCopyOfRemaining(sourceBuffer);

        assertEquals(2, immutableCopy.capacity());
        assertEquals(2, immutableCopy.remaining());
        assertEquals(0, immutableCopy.position());
        assertEquals((byte) 3, immutableCopy.get());
        assertEquals((byte) 4, immutableCopy.get());
    }

    @Test
    public void testToNonDirectBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(4);
        byte[] expected = {1, 2, 3, 4};
        bb.put(expected);
        bb.flip();

        ByteBuffer nonDirectBuffer = BinaryUtils.toNonDirectBuffer(bb);

        assertFalse(nonDirectBuffer.isDirect());
        byte[] bytes = new byte[expected.length];
        nonDirectBuffer.get(bytes);
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToNonDirectBuffer_nullBuffer() {
        assertNull(BinaryUtils.toNonDirectBuffer(null));
    }

    @Test
    public void testToNonDirectBuffer_partiallyReadBuffer() {
        ByteBuffer sourceBuffer = ByteBuffer.allocateDirect(4);
        byte[] bytes = {1, 2, 3, 4};
        sourceBuffer.put(bytes);
        sourceBuffer.position(2);

        ByteBuffer nonDirectBuffer = BinaryUtils.toNonDirectBuffer(sourceBuffer);

        assertEquals(sourceBuffer.position(), nonDirectBuffer.position());
        nonDirectBuffer.rewind();
        byte[] fromNonDirectBuffer = new byte[bytes.length];
        nonDirectBuffer.get(fromNonDirectBuffer);
        assertArrayEquals(bytes, fromNonDirectBuffer);
    }

    @Test
    public void testToNonDirectBuffer_nonDirectBuffer() {
        ByteBuffer nonDirectBuffer = ByteBuffer.allocate(0);
        assertThrows(IllegalArgumentException.class, () -> BinaryUtils.toNonDirectBuffer(nonDirectBuffer));
    }

}
