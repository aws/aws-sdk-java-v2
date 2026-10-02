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

package software.amazon.awssdk.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

public class ResponseBytesTest {
    private static final Object OBJECT = new Object();
    @Test
    public void fromByteArrayCreatesCopy() {
        byte[] input = {'a'};
        byte[] output = ResponseBytes.fromByteArray(OBJECT, input).asByteArrayUnsafe();

        input[0] = 'b';
        assertThat(output).isNotEqualTo(input);
    }

    @Test
    public void asByteArrayCreatesCopy() {
        byte[] input = {'a'};
        byte[] output = ResponseBytes.fromByteArrayUnsafe(OBJECT, input).asByteArray();

        input[0] = 'b';
        assertThat(output).isNotEqualTo(input);
    }

    @Test
    public void fromByteArrayUnsafeAndAsByteArrayUnsafeDoNotCopy() {
        byte[] input = {'a'};
        byte[] output = ResponseBytes.fromByteArrayUnsafe(OBJECT, input).asByteArrayUnsafe();

        assertThat(output).isSameAs(input);
    }

    @Test
    public void fromByteBufferUnsafe_fullBuffer_doesNotCopy() {
        byte[] inputBytes = {'a'};
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputBytes);

        ResponseBytes<Object> responseBytes = ResponseBytes.fromByteBufferUnsafe(OBJECT, inputBuffer);
        byte[] outputBytes = responseBytes.asByteArrayUnsafe();

        assertThat(inputBuffer.hasArray()).isTrue();
        assertThat(inputBuffer.isDirect()).isFalse();
        assertThat(outputBytes).isSameAs(inputBytes);

        inputBytes[0] = 'b';
        assertThat(outputBytes[0]).isEqualTo((byte) 'b');
    }

    @Test
    public void fromByteBufferUnsafe_directBuffer_createsCopy() {
        byte[] inputBytes = {'a'};
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1);
        directBuffer.put(inputBytes);
        directBuffer.flip();

        ResponseBytes<Object> responseBytes = ResponseBytes.fromByteBufferUnsafe(OBJECT, directBuffer);
        ByteBuffer outputBuffer = responseBytes.asByteBuffer();
        byte[] outputBytes = responseBytes.asByteArrayUnsafe();

        assertThat(directBuffer.hasArray()).isFalse();
        assertThat(directBuffer.isDirect()).isTrue();
        assertThat(outputBuffer.isDirect()).isFalse();
        assertThat(outputBytes).isEqualTo(inputBytes);
        assertThat(outputBytes).isNotSameAs(inputBytes);

        inputBytes[0] = 'b';
        assertThat(outputBytes[0]).isNotEqualTo((byte) 'b');
    }

    @Test
    public void fromByteBufferUnsafe_bufferWithOffset_createsCopy() {
        byte[] inputBytes = "abcdefgh".getBytes();

        ByteBuffer slicedBuffer = ByteBuffer.wrap(inputBytes, 2, 3); // "cde"

        ResponseBytes<Object> responseBytes = ResponseBytes.fromByteBufferUnsafe(OBJECT, slicedBuffer);
        byte[] outputBytes = responseBytes.asByteArrayUnsafe();

        assertThat(slicedBuffer.hasArray()).isTrue();
        assertThat(outputBytes).isEqualTo("cde".getBytes());
        assertThat(outputBytes.length).isEqualTo(3);
        assertThat(outputBytes).isNotSameAs(inputBytes);

        inputBytes[0] = 'X';
        assertThat(outputBytes[0]).isEqualTo((byte) 'c');
    }
}
