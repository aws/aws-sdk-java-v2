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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.internal.Base16Lower;

public class BinaryUtilsTest {

    @Test
    public void testHex() {
        {
            String hex = BinaryUtils.toHex(new byte[] {0});
            System.out.println(hex);
            String hex2 = Base16Lower.encodeAsString(new byte[] {0});
            Assertions.assertEquals(hex, hex2);
        }
        {
            String hex = BinaryUtils.toHex(new byte[] {-1});
            System.out.println(hex);
            String hex2 = Base16Lower.encodeAsString(new byte[] {-1});
            Assertions.assertEquals(hex, hex2);
        }
    }

    @Test
    public void testCopyBytes_Nulls() {
        Assertions.assertNull(BinaryUtils.copyAllBytesFrom(null));
        Assertions.assertNull(BinaryUtils.copyBytesFrom(null));
    }

    @Test
    public void testCopyBytesFromByteBuffer() {
        byte[] ba = {1, 2, 3, 4, 5};
        // capacity: 100
        final ByteBuffer b = ByteBuffer.allocate(100);
        b.put(ba);
        // limit: 5
        b.limit(5);
        Assertions.assertTrue(b.capacity() > b.limit());
        b.rewind();
        Assertions.assertTrue(b.position() == 0);
        b.get();
        Assertions.assertTrue(b.position() == 1);
        // backing array
        byte[] array = b.array();
        Assertions.assertTrue(array.length == 100);
        // actual data length
        byte[] allData = BinaryUtils.copyAllBytesFrom(b);
        Assertions.assertTrue(allData.length == 5);
        // copy, not reference
        Assertions.assertFalse(ba == allData);
        // partial data length
        byte[] partialData = BinaryUtils.copyBytesFrom(b);
        Assertions.assertTrue(partialData.length == 4);
    }

    @Test
    public void testCopyBytesFrom_DirectByteBuffer() {
        byte[] ba = {1, 2, 3, 4, 5};
        // capacity: 100
        final ByteBuffer b = ByteBuffer.allocateDirect(100);
        b.put(ba);
        // limit: 5
        b.limit(5);
        Assertions.assertTrue(b.capacity() > b.limit());
        b.rewind();
        Assertions.assertTrue(b.position() == 0);
        b.get();
        Assertions.assertTrue(b.position() == 1);
        // backing array
        Assertions.assertFalse(b.hasArray());
        Assertions.assertTrue(b.capacity() == 100);
        // actual data length
        byte[] allData = BinaryUtils.copyAllBytesFrom(b);
        Assertions.assertTrue(allData.length == 5);
        // copy, not reference
        Assertions.assertFalse(ba == allData);
        // partial data length
        byte[] partialData = BinaryUtils.copyBytesFrom(b);
        Assertions.assertTrue(partialData.length == 4);
    }

    @Test
    public void testCopyBytesFromByteBuffer_Idempotent() {
        byte[] ba = {1, 2, 3, 4, 5};
        final ByteBuffer b = ByteBuffer.wrap(ba);
        b.limit(4);
        Assertions.assertTrue(b.limit() == 4);
        b.rewind();
        Assertions.assertTrue(b.position() == 0);
        b.get();
        Assertions.assertTrue(b.position() == 1);
        // copy all bytes should be idempotent
        byte[] allData1 = BinaryUtils.copyAllBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        byte[] allData2 = BinaryUtils.copyAllBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        Assertions.assertFalse(allData1 == allData2);
        Assertions.assertTrue(allData1.length == 4);
        Assertions.assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4}, allData1));

        // copy partial bytes should be idempotent
        byte[] partial1 = BinaryUtils.copyBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        byte[] partial2 = BinaryUtils.copyBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        Assertions.assertFalse(partial1 == partial2);
        Assertions.assertTrue(partial1.length == 3);
        Assertions.assertTrue(Arrays.equals(new byte[] {2, 3, 4}, partial1));
    }

    @Test
    public void testCopyBytesFrom_DirectByteBuffer_Idempotent() {
        byte[] ba = {1, 2, 3, 4, 5};
        final ByteBuffer b = ByteBuffer.allocateDirect(ba.length);
        b.put(ba).rewind();
        b.limit(4);
        Assertions.assertTrue(b.limit() == 4);
        b.rewind();
        Assertions.assertTrue(b.position() == 0);
        b.get();
        Assertions.assertTrue(b.position() == 1);
        // copy all bytes should be idempotent
        byte[] allData1 = BinaryUtils.copyAllBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        byte[] allData2 = BinaryUtils.copyAllBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        Assertions.assertFalse(allData1 == allData2);
        Assertions.assertTrue(allData1.length == 4);
        Assertions.assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4}, allData1));

        // copy partial bytes should be idempotent
        byte[] partial1 = BinaryUtils.copyBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        byte[] partial2 = BinaryUtils.copyBytesFrom(b);
        Assertions.assertTrue(b.position() == 1);
        Assertions.assertFalse(partial1 == partial2);
        Assertions.assertTrue(partial1.length == 3);
        Assertions.assertTrue(Arrays.equals(new byte[] {2, 3, 4}, partial1));
    }

    @Test
    public void testCopyRemainingBytesFrom_nullBuffer() {
        assertThat(BinaryUtils.copyRemainingBytesFrom(null)).isNull();
    }

    @Test
    public void testCopyRemainingBytesFrom_noRemainingBytes() {
        ByteBuffer bb = ByteBuffer.allocate(1);
        bb.put(new byte[]{1});
        bb.flip();

        bb.get();

        assertThat(BinaryUtils.copyRemainingBytesFrom(bb)).hasSize(0);
    }

    @Test
    public void testCopyRemainingBytesFrom_fullBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(new byte[]{1, 2, 3, 4});
        bb.flip();

        byte[] copy = BinaryUtils.copyRemainingBytesFrom(bb);
        assertThat(bb).isEqualTo(ByteBuffer.wrap(copy));
        assertThat(copy).hasSize(4);
    }

    @Test
    public void testCopyRemainingBytesFrom_partiallyReadBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(new byte[]{1, 2, 3, 4});
        bb.flip();

        bb.get();
        bb.get();

        byte[] copy = BinaryUtils.copyRemainingBytesFrom(bb);
        assertThat(bb).isEqualTo(ByteBuffer.wrap(copy));
        assertThat(copy).hasSize(2);
    }
}
