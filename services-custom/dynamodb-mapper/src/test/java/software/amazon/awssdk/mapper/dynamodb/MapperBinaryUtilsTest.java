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
package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;

import org.junit.Test;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.mapper.dynamodb.internal.MapperBinaryUtils;

/**
 * Tests {@link MapperBinaryUtils#toWritableByteBuffer(SdkBytes)}, which reproduces the v1 mapper's
 * behavior of returning a writable {@link ByteBuffer} for binary attributes.
 */
public class MapperBinaryUtilsTest {

    @Test
    public void toWritableByteBuffer_nullInput_returnsNull() {
        assertNull(MapperBinaryUtils.toWritableByteBuffer(null));
    }

    @Test
    public void toWritableByteBuffer_preservesContents() {
        byte[] bytes = {1, 2, 3, 4};
        ByteBuffer result = MapperBinaryUtils.toWritableByteBuffer(SdkBytes.fromByteArray(bytes));

        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(bytes, actual);
    }

    @Test
    public void toWritableByteBuffer_returnsWritableBuffer() {
        ByteBuffer result = MapperBinaryUtils.toWritableByteBuffer(SdkBytes.fromUtf8String("data"));

        assertFalse(result.isReadOnly());
        result.put(0, (byte) 'X');
        assertEquals((byte) 'X', result.get(0));
    }

    @Test
    public void toWritableByteBuffer_returnsIndependentCopy() {
        SdkBytes source = SdkBytes.fromByteArray(new byte[] {1, 2, 3});
        ByteBuffer result = MapperBinaryUtils.toWritableByteBuffer(source);

        result.put(0, (byte) 99);

        // Mutating the returned buffer must not corrupt the source SdkBytes.
        assertArrayEquals(new byte[] {1, 2, 3}, source.asByteArray());
    }
}
