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

package software.amazon.awssdk.core.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.AbortedException;

public class SdkFilterInputStreamTest {
    private InputStream mockStream;
    private TestInputStream filterInputStream;

    @BeforeEach
    void setup() {
        mockStream = mock(InputStream.class);
        filterInputStream = new TestInputStream(mockStream);
    }

    @Test
    void isClosed_streamClosed_returnsTrue() throws IOException {
        filterInputStream.close();
        assertThat(filterInputStream.isClosed()).isTrue();
    }

    @Test
    void isClosed_readByteReturnsMinus1_returnsTrue() throws IOException {
        when(mockStream.read()).thenReturn(-1);
        filterInputStream.read();
        assertThat(filterInputStream.isClosed()).isTrue();
    }

    // Possible if two threads try to read the stream at the same time. If one of them sees -1, that value should be "sticky".
    @Test
    void isClosed_readByte_returnsMinus1ThenPos_returnsTrue() throws IOException {
        when(mockStream.read()).thenReturn(-1);
        filterInputStream.read();
        assertThat(filterInputStream.isClosed()).isTrue();

        when(mockStream.read()).thenReturn(42);
        filterInputStream.read();
        assertThat(filterInputStream.isClosed()).isTrue();
    }

    @Test
    void isClosed_readByteArray_returnsMinus1ThenPos_returnsTrue() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        filterInputStream.read(new byte[0], 0, 0);
        assertThat(filterInputStream.isClosed()).isTrue();

        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(42);
        filterInputStream.read(new byte[0], 0, 0);
        assertThat(filterInputStream.isClosed()).isTrue();
    }


    @Test
    void isClosed_readByteBytesReturnsMinus1_returnsTrue() throws IOException {
        when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        filterInputStream.read(new byte[0], 0, 0);
        assertThat(filterInputStream.isClosed()).isTrue();
    }

    @Test
    void isClosed_released_returnsTrue() {
        filterInputStream.release();
        assertThat(filterInputStream.isClosed()).isTrue();
    }

    @Test
    void isClosed_abortsDueToInterrupt_returnsTrue() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(filterInputStream::read).isInstanceOf(AbortedException.class);
            assertThat(filterInputStream.isClosed()).isTrue();
        } finally {
            // clear the flag
            Thread.interrupted();
        }
    }

    @Test
    void isClosed_eofThenReset_returnsTrue() throws IOException {
        when(mockStream.read()).thenReturn(-1);
        filterInputStream.read();
        assertThat(filterInputStream.isClosed()).isTrue();

        filterInputStream.reset();

        assertThat(filterInputStream.isClosed()).isFalse();
    }

    private static class TestInputStream extends SdkFilterInputStream {

        protected TestInputStream(InputStream in) {
            super(in);
        }

        @Override
        public boolean isClosed() {
            return super.isClosed();
        }
    }
}
